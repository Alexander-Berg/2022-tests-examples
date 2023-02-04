import * as importKeyset from '../../../../src/translations/import';
import * as orderDetailsKeyset from '../../../../src/translations/order-details';
import * as expectedOrdersKeyset from '../../../../src/translations/expected-orders';
import * as createRouteFormKeyset from '../../../../src/translations/create-route-form';
import * as commonKeyset from '../../../../src/translations/common';
import * as courierQualityReportKeyset from '../../../../src/translations/courier-quality-report';
import * as courierRouteKeyset from '../../../../src/translations/courier-route';
import * as courierMapKeyset from '../../../../src/translations/courier-map';
import selectors from '../../../src/constants/selectors';
import { courierNameRecord } from '../../../src/constants/couriers';

const tableHeadings = {
  number: courierRouteKeyset.ru.orderTable_column_number,
  status: courierQualityReportKeyset.ru.column_orderStatus,
  timeWindow: courierRouteKeyset.ru.orderTable_column_timeWindow,
  arrivalTime: courierRouteKeyset.ru.orderTable_column_arrivalTime,
  failedTimeWindow: commonKeyset.ru.time_late,
  serviceDuration: courierRouteKeyset.ru.orderTable_column_serviceDurationS,
  sharedServiceDuration: courierRouteKeyset.ru.orderTable_column_sharedServiceDurationS,
  customerName: commonKeyset.ru.client,
  address: courierQualityReportKeyset.ru.column_orderAddress,
  comments: courierQualityReportKeyset.ru.column_orderComments,
};

const popupDataHeadings = {
  number: courierRouteKeyset.ru.orderTable_column_number,
  client: commonKeyset.ru.client,
  watcher: courierQualityReportKeyset.ru.column_orderSharedWithCompanies,
  status: courierQualityReportKeyset.ru.column_orderStatus,
  address: courierQualityReportKeyset.ru.column_orderAddress,
  date: createRouteFormKeyset.ru.label_date,
  shipmentInterval: courierQualityReportKeyset.ru.column_orderInterval,
  weight: expectedOrdersKeyset.ru.column__size,
  delivered: orderDetailsKeyset.ru.viewLabels_auto_delivered_at_time,
  confirmed: importKeyset.ru.params_initialStatus_confirmed,
  arrivalTime: orderDetailsKeyset.ru.viewLabels_arrival_time,
  late: commonKeyset.ru.time_late,
  serviceTime: courierRouteKeyset.ru.orderTable_column_serviceDurationS,
  sharedServiceTime: courierRouteKeyset.ru.orderTable_column_sharedServiceDurationS,
  courierName: courierMapKeyset.ru.tableColumn_courier,
  clientPhone: orderDetailsKeyset.ru.viewLabels_phone,
  comment: courierQualityReportKeyset.ru.column_orderComments,
  deliveredRadius: courierQualityReportKeyset.ru.column_usedMarkDeliveredRadius,
};

