import selectors from 'constants/selectors';

Cypress.on('uncaught:exception', () => {
  return false;
});

// https://testpalm.yandex-team.ru/testcase/courier-689
context('Map', () => {
  before(() => {
    cy.openTaskById('c7eba041-6378e45-386a6dab-13f94b21?route=43');
  });

  it('Solution open', () => {
    cy.get(selectors.routesTimeline.table).should('be.visible');
  });

  it('Route Active', () => {
    cy.get(selectors.routesTimeline.table)
      .find('*[data-test-anchor="Асеев Василий-612087366"]')
      .should('have.class', 'route_active');
  });

  it('Zoom visible', () => {
    cy.get(selectors.app.map).find('.ymaps3x0-react--zoom-control-in').should('be.visible');

    cy.get(selectors.app.map).find('.ymaps3x0-react--zoom-control-out').should('be.visible');
  });

  it('Search visible', () => {
    cy.get(selectors.app.map).find('.ymaps3-search-control').should('be.visible');
  });

  it('Total metrics visible', () => {
    cy.get(selectors.totalMetrics.table).should('be.visible');
  });
});

context('Map Hover', () => {
  before(() => {
    cy.openTaskById(
      '94d75d37-f13603ee-f21136cc-5126b5cd?activeVehicle=2977&editedVehicleIds=#9d02c3e0-418ed9b0-f32bcfe-ff4a8d91',
    );
  });

  it('Map visible', () => {
    cy.get(selectors.app.map).should('be.visible');
  });

  it.skip('Mab Line Hover', () => {
    cy.get(selectors.app.map).should('be.visible');
    cy.wait(3000);
    cy.get('[data-test-anchor="icon-marker_М-001585745"]').trigger('mousemove');
    cy.get(selectors.app.map)
      .find('[data-test-anchor="active-point-tooltip"]')
      .should('be.visible');
  });
});

context('Query parameter', () => {
  it('Solution open on com with yandex map', () => {
    cy.openTaskById('b038bea0-35f756c9-8ffc474f-13d56cf2?map=yandex', 'com');
    cy.get(selectors.app.map).should('be.visible');
  });
  it('Solution open on ru with google map', () => {
    cy.openTaskById('b038bea0-35f756c9-8ffc474f-13d56cf2?map=google');
    cy.get(selectors.googleMap.map).should('be.visible');
  });
});
