import * as couriersKeyset from '../../../../src/translations/couriers';
import * as dashboardOrdersKeyset from '../../../../src/translations/dashboard-orders';
import selectors from '../../../src/constants/selectors';

context('Courier page', () => {
  beforeEach(() => {
    cy.fixture('testData').then(() => {
      cy.yandexLogin('manager');
      cy.get(selectors.content.dashboard.view);
      cy.get(selectors.content.dashboard.couriers.table.todayRow).click();
    });
  });

  // @see https://testpalm.yandex-team.ru/testcase/courier-38
  context('Elements on page', function () {
    it('Courier login', function () {
      cy.fixture('testData').then(({ courierNumberRecord }) => {
        cy.get(selectors.content.couriers.singleCourier.courierDetails.loginValue)
          .invoke('text')
          .should('eq', courierNumberRecord.gumba);
      });
    });

    it('Courier phone', function () {
      cy.fixture('testData').then(() => {
        cy.get(selectors.content.couriers.singleCourier.courierDetails.phoneValue).contains(
          '+7(000)0000001',
        );
      });
    });

    it('SMS switcher', function () {
      cy.fixture('testData').then(() => {
        cy.get(selectors.content.couriers.singleCourier.courierDetails.smsTumbler.on);
      });
    });
  });

  // @see https://testpalm.yandex-team.ru/testcase/courier-120
  context('Switching a route', function () {
    beforeEach(function () {
      cy.fixture('testData').then(() => {
        return cy.get(selectors.content.couriers.singleCourier.routesList.second).click();
      });
    });

    it('The switch has switched', function () {
      cy.fixture('testData').then(() => {
        cy.get(selectors.content.couriers.singleCourier.routesList.second).should(
          'have.attr',
          'disabled',
          'disabled',
        );
      });
    });

    it('The number of orders has changed', function () {
      cy.fixture('testData').then(() => {
        cy.get(selectors.content.couriers.singleCourier.orderRow).should('have.length', 2);
      });
    });
  });

  // @see https://testpalm.yandex-team.ru/testcase/courier-71
  it('Go to the order', function () {
    cy.fixture('testData').then(() => {
      cy.get(`${selectors.content.couriers.singleCourier.orderRow}:nth-child(1)`).click();
      cy.get(selectors.modal.orderPopup.view);
      cy.get(
        `${selectors.content.couriers.singleCourier.orderRow}:nth-child(1) .react-grid-Cell:nth-child(2)`,
      )
        .invoke('text')
        .then(orderNumber => {
          cy.get(selectors.modal.orderPopup.title).contains(orderNumber);
        });
    });
  });

  // @see https://testpalm.yandex-team.ru/testcase/courier-49
  it('Sidebar courier name menu item', function () {
    cy.fixture('testData').then(({ courierNameRecord }) => {
      cy.get(selectors.datePicker.labels.yesterday).click();
      cy.get(selectors.sidebar.menu.courierNameMenuItem)
        .invoke('text')
        .should('eq', courierNameRecord.gumba);
      cy.get(selectors.sidebar.menu.monitoringGroup).click();
      cy.get(selectors.sidebar.menu.dashboard).click();
      cy.get(selectors.content.dashboard.dayOrderNumber)
        .invoke('text')
        .should('eq', `4 ${dashboardOrdersKeyset.ru.orders.some}`);
    });
  });

  // @see https://testpalm.yandex-team.ru/testcase/courier-78
  it('Switching to a day without orders', function () {
    cy.fixture('testData').then(() => {
      cy.get(selectors.datePicker.input).click();
      cy.get(selectors.datePicker.daysGrid.nextMonthButton).click();
      cy.get(selectors.datePicker.daysGrid.days.superLast).click();
      cy.get(selectors.content.couriers.singleCourier.routesList.emptyRoute)
        .invoke('text')
        .should('eq', couriersKeyset.ru.title_notFoundRoute);
    });
  });
});
