import * as vehiclesReferenceBookKeyset from '../../../../src/translations/vehicles-reference-book';
import * as importKeyset from '../../../../src/translations/import';
import selectors from '../../../src/constants/selectors';
import { addressInputOnMapClearSharedSpec } from '../shared/address-input-clear.spec';
import each from 'lodash/each';

const EXCEL_FILE_NAME = 'planning2.xlsx';
const EXCEL_FILE_NAME2 = 'G1monitoring.xlsx';
// this time is enough to upload and parse an excel file
// if a file isn't uploaded during this time, then something went wrong'
const EXCEL_FILE_UPLOAD_TIMEOUT = 360000;

const TEXT_SET = ['Без координат1', 'Приблизительно3', 'С координатами51'];
const SEARCH_COORDS_BUTTON_TEXT = importKeyset.ru.orderCoordinatesWarning_action;

const table = {
  selectors: {
    selectedCell: '[data-test="cell-mask"]',
    table: {
      headers: '.react-grid-HeaderCell-sortable',
      bodyToScrool: '.react-contextmenu-wrapper > :nth-child(1)',
      sortIcon: '.pull-right',
      warning: '.group__head-count',
      contextMenu: {
        view: '.react-contextmenu--visible',
        menu: {
          sort: {
            view: '.react-contextmenu--visible > :nth-child(1)',
            subMenu: {
              asc: '.react-contextmenu--visible > :nth-child(1) > :nth-child(2) > :nth-child(1)',
              desc: '.react-contextmenu--visible > :nth-child(1) > :nth-child(2) > :nth-child(2)',
            },
          },
          deleteRow: '.react-contextmenu--visible > :nth-child(2)',
          appendRow: {
            view: '.react-contextmenu--visible > :nth-child(3)',
            subMenu: {
              toUp: '.react-contextmenu--visible > :nth-child(3) > :nth-child(2) > :nth-child(1)',
              toDown: '.react-contextmenu--visible > :nth-child(3) > :nth-child(2) > :nth-child(2)',
            },
          },
          dublicateRow: '.react-contextmenu--visible > :nth-child(4)',
        },
      },
    },
    vehicles: {
      vehicleNumber: {
        header: '.react-grid-Main .react-grid-Header .react-grid-HeaderCell:nth-child(1)',
        firstCell: '.react-grid-Main .react-grid-Row:nth-child(1) .react-grid-Cell:nth-child(1)',
        secondCell: '.react-grid-Main .react-grid-Row:nth-child(2) .react-grid-Cell:nth-child(1)',
        thirdCell: '.react-grid-Main .react-grid-Row:nth-child(3) .react-grid-Cell:nth-child(1)',
        seventhCell: '.react-grid-Main .react-grid-Row:nth-child(7) .react-grid-Cell:nth-child(1)',
      },
      capacity: {
        header: '.react-grid-Main .react-grid-Header .react-grid-HeaderCell:nth-child(7)',
        firstCell: '.react-grid-Main .react-grid-Row:nth-child(1) .react-grid-Cell:nth-child(7)',
        secondCell: '.react-grid-Main .react-grid-Row:nth-child(2) .react-grid-Cell:nth-child(7)',
      },
    },
  },
  data: {
    getRowElements: (rowNumber: number): Cypress.Chainable<JQuery<HTMLElement>> =>
      cy.get(`.react-grid-Main .react-grid-Row:nth-child(${rowNumber})`),
    getRowText: (rowNumber: number): string => {
      cy.get(`.react-grid-Main .react-grid-Row:nth-child(${rowNumber})`);
      return cy.$$(`.react-grid-Main .react-grid-Row:nth-child(${rowNumber})`).text();
    },
    vehicles: {
      columnLength: 37,
    },
    sortIcon: {
      asc: '▲',
      desc: '▼',
    },
  },
};

