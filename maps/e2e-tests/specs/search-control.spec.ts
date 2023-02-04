import selectors from 'constants/selectors';

Cypress.on('uncaught:exception', () => {
  return false;
});

context('Check searching [No Task]', () => {
  it('Working search', () => {
    cy.openEmptyTask();
    cy.get(selectors.app.map)
      .find('.ymaps3-search-control')
      .should('be.visible')
      .type('моcква')
      .find('.ymaps3-search-control__list-item')
      .click();
    cy.get(selectors.app.map)
      .should('be.visible')
      .find('.ymaps3-search-control__pin')
      .should('be.visible');
  });
});
