import { DocumentNode } from 'graphql'
import { createUser, deleteUser, grantRoles } from '@jahia/cypress'

/**
 * Regression tests for the fine-grained `publicServerIpAdmin` permission.
 *
 * These guard against the gate being silently removed or mismatched across the stack:
 *  - Backend: `@GraphQLRequiresPermission("publicServerIpAdmin")` on the `whatIsMyPublicServerIp`
 *    query (resolved as a root-node ACL check).
 *  - Frontend: `requiredPermission: 'publicServerIpAdmin'` in register.jsx gates the admin route.
 *  - RBAC content: the module ships the assignable `what-is-my-public-server-ip-administrator` role
 *    (src/main/import/roles.xml) granting ONLY that permission (+ administrationAccess for the UI).
 *
 * The "allowed" user is granted that role and nothing else — never `admin` — so the tests prove
 * fine-grained granularity, not merely that a full administrator can pass.
 */
describe('What Is My Public Server IP — permission enforcement', () => {
    const ROLE_NAME = 'what-is-my-public-server-ip-administrator'
    const DENIED_USER = 'wimpsiDeniedUser'
    const ALLOWED_USER = 'wimpsiAllowedUser'
    const PASSWORD = 'WimpsiPerm9Pwd'
    const ADMIN_PATH = '/jahia/administration/whatIsMyPublicServerIp'
    // IPv4 or IPv6 (matches the existing functional spec)
    const ipPattern = /^(\d{1,3}\.){3}\d{1,3}$|^([0-9a-fA-F]{0,4}:){2,7}[0-9a-fA-F]{0,4}$/

    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const getWhatIsMyPublicServerIp: DocumentNode = require('graphql-tag/loader!../fixtures/graphql/query/getWhatIsMyPublicServerIp.graphql')

    const errorsOf = (result: { graphQLErrors?: Array<{ message: string }>; errors?: Array<{ message: string }> }) =>
        result.graphQLErrors ?? result.errors ?? []

    const queryAs = (username: string) => {
        cy.apolloClient({ username, password: PASSWORD })
        return cy.apollo({ query: getWhatIsMyPublicServerIp })
    }

    before(() => {
        cy.login()
        createUser(DENIED_USER, PASSWORD)
        createUser(ALLOWED_USER, PASSWORD)
        // The annotation resolves the permission on the JCR root node, so grant the
        // module-shipped single-permission role on `/`.
        grantRoles('/', [ROLE_NAME], ALLOWED_USER, 'USER')
    })

    after(() => {
        cy.apolloClient() // reset the current Apollo client back to root
        cy.login()
        deleteUser(DENIED_USER)
        deleteUser(ALLOWED_USER)
    })

    describe('GraphQL API authorization', () => {
        it('denies the gated query for a user without the permission', () => {
            queryAs(DENIED_USER).then((result: never) => {
                const errs = errorsOf(result)
                expect(errs, 'denial errors').to.have.length.greaterThan(0)
                expect(errs.map((e: { message: string }) => e.message).join(' ')).to.contain('Permission denied')
            })
        })

        it('allows the gated query for a user granted only the module permission', () => {
            queryAs(ALLOWED_USER).then((result: never) => {
                expect(errorsOf(result), 'should have no errors').to.have.length(0)
                const ip = (result as { data: { whatIsMyPublicServerIp: { ip: string } } }).data.whatIsMyPublicServerIp
                    .ip
                expect(ip, 'public IP').to.be.a('string')
                expect(ip).to.match(ipPattern)
            })
        })
    })

    describe('Admin UI authorization', () => {
        it('hides the admin panel from a user without the permission', () => {
            cy.login(DENIED_USER, PASSWORD)
            cy.visit(ADMIN_PATH, { failOnStatusCode: false })
            cy.get('[class*="ipValue"]').should('not.exist')
        })

        it('shows the admin panel to a user granted only the module permission', () => {
            cy.login(ALLOWED_USER, PASSWORD)
            cy.visit(ADMIN_PATH)
            cy.get('[class*="ipValue"]').should('be.visible')
        })
    })
})
