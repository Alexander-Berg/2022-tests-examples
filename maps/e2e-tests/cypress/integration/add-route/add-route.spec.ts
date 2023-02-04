import * as couriersKeyset from '../../../../src/translations/couriers';
import * as commonKeyset from '../../../../src/translations/common';
import * as courierRouteKeyset from '../../../../src/translations/courier-route';
import * as companySettingsKeyset from '../../../../src/translations/company-settings';
import * as activeCourierKeyset from '../../../../src/translations/active-courier';
import * as courierDetailsKeyset from '../../../../src/translations/courier-details';
import * as courierMapKeyset from '../../../../src/translations/courier-map';
import * as courierQualityReportKeyset from '../../../../src/translations/courier-quality-report';
import * as dashboardKeyset from '../../../../src/translations/dashboard';
import * as createRouteFormKeyset from '../../../../src/translations/create-route-form';
import * as importKeyset from '../../../../src/translations/import';
import size from 'lodash/size';
import map from 'lodash/map';
import compact from 'lodash/compact';
import last from 'lodash/last';
import first from 'lodash/first';

import selectors from 'constants/selectors';
import dateFnsFormat from 'utils/date-fns-format';
import { getAddRouteGenerateScheme } from '../../plugins/schemes/add-routes/add-route.schema';

let routeName: string | undefined;
let routeCourier: string | undefined;

const routingModes = {
  driving: importKeyset.ru.routingModes_driving,
  transit: importKeyset.ru.routingModes_transit,
  truck: importKeyset.ru.routingModes_truck,
  walking: importKeyset.ru.routingModes_walking,
};

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

const courierDetailsLabels = {
  login: courierDetailsKeyset.ru.login,
  phone: activeCourierKeyset.ru.phone,
  smsTumbler: companySettingsKeyset.ru.sms_label,
};

const routeMapDetailsLabels = {
  builtTheRoute: courierMapKeyset.ru.routeBuiltAt,
  sentCoordinates: courierMapKeyset.ru.coordsSentAt,
  timeZone: courierMapKeyset.ru.localTimeZone,
};

const actions = {
  changeRoute: courierRouteKeyset.ru.changeCourierDropdown_title,
  showCourierOnMap: courierMapKeyset.ru.showOnMap,
  addOrder: commonKeyset.ru.table_addOrders,
};

const defaultRouteRegex = /^Маршрут №\d+$/;

