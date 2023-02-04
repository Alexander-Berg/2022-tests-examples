import * as importKeyset from '../../../../src/translations/import';
import selectors from '../../../src/constants/selectors';
import each from 'lodash/each';

const EXCEL_FILE_NAME = 'planning_depot_geocoding.xlsx';

// this time is enough to upload and parse an excel file
// if a file isn't uploaded during this time, then something went wrong
const EXCEL_FILE_UPLOAD_TIMEOUT = 60000;

const TEXT_SET = [
  `${importKeyset.ru.geoCodingSections_bad}2`,
  `${importKeyset.ru.geoCodingSections_neutral}1`,
  `${importKeyset.ru.geoCodingSections_good}2`,
];
const SEARCH_COORDS_BUTTON_TEXT = importKeyset.ru.orderCoordinatesWarning_action;

const table = {
  data: {
    getLangitude: (rowNumber: number): Cypress.Chainable<JQuery<HTMLElement>> =>
      cy.get(`.react-grid-Row:nth-child(${rowNumber}) > .react-grid-Cell:nth-child(3)`),
    getLongitude: (rowNumber: number): Cypress.Chainable<JQuery<HTMLElement>> =>
      cy.get(`.react-grid-Row:nth-child(${rowNumber}) > .react-grid-Cell:nth-child(4)`),
    getAddress: (rowNumber: number): Cypress.Chainable<JQuery<HTMLElement>> =>
      cy.get(`.react-grid-Row:nth-child(${rowNumber}) > .react-grid-Cell:nth-child(5)`),
  },
};

context('Checking depot geocoding', () => {
  describe('Modal with input for depot address', () => {
    before(() => {
      cy.yandexLogin('mvrpManager');
      cy.openAndCloseVideo();
      cy.get(selectors.content.mvrp.start);
    });

    it('Page open', () => {
      cy.get(selectors.content.mvrp.fileInput).attachFile(EXCEL_FILE_NAME);
      cy.get(selectors.content.import.tabs.tabContent, {
        timeout: EXCEL_FILE_UPLOAD_TIMEOUT,
      }).should('be.visible');
      cy.get(selectors.content.import.tabs.depots).click();
      cy.get(selectors.content.import.tabs.map).find(selectors.content.import.tabs.errorIcon);
    });

    it('Search coords at depot page', () => {
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
      cy.get(selectors.content.import.map.getSidebarItem('Depots-3'))
        .click({ scrollBehavior: false })
        .should('have.class', selectors.content.import.map.activeSidebarItemModifier);

      cy.get(selectors.content.import.map.getSidebarItem('Depots-3')).then(() => {
        cy.get(selectors.content.import.map.infoPanel)
          .should('be.visible')
          .find(selectors.content.import.map.addressInput)
          .should('have.value', '');
      });
    });

    it('Open approximate point', () => {
      cy.get(selectors.content.import.map.sidebarElementHead).then($el => {
        $el.each(function () {
          if (this.textContent === TEXT_SET[1]) {
            cy.wrap(this).click();
            cy.get(selectors.content.import.map.getSidebarItem('Depots-1'))
              .click({ scrollBehavior: false })
              .should('have.class', selectors.content.import.map.activeSidebarItemModifier);

            cy.get(selectors.content.import.map.getSidebarItem('Depots-1')).then($el => {
              const address = $el.text();

              cy.get(selectors.content.import.map.infoPanel)
                .should('be.visible')
                .find(selectors.content.import.map.addressInput)
                .should('have.value', address);

              cy.get(selectors.content.import.map.selectedPoint)
                .should('be.visible')
                .invoke('attr', 'data-test-anchor')
                .should('contain', selectors.content.import.map.depotSVG);
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
            cy.get(selectors.content.import.map.getSidebarItem('Depots-0'))
              .click({ scrollBehavior: false })
              .should('have.class', selectors.content.import.map.activeSidebarItemModifier);

            cy.get(selectors.content.import.map.getSidebarItem('Depots-0')).then($el => {
              const address = $el.text();

              cy.get(selectors.content.import.map.infoPanel)
                .should('be.visible')
                .find(selectors.content.import.map.addressInput)
                .should('have.value', address);

              cy.get(selectors.content.import.map.selectedPoint)
                .should('be.visible')
                .invoke('attr', 'data-test-anchor')
                .should('contain', selectors.content.import.map.depotSVG);
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

  describe('Change address for depot with map modal', () => {
    it('Open point without coords and put address', () => {
      cy.get(selectors.content.import.map.getSidebarItem('Depots-3'))
        .click({ scrollBehavior: false })
        .should('have.class', selectors.content.import.map.activeSidebarItemModifier);
      cy.get(selectors.content.import.map.infoPanel)
        .should('be.visible')
        .find(selectors.content.import.map.addressInput)
        .clear()
        .type('Москва, улица Льва Толстого, 16');
      cy.wait(500);
      cy.get('.geo-coding-suggest__address__menu').eq(0).click();

      cy.get(selectors.content.import.map.getSidebarItem('Depots-3')).then($el => {
        const address = $el.text();

        cy.get(selectors.content.import.map.infoPanel)
          .should('be.visible')
          .find(selectors.content.import.map.addressInput)
          .should('have.value', address);
      });
    });
  });

  describe('Depot adress should be saved after changes in the table and map', () => {
    before(() => {
      cy.get(selectors.content.import.tabs.depots).click();
    });

    it('Address should be visible when coordinates were removed', () => {
      table.data
        .getAddress(4)
        .invoke('text')
        .then($text => {
          table.data.getLangitude(4).type(' ');
          table.data.getLongitude(4).type(' ');
          table.data.getAddress(4).should('have.text', $text);
        });
    });

    it('Address should be visible when point is searching on the map', () => {
      table.data
        .getAddress(4)
        .invoke('text')
        .then($text => {
          cy.get(selectors.content.import.tabs.map).click();
          cy.get(selectors.content.import.map.getSidebarItem('Depots-3')).should(
            'have.text',
            $text,
          );
          cy.get(selectors.content.import.map.getSidebarItem('Depots-3'))
            .click({ scrollBehavior: false })
            .should('have.class', selectors.content.import.map.activeSidebarItemModifier);

          cy.get(selectors.content.import.map.getSidebarItem('Depots-3')).then(() => {
            cy.get(selectors.content.import.map.infoPanel)
              .should('be.visible')
              .find(selectors.content.import.map.addressInput)
              .should('have.value', $text);
          });
        });
    });
  });
});
