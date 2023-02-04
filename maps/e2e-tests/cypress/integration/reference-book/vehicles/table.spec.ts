import selectors from '../../../../src/constants/selectors';
import * as vehiclesReferenceBookKeyset from '../../../../../src/translations/vehicles-reference-book';

import map from 'lodash/map';

const vehiclesRefBookPage = {
  title: {
    ru: vehiclesReferenceBookKeyset.ru.tableTitle,
  },
  sizeTable: {
    ru: vehiclesReferenceBookKeyset.ru.size.many,
  },
};

const tableFieldKeys = [
  'name',
  'number',
  'routing_mode',
  'zones\\.allowed',
  'zones\\.forbidden',
  'parameters\\.width',
  'parameters\\.length',
  'parameters\\.height',
  'capacity\\.weight',
  'capacity\\.width',
  'capacity\\.depth',
  'capacity\\.height',
  'capacity\\.units',
  'shifts\\.0\\.balanced_group_id',
];

// @see https://testpalm.yandex-team.ru/courier/testcases/659

describe('check of the table in the Vehicle Reference Book)', function () {
  before(function () {
    cy.yandexLogin('refBookManager');
    cy.preserveCookies();
    cy.clearLocalforage();
    cy.openAndCloseVideo(); // если есть видео, то закрыть, если нет то ничего
    cy.get(selectors.sidebar.menu.collections).click();
    cy.get(selectors.sidebar.menu.vehiclesReferenceBook).click();
    cy.get(selectors.modal.closeButton, { timeout: 10000 }).click();
  });

  const {
    title,
    addVehicle,
    downloadXLSButton,
    tableSize,
    table,
    tableColumn,
    tableCell,
    tableRows,
  } = selectors.content.referenceBook.vehicles;
  const { opener, clearButton, checkbox } = selectors.content.referenceBook.vehicles.columnSettings;
  const { container, dataGridResize, field, dataGridDescSelector, dataGridAscSelector } =
    selectors.content.referenceBook.vehicles.tableHeader;

  it('сhecking the elements on the page', () => {
    cy.get(title).should('be.visible').and('have.text', vehiclesRefBookPage.title.ru);

    cy.get(opener).should('be.visible').and('not.be.disabled');
    cy.get(addVehicle).should('be.visible').and('not.be.disabled');
    cy.get(downloadXLSButton).should('be.visible').and('not.be.disabled');

    cy.get(tableSize)
      .should('be.visible')
      .and('have.text', `16 ${vehiclesRefBookPage.sizeTable.ru.slice(8)}`);
  });

  it('Open table settings', () => {
    cy.get(opener).click();

    cy.get(clearButton).should('be.visible').should('be.disabled');

    tableFieldKeys.forEach(key => {
      cy.get(checkbox(key))
        .scrollIntoView()
        .should('be.visible')
        .find('input')
        .should('be.checked');
    });

    tableFieldKeys.forEach(key => {
      cy.get(checkbox(key)).scrollIntoView().find('input').uncheck();
      cy.get(table).find(field(key)).should('not.exist');
    });
  });

  it('Clear table settings', () => {
    cy.get(clearButton).should('be.visible').click();

    tableFieldKeys.forEach(column => {
      cy.get(table).find(tableColumn(column)).should('exist');
    });
  });

  it('Resize column', () => {
    cy.get(table)
      .find(field(tableFieldKeys[0]))
      .closest(container)
      .then($header => {
        const width = $header[0].offsetWidth;
        cy.wrap($header)
          .find(dataGridResize)
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
                cy.get(tableRows)
                  .eq(0)
                  .find(tableCell)
                  .eq(0)
                  .then($el => {
                    cy.wrap(Math.round($el[0].offsetWidth)).should('eq', Math.round(width + 30));
                  });
              });
          });
      });
  });

  it('Sort by number asc', () => {
    cy.get(table).find(field(tableFieldKeys[0])).click();

    cy.get(table).find(field(tableFieldKeys[0])).find(dataGridAscSelector).should('be.visible');

    cy.get(table)
      .find(tableRows)
      .then($rows => {
        const names = map($rows, row => row.querySelectorAll(tableCell)[0].textContent || '');
        const sortedNumbers = [...names].sort((a, b) => {
          return a.localeCompare(b);
        });

        expect(names).to.deep.equal(sortedNumbers);
      });
  });

  it('Sort by number desc', () => {
    cy.get(table).find(field(tableFieldKeys[0])).click();

    cy.get(table).find(field(tableFieldKeys[0])).find(dataGridDescSelector).should('be.visible');

    cy.get(table)
      .find(tableRows)
      .then($rows => {
        const names = map($rows, row => row.querySelectorAll(tableCell)[0].textContent || '');
        const sortedNumbers = [...names].sort((a, b) => {
          return -1 * a.localeCompare(b);
        });

        expect(names).to.deep.equal(sortedNumbers);
      });
  });

  it('Sort by number disable', () => {
    cy.get(table).find(field(tableFieldKeys[0])).click();

    cy.get(table).find(field(tableFieldKeys[0])).find(dataGridDescSelector).should('not.exist');

    cy.get(table).find(field(tableFieldKeys[0])).find(dataGridAscSelector).should('not.exist');
  });
});
