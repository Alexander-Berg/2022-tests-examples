import selectors from 'constants/selectors';

Cypress.on('uncaught:exception', () => {
  return false;
});

context('Search routes', () => {
  it('Should visible map after open search routes', () => {
    cy.openTaskById('b3a42bb0-23fe4341-f8c47510-1f219ea7');
    cy.get(selectors.searchRoutes.button).should('exist').click().wait(300);
    cy.get(selectors.searchRoutes.input).should('be.visible');
  });
});
