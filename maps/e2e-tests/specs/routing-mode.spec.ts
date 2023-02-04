import selectors from 'constants/selectors';
import { RoutingModes } from '../../src/types/constants';

Cypress.on('uncaught:exception', () => {
  return false;
});

// https://testpalm.yandex-team.ru/testcase/courier-752
context('Routing Mode', () => {
  beforeEach(() => {
    cy.openTaskById('30ce2c3d-2fb2f0df-79680613-5c495d25');
  });

  // https://testpalm.yandex-team.ru/testcase/courier-752
  context('Check Routes Mode Table Icons', () => {
    it('Driving Icon', () => {
      cy.get(selectors.routesTimeline.table)
        .find('*[data-test-anchor="Машина 10"] .routes-mode')
        .invoke('attr', 'data-test-anchor')
        .should('eq', `routing-mode-icon_${RoutingModes.driving}`);
    });

    it('Transit Icon', () => {
      cy.get(selectors.routesTimeline.table)
        .find('*[data-test-anchor="Машина 14"] .routes-mode')
        .invoke('attr', 'data-test-anchor')
        .should('eq', `routing-mode-icon_${RoutingModes.transit}`);
    });

    it('Truck Icon', () => {
      cy.get(selectors.routesTimeline.table)
        .find('*[data-test-anchor="Машина 2"] .routes-mode')
        .invoke('attr', 'data-test-anchor')
        .should('eq', `routing-mode-icon_${RoutingModes.truck}`);
    });

    it('Walking Icon', () => {
      cy.get(selectors.routesTimeline.table)
        .find('*[data-test-anchor="Машина 28"] .routes-mode')
        .invoke('attr', 'data-test-anchor')
        .should('eq', `routing-mode-icon_${RoutingModes.walking}`);
    });
  });

  // https://testpalm.yandex-team.ru/courier/testcases/928
  context('Check Routes Mode Tooltip Icons', () => {
    it('Driving Icon', () => {
      cy.get('[data-test-anchor="timeline-order_Заказ 45"] .order').click();
      cy.get(selectors.tooltip.tooltipPanel)
        .find('[data-test-anchor="routingMode"]')
        .find('.location-tooltip__routing-mode-icon')
        .invoke('attr', 'data-test-anchor')
        .should('eq', `routing-mode-icon_${RoutingModes.driving}`);

      cy.get(selectors.tooltip.tooltipPanel)
        .find('[data-test-anchor="routingMode"]')
        .contains('Автомобиль');
    });

    it('Transit Icon', () => {
      cy.get('[data-test-anchor="timeline-order_Заказ 116"] .order').click();
      cy.get(selectors.tooltip.tooltipPanel)
        .find('[data-test-anchor="routingMode"]')
        .find('.location-tooltip__routing-mode-icon')
        .invoke('attr', 'data-test-anchor')
        .should('eq', `routing-mode-icon_${RoutingModes.transit}`);

      cy.get(selectors.tooltip.tooltipPanel)
        .find('[data-test-anchor="routingMode"]')
        .contains('Общественный транспорт');
    });

    it('Truck Icon', () => {
      cy.get('[data-test-anchor="timeline-order_Заказ 9"] .order').click();
      cy.get(selectors.tooltip.tooltipPanel)
        .find('[data-test-anchor="routingMode"]')
        .find('.location-tooltip__routing-mode-icon')
        .invoke('attr', 'data-test-anchor')
        .should('eq', `routing-mode-icon_${RoutingModes.truck}`);

      cy.get(selectors.tooltip.tooltipPanel)
        .find('[data-test-anchor="routingMode"]')
        .contains('Грузовой автомобиль');
    });

    it('Walking Icon', () => {
      cy.get('[data-test-anchor="timeline-order_Заказ 14"] .order').click();
      cy.get(selectors.tooltip.tooltipPanel)
        .find('[data-test-anchor="routingMode"]')
        .find('.location-tooltip__routing-mode-icon')
        .invoke('attr', 'data-test-anchor')
        .should('eq', `routing-mode-icon_${RoutingModes.walking}`);

      cy.get(selectors.tooltip.tooltipPanel)
        .find('[data-test-anchor="routingMode"]')
        .contains('Пешком');
    });
  });
});

context('Mixed Routing Mode', () => {
  beforeEach(() => {
    cy.openTaskById('38dbaa0c-aa2d2e63-c8a5f676-6e6afbae');
  });

  it('Walking and Driving Icons', () => {
    cy.get(selectors.routesTimeline.table)
      .find('*[data-test-anchor="Курьер 1"] .route__route-name')
      .find(`*[data-test-anchor="routing-mode-icon_${RoutingModes.walking}"]`)
      .should('be.visible');

    cy.get(selectors.routesTimeline.table)
      .find('*[data-test-anchor="Курьер 1"] .route__route-name')
      .find(`*[data-test-anchor="routing-mode-icon_${RoutingModes.driving}"]`)
      .should('be.visible');
  });
});