describe('Modal with order address', () => {
  before(() => {
    cy.yandexLogin('mvrpManager');
    cy.openAndCloseVideo();
    cy.get(selectors.content.mvrp.start);
  });

  it('Page open', () => {
    cy.get(selectors.content.mvrp.fileInput).attachFile(EXCEL_FILE_NAME);
    cy.get(selectors.content.import.tabs.tabContent, { timeout: EXCEL_FILE_UPLOAD_TIMEOUT }).should(
      'be.visible',
    );
    cy.get(selectors.content.import.tabs.map).find(selectors.content.import.tabs.errorIcon);
  });

  it('Search coords at orders page', () => {
    cy.get(selectors.content.import.notifications.actionButton, { timeout: 5000 })
      .should('exist')
      .should('have.text', SEARCH_COORDS_BUTTON_TEXT)
      .click();

    cy.get(selectors.content.import.tabs.map).find(selectors.content.import.tabs.activeIcon);

    cy.get(selectors.content.import.map.mapContainer).should('be.visible');

    each(TEXT_SET, el => {
      cy.get(selectors.content.import.map.sidebarElementHead).should('contain', el);
    });
  });

  it('Open point without coords', () => {
    cy.get(
      selectors.content.import.map.getSidebarItem(
        `Orders-${vehiclesReferenceBookKeyset.ru.extendedForm_shift_perStopLackPenalty_placeholder}`,
      ),
    )
      .click({ scrollBehavior: false })
      .should('have.class', selectors.content.import.map.activeSidebarItemModifier);

    cy.get(
      selectors.content.import.map.getSidebarItem(
        `Orders-${vehiclesReferenceBookKeyset.ru.extendedForm_shift_perStopLackPenalty_placeholder}`,
      ),
    ).then($el => {
      const address = $el.text();

      cy.get(selectors.content.import.map.infoPanel)
        .should('be.visible')
        .find(selectors.content.import.map.addressInput)
        .should('have.value', address);
    });
  });

  it('Open approximate point', () => {
    cy.get(selectors.content.import.map.sidebarElementHead).then($el => {
      $el.each(function () {
        if (this.textContent === TEXT_SET[1]) {
          cy.wrap(this).click();
          cy.get(selectors.content.import.map.getSidebarItem('Orders-26'))
            .click({ scrollBehavior: false })
            .should('have.class', selectors.content.import.map.activeSidebarItemModifier);

          cy.get(selectors.content.import.map.getSidebarItem('Orders-26')).then($el => {
            const address = $el.text();

            cy.get(selectors.content.import.map.infoPanel)
              .should('be.visible')
              .find(selectors.content.import.map.addressInput)
              .should('have.value', address);

            cy.get(selectors.content.import.map.selectedPoint)
              .should('be.visible')
              .invoke('attr', 'data-test-anchor')
              .should('contain', selectors.content.import.map.yellowSVG);
          });
        }
      });
    });
  });

  it('Close info panel and sidebar with approximate coordinates', () => {
    cy.get(selectors.content.import.map.infoPanelClose).click();
    cy.get(selectors.content.import.map.selectedPoint).should('not.exist');
    cy.get(selectors.content.import.map.infoPanel).should('not.exist');

    cy.get(selectors.content.import.map.sidebarElementHead).then($el => {
      $el.each(function () {
        if (this.textContent === TEXT_SET[1]) {
          cy.wrap(this).click({ scrollBehavior: false });
        }
      });
    });
  });

  it('Open detailed points', () => {
    cy.get(selectors.content.import.map.sidebarElementHead).then($el => {
      $el.each(function () {
        if (this.textContent === TEXT_SET[2]) {
          cy.wrap(this).click({ force: true });
          cy.get(selectors.content.import.map.getSidebarItem('Orders-4'))
            .click({ scrollBehavior: false })
            .should('have.class', selectors.content.import.map.activeSidebarItemModifier);

          cy.get(selectors.content.import.map.getSidebarItem('Orders-4')).then($el => {
            const address = $el.text();

            cy.get(selectors.content.import.map.infoPanel)
              .should('be.visible')
              .find(selectors.content.import.map.addressInput)
              .should('have.value', address);

            cy.get(selectors.content.import.map.selectedPoint)
              .should('be.visible')
              .invoke('attr', 'data-test-anchor')
              .should('contain', selectors.content.import.map.greenSVG);
          });
        }
      });
    });
  });

  it('Close info panel one more time', () => {
    cy.get(selectors.content.import.map.infoPanelClose).click();
    cy.get(selectors.content.import.map.selectedPoint).should('not.exist');
    cy.get(selectors.content.import.map.infoPanel).should('not.exist');
  });
});

