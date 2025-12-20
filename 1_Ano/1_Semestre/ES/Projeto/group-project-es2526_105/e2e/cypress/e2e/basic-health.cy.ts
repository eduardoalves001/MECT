describe('RTMP Application - Basic Health Check', () => {
  it('should load the frontend application', () => {
    cy.visit('/')
    cy.url().should('include', Cypress.config('baseUrl'))
  })

  it('should verify backend API is healthy', () => {
    cy.checkApiHealth()
  })

  it('should display the application title or header', () => {
    cy.visit('/')
    // Wait for the page to load
    cy.get('body').should('be.visible')
    
    // Check if page has loaded successfully
    cy.contains(/threat|risk|rtmp/i, { timeout: 10000 }).should('exist')
  })
})
