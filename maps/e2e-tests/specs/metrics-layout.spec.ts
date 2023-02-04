import selectors from 'constants/selectors';
import forEach from 'lodash/forEach';

Cypress.on('uncaught:exception', () => {
  return false;
});

// https://testpalm.yandex-team.ru/testcase/courier-748
context('Metrics Layout', () => {
  beforeEach(() => {
    cy.openTaskById(
      '94d75d37-f13603ee-f21136cc-5126b5cd?activeVehicle=2977&editedVehicleIds=#9d02c3e0-418ed9b0-f32bcfe-ff4a8d91',
    );
  });

  it('Check Route Selection', () => {
    cy.get(selectors.routesTimeline.table)
      .find('*[data-test-anchor="1014"]')
      .click({ force: true });

    cy.get(selectors.routesTimeline.tabSwitcher).click({ force: true });

    cy.get(selectors.metrics.metaPanelTrigger).should('be.visible');

    cy.get(selectors.metrics.table)
      .find('*[data-test-anchor="1014"]')
      .should('have.class', 'metrics__route_selected');
  });

  it('Check Scroll Up and Down', () => {
    cy.get(selectors.routesTimeline.tabSwitcher).click({ force: true });

    cy.get(selectors.metrics.table)
      .scrollTo('bottomRight')
      .get('*[data-test-anchor="966"] .metrics__route-value_col_overtime_duration_s')
      .should('be.visible')
      .get('*[data-test-anchor="966"] .metrics__route-value_col_vehicle_ref')
      .should('be.visible')
      .get(selectors.metrics.header)
      .should('be.visible');

    cy.get(selectors.metrics.table)
      .scrollTo('topLeft')
      .get('*[data-test-anchor="1014"] .metrics__route-value_col_vehicle_ref')
      .should('be.visible');
  });

  it('Check DND Up', () => {
    cy.get(selectors.app.resizer)
      .trigger('mousedown', { which: 1 })
      .trigger('mousemove', { clientX: 0, clientY: 90 })
      .trigger('mouseup', { force: true });

    cy.get(selectors.app.paneMap).invoke('outerHeight').should('be.gte', 100);
  });

  it('Check DND Down', () => {
    cy.get(selectors.app.resizer)
      .trigger('mousedown', { which: 1 })
      .trigger('mousemove', { clientX: 0, clientY: 1080 })
      .trigger('mouseup', { force: true });

    cy.get(selectors.app.paneTable).invoke('outerHeight').should('be.gte', 100);
  });
});