// @see https://testpalm.yandex-team.ru/courier/testcases/725
// TODO: need change after https://st.yandex-team.ru/BBGEO-11551
context('Interaction with lines in planning', () => {
  before(() => {
    cy.yandexLogin('mvrpManager');
    cy.clearLocalforage();
    cy.openAndCloseVideo();
  });

  it('Import planning', () => {
    cy.get(selectors.sidebar.menu.mvrp).click();

    cy.get(selectors.content.mvrp.fileInput).attachFile(EXCEL_FILE_NAME2);
    cy.get(selectors.content.import.tabs.vehicles).click({ timeout: EXCEL_FILE_UPLOAD_TIMEOUT });
  });

  context('Check sort data in vehicles tab', () => {
    it('All headers exists in vehicles tab', () => {
      cy.get(table.selectors.table.headers).should('have.length', table.data.vehicles.columnLength);
    });

    context('Sort use header', () => {
      it('After import position elements saved', () => {
        cy.get(table.selectors.vehicles.vehicleNumber.firstCell).should('have.text', 'Tesla');
        cy.get(table.selectors.vehicles.capacity.secondCell).should('have.text', '860');
      });

      it('by vehicle name', () => {
        cy.get(table.selectors.vehicles.vehicleNumber.header).click();
        cy.get(
          `${table.selectors.vehicles.vehicleNumber.header} ${table.selectors.table.sortIcon}`,
        ).should('have.text', table.data.sortIcon.asc);
        cy.get(table.selectors.vehicles.vehicleNumber.firstCell).should('have.text', 'Alfa Romeo');

        cy.get(table.selectors.vehicles.vehicleNumber.header).click();
        cy.get(
          `${table.selectors.vehicles.vehicleNumber.header} ${table.selectors.table.sortIcon}`,
        ).should('have.text', table.data.sortIcon.desc);
        cy.get(table.selectors.vehicles.vehicleNumber.firstCell).should('have.text', 'КАМАЗ');
      });

      it('by capacity', () => {
        cy.get(table.selectors.vehicles.capacity.firstCell).should('have.text', '6690');

        cy.get(table.selectors.vehicles.capacity.header).click();
        cy.get(
          `${table.selectors.vehicles.capacity.header} ${table.selectors.table.sortIcon}`,
        ).should('have.text', table.data.sortIcon.asc);
        cy.get(table.selectors.vehicles.capacity.firstCell).should('have.text', '860');

        cy.get(table.selectors.vehicles.capacity.header).click();
        cy.get(
          `${table.selectors.vehicles.capacity.header} ${table.selectors.table.sortIcon}`,
        ).should('have.text', table.data.sortIcon.desc);
        cy.get(table.selectors.vehicles.capacity.firstCell).should('have.text', '9670');
      });
    });

    context('Sort use context menu', () => {
      context('by vehicle name', () => {
        it('Use subMenu', () => {
          cy.get(table.selectors.vehicles.vehicleNumber.firstCell).should('have.text', '«Москвич»');

          cy.get(table.selectors.vehicles.vehicleNumber.firstCell).rightclick();
          cy.get(table.selectors.table.contextMenu.menu.sort.view)
            .click()
            .get(table.selectors.table.contextMenu.menu.sort.subMenu.asc)
            .click();
        });

        it('asc', () => {
          cy.get(
            `${table.selectors.vehicles.vehicleNumber.header} ${table.selectors.table.sortIcon}`,
          ).should('have.text', table.data.sortIcon.asc);
          cy.get(table.selectors.vehicles.vehicleNumber.firstCell).should(
            'have.text',
            'Alfa Romeo',
          );
          cy.get(table.selectors.vehicles.vehicleNumber.firstCell).rightclick();
          cy.get(table.selectors.table.contextMenu.menu.sort.view)
            .click()
            .get(table.selectors.table.contextMenu.menu.sort.subMenu.desc)
            .click();
        });

        it('desc', () => {
          cy.get(
            `${table.selectors.vehicles.vehicleNumber.header} ${table.selectors.table.sortIcon}`,
          ).should('have.text', table.data.sortIcon.desc);
          cy.get(table.selectors.vehicles.vehicleNumber.firstCell).should('have.text', 'КАМАЗ');
        });
      });
    });

    context('Interactions with rows', () => {
      it('Add row to up', () => {
        const checkText = table.data.getRowText(1);
        cy.get(table.selectors.vehicles.vehicleNumber.firstCell).rightclick();
        cy.get(table.selectors.table.contextMenu.menu.appendRow.view)
          .click()
          .get(table.selectors.table.contextMenu.menu.appendRow.subMenu.toUp)
          .click()
          .wait(200);

        table.data.getRowElements(1).invoke('text').should('equal', '');
        table.data.getRowElements(2).invoke('text').should('equal', checkText);

        cy.get(table.selectors.selectedCell)
          .invoke('css', 'transform')
          .should('equal', 'matrix(1, 0, 0, 1, 0, 0)');
      });

      it('Add row to down', () => {
        cy.get(table.selectors.vehicles.vehicleNumber.thirdCell).should('have.text', 'ЗИЛ');
        cy.get(table.selectors.vehicles.vehicleNumber.secondCell).rightclick();
        cy.get(table.selectors.table.contextMenu.menu.appendRow.view)
          .click()
          .get(table.selectors.table.contextMenu.menu.appendRow.subMenu.toDown)
          .click()
          .wait(100);
        table.data.getRowElements(3).invoke('text').should('equal', '');

        cy.get(table.selectors.selectedCell)
          .invoke('css', 'transform')
          .should('equal', 'matrix(1, 0, 0, 1, 0, 35)');
      });

      it('delete row', () => {
        const checkTextDownRow = table.data.getRowText(8);
        cy.get(table.selectors.vehicles.vehicleNumber.seventhCell)
          .should('have.text', '«Иж»')
          .rightclick();
        cy.get(table.selectors.table.contextMenu.menu.deleteRow).click();
        cy.wait(1000);
        table.data.getRowElements(7).invoke('text').should('equal', checkTextDownRow);
        cy.get(table.selectors.selectedCell)
          .invoke('css', 'transform')
          .should('equal', 'matrix(1, 0, 0, 1, 0, 210)');
      });

      it('dublicate row', () => {
        const checkText = table.data.getRowText(2);
        cy.get(table.selectors.vehicles.vehicleNumber.secondCell)
          .should('have.text', 'КАМАЗ')
          .rightclick();
        cy.get(table.selectors.table.contextMenu.menu.dublicateRow).click();
        table.data.getRowElements(3).invoke('text').should('equal', checkText);
        cy.get(table.selectors.vehicles.vehicleNumber.thirdCell).should('have.text', 'КАМАЗ');
        cy.get(table.selectors.table.warning).should('exist');

        cy.get(table.selectors.selectedCell)
          .invoke('css', 'transform')
          .should('equal', 'matrix(1, 0, 0, 1, 0, 35)');
      });
    });
  });

  context('Planning map', () => {
    before(() => {
      cy.get(selectors.content.import.tabs.map).click();
    });

    // @see https://testpalm.yandex-team.ru/courier/testcases/551
    addressInputOnMapClearSharedSpec();
  });
});
