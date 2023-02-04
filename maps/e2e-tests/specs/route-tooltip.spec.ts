import * as routeCostKeyset from '../../src/translations/route-cost';
import selectors from 'constants/selectors';
import forEach from 'lodash/forEach';

Cypress.on('uncaught:exception', () => {
  return false;
});

// https://testpalm.yandex-team.ru/courier/testcases/760
context('Routes tooltip', () => {
  it('Tooltip should be visible after route name hover', () => {
    cy.openTaskById('b3a42bb0-23fe4341-f8c47510-1f219ea7?route=14');
    cy.get(selectors.routesTimeline.tabSwitcher).click({ force: true });

    cy.get('*[data-test-anchor="Dodge"]')
      .first()
      .find('.metrics__cell_first')
      .trigger('mousemove', { force: true });
    cy.get('.mvrp-map-custom-tooltip_visible').should('exist');
  });

  it('Tooltip should not be visible after route name mouse out', () => {
    cy.get('*[data-test-anchor="Dodge"]')
      .first()
      .find('.metrics__cell_first')
      .trigger('mouseout', { force: true });
    cy.get('.mvrp-map-custom-tooltip_visible').should('not.exist');
  });

  describe('Check tooltip properties', () => {
    const keys = [
      { key: 'vehicle.tags', value: routeCostKeyset.ru.colDefineition_vehicleTags },
      { key: 'vehicle.cost.fixed', value: routeCostKeyset.ru.colDefineition_vehicleCostFixed },
      { key: 'vehicle.cost.run', value: routeCostKeyset.ru.colDefineition_vehicleCostRun },
      { key: 'vehicle.cost.km', value: routeCostKeyset.ru.colDefineition_vehicleCostKm },
      { key: 'vehicle.cost.hour', value: routeCostKeyset.ru.colDefineition_vehicleCostHour },
      {
        key: 'vehicle.cost.location',
        value: routeCostKeyset.ru.colDefineition_vehicleCostLocation,
      },
      { key: 'metrics.total_cost', value: routeCostKeyset.ru.colDefineition_metricsTotalCost },
      {
        key: 'metrics.total_penalty',
        value: routeCostKeyset.ru.colDefineition_metricsTotalPenalty,
      },
    ];

    before(() => {
      cy.openTaskById('b3a42bb0-23fe4341-f8c47510-1f219ea7?route=14');
      cy.get(selectors.routesTimeline.tabSwitcher).click({ force: true });
      cy.get('*[data-test-anchor="Dodge"]')
        .first()
        .find('.metrics__cell_first')
        .trigger('mousemove', { force: true });
    });

    forEach(keys, ({ key, value }) => {
      it(`Display ${key} in tooltip`, () => {
        cy.get('.mvrp-map-custom-tooltip_visible')
          .find(`[data-test-anchor="${key}"] .route-tooltip-content__label`)
          .invoke('text')
          .should('eq', value);
        cy.get('.mvrp-map-custom-tooltip_visible')
          .find(`[data-test-anchor="${key}"]`)
          .find('.route-tooltip-content__value')
          .invoke('text')
          .should('not.be.null');
        cy.get('.mvrp-map-custom-tooltip_visible')
          .find(`[data-test-anchor="${key}"]`)
          .find('.route-tooltip-content__units')
          .invoke('width')
          .should('be.gte', 19);
      });
    });
  });

  it('Total cost property should exist in tooltip', () => {
    cy.openTaskById('57a2673-dd43fd3-de7b280a-cddbf15e');
    cy.get(selectors.routesTimeline.tabSwitcher).click({ force: true });

    cy.get('*[data-test-anchor="TestMVRP2"]')
      .find('.metrics__cell_first')
      .trigger('mousemove', { force: true });
    cy.get('.mvrp-map-custom-tooltip_visible')
      .find('tr')
      .should('have.length', 1)
      .invoke('attr', 'data-test-anchor')
      .should('eq', 'metrics.total_cost');
  });

  it('Tooltip should be autosize and have ', () => {
    cy.openTaskById('c7eba041-6378e45-386a6dab-13f94b21?route=43');
    cy.get(selectors.routesTimeline.tabSwitcher).click({ force: true });

    cy.get('*[data-test-anchor="Асеев Василий-612087366"]')
      .find('.metrics__cell_first')
      .trigger('mousemove', { force: true });
    cy.get('.mvrp-map-custom-tooltip_visible').invoke('width').should('be.lte', 560);
    cy.get('.mvrp-map-custom-tooltip_visible')
      .find('[title="vehicle.tags"]')
      .invoke('width')
      .should('be.lte', 560);
  });
});
