import selectors from '../../../../src/constants/selectors';

describe('Manager V.', function () {
  beforeEach(function () {
    cy.preserveCookies();
  });

  before(function () {
    cy.yandexLogin('companyVmanager');
    cy.get(selectors.content.dashboard.view).should('exist');
  });

  describe('Work with other companies', function () {
    // @see https://testpalm.yandex-team.ru/testcase/courier-135
    it('Company is visible in company selector', function () {
      cy.fixture('company-data').then(({ B }) => {
        cy.get(selectors.sidebar.companySelector.control).click();
        cy.get(selectors.sidebar.companySelector.orgs.dropdown).should('exist');
        cy.get(selectors.sidebar.companySelector.orgs.dropdown)
          .invoke('text')
          .should('contain', B.companyName);
      });
    });

    it('Goto company A', function () {
      cy.fixture('company-data').then(({ B }) => {
        cy.get(selectors.sidebar.companySelector.orgs.dropdownItems.firstOrg).click();
        cy.get(selectors.sidebar.companySelector.orgs.currentName).should('exist');

        cy.get(selectors.sidebar.companySelector.orgs.currentName)
          .invoke('text')
          .should('eq', B.companyName);
      });
    });

    after(function () {
      cy.get(selectors.sidebar.companySelector.control).click();
    });
  });

  describe('Dashboard couriers', function () {
    // @see https://testpalm.yandex-team.ru/testcase/courier-136
    it('See company B order from dashboard', function () {
      cy.fixture('company-data').then(({ V }) => {
        cy.get(selectors.content.dashboard.couriers.table.orders.sequenced).click();
        cy.get(selectors.modal.orderPopup.watcher).should('exist');
        cy.get(selectors.modal.orderPopup.watcher).invoke('text').should('eq', V.companyName);
      });
    });

    after(function () {
      cy.get(selectors.modal.orderPopup.closeButton).click();
    });
  });

  describe('Orders', function () {
    before(function () {
      cy.get(selectors.sidebar.menu.monitoringGroup).click();
      cy.get(selectors.sidebar.menu.orders).click();
    });

    // @see https://testpalm.yandex-team.ru/testcase/courier-182
    it('Validation fields in orders table', function () {
      cy.fixture('company-data').then(({ V }) => {
        cy.get(selectors.content.orders.tableLoaded).should('exist');
        cy.get(selectors.content.orders.tableRows).invoke('text').should('contain', V.companyName);
      });
    });

    after(function () {
      cy.get(selectors.sidebar.menu.dashboard).click();
      cy.get(selectors.sidebar.menu.monitoringGroup).click();
    });
  });
});
