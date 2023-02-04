import selectors from 'constants/selectors';

context('SVRP Task', () => {
    it('Metrics open at svrp task', () => {
        cy.openTaskById('c0d45779-b008a222-451269bc-4c7ea88');
        cy.get(selectors.routesTimeline.tabSwitcher).click();
        cy.get('*[data-test-anchor="16217_route_0"]');
    });
});
