import * as courierMapKeyset from '../../../../src/translations/courier-map';
import * as importKeyset from '../../../../src/translations/import';
import * as orderDetailsKeyset from '../../../../src/translations/order-details';
import * as expectedOrdersKeyset from '../../../../src/translations/expected-orders';
import * as createRouteFormKeyset from '../../../../src/translations/create-route-form';
import * as courierQualityReportKeyset from '../../../../src/translations/courier-quality-report';
import * as commonKeyset from '../../../../src/translations/common';
import * as courierRouteKeyset from '../../../../src/translations/courier-route';
import selectors from '../../../src/constants/selectors';
import urls from '../../../src/utils/urls';
import type { IPointGeometry } from 'yandex-maps';

const fieldLabels = {
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

describe('Order modal', function () {
  beforeEach(function () {
    cy.preserveCookies();
  });

  before(function () {
    cy.fixture('company-data').then(({ common }) => {
      const link = urls.ordersList.createLink(common.companyId, {});
      cy.yandexLogin('admin', { link });
    });
  });

  it('Open from orders list', function () {
    cy.get(selectors.content.orders.tableLoaded);
    cy.get(selectors.content.orders.firstOrder).click();
    cy.get(selectors.modal.orderPopup.view).should('exist');
  });

  describe(commonKeyset.en.close, function () {
    // @see https://testpalm.yandex-team.ru/testcase/courier-56
    it('Click in cross', function () {
      cy.get(selectors.modal.orderPopup.closeButton).click();
      cy.get(selectors.modal.orderPopup.view).should('not.exist');
    });

    after(function () {
      cy.get(selectors.content.orders.firstOrder).click();
      cy.get(selectors.modal.orderPopup.view);
    });
  });

  // @see https://testpalm.yandex-team.ru/testcase/courier-55
  describe('Map label', function () {
    before(function () {
      cy.get(selectors.modal.orderPopup.map.placemark).triggerOnLayer(
        selectors.modal.orderPopup.map.events,
        { event: 'click' },
      );
    });

    it('Click in marker open ballon', function () {
      cy.get(selectors.modal.orderPopup.map.balloon.view).should('exist');
    });

    it('Close ballon by cross', function () {
      cy.get(selectors.modal.orderPopup.map.balloon.closeButton).click();
      cy.get(selectors.modal.orderPopup.map.balloon.view).should('not.exist');
    });
  });

  // @see https://testpalm.yandex-team.ru/courier/testcases/338
  describe('Info and map', function () {
    before(function () {
      cy.yandexLogin('admin');
    });

    it('should open orders list from sidebar link', function () {
      cy.get(selectors.sidebar.menu.monitoringGroup).click();
      cy.get(selectors.sidebar.menu.orders).click();

      cy.get(selectors.content.orders.tableLoaded).should('exist');
    });

    it('should open order modal by clicking order in list', function () {
      cy.get(selectors.content.orders.tableRows).contains('additional-1').click();

      cy.get(selectors.modal.orderPopup.view).should('exist');

      cy.get(selectors.modal.orderPopup.closeButton).click();
    });

    it('should contain map with correct position', function () {
      cy.get(selectors.content.orders.tableRows).contains('additional-1').click();
      cy.window()
        .its('map')
        .should('exist')
        .then(map => {
          const [x, y] = map.getCenter();
          const pointCoordinates = (
            map.geoObjects.get(0).geometry as IPointGeometry
          ).getCoordinates();

          expect(x).to.be.closeTo(60, 1e-6);
          expect(y).to.be.closeTo(31, 1e-6);

          expect(pointCoordinates).to.have.ordered.members([60, 31]);
        });
    });

    it('should close after click on backdrop', function () {
      cy.get(selectors.modal.orderPopup.backdrop).trigger('mousedown', 10, 10);

      cy.get(selectors.modal.orderPopup.view).should('not.exist');
      cy.get(selectors.content.orders.tableLoaded).should('be.visible');
    });

    it('should close after click on close button', function () {
      cy.get(selectors.content.orders.tableRows).contains('additional-1').click();
      cy.get(selectors.modal.orderPopup.closeButton).click();

      cy.get(selectors.modal.orderPopup.view).should('not.exist');
      cy.get(selectors.content.orders.tableLoaded).should('be.visible');
    });

    it('should contain link to maps on address info row', function () {
      cy.get(selectors.content.orders.tableRows).contains('additional-1').click();

      cy.get(selectors.modal.orderPopup.view).should('exist');
      cy.get(selectors.modal.orderPopup.addressRowLink)
        .should('have.prop', 'href')
        .and('include', 'maps.yandex.ru/?text=');
    });

    it('should navigate to courier profile on courier name click', function () {
      cy.fixture('testData').then(({ courierNameRecord }) => {
        cy.get(selectors.modal.orderPopup.courierNameRowLink).click();

        cy.get(selectors.content.courier.view).should('exist');
        cy.get(selectors.content.courier.route.map.common.container).should('exist');
        cy.get(selectors.content.courier.ordersTable).should('exist');

        cy.get(selectors.sidebar.menu.courierNameMenuItem).should(
          'have.text',
          courierNameRecord.gumba,
        );
        cy.get(selectors.sidebar.companySelector.depots.currentDepotAddress)
          .should('exist')
          .and('have.text', 'Россия, Санкт-Петербург, Пискаревский проспект, 2к2');
      });
    });
  });
});
