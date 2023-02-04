import * as importKeyset from '../../../../src/translations/import';
import selectors from '../../../src/constants/selectors';

const EXCEL_FILE_NAME = 'planning_depot_geocoding.xlsx';

// this time is enough to upload and parse an excel file
// if a file isn't uploaded during this time, then something went wrong
const EXCEL_FILE_UPLOAD_TIMEOUT = 60000;

const notificationTexts = {
  addressRequiredToFindPoint: importKeyset.ru['notifications_SHEETS.DEPOTS.ADDRESS.REQUIRED'],
  coordinatesRequiredGroup:
    importKeyset.ru['notificationsGroups_SHEETS.DEPOT.COORDINATES.REQUIRED'],
  coordinatesRequired: importKeyset.ru['notifications_SHEETS.DEPOTS.COORDINATES.REQUIRED'],
};

const table = {
  selectors: {
    selectedCell: '[data-test="cell-mask"]',
    table: {
      notificationActive: '.data-grid-table__notification_active',
    },
  },
  data: {
    getLangitude: (rowNumber: number): Cypress.Chainable<JQuery<HTMLElement>> =>
      cy.get(`.react-grid-Row:nth-child(${rowNumber}) > .react-grid-Cell:nth-child(3)`),
    getLongitude: (rowNumber: number): Cypress.Chainable<JQuery<HTMLElement>> =>
      cy.get(`.react-grid-Row:nth-child(${rowNumber}) > .react-grid-Cell:nth-child(4)`),
    getAddress: (rowNumber: number): Cypress.Chainable<JQuery<HTMLElement>> =>
      cy.get(`.react-grid-Row:nth-child(${rowNumber}) > .react-grid-Cell:nth-child(5)`),
  },
};

describe('Notifications for depots geocoding', () => {
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
    cy.get(selectors.content.import.tabs.depots).click({ force: true });
  });

  describe('The depot with correct address (first depot in the table)', () => {
    it('latitude should be set and notifications should not exist', () => {
      table.data.getLangitude(1).should('have.text', '55.733974');

      table.data.getLangitude(1).find('.data-grid-table__notification_warn').should('not.exist');

      table.data.getLangitude(1).find('.data-grid-table__notification_error').should('not.exist');
    });

    it('longitude should be set and notifications should not exist', () => {
      table.data.getLongitude(1).should('have.text', '37.587093');

      table.data.getLangitude(1).find('.data-grid-table__notification_warn').should('not.exist');

      table.data.getLangitude(1).find('.data-grid-table__notification_error').should('not.exist');
    });
  });

  describe('The depot with wrong address (the third address in the table)', () => {
    it('latitude should not be set and error should exist', () => {
      table.data.getLangitude(3).should('have.text', '');

      table.data.getLangitude(3).find('.data-grid-table__notification_error').should('exist');
    });

    it('longitude should be set and notifications should not exist', () => {
      table.data.getLongitude(3).should('have.text', '');

      table.data.getLangitude(3).find('.data-grid-table__notification_error').should('exist');
    });

    it('The address cell should not be empty and have notifications', () => {
      table.data.getAddress(3).should('not.be.empty');

      table.data.getAddress(3).find('.data-grid-table__notification_error').should('not.exist');

      table.data.getAddress(3).find('.data-grid-table__notification_warn').should('not.exist');
    });

    it('Notification about empty coordinates should exist', () => {
      cy.get(selectors.content.import.notifications.list)
        .contains(notificationTexts.coordinatesRequiredGroup)
        .should('exist')
        .click({ force: true });
      table.data.getLangitude(3).click({ force: true });
      cy.get(selectors.content.import.notifications.activeNotification)
        .should('exist')
        .should('have.text', notificationTexts.coordinatesRequired);
    });
  });

  describe('The depot with empty address (the fourth address in the table)', () => {
    it('latitude should not be set and errors should not exist', () => {
      table.data.getLangitude(4).should('have.text', '');

      table.data.getLangitude(4).find('.data-grid-table__notification_error').should('not.exist');

      table.data.getLangitude(4).find('.data-grid-table__notification_warn').should('not.exist');
    });

    it('longitude should not be set and notifications should not exist', () => {
      table.data.getLongitude(4).should('have.text', '');

      table.data.getLangitude(4).find('.data-grid-table__notification_error').should('not.exist');
    });

    it('The address cell should be empty and have notifications', () => {
      table.data.getAddress(4).should('have.text', '');

      table.data.getAddress(4).find('.data-grid-table__notification_error').should('exist');

      table.data.getAddress(4).find('.data-grid-table__notification_warn').should('not.exist');
    });

    it('Notification about empty adresss should exist', () => {
      cy.get(selectors.content.import.notifications.list)
        .contains(notificationTexts.addressRequiredToFindPoint)
        .should('exist')
        .click();
      table.data.getAddress(4).find(table.selectors.table.notificationActive).should('exist');
    });
  });
});
