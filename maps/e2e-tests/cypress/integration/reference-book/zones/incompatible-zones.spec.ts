import * as zonesReferenceBookKeyset from '../../../../../src/translations/zones-reference-book';
import selectors from '../../../../src/constants/selectors';

const INCOMPATIBLE_ZONE_LIST_TITLE = zonesReferenceBookKeyset.ru.edit_zones_list_incompatible;
const INCOMPATIBLE_ZONE_LIST_MODAL_TITLE =
  zonesReferenceBookKeyset.ru.edit_zones_modal_incompatible_zones_title;

const ZONE_1_NAME = 'Zone #790';

const getDataListHead = (): Cypress.Chainable =>
  cy.contains(selectors.content.referenceBook.zones.zoneDataListHead, INCOMPATIBLE_ZONE_LIST_TITLE);
const getDataListBody = (): Cypress.Chainable => getDataListHead().next();

// TODO: should not skip it when backend implement incompatible zones
context.skip('Check incompatible zones', () => {
  before(() => {
    cy.yandexLogin('admin');
  });

  it('should open a zones reference book', () => {
    cy.get(selectors.sidebar.menu.collections).click();
    cy.get(selectors.sidebar.menu.zonesReferenceBook).click();
  });

  it('should open a zone', () => {
    cy.contains(selectors.content.referenceBook.zones.tableRow.zoneName, ZONE_1_NAME).click();
  });

  it('should open a incompatible zone list', () => {
    getDataListBody().should('have.css', 'max-height').and('match', /^0px$/);

    getDataListHead().click();

    getDataListBody().should('have.css', 'max-height').and('not.match', /^0px$/);
  });

  it('should close a incompatible zone list', () => {
    getDataListBody().should('have.css', 'max-height').and('not.match', /^0px$/);

    getDataListHead().click();

    getDataListBody().should('have.css', 'max-height').and('match', /^0px$/);
  });

  it('should open a incompatible zones edit modal', () => {
    getDataListHead().within(() => {
      cy.get(selectors.content.referenceBook.zones.zoneDataListEditBtn).click();
    });

    cy.get(selectors.content.referenceBook.zones.zoneDataListModal).should(
      'contain.text',
      INCOMPATIBLE_ZONE_LIST_MODAL_TITLE,
    );
  });

  it('should close a incompatible zones edit modal', () => {
    cy.get(selectors.content.referenceBook.zones.zoneDataListModalCancelBtn).click();

    cy.get(selectors.content.referenceBook.zones.zoneDataListModal).should('not.exist');
  });

  it('should open a incompatible zones edit modal', () => {
    getDataListHead().within(() => {
      cy.get(selectors.content.referenceBook.zones.zoneDataListEditBtn).click();
    });

    cy.get(selectors.content.referenceBook.zones.zoneDataListModal).should(
      'contain.text',
      INCOMPATIBLE_ZONE_LIST_MODAL_TITLE,
    );
  });

  it('should cancel an editing and close a incompatible zones edit modal', () => {
    cy.get(selectors.content.referenceBook.zones.zoneDataListModalCancelBtn).click();

    cy.get(selectors.content.referenceBook.zones.zoneDataListModal).should('not.exist');
  });

  it('should open a incompatible zones edit modal', () => {
    getDataListHead().within(() => {
      cy.get(selectors.content.referenceBook.zones.zoneDataListEditBtn).click();
    });

    cy.get(selectors.content.referenceBook.zones.zoneDataListModal).should(
      'contain.text',
      INCOMPATIBLE_ZONE_LIST_MODAL_TITLE,
    );
  });

  it('should add a incompatible zones', () => {
    // TODO: should implement this test case when backend implement incompatible zones
  });

  it('should remove a incompatible zones', () => {
    // TODO: should implement this test case when backend implement incompatible zones
  });

  it('should save a incompatible zones', () => {
    cy.get(selectors.content.referenceBook.zones.zoneDataListModalSaveBtn).click();

    cy.get(selectors.content.referenceBook.zones.zoneDataListModal).should('not.exist');

    // TODO: should check that a changes are saved when backend implement incompatible zones
  });
});
