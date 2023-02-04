import selectors from '../../../src/constants/selectors';

// @see https://testpalm.yandex-team.ru/testcase/courier-27
context('Links', function () {
  it('Click on logo', function () {
    cy.yandexLogin('manager');
    cy.get(selectors.content.dashboard.view);
    const href = cy.get(selectors.sidebar.serviceLogo).invoke('attr', 'href');
    href.should('exist');
  });
});
