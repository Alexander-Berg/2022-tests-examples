import * as importKeyset from '../../../../src/translations/import';
import * as ordersKeyset from '../../../../src/translations/orders';
import * as dashboardOrdersKeyset from '../../../../src/translations/dashboard-orders';
import * as orderDetailsKeyset from '../../../../src/translations/order-details';
import * as courierRouteKeyset from '../../../../src/translations/courier-route';
import * as vehiclesReferenceBookKeyset from '../../../../src/translations/vehicles-reference-book';
import * as courierMapKeyset from '../../../../src/translations/courier-map';
import * as commonKeyset from '../../../../src/translations/common';
import moment from 'moment';
import time from '../../../src/utils/time';
import selectors from '../../../src/constants/selectors';

import urls from '../../../src/utils/urls';
import { courierNameRecord } from '../../../src/constants/couriers';
import 'moment/locale/ru';
import parseISO from 'date-fns/parseISO';
import dateFnsFormat from '../../../src/utils/date-fns-format';

moment.locale('ru');

// @see https://testpalm.yandex-team.ru/courier/testsuite/5fda1df8c79113008cf8a0db?testcase=296
const roles: Array<keyof AccountsT> = ['admin', 'manager'];

context('Dashboard orders', () => {
  beforeEach(() => {
    cy.preserveCookies();
  });

  roles.forEach(role => {
    context(`As ${role} role`, () => {
      describe('Orders for today', () => {
        before(() => {
          cy.yandexLogin(role);
        });

        it('Calendar opened / closed', () => {
          cy.get(selectors.sidebar.menu.monitoringGroup).click();
          cy.get(selectors.sidebar.menu.dashboard).click();
          cy.get(selectors.content.dashboard.dayTitle).click();
          cy.get(selectors.datePicker.daysGrid.days[15]);
          cy.get(selectors.sidebar.menu.dashboard).click();
          cy.get(selectors.datePicker.daysGrid.days[15]).should('not.exist');
        });

        it('Valid number of orders in the dashboard', () => {
          cy.get(selectors.content.dashboard.view);
          cy.get(selectors.content.dashboard.dayOrderNumber)
            .invoke('text')
            .should('eq', `16 ${dashboardOrdersKeyset.ru.orders.many}`);
        });

        it('Valid couriers list', () => {
          cy.get(selectors.content.dashboard.couriers.table.row).should('have.length', 4);
        });

        // @see https://testpalm.yandex-team.ru/testcase/courier-30
        it('Valid date preset', () => {
          cy.get(selectors.content.dashboard.dayTitle)
            .invoke('text')
            .should('eq', commonKeyset.ru.dateRangeFilter_today);
        });

        it('Clicking on the number of orders opens the list of orders', () => {
          cy.get(selectors.content.dashboard.view);
          cy.get(selectors.content.dashboard.dayOrderNumber).click();
          cy.get(selectors.content.orders.tableLoaded);
        });

        describe('Orders list', () => {
          before(() => {
            cy.fixture('company-data').then(({ common }) => {
              const link = urls.ordersList.createLink(common.companyId, {
                date: time.TIME_TODAY,
              });

              cy.yandexLogin(role, { link });
              cy.get(selectors.content.orders.tableLoaded);
            });
          });

          it('Validation of the number of orders in the list', () => {
            cy.get(selectors.content.orders.view);
            cy.get(selectors.content.orders.tableRows).should('have.length', 16);
          });

          it('Validation of the selected date (preset)', () => {
            cy.get(selectors.content.orders.view);
            cy.get(selectors.content.orders.dateFilter.selectedPreset)
              .invoke('text')
              .should('eq', commonKeyset.ru.dateRangeFilter_today);
          });

          it('Validation of the date in the input', () => {
            const date = parseISO(time.TIME_TODAY);
            const dateStr = dateFnsFormat(date, 'dd MMM yyyy').replace('.', '');

            cy.get(selectors.content.orders.view);
            cy.get(selectors.content.orders.dateFilter.input).invoke('text').should('eq', dateStr);
          });

          it('The order menu is highlighted in the sidebar', () => {
            cy.get(selectors.sidebar.menu.monitoringGroup).click();
            cy.get(selectors.sidebar.selectedItem)
              .invoke('text')
              .should('eq', courierMapKeyset.ru.tableColumn_ordersCount);
          });
        });
      });

      // @see https://testpalm.yandex-team.ru/testcase/courier-41
      describe('Orders for tomorrow', () => {
        before(() => {
          cy.fixture('company-data').then(({ common }) => {
            const date = moment(time.TIME_TODAY).add(1, 'days').format(urls.dashboard.dateFormat);

            const link = urls.dashboard.createLink(common.companyId, { date });

            cy.yandexLogin(role, { link });
            cy.get(selectors.content.dashboard.view);
          });
        });

        it('Valid number of orders in the dashboard', () => {
          cy.get(selectors.content.dashboard.view);
          cy.get(selectors.content.dashboard.dayOrderNumber)
            .invoke('text')
            .should('eq', `8 ${dashboardOrdersKeyset.ru.orders.many}`);
        });

        it('Valid couriers list', () => {
          cy.get(selectors.content.dashboard.couriers.table.row).should('have.length', 2);
        });

        // @see https://testpalm.yandex-team.ru/testcase/courier-30
        it('Valid date preset', () => {
          cy.get(selectors.content.dashboard.dayTitle)
            .invoke('text')
            .should('eq', commonKeyset.ru.dateRangeFilter_tomorrow);
        });

        it('Clicking on the number of orders opens the list of orders', () => {
          cy.get(selectors.content.dashboard.view);
          cy.get(selectors.content.dashboard.dayOrderNumber).click();
          cy.get(selectors.content.orders.view);
        });

        describe('Orders list', () => {
          before(() => {
            cy.fixture('company-data').then(({ common }) => {
              const date = moment(time.TIME_TODAY)
                .add(1, 'days')
                .format(urls.ordersList.dateFormat);

              const link = urls.ordersList.createLink(common.companyId, { date });

              cy.yandexLogin(role, { link });
              cy.get(selectors.content.orders.tableLoaded);
            });
          });

          it('Validation of the number of orders in the list', () => {
            cy.get(selectors.content.orders.view);
            cy.get(selectors.content.orders.tableRows).should('have.length', 8);
          });

          it('Validation of the selected date (preset)', () => {
            cy.get(selectors.content.orders.view);
            cy.get(selectors.content.orders.dateFilter.selectedPreset)
              .invoke('text')
              .should('eq', commonKeyset.ru.dateRangeFilter_tomorrow);
          });

          it('Validation of the date in the input', () => {
            const date = parseISO(time.TIME_TODAY);
            date.setDate(date.getDate() + 1);
            const dateStr = dateFnsFormat(date, 'dd MMM yyyy').replace('.', '');

            cy.get(selectors.content.orders.view);
            cy.get(selectors.content.orders.dateFilter.input).invoke('text').should('eq', dateStr);
          });
        });
      });

      // @see https://testpalm.yandex-team.ru/testcase/courier-43"
      describe('Orders for yesterday', () => {
        before(() => {
          cy.fixture('company-data').then(({ common }) => {
            const date = moment(time.TIME_TODAY)
              .subtract(1, 'days')
              .format(urls.dashboard.dateFormat);

            const link = urls.dashboard.createLink(common.companyId, { date });

            cy.yandexLogin(role, { link });
            cy.get(selectors.content.dashboard.view);
          });
        });

        it('Valid number of orders in the dashboard', () => {
          cy.get(selectors.content.dashboard.view);
          cy.get(selectors.content.dashboard.dayOrderNumber)
            .invoke('text')
            .should('eq', `4 ${dashboardOrdersKeyset.ru.orders.some}`);
        });

        it('Valid couriers list', () => {
          cy.get(selectors.content.dashboard.couriers.table.row).should('have.length', 1);
        });

        // @see https://testpalm.yandex-team.ru/testcase/courier-30
        it('Valid date preset', () => {
          cy.get(selectors.content.dashboard.dayTitle)
            .invoke('text')
            .should('eq', commonKeyset.ru.dateRangeFilter_yesterday);
        });

        it('Clicking on the number of orders opens the list of orders', () => {
          cy.get(selectors.content.dashboard.view);
          cy.get(selectors.content.dashboard.dayOrderNumber).click();
          cy.get(selectors.content.orders.view);
        });

        describe('Orders list', () => {
          before(() => {
            cy.fixture('company-data').then(({ common }) => {
              const date = moment(time.TIME_TODAY)
                .subtract(1, 'days')
                .format(urls.ordersList.dateFormat);

              const link = urls.ordersList.createLink(common.companyId, { date });

              cy.yandexLogin(role, { link });
              cy.get(selectors.content.orders.tableLoaded);
            });
          });

          it('Validation of the number of orders in the list', () => {
            cy.get(selectors.content.orders.view);
            cy.get(selectors.content.orders.tableRows).should('have.length', 4);
          });

          it('Validation of the selected date (preset)', () => {
            cy.get(selectors.content.orders.view);
            cy.get(selectors.content.orders.dateFilter.selectedPreset)
              .invoke('text')
              .should('eq', commonKeyset.ru.dateRangeFilter_yesterday);
          });

          it('Validation of the date in the input', () => {
            const date = parseISO(time.TIME_TODAY);
            date.setDate(date.getDate() - 1);
            const dateStr = dateFnsFormat(date, 'dd MMM yyyy').replace('.', '');

            cy.get(selectors.content.orders.view);
            cy.get(selectors.content.couriers.dateFilter.input)
              .invoke('text')
              .should('eq', dateStr);
          });
        });
      });

      // @see https://testpalm.yandex-team.ru/testcase/courier-42
      describe('Orders for the day after tomorrow', () => {
        before(() => {
          cy.fixture('company-data').then(({ common }) => {
            const date = moment(time.TIME_TODAY).add(2, 'days').format(urls.dashboard.dateFormat);

            const link = urls.dashboard.createLink(common.companyId, { date });

            cy.yandexLogin(role, { link });
            cy.get(selectors.content.dashboard.view);
          });
        });

        it('Valid number of orders in the dashboard', () => {
          cy.get(selectors.content.dashboard.view);
          cy.get(selectors.content.dashboard.dayOrderNumber)
            .invoke('text')
            .should('eq', `7 ${dashboardOrdersKeyset.ru.orders.many}`);
        });

        // @see https://testpalm.yandex-team.ru/testcase/courier-30
        it('Valid date preset', () => {
          const date = moment(time.TIME_TODAY).add(2, 'days');
          cy.get(selectors.content.dashboard.dayTitle)
            .invoke('text')
            .should(
              'eq',
              `${date.format('D')} ${
                time.DATE_SNIPPET.months[parseInt(date.format('MM'), 10) - 1]
              }`,
            );
        });

        it('Clicking on the number of orders opens the list of orders', () => {
          cy.get(selectors.content.dashboard.view);
          cy.get(selectors.content.dashboard.dayOrderNumber).click();
          cy.get(selectors.content.orders.view);
        });

        // @see https://testpalm.yandex-team.ru/testcase/courier-90
        describe('Orders list', () => {
          before(() => {
            cy.fixture('company-data').then(({ common }) => {
              const date = moment(time.TIME_TODAY)
                .add(2, 'days')
                .format(urls.ordersList.dateFormat);

              const link = urls.ordersList.createLink(common.companyId, { date });

              cy.yandexLogin(role, { link });
              cy.get(selectors.content.orders.tableLoaded);
            });
          });

          it('Validation of the number of orders in the list', () => {
            cy.get(selectors.content.orders.view);
            cy.get(selectors.content.orders.tableRows).should('have.length', 7);
          });

          it('There is no dedicated day (preset)', () => {
            cy.get(selectors.content.orders.view);
            cy.get(selectors.content.orders.dateFilter.selectedPreset).should('not.exist');
          });

          it('Validation of the date in the input', () => {
            const date = parseISO(time.TIME_TODAY);
            date.setDate(date.getDate() + 2);
            const dateStr = dateFnsFormat(date, 'dd MMM yyyy').replace('.', '');

            cy.get(selectors.content.orders.view);
            cy.get(selectors.content.couriers.dateFilter.input)
              .invoke('text')
              .should('eq', dateStr);
          });
        });
      });

      // @see https://testpalm.yandex-team.ru/testcase/courier-187
      describe('Orders for the day before yesterday', () => {
        before(() => {
          cy.fixture('company-data').then(({ common }) => {
            const date = moment(time.TIME_TODAY)
              .subtract(2, 'days')
              .format(urls.dashboard.dateFormat);

            const link = urls.dashboard.createLink(common.companyId, { date });

            cy.yandexLogin(role, { link });
            cy.get(selectors.content.dashboard.view);
          });
        });

        it('Valid number of orders in the dashboard', () => {
          cy.get(selectors.content.dashboard.view);
          cy.get(selectors.content.dashboard.dayOrderNumber)
            .invoke('text')
            .should('eq', `4 ${dashboardOrdersKeyset.ru.orders.some}`);
        });

        // @see https://testpalm.yandex-team.ru/testcase/courier-30
        it('Valid date preset', () => {
          const date = moment(time.TIME_TODAY).subtract(2, 'days');
          cy.get(selectors.content.dashboard.dayTitle)
            .invoke('text')
            .should(
              'eq',
              `${date.format('D')} ${
                time.DATE_SNIPPET.months[parseInt(date.format('MM'), 10) - 1]
              }`,
            );
        });

        it('Clicking on the number of orders opens the list of orders', () => {
          cy.get(selectors.content.dashboard.view);
          cy.get(selectors.content.dashboard.dayOrderNumber).click();
          cy.get(selectors.content.orders.view);
        });

        describe('Orders list', () => {
          before(() => {
            cy.fixture('company-data').then(({ common }) => {
              const date = moment(time.TIME_TODAY)
                .subtract(2, 'days')
                .format(urls.ordersList.dateFormat);

              const link = urls.ordersList.createLink(common.companyId, { date });

              cy.yandexLogin(role, { link });
              cy.get(selectors.content.orders.tableLoaded);
            });
          });

          it('Validation of the number of orders in the list', () => {
            cy.get(selectors.content.orders.view);
            cy.get(selectors.content.orders.tableRows).should('have.length', 4);
          });

          it('There is no dedicated day (preset)', () => {
            cy.get(selectors.content.orders.view);
            cy.get(selectors.content.orders.dateFilter.selectedPreset).should('not.exist');
          });

          it('Validation of the date in the input', () => {
            const date = parseISO(time.TIME_TODAY);
            date.setDate(date.getDate() - 2);
            const dateStr = dateFnsFormat(date, 'dd MMM yyyy').replace('.', '');

            cy.get(selectors.content.orders.view);
            cy.get(selectors.content.couriers.dateFilter.input)
              .invoke('text')
              .should('eq', dateStr);
          });
        });
      });

      context('Order for the far future', () => {
        const date = moment(time.TIME_TODAY).add(350, 'days').format(urls.dashboard.dateFormat);
        before(() => {
          cy.fixture('company-data').then(({ common }) => {
            const link = urls.dashboard.createLink(common.companyId, { date });
            cy.yandexLogin(role, { link });
          });
        });

        it('List of routes displayed and title is correct', () => {
          const dateStr = dateFnsFormat(date, 'd MMMM');
          cy.get(selectors.content.dashboard.dayTitle).invoke('text').should('eq', dateStr);
          cy.get(selectors.content.dashboard.couriers.table.row).should('have.length', 0);
          cy.get(selectors.content.dashboard.dayOrderNumber)
            .invoke('text')
            .should('eq', `0 ${dashboardOrdersKeyset.ru.orders.many}`);
          cy.get(selectors.content.dashboard.dayOrderNumber).click();
        });

        it('Orders page is correct', () => {
          const dateStr = dateFnsFormat(date, 'dd MMM yyyy').replace('.', '');
          cy.get(selectors.content.orders.dateFilter.input).invoke('text').should('eq', dateStr);
          cy.get(selectors.content.orders.dateFilter.selectedPreset).should('not.exist');
          cy.get(selectors.content.orders.message)
            .invoke('text')
            .should('eq', courierRouteKeyset.ru.emptyOrders);
          cy.get(selectors.content.orders.tableRows).should('have.length', 0);
        });
      });
    });
  });

  describe('The transition to the pop-up of the order.', () => {
    before(() => {
      cy.yandexLogin('manager');
      cy.get(selectors.content.dashboard.view);
    });

    // @see https://testpalm.yandex-team.ru/testcase/courier-35
    describe('Delivered late', () => {
      before(() => {
        cy.get(selectors.content.dashboard.couriers.table.orders.finishedLate).click();
      });

      after(() => {
        cy.go('back');
        cy.get(selectors.modal.orderPopup.closeButton).click();
      });

      it('The order pop-up opened', () => {
        cy.get(selectors.modal.orderPopup.title);
      });

      it('Correct order number in the header', () => {
        const orderNumber = 'extraTESTfinished';
        cy.get(selectors.modal.orderPopup.title).invoke('text').should('contain', orderNumber);
      });

      it('Order Status: Delivered', () => {
        cy.get(selectors.modal.orderPopup.title);
        cy.get(selectors.modal.orderPopup.statusRow)
          .invoke('text')
          .should('eq', orderDetailsKeyset.ru.viewLabels_auto_delivered_at_time);
      });

      it('Correct date', () => {
        cy.get(selectors.modal.orderPopup.title);
        cy.get(selectors.modal.orderPopup.dateRow)
          .invoke('text')
          .should('eq', moment().format('DD.MM.YYYY'));
      });

      it('The correct delivery interval', () => {
        cy.get(selectors.modal.orderPopup.title);
        cy.get(selectors.modal.orderPopup.shipmentIntervalRow)
          .invoke('text')
          .should('match', /00:00 [–-—] 00:01/);
      });

      it('Correct weight', () => {
        cy.get(selectors.modal.orderPopup.title);
        cy.get(selectors.modal.orderPopup.weightRow).invoke('text').should('eq', '10 кг');
      });

      it('Correct service time', () => {
        cy.get(selectors.modal.orderPopup.title);
        cy.get(selectors.modal.orderPopup.serviceTimeRow).invoke('text').should('eq', '10 мин');
      });

      it('The correct phone number of the client', () => {
        cy.get(selectors.modal.orderPopup.title);
        cy.get(selectors.modal.orderPopup.clientPhoneRow)
          .invoke('text')
          .should('eq', '+70000000000');
      });

      it('Correct commentary', () => {
        cy.get(selectors.modal.orderPopup.title);
        cy.get(selectors.modal.orderPopup.commentRow).invoke('text').should('eq', 'Не кантовать!');
      });

      it('Time of delay', () => {
        cy.get(selectors.modal.orderPopup.title);
        cy.get(selectors.modal.orderPopup.lateRow)
          .invoke('text')
          .should('match', /[0-9][0-9]* ч ([1-5][0-9])|([0-9]) мин$/);
      });

      it('Couriers name', () => {
        cy.get(selectors.modal.orderPopup.title);
        cy.get(selectors.modal.orderPopup.courierNameRow)
          .invoke('text')
          .should('contain', courierNameRecord.kypa);
      });

      it('Address', () => {
        cy.get(selectors.modal.orderPopup.title);
        cy.get(selectors.modal.orderPopup.addressRow)
          .invoke('text')
          .should('eq', 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2');
      });

      it('Clicking on the address opens Yandex. Maps', () => {
        cy.get(selectors.modal.orderPopup.title);
        cy.get(`${selectors.modal.orderPopup.addressRow}:nth-child(2) a`)
          .should('have.attr', 'href')
          .and('include', 'maps.yandex.ru');
      });

      it('Clicking on the courier`s name opens the courier`s page', () => {
        cy.get(selectors.modal.orderPopup.title);
        cy.get(`${selectors.modal.orderPopup.courierNameRow}:nth-child(2) a`).click({
          force: true,
        });
        cy.get(selectors.content.couriers.singleCourier.courierName)
          .invoke('text')
          .should('eq', courierNameRecord.kypa);
      });
    });

    // @see https://testpalm.yandex-team.ru/testcase/courier-50
    describe('Cancelled.', () => {
      before(() => {
        cy.get(selectors.content.dashboard.couriers.table.orders.cancelled).first().click();
      });

      after(() => {
        cy.get(selectors.modal.orderPopup.closeButton).click();
      });

      it('The order pop-up opened', () => {
        cy.get(selectors.modal.orderPopup.title);
      });

      it('Correct order number in the header', () => {
        const orderNumber = 'TESTcancelled';

        cy.get(selectors.modal.orderPopup.title)
          .invoke('text')
          .should('eq', `Заказ ${orderNumber}`);
      });

      it('Order Status: Delivered', () => {
        cy.get(selectors.modal.orderPopup.statusRow)
          .invoke('text')
          .should('eq', ordersKeyset.ru.status_cancelled);
      });

      it('Correct date', () => {
        cy.get(selectors.modal.orderPopup.dateRow)
          .invoke('text')
          .should('eq', moment().format('DD.MM.YYYY'));
      });

      it('The correct delivery interval', () => {
        cy.get(selectors.modal.orderPopup.shipmentIntervalRow)
          .invoke('text')
          .should('match', /12:00 [–-—] 18:00/);
      });

      it('Correct weight', () => {
        cy.get(selectors.modal.orderPopup.weightRow).invoke('text').should('eq', '10 кг');
      });

      it('Correct service time', () => {
        cy.get(selectors.modal.orderPopup.serviceTimeRow).invoke('text').should('eq', '10 мин');
      });

      it('The correct phone number of the client', () => {
        cy.get(selectors.modal.orderPopup.clientPhoneRow)
          .invoke('text')
          .should('eq', '+70000000000');
      });

      it('Correct commentary', () => {
        cy.get(selectors.modal.orderPopup.commentRow).invoke('text').should('eq', 'Не кантовать!');
      });

      it('Time of delay', () => {
        cy.get(selectors.modal.orderPopup.lateRow).invoke('text').should('eq', '');
      });

      it('The "Delivered" field is empty', () => {
        cy.get(selectors.modal.orderPopup.deliveredRow).invoke('text').should('eq', '');
      });

      it('Couriers name', () => {
        cy.get(selectors.modal.orderPopup.courierNameRow)
          .invoke('text')
          .should('contain', courierNameRecord.kypa);
      });

      it('Address', () => {
        cy.get(selectors.modal.orderPopup.addressRow)
          .invoke('text')
          .should('eq', 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2');
      });

      it('Clicking on the address opens Yandex. Maps', () => {
        cy.get(selectors.modal.orderPopup.title);
        cy.get(`${selectors.modal.orderPopup.addressRow}:nth-child(2) a`)
          .should('have.attr', 'href')
          .and('include', 'maps.yandex.ru');
      });
    });

    // @see https://testpalm.yandex-team.ru/testcase/courier-53
    describe('Late', () => {
      before(() => {
        cy.get(selectors.content.dashboard.couriers.table.orders.late).click();
      });

      after(() => {
        cy.go('back');
        cy.get(selectors.modal.orderPopup.closeButton).click();
      });

      it('The order pop-up opened', () => {
        cy.get(selectors.modal.orderPopup.title);
      });

      it('Correct order number in the header', () => {
        const orderNumber = 'extraTESTconfirmed';
        cy.get(selectors.modal.orderPopup.title)
          .invoke('text')
          .should('eq', `Заказ ${orderNumber}`);
      });

      it('Order Status: Delivered', () => {
        cy.get(selectors.modal.orderPopup.title);
        cy.get(selectors.modal.orderPopup.statusRow)
          .invoke('text')
          .should('eq', importKeyset.ru.params_initialStatus_confirmed);
      });

      it('Correct date', () => {
        cy.get(selectors.modal.orderPopup.title);
        cy.get(selectors.modal.orderPopup.dateRow)
          .invoke('text')
          .should('eq', moment().format('DD.MM.YYYY'));
      });

      it('The correct delivery interval', () => {
        cy.get(selectors.modal.orderPopup.title);
        cy.get(selectors.modal.orderPopup.shipmentIntervalRow)
          .invoke('text')
          .should('match', /00:00 [–-—] 00:01/);
      });

      it('Correct weight', () => {
        cy.get(selectors.modal.orderPopup.title);
        cy.get(selectors.modal.orderPopup.weightRow).invoke('text').should('eq', '10 кг');
      });

      it('Correct service time', () => {
        cy.get(selectors.modal.orderPopup.title);
        cy.get(selectors.modal.orderPopup.serviceTimeRow).invoke('text').should('eq', '10 мин');
      });

      it('The correct phone number of the client', () => {
        cy.get(selectors.modal.orderPopup.title);
        cy.get(selectors.modal.orderPopup.clientPhoneRow)
          .invoke('text')
          .should('eq', '+70000000000');
      });

      it('Correct commentary', () => {
        cy.get(selectors.modal.orderPopup.title);
        cy.get(selectors.modal.orderPopup.commentRow).invoke('text').should('eq', 'Не кантовать!');
      });

      it('Time of delay', () => {
        cy.get(selectors.modal.orderPopup.title);
        cy.get(selectors.modal.orderPopup.lateRow)
          .invoke('text')
          .should('match', /[0-9][0-9]* ч ([1-5][0-9])|([0-9]) мин$/);
      });

      it('The "Delivered" field is empty', () => {
        cy.get(selectors.modal.orderPopup.title);
        cy.get(selectors.modal.orderPopup.deliveredRow).invoke('text').should('eq', '');
      });

      it('Couriers name', () => {
        cy.get(selectors.modal.orderPopup.title);
        cy.get(selectors.modal.orderPopup.courierNameRow)
          .invoke('text')
          .should('contain', courierNameRecord.kypa);
      });

      it('Address', () => {
        cy.get(selectors.modal.orderPopup.title);
        cy.get(selectors.modal.orderPopup.addressRow)
          .invoke('text')
          .should('eq', 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2');
      });

      it('Clicking on the address opens Yandex. Maps', () => {
        cy.get(selectors.modal.orderPopup.title);
        cy.get(`${selectors.modal.orderPopup.addressRow}:nth-child(2) a`)
          .should('have.attr', 'href')
          .and('include', 'maps.yandex.ru');
      });

      it('Clicking on the courier`s name opens the courier`s page', () => {
        cy.get(selectors.modal.orderPopup.title);
        cy.get(`${selectors.modal.orderPopup.courierNameRow}:nth-child(2) a`).click({
          force: true,
        });
        cy.get(selectors.content.couriers.singleCourier.courierName)
          .invoke('text')
          .should('eq', courierNameRecord.kypa);
      });
    });

    // @see https://testpalm.yandex-team.ru/testcase/courier-51
    describe('Delivered', () => {
      before(() => {
        cy.get(selectors.content.dashboard.couriers.table.orders.finished).click();
      });

      after(() => {
        cy.go('back');
        cy.get(selectors.modal.orderPopup.closeButton).click();
      });

      it('The order pop-up opened', () => {
        cy.get(selectors.modal.orderPopup.title);
      });

      it('Correct order number in the header', () => {
        const orderNumber = 'TESTfinished';

        cy.get(selectors.modal.orderPopup.title)
          .invoke('text')
          .should('eq', `Заказ ${orderNumber}`);
      });

      it('Order Status: Delivered', () => {
        cy.get(selectors.modal.orderPopup.title);
        cy.get(selectors.modal.orderPopup.statusRow)
          .invoke('text')
          .should('eq', orderDetailsKeyset.ru.viewLabels_auto_delivered_at_time);
      });

      it('Correct date', () => {
        cy.get(selectors.modal.orderPopup.title);
        cy.get(selectors.modal.orderPopup.dateRow)
          .invoke('text')
          .should('eq', moment().format('DD.MM.YYYY'));
      });

      it('The correct delivery interval', () => {
        cy.get(selectors.modal.orderPopup.title);
        cy.get(selectors.modal.orderPopup.shipmentIntervalRow)
          .invoke('text')
          .should('match', /00:00 [–-—] 23:59/);
      });

      it('Correct weight', () => {
        cy.get(selectors.modal.orderPopup.title);
        cy.get(selectors.modal.orderPopup.weightRow).invoke('text').should('eq', '10 кг');
      });

      it('Correct service time', () => {
        cy.get(selectors.modal.orderPopup.title);
        cy.get(selectors.modal.orderPopup.serviceTimeRow).invoke('text').should('eq', '10 мин');
      });

      it('The correct phone number of the client', () => {
        cy.get(selectors.modal.orderPopup.title);
        cy.get(selectors.modal.orderPopup.clientPhoneRow)
          .invoke('text')
          .should('eq', '+70000000000');
      });

      it('Correct commentary', () => {
        cy.get(selectors.modal.orderPopup.title);
        cy.get(selectors.modal.orderPopup.commentRow).invoke('text').should('eq', 'Не кантовать!');
      });

      it('Time of delay', () => {
        cy.get(selectors.modal.orderPopup.title);
        cy.get(selectors.modal.orderPopup.lateRow).invoke('text').should('eq', '');
      });

      it('The "Delivered" field', () => {
        cy.get(selectors.modal.orderPopup.title);
        cy.get(selectors.modal.orderPopup.deliveredRow)
          .invoke('text')
          .should('match', /\d{4}-\d{2}-\d{2} [0-9][0-9]*:([1-5][0-9])|([0-9])$/);
      });

      it('Couriers name', () => {
        cy.get(selectors.modal.orderPopup.title);
        cy.get(selectors.modal.orderPopup.courierNameRow)
          .invoke('text')
          .should('contain', courierNameRecord.kypa);
      });

      it('Address', () => {
        cy.get(selectors.modal.orderPopup.title);
        cy.get(selectors.modal.orderPopup.addressRow)
          .invoke('text')
          .should('eq', 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2');
      });

      it('Clicking on the address opens Yandex. Maps', () => {
        cy.get(selectors.modal.orderPopup.title);
        cy.get(`${selectors.modal.orderPopup.addressRow}:nth-child(2) a`)
          .should('have.attr', 'href')
          .and('include', 'maps.yandex.ru');
      });

      it('Clicking on the courier`s name opens the courier`s page', () => {
        cy.get(selectors.modal.orderPopup.title);
        cy.get(`${selectors.modal.orderPopup.courierNameRow}:nth-child(2) a`).click({
          force: true,
        });
        cy.get(selectors.content.couriers.singleCourier.courierName)
          .invoke('text')
          .should('eq', courierNameRecord.kypa);
      });
    });

    // @see https://testpalm.yandex-team.ru/testcase/courier-51
    describe('According to the plan', () => {
      before(() => {
        cy.get(
          `.dashboard-couriers__table-row:nth-child(5) ${selectors.content.dashboard.couriers.table.orders.sequenced}:nth-child(2)`,
        ).click();
      });

      after(() => {
        cy.go('back');
        cy.get(selectors.modal.orderPopup.closeButton).click();
      });

      it('The order pop-up opened', () => {
        cy.get(selectors.modal.orderPopup.title);
      });

      it('Correct order number in the header', () => {
        const orderNumber = 'TESTconfirmed';
        cy.get(selectors.modal.orderPopup.title)
          .invoke('text')
          .should('eq', `Заказ ${orderNumber}`);
      });

      it('Order Status: Confirmed', () => {
        cy.get(selectors.modal.orderPopup.title);
        cy.get(selectors.modal.orderPopup.statusRow)
          .invoke('text')
          .should('eq', importKeyset.ru.params_initialStatus_confirmed);
      });

      it('Correct date', () => {
        cy.get(selectors.modal.orderPopup.title);
        cy.get(selectors.modal.orderPopup.dateRow)
          .invoke('text')
          .should('eq', moment().format('DD.MM.YYYY'));
      });

      it('The correct delivery interval', () => {
        cy.get(selectors.modal.orderPopup.title);
        cy.get(selectors.modal.orderPopup.shipmentIntervalRow)
          .invoke('text')
          .should('match', /00:00 [–-—] 23:59/);
      });

      it('Correct weight', () => {
        cy.get(selectors.modal.orderPopup.title);
        cy.get(selectors.modal.orderPopup.weightRow).invoke('text').should('eq', '10 кг');
      });

      it('Correct service time', () => {
        cy.get(selectors.modal.orderPopup.title);
        cy.get(selectors.modal.orderPopup.serviceTimeRow).invoke('text').should('eq', '10 мин');
      });

      it('The correct phone number of the client', () => {
        cy.get(selectors.modal.orderPopup.title);
        cy.get(selectors.modal.orderPopup.clientPhoneRow)
          .invoke('text')
          .should('eq', '+70000000000');
      });

      it('Correct commentary', () => {
        cy.get(selectors.modal.orderPopup.title);
        cy.get(selectors.modal.orderPopup.commentRow).invoke('text').should('eq', 'Не кантовать!');
      });

      it('Time of delay', () => {
        cy.get(selectors.modal.orderPopup.title);
        cy.get(selectors.modal.orderPopup.lateRow).invoke('text').should('eq', '');
      });

      it('The "Delivered" field', () => {
        cy.get(selectors.modal.orderPopup.title);
        cy.get(selectors.modal.orderPopup.deliveredRow).invoke('text').should('eq', '');
      });

      it('Couriers name', () => {
        cy.get(selectors.modal.orderPopup.title);
        cy.get(selectors.modal.orderPopup.courierNameRow)
          .invoke('text')
          .should('contain', courierNameRecord.kypa);
      });

      it('Address', () => {
        cy.get(selectors.modal.orderPopup.title);
        cy.get(selectors.modal.orderPopup.addressRow)
          .invoke('text')
          .should('eq', 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2');
      });

      it('Clicking on the address opens Yandex. Maps', () => {
        cy.get(selectors.modal.orderPopup.title);
        cy.get(`${selectors.modal.orderPopup.addressRow}:nth-child(2) a`)
          .should('have.attr', 'href')
          .and('include', 'maps.yandex.ru');
      });

      it('Clicking on the courier`s name opens the courier`s page', () => {
        cy.get(selectors.modal.orderPopup.title);
        cy.get(`${selectors.modal.orderPopup.courierNameRow}:nth-child(2) a`).click({
          force: true,
        });
        cy.get(selectors.content.couriers.singleCourier.courierName)
          .invoke('text')
          .should('eq', courierNameRecord.kypa);
      });
    });
  });

  // @see https://testpalm.yandex-team.ru/courier/testcases/551
  describe('Address input clear', () => {
    before(() => {
      cy.yandexLogin('admin');
      cy.get(selectors.content.dashboard.view);
    });

    it('should open edit order form from dashboard', () => {
      cy.get(selectors.content.dashboard.couriers.table.orders.sequenced).first().click();
      cy.get(selectors.modal.orderPopup.editButton).click();

      cy.get(selectors.modal.orderPopup.addressRow)
        .find(selectors.suggest.container)
        .should('exist');

      cy.get(selectors.suggest.input).should('exist').and('not.have.value', '');
      cy.get(selectors.suggest.clear).should('exist');
    });

    it('should clear address input', () => {
      cy.get(selectors.suggest.clear).click();

      cy.get(selectors.suggest.input).should('have.value', '');
    });

    it('should show suggests when typing', () => {
      cy.get(selectors.suggest.input).type('москва');

      cy.get(selectors.suggest.list).should('exist');
      cy.get(selectors.suggest.listOptions).should('have.length.above', 0);
    });

    it('should clear address input after suggests', () => {
      cy.get(selectors.suggest.clear).click();

      cy.get(selectors.suggest.input).should('have.value', '');
    });

    it('should clear address input after suggest select', () => {
      cy.get(selectors.suggest.input).type('москва');
      cy.get(selectors.suggest.listOptions).first().click();
      cy.get(selectors.suggest.clear).click();

      cy.get(selectors.suggest.input).should('have.value', '');
    });
  });
});
