import * as activeCourierKeyset from '../../../../src/translations/active-courier';
import * as courierDetailsKeyset from '../../../../src/translations/courier-details';
import * as commonKeyset from '../../../../src/translations/common';
import * as couriersKeyset from '../../../../src/translations/couriers';
import * as courierRouteKeyset from '../../../../src/translations/courier-route';
import selectors from '../../../src/constants/selectors';
import { courierNameRecord, courierNumberRecord } from '../../../src/constants/couriers';
import { routeNumberRecord } from '../../../src/constants/routes';
import filter from 'lodash/filter';
import values from 'lodash/values';
import size from 'lodash/size';
import uniq from 'lodash/uniq';

const sizeWithoutCurrentCourier = size(uniq(values(courierNumberRecord))) - 1;

const changeRouteDropdown = {
  button: courierRouteKeyset.ru.changeCourierDropdown_title,
  options: {
    changeCourierOnCourier: courierRouteKeyset.ru.changeCourierDropdown_courier,
    moveOrders: courierRouteKeyset.ru.moveOrders_dropdownAction,
    changeCourierOnTracker: courierRouteKeyset.ru.changeCourierDropdown_tracker,
    removeRoute: courierRouteKeyset.ru.changeCourierDropdown_removeRoute,
  },
};

const routeTransferDialog = {
  title: courierRouteKeyset.ru.changeCourierDropdown_courier,
  addNewCourierButton: couriersKeyset.ru.title_modalChangeRouteCourier_addNew,
  cancelButton: commonKeyset.ru.title_cancel,
  submitButton: couriersKeyset.ru.title_send_route,
  noDataTitle: couriersKeyset.ru.title_modalChangeRouteCourier_emptySearch,
};

const dialogTableColumns = {
  login: courierDetailsKeyset.ru.login,
  phone: activeCourierKeyset.ru.phone,
};

const shouldMatchRouteToCourier = (
  route: string | RegExp,
  courier: string,
): Cypress.Chainable<JQuery<HTMLElement>> => {
  cy.contains(selectors.content.dashboard.couriers.table.routeName, route)
    .closest(selectors.content.dashboard.couriers.table.row)
    .as('routeRow')
    .find(selectors.content.dashboard.couriers.table.courierNameContainer)
    .invoke('text')
    .then(routeRowCourier => {
      if (routeRowCourier) {
        expect(routeRowCourier).to.be.equal(courier);
      } else {
        cy.get('@routeRow')
          .prevAll(selectors.content.dashboard.couriers.table.row)
          .find(`${selectors.content.dashboard.couriers.table.courierNameContainer}:not(:empty)`)
          .last()
          .should('have.text', courier);
      }
    });
  return cy.get('@routeRow');
};

