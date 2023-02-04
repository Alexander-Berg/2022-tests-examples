import selectors from 'constants/selectors';

Cypress.on('uncaught:exception', () => {
  return false;
});

// https://testpalm.yandex-team.ru/courier/testcases/753

context('Appearace of routing', () => {
  it('Timeline appearance', () => {
    cy.openTaskById('20dd8e8a-8b713cf1-baaa90a7-e697abc4');

    cy.get('*[data-test-anchor="-1.23:00"]')
      .should('be.visible')
      .next()
      .invoke('attr', 'data-test-anchor')
      .should('eq', `0:00`);

    cy.get(selectors.routesTimeline.table)
      .scrollTo('right')
      .get('*[data-test-anchor="2.3:00"]')
      .should('be.visible')
      .prev()
      .invoke('attr', 'data-test-anchor')
      .should('eq', `2.2:00`);
  });

  it('The selected order on the timeline is removed when a new route is selected', () => {
    cy.openTaskById('6972f98-d7b656d6-d8b5ef0b-9e12746f');
    cy.get('*[data-test-anchor="timeline-order_1"] .order')
      .click()
      .should('have.class', 'order_selected');
    cy.get(
      '*[data-test-anchor="Костенков"].route .route__route-name-text',
    ).click();
    cy.get('*[data-test-anchor="Костенков"].route').should(
      'have.class',
      'route_active',
    );
    cy.get('*[data-test-anchor="timeline-order_1"] .order').should(
      'not.have.class',
      'order_selected',
    );
  });

  it('Route line does not go beyond the boundaries of the order blocks', () => {
    cy.openTaskById('6972f98-d7b656d6-d8b5ef0b-9e12746f');

    cy.get('*[data-test-anchor="Курбанов"] .route-line').then($routeLine => {
      const routeLineOffset = $routeLine.position().left;
      const routeLineWidth = $routeLine[0].offsetWidth;

      cy.get('*[data-test-anchor="Курбанов"] .order')
        .first()
        .then($firstOrder => {
          const firstOrderOffet = $firstOrder.position().left;
          expect(routeLineOffset).to.eq(firstOrderOffet);
        });

      cy.get('*[data-test-anchor="Курбанов"] .order')
        .last()
        .then($lastOrder => {
          const lastOrderOffet = $lastOrder.position().left;
          const lastOrderWidth = $lastOrder[0].offsetWidth;
          expect(routeLineWidth + routeLineOffset).to.be.lessThan(lastOrderOffet + lastOrderWidth);
          expect(routeLineWidth + routeLineOffset).to.be.greaterThan(lastOrderOffet);
        });
    });
  });

  it('Check the appearance of the route line with waiting', () => {
    cy.openTaskById('f6a0532-5032d0a-482c632d-6bad332e');

    cy.get('*[data-test-anchor="Ковры Карш"]')
      .find('[data-test-anchor="timeline-order_15.09.21.056"]')
      .find('.timeline-order__waiting')
      .should('be.visible');
  });

  it('Check if there is no line between the block of points', () => {
    cy.openTaskById('f6a0532-5032d0a-482c632d-6bad332e');

    cy.get('[data-test-anchor="timeline-order_15.09.21.028"]')
      .find('.order')
      .then($orderBlock => {
        const orderBlockOffset = $orderBlock.position().left;
        const orderBlockWidth = $orderBlock[0].offsetWidth;
        cy.log(`${orderBlockWidth}`);
        cy.get('[data-test-anchor="timeline-order_break_9_9"]')
          .find('.order')
          .then($waitingBlock => {
            const waitingBlockOffset = $waitingBlock.position().left;
            expect(waitingBlockOffset).to.be.closeTo(orderBlockOffset + orderBlockWidth, 1);
          });
      });
  });

  it('Check the width of the blocks', () => {
    cy.openTaskById('f6a0532-5032d0a-482c632d-6bad332e');

    cy.get('.route__cell').then($cell => {
      const cellWidth = $cell[0].offsetWidth;
      const minutesInHour = 60;

      cy.get('*[data-test-anchor="timeline-order_R0_0_76"]')
        .find('.order')
        .should('be.visible')
        .then($emptyOrderBlock => {
          const emptyOrderBlock = $emptyOrderBlock[0].offsetWidth;
          expect(emptyOrderBlock).to.be.closeTo(0, 2);
        });

      cy.get('*[data-test-anchor="timeline-order_15.09.21.047"]')
        .find('.order')
        .then($secondOrderBlock => {
          const secondOrderBlock = $secondOrderBlock[0].offsetWidth;
          const totalServiceDuration = 45;
          expect(secondOrderBlock).to.be.closeTo(
            (totalServiceDuration / minutesInHour) * cellWidth,
            2,
          );
        });

      cy.get('*[data-test-anchor="timeline-order_15.09.21.052"]')
        .find('.order')
        .then($secondOrderBlock => {
          const secondOrderBlock = $secondOrderBlock[0].offsetWidth;
          const totalServiceDuration = 25;
          expect(secondOrderBlock).to.be.closeTo(
            (totalServiceDuration / minutesInHour) * cellWidth,
            2,
          );
        });
    });
  });

  it('Check appearance of the multiorder with same service time', () => {
    cy.openTaskById('d9e72db0-6ccb83db-f3307683-d3dd2038');

    cy.get('[data-test-anchor="timeline-order_Тестовый заказ 20"]')
      .find('.order')
      .should('have.text', '8');
    cy.get('[data-test-anchor="timeline-order_Тестовый заказ 20"]')
      .nextUntil('[data-test-anchor="timeline-order_Тестовый заказ 14"]')
      .then($multiorder => {
        expect($multiorder).to.have.length(4);
        const firstOrderBlock = $multiorder.first().find('.order');
        const firstOrderLeftPosititon = firstOrderBlock.position().left;

        const lastOrderBlock = $multiorder.last().find('.order');
        const lastOrderLeftPosition = lastOrderBlock.position().left;
        const lastOrderWidth = lastOrderBlock[0].offsetWidth;

        cy.wrap(lastOrderBlock).should('be.visible').should('have.text', '12');
        expect(firstOrderLeftPosititon).to.eq(lastOrderLeftPosition);
        expect(lastOrderWidth).to.be.closeTo(40, 2);
      });
  });

  it('Check appearance of the multiorder with different service time', () => {
    cy.openTaskById('954ca0ae-8bfce6be-9b03a040-3c59f028?route=0');

    cy.get('*[data-test-anchor="timeline-order_Тестовый заказ 20"]')
      .find('.order')
      .should('have.text', '12');
    cy.get('*[data-test-anchor="timeline-order_Тестовый заказ 20"]')
      .nextUntil('*[data-test-anchor="timeline-order_Тестовый заказ 6"]')
      .then($multiorder => {
        expect($multiorder).to.have.length(4);
        const firstOrderBlock = $multiorder.first().find('.order');
        const firstOrderLeftPosititon = firstOrderBlock.position().left;

        const lastOrderBlock = $multiorder.last().find('.order');
        const lastOrderLeftPosition = lastOrderBlock.position().left;
        const lastOrderWidth = lastOrderBlock[0].offsetWidth;

        cy.wrap(lastOrderBlock).should('be.visible').should('have.text', '16');
        expect(firstOrderLeftPosititon).to.eq(lastOrderLeftPosition);
        expect(lastOrderWidth).to.be.closeTo(115, 2);
      });
  });
});
