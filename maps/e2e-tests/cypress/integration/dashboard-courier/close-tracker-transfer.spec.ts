import * as commonKeyset from '../../../../src/translations/common';
import * as couriersKeyset from '../../../../src/translations/couriers';
import * as courierRouteKeyset from '../../../../src/translations/courier-route';
import selectors from '../../../src/constants/selectors';
import { courierNameRecord } from '../../../src/constants/couriers';
import dateFnsFormat from '../../../src/utils/date-fns-format';

// @see https://testpalm.yandex-team.ru/testcase/courier-494

const today = dateFnsFormat(new Date(), 'dd MMM yyyy').replace('.', '');

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
  title: courierRouteKeyset.ru.changeCourierDropdown_tracker,
  numberOfTracker: couriersKeyset.ru.title_trackerNumber_label,
  helpLink: couriersKeyset.ru.title_trackerNumber_hint,
  cancelButton: commonKeyset.ru.title_cancel,
  submitButton: couriersKeyset.ru.title_send_route,
};

// https://testpalm.yandex-team.ru/testcase/courier-494

context('Dashboard courier', () => {
  before(() => {
    cy.preserveCookies();
    cy.yandexLogin('manager');
    cy.get(selectors.sidebar.menu.monitoringGroup).click();
    cy.get(selectors.sidebar.menu.dashboard).click();
  });

  it('should be accessible from dashboard', function () {
    cy.get(selectors.content.dashboard.couriers.table.courierNames)
      .contains(courierNameRecord.gumba)
      .click({ scrollBehavior: false });
    cy.get(selectors.content.courier.route.changeRouteDropdown.button)
      .should('have.text', changeRouteDropdown.button)
      .should('be.visible');
    cy.get(selectors.content.courier.route.table).should('be.visible');
  });

  context('Transfer on tracker dialog', () => {
    before(() => {
      cy.get(selectors.content.courier.route.selector.activeRouteButton)
        .invoke('text')
        .as('initialRoute');
    });

    it('should be opened after selecting in dropdown', () => {
      cy.get(selectors.content.courier.route.changeRouteDropdown.button).click({
        scrollBehavior: false,
      });
      cy.get(selectors.content.courier.route.changeRouteDropdown.options.changeCourierOnTracker)
        .should('have.text', changeRouteDropdown.options.changeCourierOnTracker)
        .should('be.visible');

      cy.get(
        selectors.content.courier.route.changeRouteDropdown.options.changeCourierOnTracker,
      ).click({ scrollBehavior: false });
      cy.get(selectors.modal.transferOnTracker.root).should('be.visible');
    });

    it('should contain title, input, actions', () => {
      cy.get(selectors.modal.transferOnTracker.root).should('be.visible').as('dialog');
      cy.get('@dialog')
        .find(selectors.modal.transferOnTracker.title)
        .should('be.visible')
        .and('have.text', routeTransferDialog.title);

      cy.get('@dialog')
        .find(selectors.modal.transferOnTracker.trackerNumberInput)
        .should('be.visible')
        .invoke('attr', 'id')
        .then(id => {
          cy.get('@dialog')
            .find(`label[for="${id}"]`)
            .should('have.text', routeTransferDialog.numberOfTracker);
        });

      cy.get('@dialog')
        .find(selectors.modal.transferOnTracker.submitButton)
        .should('be.visible')
        .and('be.disabled')
        .and('have.text', routeTransferDialog.submitButton);
      cy.get('@dialog')
        .find(selectors.modal.transferOnTracker.helpLink)
        .should('be.visible')
        .and('have.text', routeTransferDialog.helpLink);
      cy.get('@dialog')
        .find(selectors.modal.transferOnTracker.cancelButton)
        .should('be.visible')
        .and('have.text', routeTransferDialog.cancelButton);
    });

    it('should be closed after "cancel" button click', function () {
      cy.get(selectors.modal.transferOnTracker.root).should('be.visible').as('dialog');
      cy.get('@dialog')
        .find(selectors.modal.transferOnTracker.cancelButton)
        .click({ scrollBehavior: false });
      cy.get(selectors.modal.transferOnTracker.root).should('not.exist');

      cy.get(selectors.content.courier.name)
        .should('be.visible')
        .and('have.text', courierNameRecord.gumba);

      cy.get(selectors.content.courier.date.input).should('have.text', today);
      cy.get(selectors.content.courier.route.selector.activeRouteButton).should(
        'have.text',
        this.initialRoute,
      );
    });

    it('should be opened after selecting in dropdown', () => {
      cy.get(selectors.content.courier.route.changeRouteDropdown.button).click({
        scrollBehavior: false,
      });
      cy.get(selectors.content.courier.route.changeRouteDropdown.options.changeCourierOnTracker)
        .should('have.text', changeRouteDropdown.options.changeCourierOnTracker)
        .should('be.visible');

      cy.get(
        selectors.content.courier.route.changeRouteDropdown.options.changeCourierOnTracker,
      ).click({ scrollBehavior: false });
      cy.get(selectors.modal.transferOnTracker.root).should('be.visible');
    });

    it('should be closed after close cross click', function () {
      cy.get(selectors.modal.transferOnTracker.root).should('be.visible').as('dialog');
      cy.get('@dialog')
        .find(selectors.modal.transferOnTracker.closeIcon)
        .click({ scrollBehavior: false });
      cy.get(selectors.modal.transferOnTracker.root).should('not.exist');

      cy.get(selectors.content.courier.name)
        .should('be.visible')
        .and('have.text', courierNameRecord.gumba);

      cy.get(selectors.content.courier.date.input).should('have.text', today);
      cy.get(selectors.content.courier.route.selector.activeRouteButton).should(
        'have.text',
        this.initialRoute,
      );
    });

    it('should be opened after selecting in dropdown', () => {
      cy.get(selectors.content.courier.route.changeRouteDropdown.button).click({
        scrollBehavior: false,
      });
      cy.get(selectors.content.courier.route.changeRouteDropdown.options.changeCourierOnTracker)
        .should('have.text', changeRouteDropdown.options.changeCourierOnTracker)
        .should('be.visible');

      cy.get(
        selectors.content.courier.route.changeRouteDropdown.options.changeCourierOnTracker,
      ).click({ scrollBehavior: false });
      cy.get(selectors.modal.transferOnTracker.root).should('be.visible');
    });

    it('should be closed after click outside', function () {
      cy.get('body').click(0, 0, { scrollBehavior: false });
      cy.get(selectors.modal.transferOnTracker.root).should('not.exist');

      cy.get(selectors.content.courier.name)
        .should('be.visible')
        .and('have.text', courierNameRecord.gumba);

      cy.get(selectors.content.courier.date.input).should('have.text', today);
      cy.get(selectors.content.courier.route.selector.activeRouteButton).should(
        'have.text',
        this.initialRoute,
      );
    });
  });
});
