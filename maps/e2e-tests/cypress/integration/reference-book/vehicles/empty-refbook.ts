import * as vehiclesReferenceBookKeyset from '../../../../../src/translations/vehicles-reference-book';
import selectors from '../../../../src/constants/selectors';

// @see https://testpalm.yandex-team.ru/courier/testcases/658

const emptyMessageReferenceBook = {
  title: vehiclesReferenceBookKeyset.ru.emptyMessageTitle,
  description: vehiclesReferenceBookKeyset.ru.emptyMessageDescription,
};

context('Reference Book without vehicles', () => {
  beforeEach(() => {
    cy.yandexLogin('mvrpViewManager');
    cy.clearLocalforage();
    cy.openAndCloseVideo();
    cy.get(selectors.sidebar.menu.collections).click();
    cy.get(selectors.sidebar.menu.vehiclesReferenceBook).click();
    cy.get(selectors.modal.closeButton, { timeout: 10000 }).click();
  });

  it('should be displayed a message with icon for a company without a vehicle', () => {
    const { noVehicleIcon, noVehicleTitle, noVehicleDescription } =
      selectors.content.referenceBook.vehicles.emptyMessage;

    cy.get(noVehicleIcon).should('be.visible');

    cy.get(noVehicleTitle).should('have.text', emptyMessageReferenceBook.title).and('be.visible');

    cy.get(noVehicleDescription)
      .should('have.text', emptyMessageReferenceBook.description)
      .and('be.visible');
  });

  it('should be displayed action buttons and not be visible the table settings button for a company without a vehicle', () => {
    const { addVehicle, downloadXLSButton, columnSettings } =
      selectors.content.referenceBook.vehicles;

    cy.get(addVehicle).should('be.visible');

    cy.get(downloadXLSButton).should('be.visible');

    cy.get(columnSettings.opener).should('not.exist');
  });
});
