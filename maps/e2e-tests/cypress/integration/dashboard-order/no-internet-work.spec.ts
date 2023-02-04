import * as errorPanelKeyset from '../../../../src/translations/error-panel';
import * as vehiclesReferenceBookKeyset from '../../../../src/translations/vehicles-reference-book';
import * as orderDetailsKeyset from '../../../../src/translations/order-details';
import * as couriersKeyset from '../../../../src/translations/couriers';
import { IPointGeometry } from 'yandex-maps';
import selectors from '../../../src/constants/selectors';

describe('No internet work', () => {
  before(function () {
    cy.yandexLogin('admin');
  });

  it('Enter the page', function () {
    cy.get(selectors.sidebar.menu.monitoringGroup).click();
    cy.get(selectors.sidebar.menu.dashboard).click();

    cy.get(selectors.content.dashboard.view).should('exist');
    cy.get(selectors.content.dashboard.couriers.table.row).should('exist');
  });

  it('Open an order window', function () {
    cy.get(selectors.content.dashboard.couriers.table.orders.anyClickable).first().click();

    cy.get(selectors.modal.orderPopup.view).should('exist');
    cy.get(selectors.content.dashboard.orders.modal.chatButton).should(
      'have.text',
      couriersKeyset.ru.messenger_button,
    );
    cy.get(selectors.content.dashboard.orders.modal.removeButton).should(
      'have.text',
      orderDetailsKeyset.ru.editorActions_remove,
    );
    cy.get(selectors.modal.orderPopup.editButton).should(
      'have.text',
      orderDetailsKeyset.ru.editorActions_open,
    );
    cy.get(selectors.content.dashboard.orders.modal.editTable).should('not.exist');
  });

  it('Click the update button', function () {
    cy.get(selectors.modal.orderPopup.editButton).click();
    cy.get(selectors.content.dashboard.orders.modal.editTable).should('exist');
  });

  it('Try to submit the form in the offline mode', function () {
    cy.intercept('*', req => {
      req.reply({
        delayMs: 5000,
        forceNetworkError: true,
      });
    }).as('req');
    cy.get(selectors.content.dashboard.orders.modal.orderNumberInput).type(
      vehiclesReferenceBookKeyset.ru.extendedForm_shift_perStopLackPenalty_placeholder,
    );
    cy.get(selectors.content.dashboard.orders.modal.saveButton).click();
    cy.get(selectors.modal.orderPopup.loader).should('exist');

    cy.wait('@req').then(() => {
      cy.get(selectors.modal.errorPopup.view).should('exist');
      cy.get(selectors.modal.errorPopup.messageTitle).then(titles => {
        const texts = titles.toArray().map(el => el.textContent);
        expect(texts).to.include(errorPanelKeyset.ru.createOrder);
      });
    });

    cy.wait(31_000).then(() => {
      cy.get(selectors.modal.errorPopup.messageTitle).then(titles => {
        const texts = titles.toArray().map(el => el.textContent);
        expect(texts).to.include(errorPanelKeyset.ru.getCourierPosition);
      });
    });
  });

  it('Close the error window after the update attempt', function () {
    cy.get(selectors.modal.errorPopup.closeButton).click();
    cy.get(selectors.content.dashboard.orders.modal.cancelButton).click();

    cy.get(selectors.modal.errorPopup.view).should('not.exist');
    cy.get(selectors.content.dashboard.orders.modal.editTable).should('not.exist');
    cy.get(selectors.content.dashboard.orders.modal.showTable).should('exist');
  });

  it('Try to delete an order in the offline mode', function () {
    cy.intercept('*', req => {
      req.reply({
        delayMs: 5000,
        forceNetworkError: true,
      });
    }).as('req');
    cy.get(selectors.content.dashboard.orders.modal.removeButton).click();
    cy.get(selectors.content.dashboard.orders.modal.removeConfirmButton).click();
    cy.wait('@req').then(() => {
      cy.get(selectors.modal.errorPopup.view).should('exist');
      cy.get(selectors.modal.errorPopup.messageTitle).then(titles => {
        const texts = titles.toArray().map(el => el.textContent);
        expect(texts).to.include(orderDetailsKeyset.ru.error_RemoveOrder);
      });
    });
  });

  it('Close the error window after the deletion attempt', function () {
    cy.get(selectors.modal.errorPopup.closeButton).click();
    cy.get(selectors.modal.errorPopup.view).should('not.exist');

    cy.get(selectors.content.dashboard.orders.modal.removeCancelButton).click();
    cy.get(selectors.content.dashboard.orders.modal.removeConfirmWindow).should('not.exist');

    cy.get(selectors.modal.orderPopup.closeButton).click();
    cy.get(selectors.modal.orderPopup.view).should('not.exist');
    cy.get(selectors.content.dashboard.view).should('exist');
  });

  it('Open the order creation form', function () {
    cy.get(selectors.content.dashboard.couriers.table.orders.dropdown).first().click();
    cy.get(selectors.content.dashboard.couriers.table.orders.addOrderButton).click();
    cy.get(selectors.content.dashboard.orders.modal.newOrderView).should('exist');
    cy.get(selectors.content.dashboard.orders.modal.createButton).should('have.attr', 'disabled');
  });

  it('Type an address', function () {
    cy.get(selectors.content.dashboard.orders.modal.addressInput).type('Москва');
    cy.get(selectors.content.dashboard.orders.modal.addressSuggestion).first().click();
    cy.get(selectors.content.dashboard.orders.modal.map).should('exist');
    cy.window()
      .its('map')
      .should('exist')
      .then(map => {
        const [x, y] = map.getCenter();

        expect(x).to.be.closeTo(55.755, 0.05);
        expect(y).to.be.closeTo(37.617, 0.05);

        const pinCoords = (map.geoObjects.get(0).geometry as IPointGeometry).getCoordinates();
        expect(pinCoords?.[0]).to.be.closeTo(55.755, 0.05);
        expect(pinCoords?.[1]).to.be.closeTo(37.617, 0.05);
      });

    cy.get(selectors.content.dashboard.orders.modal.createButton).should(
      'not.have.attr',
      'disabled',
    );
  });

  it('Try to create an order in the offline mode', function () {
    cy.intercept('*', { middleware: true }, req => {
      req.reply({
        delay: 5000,
      });
    }).as('req');

    cy.intercept('*', req => {
      req.destroy();
    });

    cy.get(selectors.content.dashboard.orders.modal.createButton).click();
    cy.get(selectors.modal.orderPopup.loader).should('exist');

    cy.wait('@req').then(() => {
      cy.get(selectors.modal.errorPopup.view).should('exist');
      cy.get(selectors.modal.errorPopup.messageTitle).then(titles => {
        const texts = titles.toArray().map(el => el.textContent);
        expect(texts).to.include(errorPanelKeyset.ru.createOrder);
      });
    });
  });
});
