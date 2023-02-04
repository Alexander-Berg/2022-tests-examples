import * as depotSettingsKeyset from '../../../../../src/translations/depot-settings';
import * as depotsKeyset from '../../../../../src/translations/depots';
import selectors from '../../../../src/constants/selectors';

describe('Admin B.', function () {
  beforeEach(function () {
    cy.preserveCookies();
  });

  before(function () {
    cy.yandexLogin('companyBadmin');
    cy.get(selectors.content.dashboard.view).should('exist');
  });

  describe('Work with other companies', function () {
    // @see https://testpalm.yandex-team.ru/testcase/courier-121
    it('Company is visible in company selector', function () {
      cy.fixture('company-data').then(({ A }) => {
        cy.get(selectors.sidebar.companySelector.control).click();
        cy.get(selectors.sidebar.companySelector.orgs.dropdown).should('exist');
        cy.get(selectors.sidebar.companySelector.orgs.dropdown)
          .invoke('text')
          .should('contain', A.companyName);
      });
    });

    // @see https://testpalm.yandex-team.ru/testcase/courier-123
    it('Goto company A', function () {
      cy.fixture('company-data').then(({ A }) => {
        cy.get(selectors.sidebar.companySelector.orgs.dropdownItems.firstOrg).click();
        cy.get(selectors.sidebar.companySelector.orgs.currentName).should('exist');

        cy.get(selectors.sidebar.companySelector.orgs.currentName)
          .invoke('text')
          .should('eq', A.companyName);
      });
    });

    after(function () {
      cy.get(selectors.sidebar.companySelector.control).click();
    });
  });

  describe('Dashboard couriers', function () {
    // @see https://testpalm.yandex-team.ru/testcase/courier-179
    it('Couriers is visible in dashboard', function () {
      cy.fixture('testData').then(({ courierNameRecord }) => {
        cy.get(selectors.content.dashboard.couriers.table.row)
          .invoke('text')
          .should('contain', courierNameRecord.red);
      });
    });

    // @see https://testpalm.yandex-team.ru/testcase/courier-125
    it('See company A order from dashboard', function () {
      cy.fixture('company-data').then(({ B }) => {
        cy.get(selectors.content.dashboard.couriers.table.orders.sequenced).click();
        cy.get(selectors.modal.orderPopup.watcher).should('exist');
        cy.get(selectors.modal.orderPopup.watcher).invoke('text').should('eq', B.companyName);
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

    // @see https://testpalm.yandex-team.ru/testcase/courier-124
    it('Validation fields in orders table', function () {
      cy.fixture('company-data').then(({ B }) => {
        cy.get(selectors.content.orders.tableLoaded).should('exist');
        cy.get(selectors.content.orders.tableRows).invoke('text').should('contain', B.companyName);
      });
    });

    after(function () {
      cy.get(selectors.sidebar.menu.dashboard).click();
      cy.get(selectors.sidebar.menu.monitoringGroup).click();
    });
  });

  describe('Depots', function () {
    // @see https://testpalm.yandex-team.ru/testcase/courier-177
    it('Can see depots of company A', function () {
      cy.fixture('testData').then(({ depotNameRecord }) => {
        cy.get(selectors.sidebar.companySelector.control).click();

        cy.get(selectors.sidebar.companySelector.depots.dropdown)
          .invoke('text')
          .should(
            'eq',
            [
              depotsKeyset.ru.selectTitle,
              depotSettingsKeyset.ru.allDepots,
              depotNameRecord.compAVisible,
            ].join(''),
          );
      });
    });

    after(function () {
      cy.get(selectors.sidebar.companySelector.control).click();
    });
  });
});
