import selectors from 'constants/selectors';

Cypress.on('uncaught:exception', () => {
  return false;
});

// https://testpalm.yandex-team.ru/courier/testcases/747
context('Timeline: basic cases', () => {
  context('timeline with scroll', () => {
    beforeEach(() => {
      cy.openTaskById(
        '94d75d37-f13603ee-f21136cc-5126b5cd?activeVehicle=2977&editedVehicleIds=#9d02c3e0-418ed9b0-f32bcfe-ff4a8d91',
      );
    });

    it('timeline table should be visible when open solution', () => {
      cy.get(selectors.routesTimeline.table)
        .should('be.visible')
        .get(selectors.routesTimeline.tabSwitcher)
        .should('be.visible');
    });

    it('Check scroll timeline bottom/right and top/left', () => {
      cy.get(selectors.routesTimeline.table)
        .scrollTo('bottomRight')
        .get(selectors.routesTimeline.tabSwitcher)
        .should('be.visible')
        .get('*[data-test-anchor="966"] .route__route-name')
        .should('be.visible')
        .get('*[data-test-anchor="21:00"]')
        .should('be.visible');

      cy.get(selectors.routesTimeline.table)
        .scrollTo('topLeft')
        .get(selectors.routesTimeline.tabSwitcher)
        .should('be.visible')
        .get('*[data-test-anchor="1014"] .route__route-name')
        .should('be.visible')
        .get('*[data-test-anchor="1:00"]')
        .should('be.visible');
    });

    it('Visible brackets indicating the shift time for the machine when hover route', () => {
      // The real Hover method does some scrolling so that the method works correctly, the page is scrolled to the very bottom
      cy.get(selectors.routesTimeline.table).scrollTo('bottom');
      cy.get('*[data-test-anchor="966"] .route-shift-time-window')
        .should('have.css', 'display', 'none')
        .get('*[data-test-anchor="966"]')
        .should('have.css', 'background-color', 'rgba(0, 0, 0, 0)')
        .get('*[data-test-anchor="966"]')
        .realHover()
        .get('*[data-test-anchor="966"]')
        .should('have.css', 'background-color', 'rgba(0, 0, 0, 0.067)')
        .get('*[data-test-anchor="966"] .route-shift-time-window')
        .should('be.visible');
    });

    it('Check resizer down', () => {
      cy.get(selectors.app.resizer)
        .trigger('mousedown', { which: 1 })
        .trigger('mousemove', { clientX: 0, clientY: 1080 })
        .trigger('mouseup', { force: true });

      cy.get(selectors.app.paneTable).invoke('outerHeight').should('be.gte', 100);

      cy.get('.timeline-header')
        .should('be.visible')
        .get(selectors.routesTimeline.tabSwitcher)
        .should('be.visible');

      cy.get(selectors.totalMetrics.values).should('be.visible');
    });

    it('Check resizer up', () => {
      cy.get(selectors.app.resizer)
        .trigger('mousedown', { which: 1 })
        .trigger('mousemove', { clientX: 0, clientY: 90 })
        .trigger('mouseup', { force: true });

      cy.get(selectors.app.paneMap).invoke('outerHeight').should('be.gte', 100);

      cy.get('.timeline-header')
        .should('be.visible')
        .get(selectors.routesTimeline.tabSwitcher)
        .should('be.visible');

      cy.get(selectors.totalMetrics.values).should('be.visible');
    });
  });

  context('timeline without scroll', () => {
    beforeEach(() => {
      cy.openTaskById('d637652c-89c04e4-1431c760-954800ef');
    });

    it('Check scrolling with a non-scrollable timeline', () => {
      cy.get(selectors.routesTimeline.table)
        .scrollTo('bottomRight', { ensureScrollable: false })
        .get('*[data-test-anchor="ТестМВРП"] .route__route-name')
        .should('be.visible')
        .get('*[data-test-anchor="8:00"]')
        .should('be.visible')
        .get('*[data-test-anchor="11:00"]')
        .should('be.visible');
    });
  });
});

context('timeline scroll on load', () => {
  it('timeline will scroll to the route from the address bar after the page loads', () => {
    cy.openTaskById('94d75d37-f13603ee-f21136cc-5126b5cd?route=117');
    cy.wait(1000);
    cy.get('*[data-test-anchor="1327"].route .route__route-name').should(
      'be.visible',
    );
  });
});

context('Routes lasting several days', () => {
  it('Timeline includes several days', () => {
    cy.openTaskById('b3a42bb0-23fe4341-f8c47510-1f219ea7');
    cy.get('[data-test-anchor="9.5:00"]').should('exist');
  });
});
