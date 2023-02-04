import selectors from 'constants/selectors';
import times from 'lodash/times';

// https://testpalm.yandex-team.ru/courier/testcases/723
context('Routes and order pins', () => {
  before(() => {
    cy.openTaskById('59710cfd-1150b2fe-c5f12147-648dbf05');
  });

  it('Route should be highlighted on the map when timeline route is clicked', () => {
    cy.get('[data-test-anchor="Машина 21"]')
      .click({ force: true })
      .should('have.class', 'route_active');

    cy.get('[data-test-anchor="active:11"]').should('exist');

    cy.get('[data-test-anchor*="icon-marker-id_Заказ 87"]').should('be.visible');

    cy.get('[data-test-anchor*="icon-marker-id_Заказ 41"]').should('be.visible');

    cy.get('[data-test-anchor="timeline-order_Заказ 87"]').contains('1');

    cy.get('[data-test-anchor="Машина 21"]')
      .find(selectors.routesTimeline.order)
      .first()
      .invoke('text')
      .should('eq', '');

    cy.get('[data-test-anchor="Машина 21"]')
      .find(selectors.routesTimeline.order)
      .last()
      .invoke('text')
      .should('eq', '');
  });

  it('Depo pin should be highlighted after click on it', () => {
    const testID = 'R11_11_100';
    cy.get(`[data-test-anchor*="icon-marker-id_${testID}"]`)
      .invoke('attr', 'style')
      .as('style_default');
    cy.get(`[data-test-anchor*="icon-marker-id_${testID}"]`).click({ force: true });
    cy.get(`[data-test-anchor*="icon-marker-id_${testID}"]`)
      .invoke('attr', 'style')
      .as('style_active');
    cy.get('@style_default').then(style_default => {
      cy.get('@style_active').then(style_active => {
        expect(style_default).to.not.equal(style_active);
      });
    });

    cy.get(`[data-test-anchor="tooltip_icon-marker__tooltip_${testID}"]`).should('be.visible');

    cy.get(`[data-test-anchor="timeline-order_${testID}"]`)
      .find(selectors.routesTimeline.order)
      .should('have.class', 'order_selected');
  });

  it('Garage pin should be highlighted after click on it in timeline', () => {
    cy.get('[data-test-anchor="Машина 2"]').click({ force: true });
    const testID = 'Заказ 2';
    cy.get(`[data-test-anchor*="icon-marker-id_${testID}"]`)
      .invoke('attr', 'style')
      .as('style_default');
    cy.get(`[data-test-anchor="timeline-order_${testID}"]`)
      .find(selectors.routesTimeline.order)
      .click({ force: true });
    cy.get(`[data-test-anchor*="icon-marker-id_${testID}"]`)
      .invoke('attr', 'style')
      .as('style_active');
    cy.get('@style_default').then(style_default => {
      cy.get('@style_active').then(style_active => {
        expect(style_default).to.not.equal(style_active);
      });
    });

    cy.get(`[data-test-anchor="timeline-order_${testID}"]`)
      .find(selectors.routesTimeline.order)
      .should('have.class', 'order_selected');

    cy.get(selectors.tooltip.tooltipPanel).should('be.visible');
  });

  it('Garage pin should be highlighted after click on it on the map', () => {
    cy.get('[data-test-anchor*="icon-marker-id_Заказ 5"]').click({ force: true });

    cy.get('[data-test-anchor="timeline-order_Заказ 5"]')
      .find(selectors.routesTimeline.order)
      .should('have.class', 'order_selected');

    cy.get('[data-test-anchor="timeline-order_Заказ 2"]')
      .find('order_selected')
      .should('not.exist');
  });

  it('Dropped off orders should be highlighted on the map when timeline route is clicked', () => {
    cy.get('[data-test-anchor="Машина 11"]').click({ force: true });

    cy.get('[data-test-anchor="active:8"]').should('exist');

    cy.get('[data-test-anchor="Машина 11"]')
      .find(selectors.routesTimeline.order)
      .last()
      .invoke('text')
      .should('eq', '16▼');

    cy.get('[data-test-anchor="Машина 11"]')
      .find(selectors.routesTimeline.order)
      .eq(4)
      .invoke('text')
      .should('eq', '4▲');

    cy.get('[data-test-anchor*="icon-marker-id_Заказ 137"]')
      .should('be.visible')
      .click({ force: true });

    cy.get(selectors.tooltip.tooltipPanel).should('be.visible');

    cy.get('[data-test-anchor="timeline-order_Заказ 137"]')
      .find(selectors.routesTimeline.order)
      .should('have.class', 'order_selected');
  });

  it('Route with failed delivery window', () => {
    cy.get('[data-test-anchor="Машина 4"]')
      .click({ force: true })
      .find('.route-shift-failed-time-window')
      .should('be.visible')
      .should('have.css', 'background-color')
      .and('eq', 'rgba(255, 67, 61, 0.8)');

    cy.get('[data-test-anchor="timeline-order_Заказ 78"]')
      .find(selectors.routesTimeline.order)
      .should('have.css', 'background-color')
      .and('eq', 'rgb(255, 79, 79)');

    cy.get('[data-test-anchor="Машина 4"]')
      .find(selectors.routesTimeline.order)
      .last()
      .invoke('text')
      .should('not.eq', '');

    cy.get('[data-test-anchor="timeline-order_Заказ 78"]')
      .find(selectors.routesTimeline.order)
      .click()
      .should('have.class', 'order_selected')
      .should('have.css', 'background-color')
      .and('eq', 'rgb(255, 79, 79)');

    cy.get(selectors.tooltip.tooltipPanel).should('be.visible');
  });

  it('Query parameter should remain then route clicked ', () => {
    cy.openTaskById('59710cfd-1150b2fe-c5f12147-648dbf05?map=google');
    cy.get('[data-test-anchor="Машина 21"]')
      .click({ force: true })
      .should('have.class', 'route_active');
    cy.get(selectors.googleMap.map).should('be.visible');
  });
});

