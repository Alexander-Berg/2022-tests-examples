import * as commonKeyset from '../../src/translations/common';
import * as totalMetricsKeyset from '../../src/translations/total-metrics';
import selectors from 'constants/selectors';
import forEach from 'lodash/forEach';

Cypress.on('uncaught:exception', () => {
  return false;
});

context('Walking order tooltip', () => {
  describe('Tooltip open and closes', () => {
    it('Tooltip should be visible after dropped pin hover', () => {
      cy.openTaskById('159e9038-cc0e28a0-4a647fc-b0c9c031?route=0');

      cy.get('[data-test-anchor="icon-marker-id_42 icon-marker-type_location"]')
        .first()
        .trigger('mousemove', { force: true });
      cy.get('[data-test-anchor="tooltip_icon-marker__tooltip_42"]')
        .parent()
        .should('have.class', 'mvrp-map-custom-tooltip_visible');
      cy.get('[data-test-anchor="icon-marker-id_42 icon-marker-type_location"]')
        .first()
        .trigger('mouseout');
      cy.get('[data-test-anchor="tooltip_icon-marker__tooltip_42"]')
        .parent()
        .should('not.have.class', 'mvrp-map-custom-tooltip_visible');
    });

    it('Tooltip should be fixed after click', () => {
      cy.openTaskById('159e9038-cc0e28a0-4a647fc-b0c9c031?route=0');

      cy.get('[data-test-anchor="icon-marker-id_42 icon-marker-type_location"]')
        .first()
        .click({ force: true });
      cy.get(selectors.tooltip.tooltipPanel).should('exist').invoke('attr', 'style').as('style_1');
      cy.get('[data-test-anchor="icon-marker-id_42 icon-marker-type_location"]')
        .first()
        .trigger('mouseout', { force: true });
      cy.get('.mvrp-map-custom-tooltip_visible')
        .should('exist')
        .invoke('attr', 'style')
        .as('style_2');
      cy.get('@style_1').then(style_1 => {
        cy.get('@style_2').then(style_2 => {
          expect(style_1).to.equal(style_2);
        });
      });
    });

    it('Tooltip should be closed after cross button click', () => {
      cy.get('[data-test-anchor="tooltip_icon-marker__tooltip_42"]')
        .find('.location-tooltip__close-button')
        .click({ force: true });

      cy.get('[data-test-anchor="tooltip_icon-marker__tooltip_42"]')
        .parent()
        .should('not.have.class', 'mvrp-map-custom-tooltip_visible');
    });
  });

  describe('Check tooltip data', () => {
    const keys = [
      { key: 'id', value: totalMetricsKeyset.ru.popupColDefineition_id },
      { key: 'point_ref', value: totalMetricsKeyset.ru.popupColDefineition_pointRef },
      {
        key: 'transit_distance_m',
        value: totalMetricsKeyset.ru.popupColDefineition_transitDistanceM,
      },
      {
        key: 'transit_duration_s',
        value: totalMetricsKeyset.ru.popupColDefineition_transitDurationS,
      },
      { key: 'arrival_time_s', value: totalMetricsKeyset.ru.popupColDefineition_arrivalTimeS },
      { key: 'departure_time_s', value: totalMetricsKeyset.ru.popupColDefineition_departureTimeS },
      {
        key: 'waiting_duration_s',
        value: totalMetricsKeyset.ru.popupColDefineition_waitingDurationS,
      },
      {
        key: 'points_shared_service_duration',
        value: totalMetricsKeyset.ru.popupColDefineition_sharedServiceDurationS,
      },
      {
        key: 'service_duration_s',
        value: totalMetricsKeyset.ru.popupColDefineition_serviceDurationS,
      },
      { key: 'time_window', value: totalMetricsKeyset.ru.popupColDefineition_timeWindow },
      { key: 'weight_kg', value: totalMetricsKeyset.ru.popupColDefineition_weightKg },
      { key: 'dimensions', value: totalMetricsKeyset.ru.popupColDefineition_dimensions },
      { key: 'placement', value: totalMetricsKeyset.ru.popupColDefineition_placement },
      {
        key: 'points_at_same_stop',
        value: totalMetricsKeyset.ru.popupColDefineition_pointsAtSameStop,
      },
      { key: 'penalty_drop', value: totalMetricsKeyset.ru.popupColDefineition_penaltyDrop },
      {
        key: 'penalty_out_of_time_fixed',
        value: totalMetricsKeyset.ru.popupColDefineition_penaltyOutOfTimeFixed,
      },
      {
        key: 'penalty_out_of_time_minute',
        value: totalMetricsKeyset.ru.popupColDefineition_penaltyOutOfTimeMinute,
      },
      { key: 'point_type', value: totalMetricsKeyset.ru.popupColDefineition_pointType },
      { key: 'coordinates', value: totalMetricsKeyset.ru.popupColDefinition_coordinates },
    ];

    it('Open tooltip', () => {
      cy.openTaskById('159e9038-cc0e28a0-4a647fc-b0c9c031?route=0');

      cy.get('[data-test-anchor="icon-marker-id_42 icon-marker-type_location"]')
        .first()
        .click({ force: true });
      cy.get(selectors.tooltip.tooltipPanel).should('exist');
    });

    it('Display title tooltip', () => {
      cy.get(selectors.tooltip.locationToolTip.title).should('exist');
    });

    it('Display routing mode', () => {
      cy.get('[data-test-anchor="routingMode"]')
        .find('.location-tooltip__routing-mode-label')
        .invoke('text')
        .should('eq', commonKeyset.ru.routingMode);
      cy.get('[data-test-anchor="routingMode"]')
        .find('.location-tooltip__routing-mode-value')
        .should('exist');
    });

    it('Open hint on routing mode', () => {
      cy.get('[data-test-anchor="routingMode"]')
        .find('.location-tooltip__routing-mode-label')
        .trigger('mouseenter')
        .trigger('mousemove');
      cy.wait(300);
      cy.get('[id="routing-mode-hint"]').get('[data-test-anchor="location-hint-text-content"]');
    });

    forEach(keys, ({ key, value }) => {
      it(`Display ${key} in tooltip`, () => {
        cy.get(`[data-test-anchor="${key}"]`)
          .find(selectors.tooltip.locationToolTip.label)
          .invoke('text')
          .should('eq', value);
        cy.get(`[data-test-anchor="${key}"]`)
          .find(selectors.tooltip.locationToolTip.value)
          .invoke('text')
          .should('exist');
      });
    });

    it('Point type should equal parking in tooltip', () => {
      cy.get(`[data-test-anchor="point_type"]`)
        .find(selectors.tooltip.locationToolTip.value)
        .invoke('text')
        .should('eq', 'delivery');
    });
  });
});

