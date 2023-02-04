import * as couriersKeyset from '../../../../src/translations/couriers';
import * as activeCourierKeyset from '../../../../src/translations/active-courier';
import * as courierDetailsKeyset from '../../../../src/translations/courier-details';
import * as courierRouteKeyset from '../../../../src/translations/courier-route';
import selectors from '../../../src/constants/selectors';
import { courierNameRecord, courierNumberRecord } from '../../../src/constants/couriers';
import { routeNumberRecord } from '../../../src/constants/routes';

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

// @see https://testpalm.yandex-team.ru/testcase/courier-523
context('Dashboard courier', () => {
  const initialCourier = courierNumberRecord.kypa;
  const initialCourierRegex = new RegExp(`\\b${initialCourier}\\b`);
  const initialCourierName = courierNameRecord.kypa;
  const firstChosen = courierNumberRecord.courierForSearch;
  const firstChosenRegex = new RegExp(`\\b${firstChosen}\\b`);
  const secondChosen = courierNumberRecord.gumba;
  const secondChosenRegex = new RegExp(`\\b${secondChosen}\\b`);
  const secondChosenName = courierNameRecord.gumba;
  const testingRoute = routeNumberRecord.DIFFERENT_STATUSES;
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

    cy.get(selectors.content.courier.route.changeRouteDropdown.button).click();
  });

  context('Route transfer dialog', () => {
    before(() => {
      cy.get(
        selectors.content.courier.route.changeRouteDropdown.options.changeCourierOnCourier,
      ).click();
    });

    it('should contain title, subtitle, search, table with entries, actions', () => {
      cy.get(selectors.modal.changeRouteCourier.title)
        .should('have.text', courierRouteKeyset.ru.changeCourierDropdown_courier)
        .and('be.visible');
      cy.get(selectors.modal.changeRouteCourier.subtitle)
        .invoke('text')
        .then((text: string) => {
          const courierMatch = text.match(initialCourierRegex);
          const phone = '+7(000)0000002';
          expect(courierMatch).to.be.instanceOf(Array);
          expect(courierMatch!).have.length(1);
          expect(courierMatch![0]).equal(initialCourier);
          expect(text).to.include(phone);
        });

      cy.get(selectors.modal.changeRouteCourier.couriersTable.headCell)
        .should('be.visible')
        .and('have.length', 2);
      cy.get(selectors.modal.changeRouteCourier.couriersTable.loginHeadCell)
        .should('be.visible')
        .and('have.text', courierDetailsKeyset.ru.login);
      cy.get(selectors.modal.changeRouteCourier.couriersTable.phoneHeadCell)
        .should('be.visible')
        .and('have.text', activeCourierKeyset.ru.phone);
      cy.get(selectors.modal.changeRouteCourier.submitButton)
        .should('have.text', couriersKeyset.ru.title_send_route)
        .and('be.visible')
        .and('be.disabled');
      cy.get(selectors.modal.changeRouteCourier.couriersTable.checkedRadio).should('not.exist');
    });
    it('should display 1 checked login on courier select', () => {
      cy.contains(selectors.modal.changeRouteCourier.couriersTable.loginCells, firstChosenRegex)
        .as('firstChosenCourier')
        .click();
      cy.get('@firstChosenCourier')
        .find(selectors.modal.changeRouteCourier.couriersTable.checkedRadio)
        .should('be.visible');
      cy.get(selectors.modal.changeRouteCourier.couriersTable.checkedRadio)
        .should('be.visible')
        .and('have.length', 1);
      cy.contains(selectors.modal.changeRouteCourier.couriersTable.loginCells, secondChosenRegex)
        .as('secondChosenCourier')
        .click();
      cy.get('@secondChosenCourier')
        .find(selectors.modal.changeRouteCourier.couriersTable.checkedRadio)
        .should('be.visible');
      cy.get(selectors.modal.changeRouteCourier.couriersTable.checkedRadio)
        .should('be.visible')
        .and('have.length', 1);
      cy.get(selectors.modal.changeRouteCourier.subtitle)
        .invoke('text')
        .then((text: string) => {
          const courierMatch = text.match(secondChosenRegex);
          const phone = '+7(000)0000001';
          expect(courierMatch).to.be.instanceOf(Array);
          expect(courierMatch!).have.length(1);
          expect(courierMatch![0]).equal(secondChosen);
          expect(text).to.include(phone);
        });
    });
  });

  context('Route has been transferred', () => {
    before(() => {
      cy.get(selectors.modal.changeRouteCourier.submitButton).click();
    });

    it('should not have orders after order transfer', () => {
      cy.get(selectors.content.courier.route.noRoutes)
        .should('be.visible')
        .and('have.text', couriersKeyset.ru.title_notFoundRoute);
    });

    context('Courier who has received the route', () => {
      before(() => {
        cy.get(selectors.sidebar.menu.dashboard).click();
      });

      it('should display transferred route', () => {
        cy.get(selectors.content.dashboard.couriers.table.courierNames).should(
          'not.contain.text',
          initialCourierName,
        );

        shouldMatchRouteToCourier(testingRouteRegex, secondChosenName);
      });

      // changes rollback
      after(() => {
        cy.get(selectors.content.dashboard.couriers.table.routeName).contains(testingRoute).click();
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
});
