import * as commonKeyset from '../../src/translations/common';
import selectors from 'constants/selectors';
import forEach from 'lodash/forEach';

Cypress.on('uncaught:exception', () => {
  return false;
});

//https://testpalm.yandex-team.ru/courier/testcases/765
context('Break tooltip', () => {
  describe('Tooltip opens and closes', () => {
    beforeEach(() => {
      cy.openTaskById('f6a0532-5032d0a-482c632d-6bad332e?route=7');
    });

    it('Tooltip should be visible after timeline order hover', () => {
      cy.get('[data-test-anchor="timeline-order_break_5_5"] .order').trigger('mousemove', {
        force: true,
      });

      cy.get(selectors.tooltip.tooltipPanel).should('exist');
    });

    it('Tooltip should not be visible after timeline order mouseout', () => {
      cy.get('[data-test-anchor="timeline-order_break_5_5"] .order').trigger('mouseout', {
        force: true,
      });

      cy.get(selectors.tooltip.tooltipPanel).should('not.exist');
    });

    it('Tooltip should be visible after timeline order click', () => {
      cy.get('[data-test-anchor="timeline-order_break_5_5"] .order').click({
        force: true,
      });
      cy.get(selectors.tooltip.tooltipPanel).should('exist');
    });

    it('Tooltip should not be visible after tooltip cross button click', () => {
      cy.get('[data-test-anchor="timeline-order_break_5_5"] .order').click({
        force: true,
      });
      cy.get(selectors.tooltip.tooltipPanel)
        .should('exist')
        .find('.location-tooltip__close-button')
        .click({ force: true });
      cy.get(selectors.tooltip.tooltipPanel).should('not.exist');
    });

    it('Tooltip should be reopened in map after map marker click', () => {
      cy.get('[data-test-anchor="timeline-order_break_5_5"] .order').click({
        force: true,
      });
      cy.get(selectors.tooltip.tooltipPanel).should('exist').invoke('attr', 'style').as('style_1');
      cy.get('[data-test-anchor="icon-marker-id_break_5_5 icon-marker-type_break"]').click({
        force: true,
      });
      cy.get(selectors.tooltip.tooltipPanel).should('exist').invoke('attr', 'style').as('style_2');
      cy.get('@style_1').then(style_1 => {
        cy.get('@style_2').then(style_2 => {
          expect(style_1).not.to.equal(style_2);
        });
      });
    });

    it('Tooltip should be fixed after click', () => {
      cy.get('[data-test-anchor="timeline-order_break_5_5"] .order').click({
        force: true,
      });
      cy.get(selectors.tooltip.tooltipPanel).should('exist').invoke('attr', 'style').as('style_1');
      cy.get('[data-test-anchor="icon-marker-id_break_5_5 icon-marker-type_break"]').trigger(
        'mouseout',
        { force: true },
      );
      cy.get(selectors.tooltip.tooltipPanel).should('exist').invoke('attr', 'style').as('style_2');
      cy.get('@style_1').then(style_1 => {
        cy.get('@style_2').then(style_2 => {
          expect(style_1).to.equal(style_2);
        });
      });
    });
  });

  describe('Check tooltip data', () => {
    const keys = [
      { key: 'id', value: 'Идентификатор' },
      { key: 'vehicle_ref', value: 'Курьер/машина' },
      { key: 'point_ref', value: 'Номер заказа' },
      { key: 'transit_distance_m', value: 'Длина пути до точки' },
      { key: 'transit_duration_s', value: 'Время в пути' },
      { key: 'arrival_time_s', value: 'Прибытие' },
      { key: 'departure_time_s', value: 'Отбытие' },
      { key: 'waiting_duration_s', value: 'Ожидание' },
      { key: 'service_duration_s', value: 'Обслуживание заказа' },
      { key: 'weight_kg', value: 'Вес' },
      { key: 'points_at_same_stop', value: 'ID заказов в этой точке' },
    ];

    it('Display title and routing mode', () => {
      cy.get(selectors.tooltip.locationToolTip.title).should('exist');
      cy.get('*[data-test-anchor="routingMode"]')
        .find('.location-tooltip__routing-mode-label')
        .invoke('text')
        .should('eq', commonKeyset.ru.routingMode);
      cy.get('*[data-test-anchor="routingMode"]')
        .find('.location-tooltip__routing-mode-value')
        .should('not.be.null');
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
          .should('exist');
      });
    });
  });

  describe('Penalty info should exist in tooltip', () => {
    before(() => {
      cy.openTaskById('85d6615d-a0c4db05-ed19d3-c91d5c73?route=0');
    });

    it('Open tooltip', () => {
      cy.get('[data-test-anchor="timeline-order_break_26_26"] .order').click({
        force: true,
      });
      cy.get(selectors.tooltip.tooltipPanel).should('exist');
    });

    const penaltyKeys = [
      { key: 'penalty_early_fixed', value: 'Штраф за приезд раньше' },
      { key: 'penalty_early_minute', value: 'Минута приезда раньше' },
      { key: 'penalty_late_fixed', value: 'Штраф за опоздание' },
      { key: 'penalty_late_minute', value: 'Минута опоздания' },
    ];

    forEach(penaltyKeys, ({ key, value }) => {
      it(`Display ${key} in tooltip`, () => {
        cy.get(`[data-test-anchor="${key}"]`)
          .find(selectors.tooltip.locationToolTip.label)
          .invoke('text')
          .should('eq', value);
        cy.get(`[data-test-anchor="${key}"]`)
          .find(selectors.tooltip.locationToolTip.value)
          .invoke('text')
          .and('not.be.null');
      });
    });
  });
});