// https://testpalm.yandex-team.ru/courier/testcases/767
context('Map zoom', () => {
  before(() => {
    cy.openTaskById('be4c43-845f2088-89b36806-e589c263');
    times(15, () =>
      cy.get(selectors.app.map).find('.ymaps3x0-react--zoom-control-out').click({ force: true }),
    );
    cy.wait(1000);
  });

  it('Map should not zoom in when route in timeline clicked', () => {
    cy.get(selectors.app.map)
      .find(selectors.yamaps.layerContainer)
      .invoke('attr', 'data-z')
      .as('mapZoom');

    cy.get('[data-test-anchor="Андреев Андрей"]').click({ force: true });
    cy.wait(3000);
    cy.get(selectors.app.map)
      .find(selectors.yamaps.layerContainer)
      .invoke('attr', 'data-z')
      .as('mapZoom_2');

    cy.get('@mapZoom').then(mapZoom => {
      cy.get('@mapZoom_2').then(mapZoom_2 => {
        const defaultZoom = parseInt(mapZoom.toString());
        const updatedZoom = parseInt(mapZoom_2.toString());
        expect(defaultZoom).to.be.not.lt(updatedZoom);
      });
    });
  });

  it('Map should  zoom in when route in timeline double clicked', () => {
    cy.get(selectors.app.map)
      .find(selectors.yamaps.layerContainer)
      .invoke('attr', 'data-z')
      .as('mapZoom');

    cy.get('[data-test-anchor="Андреев Андрей"]').dblclick({ force: true });
    cy.wait(1000);
    cy.get(selectors.app.map)
      .find(selectors.yamaps.layerContainer)
      .invoke('attr', 'data-z')
      .as('mapZoom_2');

    cy.get('@mapZoom').then(mapZoom => {
      cy.get('@mapZoom_2').then(mapZoom_2 => {
        const defaultZoom = parseInt(mapZoom.toString());
        const updatedZoom = parseInt(mapZoom_2.toString());
        expect(defaultZoom).to.be.lt(updatedZoom);
      });
    });
  });

  it('Map should not change its appearance when clicking on order pins on map', () => {
    cy.get(selectors.app.map)
      .find(selectors.yamaps.layerContainer)
      .invoke('attr', 'style')
      .as('mapStyle');

    cy.get('[data-test-anchor*="icon-marker-id_1355335"]').click({ force: true });

    cy.get(selectors.app.map)
      .find(selectors.yamaps.layerContainer)
      .invoke('attr', 'style')
      .as('mapStyle_2');

    cy.get('@mapStyle').then(mapStyle => {
      cy.get('@mapStyle_2').then(mapStyle_2 => {
        expect(mapStyle).eq(mapStyle_2);
      });
    });
  });

  it('Map should hide previous and show whole new route when this route is clicked in timeline', () => {
    cy.get('[data-test-anchor="Аксенов Алексей"]').click({ force: true });
    cy.get('[data-test-anchor*="icon-marker-id_1355335"]').should('not.exist');
    cy.get('[data-test-anchor*="icon-marker-id_1355751"]').should('be.visible');
    cy.get('[data-test-anchor*="icon-marker-id_1355705"]').should('be.visible');
  });

  it('Map should not change its appearance when clicking on the order blocks on timeline', () => {
    cy.get(selectors.app.map)
      .find(selectors.yamaps.layerContainer)
      .invoke('attr', 'style')
      .as('mapStyle');

    cy.get('[data-test-anchor*="timeline-order_1356443"]')
      .find(selectors.routesTimeline.order)
      .click({ force: true });

    cy.get(selectors.app.map)
      .find(selectors.yamaps.layerContainer)
      .invoke('attr', 'style')
      .as('mapStyle_2');

    cy.get('@mapStyle').then(mapStyle => {
      cy.get('@mapStyle_2').then(mapStyle_2 => {
        expect(mapStyle).eq(mapStyle_2);
      });
    });
  });
});
