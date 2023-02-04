import * as commonKeyset from '../../../../src/translations/common';
import * as couriersKeyset from '../../../../src/translations/couriers';
import * as courierRouteKeyset from '../../../../src/translations/courier-route';
import selectors from '../../../src/constants/selectors';
import { courierNameRecord } from '../../../src/constants/couriers';

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

const login = '7776690';
const phone = '+79990002222';
const anotherPhone = '+79990001111';

// @see https://testpalm.yandex-team.ru/courier/testcases/454
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
      .click();
    cy.get(selectors.content.courier.route.changeRouteDropdown.button)
      .should('have.text', changeRouteDropdown.button)
      .should('be.visible');
  });

  context('Transfer dialog', () => {
    it('should contain heading, search, table with entries, actions', () => {
      cy.get(selectors.content.courier.route.changeRouteDropdown.button).click();
      cy.get(
        selectors.content.courier.route.changeRouteDropdown.options.changeCourierOnCourier,
      ).click();

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
      cy.get(selectors.modal.changeRouteCourier.couriersTable.loginCells).should('be.visible');
      cy.get(selectors.modal.changeRouteCourier.couriersTable.phoneCells).should('be.visible');
    });

    it('should contain new courier form after "add new courier" button click', () => {
      cy.get(selectors.modal.changeRouteCourier.addNewCourierButton).click({
        scrollBehavior: false,
      });

      cy.get(selectors.modal.changeRouteCourier.newCourierNumber).should('be.visible');
      cy.get(selectors.modal.changeRouteCourier.newCourierPhone)
        .should('be.visible')
        .and('have.attr', 'placeholder', commonKeyset.ru.phone_placeholder);
      cy.get(selectors.modal.changeRouteCourier.newCourierSubmit)
        .should('be.visible')
        .and('be.disabled');
    });

    it('should enable submit button after filling the form', () => {
      cy.get(selectors.modal.changeRouteCourier.newCourierNumber).type(login, {
        scrollBehavior: false,
      });
      cy.get(selectors.modal.changeRouteCourier.newCourierPhone).type(phone, {
        scrollBehavior: false,
      });
      cy.get(selectors.modal.changeRouteCourier.newCourierSubmit)
        .should('be.visible')
        .and('not.be.disabled');
    });

    it('should save form data on list scroll', () => {
      cy.get(selectors.modal.changeRouteCourier.scrollbar)
        .trigger('mousedown', { button: 0, scrollBehavior: false, force: true })
        .trigger('mousemove', { clientY: 1800, force: true })
        .trigger('mousedown', { button: 0, scrollBehavior: false, force: true });

      cy.get(selectors.modal.changeRouteCourier.newCourierNumber).should('not.exist');
      cy.get(selectors.modal.changeRouteCourier.newCourierPhone).should('not.exist');

      cy.get(selectors.modal.changeRouteCourier.scrollbar)
        .trigger('mousedown', { button: 0, scrollBehavior: false, force: true })
        .trigger('mousemove', { clientY: 0, force: true })
        .trigger('mousedown', { button: 0, scrollBehavior: false, force: true });

      cy.get(selectors.modal.changeRouteCourier.newCourierNumber)
        .should('be.visible')
        .and('have.value', login);
      cy.get(selectors.modal.changeRouteCourier.newCourierPhone)
        .should('be.visible')
        .and('have.value', phone);
    });

    it('should display new courier after form submit', () => {
      cy.get(selectors.modal.changeRouteCourier.newCourierSubmit).click({ scrollBehavior: false });
      cy.get(selectors.modal.changeRouteCourier.couriersTable.loginCells)
        .eq(0)
        .should('have.text', login)
        .find(selectors.modal.changeRouteCourier.couriersTable.checkedRadio)
        .should('be.visible');
      cy.get(selectors.modal.changeRouteCourier.couriersTable.phoneCells)
        .eq(0)
        .should('have.text', phone);
      cy.get(selectors.modal.changeRouteCourier.newCourierNumber).should('not.exist');
      cy.get(selectors.modal.changeRouteCourier.newCourierPhone).should('not.exist');
      cy.get(selectors.modal.changeRouteCourier.newCourierSubmit).should('not.exist');
    });

    it('should contain new courier form after "add new courier" button click', () => {
      cy.get(selectors.modal.changeRouteCourier.addNewCourierButton).click({
        scrollBehavior: false,
      });

      cy.get(selectors.modal.changeRouteCourier.newCourierNumber).should('be.visible');
      cy.get(selectors.modal.changeRouteCourier.newCourierPhone)
        .should('be.visible')
        .and('have.attr', 'placeholder', commonKeyset.ru.phone_placeholder);
      cy.get(selectors.modal.changeRouteCourier.newCourierSubmit)
        .should('be.visible')
        .and('be.disabled');
    });

    it('should be closed after clicking on table heading cell', () => {
      cy.get(selectors.modal.changeRouteCourier.couriersTable.phoneHeadCell).click({
        scrollBehavior: false,
      });
      cy.get(selectors.modal.changeRouteCourier.couriersTable.phoneHeadCellSortAscending).should(
        'be.visible',
      );
      cy.get(selectors.modal.changeRouteCourier.newCourierNumber).should('not.exist');
      cy.get(selectors.modal.changeRouteCourier.newCourierPhone).should('not.exist');
      cy.get(selectors.modal.changeRouteCourier.newCourierSubmit).should('not.exist');
    });

    it('should forbid to add with login or phone of existing courier', () => {
      cy.get(selectors.modal.changeRouteCourier.addNewCourierButton).click({
        scrollBehavior: false,
      });

      cy.get(selectors.modal.changeRouteCourier.newCourierNumber).type(login, {
        scrollBehavior: false,
      });
      cy.get(selectors.modal.changeRouteCourier.newCourierPhone).type(phone, {
        scrollBehavior: false,
      });
      cy.get(selectors.modal.changeRouteCourier.newCourierSubmit)
        .should('be.visible')
        .and('be.disabled');

      cy.get(selectors.modal.changeRouteCourier.newCourierPhone).clear({ scrollBehavior: false });
      cy.get(selectors.modal.changeRouteCourier.newCourierPhone).type(anotherPhone, {
        scrollBehavior: false,
      });
      cy.get(selectors.modal.changeRouteCourier.newCourierSubmit)
        .should('be.visible')
        .and('be.disabled');
    });
  });

  // removing created courier
  after(() => {
    cy.get('body').click(0, 0);
    const removeButtonText = couriersKeyset.ru.courierLine_removeTitle;
    const confirmRemoveButtonText = couriersKeyset.ru.removeDialog_submit;
    cy.get(selectors.sidebar.menu.couriers).click();
    cy.get(`[data-test-anchor="${login}"]`).contains('button', removeButtonText).click();
    cy.contains('button', confirmRemoveButtonText).click();
    cy.get(selectors.sidebar.menu.dashboard).click({ scrollBehavior: false });
  });
});
