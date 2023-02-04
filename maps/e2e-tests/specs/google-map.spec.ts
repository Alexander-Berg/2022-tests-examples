import selectors from 'constants/selectors';
import times from 'lodash/times';

// https://testpalm.yandex-team.ru/testcase/courier-722
context('Google map', () => {
  before(() => {
    cy.openTaskById('b038bea0-35f756c9-8ffc474f-13d56cf2', 'com');
  });

  it('Solution open', () => {
    cy.get(selectors.routesTimeline.table).should('be.visible');
  });

  it('Zoom visible', () => {
    cy.get(selectors.googleMap.zoomIn).should('be.visible');
    cy.get(selectors.googleMap.zoomOut).should('be.visible');
  });

  it('Search visible', () => {
    cy.get(selectors.googleMap.searchControl).should('be.visible');
  });

  it('Total metrics visible', () => {
    cy.get(selectors.totalMetrics.table).should('be.visible');
  });

  it('Support button visible', () => {
    cy.get(selectors.googleMap.supportButton).should('be.visible');
  });

  it('Load json visible', () => {
    cy.get(selectors.googleMap.loadJSONButton).should('be.visible');
  });

  it('Traffic button visible', () => {
    cy.get(selectors.googleMap.trafficButton).should('be.visible');
  });
});

context('Search control', () => {
  it('Search input should be active', () => {
    cy.get(selectors.app.map)
      .find('.google-search-control')
      .should('be.visible')
      .type('1050 Borregas Avenue, Sunnyvale, CA, US')
      .get('.pac-item-query')
      .first()
      .click();
  });

  it('Search input should be empty after click on cancel button', () => {
    cy.get('.google-search-control').find('.google-search-control__close-icon').click();
    cy.get('.google-search-control').find('.google-search-control__input').should('have.value', '');
  });
});

context('Traffic mode', () => {
  it('Turn on traffic mode', () => {
    cy.get(selectors.googleMap.trafficButton).should('be.visible').click({ force: true });
    cy.get(selectors.googleMap.trafficButton)
      .should('have.css', 'background-color')
      .and('eq', 'rgb(255, 235, 160)');
  });

  it('Turn off traffic mode', () => {
    cy.get(selectors.googleMap.trafficButton).should('be.visible').click({ force: true });
    cy.get(selectors.googleMap.trafficButton)
      .should('have.css', 'background-color')
      .and('eq', 'rgb(255, 255, 255)');
  });
});

context('Zoom', () => {
  it('Map should zoom out when click on zoom out button', () => {
    times(20, () => {
      cy.get(selectors.googleMap.zoomOut).click({ force: true });
    });
    cy.get(selectors.googleMap.zoomOut).should('be.disabled');
    cy.get(selectors.googleMap.zoomIn).should('not.be.disabled');
  });

  it('Map should zoom in when click on zoom in button', () => {
    times(20, () => {
      cy.get(selectors.googleMap.zoomIn).click({ force: true });
    });
    cy.get(selectors.googleMap.zoomIn).should('be.disabled');
    cy.get(selectors.googleMap.zoomOut).should('not.be.disabled');
  });
});

context('Check DND', () => {
  it('Check DND Up', () => {
    cy.get(selectors.app.resizer)
      .trigger('mousedown', { which: 1 })
      .trigger('mousemove', { clientX: 0, clientY: 90 })
      .trigger('mouseup', { force: true });

    cy.get(selectors.app.paneMap).invoke('outerHeight').should('be.gte', 100);
    cy.get(selectors.googleMap.supportButton).should('be.visible');
    cy.get(selectors.app.map).find('.gm-control-active').should('be.visible');
  });

  it('Check DND Down', () => {
    cy.get(selectors.app.resizer)
      .trigger('mousedown', { which: 1 })
      .trigger('mousemove', { clientX: 0, clientY: 1080 })
      .trigger('mouseup', { force: true });

    cy.get(selectors.app.paneTable).invoke('outerHeight').should('be.gte', 100);
    cy.get(selectors.googleMap.supportButton).should('be.visible');
    cy.get(selectors.app.map).find('.gm-control-active').should('be.visible');
  });
});
