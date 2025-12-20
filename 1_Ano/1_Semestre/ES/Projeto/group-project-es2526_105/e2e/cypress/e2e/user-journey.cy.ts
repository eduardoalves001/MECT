describe('RTMP Application - User Journey Happy Path', () => {
  let testData: any

  before(() => {
    cy.fixture('testData').then((data) => {
      testData = data
    })
  })

  beforeEach(() => {
    // Clear session storage and cookies before each test
    cy.clearCookies()
    cy.clearLocalStorage()
  })

  it('should complete the full user journey: login, create threat model, and verify', () => {
    const testThreatModel = {
      name: `${testData.testThreatModel.namePrefix} ${Date.now()}`,
      description: testData.testThreatModel.description,
    }

    // Step 1-6: Login with Keycloak
    cy.loginWithKeycloak(testData.testUser.username, testData.testUser.password)
    cy.screenshot('01-after-login')
    cy.log('✓ User authenticated successfully')

    // Step 7: Navigate to Models page
    cy.navigateToPage('Models')
    cy.contains('Threat Models', { timeout: 10000 }).should('be.visible')
    cy.screenshot('02-models-page')
    cy.log('✓ Navigated to Models page')

    // Step 8-9: Create a new threat model
    cy.createThreatModel(testThreatModel.name, testThreatModel.description)
    cy.screenshot('03-model-created')
    cy.log('✓ Threat model created successfully')

    // Step 10: Verify the model appears in the list
    cy.verifyThreatModelExists(testThreatModel.name)
    cy.log('✓ Threat model visible in the list')

    // Step 11: View model details
    cy.wait(1000)
    cy.contains(testThreatModel.name).click()
    cy.contains(testThreatModel.name, { timeout: 10000 }).should('be.visible')
    cy.contains(testThreatModel.description).should('be.visible')
    cy.screenshot('04-model-details-page')
    cy.log('✓ Threat model details page opened successfully')

    // Step 12: Clean up - delete the created threat model from details page
    cy.deleteThreatModel(testThreatModel.name)
    cy.screenshot('05-model-deleted')
    cy.log('✓ Test threat model deleted successfully')
  })
})
