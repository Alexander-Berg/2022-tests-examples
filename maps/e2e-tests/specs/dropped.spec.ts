import selectors from 'constants/selectors';

Cypress.on('uncaught:exception', () => {
  return false;
});

// https://testpalm.yandex-team.ru/testcase/courier-724
context('Dropped', () => {
  beforeEach(() => {
    cy.openTaskById('f3b29a3c-92d1811d-a837198e-56bb94a8');
  });

  it('Dropped row should be visible', () => {
    cy.openTaskById('f3b29a3c-92d1811d-a837198e-56bb94a8');
    cy.get(selectors.routesTimeline.table).scrollTo('bottom');
    cy.get(selectors.routesTimeline.table)
      .find('[data-test-anchor="dropped"]')
      .should('be.visible');
  });

  it('Dropped row should be visible', () => {
    cy.get(selectors.routesTimeline.table).scrollTo('bottom');
    cy.get(selectors.routesTimeline.table)
      .find('[data-test-anchor="dropped"]')
      .should('be.visible');
  });

  it('Dropped line should not be visible', () => {
    cy.get(selectors.routesTimeline.table).scrollTo('bottom');
    cy.get(selectors.routesTimeline.table)
      .get('[data-test-anchor="dropped"]')
      .find('.route-timeline')
      .should('not.exist');
  });

  it('Dropped pins should be visible after click', () => {
    cy.get(selectors.routesTimeline.table).scrollTo('bottom');
    cy.get(selectors.routesTimeline.table).get('[data-test-anchor="dropped"]').click();
    cy.get('[data-test-anchor*="icon-marker-type_dropped"]').should('be.visible');
  });

  it('Dropped pins should be visible after click twicw', () => {
    cy.get(selectors.routesTimeline.table).scrollTo('bottom');
    cy.get(selectors.routesTimeline.table).get('[data-test-anchor="dropped"]').click();
    cy.get(selectors.routesTimeline.table).get('[data-test-anchor="dropped"]').click();
    cy.get('[data-test-anchor*="icon-marker-type_dropped"]').should('be.visible');
  });

  it('Tooltip should be visible after dropped pin hover', () => {
    cy.get(selectors.routesTimeline.table).scrollTo('bottom');
    cy.get(selectors.routesTimeline.table).get('[data-test-anchor="dropped"]').click();
    cy.get('[data-test-anchor="icon-marker-id_96242790 icon-marker-type_dropped"]').trigger(
      'mousemove',
      { force: true },
    );
    cy.get('[data-test-anchor="tooltip_icon-marker__tooltip_96242790"]').should('exist');
  });

  it('Tooltip should be fixed after click', () => {
    cy.get(selectors.routesTimeline.table).scrollTo('bottom');
    cy.get(selectors.routesTimeline.table).get('[data-test-anchor="dropped"]').click();
    cy.get('[data-test-anchor="icon-marker-id_96242790 icon-marker-type_dropped"]').click();
    cy.get('.mvrp-map-custom-tooltip_visible').invoke('attr', 'style').as('style_1');
    cy.get('[data-test-anchor="icon-marker-id_96220354 icon-marker-type_dropped"]').trigger(
      'mousemove',
      { force: true },
    );
    cy.get('.mvrp-map-custom-tooltip_visible').invoke('attr', 'style').as('style_2');

    cy.get('@style_1').then(style_1 => {
      cy.get('@style_2').then(style_2 => {
        expect(style_1).to.equal(style_2);
      });
    });
  });

  it('Dropped pin should not be visible if route selected', () => {
    cy.get(selectors.routesTimeline.table).scrollTo('bottom');
    cy.get(selectors.routesTimeline.table).get('[data-test-anchor="dropped"]').click();
    cy.get('[data-test-anchor*="icon-marker-type_dropped"]').should('be.visible');
    cy.get(selectors.routesTimeline.table)
      .get('[data-test-anchor="Прудников Артур-22502"]')
      .click();
    cy.get(selectors.app.map)
      .find('[data-test-anchor*="icon-marker-type_dropped"]')
      .should('not.exist');
  });

  it('Dropped should not be visible on metrics tab', () => {
    cy.get(selectors.routesTimeline.table).scrollTo('bottom');
    cy.get(selectors.routesTimeline.table).get('[data-test-anchor="dropped"]').click();
    cy.get(selectors.routesTimeline.tabSwitcher).click({ force: true });
    cy.get(selectors.metrics.table).scrollTo('bottom');
    cy.get(selectors.metrics.table).find('[data-test-anchor="dropped"]').should('not.exist');
  });

  it('Dropped pins should not be visible on metrics tab route active', () => {
    cy.get(selectors.routesTimeline.table).scrollTo('bottom');
    cy.get(selectors.routesTimeline.table).get('[data-test-anchor="dropped"]').click();
    cy.get(selectors.routesTimeline.tabSwitcher).click({ force: true });
    cy.get('[data-test-anchor="Вершинин Николай-21084"]').click();
    cy.get(selectors.app.map)
      .find('[data-test-anchor*="icon-marker-type_dropped"]')
      .should('not.exist');
  });

  it('Dropped row should not exists if there are no dropped orders', () => {
    cy.openTaskById(
      '94d75d37-f13603ee-f21136cc-5126b5cd?activeVehicle=2977&editedVehicleIds=#9d02c3e0-418ed9b0-f32bcfe-ff4a8d91',
    );
    cy.get(selectors.routesTimeline.table).scrollTo('bottom');
    cy.wait(1000);
    cy.get(selectors.routesTimeline.table).find('[data-test-anchor="dropped"]').should('not.exist');
  });
});