// https://testpalm.yandex-team.ru/courier/testcases/778
context('Metrics: adapting columns to the text', () => {
  const columnsKeys = [
    'vehicle_id',
    'orders_count',
    'total_transit_distance_m',
    'total_duration_s',
    'total_transit_duration_s',
    'total_waiting_duration_s',
    'total_failed_time_window_count',
    'total_failed_time_window_duration_s',
    'avg_failed_time_window_duration_s',
    'capacity',
    'total_weight',
    'route_start_time',
    'route_end_time',
    'return_to_depot',
    'utilization_weight_kg',
    'utilization_weight_perc',
    'utilization_units',
    'utilization_units_perc',
    'utilization_volume_m3',
    'utilization_volume_perc',
    'failed_time_window_depot_count',
    'failed_time_window_depot_duration_s',
    'failed_time_window_locations_count',
    'failed_time_window_locations_duration_s',
    'failed_time_window_shifts_count',
    'failed_time_window_shifts_duration_s',
    'overtime_shifts_count',
    'overtime_duration_s',
  ];
  const testcases = [
    {
      id: 'a4ee2cb4-5b2ca5ea-fffd7d8a-e8de31da?route=0',
      description: 'All columns should adapt to text when some data is not provided',
      dataTest: 'Тест',
    },
    {
      id: '862b7fa1-1f5f814c-7d8886cb-6725dbd9',
      description: 'All columns should adapt to text when some data takes up more space',
      dataTest: 'Тест',
    },
    {
      id: 'c0183a80-14130316-a622f06-d961cb98',
      description: 'All columns should adapt to text when some routes dont have name',
      dataTest: '0',
    },
  ];

  forEach(testcases, ({ id, description, dataTest }) => {
    describe(`${description}`, () => {
      before(() => {
        cy.openTaskById(`${id}`);
        cy.get(selectors.routesTimeline.tabSwitcher).click();
      });

      it('Control and name columns should have the same width', () => {
        cy.get(selectors.metrics.header)
          .find('.metrics__controls')
          .invoke('outerWidth')
          .as('headerWidth');
        cy.get(`[data-test-anchor='${dataTest}']`)
          .find('.metrics__route-value_col_vehicle_ref')
          .invoke('outerWidth')
          .as('dataWidth');
        cy.get('@headerWidth').then(headerWidth => {
          cy.get('@dataWidth').then(dataWidth => {
            expect(headerWidth).to.equal(dataWidth);
          });
        });
      });

      forEach(columnsKeys, key => {
        it(`${key} column should have same width and posiiton in all rows`, () => {
          cy.get(selectors.metrics.header)
            .find(`[data-test-anchor='${key}']`)
            .invoke('outerWidth')
            .as('headerWidth');
          cy.get(`[data-test-anchor='${dataTest}']`)
            .find(`.metrics__route-value_col_${key}`)
            .invoke('outerWidth')
            .as('dataWidth');
          cy.get('@headerWidth').then(headerWidth => {
            cy.get('@dataWidth').then(dataWidth => {
              expect(headerWidth).to.equal(dataWidth);
            });
          });

          cy.get(selectors.metrics.header)
            .find(`[data-test-anchor='${key}']`)
            .then($headerCell => {
              const headerLeftOffset = $headerCell[0].getBoundingClientRect().left;
              cy.get(`[data-test-anchor='${dataTest}']`)
                .find(`.metrics__route-value_col_${key}`)
                .then($dataCell => {
                  const cellLeftOffset = $dataCell[0].getBoundingClientRect().left;
                  expect(headerLeftOffset).to.eq(cellLeftOffset);
                });
            });
        });
      });
    });
  });

  describe('Case with very long ID', () => {
    before(() => {
      cy.openTaskById('dc8491e2-2cf16fca-47be9875-f66320aa?route=0');
      cy.get(selectors.routesTimeline.tabSwitcher).click();
    });

    it('Column vehicle_ref should hide overflowed text', () => {
      cy.get('.metrics__route-value_col_vehicle_ref')
        .should('have.css', 'overflow')
        .and('eq', 'hidden');

      cy.get('.metrics__route-value_col_vehicle_ref')
        .should('have.css', 'max-width')
        .and('eq', '550px');
    });

    it('Table should be scrollable when name is too long', () => {
      cy.get(selectors.metrics.table).scrollTo('25%', '0%');
      cy.get(selectors.metrics.header)
        .find('[data-test-anchor="orders_count"]')
        .should('be.visible');
    });

    it('Control and name columns should have the same width', () => {
      cy.get(selectors.metrics.header)
        .find('.metrics__controls')
        .invoke('outerWidth')
        .as('headerWidth');
      cy.get('.metrics__route_selected')
        .find('.metrics__route-value_col_vehicle_ref')
        .invoke('outerWidth')
        .as('dataWidth');
      cy.get('@headerWidth').then(headerWidth => {
        cy.get('@dataWidth').then(dataWidth => {
          expect(headerWidth).to.equal(dataWidth);
        });
      });
    });

    it('Control and name columns should have the same width', () => {
      cy.get(selectors.metrics.header)
        .find('.metrics__controls')
        .invoke('outerWidth')
        .as('headerWidth');
      cy.get('.metrics__route_selected')
        .find('.metrics__route-value_col_vehicle_ref')
        .invoke('outerWidth')
        .as('dataWidth');
      cy.get('@headerWidth').then(headerWidth => {
        cy.get('@dataWidth').then(dataWidth => {
          expect(headerWidth).to.equal(dataWidth);
        });
      });
    });

    forEach(columnsKeys, key => {
      it(`${key} column should have same width and posiiton in all rows`, () => {
        cy.get(selectors.metrics.header)
          .find(`[data-test-anchor='${key}']`)
          .invoke('outerWidth')
          .as('headerWidth');
        cy.get('.metrics__route_selected')
          .find(`.metrics__route-value_col_${key}`)
          .invoke('outerWidth')
          .as('dataWidth');
        cy.get('@headerWidth').then(headerWidth => {
          cy.get('@dataWidth').then(dataWidth => {
            expect(headerWidth).to.equal(dataWidth);
          });
        });

        cy.get(selectors.metrics.header)
          .find(`[data-test-anchor='${key}']`)
          .then($headerCell => {
            const headerLeftOffset = $headerCell[0].getBoundingClientRect().left;
            cy.get('.metrics__route_selected')
              .find(`.metrics__route-value_col_${key}`)
              .then($dataCell => {
                const cellLeftOffset = $dataCell[0].getBoundingClientRect().left;
                expect(headerLeftOffset).to.eq(cellLeftOffset);
              });
          });
      });
    });
  });
});
