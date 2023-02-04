import selectors from '../../../src/constants/selectors';

context('Remove route for courier with single route', function () {
  beforeEach(() => {
    cy.fixture('testData').then(() => {
      cy.yandexLogin('companyEmanager');
      cy.get(selectors.content.dashboard.view);
      cy.get(selectors.content.dashboard.couriers.table.todayRow).click();
    });
  });

  it('Succesfull remove route', function () {
    cy.fixture('testData').then(() => {
      cy.get(selectors.content.couriers.singleCourier.routeActions.dropdownButton).click();
      cy.get(selectors.content.couriers.singleCourier.routeActions.removeRoute).click();
      cy.get(selectors.modal.dialog.submit).should('exist').click();

      cy.get(selectors.content.couriers.singleCourier.courierDetails.emptyRoutes).should('exist');
    });
  });
});
