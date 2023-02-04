import * as managersSettingsKeyset from '../../../../../src/translations/managers-settings';
import * as commonKeyset from '../../../../../src/translations/common';
import * as courierQualityReportKeyset from '../../../../../src/translations/courier-quality-report';
import * as courierRouteKeyset from '../../../../../src/translations/courier-route';
import * as sidebarKeyset from '../../../../../src/translations/sidebar';
import * as importRoutesKeyset from '../../../../../src/translations/import-routes';
import * as courierMapKeyset from '../../../../../src/translations/courier-map';
import * as importKeyset from '../../../../../src/translations/import';
import selectors from '../../../../src/constants/selectors';
import { stubCourierRouteCoordinates } from '../../shared/courier-route-coordinates.spec';

const menuItems = {
  map: importKeyset.ru.tabTitles_map,
  orders: courierMapKeyset.ru.tableColumn_ordersCount,
  couriers: importRoutesKeyset.ru.tabTitles_vehicles,
  couriersSettings: sidebarKeyset.ru.couriersSettings,
  courierQualityReport: sidebarKeyset.ru.courierQualityReport,
};

const tableColumns = {
  number: courierRouteKeyset.ru.orderTable_column_number,
  status: courierQualityReportKeyset.ru.column_orderStatus,
  time_window: courierRouteKeyset.ru.orderTable_column_timeWindow,
  arrival_time: courierRouteKeyset.ru.orderTable_column_arrivalTime,
  failed_time_window: commonKeyset.ru.time_late,
  service_duration_s: courierRouteKeyset.ru.orderTable_column_serviceDurationS,
  shared_service_duration_s: courierRouteKeyset.ru.orderTable_column_sharedServiceDurationS,
  customer_name: commonKeyset.ru.client,
  address: courierQualityReportKeyset.ru.column_orderAddress,
  comments: courierQualityReportKeyset.ru.column_orderComments,
};

const couriersSettingsTitle = importRoutesKeyset.ru.tabTitles_vehicles;

