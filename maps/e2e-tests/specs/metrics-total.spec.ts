import selectors from 'constants/selectors';
import forEach from 'lodash/forEach';
import filter from 'lodash/filter';

Cypress.on('uncaught:exception', () => {
  return false;
});

const keys = [
  { key: 'routes_count', warning: false },
  { key: 'orders_count', warning: false },
  { key: 'dropped_locations_count', warning: false },
  { key: 'total_transit_distance_m', warning: false },
  { key: 'avg_transit_distance_m', warning: false },
  { key: 'total_duration_s', warning: false },
  { key: 'avg_duration_s', warning: false },
  { key: 'total_transit_duration_s', warning: false },
  { key: 'avg_transit_duration_s', warning: false },
  { key: 'total_waiting_duration_s', warning: false, nilCheck: true },
  { key: 'avg_waiting_duration_s', warning: false, nilCheck: true },
  { key: 'failed_time_window_depot_count', warning: true, nilCheck: true },
  { key: 'failed_time_window_depot_duration_s', warning: true, nilCheck: true },
  {
    key: 'avg_failed_time_window_depot_duration_s',
    warning: false,
    nilCheck: true,
  },
  { key: 'failed_time_window_locations_count', warning: true, nilCheck: true },
  {
    key: 'failed_time_window_locations_duration_s',
    warning: true,
    nilCheck: true,
  },
  {
    key: 'avg_failed_time_window_locations_duration_s',
    warning: true,
    nilCheck: true,
  },
  { key: 'failed_time_window_shifts_count', warning: true, nilCheck: true },
  { key: 'failed_time_window_shifts_duration_s', warning: true, nilCheck: true },
  {
    key: 'avg_failed_time_window_shifts_duration_s',
    warning: true,
  },
  { key: 'overtime_shifts_count', warning: true, nilCheck: true },
  { key: 'overtime_duration_s', warning: true, nilCheck: true },
  { key: 'avg_overtime_duration_s', warning: true, nilCheck: true },
];

// https://testpalm.yandex-team.ru/testcase/courier-749
context('Metrics Values', () => {
  before(() => {
    cy.openTaskById('b3a42bb0-23fe4341-f8c47510-1f219ea7');
  });

  it('Count total metrics header', () => {
    cy.get(selectors.totalMetrics.table).find('thead td').should('have.length', 11);
  });

  it('Count total metrics header', () => {
    cy.get(selectors.totalMetrics.table).find('thead td').should('not.be.hidden');
  });

  it('Count total metrics cells', () => {
    cy.get(selectors.totalMetrics.values).should('have.length', 46);
  });

  forEach(keys, ({ key, warning }) => {
    // derive test name from data

    it(`Check metrics ${key} tooltip`, () => {
      cy.get(selectors.totalMetrics.table)
        .find(`[data-test-anchor="${key}"] .total-metrics__value`)
        .trigger('mousemove', { which: 1 });
      cy.get(`[data-test-anchor="total-metrics_tooltip_${key}"]`).should(
        'have.class',
        'mvrp-map-custom-tooltip_visible',
      );
      cy.get(selectors.totalMetrics.table)
        .find(`[data-test-anchor="${key}"] .total-metrics__value`)
        .trigger('mouseout', { which: 1 });
    });

    if (warning) {
      it(`Check metrics ${key} warning`, () => {
        cy.get(selectors.totalMetrics.table)
          .find(`.total-metrics__cell_mod_warning [data-test-anchor="${key}"]`)
          .should('be.visible');
      });
    }
  });
});

context('Check metrics normal', () => {
  before(() => {
    cy.openTaskById('94d75d37-f13603ee-f21136cc-5126b5cd');
  });

  it(`Check metrics normal`, () => {
    cy.get(selectors.totalMetrics.table)
      .find(`.total-metrics__cell_mod_warning `)
      .should('have.length', 0);
  });

  forEach(filter(keys, 'nilCheck'), ({ key }) => {
    // derive test name from data
    it(`Check metrics ${key} nil`, () => {
      cy.get(selectors.totalMetrics.table)
        .find(`[data-test-anchor="${key}"] .total-metrics__value`)
        .invoke('text')
        .should('match', /^[0:]+$/);
    });
  });
});
