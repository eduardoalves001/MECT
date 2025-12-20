const { defineConfig } = require('cypress')

module.exports = defineConfig({
  e2e: {
    baseUrl: process.env.CYPRESS_baseUrl || 'http://localhost:8090',
    supportFile: 'cypress/support/e2e.ts',
    specPattern: 'cypress/e2e/**/*.cy.{js,jsx,ts,tsx}',
    video: true,
    videoCompression: 32,
    screenshotOnRunFailure: true,
    videosFolder: 'cypress/videos',
    screenshotsFolder: 'cypress/screenshots',
    viewportWidth: 1280,
    viewportHeight: 720,
    trashAssetsBeforeRuns: true,
    env: {
      apiUrl: process.env.CYPRESS_apiUrl || 'http://localhost:8095',
      keycloakUrl: process.env.CYPRESS_keycloakUrl || 'http://localhost:8097',
    },
    setupNodeEvents(on, config) {
      // implement node event listeners here
    },
  },
})
