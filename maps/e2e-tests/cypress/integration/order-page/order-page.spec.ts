import * as courierRouteKeyset from '../../../../src/translations/courier-route';
import * as commonKeyset from '../../../../src/translations/common';
import moment from 'moment';
import time from '../../../src/utils/time';
import selectors from '../../../src/constants/selectors';

import urls from '../../../src/utils/urls';
import 'moment/locale/ru';
import map from 'lodash/map';
import parseISO from 'date-fns/parseISO';
import dateFnsFormat from '../../../src/utils/date-fns-format';

moment.locale('ru');

const tableFieldKeys = [
  'number',
  'customer_name',
  'phone',
  'companies',
  'status',
  'route_date',
  'time_window',
  'arrival_time',
  'confirmed_at_time',
  'delivered_at_time',
  'failed_time_window',
  'service_duration_s',
  'shared_service_duration_s',
  'courier_name',
  'mark_delivered_radius',
  'address',
  'comments',
  'lat',
  'lon',
];

describe('Order page', function () {
  beforeEach(() => {
    cy.preserveCookies();
  });

  before(function () {
    cy.fixture('company-data').then(({ common }) => {
      const link = urls.ordersList.createLink(common.companyId, {});

      cy.yandexLogin('manager', { link });
      cy.waitForElement(selectors.content.orders.tableLoaded, { timeout: 10000 });
    });
  });

  // @see https://testpalm.yandex-team.ru/testcase/courier-80
  describe('Change date (today - tomorrow)', function () {
    before(function () {
      cy.get(selectors.content.orders.dateFilter.presetTomorrow).click();
      cy.waitForElement(selectors.content.orders.tableLoaded);
    });

    it('Count of orders was changed', function () {
      cy.get(selectors.content.orders.tableRows).should('have.length', 8);
    });

    it('Date in input was changed', function () {
      const date = parseISO(time.TIME_TODAY);
      date.setDate(date.getDate() + 1);
      const dateStr = dateFnsFormat(date, 'dd MMM yyyy').replace('.', '');

      cy.get(selectors.content.orders.dateFilter.input).invoke('text').should('eq', dateStr);
    });

    it('Day is highlighted (preset)', function () {
      cy.get(selectors.content.orders.dateFilter.presetTomorrow)
        .invoke('text')
        .should('eq', commonKeyset.ru.dateRangeFilter_tomorrow);
    });

    after(function () {
      cy.get(selectors.content.orders.dateFilter.presetToday).click();
      cy.waitForElement(selectors.content.orders.tableLoaded);
    });
  });

  // @see https://testpalm.yandex-team.ru/testcase/courier-81
  describe('Change date (yesterday - today)', function () {
    before(function () {
      cy.get(selectors.content.orders.dateFilter.presetYesterday).click();
      cy.waitForElement(selectors.content.orders.tableLoaded);
      cy.get(selectors.content.orders.dateFilter.presetToday).click();
      cy.waitForElement(selectors.content.orders.tableLoaded);
      cy.wait(200);
    });

    it('Count of orders was changed', function () {
      cy.get(selectors.content.orders.tableRows).should('have.length', 16);
    });

    it('Date in input was changed', function () {
      const date = parseISO(time.TIME_TODAY);
      const dateStr = dateFnsFormat(date, 'dd MMM yyyy').replace('.', '');

      cy.get(selectors.content.orders.dateFilter.input).invoke('text').should('eq', dateStr);
    });

    it('Day is highlighted (preset)', function () {
      cy.get(selectors.content.orders.dateFilter.presetToday)
        .invoke('text')
        .should('eq', commonKeyset.ru.dateRangeFilter_today);
    });

    after(function () {
      cy.get(selectors.content.orders.dateFilter.presetToday).click();
      cy.waitForElement(selectors.content.orders.tableLoaded);
    });
  });

  describe('Filters', function () {
    afterEach(function () {
      cy.get(selectors.content.orders.filter.input).clear().type('{backspace}').clear().wait(100);
    });

    // @see https://testpalm.yandex-team.ru/testcase/courier-82
    it('Search non existing order', function () {
      cy.get(selectors.content.orders.filter.input).type('789').type('{enter}');
      cy.wait(1100);
      cy.get(selectors.content.orders.message)
        .invoke('text')
        .should('eq', courierRouteKeyset.ru.emptyOrders);
    });

    // @see https://testpalm.yandex-team.ru/testcase/courier-93
    it('Search by order name', function () {
      const orderNumber = '14';

      cy.get(selectors.content.orders.filter.input).type(orderNumber).type('{enter}');
      cy.wait(1100);

      cy.get(selectors.content.orders.tableRows).invoke('text').should('include', orderNumber);
    });

    // @see https://testpalm.yandex-team.ru/testcase/courier-94
    it('Search by customer', function () {
      const inputValue = 'shy';
      const customerName = 'Shy Guy';

      cy.get(selectors.content.orders.filter.input).type(inputValue).type('{enter}');
      cy.wait(1100);

      cy.get(selectors.content.orders.tableRows).invoke('text').should('include', customerName);
    });

    // @see https://testpalm.yandex-team.ru/testcase/courier-102
    it('Search by customer with Upper case', function () {
      const inputValue = 'Shy';
      const customerName = 'Shy Guy';

      cy.get(selectors.content.orders.filter.input).type(inputValue).type('{enter}');
      cy.wait(1100);

      cy.get(selectors.content.orders.tableRows).invoke('text').should('include', customerName);
    });

    // @see https://testpalm.yandex-team.ru/testcase/courier-95
    it('Search by phone', function () {
      const phone = '+70002222222';
      const orderNumber = 'phoneNumberTest';

      cy.get(selectors.content.orders.filter.input).type(phone).type('{enter}');
      cy.wait(1100);

      cy.get(selectors.content.orders.tableRows).invoke('text').should('include', orderNumber);
    });

    // @see https://testpalm.yandex-team.ru/testcase/courier-99
    it('Search by city', function () {
      const city = 'москва';
      const orderNumber = 'citySearchTest';

      cy.get(selectors.content.orders.filter.input).type(city).type('{enter}');
      cy.wait(1100);

      cy.get(selectors.content.orders.tableRows).invoke('text').should('include', orderNumber);
    });

    // @see https://testpalm.yandex-team.ru/testcase/courier-100
    it('Search by street', function () {
      const street = 'казанская';
      const orderNumber = 'streetSearchTest';

      cy.get(selectors.content.orders.filter.input).type(street).type('{enter}');
      cy.wait(1100);

      cy.get(selectors.content.orders.tableRows).invoke('text').should('include', orderNumber);
    });

    // @see https://testpalm.yandex-team.ru/testcase/courier-101
    it('Search by full address', function () {
      const fullAddress = 'Греция, Китнос, Хора 3';
      const orderNumber = 'fullAddressSearchTest';

      cy.get(selectors.content.orders.filter.input).type(fullAddress).type('{enter}');
      cy.wait(1100);

      cy.get(selectors.content.orders.tableRows).invoke('text').should('include', orderNumber);
    });
  });

  // @see https://testpalm.yandex-team.ru/testcase/courier-96
  describe('Search sequenced orders(is:sequenced)', function () {
    before(function () {
      cy.get(selectors.content.orders.filter.input).type('is:sequenced').type('{enter}');
      cy.waitForElement(selectors.content.orders.tableLoaded);
    });

    it('Tag is visible', function () {
      cy.get(selectors.content.orders.filter.tag).should('exist');
    });

    it('Search work', function () {
      cy.get(selectors.content.orders.tableRows).should('have.length', 11);
    });

    after(function () {
      cy.get(selectors.content.orders.filter.input).clear().type('{backspace}').clear();
    });
  });

  // @see https://testpalm.yandex-team.ru/testcase/courier-97
  describe('Search finished orders(is:finished)', function () {
    before(function () {
      cy.get(selectors.content.orders.filter.input).type('is:finished').type('{enter}');
      cy.waitForElement(selectors.content.orders.tableLoaded);
    });
    it('Tag is visible', function () {
      cy.get(selectors.content.orders.filter.tag).should('exist');
    });

    it('Search work', function () {
      cy.get(selectors.content.orders.tableRows).should('have.length', 1);
    });

    after(function () {
      cy.get(selectors.content.orders.filter.input).clear().type('{backspace}');
    });
  });

  // @see https://testpalm.yandex-team.ru/testcase/courier-98
  describe('Search cancelled orders(is:cancelled)', function () {
    before(function () {
      cy.get(selectors.content.orders.filter.input).type('is:cancelled').type('{enter}');
      cy.waitForElement(selectors.content.orders.tableLoaded);
    });
    it('Tag is visible', function () {
      cy.get(selectors.content.orders.filter.tag).should('exist');
    });

    it('Search work', function () {
      cy.get(selectors.content.orders.tableRows).should('have.length', 2);
    });

    after(function () {
      cy.get(selectors.content.orders.filter.input).clear().type('{backspace}');
    });
  });

  describe('Search cancelled order(is:cancelled) by number "TESTcancelled"', function () {
    before(function () {
      cy.get(selectors.content.orders.filter.input)
        .type('is:cancelled')
        .type('{enter}')
        .type('TESTcancelled');
      cy.waitForElement(selectors.content.orders.tableLoaded);
    });
    it('Tag is visible', function () {
      cy.get(selectors.content.orders.filter.tag).should('exist');
    });

    it('Search work', function () {
      cy.get(selectors.content.orders.tableRows).should('have.length', 1);
    });

    after(function () {
      cy.get(selectors.content.orders.filter.input).clear().type('{backspace}');
    });
  });

  // @see https://testpalm.yandex-team.ru/testcase/courier-69
  describe('After change date filter is not changed', function () {
    before(function () {
      cy.fixture('company-data').then(({ common }) => {
        const link = urls.ordersList.createLink(common.companyId, {
          filter: urls.ordersList.filters.isCancelled,
        });

        cy.yandexLogin('manager', { link });
        cy.waitForElement(selectors.content.orders.tableLoaded);
        cy.get(selectors.content.orders.dateFilter.days.yesterday).click();
      });
    });

    it('Tag not hide', function () {
      cy.get(selectors.content.orders.filter.tag).should('exist');
    });

    it('Search work', function () {
      cy.get(selectors.content.orders.tableRows).should('have.length', 3);
    });

    after(function () {
      cy.get(selectors.content.orders.filter.input).clear().type('{backspace}');
    });
  });

  describe('Value from url', function () {
    before(function () {
      cy.fixture('company-data').then(({ common }) => {
        const orderNumber = '11';
        const link = urls.ordersList.createLink(common.companyId, {
          date: time.TIME_TODAY,
          filter: orderNumber,
        });

        cy.yandexLogin('manager', { link });
        cy.waitForElement(selectors.content.orders.tableLoaded);
      });
    });

    // @TODO: Написать кейс в тестпалме про это
    it('Filter value from url', function () {
      const orderNumber = '11';
      cy.get(selectors.content.orders.tableRows).invoke('text').should('include', orderNumber);
    });

    // @see https://testpalm.yandex-team.ru/testcase/courier-84
    it('Orders list with search', function () {
      cy.get(selectors.content.orders.filter.input).clear();
      cy.get(selectors.content.orders.tableRows).should('have.length', 16);
    });

    // @see https://testpalm.yandex-team.ru/testcase/courier-83
    // skip until https://st.yandex-team.ru/BBGEO-12746
    it.skip('Search last order', function () {
      const orderNumber = '42';
      const date = moment(time.TIME_TODAY).add(3, 'days').format('DD.MM.YYYY');

      cy.get(selectors.content.orders.dateFilter.input).clear().type(date);
      cy.waitForElement(selectors.content.orders.tableLoaded);
      cy.get(selectors.content.orders.filter.input).type(orderNumber);
      cy.waitForElement(selectors.content.orders.tableLoaded);

      cy.get(selectors.content.orders.tableRows).invoke('text').should('include', orderNumber);
    });

    // after(function () {
    //   const date = moment(time.TIME_TODAY).format('DD.MM.YYYY');
    //   cy.get(selectors.content.orders.dateFilter.input).clear().type(date);
    //   cy.waitForElement(selectors.content.orders.tableLoaded);
    //   cy.get(selectors.content.orders.filter.input).clear().type('{backspace}').clear();
    //   cy.waitForElement(selectors.content.orders.tableLoaded);
    // });
  });

  describe('Order modal', function () {
    // @see https://testpalm.yandex-team.ru/testcase/courier-88
    it('Can open', function () {
      const orderNumber = 'fullAddressSearchTest';
      cy.get(selectors.content.orders.dateFilter.presetToday).click();
      cy.waitForElement(selectors.content.orders.tableLoaded);
      cy.get(selectors.content.orders.filter.input).type(orderNumber).type('{enter}').wait(2000);
      cy.waitForElement(selectors.content.orders.tableLoaded);

      cy.get(selectors.content.orders.tableRows).children().first().click();
      cy.get(selectors.modal.orderPopup.title).invoke('text').should('include', orderNumber);
    });

    after(function () {
      cy.get(selectors.modal.orderPopup.closeButton).click();
      cy.get(selectors.content.orders.filter.input).clear().type('{backspace}');
    });
  });

  describe('Order list fields are presented', function () {
    // @see https://testpalm.yandex-team.ru/courier/testcases/427
    before(function () {
      cy.fixture('company-data').then(({ someUpperCase }) => {
        const link = urls.dashboard.createLink(someUpperCase.companyId, {});
        cy.yandexLogin('admin', { link });
        cy.get(selectors.content.dashboard.view);
        cy.get(selectors.sidebar.menu.monitoringGroup)
          .click()
          .wait(50)
          .get(selectors.sidebar.menu.orders)
          .click();
        cy.waitForElement(selectors.content.orders.tableLoaded);
        cy.get(selectors.content.orders.columnSettings.opener).click();
        cy.get(selectors.content.orders.columnSettings.clearButton).click({ force: true });
        cy.get(selectors.content.orders.columnSettings.opener).click();
      });
    });

    it('Table fields are presented', function () {
      tableFieldKeys.forEach(key => {
        cy.get(selectors.content.orders.tableLoaded)
          .find(selectors.content.orders.tableHeader.field(key))
          .should('exist');
      });
    });
  });

  // @see https://testpalm.yandex-team.ru/courier/testcases/502
  describe('Order table functionality', () => {
    before(() => {
      cy.clearCookies();
      cy.yandexLogin('admin');
      cy.get(selectors.sidebar.menu.monitoringGroup)
        .click()
        .wait(50)
        .get(selectors.sidebar.menu.orders)
        .click();
    });

    it('Open table settings', () => {
      cy.get(selectors.content.orders.columnSettings.opener).click();

      cy.get(selectors.content.orders.columnSettings.clearButton)
        .should('be.visible')
        .should('be.disabled');
      tableFieldKeys.forEach(key => {
        cy.get(selectors.content.orders.columnSettings.checkbox(key))
          .scrollIntoView()
          .should('be.visible')
          .find('input')
          .should('be.checked');
      });
    });

    tableFieldKeys.forEach(key => {
      it(`Disable checkbox ${key}`, () => {
        cy.get(selectors.content.orders.columnSettings.checkbox(key))
          .scrollIntoView()
          .find('input')
          .uncheck();
        cy.get(selectors.content.orders.tableLoaded)
          .find(selectors.content.orders.tableHeader.field(key))
          .should('not.exist');
      });
    });

    it('There is no table now', () => {
      cy.get(selectors.content.orders.tableRows).should('not.exist');
    });

    it('Clear filter', () => {
      cy.get(selectors.content.orders.columnSettings.clearButton).click();

      tableFieldKeys.forEach(key => {
        cy.get(selectors.content.orders.tableLoaded)
          .find(selectors.content.orders.tableHeader.field(key))
          .should('exist');
      });

      cy.get(selectors.content.orders.columnSettings.clearButton)
        .should('be.visible')
        .should('be.disabled');
    });

    it('Move first field to third position', () => {
      cy.get(selectors.content.orders.columnSettings.dndWrap)
        .eq(0)
        .find(selectors.content.orders.columnSettings.checkbox(tableFieldKeys[0]))
        .should('exist');

      cy.get(selectors.content.orders.columnSettings.dndWrap)
        .eq(0)
        .dragToElement(selectors.content.orders.columnSettings.dndWrap + ':nth-child(4)');

      cy.get(selectors.content.orders.columnSettings.dndWrap)
        .eq(0)
        .find(selectors.content.orders.columnSettings.checkbox(tableFieldKeys[0]))
        .should('not.exist');

      cy.get(selectors.content.orders.columnSettings.dndWrap)
        .eq(0)
        .find(selectors.content.orders.columnSettings.checkbox(tableFieldKeys[2]))
        .should('exist');

      cy.get(selectors.content.orders.tableLoaded)
        .find(selectors.content.orders.tableHeader.anyField)
        .eq(0)
        .then($el => {
          cy.wrap($el.attr('data-test-anchor')).should('eq', `column-header--${tableFieldKeys[2]}`);
        });

      cy.get(selectors.content.orders.columnSettings.clearButton)
        .should('be.visible')
        .should('not.be.disabled');
    });

    it('Move fifth field to the first position', () => {
      cy.get(selectors.content.orders.columnSettings.dndWrap)
        .eq(5)
        .find(selectors.content.orders.columnSettings.checkbox(tableFieldKeys[5]))
        .should('exist');

      cy.get(selectors.content.orders.columnSettings.dndWrap)
        .eq(5)
        .dragToElement(selectors.content.orders.columnSettings.dndWrap + ':nth-child(2)');

      cy.get(selectors.content.orders.columnSettings.dndWrap)
        .eq(5)
        .find(selectors.content.orders.columnSettings.checkbox(tableFieldKeys[5]))
        .should('not.exist');

      cy.get(selectors.content.orders.columnSettings.dndWrap)
        .eq(0)
        .find(selectors.content.orders.columnSettings.checkbox(tableFieldKeys[5]))
        .should('exist');

      cy.get(selectors.content.orders.tableLoaded)
        .find(selectors.content.orders.tableHeader.anyField)
        .eq(0)
        .then($el => {
          cy.wrap($el.attr('data-test-anchor')).should('eq', `column-header--${tableFieldKeys[5]}`);
        });
    });

    it('Reloaded page have previous settings', () => {
      cy.reload();
      cy.waitForElement(selectors.content.orders.tableLoaded, { timeout: 10000 });

      cy.get(selectors.content.orders.columnSettings.clearButton).should('not.exist');

      cy.get(selectors.content.orders.tableLoaded)
        .find(selectors.content.orders.tableHeader.anyField)
        .eq(0)
        .then($el => {
          cy.wrap($el.attr('data-test-anchor')).should('eq', `column-header--${tableFieldKeys[5]}`);
        });
    });

    it('Clear settings', () => {
      cy.get(selectors.content.orders.columnSettings.opener).click();
      cy.get(selectors.content.orders.columnSettings.clearButton).click();
      cy.get(selectors.content.orders.columnSettings.opener).click();

      cy.get(selectors.content.orders.tableLoaded)
        .find(selectors.content.orders.tableHeader.anyField)
        .eq(0)
        .then($el => {
          cy.wrap($el.attr('data-test-anchor')).should('eq', `column-header--${tableFieldKeys[0]}`);
        });
    });

    it('Open popup', () => {
      cy.get(selectors.content.orders.tableLoaded)
        .find(selectors.content.orders.firstTableRow)
        .find(selectors.content.orders.tableCell)
        .eq(0)
        .click({ force: true });

      cy.get(selectors.modal.orderPopup.view).should('be.visible');
    });

    it('Close popup', () => {
      cy.get(selectors.modal.orderPopup.closeButton).click();

      cy.get(selectors.modal.orderPopup.view).should('not.exist');
    });

    it('Resize column', () => {
      cy.get(selectors.content.orders.tableLoaded)
        .find(selectors.content.orders.tableHeader.field(tableFieldKeys[0]))
        .closest(selectors.content.orders.tableHeader.container)
        .then($header => {
          const width = $header[0].offsetWidth;
          cy.wrap($header)
            .find(selectors.content.orders.tableHeader.dataGridResize)
            .then($drag => {
              const rect = $drag[0].getBoundingClientRect();
              const dragCenterX = rect.right - rect.width / 2;

              cy.wrap($drag)
                .trigger('mouseenter', { force: true })
                .trigger('mouseover', { force: true })
                .trigger('mousedown', {
                  button: 0,
                  pageY: rect.top + 1,
                  pageX: dragCenterX,
                  force: true,
                })
                .wait(10);
              cy.wrap($header)
                .trigger('mousemove', {
                  button: 0,
                  pageY: rect.top + 1,
                  pageX: dragCenterX + 30,
                  force: true,
                })
                .wait(10)
                .trigger('mouseup', {
                  button: 0,
                  pageY: rect.top + 1,
                  pageX: dragCenterX + 30,
                  force: true,
                })
                .then($header => {
                  cy.wrap(Math.round($header[0].offsetWidth))
                    .should('eq', Math.round(width + 30))
                    .wait(100);
                  cy.get(selectors.content.orders.tableRows)
                    .eq(0)
                    .find(selectors.content.orders.tableCell)
                    .eq(0)
                    .then($el => {
                      cy.wrap(Math.round($el[0].offsetWidth)).should('eq', Math.round(width + 30));
                    });
                });
            });
        });
    });

    it('Sort by number asc', () => {
      cy.get(selectors.content.orders.tableLoaded)
        .find(selectors.content.orders.tableHeader.field(tableFieldKeys[0]))
        .click();

      cy.get(selectors.content.orders.tableLoaded)
        .find(selectors.content.orders.tableHeader.field(tableFieldKeys[0]))
        .closest(selectors.content.orders.tableHeader.dataGridAscSelector)
        .should('be.visible');

      cy.get(selectors.content.orders.tableLoaded)
        .find(selectors.content.orders.tableRows)
        .then($rows => {
          const numbers = map(
            $rows,
            row => row.querySelectorAll(selectors.content.orders.tableCell)[0].textContent || '',
          );
          const sortedNumbers = [...numbers].sort((a, b) => {
            return a.localeCompare(b);
          });

          expect(numbers).to.deep.equal(sortedNumbers);
        });
    });

    it('Sort by number desc', () => {
      cy.get(selectors.content.orders.tableLoaded)
        .find(selectors.content.orders.tableHeader.field(tableFieldKeys[0]))
        .click();

      cy.get(selectors.content.orders.tableLoaded)
        .find(selectors.content.orders.tableHeader.field(tableFieldKeys[0]))
        .closest(selectors.content.orders.tableHeader.dataGridDescSelector)
        .should('be.visible');

      cy.get(selectors.content.orders.tableLoaded)
        .find(selectors.content.orders.tableRows)
        .then($rows => {
          const numbers = map(
            $rows,
            row => row.querySelectorAll(selectors.content.orders.tableCell)[0].textContent || '',
          );
          const sortedNumbers = [...numbers].sort((a, b) => {
            return -1 * a.localeCompare(b);
          });

          expect(numbers).to.deep.equal(sortedNumbers);
        });
    });

    it('Sort by number disable', () => {
      cy.get(selectors.content.orders.tableLoaded)
        .find(selectors.content.orders.tableHeader.field(tableFieldKeys[0]))
        .click();

      cy.get(selectors.content.orders.tableLoaded)
        .find(selectors.content.orders.tableHeader.field(tableFieldKeys[0]))
        .closest(selectors.content.orders.tableHeader.dataGridDescSelector)
        .should('not.exist');

      cy.get(selectors.content.orders.tableLoaded)
        .find(selectors.content.orders.tableHeader.field(tableFieldKeys[0]))
        .closest(selectors.content.orders.tableHeader.dataGridAscSelector)
        .should('not.exist');
    });
  });

  describe('Go to dashboard', function () {
    // @see https://testpalm.yandex-team.ru/testcase/courier-70
    it('Return from dashboard', function () {
      cy.get(selectors.content.orders.dateFilter.presetTomorrow)
        .click()
        .waitForElement(selectors.content.orders.tableLoaded);
      cy.get(selectors.sidebar.menu.monitoringGroup).click();
      cy.get(selectors.sidebar.menu.dashboard).click();
      cy.get(selectors.content.dashboard.view);
      cy.get(selectors.content.dashboard.dayTitle)
        .invoke('text')
        .should('eq', commonKeyset.ru.dateRangeFilter_tomorrow);
    });

    after(function () {
      cy.get(selectors.sidebar.menu.orders).click();
      cy.get(selectors.sidebar.menu.monitoringGroup).click();
      cy.get(selectors.content.orders.dateFilter.presetToday).click();
    });
  });
});
