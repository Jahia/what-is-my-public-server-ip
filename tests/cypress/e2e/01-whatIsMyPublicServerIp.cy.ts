import {DocumentNode} from 'graphql'

describe('What Is My Public Server IP', () => {
    const adminPath = '/jahia/administration/whatIsMyPublicServerIp'
    // IPv4 or IPv6
    const ipPattern = /^(\d{1,3}\.){3}\d{1,3}$|^([0-9a-fA-F]{0,4}:){2,7}[0-9a-fA-F]{0,4}$/
    let getWhatIsMyPublicServerIp: DocumentNode

    getWhatIsMyPublicServerIp = require('graphql-tag/loader!../fixtures/graphql/query/getWhatIsMyPublicServerIp.graphql')

    before(() => {
        cy.login()
    })

    it('check page loads and renders the IP card', () => {
        cy.login()
        cy.visit(adminPath)
        
        
    })

    it('displays a valid public IP address', () => {
        cy.login()
        cy.visit(adminPath)

        cy.get('[class*="ipValue"]')
            .invoke('text')
            .invoke('trim')
            .should('match', ipPattern)
    })

    it('fetches public IP via GraphQL API', () => {
        cy.login()
        cy.apollo({query: getWhatIsMyPublicServerIp})
            .its('data.whatIsMyPublicServerIp')
            .should('be.a', 'string')
            .and('match', ipPattern)
    })

    it('IP displayed in UI matches the GraphQL API response', () => {
        cy.login()
        cy.apollo({query: getWhatIsMyPublicServerIp})
            .its('data.whatIsMyPublicServerIp')
            .then((ip: string) => {
                cy.visit(adminPath)
                cy.get('[class*="ipValue"]').should('contain.text', ip)
            })
    })
})
