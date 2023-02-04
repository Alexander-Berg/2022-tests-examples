import * as totalMetricsKeyset from '../../src/translations/total-metrics';
import selectors from 'constants/selectors';
import forEach from 'lodash/forEach';

Cypress.on('uncaught:exception', () => {
  return false;
});

//https://testpalm.yandex-team.ru/courier/testcases/763
context('Dropped orders tooltip', () => {
  describe('Tooltip open and closes', () => {
    before(() => {
      cy.openTaskById('f3b29a3c-92d1811d-a837198e-56bb94a8?route=dropped');
    });

    it('Tooltip should be visible after dropped pin hover', () => {
      cy.get('[data-test-anchor="icon-marker-id_95795488 icon-marker-type_dropped"]').trigger(
        'mousemove',
        { force: true },
      );
      cy.get('[data-test-anchor="tooltip_icon-marker__tooltip_95795488"]')
        .parent()
        .should('have.class', 'mvrp-map-custom-tooltip_visible');
      cy.get('[data-test-anchor="icon-marker-id_95795488 icon-marker-type_dropped"]').trigger(
        'mouseout',
        { force: true },
      );
      cy.get('[data-test-anchor="tooltip_icon-marker__tooltip_95795488"]')
        .parent()
        .should('not.have.class', 'mvrp-map-custom-tooltip_visible');
    });

    it('Tooltip should be fixed after click', () => {
      cy.get('[data-test-anchor="icon-marker-id_95795488 icon-marker-type_dropped"]').click({
        force: true,
      });
      cy.get(selectors.tooltip.tooltipPanel).should('exist').invoke('attr', 'style').as('style_1');
      cy.get('[data-test-anchor="icon-marker-id_95795488 icon-marker-type_dropped"]').trigger(
        'mouseout',
        { force: true },
      );
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

    it('Tooltip should have max height and be scrollable', () => {
      cy.get(selectors.tooltip.tooltipPanel).invoke('height').should('be.lte', 520);
      cy.get(selectors.tooltip.scrollableContainer)
        .scrollTo('bottom')
        .find('[data-test-anchor="penalty_out_of_time_minute"]')
        .should('be.visible');
    });

    it('Tooltip should be closed after cross button click', () => {
      cy.get('[data-test-anchor="tooltip_icon-marker__tooltip_95795488"]')
        .find('.location-tooltip__close-button')
        .click({ force: true });

      cy.get('[data-test-anchor="tooltip_icon-marker__tooltip_95795488"]')
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
      { key: 'drop_reason', value: totalMetricsKeyset.ru.popupColDefineition_dropReason },
      {
        key: 'points_at_same_stop',
        value: totalMetricsKeyset.ru.popupColDefineition_pointsAtSameStop,
      },
      { key: 'required_tags', value: totalMetricsKeyset.ru.popupColDefineition_requiredTags },
      { key: 'penalty_drop', value: totalMetricsKeyset.ru.popupColDefineition_penaltyDrop },
      {
        key: 'penalty_out_of_time_fixed',
        value: totalMetricsKeyset.ru.popupColDefineition_penaltyOutOfTimeFixed,
      },
      {
        key: 'penalty_out_of_time_minute',
        value: totalMetricsKeyset.ru.popupColDefineition_penaltyOutOfTimeMinute,
      },
      { key: 'departure_time_s', value: totalMetricsKeyset.ru.popupColDefineition_departureTimeS },
    ];

    it('Open tooltip', () => {
      cy.get('[data-test-anchor="icon-marker-id_95795488 icon-marker-type_dropped"]').click({
        force: true,
      });
      cy.get(selectors.tooltip.tooltipPanel).should('exist');
    });

    it('Display title tooltip', () => {
      cy.get(selectors.tooltip.locationToolTip.title).should('exist');
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
  });
});
