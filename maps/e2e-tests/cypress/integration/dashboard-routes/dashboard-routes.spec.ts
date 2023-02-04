import * as commonKeyset from '../../../../src/translations/common';
import * as courierRouteKeyset from '../../../../src/translations/courier-route';
import * as dashboardKeyset from '../../../../src/translations/dashboard';
import selectors from '../../../src/constants/selectors';
import { times } from 'lodash';

// @see https://testpalm.yandex-team.ru/courier/testcases/539
describe('Routes editing from dashboard', () => {
  const expectElementInViewport = (selector: string): void => {
    cy.get(selector).should('be.visible');
    cy.get(selector).then($els => {
      cy.window().then(window => {
        const $el = $els[0];
        expect(
          $el.clientTop >= window.pageYOffset &&
            $el.clientLeft >= window.pageXOffset &&
            $el.clientTop + $el.clientHeight <= window.pageYOffset + window.innerHeight &&
            $el.clientLeft + $el.clientWidth <= window.pageXOffset + window.innerWidth,
          `selector ${selector} should be in viewport`,
        ).to.eq(true);
      });
    });
  };

  before(() => {
    cy.yandexLogin('manager');

    cy.get(selectors.sidebar.menu.monitoringGroup).click();
    cy.get(selectors.content.dashboard.moreOptionsDropdown.button).click();
    cy.get(selectors.content.dashboard.moreOptionsDropdown.options.createRoute).click();
  });

  it('should not be able to add route before filling fields', () => {
    cy.get(selectors.modal.createRoute.createRouteSubmit).should('be.disabled');
  });

  it('should be able to add route after filling required fields', () => {
    cy.get(selectors.modal.createRoute.nameField).find('input').invoke('val').as('routeName');

    cy.get(selectors.modal.createRoute.depotField)
      .find(selectors.modal.createRoute.fieldInput)
      .click();

    cy.get(selectors.modal.createRoute.depotField).find(selectors.select.option).first().click();

    cy.get(selectors.modal.createRoute.courierField)
      .find(selectors.modal.createRoute.fieldInput)
      .click();

    cy.get(selectors.modal.createRoute.courierField).find(selectors.select.option).first().click();

    cy.get(selectors.modal.createRoute.createRouteSubmit)
      .should('be.not.disabled')
      .click()
      .wait(500);
  });

  it('should show dashboard with added route', function () {
    cy.get(selectors.content.dashboard.view).should('exist');
    cy.get(selectors.content.dashboard.couriers.table.row).contains(this.routeName).should('exist');
  });

  it('should not have orders', function () {
    cy.contains(this.routeName)
      .parent(selectors.content.dashboard.couriers.table.row)
      .within(() => {
        cy.get(selectors.content.dashboard.couriers.table.orders.anyOrder).should('not.exist');
      });
  });

  it('should contain option to add order in route row dropdown', function () {
    cy.contains(this.routeName)
      .parent(selectors.content.dashboard.couriers.table.row)
      .within(() => {
        cy.get(selectors.content.dashboard.couriers.table.orders.menu).click();
        cy.get(selectors.content.dashboard.couriers.table.orders.menuOption)
          .should('have.lengthOf', 1)
          .first()
          .should('have.text', dashboardKeyset.ru.addOrder)
          .click();
      });
  });

  it('should open add order form on click dropdown option', () => {
    cy.get(selectors.modal.orderPopup.view).should('exist');
  });

  it('show order info after order create', () => {
    cy.get(selectors.suggest.input).type('москва');
    cy.get(selectors.suggest.listOptions).first().click();
    cy.get(selectors.modal.orderPopup.createButton).click();

    cy.get(selectors.modal.orderPopup.loader).should('exist');
    cy.get(selectors.modal.orderPopup.loader).should('not.exist');
    cy.get(selectors.modal.orderPopup.editTitle).should('not.exist');
    cy.get(selectors.modal.orderPopup.editFooter).should('not.exist');
    cy.get(selectors.modal.orderPopup.view).should('exist');
  });

  it('should show added order on dashboard', function () {
    cy.get(selectors.modal.orderPopup.closeButton).click();

    cy.get(selectors.modal.orderPopup.view).should('not.exist');
    cy.get(selectors.content.dashboard.view).should('exist');
    cy.contains(this.routeName)
      .parent(selectors.content.dashboard.couriers.table.row)
      .should('exist')
      .within(() => {
        cy.get(selectors.content.dashboard.couriers.table.orders.anyOrder)
          .should('exist')
          .and('have.length', 1);
      });
  });

  it('should still contain one option to add order in route row dropdown', function () {
    cy.contains(this.routeName)
      .parent(selectors.content.dashboard.couriers.table.row)
      .within(() => {
        cy.get(selectors.content.dashboard.couriers.table.orders.menu).click();
        cy.get(selectors.content.dashboard.couriers.table.orders.menuOption)
          .should('have.lengthOf', 1)
          .first()
          .should('have.text', dashboardKeyset.ru.addOrder)
          .click();
      });
  });

  it('should add second order to route', function () {
    cy.get(selectors.suggest.input).type('москва');
    cy.get(selectors.suggest.listOptions).first().click();
    cy.get(selectors.modal.orderPopup.createButton).click();

    cy.get(selectors.modal.orderPopup.loader).should('not.exist');
    cy.get(selectors.modal.orderPopup.closeButton).click();

    cy.get(selectors.modal.orderPopup.view).should('not.exist');
    cy.get(selectors.content.dashboard.view).should('exist');
    cy.contains(this.routeName)
      .parent(selectors.content.dashboard.couriers.table.row)
      .should('exist')
      .within(() => {
        cy.get(selectors.content.dashboard.couriers.table.orders.anyOrder)
          .should('exist')
          .and('have.length', 2);
      });
  });

  times(2, n => {
    it('should contain dropdown option to change orders when two or more route orders exists', function () {
      cy.contains(this.routeName)
        .parent(selectors.content.dashboard.couriers.table.row)
        .within(() => {
          cy.get(selectors.content.dashboard.couriers.table.orders.menu).click();
          cy.get(selectors.content.dashboard.couriers.table.orders.menuOption)
            .should('have.lengthOf', 2)
            .first()
            .should('have.text', dashboardKeyset.ru.addOrder);

          cy.get(selectors.content.dashboard.couriers.table.orders.menuOption)
            .last()
            .should('have.text', dashboardKeyset.ru.changeRouteOrdersSequence)
            .click();
        });
    });

    it('should open route page after change orders option click', () => {
      if (n === 0) {
        // it loads very fast the second time, so skip loader check
        cy.get(selectors.content.courier.loader).should('exist');
        cy.get(selectors.content.courier.loader).should('not.exist');
      }

      cy.get(selectors.sidebar.menu.courierNameMenuItem).should('be.visible');
      cy.get(selectors.content.courier.name).should('be.visible');
      cy.get(selectors.content.courier.route.map.common.container).should('be.visible');
      cy.get(selectors.content.courier.route.map.yandex.placemark).should('have.length', 4); // two orders and depot

      expectElementInViewport(selectors.content.courier.route.table);
      cy.get(selectors.content.courier.route.tableActions)
        .find('button')
        .contains(courierRouteKeyset.ru.changeOrdersSequence_save)
        .should('be.visible');
      cy.get(selectors.content.courier.route.tableActions)
        .find('button')
        .contains(commonKeyset.ru.title_cancel)
        .should('be.visible');
    });

    it('should not show courier in sidebar when navigating to dashboard', () => {
      cy.get(selectors.sidebar.menu.dashboard).click();

      cy.get(selectors.content.dashboard.view).should('exist');
      cy.get(selectors.content.dashboard.couriers.table.view).should('exist');
      cy.get(selectors.sidebar.menu.courierNameMenuItem).should('not.exist');
    });
  });

  after(function () {
    cy.get(selectors.content.dashboard.couriers.table.routeName).contains(this.routeName).click();
    cy.get(selectors.content.courier.route.changeRouteDropdown.button).click();
    cy.get(selectors.content.courier.route.changeRouteDropdown.options.removeRoute).click();
    cy.get(selectors.modal.dialog.submit).click();
  });
});
