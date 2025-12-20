// ***********************************************
// Custom commands for Cypress tests
// ***********************************************

/// <reference types="cypress" />

declare global {
  namespace Cypress {
    interface Chainable {
      /**
       * Custom command to check if API is healthy
       * @example cy.checkApiHealth()
       */
      checkApiHealth(): Chainable<void>

      /**
       * Custom command to login with Keycloak
       * @example cy.loginWithKeycloak('user', 'user')
       */
      loginWithKeycloak(username: string, password: string): Chainable<void>

      /**
       * Navigate to a page using the sidebar
       * @example cy.navigateToPage('Models')
       */
      navigateToPage(pageName: string): Chainable<void>

      /**
       * Create a new threat model
       * @example cy.createThreatModel('Model Name', 'Description')
       */
      createThreatModel(name: string, description: string): Chainable<void>

      /**
       * Delete a threat model by name
       * @example cy.deleteThreatModel('Model Name')
       */
      deleteThreatModel(modelName: string): Chainable<void>

      /**
       * Verify threat model exists in the list
       * @example cy.verifyThreatModelExists('Model Name')
       */
      verifyThreatModelExists(modelName: string): Chainable<void>
    }
  }
}

Cypress.Commands.add('checkApiHealth', () => {
  const apiUrl = Cypress.env('apiUrl')
  cy.request({
    url: `${apiUrl}/actuator/health`,
    failOnStatusCode: false,
  }).then((response) => {
    expect(response.status).to.eq(200)
  })
})

Cypress.Commands.add('loginWithKeycloak', (username: string, password: string) => {
  const baseUrl = Cypress.config('baseUrl')
  const keycloakUrl = Cypress.env('keycloakUrl')

  cy.visit('/')

  // Click Sign In button
  cy.contains('button', 'Sign In', { timeout: 10000 }).should('be.visible').click()

  // Handle Keycloak login
  cy.origin(keycloakUrl, { args: { username, password } }, ({ username, password }) => {
    cy.url({ timeout: 10000 }).should('include', '/realms/rtmp/protocol/openid-connect/auth')
    cy.get('#username', { timeout: 10000 }).should('be.visible').type(username)
    cy.get('#password').should('be.visible').type(password)
    cy.get('#kc-login').click()
  })

  // Wait for redirect back to app
  cy.url({ timeout: 15000 }).should('include', baseUrl)
  cy.url().should('not.include', keycloakUrl)
  cy.contains('Risk & Threat Modelling Platform', { timeout: 10000 }).should('be.visible')

  // Wait for auth to complete
  cy.wait(2000)

  // Verify authentication
  cy.contains('Models', { timeout: 10000 }).should('be.visible')
})

Cypress.Commands.add('navigateToPage', (pageName: string) => {
  cy.contains(pageName).click()
  cy.wait(1000)
})

Cypress.Commands.add('createThreatModel', (name: string, description: string) => {
  // Click Add Model button
  cy.contains('button', 'Add Model', { timeout: 10000 }).should('be.visible').click()
  cy.wait(1000)

  // Verify we're on the form page
  cy.contains('Add New Model', { timeout: 10000 }).should('be.visible')
  cy.screenshot('form-add-new-model')

  // Fill in the form
  cy.get('input#name', { timeout: 10000 }).should('be.visible').clear().type(name)
  cy.get('textarea#description').should('be.visible').clear().type(description)
  cy.screenshot('form-filled')

  // Submit the form
  cy.contains('button', 'Create Model').should('be.visible').should('not.be.disabled').click()
  cy.wait(1000)

  // Verify we're back on the models list
  cy.contains('Threat Models', { timeout: 10000 }).should('be.visible')
})

Cypress.Commands.add('deleteThreatModel', (modelName: string) => {
  // Stub the confirmation dialog before clicking
  cy.window().then((win) => {
    cy.stub(win, 'confirm').returns(true)
  })

  // Click the "..." actions button for the threat model (not the user menu)
  // Look for the button that's near the threat model content, likely in a header or actions area
  cy.get('button[aria-haspopup="menu"]', { timeout: 10000 }).last().should('be.visible').click()

  // Wait after clicking the "..." modal
  cy.wait(1000)
  cy.screenshot('delete-modal-open')

  // Click the delete button in the popup
  cy.get('[role="menuitem"]').contains('Delete', { timeout: 5000 }).should('be.visible').click()
  cy.wait(3000)

  // Model should be deleted - verify by checking that the model name is no longer on the page
  cy.contains(modelName).should('not.exist')
})

Cypress.Commands.add('verifyThreatModelExists', (modelName: string) => {
  cy.contains(modelName, { timeout: 10000 }).should('be.visible')
})

export {}
