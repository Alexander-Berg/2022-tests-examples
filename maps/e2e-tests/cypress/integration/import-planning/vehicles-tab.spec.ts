import selectors from '../../../src/constants/selectors';

const EXCEL_FILE_NAME = 'planning.xlsx';
// this time is enough to upload an excel file
// if a file isn't uploaded during this time, then something went wrong
const EXCEL_FILE_UPLOAD_TIMEOUT = 30000;

// https://st.yandex-team.ru/BBGEO-10617
context.skip('Vehicles tab', () => {
  beforeEach(() => {
    cy.yandexLogin('mvrpManager');
    cy.clearLocalforage();

    // FIXME: sometimes modal window don't open, so should extract closing logic of modal window
    // to cypress command like `closeModalIfIsOpened` and catch there errors when modal isn't opened
    cy.get(selectors.modal.closeButton, { timeout: 10000 }).click();
    cy.get(selectors.sidebar.menu.mvrp).click();
    cy.get(selectors.content.mvrp.start).click();
  });

  it('should use vehicles reference book by default', () => {
    cy.get(selectors.content.import.tabs.vehicles).click();

    cy.get(selectors.content.import.vehiclesRefBookTable).should('be.visible');
  });

  it('should allow user to select vehicles source when excel imported', () => {
    cy.get(selectors.content.import.fileInput).attachFile(EXCEL_FILE_NAME);
    cy.get(selectors.content.import.tabs.vehicles).click({ timeout: EXCEL_FILE_UPLOAD_TIMEOUT });
    cy.get(selectors.content.import.useVehiclesSource).should('be.visible');

    cy.get(selectors.content.mvrp.startNewPlanning).click();
    cy.get(selectors.content.mvrp.fileInput).attachFile(EXCEL_FILE_NAME);
    cy.get(selectors.content.import.tabs.vehicles).click({ timeout: EXCEL_FILE_UPLOAD_TIMEOUT });
    cy.get(selectors.content.import.useVehiclesSource).should('be.visible');
  });

  it('should allow user to select vehicles source when excel imported over example file', () => {
    cy.get(selectors.content.import.tabs.vehicles).click();
    cy.get(selectors.content.import.fileInput).attachFile(EXCEL_FILE_NAME);

    cy.get(selectors.content.import.tabs.vehicles).click({ timeout: EXCEL_FILE_UPLOAD_TIMEOUT });
    cy.get(selectors.content.import.useVehiclesSource).should('be.visible');
  });

  it('should allow user to select vehicles source when excel DnD file imported over example file', () => {
    cy.get(selectors.content.import.tabs.vehicles).click();
    cy.get(selectors.content.import.dnd).attachFile(EXCEL_FILE_NAME, {
      subjectType: 'drag-n-drop',
    });

    cy.get(selectors.content.import.tabs.vehicles).click({ timeout: EXCEL_FILE_UPLOAD_TIMEOUT });
    cy.get(selectors.content.import.useVehiclesSource).should('be.visible');
  });

  it('should select all vehicles by default', () => {
    cy.get(selectors.content.import.tabs.vehicles).click();

    cy.get(selectors.table.checkboxCheckAll).should('be.checked');
  });

  it('should select all vehicles by default on every "mvrp import session"', () => {
    cy.get(selectors.content.import.tabs.vehicles).click();

    cy.get(selectors.table.checkboxCheckAll).should('be.checked');
    cy.get(selectors.table.checkboxCheckAll).click();
    cy.get(selectors.table.checkboxCheckAll).should('not.be.checked');

    cy.get(selectors.content.mvrp.startNewPlanning).click();

    cy.get(selectors.content.mvrp.start).click();
    cy.get(selectors.content.import.tabs.vehicles).click();

    cy.get(selectors.table.checkboxCheckAll).should('be.checked');
  });

  it('should remain user vehicles source choice during "mvrp import session"', () => {
    cy.get(selectors.content.import.tabs.vehicles).click();

    cy.get(selectors.content.import.tabs.map).click();

    cy.get(selectors.content.import.tabs.vehicles).click();

    cy.get(selectors.content.import.useVehiclesSource, {
      timeout: 1000,
    }).should('not.exist');
  });
});
