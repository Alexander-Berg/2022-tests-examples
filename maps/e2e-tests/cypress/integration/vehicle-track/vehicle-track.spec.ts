import * as importKeyset from '../../../../src/translations/import';
import selectors from '../../../src/constants/selectors';

describe('Vehicle track', function () {
  before(() => {
    cy.fixture('testData').then(() => {
      cy.yandexLogin('admin');
    });
  });
  it('Open pop-up order', () => {
    cy.get(selectors.content.dashboard.view);
    cy.get(selectors.content.dashboard.couriers.table.orders.late).invoke('last').click();
    cy.get(selectors.modal.orderPopup.title)
      .invoke('text')
      .should('eq', 'Заказ extraTESTconfirmed');
    cy.get(selectors.modal.orderPopup.addressRow)
      .invoke('text')
      .should('eq', 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2');
    cy.get(selectors.modal.orderPopup.statusRow)
      .invoke('text')
      .should('eq', importKeyset.ru.params_initialStatus_confirmed);
  });

  it('Get button to getting vehicle track link', () => {
    cy.get(selectors.modal.orderPopup.title).dblclick().end();
    cy.get(selectors.modal.orderPopup.buttonGetTrackLink).click();
  });

  it('Track link exists', () => {
    cy.get(selectors.modal.orderPopup.trackingLink);
  });

  it('Vehicle track open', () => {
    cy.get(selectors.modal.orderPopup.trackingLink).then($trackingLink => {
      cy.forceVisit($trackingLink.first().text());
    });
  });

  describe('Vehicle track elements', () => {
    it('Warning exist', () => {
      cy.get(selectors.tracker.order.warning)
        .invoke('text')
        .should('eq', 'Заказ доставят с опозданием');
    });
    it('Map exist', () => {
      cy.get(selectors.tracker.map);
    });

    it('History exist', () => {
      cy.get(selectors.tracker.history.view);
      cy.get(selectors.tracker.history.items).should('have.length', 2);
    });

    it('Order address exist', () => {
      cy.get(selectors.tracker.mapLegend.description)
        .invoke('text')
        .should('eq', 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2');
    });

    it('Deatils is opening', () => {
      cy.get(selectors.tracker.detailsButton).click();
      cy.get('.order__payment-option');
    });
  });
});