context('Dashboard courier', () => {
  const initialCourier = courierNumberRecord.gumba;
  const initialCourierRegex = new RegExp(`\\b${initialCourier}\\b`);
  const initialCourierName = courierNameRecord.gumba;
  const chosen = courierNumberRecord.kypa;
  const chosenRegex = new RegExp(`\\b${chosen}\\b`);
  const chosenName = courierNameRecord.kypa;
  const testingRoute = routeNumberRecord.TODAY;
  const testingRouteRegex = new RegExp(`^${testingRoute}$`);

  before(() => {
    cy.preserveCookies();
    cy.yandexLogin('manager');
    cy.get(selectors.sidebar.menu.monitoringGroup).click();
    cy.get(selectors.sidebar.menu.dashboard).click();
  });

  it('should be reachable from dashboard', function () {
    shouldMatchRouteToCourier(testingRouteRegex, initialCourierName)
      .find(selectors.content.dashboard.couriers.table.routeName)
      .click();

    cy.get(selectors.content.courier.name).should('have.text', initialCourierName);

    cy.get(selectors.content.courier.route.changeRouteDropdown.button)
      .should('have.text', changeRouteDropdown.button)
      .should('be.visible');
    cy.get(selectors.content.courier.route.table).should('be.visible');
    cy.get(selectors.content.courier.route.map.common.map).should('be.visible');
  });

  it('should display route settings menu', function () {
    cy.get(selectors.content.courier.route.changeRouteDropdown.button).click();
    cy.get(selectors.content.courier.route.changeRouteDropdown.options.changeCourierOnCourier)
      .should('have.text', changeRouteDropdown.options.changeCourierOnCourier)
      .should('be.visible');
    cy.get(selectors.content.courier.route.changeRouteDropdown.options.moveOrders)
      .should('have.text', changeRouteDropdown.options.moveOrders)
      .should('be.visible');
    cy.get(selectors.content.courier.route.changeRouteDropdown.options.changeCourierOnTracker)
      .should('have.text', changeRouteDropdown.options.changeCourierOnTracker)
      .should('be.visible');
    cy.get(selectors.content.courier.route.changeRouteDropdown.options.removeRoute)
      .should('have.text', changeRouteDropdown.options.removeRoute)
      .should('be.visible');
  });

  context('Route transfer dialog', () => {
    before(() => {
      cy.get(
        selectors.content.courier.route.changeRouteDropdown.options.changeCourierOnCourier,
      ).click();
    });

    it('should contain title, search, table with entries, actions', () => {
      cy.get(selectors.modal.changeRouteCourier.title)
        .should('have.text', routeTransferDialog.title)
        .and('be.visible');
      cy.get(selectors.modal.changeRouteCourier.searchInput).should('be.visible');
      cy.get(selectors.modal.changeRouteCourier.cancelButton)
        .should('be.visible')
        .and('have.text', routeTransferDialog.cancelButton);
      cy.get(selectors.modal.changeRouteCourier.addNewCourierButton)
        .should('have.text', routeTransferDialog.addNewCourierButton)
        .and('be.visible');
      cy.get(selectors.modal.changeRouteCourier.submitButton)
        .should('have.text', routeTransferDialog.submitButton)
        .and('be.visible')
        .and('be.disabled');

      cy.get(selectors.modal.changeRouteCourier.couriersTable.headCell)
        .should('be.visible')
        .and('have.length', 2);

      cy.get(selectors.modal.changeRouteCourier.couriersTable.loginHeadCell)
        .should('be.visible')
        .and('have.text', dialogTableColumns.login);
      cy.get(selectors.modal.changeRouteCourier.couriersTable.phoneHeadCell)
        .should('be.visible')
        .and('have.text', dialogTableColumns.phone);

      cy.get(selectors.modal.changeRouteCourier.couriersTable.checkedRadio).should('not.exist');
    });
  });

  context('Search result', () => {
    afterEach(() => {
      cy.get(selectors.modal.changeRouteCourier.searchFieldClearIcon).click();
      cy.get(selectors.modal.changeRouteCourier.couriersTable.loginCells).should(
        'have.length',
        sizeWithoutCurrentCourier,
      );
    });

    // @see https://testpalm.yandex-team.ru/testcase/courier-522

    it('should contain filtered table on search input (login)', () => {
      cy.get(selectors.modal.changeRouteCourier.searchInput).type(courierNumberRecord.kypa);
      cy.contains(
        selectors.modal.changeRouteCourier.couriersTable.loginCells,
        new RegExp(`^${courierNumberRecord.kypa}$`),
      )
        .should('be.visible')
        .and('have.text', courierNumberRecord.kypa);
    });

    it('should contain filtered table on search input (login number substring)', () => {
      const search = courierNumberRecord.john.substr(0, 3);
      cy.get(selectors.modal.changeRouteCourier.searchInput).type(search);
      cy.contains(
        selectors.modal.changeRouteCourier.couriersTable.loginCells,
        new RegExp(`^${courierNumberRecord.john}$`),
      )
        .should('be.visible')
        .and('have.text', courierNumberRecord.john);
    });

    it('should contain filtered table on search input (login letter substring)', () => {
      const search = courierNumberRecord.john.substr(courierNumberRecord.john.length - 3);
      cy.get(selectors.modal.changeRouteCourier.searchInput).type(search);
      cy.contains(
        selectors.modal.changeRouteCourier.couriersTable.loginCells,
        new RegExp(`^${courierNumberRecord.john}$`),
      )
        .should('be.visible')
        .and('have.text', courierNumberRecord.john);
    });

    it('should show all couriers on number search (phone search)', () => {
      cy.get(selectors.modal.changeRouteCourier.searchInput).type('0002');
      cy.get(selectors.modal.changeRouteCourier.couriersTable.loginCells)
        .contains(new RegExp(`^${courierNumberRecord.kypa}$`))
        .should('be.visible');
    });

    // @see https://testpalm.yandex-team.ru/testcase/courier-521

    it('should contain filtered table on search input(full name)', () => {
      cy.get(selectors.modal.changeRouteCourier.searchInput).type(courierNameRecord.kypa);
      cy.get(selectors.modal.changeRouteCourier.couriersTable.loginCells).should('have.length', 1);
      cy.contains(
        selectors.modal.changeRouteCourier.couriersTable.loginCells,
        new RegExp(`^${courierNumberRecord.kypa}$`),
      )
        .should('be.visible')
        .and('have.text', courierNumberRecord.kypa);
    });

    it('should contain filtered table on search input(name substring)', () => {
      cy.get(selectors.modal.changeRouteCourier.searchInput).type(
        courierNameRecord.kypa.substr(0, 2),
      );
      cy.get(selectors.modal.changeRouteCourier.couriersTable.loginCells).should('have.length', 1);
      cy.contains(
        selectors.modal.changeRouteCourier.couriersTable.loginCells,
        new RegExp(`^${courierNumberRecord.kypa}$`),
      )
        .should('be.visible')
        .and('have.text', courierNumberRecord.kypa);
    });

    it('should handle nonexistent courier', () => {
      cy.get(selectors.modal.changeRouteCourier.searchInput).type('несуществующий курьер');
      cy.get(selectors.modal.changeRouteCourier.couriersTable.loginCells).should('have.length', 0);
      cy.get(selectors.modal.changeRouteCourier.couriersTable.noDataTitle)
        .should('have.text', routeTransferDialog.noDataTitle)
        .and('be.visible');
      cy.get(selectors.modal.changeRouteCourier.addNewCourierButton)
        .should('have.text', routeTransferDialog.addNewCourierButton)
        .and('be.visible');
      cy.get(selectors.modal.changeRouteCourier.submitButton)
        .should('have.text', routeTransferDialog.submitButton)
        .and('be.visible')
        .and('be.disabled');
    });

    it('should show all couriers on number search', () => {
      cy.get(selectors.modal.changeRouteCourier.searchInput).type('2');
      cy.get(selectors.modal.changeRouteCourier.couriersTable.loginCells)
        .contains(new RegExp(`^${courierNumberRecord.kypa}$`))
        .should('be.visible');
      cy.get(selectors.modal.changeRouteCourier.couriersTable.loginCells)
        .contains(new RegExp(`^${courierNumberRecord.courierForRemove}$`))
        .should('be.visible');
    });

    it('should contain filtered table on search input(uppercase name)', () => {
      cy.get(selectors.modal.changeRouteCourier.searchInput).type(
        courierNameRecord.kypa.toUpperCase(),
      );
      cy.get(selectors.modal.changeRouteCourier.couriersTable.loginCells).should('have.length', 1);
      cy.get(selectors.modal.changeRouteCourier.couriersTable.loginCells)
        .contains(new RegExp(`^${courierNumberRecord.kypa}$`))
        .should('be.visible');
    });

    it('should contain filtered table on search input(english name)', () => {
      cy.get(selectors.modal.changeRouteCourier.searchInput).type(courierNameRecord.john);
      cy.get(selectors.modal.changeRouteCourier.couriersTable.loginCells).should('have.length', 1);
      cy.get(selectors.modal.changeRouteCourier.couriersTable.loginCells)
        .contains(new RegExp(`^${courierNumberRecord.john}$`))
        .should('be.visible');
    });
  });

  context('Transfer route', () => {
    it('should display 1 checked login on courier select', () => {
      cy.contains(selectors.modal.changeRouteCourier.couriersTable.loginCells, chosenRegex)
        .as('chosenCourier')
        .click();
      cy.get('@chosenCourier')
        .find(selectors.modal.changeRouteCourier.couriersTable.checkedRadio)
        .should('be.visible');
      cy.get(selectors.modal.changeRouteCourier.couriersTable.checkedRadio)
        .should('be.visible')
        .and('have.length', 1);
    });
  });

  context('Route has been transferred', () => {
    before(() => {
      cy.get(selectors.modal.changeRouteCourier.submitButton).click();
    });

    it('should stay on courier page and display other route', () => {
      cy.get(selectors.content.courier.name).should('have.text', initialCourierName);
      cy.get(selectors.modal.changeRouteCourier.root).should('not.exist');
      cy.get(selectors.content.courier.route.selector.routeButton).should('exist');
    });
  });

  context('Courier who has received the route', () => {
    before(() => {
      cy.get(selectors.sidebar.menu.dashboard).click();
    });

    it('should display transferred route', () => {
      // checks if there are no duplicates of testingRoute
      cy.get(selectors.content.dashboard.couriers.table.routeName).then($els => {
        const matches = filter($els, $el => testingRouteRegex.test($el.textContent ?? ''));
        expect(matches).have.length(1);
      });

      shouldMatchRouteToCourier(testingRouteRegex, chosenName);
    });

    // changes rollback
    after(() => {
      cy.get(selectors.content.dashboard.couriers.table.routeName)
        .contains(testingRouteRegex)
        .click();
      cy.get(selectors.content.courier.route.changeRouteDropdown.button).click();
      cy.get(
        selectors.content.courier.route.changeRouteDropdown.options.changeCourierOnCourier,
      ).click();
      cy.contains(
        selectors.modal.changeRouteCourier.couriersTable.loginCells,
        initialCourierRegex,
      ).click();
      cy.get(selectors.modal.changeRouteCourier.submitButton).click();
    });
  });
});
