import selectors from '../../../src/constants/selectors';
import urls from '../../../src/utils/urls';

context('Mobile couriers page', () => {
  beforeEach(() => {
    cy.fixture('company-data').then(({ appRole }) => {
      const link = urls.usersCourier.createLink(appRole.companyId);

      cy.log(link);
      cy.yandexLogin('appRoleManager', { link });
    });
  });

  it('fetch couriers by page', () => {
    const API_URL = Cypress.env('API_URL');

    cy.fixture('company-data').then(({ appRole }) => {
      const { companyId } = appRole;

      cy.intercept({
        method: 'GET', // Route all GET requests
        url: `${API_URL}/companies/${companyId}/users?page=1`,
      }).as('first');
      cy.intercept({
        method: 'GET', // Route all GET requests
        url: `${API_URL}/companies/${companyId}/users?page=2`,
      }).as('second');

      cy.wait('@first').its('request.url').should('include', 'page=1');
      cy.wait('@second').its('request.url').should('include', 'page=2');
    });
  });

  it('has last courier in list', () => {
    const API_URL = Cypress.env('API_URL');

    cy.fixture('company-data').then(({ appRole }) => {
      const { companyId } = appRole;
      cy.intercept({
        method: 'GET', // Route all GET requests
        url: `${API_URL}/companies/${companyId}/users?page=2`,
      }).as('second');
      cy.wait('@second').then(interception => {
        const lastCreatedCourier: string = interception?.response?.body[0].login;
        cy.log(`last courier login ${lastCreatedCourier}`);

        cy.get('.users__list .search input').type(lastCreatedCourier);
        cy.get(selectors.content.usersCourier.list.courierName)
          .invoke('text')
          .should('eq', lastCreatedCourier);
      });
    });
  });
});
