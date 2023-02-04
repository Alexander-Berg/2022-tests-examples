import * as courierQualityReportKeyset from '../../../../src/translations/courier-quality-report';
import time from '../../../src/utils/time';
import selectors from '../../../src/constants/selectors';
import urls from '../../../src/utils/urls';
import parseISO from 'date-fns/parseISO';
import dateFnsFormat from '../../../src/utils/date-fns-format';
import values from 'lodash/values';

enum RoutingModeEnum {
  DRIVING = 'driving',
  WALKING = 'walking',
  TRANSIT = 'transit',
  TRUCK = 'truck',
}

const ROUTING_MODE_CAPTION = courierQualityReportKeyset.ru.column_routeRoutingMode;

// @see https://testpalm.yandex-team.ru/testcase/courier-994
// @see https://testpalm.yandex-team.ru/testcase/courier-505
describe('Check downloaded courier quality report (excel)', function () {
  beforeEach(() => {
    cy.preserveCookies();
    cy.get(selectors.courierQualityReport.columnSettings.opener).click();
    cy.get(selectors.courierQualityReport.columnSettings.clearButton).click({ force: true });
  });

  before(function () {
    cy.fixture('company-data').then(({ common }) => {
      const link = urls.ordersList.createLink(common.companyId, {});

      cy.yandexLogin('manager', { link });
      cy.waitForElement(selectors.sidebar.menu.reports).click();
      cy.get(selectors.sidebar.menu.reportsItems.courierQualityReport).click();
    });
  });

  after(() => {
    cy.deleteDownloadsFolder();
  });

  const date = parseISO(time.TIME_TODAY);
  const dateStr = dateFnsFormat(date, 'yyyy-MM-dd');
  const generatedFileName = `couriers-report-${dateStr}-${dateStr}.xlsx`;

  it('The report must match the appearance of the response in the version when no changes are made', function () {
    cy.get(selectors.courierQualityReport.downloadXLSXButton).click();
    // wait download
    cy.wait(1000);
    cy.get(selectors.courierQualityReport.tableHeaders)
      .then($els => {
        return Cypress._.map(Cypress.$.makeArray($els), 'innerText');
      })
      .validateRowInExcelFile(generatedFileName, 0);
  });

  it('The transport column should be presented in the table', () => {
    cy.get(selectors.courierQualityReport.tableHead)
      .find(selectors.courierQualityReport.tableColumn('route_routing_mode'))
      // на старой таблице не получится скроллить элемент в шапке, поэтому и видимость проверить тоже нельзя
      .should('have.text', ROUTING_MODE_CAPTION);
  });

  it('All routing modes should be presented', () => {
    cy.get(selectors.courierQualityReport.tableBody)
      .find(selectors.courierQualityReport.tableCellRoutingMode)
      .then($el => {
        const routingModes = new Set(values(RoutingModeEnum));
        $el.each(function () {
          const routingMode = (this.getAttribute('value') || '') as any;
          cy.wrap(routingMode).should('not.eq', '');
          routingModes.delete(routingMode);
        });
        cy.wrap(routingModes.size).should('eq', 0);
      });
  });

  it('The routing mode column in xls same as a column at the page', function () {
    cy.get(selectors.courierQualityReport.downloadXLSXButton).click();
    // wait download
    cy.wait(1000);
    cy.get(selectors.courierQualityReport.tableHeaders).then($els => {
      let columnIndex = -1;
      $els.each(function (index) {
        if (this.innerText === ROUTING_MODE_CAPTION) {
          columnIndex = index;
        }
      });

      cy.wrap(columnIndex).should('be.gt', -1);
      cy.get(selectors.courierQualityReport.tableCellRoutingMode)
        .then($els => {
          return Cypress._.map(Cypress.$.makeArray($els), 'innerText');
        })
        .validateColumnInExcelFile(generatedFileName, columnIndex);
    });
  });

  it('The report should be empty when all columns are hidden', function () {
    cy.get('.setting-row__disable').each($el => {
      cy.wrap($el).click();
    });

    cy.get(selectors.courierQualityReport.downloadXLSXButton).click();
    // wait download
    cy.wait(1000);

    cy.get(selectors.courierQualityReport.tableHeaders)
      .should('not.exist')
      .then($els => {
        return Cypress._.map(Cypress.$.makeArray($els), 'innerText');
      })
      .validateRowInExcelFile(generatedFileName, 0);
  });

  it('The report must match the column order in the web version, when the first column is shifted to the bottom', function () {
    cy.get('.dnd-wrap').first().dragToElement('.dnd-wrap:last-child');
    cy.get(selectors.courierQualityReport.downloadXLSXButton).click();
    // wait download
    cy.wait(1000);
    cy.get(selectors.courierQualityReport.tableHeaders)
      .then($els => {
        return Cypress._.map(Cypress.$.makeArray($els), 'innerText');
      })
      .validateRowInExcelFile(generatedFileName, 0);
  });

  it('The report must match the column order in the web version, when the last column is shifted to the top', function () {
    cy.get('.dnd-wrap').last().dragToElement('.dnd-wrap:first');
    cy.get(selectors.courierQualityReport.downloadXLSXButton).click();
    // wait download
    cy.wait(1000);
    cy.get(selectors.courierQualityReport.tableHeaders)
      .then($els => {
        return Cypress._.map(Cypress.$.makeArray($els), 'innerText');
      })
      .validateRowInExcelFile(generatedFileName, 0);
  });

  it('The report must match the column order in the web version, when the last column is shifted to the third place from the bottom', function () {
    cy.get('.dnd-wrap').last().dragToElement('.dnd-wrap:nth-last-child(-n+4)');
    cy.get(selectors.courierQualityReport.downloadXLSXButton).click();
    // wait download
    cy.wait(1000);
    cy.get(selectors.courierQualityReport.tableHeaders)
      .then($els => {
        return Cypress._.map(Cypress.$.makeArray($els), 'innerText');
      })
      .validateRowInExcelFile(generatedFileName, 0);
  });

  it('The report must match the column order in the web version when the column widths are changed', function () {
    cy.get('.react-grid-HeaderCell__draggable')
      .first()
      .trigger('mousedown', { which: 1 })
      .trigger('mousemove', 500, 0, { force: true })
      .trigger('mouseup', { force: true });
    cy.get('.react-grid-HeaderCell__draggable')
      .last()
      .trigger('mousedown', { which: 1 })
      .trigger('mousemove', 100, 0, { force: true })
      .trigger('mouseup', { force: true });

    cy.get(selectors.courierQualityReport.downloadXLSXButton).click();
    // wait download
    cy.wait(1000);

    cy.get(selectors.courierQualityReport.tableHeaders)
      .then($els => {
        return Cypress._.map(Cypress.$.makeArray($els), 'innerText');
      })
      .validateRowInExcelFile(generatedFileName, 0);
  });
});
