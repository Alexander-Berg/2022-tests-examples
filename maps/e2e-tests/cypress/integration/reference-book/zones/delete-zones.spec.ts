import selectors from '../../../../src/constants/selectors';

// TODO: should not skip it when zones will opened in production
context.skip('Delete zones', () => {
  before(() => {
    cy.yandexLogin('admin');

    cy.get(selectors.sidebar.menu.collections).click();
    cy.get(selectors.sidebar.menu.zonesReferenceBook).click();
  });

  it('should delete a zones', () => {
    const ZONE_NAMES = [
      `Zone #2091 (Don't pick me I will be deleted)`,
      `Zone #2092 (Don't pick me I will be deleted)`,
    ];

    ZONE_NAMES.forEach(zoneName => {
      cy.get(selectors.content.referenceBook.zones.table).should('contain.text', zoneName);

      cy.contains(selectors.content.referenceBook.zones.tableRow.zoneName, zoneName).click();
      cy.get(selectors.content.referenceBook.zones.deleteZone)
        .click()
        .get(selectors.modal.dialog.submit)
        .click();

      cy.get(selectors.content.referenceBook.zones.table).should('not.contain.text', zoneName);
    });
  });

  it('should cancel a deletion of zone', () => {
    const ZONE_NAME = 'Zone #2093';

    cy.get(selectors.content.referenceBook.zones.table).should('contain.text', ZONE_NAME);

    cy.contains(selectors.content.referenceBook.zones.tableRow.zoneName, ZONE_NAME).click();
    cy.get(selectors.content.referenceBook.zones.deleteZone)
      .click()
      .get(selectors.modal.dialog.cancel)
      .click();
    cy.get(selectors.content.referenceBook.zones.cancelEditing).click();

    cy.get(selectors.content.referenceBook.zones.table).should('contain.text', ZONE_NAME);
  });
});