//https://testpalm.yandex-team.ru/courier/testcases/926
context('Parking tooltip', () => {
  describe('Tooltip open and closes', () => {
    before(() => {
      cy.openTaskById('159e9038-cc0e28a0-4a647fc-b0c9c031?route=0');
    });

    it('Tooltip should be visible after dropped pin hover', () => {
      cy.get('[data-test-anchor="icon-marker-id_51 icon-marker-type_location"]')
        .first()
        .trigger('mousemove', { force: true });
      cy.get('[data-test-anchor="tooltip_icon-marker__tooltip_51"]')
        .parent()
        .should('have.class', 'mvrp-map-custom-tooltip_visible');
      cy.get('[data-test-anchor="icon-marker-id_51 icon-marker-type_location"]')
        .first()
        .trigger('mouseout', { force: true });
      cy.get('[data-test-anchor="tooltip_icon-marker__tooltip_51"]')
        .parent()
        .should('not.have.class', 'mvrp-map-custom-tooltip_visible');
    });

    it('Tooltip should be fixed after click', () => {
      cy.get('[data-test-anchor="icon-marker-id_51 icon-marker-type_location"]')
        .first()
        .click({ force: true });
      cy.get(selectors.tooltip.tooltipPanel).should('exist').invoke('attr', 'style').as('style_1');
      cy.get('[data-test-anchor="icon-marker-id_51 icon-marker-type_location"]')
        .first()
        .trigger('mouseout', { force: true });
      cy.get('.mvrp-map-custom-tooltip_visible')
        .should('exist')
        .invoke('attr', 'style')
        .as('style_2');
      cy.get('@style_1').then(style_1 => {
        cy.get('@style_2').then(style_2 => {
          expect(style_1).to.equal(style_2);
        });
      });
    });

    it('Tooltip should be closed after cross button click', () => {
      cy.get('[data-test-anchor="tooltip_icon-marker__tooltip_51"]')
        .find('.location-tooltip__close-button')
        .click({ force: true });

      cy.get('[data-test-anchor="tooltip_icon-marker__tooltip_51"]')
        .parent()
        .should('not.have.class', 'mvrp-map-custom-tooltip_visible');
    });
  });

  describe('Check tooltip data', () => {
    const keys = [
      { key: 'point_ref', value: totalMetricsKeyset.ru.popupColDefineition_pointRef },
      { key: 'id', value: totalMetricsKeyset.ru.popupColDefineition_id },
      {
        key: 'transit_distance_m',
        value: totalMetricsKeyset.ru.popupColDefineition_transitDistanceM,
      },
      {
        key: 'transit_duration_s',
        value: totalMetricsKeyset.ru.popupColDefineition_transitDurationS,
      },
      { key: 'arrival_time_s', value: totalMetricsKeyset.ru.popupColDefineition_arrivalTimeS },
      {
        key: 'waiting_duration_s',
        value: totalMetricsKeyset.ru.popupColDefineition_waitingDurationS,
      },
      {
        key: 'points_shared_service_duration',
        value: totalMetricsKeyset.ru.popupColDefineition_sharedServiceDurationS,
      },
      {
        key: 'service_duration_s',
        value: totalMetricsKeyset.ru.popupColDefineition_serviceDurationS,
      },
      {
        key: 'points_at_same_stop',
        value: totalMetricsKeyset.ru.popupColDefineition_pointsAtSameStop,
      },
      { key: 'point_type', value: totalMetricsKeyset.ru.popupColDefineition_pointType },
    ];

    it('Open tooltip', () => {
      cy.get('[data-test-anchor="icon-marker-id_51 icon-marker-type_location"]')
        .first()
        .click({ force: true });
      cy.get(selectors.tooltip.tooltipPanel).should('exist');
    });

    it('Display title tooltip', () => {
      cy.get(selectors.tooltip.locationToolTip.title).should('exist');
    });

    it('Display routing mode', () => {
      cy.get('[data-test-anchor="routingMode"]')
        .find('.location-tooltip__routing-mode-label')
        .invoke('text')
        .should('eq', commonKeyset.ru.routingMode);
      cy.get('[data-test-anchor="routingMode"]')
        .find('.location-tooltip__routing-mode-value')
        .should('exist');
    });

    it('Open hint on routing mode', () => {
      cy.get('[data-test-anchor="routingMode"]')
        .find('.location-tooltip__routing-mode-label')
        .trigger('mouseenter')
        .trigger('mousemove');
      cy.wait(300);
      cy.get('[id="routing-mode-hint"]').get('[data-test-anchor="location-hint-text-content"]');
    });

    forEach(keys, ({ key, value }) => {
      it(`Display ${key} in tooltip`, () => {
        cy.get(`[data-test-anchor="${key}"]`)
          .find(selectors.tooltip.locationToolTip.label)
          .invoke('text')
          .should('eq', value);
        cy.get(`[data-test-anchor="${key}"]`)
          .find(selectors.tooltip.locationToolTip.value)
          .invoke('text')
          .should('exist');
      });
    });

    it('Point type should equal parking in tooltip', () => {
      cy.get(`[data-test-anchor="point_type"]`)
        .find(selectors.tooltip.locationToolTip.value)
        .invoke('text')
        .should('eq', 'parking');
    });
  });
});
