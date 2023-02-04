import * as commonKeyset from '../../src/translations/common';
import * as totalMetricsKeyset from '../../src/translations/total-metrics';
import selectors from 'constants/selectors';
import forEach from 'lodash/forEach';

Cypress.on('uncaught:exception', () => {
  return false;
});

//https://testpalm.yandex-team.ru/courier/testcases/764
context('Depo tooltip', () => {
  describe('Tooltip open and closes', () => {
    before(() => {
      cy.openTaskById('59710cfd-1150b2fe-c5f12147-648dbf05?route=3');
    });

    it('Tooltip should be visible after timeline depo hover', () => {
      cy.get('[data-test-anchor="Машина 5"]')
        .find('[data-test-anchor="timeline-order_R1_1_100"] .order')
        .trigger('mousemove', { force: true });
      cy.get(selectors.tooltip.tooltipPanel).should('exist');
    });

    it('Tooltip should not be visible after timeline depo mouseout', () => {
      cy.get('[data-test-anchor="Машина 5"]')
        .find('[data-test-anchor="timeline-order_R1_1_100"] .order')
        .trigger('mouseout', { force: true });
      cy.get(selectors.tooltip.tooltipPanel).should('not.exist');
    });

    it('Tooltip should be visible and fixed after timeline depo click', () => {
      cy.get(selectors.tooltip.tooltipPanel).should('not.exist');
      cy.get('[data-test-anchor="Машина 5"]')
        .find('[data-test-anchor="timeline-order_R1_1_100"] .order')
        .click({ force: true });
      cy.get(selectors.tooltip.tooltipPanel).should('exist').invoke('attr', 'style').as('style_1');
      cy.get(selectors.app.map).trigger('mouseover', { force: true });
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

    it('Tooltip should not be visible after tooltip cross click', () => {
      cy.get(selectors.tooltip.tooltipPanel)
        .should('exist')
        .find('.location-tooltip__close-button')
        .click({ force: true });
      cy.get(selectors.tooltip.tooltipPanel).should('not.exist');
    });
  });

  describe('Check tooltip data', () => {
    const keys = [
      { key: 'id', value: totalMetricsKeyset.ru.popupColDefineition_id },
      { key: 'vehicle_ref', value: totalMetricsKeyset.ru.popupColDefineition_vehicleRef },
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
        key: 'service_duration_s',
        value: totalMetricsKeyset.ru.popupColDefineition_serviceDurationS,
      },
      { key: 'time_window', value: totalMetricsKeyset.ru.popupColDefineition_timeWindow },
      {
        key: 'points_at_same_stop',
        value: totalMetricsKeyset.ru.popupColDefineition_pointsAtSameStop,
      },
      {
        key: 'penalty_out_of_time_fixed',
        value: totalMetricsKeyset.ru.popupColDefineition_penaltyOutOfTimeFixed,
      },
      {
        key: 'penalty_out_of_time_minute',
        value: totalMetricsKeyset.ru.popupColDefineition_penaltyOutOfTimeMinute,
      },
      {
        key: 'penalty_early_fixed',
        value: totalMetricsKeyset.ru.popupColDefineition_penaltyEarlyFixed,
      },
      {
        key: 'penalty_early_minute',
        value: totalMetricsKeyset.ru.popupColDefineition_penaltyEarlyMinute,
      },
      {
        key: 'penalty_late_fixed',
        value: totalMetricsKeyset.ru.popupColDefineition_penaltyLateFixed,
      },
      {
        key: 'penalty_late_minute',
        value: totalMetricsKeyset.ru.popupColDefineition_penaltyLateMinute,
      },
      { key: 'coordinates', value: totalMetricsKeyset.ru.popupColDefinition_coordinates },
    ];

    it('Open tooltip', () => {
      cy.get('[data-test-anchor="Машина 5"]')
        .find('[data-test-anchor="timeline-order_R1_1_100"] .order')
        .click({ force: true });

      cy.get(selectors.tooltip.tooltipPanel).should('exist');
    });

    it('Title should be visible in tooltip', () => {
      cy.get(selectors.tooltip.locationToolTip.title).invoke('text').should('not.be.null');
    });

    it('Routing mode should be visible in tooltip', () => {
      cy.get('[data-test-anchor="routingMode"]')
        .find('.location-tooltip__routing-mode-label')
        .invoke('text')
        .should('eq', commonKeyset.ru.routingMode);
      cy.get('[data-test-anchor="routingMode"]')
        .find('.location-tooltip__routing-mode-value')
        .should('exist');
    });

    forEach(keys, ({ key, value }) => {
      it(`Display ${key}`, () => {
        cy.get(`[data-test-anchor="${key}"]`)
          .find(selectors.tooltip.locationToolTip.label)
          .invoke('text')
          .should('eq', value);
        cy.get(`[data-test-anchor="${key}"]`)
          .find(selectors.tooltip.locationToolTip.value)
          .invoke('text')
          .should('not.be.null');
      });
    });
  });

  describe('Check arrival on time and delays data', () => {
    it('Delay should be in tooltip', () => {
      cy.openTaskById('76e56b70-fb3b0a1f-bb41e3fe-2f2b1a6f?route=14');

      cy.get('[data-test-anchor="icon-marker-id_R5_5_100 icon-marker-type_depot"]').click();

      cy.get(selectors.tooltip.tooltipPanel)
        .should('exist')
        .find(`[data-test-anchor="failed_time_window"]`)
        .find(selectors.tooltip.locationToolTip.label)
        .invoke('text')
        .should('eq', 'Опоздание')
        .get(`[data-test-anchor="failed_time_window"]`)
        .find(selectors.tooltip.locationToolTip.value)
        .invoke('text')
        .should('contain', 'Позже на');
    });

    it('Arrival on time value should be in tooltip', () => {
      cy.openTaskById('76e56b70-fb3b0a1f-bb41e3fe-2f2b1a6f?route=0');

      cy.get('[data-test-anchor="Машина 1"] .route__route-name').click({ force: true });
      cy.get('[data-test-anchor="icon-marker-id_R0_0_100 icon-marker-type_depot"]').click({
        force: true,
      });

      cy.get(selectors.tooltip.tooltipPanel)
        .should('exist')
        .find(`[data-test-anchor="failed_time_window"]`)
        .find(selectors.tooltip.locationToolTip.label)
        .invoke('text')
        .should('eq', 'Опоздание')
        .get(`[data-test-anchor="failed_time_window"]`)
        .find(selectors.tooltip.locationToolTip.value)
        .invoke('text')
        .should('contain', 'Раньше на');
    });
  });
});

context('Garage tooltip', () => {
  describe('Check tooltip data', () => {
    before(() => {
      cy.openTaskById('6972f98-d7b656d6-d8b5ef0b-9e12746f?route=0');
    });

    const keys = [
      { key: 'id', value: totalMetricsKeyset.ru.popupColDefineition_id },
      { key: 'vehicle_ref', value: totalMetricsKeyset.ru.popupColDefineition_vehicleRef },
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
      {
        key: 'waiting_duration_s',
        value: totalMetricsKeyset.ru.popupColDefineition_waitingDurationS,
      },
      {
        key: 'service_duration_s',
        value: totalMetricsKeyset.ru.popupColDefineition_serviceDurationS,
      },
      { key: 'time_window', value: totalMetricsKeyset.ru.popupColDefineition_timeWindow },
      {
        key: 'points_shared_service_duration',
        value: totalMetricsKeyset.ru.popupColDefineition_sharedServiceDurationS,
      },
      {
        key: 'points_at_same_stop',
        value: totalMetricsKeyset.ru.popupColDefineition_pointsAtSameStop,
      },
      { key: 'required_tags', value: totalMetricsKeyset.ru.popupColDefineition_requiredTags },
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
      cy.get('[data-test-anchor="timeline-order_Костенков1"] .order').click({
        force: true,
      });

      cy.get(selectors.tooltip.tooltipPanel).should('exist');
    });

    it('Display title', () => {
      cy.get(selectors.tooltip.locationToolTip.title).invoke('text').should('not.be.null');
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

    forEach(keys, ({ key, value }) => {
      it(`Display ${key}`, () => {
        cy.get(`[data-test-anchor="${key}"]`)
          .find(selectors.tooltip.locationToolTip.label)
          .invoke('text')
          .should('eq', value);
        cy.get(`[data-test-anchor="${key}"]`)
          .find(selectors.tooltip.locationToolTip.value)
          .invoke('text')
          .should('not.be.null');
      });
    });

    it('Point type should equal garage in tooltip', () => {
      cy.get(`[data-test-anchor="point_type"]`)
        .find(selectors.tooltip.locationToolTip.value)
        .invoke('text')
        .should('eq', 'garage');
    });
  });
});
