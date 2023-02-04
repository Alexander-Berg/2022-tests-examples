import * as courierQualityReportKeyset from '../../../../src/translations/courier-quality-report';
import * as importKeyset from '../../../../src/translations/import';
import selectors from '../../../src/constants/selectors';
import { courierNameRecord } from '../../../src/constants/couriers';
import { routeNumberRecord } from '../../../src/constants/routes';

const tableFieldKeys = [
  'type',
  'order_number',
  'order_address',
  'suggested_order_number',
  'not_in_order',
  'customer_name',
  'order_status',
  'courier_name',
  'route_number',
  'route_routing_mode',
  'arrived_at',
  'order_visited_at',
  'left_at',
  'used_mark_delivered_radius',
  'order_completed_at',
  'order_interval',
  'order_interval_time',
  'time_interval_error',
  'courier_number',
  'air_distance',
  'order_confirmed_at',
  'late_call_before_delivery',
  'depot_number',
  'segment_distance_m',
  'time_at_point',
  'transit_idle_duration',
  'location_idle_duration',
  'date',
  'order_lat',
  'order_lon',
  'refined_lat',
  'refined_lon',
  'order_comments',
  'order_failedTimeWindowHow',
  'order_weight',
  'order_volume',
  'order_status_new_comments',
  'order_status_confirmed_comments',
  'order_status_finished_comments',
  'order_status_partially_finished_comments',
  'order_status_cancelled_comments',
  'order_status_postponed_comments',
  'route_imei',
];

// https://testpalm.yandex-team.ru/courier/testsuite/5fda1df8c79113008cf8a0db?testcase=297
describe('Check courier quality reports fields', () => {
  before(() => {
    cy.yandexLogin('admin');
  });

  it('should open a courier quality report', () => {
    cy.waitForElement(selectors.sidebar.menu.reports).click();
    cy.get(selectors.sidebar.menu.reportsItems.courierQualityReport).click();
  });

  it('check toolbox elements', () => {
    cy.get(selectors.courierQualityReport.searchField).should('exist');

    cy.get(selectors.courierQualityReport.datePickerStart).should('exist');
    cy.get(selectors.courierQualityReport.datePickerEnd).should('exist');

    cy.get(
      `${selectors.courierQualityReport.datePickerStart} ${selectors.courierQualityReport.datePickerPresets.dayBeforeYesterday}`,
    ).should('exist');
    cy.get(
      `${selectors.courierQualityReport.datePickerStart} ${selectors.courierQualityReport.datePickerPresets.today}`,
    ).should('exist');
    cy.get(
      `${selectors.courierQualityReport.datePickerStart} ${selectors.courierQualityReport.datePickerPresets.yesterday}`,
    ).should('exist');

    cy.get(
      `${selectors.courierQualityReport.datePickerEnd} ${selectors.courierQualityReport.datePickerPresets.dayBeforeYesterday}`,
    ).should('exist');
    cy.get(
      `${selectors.courierQualityReport.datePickerStart} ${selectors.courierQualityReport.datePickerPresets.today}`,
    ).should('exist');
    cy.get(
      `${selectors.courierQualityReport.datePickerStart} ${selectors.courierQualityReport.datePickerPresets.yesterday}`,
    ).should('exist');

    cy.get(selectors.courierQualityReport.datePickerButton).should('exist').should('be.disabled');

    cy.get(selectors.courierQualityReport.downloadXLSXButton).should('exist');

    cy.get(selectors.courierQualityReport.table).should('exist');
  });

  it('check table fields', () => {
    tableFieldKeys.forEach(column => {
      cy.get(selectors.courierQualityReport.table)
        .find(selectors.courierQualityReport.tableColumn(column))
        .should('exist');
    });
  });
});

