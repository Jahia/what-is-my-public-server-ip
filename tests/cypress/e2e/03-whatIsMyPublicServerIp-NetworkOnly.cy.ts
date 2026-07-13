/**
 * S27 — the admin component forces a GraphQL round-trip on every mount
 * (`fetchPolicy: 'network-only'`). A `cache-first` regression would fire the query only once
 * across two mounts; this asserts the intercept fires on BOTH mounts (>= 2 network calls).
 *
 * Only the *client* behaviour is asserted here — the server-side 60s cache (U1) still shields
 * `checkip.amazonaws.com` from repeated egress.
 */
describe('What Is My Public Server IP — network-only fetch policy', () => {
    const adminPath = '/jahia/administration/whatIsMyPublicServerIp'

    before(() => {
        cy.login()
    })

    it('re-queries GraphQL on every mount (fetchPolicy: network-only)', () => {
        let hits = 0
        cy.intercept('POST', '**/graphql', (req) => {
            if (typeof req.body?.query === 'string' && req.body.query.includes('whatIsMyPublicServerIp')) {
                hits += 1
            }
        }).as('graphql')

        // First mount
        cy.login()
        cy.visit(adminPath)
        cy.get('[class*="ipValue"]').should('exist')

        // Navigate away, then mount a second time
        cy.visit('/jahia/administration/')
        cy.visit(adminPath)
        cy.get('[class*="ipValue"]').should('exist')

        // A cache-first regression would leave hits === 1
        cy.then(() => {
            expect(hits, 'GraphQL query fired on both mounts').to.be.gte(2)
        })
    })
})
