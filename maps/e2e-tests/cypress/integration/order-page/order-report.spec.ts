import time from '../../../src/utils/time';
import selectors from '../../../src/constants/selectors';

import urls from '../../../src/utils/urls';
import parseISO from 'date-fns/parseISO';
import dateFnsFormat from '../../../src/utils/date-fns-format';

// @see https://testpalm.yandex-team.ru/testcase/courier-993
describe('Download orders report (excel)', function () {
  beforeEach(() => {
    cy.preserveCookies();
    cy.get(selectors.content.orders.columnSettings.opener).click();
    cy.get(selectors.content.orders.columnSettings.clearButton).click({ force: true });
  });

  before(function () {
    cy.fixture('company-data').then(({ common }) => {
      const link = urls.ordersList.createLink(common.companyId, {});

      cy.yandexLogin('manager', { link });
      cy.waitForElement(selectors.content.orders.tableLoaded);
    });
  });

  after(() => {
    cy.deleteDownloadsFolder();
  });

  const date = parseISO(time.TIME_TODAY);
  const dateStr = dateFnsFormat(date, 'yyyy-MM-dd');
  const generatedFileName = `orders-${dateStr}.xlsx`;

  it('The report must match the appearance of the response in the version when no changes are made', function () {
    cy.get(selectors.content.orders.downloadBtn).click();
    // wait download
    cy.wait(1000);
    cy.get(selectors.content.orders.tableHeader.headerTitle)
      .then($els => {
        return Cypress._.map(Cypress.$.makeArray($els), 'innerText');
      })
      .validateRowInExcelFile(generatedFileName, 0);
  });

  it('The report should be empty when all columns are hidden', function () {
    cy.get('.setting-row__disable').each($el => {
      cy.wrap($el).click();
    });

    cy.get(selectors.content.orders.downloadBtn).click();
    // wait download
    cy.wait(1000);
    cy.get(selectors.content.orders.tableHeader.headerTitle)
      .should('not.exist')
      .then($els => {
        return Cypress._.map(Cypress.$.makeArray($els), 'innerText');
      })
      .validateRowInExcelFile(generatedFileName, 0);
  });

  it('The report must match the column order in the web version, when the first column is shifted to the bottom', function () {
    cy.get('.dnd-wrap').first().dragToElement('.dnd-wrap:last-child');
    cy.get(selectors.content.orders.downloadBtn).click();
    // wait download
    cy.wait(1000);
    cy.get(selectors.content.orders.tableHeader.headerTitle)
      .then($els => {
        return Cypress._.map(Cypress.$.makeArray($els), 'innerText');
      })
      .validateRowInExcelFile(generatedFileName, 0);
  });

  it('The report must match the column order in the web version, when the last column is shifted to the top', function () {
    cy.get('.dnd-wrap').last().dragToElement('.dnd-wrap:first');
    cy.get(selectors.content.orders.downloadBtn).click();
    // wait download
    cy.wait(1000);
    cy.get(selectors.content.orders.tableHeader.headerTitle)
      .then($els => {
        return Cypress._.map(Cypress.$.makeArray($els), 'innerText');
      })
      .validateRowInExcelFile(generatedFileName, 0);
  });

  it('The report must match the column order in the web version when, the last column is shifted to the third place from the bottom', function () {
    cy.get('.dnd-wrap').last().dragToElement('.dnd-wrap:nth-last-child(-n+4)');
    cy.get(selectors.content.orders.downloadBtn).click();
    // wait download
    cy.wait(1000);
    cy.get(selectors.content.orders.tableHeader.headerTitle)
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

    cy.get(selectors.content.orders.downloadBtn).click();
    // wait download
    cy.wait(1000);

    cy.get(selectors.content.orders.tableHeader.headerTitle)
      .then($els => {
        return Cypress._.map(Cypress.$.makeArray($els), 'innerText');
      })
      .validateRowInExcelFile(generatedFileName, 0);
  });
});
