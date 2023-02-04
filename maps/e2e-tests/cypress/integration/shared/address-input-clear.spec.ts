import * as createRouteFormKeyset from '../../../../src/translations/create-route-form';
import * as courierMapKeyset from '../../../../src/translations/courier-map';
import * as importKeyset from '../../../../src/translations/import';
import selectors from '../../../src/constants/selectors';

// @see https://testpalm.yandex-team.ru/courier/testcases/551
export const addressInputOnMapClearSharedSpec = (): void => {
  describe('Address input clear', () => {
    it('should show order on map', () => {
      cy.get(selectors.modal.importRoutes.map.ordersHead)
        .contains(importKeyset.ru.geoCodingSections_good)
        .click();
      cy.get(selectors.modal.importRoutes.map.orderRow).first().click();

      cy.get(selectors.modal.importRoutes.map.panel.container).should('exist');
      cy.get(selectors.modal.importRoutes.map.panel.column)
        .contains(courierMapKeyset.ru.tableColumn_ordersCount)
        .should('exist');
      cy.get(selectors.modal.importRoutes.map.panel.column)
        .contains(createRouteFormKeyset.ru.label_number)
        .should('exist');
      cy.get(selectors.modal.importRoutes.map.panel.container)
        .find(selectors.suggest.container)
        .should('exist');
      cy.get(selectors.modal.importRoutes.map.panel.container)
        .find(selectors.suggest.clear)
        .should('exist');
      cy.get(selectors.modal.importRoutes.map.panel.button)
        .contains(importKeyset.ru.geoCodingPanel_next)
        .should('exist');
      cy.get(selectors.modal.importRoutes.map.panel.closeButton).should('exist');
    });

    it('should clear address input', () => {
      cy.get(selectors.suggest.clear).click();

      cy.get(selectors.suggest.input).should('have.value', '');
    });

    it('should show suggests when typing', () => {
      cy.get(selectors.suggest.input).type('москва');

      cy.get(selectors.suggest.list).should('exist');
      cy.get(selectors.suggest.listOptions).should('have.length.above', 0);
    });

    it('should clear address input after suggests', () => {
      cy.get(selectors.suggest.clear).click();

      cy.get(selectors.suggest.input).should('have.value', '');
      cy.get(selectors.suggest.list).should('exist');
      cy.get(selectors.suggest.noOptions).should('exist');
    });

    it('should clear address input after suggest select', () => {
      cy.get(selectors.suggest.input).type('москва');
      cy.get(selectors.suggest.listOptions).first().click();
      cy.get(selectors.suggest.clear).click();

      cy.get(selectors.suggest.input).should('have.value', '');
    });
  });
};
