import selectors from 'constants/selectors';

const JSON_FILE_NAME = 'solution.json';
const JSON_FILE_NAME_2 = 'solution2.json';

// https://testpalm.yandex-team.ru/courier/testcases/685
context('load json with solution', () => {
  before(() => {
    cy.openEmptyTask();
  });

  it('Timeline should be empty, map is visible', () => {
    cy.get(selectors.routesTimeline.table).should('not.exist');
    cy.get(selectors.app.map).should('be.visible');
  });

  it('Load JSON button should be visible and active when map is opened', () => {
    cy.get(selectors.loadJSONButton).should('be.visible').and('not.be.disabled');
  });

  it('Solution should be opened when downloading the JSON file', () => {
    cy.get(selectors.loadJSONButton).should('be.visible').click();

    cy.get('[data-test-anchor="load-json-input"]').attachFile(JSON_FILE_NAME);
    cy.get(selectors.app.map).should('be.visible');
    cy.get(selectors.totalMetrics.table).should('be.visible');
    cy.get(selectors.routesTimeline.table).should('be.visible').find('.route').should('be.visible');
  });

  it('Solution should be opened when reloading the solution json file', () => {
    cy.get(selectors.loadJSONButton).should('be.visible').click();

    cy.get('[data-test-anchor="load-json-input"]').attachFile(JSON_FILE_NAME);
    cy.get(selectors.app.map).should('be.visible');
    cy.get(selectors.routesTimeline.table).should('be.visible').find('.route').should('be.visible');
    cy.get(selectors.totalMetrics.table).should('be.visible');
  });

  it('Route should be highlighted on the map after click on timeline', () => {
    cy.get('[data-test-anchor="m001_3"]').first().click();
    cy.get('[data-test-anchor*="icon-marker-id_loc 1190"]').should('be.visible');
    cy.get('[data-test-anchor="active:0"]').should('exist');
  });

  it('Previous routes should disappear and new solution should be opened when downloading the new solution`s json file', () => {
    cy.get(selectors.loadJSONButton).should('be.visible').click();

    cy.get('[data-test-anchor="load-json-input"]').attachFile(JSON_FILE_NAME_2);
    cy.get('[data-test-anchor*="icon-marker-id_loc 1190"]').should('not.exist');
    cy.get(selectors.app.map).should('be.visible');
    cy.get(selectors.routesTimeline.table).should('be.visible').find('.route').should('be.visible');
    cy.get(selectors.totalMetrics.table).should('be.visible');
  });

  it('Route should be highlighted on the map after click on timeline', () => {
    cy.get('[data-test-anchor="m001_3"]').first().click();
    cy.get('[data-test-anchor*="icon-marker-id_loc 7"]').should('be.visible');
    cy.get('[data-test-anchor="active:0"]').should('exist');
  });

  it('Metrics should be visible after metrics button click', () => {
    cy.get(selectors.routesTimeline.tabSwitcher).click({ force: true });
    cy.get(selectors.metrics.table).should('be.visible');
    cy.get('.metrics__route_selected').invoke('attr', 'data-test-anchor').should('eq', 'm001_3');
  });

  it('Solution open on ru with google map after json upload', () => {
    cy.openTaskById('b038bea0-35f756c9-8ffc474f-13d56cf2?map=google');
    cy.get(selectors.googleMap.loadJSONButton).should('be.visible').click();
    cy.get('[data-test-anchor="load-json-input"]').attachFile(JSON_FILE_NAME);
    cy.get(selectors.googleMap.map).should('be.visible');
  });
});
