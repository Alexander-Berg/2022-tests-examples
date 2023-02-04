import selectors from 'constants/selectors';

import times from 'lodash/times';

context('Map interaction', () => {
  it('When you click on the route edge, the route is highlighted', () => {
    cy.openTaskById('59710cfd-1150b2fe-c5f12147-648dbf05');

    times(3, () => {
      cy.get(selectors.app.map).find('.ymaps3x0-react--zoom-control-in').click({ force: true });
      cy.wait(200);
    });
    cy.wait(500);

    // In case this test breaks down again
    // Here it is very tricky to consider x and y values
    // You need to open the solution and add a listener (document.addEventListener('click', (event) => console.log(event.x, event.y)))
    // to the document in the console, but you have to do it in an open cypress test (npr run cypress-open),
    // because the screen resolution on your computer may not be the same as on the stand where the tests are run

    cy.get(selectors.app.map).click(737, 518);
    cy.get('*[data-test-anchor="Машина 21"].route').should('have.class', 'route_active');
  });

  it('When you click on the order point, the route is highlighted', () => {
    cy.openTaskById('59710cfd-1150b2fe-c5f12147-648dbf05');

    times(3, () => {
      cy.get(selectors.app.map).find('.ymaps3x0-react--zoom-control-in').click({ force: true });
      cy.wait(200);
    });
    cy.wait(500);

    cy.get(selectors.app.map).click(737, 518); // Click on route
    cy.wait(200);
    cy.get(selectors.app.map).click(741, 501); // Click on order
    cy.get('*[data-test-anchor="Машина 21"].route').should('have.class', 'route_active');
    cy.get('*[data-test-anchor="timeline-order_Заказ 15"] .order').should(
      'have.class',
      'order_selected',
    );
  });
});
