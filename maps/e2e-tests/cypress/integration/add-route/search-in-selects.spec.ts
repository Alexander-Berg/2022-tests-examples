import * as importKeyset from '../../../../src/translations/import';
import * as courierMapKeyset from '../../../../src/translations/courier-map';
import * as courierQualityReportKeyset from '../../../../src/translations/courier-quality-report';
import * as dashboardKeyset from '../../../../src/translations/dashboard';
import * as createRouteFormKeyset from '../../../../src/translations/create-route-form';
import selectors from '../../../src/constants/selectors';
import { courierNameRecord } from '../../../src/constants/couriers';
import { depotNameRecord } from '../../../src/constants/depots';
import dateFnsFormat from '../../../src/utils/date-fns-format';

// @see https://testpalm.yandex-team.ru/courier/testcases/532
const moreOptionsDropdownOptions = {
  createRoute: createRouteFormKeyset.ru.title,
  deleteRoutes: dashboardKeyset.ru.deleteRoutes,
};

const fieldLabels = {
  name: createRouteFormKeyset.ru.label_number,
  date: createRouteFormKeyset.ru.label_date,
  depot: courierQualityReportKeyset.ru.row_type_depot,
  courier: courierMapKeyset.ru.tableColumn_courier,
  mode: createRouteFormKeyset.ru.label_routingMode,
};

const formPlaceholders = {
  depot: createRouteFormKeyset.ru.placeholder_depotId,
  courier: createRouteFormKeyset.ru.placeholder_courierId,
};

const defaultRouteRegex = /^Маршрут №\d+$/;

context('Empty route', function () {
  before(() => {
    cy.preserveCookies();
    cy.yandexLogin('manager');
    cy.get(selectors.sidebar.menu.monitoringGroup).click();
    cy.get(selectors.sidebar.menu.dashboard).click();
    cy.get(selectors.content.dashboard.moreOptionsDropdown.button).click();
    cy.get(selectors.content.dashboard.moreOptionsDropdown.options.createRoute).click();
  });

  context('Depot field', () => {
    before(() => {
      cy.get(selectors.modal.createRoute.depotField)
        .find(selectors.modal.createRoute.fieldInput)
        .click();
    });

    beforeEach(() => {
      cy.get(selectors.modal.createRoute.depotField).as('depot');
    });

    afterEach(() => {
      cy.get('@depot').find('input').clear();
    });

    after(() => {
      //click outside
      cy.get(selectors.modal.createRoute.root).click(0, 0);
    });

    it('should display options on search', function () {
      cy.log(depotNameRecord.castlePeach.slice(0, 3));
      cy.get('@depot').click().type(depotNameRecord.castlePeach.slice(0, 3));
      cy.get('@depot').find(selectors.select.option).should('have.length', 1);
      cy.get('@depot')
        .find(selectors.select.option)
        .should('have.text', depotNameRecord.castlePeach);
    });

    it('should display options on search with special chars', function () {
      cy.get('@depot').click().type('_');
      cy.get('@depot').find(selectors.select.option).should('be.visible');
    });
  });

  context('Courier field', () => {
    before(() => {
      cy.get(selectors.modal.createRoute.courierField)
        .find(selectors.modal.createRoute.fieldInput)
        .click();
    });

    beforeEach(() => {
      cy.get(selectors.modal.createRoute.courierField).as('courier');
    });

    afterEach(() => {
      cy.get('@courier').find('input').clear();
    });

    it('should display options on search', function () {
      cy.get('@courier').click().type(courierNameRecord.kypa.slice(0, 3));
      cy.get('@courier').find(selectors.select.option).should('have.length', 1);
      cy.get('@courier').find(selectors.select.option).should('have.text', courierNameRecord.kypa);
    });

    it('should display options on search with special chars', function () {
      cy.get('@courier').click().type('+');
      cy.get('@courier').find(selectors.select.option).should('be.visible');
    });
  });
});
