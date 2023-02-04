import selectors from '../../../../src/constants/selectors';

describe('Admin MVRP', function () {
  // @see https://testpalm.yandex-team.ru/testcase/courier-557
  before(() => {
    cy.preserveCookies();
    cy.clearLocalforage();
    cy.yandexLogin('mvrpManager');
    cy.openAndCloseVideo({ onlyOpen: true });
  });

  after(() => {
    cy.clearCookies();
  });

  it('Planning page opened with help video', () => {
    cy.get(selectors.modal.helpVideo.popup).should('be.visible');
    cy.get(selectors.modal.helpVideo.close).click().wait(300);
    cy.get(selectors.modal.helpVideo.popup).should('not.exist');

    cy.get(selectors.content.mvrp.start).should('be.visible');
  });

  it('New tab without help video', () => {
    cy.forceVisit(Cypress.env('BASE_URL'));

    cy.get(selectors.modal.helpVideo.popup).should('not.exist');
    cy.get(selectors.content.mvrp.start).should('be.visible');
  });

  it('Open dashboard', () => {
    cy.get(selectors.sidebar.menu.monitoringGroup).click();
    cy.get(selectors.sidebar.menu.dashboard).click();

    cy.get(selectors.content.dashboard.view).should('be.visible');
  });

  it('Logout', () => {
    cy.get(selectors.sidebar.user.control).click();
    cy.get<HTMLAnchorElement>(selectors.sidebar.user.logout).then($el => {
      const URLObj = new URL($el[0].href);
      cy.wrap(URLObj.hostname).should('eq', 'passport.yandex.ru');
      cy.wrap(URLObj.searchParams.get('action')).should('eq', 'logout');
      cy.location('pathname').should(pathname => {
        if (URLObj.searchParams.get('retpath')?.includes(pathname)) {
          return true;
        }
        throw new Error('retpath incorrect');
      });
    });

    // don't check actual logout here
  });
});