describe('Check table functionality', () => {
  // @see https://testpalm.yandex-team.ru/courier/testcases/503
  before(() => {
    cy.yandexLogin('admin');
    cy.waitForElement(selectors.sidebar.menu.reports).click();
    cy.get(selectors.sidebar.menu.reportsItems.courierQualityReport).click();
    cy.get(selectors.courierQualityReport.columnSettings.opener).click();
    cy.get(selectors.courierQualityReport.columnSettings.clearButton).then($button => {
      if (!$button.attr('disabled')) {
        cy.wrap($button).click();
      }
    });
    cy.get(selectors.courierQualityReport.columnSettings.opener).click();
  });

  it('Open table settings', () => {
    cy.get(selectors.courierQualityReport.columnSettings.opener).click();

    cy.get(selectors.courierQualityReport.columnSettings.clearButton)
      .should('be.visible')
      .should('be.disabled');

    tableFieldKeys.forEach(key => {
      cy.get(selectors.courierQualityReport.columnSettings.checkbox(key))
        .scrollIntoView()
        .should('be.visible')
        .find('input')
        .should('be.checked');
    });
  });

  tableFieldKeys.forEach(key => {
    it(`Disable checkbox ${key}`, () => {
      cy.get(selectors.courierQualityReport.columnSettings.checkbox(key))
        .scrollIntoView()
        .find('input')
        .uncheck();
      cy.get(selectors.courierQualityReport.table)
        .find(selectors.courierQualityReport.tableHeader.field(key))
        .should('not.exist');
    });
  });

  it('Clear filter', () => {
    cy.get(selectors.courierQualityReport.columnSettings.clearButton).click();

    tableFieldKeys.forEach(key => {
      cy.get(selectors.courierQualityReport.table)
        .find(selectors.courierQualityReport.tableHeader.field(key))
        .should('exist');
    });

    cy.get(selectors.courierQualityReport.columnSettings.clearButton)
      .should('be.visible')
      .should('be.disabled');
  });

  it('Move first field to third position', () => {
    cy.get(selectors.courierQualityReport.columnSettings.dndWrap)
      .eq(0)
      .find(selectors.courierQualityReport.columnSettings.checkbox(tableFieldKeys[0]))
      .should('exist');

    cy.get(selectors.courierQualityReport.columnSettings.dndWrap)
      .eq(0)
      .dragToElement(selectors.courierQualityReport.columnSettings.dndWrap + ':nth-child(4)');

    cy.get(selectors.courierQualityReport.columnSettings.dndWrap)
      .eq(0)
      .find(selectors.courierQualityReport.columnSettings.checkbox(tableFieldKeys[0]))
      .should('not.exist');

    cy.get(selectors.courierQualityReport.columnSettings.dndWrap)
      .eq(0)
      .find(selectors.courierQualityReport.columnSettings.checkbox(tableFieldKeys[2]))
      .should('exist');

    cy.get(selectors.courierQualityReport.table)
      .find(selectors.courierQualityReport.tableHeader.anyField)
      .eq(0)
      .should('have.class', `columnName_${tableFieldKeys[2]}`);

    cy.get(selectors.courierQualityReport.columnSettings.clearButton)
      .should('be.visible')
      .should('not.be.disabled');
  });

  it('Move fifth field to the first position', () => {
    cy.get(selectors.courierQualityReport.columnSettings.dndWrap)
      .eq(5)
      .find(selectors.courierQualityReport.columnSettings.checkbox(tableFieldKeys[5]))
      .should('exist');

    cy.get(selectors.courierQualityReport.columnSettings.dndWrap)
      .eq(5)
      .dragToElement(selectors.courierQualityReport.columnSettings.dndWrap + ':nth-child(2)');

    cy.get(selectors.courierQualityReport.columnSettings.dndWrap)
      .eq(5)
      .find(selectors.courierQualityReport.columnSettings.checkbox(tableFieldKeys[5]))
      .should('not.exist');

    cy.get(selectors.courierQualityReport.columnSettings.dndWrap)
      .eq(0)
      .find(selectors.courierQualityReport.columnSettings.checkbox(tableFieldKeys[5]))
      .should('exist');

    cy.get(selectors.courierQualityReport.table)
      .find(selectors.courierQualityReport.tableHeader.anyField)
      .eq(0)
      .should('have.class', `columnName_${tableFieldKeys[5]}`);
  });

  it('Reloaded page have previous settings', () => {
    cy.reload();
    cy.waitForElement(selectors.courierQualityReport.table, { timeout: 10000 });

    cy.get(selectors.courierQualityReport.columnSettings.clearButton).should('not.exist');

    cy.get(selectors.courierQualityReport.table)
      .find(selectors.courierQualityReport.tableHeader.anyField)
      .eq(0)
      .should('have.class', `columnName_${tableFieldKeys[5]}`);
  });

  it('Search', () => {
    cy.get(selectors.courierQualityReport.tableRows)
      .eq(1)
      .find(selectors.courierQualityReport.tableCell)
      .eq(1)
      .click() // ничего не открылось после клика
      .invoke('text')
      .then(text => {
        cy.get(selectors.courierQualityReport.searchField).clear().type(text);

        cy.get(selectors.courierQualityReport.tableRows)
          .should('have.length', 1)
          .eq(0)
          .find(selectors.courierQualityReport.tableCell)
          .eq(1)
          .should('have.text', text);
      });
  });

  after('Clear filter', () => {
    cy.get(selectors.courierQualityReport.searchField).clear();
    cy.get(selectors.courierQualityReport.columnSettings.opener).click();
    cy.get(selectors.courierQualityReport.columnSettings.clearButton).click({ force: true });
  });
});
// @see https://testpalm.yandex-team.ru/courier/testcases/294
describe('Search', () => {
  afterEach(() => {
    cy.get(selectors.courierQualityReport.searchField).clear();
  });

  it('should display results on search by order', () => {
    const orderNumber = 'phoneNumberTest';
    const search = orderNumber.substr(0, 3);

    cy.get(selectors.courierQualityReport.searchField).type(search);
    cy.waitFor(selectors.courierQualityReport.preloader);
    cy.get(selectors.courierQualityReport.preloader).should('not.exist');
    cy.contains(selectors.courierQualityReport.tableColumn('order_number'), orderNumber, {
      matchCase: false,
    }).should('be.visible');
    cy.get(selectors.courierQualityReport.tableRow).each($row => {
      expect($row.text().toLowerCase()).contains(search.toLowerCase());
    });
  });

  it('should display results on search by customer', () => {
    const customer = 'Bowser';
    const search = customer.substr(0, 3);

    cy.get(selectors.courierQualityReport.searchField).type(search);
    cy.waitFor(selectors.courierQualityReport.preloader);
    cy.get(selectors.courierQualityReport.preloader).should('not.exist');
    cy.contains(selectors.courierQualityReport.tableColumn('customer_name'), customer, {
      matchCase: false,
    }).should('be.visible');
    cy.get(selectors.courierQualityReport.tableRow).each($row => {
      expect($row.text().toLowerCase()).contains(search.toLowerCase());
    });
  });

  it('should display results on search by courier name', () => {
    const courier = courierNameRecord.gumba;
    const search = courier.substr(0, 3);

    cy.get(selectors.courierQualityReport.searchField).type(search);
    cy.waitFor(selectors.courierQualityReport.preloader);
    cy.get(selectors.courierQualityReport.preloader).should('not.exist');
    cy.contains(selectors.courierQualityReport.tableColumn('courier_name'), courier, {
      matchCase: false,
    }).should('be.visible');
    cy.get(selectors.courierQualityReport.tableRow).each($row => {
      expect($row.text().toLowerCase()).contains(search.toLowerCase());
    });
  });

  it('should display results on search by route number', () => {
    const route = routeNumberRecord.TODAY;
    const search = route.substr(0, 3);

    cy.get(selectors.courierQualityReport.searchField).type(search);
    cy.waitFor(selectors.courierQualityReport.preloader);
    cy.get(selectors.courierQualityReport.preloader).should('not.exist');
    cy.contains(selectors.courierQualityReport.tableColumn('route_number'), route, {
      matchCase: false,
    }).should('exist');
    cy.get(selectors.courierQualityReport.tableRow).each($row => {
      expect($row.text().toLowerCase()).contains(search.toLowerCase());
    });
  });

  it('should display results on search by status', () => {
    const status = importKeyset.ru.params_initialStatus_confirmed;
    const search = status;

    cy.get(selectors.courierQualityReport.searchField).type(search);
    cy.waitFor(selectors.courierQualityReport.preloader);
    cy.get(selectors.courierQualityReport.preloader).should('not.exist');
    cy.contains(selectors.courierQualityReport.tableColumn('order_status'), status, {
      matchCase: false,
    }).should('exist');
    cy.get(selectors.courierQualityReport.tableRow).each($row => {
      expect($row.text().toLowerCase()).contains(search.toLowerCase());
    });
  });

  it('should display results on search by address', () => {
    const address = 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2';
    const search = address.substr(0, 3);

    cy.get(selectors.courierQualityReport.searchField).type(search);
    cy.waitFor(selectors.courierQualityReport.preloader);
    cy.get(selectors.courierQualityReport.preloader).should('not.exist');
    cy.contains(selectors.courierQualityReport.tableColumn('order_address'), address, {
      matchCase: false,
    }).should('exist');
    cy.get(selectors.courierQualityReport.tableRow).each($row => {
      expect($row.text().toLowerCase()).contains(search.toLowerCase());
    });
  });

  it('should display results on search by address (with spaces)', () => {
    const address = 'Греция, Китнос, Хора 3';
    const search = address.substr(0, 3);

    cy.get(selectors.courierQualityReport.searchField).type('     ' + search + '     ');
    cy.waitFor(selectors.courierQualityReport.preloader);
    cy.get(selectors.courierQualityReport.preloader).should('not.exist');
    cy.contains(selectors.courierQualityReport.tableColumn('order_address'), address, {
      matchCase: false,
    }).should('exist');
    cy.get(selectors.courierQualityReport.tableRow).each($row => {
      expect($row.text().toLowerCase()).contains(search.toLowerCase());
    });
  });

  it('should not display results on search by non-searchable data', () => {
    const search = '59.943557';
    const noDataText = courierQualityReportKeyset.ru.emptyResult;

    cy.get(selectors.courierQualityReport.searchField).type(search);
    cy.waitFor(selectors.courierQualityReport.preloader);
    cy.get(selectors.courierQualityReport.preloader).should('not.exist');
    cy.contains(selectors.courierQualityReport.tableColumn('order_address'), search, {
      matchCase: false,
    }).should('not.exist');
    cy.get(selectors.courierQualityReport.noData).should('be.visible').and('have.text', noDataText);
  });
});
