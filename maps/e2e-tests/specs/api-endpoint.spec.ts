const PRODUCTION_TASK_HASH = '30ce2c3d-2fb2f0df-79680613-5c495d25';
const TESTING_TASK_HASH = 'db10d7ac-b3ba3e62-46713dbe-9cf267b7';
const TESTING_TASK_LOAD_TIMEOUT = 60000; // very big

Cypress.on('uncaught:exception', () => {
  return false;
});

context('Api Endpoint', () => {
  context('Check production', () => {
    it('Production task', () => {
      cy.openTaskById(PRODUCTION_TASK_HASH);
    });
  });

  context('Check testing', () => {
    it('Testing task', () => {
      cy.openTaskById(TESTING_TASK_HASH, 'ru', {
        timeout: TESTING_TASK_LOAD_TIMEOUT,
      });
    });
  });
});