// @see https://testpalm.yandex-team.ru/courier/testcases/357
context('Courier page', () => {
  before(() => {
    cy.preserveCookies();
    cy.yandexLogin('manager');
    cy.get(selectors.sidebar.menu.monitoringGroup).click();
    cy.get(selectors.sidebar.menu.dashboard).click();
    cy.get(selectors.content.dashboard.couriers.table.courierNames)
      .contains(courierNameRecord.gumba)
      .click();
  });

  context('Order popup', () => {
    it('should contain all required fields', function () {
      cy.get(selectors.content.courier.route.tableRow).eq(0).click();

      cy.get(selectors.modal.orderPopup.view).should('be.visible');
      cy.get(selectors.modal.orderPopup.numberRow)
        .should('exist')
        .siblings('th')
        .should('have.text', popupDataHeadings.number);
      cy.get(selectors.modal.orderPopup.clientRow)
        .should('exist')
        .siblings('th')
        .should('have.text', popupDataHeadings.client);
      cy.get(selectors.modal.orderPopup.watcher)
        .should('exist')
        .siblings('th')
        .should('have.text', popupDataHeadings.watcher);
      cy.get(selectors.modal.orderPopup.statusRow)
        .should('exist')
        .siblings('th')
        .should('have.text', popupDataHeadings.status);
      cy.get(selectors.modal.orderPopup.addressRow)
        .should('exist')
        .siblings('th')
        .should('have.text', popupDataHeadings.address);
      cy.get(selectors.modal.orderPopup.dateRow)
        .should('exist')
        .siblings('th')
        .should('have.text', popupDataHeadings.date);
      cy.get(selectors.modal.orderPopup.shipmentIntervalRow)
        .should('exist')
        .siblings('th')
        .should('have.text', popupDataHeadings.shipmentInterval);
      cy.get(selectors.modal.orderPopup.weightRow)
        .should('exist')
        .siblings('th')
        .should('have.text', popupDataHeadings.weight);
      cy.get(selectors.modal.orderPopup.deliveredRow)
        .should('exist')
        .siblings('th')
        .should('have.text', popupDataHeadings.delivered);
      cy.get(selectors.modal.orderPopup.confirmedRow)
        .should('exist')
        .siblings('th')
        .should('have.text', popupDataHeadings.confirmed);
      cy.get(selectors.modal.orderPopup.arrivalTimeRow)
        .should('exist')
        .siblings('th')
        .should('have.text', popupDataHeadings.arrivalTime);
      cy.get(selectors.modal.orderPopup.lateRow)
        .should('exist')
        .siblings('th')
        .should('have.text', popupDataHeadings.late);
      cy.get(selectors.modal.orderPopup.serviceTimeRow)
        .should('exist')
        .siblings('th')
        .should('have.text', popupDataHeadings.serviceTime);
      cy.get(selectors.modal.orderPopup.sharedServiceTimeRow)
        .should('exist')
        .siblings('th')
        .should('have.text', popupDataHeadings.sharedServiceTime);
      cy.get(selectors.modal.orderPopup.courierNameRow)
        .should('exist')
        .siblings('th')
        .should('have.text', popupDataHeadings.courierName);
      cy.get(selectors.modal.orderPopup.clientPhoneRow)
        .should('exist')
        .siblings('th')
        .should('have.text', popupDataHeadings.clientPhone);
      cy.get(selectors.modal.orderPopup.commentRow)
        .should('exist')
        .siblings('th')
        .should('have.text', popupDataHeadings.comment);
      cy.get(selectors.modal.orderPopup.deliveredRadiusRow)
        .should('exist')
        .siblings('th')
        .should('have.text', popupDataHeadings.deliveredRadius);
      cy.get(selectors.modal.orderPopup.map.container).should('be.visible');
    });

    it('should be closed after close button click', function () {
      cy.get(selectors.modal.orderPopup.closeButton).click();

      cy.get(selectors.modal.orderPopup.view).should('not.exist');
      cy.get(selectors.content.courier.name).should('have.text', courierNameRecord.gumba);
      cy.get(selectors.content.courier.route.table).as('table');

      cy.get('@table')
        .find(selectors.content.courier.route.tableColumns.number)
        .should('exist')
        .and('have.text', tableHeadings.number);
      cy.get('@table')
        .find(selectors.content.courier.route.tableColumns.arrivalTime)
        .should('exist')
        .and('have.text', tableHeadings.arrivalTime);
      cy.get('@table')
        .find(selectors.content.courier.route.tableColumns.comments)
        .should('exist')
        .and('have.text', tableHeadings.comments);
      cy.get('@table')
        .find(selectors.content.courier.route.tableColumns.status)
        .should('exist')
        .and('have.text', tableHeadings.status);
      cy.get('@table')
        .find(selectors.content.courier.route.tableColumns.customerName)
        .should('exist')
        .and('have.text', tableHeadings.customerName);
      cy.get('@table')
        .find(selectors.content.courier.route.tableColumns.address)
        .should('exist')
        .and('have.text', tableHeadings.address);
      cy.get('@table')
        .find(selectors.content.courier.route.tableColumns.failedTimeWindow)
        .should('exist')
        .and('have.text', tableHeadings.failedTimeWindow);
      cy.get('@table')
        .find(selectors.content.courier.route.tableColumns.serviceDuration)
        .should('exist')
        .and('have.text', tableHeadings.serviceDuration);
      cy.get('@table')
        .find(selectors.content.courier.route.tableColumns.sharedServiceDuration)
        .should('exist')
        .and('have.text', tableHeadings.sharedServiceDuration);
      cy.get('@table')
        .find(selectors.content.courier.route.tableColumns.timeWindow)
        .should('exist')
        .and('have.text', tableHeadings.timeWindow);
    });

    it('should be visible after click on table order', function () {
      cy.get(selectors.content.courier.route.tableRow).eq(0).click();
      cy.get(selectors.modal.orderPopup.view).should('be.visible');
    });

    it('should be closed after click outside', function () {
      cy.get('body').click(0, 0);
      cy.get(selectors.modal.orderPopup.view).should('not.exist');
    });

    it('dashboard click', function () {
      cy.get(selectors.sidebar.menu.dashboard).click();
      cy.get(selectors.sidebar.menu.courierNameMenuItem).should('not.exist');
      cy.get(selectors.content.dashboard.couriers.table.courierNames).should('be.visible');
    });
  });
});