// @see https://testpalm.yandex-team.ru/courier/testcases/607
context(managersSettingsKeyset.en.role_dispatcher, function () {
  before(function () {
    cy.yandexLogin('dispatcher');
  });

  stubCourierRouteCoordinates();

  it('should see dashboard', () => {
    cy.get(selectors.content.dashboard.view).should('exist');
    cy.get(selectors.content.dashboard.couriers.table.view).should('exist');
    cy.get(selectors.content.dashboard.orders.chart.view).should('exist');
    cy.get(selectors.content.dashboard.dayTotalRoutesNumber).should('exist');
    cy.get(selectors.content.dashboard.dayOrderNumber).should('exist');

    cy.get(selectors.content.dashboard.importRoutes).should('not.exist');
    cy.get(selectors.content.dashboard.moreOptionsDropdown.button).should('not.exist');
  });

  it('should see order info popup', () => {
    cy.get(selectors.content.dashboard.couriers.table.orders.sequenced).first().click();

    cy.get(selectors.modal.orderPopup.view).should('exist');
    cy.get(selectors.modal.orderPopup.map.container).should('exist');
    cy.get(selectors.modal.orderPopup.editButton).should('not.exist');
    cy.get(selectors.modal.orderPopup.removeButton).should('not.exist');
  });

  it('should see courier profile', () => {
    cy.get(selectors.modal.orderPopup.closeButton).click();

    cy.get(selectors.content.dashboard.couriers.table.courierNameContainer).first().click();

    cy.get(selectors.content.couriers.singleCourier.view).should('exist');
    cy.get(selectors.content.courier.name).should('exist');
    cy.get(selectors.content.courier.date.input).should('exist');
    cy.get(selectors.content.couriers.singleCourier.courierDetails.loginValue).should('exist');
    cy.get(selectors.content.couriers.singleCourier.courierDetails.phoneValue).should('exist');
    cy.get(selectors.content.courier.details.smsTumbler)
      .should('exist')
      .find('input')
      .should('be.disabled');

    cy.get(selectors.content.courier.route.map.common.map).should('exist');
    cy.get(selectors.content.courier.route.map.yandex.courierPosition).should('exist');
    cy.get(selectors.content.courier.route.map.yandex.activeOrder).should('exist');
    cy.get(selectors.content.courier.route.map.yandex.trackLayer).should('exist');
    cy.get(selectors.content.courier.route.map.common.details.root).should('exist');
    cy.get(selectors.content.courier.route.map.common.slider).should('exist');

    cy.get(selectors.content.courier.route.table).should('exist');
    cy.get(selectors.content.courier.route.selector.routeButton).should('exist');
    cy.get(selectors.content.courier.route.selector.activeRouteButton).should('exist');
    cy.get(selectors.content.courier.route.tableHeader('index_false'))
      .should('exist')
      .and('have.text', courierRouteKeyset.ru.orderTable_column_index);
    cy.get(selectors.content.courier.route.tableHeader('number'))
      .should('exist')
      .and('have.text', tableColumns.number);
    cy.get(selectors.content.courier.route.tableHeader('status'))
      .should('exist')
      .and('have.text', tableColumns.status);
    cy.get(selectors.content.courier.route.tableHeader('time_window'))
      .should('exist')
      .and('have.text', tableColumns.time_window);
    cy.get(selectors.content.courier.route.tableHeader('arrival_time'))
      .should('exist')
      .and('have.text', tableColumns.arrival_time);
    cy.get(selectors.content.courier.route.tableHeader('failed_time_window'))
      .should('exist')
      .and('have.text', tableColumns.failed_time_window);
    cy.get(selectors.content.courier.route.tableHeader('service_duration_s'))
      .should('exist')
      .and('have.text', tableColumns.service_duration_s);
    cy.get(selectors.content.courier.route.tableHeader('shared_service_duration_s'))
      .should('exist')
      .and('have.text', tableColumns.shared_service_duration_s);
    cy.get(selectors.content.courier.route.tableHeader('customer_name'))
      .should('exist')
      .and('have.text', tableColumns.customer_name);
    cy.get(selectors.content.courier.route.tableHeader('address'))
      .should('exist')
      .and('have.text', tableColumns.address);
    cy.get(selectors.content.courier.route.tableHeader('comments'))
      .should('exist')
      .and('have.text', tableColumns.comments);

    cy.get(selectors.content.courier.addOrderButton).should('not.exist');
    cy.get(selectors.content.courier.changeOrderButton).should('not.exist');
    cy.get(selectors.content.courier.optimizeRouteButton).should('not.exist');
  });

  it('should see monitoring submenu', () => {
    cy.get(selectors.sidebar.menu.monitoringGroup).click();

    cy.get(selectors.sidebar.menu.dashboard).should('exist');
    cy.get(selectors.sidebar.menu.map).should('exist');
    cy.get(selectors.sidebar.menu.orders).should('exist');
    cy.get(selectors.sidebar.menu.couriers).should('exist');
    cy.get(selectors.sidebar.menu.courierNameMenuItem).should('exist');
  });

  it('should see map section', () => {
    cy.get(selectors.sidebar.menu.map).click();

    cy.get(selectors.content.map.view).should('exist');
    cy.get(selectors.sidebar.selectedItem).should('have.text', menuItems.map);
  });

  it('should see orders section', () => {
    cy.get(selectors.sidebar.menu.orders).click();

    cy.get(selectors.content.orders.view).should('exist');
    cy.get(selectors.sidebar.selectedItem).should('have.text', menuItems.orders);
    cy.get(selectors.content.orders.table).should('exist');
    cy.get(selectors.content.orders.tableRows).should('exist');
    cy.get(selectors.content.orders.downloadBtn).should('exist');
  });

  it('should see message when no orders available', () => {
    cy.get(selectors.content.orders.filter.input).type('987');

    cy.get(selectors.content.orders.message).should('exist');
    cy.get(selectors.content.orders.downloadBtn).find('button').should('be.disabled');
  });

  it('should see couriers section', () => {
    cy.get(selectors.sidebar.menu.couriers).click();

    cy.get(selectors.sidebar.selectedItem).should('have.text', menuItems.couriers);
    cy.get(selectors.content.couriers.list).should('exist');
    cy.get(selectors.content.couriers.search.input).should('exist');
    cy.get(selectors.content.couriers.search.lineSms).should('be.disabled');
    cy.get(selectors.content.couriers.search.lineRemove).should('not.exist');
    cy.get(selectors.content.couriers.newCourier.title).should('not.exist');
    cy.get(selectors.content.couriers.newCourier.number).should('not.exist');
    cy.get(selectors.content.couriers.newCourier.submitButton).should('not.exist');
  });

  it('should see couriers work quality report', () => {
    cy.get(selectors.sidebar.menu.reports).click();
    cy.get(selectors.sidebar.menu.reportsItems.courierQualityReport).click();

    cy.get(selectors.sidebar.menu.reportsItems.planFact).should('not.exist');
    cy.get(selectors.sidebar.selectedItem).should('have.text', menuItems.courierQualityReport);
    cy.get(selectors.courierQualityReport.table).should('exist');
    cy.get(selectors.courierQualityReport.tableRow).should('exist');
    cy.get(selectors.courierQualityReport.searchField).should('exist');
    cy.get(selectors.courierQualityReport.datePickerStart).should('exist');
    cy.get(selectors.courierQualityReport.datePickerEnd).should('exist');
    cy.get(selectors.courierQualityReport.downloadXLSXButton)
      .find('button')
      .should('not.be.disabled');
  });

  it('should see mobile app settings section', () => {
    cy.get(selectors.sidebar.menu.settingsGroup).click();
    cy.get(selectors.sidebar.menu.couriersSettings).click();

    cy.get(selectors.sidebar.selectedItem).should('have.text', menuItems.couriersSettings);
    cy.get(selectors.users.title).should('have.text', couriersSettingsTitle);
    cy.get(selectors.users.search).should('exist');
    cy.get(selectors.users.user).should('exist');

    cy.get(selectors.users.addForm).should('not.exist');
    cy.get(selectors.users.addBtn).should('not.exist');
    cy.get(selectors.users.removeBtn).should('not.exist');
  });

  it('should have non-clickable tariff menu item', () => {
    cy.url().then(url => {
      cy.get(selectors.sidebar.tariffInfo).click().wait(1000);
      cy.url().should('eq', url);
    });
  });
});
