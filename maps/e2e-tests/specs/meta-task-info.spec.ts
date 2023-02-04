import selectors from 'constants/selectors';

//https://testpalm.yandex-team.ru/courier/testcases/750
context('Meta Task Info', () => {
  beforeEach(() => {
    cy.openTaskById('f6a0532-5032d0a-482c632d-6bad332e');
  });

  it('Panel Opens (from Timeline)', () => {
    cy.get(selectors.metaTaskInfo.button).click();

    cy.get(selectors.metaTaskInfo.metrics).find('.table__header').should('exist');
    cy.get(selectors.metaTaskInfo.options).find('.table__header').should('exist');
  });

  it('Panel Opens (from Metrics)', () => {
    cy.get(selectors.routesTimeline.tabSwitcher).click();
    cy.get(selectors.metaTaskInfo.button).click();

    cy.get(selectors.metaTaskInfo.metrics).find('.table__header').should('exist');
    cy.get(selectors.metaTaskInfo.options).find('.table__header').should('exist');
  });

  it('Scrolls Up and Down', () => {
    cy.get(selectors.metaTaskInfo.button).click();

    cy.get(selectors.metaTaskInfo.panelInner)
      .scrollTo('bottom')
      .contains('weighted_drop_penalty')
      .should('be.visible');

    cy.get(selectors.metaTaskInfo.panelInner)
      .scrollTo('top')
      .contains('gned_locations_count')
      .should('be.visible');
  });

  it('Closes when Click on Route', () => {
    cy.get(selectors.metaTaskInfo.button).click();
    cy.get('[data-test-anchor="Киев 393"]> div:nth-child(5)').click({ force: true });

    cy.get(selectors.metaTaskInfo.panel).should('not.exist');
  });

  it('Closes when Click on Cross', () => {
    cy.get(selectors.metaTaskInfo.button).click();
    cy.get(selectors.metaTaskInfo.closer).click();

    cy.get(selectors.metaTaskInfo.panel).should('not.exist');
  });

  it('Closes when Click on Order in Timeline', () => {
    cy.get(selectors.metaTaskInfo.button).click();

    cy.get('[data-test-anchor="timeline-order_15.09.21.015"]')
      .find(selectors.routesTimeline.order)
      .click();

    cy.get(selectors.metaTaskInfo.panel).should('not.exist');
  });

  it('Closes when Click on Pin on the Map', () => {
    cy.get(selectors.metaTaskInfo.button).click();

    cy.get('[data-test-anchor="Киев 393"]').click();
    cy.get('[data-test-anchor="icon-marker-id_15.09.21.027 icon-marker-type_location"]').click({
      force: true,
    });
    cy.get(selectors.metaTaskInfo.panel).should('not.exist');
  });
});