context('Empty route', function () {
  const BRANCH_NAME = Cypress.env('BRANCH_NAME');
  const schema = getAddRouteGenerateScheme(BRANCH_NAME);
  const courierAmount = size(schema.addRoute.couriers);
  const depotAmount = size(schema.addRoute.depots);

  before(function () {
    cy.preserveCookies();
    cy.makeData('add-route');

    cy.yandexLogin('managerAddRoute');
    cy.get(selectors.sidebar.menu.monitoringGroup).click();
    cy.get(selectors.sidebar.menu.dashboard).click();
  });

  after(function () {
    cy.removeData('add-route');
  });

  describe('Empty route form', () => {
    it('should be reachable from dashboard', function () {
      cy.get(selectors.content.dashboard.moreOptionsDropdown.button).click();
      cy.get(selectors.content.dashboard.moreOptionsDropdown.options.createRoute).should(
        'have.text',
        moreOptionsDropdownOptions.createRoute,
      );
      cy.get(selectors.content.dashboard.moreOptionsDropdown.options.deleteRoutes).should(
        'have.text',
        moreOptionsDropdownOptions.deleteRoutes,
      );
      cy.get(selectors.content.dashboard.moreOptionsDropdown.options.createRoute).click();
      cy.get(selectors.modal.createRoute.root).should('be.visible');
    });

    it('should display fields with default values and submit button', function () {
      cy.get(selectors.modal.createRoute.nameField).as('name').should('be.visible');
      cy.get(selectors.modal.createRoute.dateField).as('date').should('be.visible');
      cy.get(selectors.modal.createRoute.depotField).as('depot').should('be.visible');
      cy.get(selectors.modal.createRoute.courierField).as('courier').should('be.visible');
      cy.get(selectors.modal.createRoute.modeField).as('mode').should('be.visible');

      cy.get('@name')
        .find(selectors.modal.createRoute.fieldLabel)
        .should('have.text', fieldLabels.name);
      cy.get('@date')
        .find(selectors.modal.createRoute.fieldLabel)
        .should('have.text', fieldLabels.date);
      cy.get('@depot')
        .find(selectors.modal.createRoute.fieldLabel)
        .should('have.text', fieldLabels.depot);
      cy.get('@courier')
        .find(selectors.modal.createRoute.fieldLabel)
        .should('have.text', fieldLabels.courier);
      cy.get('@mode')
        .find(selectors.modal.createRoute.fieldLabel)
        .should('have.text', fieldLabels.mode);

      cy.get('@name').find('input').invoke('val').should('match', defaultRouteRegex);
      cy.get('@name')
        .find('input')
        .invoke('val')
        .then(nameValue => {
          routeName = nameValue as string;
        });
      cy.get('@date').find('input').should('have.value', dateFnsFormat(new Date(), 'dd MMMM'));
      cy.get('@depot').find(selectors.select.value).should('not.exist');
      cy.get('@courier').find(selectors.select.value).should('not.exist');
      cy.get('@mode')
        .find(selectors.select.value)
        .should('have.text', importKeyset.ru.routingModes_driving);

      cy.get('@depot')
        .find(selectors.select.placeholder)
        .should('have.text', formPlaceholders.depot);
      cy.get('@courier')
        .find(selectors.select.placeholder)
        .should('have.text', formPlaceholders.courier);

      cy.get(selectors.modal.createRoute.createRouteSubmit).should('be.disabled');
    });

    describe('Depot field', () => {
      beforeEach(() => {
        cy.get(selectors.modal.createRoute.depotField).as('depot');
      });

      it('should be select with options', function () {
        cy.get('@depot').find(selectors.modal.createRoute.fieldInput).click();
        cy.get('@depot').find(selectors.select.option).should('have.length', depotAmount);
        cy.get('@depot')
          .find(selectors.select.optionsList)
          .should('be.visible')
          .and('have.css', 'overflow-y', 'auto');
      });

      it('should display selected value on change', function () {
        cy.get('@depot').find(selectors.select.option).first().invoke('text').as('depotToChoose');
        cy.get('@depot')
          .find(selectors.select.option)
          .first()
          .click()
          .then(() => {
            cy.get('@depot').find(selectors.select.optionsList).should('not.exist');
            cy.get('@depot').find(selectors.select.value).should('have.text', this.depotToChoose);
          });
      });

      it('should not enable submit button on change', function () {
        cy.get(selectors.modal.createRoute.createRouteSubmit).should('be.disabled');
      });
    });

    describe('Courier field', () => {
      beforeEach(() => {
        cy.get(selectors.modal.createRoute.courierField).as('courier');
      });

      it('should be select with options', function () {
        cy.get('@courier').find(selectors.modal.createRoute.fieldInput).click();
        cy.get('@courier').find(selectors.select.option).should('have.length', courierAmount);
        cy.get('@courier')
          .find(selectors.select.optionsList)
          .should('be.visible')
          .and('have.css', 'overflow-y', 'auto');
      });

      it('should display selected value on change', function () {
        cy.get('@courier')
          .find(selectors.select.option)
          .first()
          .invoke('text')
          .as('courierToChoose');
        cy.get('@courier')
          .find(selectors.select.option)
          .first()
          .click()
          .then(() => {
            routeCourier = this.courierToChoose;
            cy.get('@courier').find(selectors.select.optionsList).should('not.exist');
            cy.get('@courier')
              .find(selectors.select.value)
              .should('have.text', this.courierToChoose);
          });
      });

      it('should enable submit button on depot and courier change', function () {
        cy.get(selectors.modal.createRoute.createRouteSubmit).should('not.be.disabled');
      });
    });

    describe('Route mode field', () => {
      beforeEach(() => {
        cy.get(selectors.modal.createRoute.modeField).as('mode');
      });

      it('should be select with options', function () {
        cy.get('@mode').find(selectors.modal.createRoute.fieldInput).click();
        cy.get('@mode').find(selectors.select.option).should('have.length', 4);
        cy.get('@mode')
          .contains(selectors.select.option, routingModes.driving)
          .should('be.visible');
        cy.get('@mode')
          .contains(selectors.select.option, routingModes.walking)
          .should('be.visible');
        cy.get('@mode')
          .contains(selectors.select.option, routingModes.transit)
          .should('be.visible');
        cy.get('@mode').contains(selectors.select.option, routingModes.truck).should('be.visible');
      });

      it('should display selected value on change', function () {
        cy.get('@mode')
          .contains(selectors.select.option, importKeyset.ru.routingModes_walking)
          .click();

        cy.get('@mode').find(selectors.select.optionsList).should('not.exist');
        cy.get('@mode').find(selectors.select.value).should('have.text', routingModes.walking);
      });
    });
  });

  describe('Created empty route', () => {
    before(() => {
      cy.get(selectors.modal.createRoute.createRouteSubmit).click();
    });

    it('should be displayed in list of routes', function () {
      cy.get(selectors.modal.createRoute.root).should('not.exist');
      cy.contains(selectors.content.dashboard.couriers.table.routeName, routeName as string)
        .should('be.visible')
        .closest(selectors.content.dashboard.couriers.table.row)
        .as('routeRow');

      // courier name is displayed only in first route row of the courier route rows
      // so if courier name is present, it should be equal to name value from form
      // otherwise previous nonempty courier name should be equal to name value from form
      cy.get('@routeRow')
        .find(selectors.content.dashboard.couriers.table.courierNameContainer)
        .invoke('text')
        .then(routeRowCourier => {
          if (routeRowCourier) {
            expect(routeRowCourier).to.be.equal(routeCourier as string);
          } else {
            cy.get('@routeRow')
              .prevAll(selectors.content.dashboard.couriers.table.row)
              .then(rows => {
                const courierNames = map(
                  rows,
                  row =>
                    row.querySelector(selectors.content.dashboard.couriers.table.courierNames)
                      ?.textContent,
                );
                const lastExistedName = last(compact(courierNames));
                expect(lastExistedName).eq(routeCourier);
              });
          }
        });
      cy.get('@routeRow')
        .find(selectors.content.dashboard.couriers.table.orders.root)
        .find('button')
        .should('have.length', 1);
      cy.get('@routeRow')
        .find(selectors.content.dashboard.couriers.table.orders.root)
        .find(selectors.content.dashboard.couriers.table.orders.menu)
        .should('be.visible');
    });

    it('should display courier info', function () {
      cy.wait(200);
      cy.get('.courier-route-loading').should('not.exist');

      cy.contains(
        selectors.content.dashboard.couriers.table.routeName,
        routeName as string,
      ).click();

      cy.get(selectors.content.courier.name).should('have.text', routeCourier);
      cy.get(selectors.content.courier.details.root).as('courierDetails').should('be.visible');
      cy.get('@courierDetails')
        .find(selectors.content.courier.details.label)
        .eq(0)
        .should('have.text', courierDetailsLabels.login);
      cy.get('@courierDetails')
        .find(selectors.content.courier.details.value)
        .eq(0)
        .should('not.be.empty');
      cy.get('@courierDetails')
        .find(selectors.content.courier.details.label)
        .eq(1)
        .should('have.text', courierDetailsLabels.phone);
      cy.get('@courierDetails')
        .find(selectors.content.courier.details.value)
        .eq(1)
        .should('not.be.empty');
      cy.get('@courierDetails')
        .find(selectors.content.courier.details.label)
        .eq(2)
        .should('have.text', courierDetailsLabels.smsTumbler);
      cy.get('@courierDetails')
        .find(selectors.content.courier.details.value)
        .eq(2)
        .find(selectors.content.courier.details.smsTumbler)
        .should('be.visible');
    });

    it('should display map with map elements', function () {
      cy.get(selectors.content.courier.route.map.yandex.places).children().should('have.length', 1);
      cy.get(selectors.content.courier.route.map.yandex.places)
        .children()
        .eq(0)
        .find(selectors.content.courier.route.map.yandex.depotIcon)
        .should('be.visible');
      cy.get(selectors.content.courier.route.map.yandex.rulerIcon).should('be.visible');
      cy.get(selectors.content.courier.route.map.yandex.zoom).should('be.visible');

      cy.get(selectors.content.courier.route.map.common.details.root)
        .as('mapRouteDetails')
        .should('be.visible');
      cy.get('@mapRouteDetails')
        .find(selectors.content.courier.route.map.common.details.label)
        .eq(0)
        .should('have.text', routeMapDetailsLabels.builtTheRoute);
      cy.get('@mapRouteDetails')
        .find(selectors.content.courier.route.map.common.details.label)
        .eq(1)
        .should('have.text', routeMapDetailsLabels.sentCoordinates);
      cy.get('@mapRouteDetails')
        .find(selectors.content.courier.route.map.common.details.label)
        .eq(2)
        .should('have.text', routeMapDetailsLabels.timeZone);
      cy.get('@mapRouteDetails')
        .find(selectors.content.courier.route.map.common.details.value)
        .eq(2)
        .should('not.be.empty');
    });

    it('should display route controls, info and actions', function () {
      cy.get(selectors.content.courier.route.map.common.slider).should('be.visible');
      cy.get(selectors.content.courier.route.map.common.time).should('be.visible');
      cy.get(selectors.content.courier.route.map.common.courierButton)
        .should('be.visible')
        .and('have.text', actions.showCourierOnMap);

      cy.get(selectors.content.courier.route.changeRouteDropdown.button)
        .should('have.text', actions.changeRoute)
        .should('be.visible');
      cy.get(selectors.content.courier.route.tableSettings).should('be.visible');

      cy.get(selectors.content.courier.addOrderButton)
        .should('be.visible')
        .and('have.text', actions.addOrder);
    });

    after(() => {
      cy.get(selectors.content.courier.route.changeRouteDropdown.button).click();
      cy.get(selectors.content.courier.route.changeRouteDropdown.options.removeRoute).click();
      cy.contains('button', couriersKeyset.ru.removeDialog_submit).click();

      cy.get(selectors.sidebar.menu.dashboard).click({ scrollBehavior: false });
    });
  });

  // @see https://testpalm.yandex-team.ru/courier/testcases/535
  describe('Add route form validation', () => {
    before(() => {
      cy.get(selectors.content.dashboard.view);
      cy.get(selectors.content.dashboard.dayOrderNumber).invoke('text').as('initialDayCount');
      cy.get(selectors.content.dashboard.dayTotalRoutesNumber)
        .invoke('text')
        .as('initialRoutesCount');
    });

    it('should open add route form', () => {
      cy.get(selectors.content.dashboard.moreOptionsDropdown.button).click();
      cy.get(selectors.content.dashboard.moreOptionsDropdown.options.createRoute).click();

      cy.get(selectors.modal.createRoute.nameField).should('be.visible');
      cy.get(selectors.modal.createRoute.depotField).should('be.visible');
      cy.get(selectors.modal.createRoute.courierField).should('be.visible');
    });

    it('should select depot', function () {
      cy.get(selectors.modal.createRoute.depotField)
        .find(selectors.modal.createRoute.fieldInput)
        .click();

      cy.get(selectors.modal.createRoute.depotField)
        .find(selectors.select.option)
        .first()
        .invoke('text')
        .as('depotToChoose');

      cy.get(selectors.modal.createRoute.depotField)
        .find(selectors.select.option)
        .first()
        .click()
        .then(() => {
          cy.get(selectors.modal.createRoute.depotField)
            .find(selectors.select.optionsList)
            .should('not.exist');
          cy.get(selectors.modal.createRoute.depotField)
            .find(selectors.select.value)
            .should('have.text', this.depotToChoose);
        });
    });

    it('should select courier', function () {
      cy.get(selectors.modal.createRoute.courierField)
        .find(selectors.modal.createRoute.fieldInput)
        .click();

      cy.get(selectors.modal.createRoute.courierField)
        .find(selectors.select.option)
        .first()
        .invoke('text')
        .as('courierToChoose');

      cy.get(selectors.modal.createRoute.courierField)
        .find(selectors.select.option)
        .first()
        .click()
        .then(() => {
          cy.get(selectors.modal.createRoute.courierField)
            .find(selectors.select.optionsList)
            .should('not.exist');
          cy.get(selectors.modal.createRoute.courierField)
            .find(selectors.select.value)
            .should('have.text', this.courierToChoose);
        });
    });

    it('should disable add route button when name input is cleared', () => {
      cy.get(selectors.modal.createRoute.nameField).clear();

      cy.get(selectors.modal.createRoute.createRouteSubmit).should('be.disabled');
    });

    it('should validate duplicated route name', () => {
      const someExistRouteNumber = first(schema.addRoute.routes)?.data?.number;

      if (!someExistRouteNumber) {
        throw new Error(`can't get someExistRouteNumber`);
      }

      cy.get(selectors.modal.createRoute.nameField).type(someExistRouteNumber);

      cy.get(selectors.modal.createRoute.createRouteSubmit).should('be.not.disabled');
      // a moment later
      cy.get(selectors.modal.createRoute.createRouteSubmit).should('be.disabled');
      cy.get(selectors.modal.createRoute.fieldError)
        .contains(createRouteFormKeyset.ru.duplicatedRouteNumber)
        .should('exist');
    });

    it('should not change dashboard info after closing', function () {
      cy.get(selectors.content.dashboard.view).should('not.be.visible');
      cy.get(selectors.modal.createRoute.closeButton).click();

      cy.get(selectors.content.dashboard.view).should('be.visible');
      cy.get(selectors.content.dashboard.dayOrderNumber).should('have.text', this.initialDayCount);
      cy.get(selectors.content.dashboard.dayTotalRoutesNumber).should(
        'have.text',
        this.initialRoutesCount,
      );
    });

    it('should validate duplicated route name on submit', () => {
      cy.get(selectors.content.dashboard.moreOptionsDropdown.button).click();
      cy.get(selectors.content.dashboard.moreOptionsDropdown.options.createRoute).click();

      const someExistRouteNumber = first(schema.addRoute.routes)?.data?.number;

      if (!someExistRouteNumber) {
        throw new Error(`can't get someExistRouteNumber`);
      }

      cy.get(selectors.modal.createRoute.depotField)
        .find(selectors.modal.createRoute.fieldInput)
        .click()
        .get(selectors.modal.createRoute.depotField)
        .find(selectors.select.option)
        .first()
        .click();

      cy.get(selectors.modal.createRoute.courierField)
        .find(selectors.modal.createRoute.fieldInput)
        .click()
        .get(selectors.modal.createRoute.courierField)
        .find(selectors.select.option)
        .first()
        .click();

      cy.get(selectors.modal.createRoute.nameField).clear().type(someExistRouteNumber);
      cy.get(selectors.modal.createRoute.createRouteSubmit).click();

      cy.get(selectors.modal.errorPopup.view).should('exist');
      cy.get(selectors.modal.errorPopup.log).should(
        'contain.text',
        `Route with ${someExistRouteNumber} number already exists`,
      );
    });

    it('should show filled form after closing error panel', function () {
      cy.get(selectors.modal.errorPopup.closeButton).click();
      const someExistRouteNumber = first(schema.addRoute.routes)?.data?.number;

      if (!someExistRouteNumber) {
        throw new Error(`can't get someExistRouteNumber`);
      }

      cy.get(selectors.modal.createRoute.root).should('exist');

      cy.get(selectors.modal.createRoute.nameField)
        .find(selectors.modal.createRoute.textInput)
        .should('have.value', someExistRouteNumber);
      cy.get(selectors.modal.createRoute.courierField)
        .find(selectors.select.value)
        .should('have.text', this.courierToChoose);
      cy.get(selectors.modal.createRoute.depotField)
        .find(selectors.select.value)
        .should('have.text', this.depotToChoose);
      cy.get(selectors.modal.createRoute.createRouteSubmit).should('be.disabled');
      cy.get(selectors.modal.createRoute.fieldError)
        .contains(createRouteFormKeyset.ru.duplicatedRouteNumber)
        .should('exist');
    });
  });
});
