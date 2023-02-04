import * as totalMetricsKeyset from '../../src/translations/total-metrics';
import selectors from 'constants/selectors';
import forEach from 'lodash/forEach';

Cypress.on('uncaught:exception', () => {
  return false;
});

// https://testpalm.yandex-team.ru/courier/testcases/751
context('Order Tooltip', () => {
  describe('Standard order', () => {
    before(() => {
      cy.openTaskById('6972f98-d7b656d6-d8b5ef0b-9e12746f');
    });

    it('Tooltip should be visible after timeline order block hover', () => {
      cy.get('[data-test-anchor="timeline-order_16"]')
        .find(selectors.routesTimeline.order)
        .trigger('mousemove');

      cy.get(selectors.tooltip.tooltipPanel).should('exist');
    });

    it('Tooltip should be closed after timeline order block mouseout', () => {
      cy.get('[data-test-anchor="timeline-order_16"]')
        .find(selectors.routesTimeline.order)
        .trigger('mousemove');
      cy.get('[data-test-anchor="timeline-order_16"]')
        .find(selectors.routesTimeline.order)
        .trigger('mouseout');

      cy.get(selectors.tooltip.tooltipPanel).should('not.exist');
    });

    it('Tooltip should be visible after timeline order block click', () => {
      cy.get('[data-test-anchor="timeline-order_16"]')
        .find(selectors.routesTimeline.order)
        .click()
        .trigger('mouseout');

      cy.get(selectors.tooltip.tooltipPanel).should('exist');
    });

    it('Tooltip should be closed after cross button click', () => {
      cy.get('[data-test-anchor="timeline-order_16"]').find(selectors.routesTimeline.order).click();
      cy.get(selectors.tooltip.locationToolTip.cross).click();

      cy.get(selectors.tooltip.tooltipPanel).should('not.exist');
    });

    it('Tooltip should be closed after click out of tooltip', () => {
      cy.get('[data-test-anchor="timeline-order_16"]').find(selectors.routesTimeline.order).click();
      cy.get(selectors.tooltip.locationToolTip.cross).click();

      cy.get(selectors.tooltip.tooltipPanel).should('not.exist');
    });

    context('Displays data', () => {
      before(() => {
        cy.openTaskById('6972f98-d7b656d6-d8b5ef0b-9e12746f');
      });
      it('Displays title and description', () => {
        cy.get('[data-test-anchor="timeline-order_16"]')
          .find(selectors.routesTimeline.order)
          .click();

        cy.get(selectors.tooltip.locationToolTip.title).should('exist');
        cy.get(selectors.tooltip.locationToolTip.description).should('exist');
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

      forEach(keys, ({ key, value }) => {
        it(`Display ${key}`, () => {
          cy.get(selectors.tooltip.tooltipPanel)
            .find(`[data-test-anchor="${key}"]`)
            .find(selectors.tooltip.locationToolTip.label)
            .invoke('text')
            .should('eq', value);
          cy.get(selectors.tooltip.tooltipPanel)
            .find(`[data-test-anchor="${key}"]`)
            .find(selectors.tooltip.locationToolTip.value)
            .invoke('text')
            .should('exist');
        });
      });
    });

    it('Tooltip should be scrollable', () => {
      cy.get('[data-test-anchor="timeline-order_16"]').find(selectors.routesTimeline.order).click();

      cy.get(selectors.tooltip.locationToolTip.inner)
        .scrollTo('bottom')
        .contains('delivery')
        .should('be.visible');

      cy.get(selectors.tooltip.locationToolTip.inner)
        .scrollTo('top')
        .contains('16')
        .should('be.visible');
    });

    it('Tooltip should had max height', () => {
      cy.get(selectors.tooltip.tooltipPanel).invoke('height').should('be.lte', 520);
    });

    it('Toolltip should change info after click on another order', () => {
      cy.get('[data-test-anchor="timeline-order_16"]').find(selectors.routesTimeline.order).click();
      cy.get(selectors.tooltip.tooltipPanel)
        .find(selectors.tooltip.locationToolTip.title)
        .invoke('text')
        .as('tooltip_1');
      cy.get('[data-test-anchor="timeline-order_12"]').find(selectors.routesTimeline.order).click();
      cy.get(selectors.tooltip.tooltipPanel)
        .find(selectors.tooltip.locationToolTip.title)
        .invoke('text')
        .as('tooltip_2');
      cy.get('@tooltip_1').then(tooltip_1 => {
        cy.get('@tooltip_2').then(tooltip_2 => {
          expect(tooltip_1).not.to.equal(tooltip_2);
        });
      });
    });

    it('Toolltip opened in timeline should change info after click on map marker', () => {
      cy.get('[data-test-anchor="timeline-order_16"]').find(selectors.routesTimeline.order).click();
      cy.get(selectors.tooltip.tooltipPanel)
        .find(selectors.tooltip.locationToolTip.title)
        .invoke('text')
        .as('tooltip_1');
      cy.get('[data-test-anchor*="icon-marker-id_27"]').click();
      cy.get(selectors.tooltip.tooltipPanel)
        .find(selectors.tooltip.locationToolTip.title)
        .invoke('text')
        .as('tooltip_2');
      cy.get('@tooltip_1').then(tooltip_1 => {
        cy.get('@tooltip_2').then(tooltip_2 => {
          expect(tooltip_1).not.to.equal(tooltip_2);
        });
      });
    });

    it('Tooltip opened in map should change info after click another order pin in map', () => {
      cy.get('[data-test-anchor*="icon-marker-id_27"]').click();
      cy.get(selectors.tooltip.tooltipPanel)
        .find(selectors.tooltip.locationToolTip.title)
        .invoke('text')
        .as('tooltip_1');
      cy.get('[data-test-anchor*="icon-marker-id_16"]').click();
      cy.get(selectors.tooltip.tooltipPanel)
        .find(selectors.tooltip.locationToolTip.title)
        .invoke('text')
        .as('tooltip_2');
      cy.get('@tooltip_1').then(tooltip_1 => {
        cy.get('@tooltip_2').then(tooltip_2 => {
          expect(tooltip_1).not.to.equal(tooltip_2);
        });
      });
    });
  });

  describe('Late order', () => {
    before(() => {
      cy.openTaskById('59710cfd-1150b2fe-c5f12147-648dbf05?route=2');
    });

    it('Displays info about being late', () => {
      const keys = [
        { key: 'failed_time_window', value: 'Опоздание' },
        { key: 'penalty_drop', value: 'Штраф за недоставку' },
        { key: 'penalty_out_of_time_fixed', value: 'Штраф за нарушение окна' },
        { key: 'penalty_out_of_time_minute', value: 'Минута нарушения окна' },
        { key: 'penalty_early_fixed', value: 'Штраф за приезд раньше' },
        { key: 'penalty_early_minute', value: 'Минута приезда раньше' },
        { key: 'penalty_late_fixed', value: 'Штраф за опоздание' },
        { key: 'penalty_late_minute', value: 'Минута опоздания' },
      ];
      cy.get('[data-test-anchor="timeline-order_Заказ 78"]')
        .find(selectors.routesTimeline.order)
        .trigger('mousemove');

      cy.get(selectors.tooltip.tooltipPanel).should('exist');

      forEach(keys, ({ key, value }) => {
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

    it('Shows info about unfeasible reasons', () => {
      cy.get('[data-test-anchor="timeline-order_Заказ 38"]')
        .find(selectors.routesTimeline.order)
        .click();

      cy.get(selectors.tooltip.locationToolTip.inner)
        .scrollTo('bottom')
        .find('[data-test-anchor="unfeasible_reasons"]')
        .find(selectors.tooltip.locationToolTip.label)
        .invoke('text')
        .should('exist');

      cy.get(selectors.tooltip.locationToolTip.inner)
        .scrollTo('bottom')
        .find('[data-test-anchor="unfeasible_reasons"]')
        .find(selectors.tooltip.locationToolTip.value)
        .invoke('text')
        .should('exist');
    });
  });

  //https://st.yandex-team.ru/BBGEO-12047
  describe('Tooltip from the right edge', () => {
    before(() => {
      cy.openTaskById('55040f06-fb09ba33-4c9db75c-3d4772a7?route=53');
    });

    it('Opens', () => {
      cy.get(selectors.routesTimeline.table).scrollTo('bottomLeft');

      cy.get('[data-test-anchor="timeline-order_Заказ 778"]')
        .find(selectors.routesTimeline.order)
        .click({ scrollBehavior: false });
    });

    it('Scrolls up and down', () => {
      cy.get(selectors.tooltip.locationToolTip.inner)
        .scrollTo('bottom')
        .contains('delivery')
        .should('be.visible');

      cy.get(selectors.tooltip.locationToolTip.inner)
        .scrollTo('top')
        .contains('Заказ 778')
        .should('be.visible');
    });
  });
});
