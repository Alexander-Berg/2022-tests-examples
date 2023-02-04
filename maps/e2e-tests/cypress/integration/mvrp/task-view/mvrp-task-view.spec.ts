import * as mvrpKeyset from '../../../../../src/translations/mvrp';
import selectors from '../../../../src/constants/selectors';
import urls from '../../../../src/utils/urls';

context('Mvrp task view', () => {
  before(() => {
    cy.preserveCookies();
    cy.clearLocalforage();

    cy.fixture('company-data').then(({ mvrpView }) => {
      const TASK_ID = 'c5a3e61a-617c1e76-3de4f15b-f733dab0';
      const link = urls.mvrp.openTask(mvrpView.companyId, TASK_ID);

      cy.yandexLogin('mvrpViewManager', { link });
      cy.openAndCloseVideo();
    });
  });

  it('should show selected route orders', () => {
    cy.get(selectors.content.mvrp.routes.firstRow).should('exist');
    cy.wait(300);
    cy.get(selectors.content.mvrp.routes.firstRowSomeVisibleCell).click();

    cy.get(selectors.content.mvrp.activeRoute.firstOrderRow).should('exist');
  });

  it('should all columns in the table be visible', () => {
    cy.get(selectors.content.mvrp.activeRoute.headerSomeVisibleCell)
      .invoke('text')
      .should('eq', mvrpKeyset.ru.xlsxExport_metricsHeaders_total_waiting_duration);
    cy.get(selectors.content.mvrp.activeRoute.firstOrderRowSomeVisibleCell)
      .invoke('text')
      .should('eq', '08:22');
  });

  it('should all columns in the table be visible after change page', () => {
    cy.get(selectors.sidebar.menu.monitoringGroup).click();
    cy.get(selectors.sidebar.menu.dashboard).click();
    cy.wait(200);
    cy.get(selectors.sidebar.menu.mvrp).click();

    cy.get(selectors.content.mvrp.routes.firstRow).should('exist');
    cy.wait(300);
    cy.get(selectors.content.mvrp.routes.firstRowSomeVisibleCell).click();

    cy.get(selectors.content.mvrp.activeRoute.headerSomeVisibleCell)
      .invoke('text')
      .should('eq', mvrpKeyset.ru.xlsxExport_metricsHeaders_total_waiting_duration);
    cy.get(selectors.content.mvrp.activeRoute.firstOrderRowSomeVisibleCell)
      .invoke('text')
      .should('eq', '08:22');
  });
});
