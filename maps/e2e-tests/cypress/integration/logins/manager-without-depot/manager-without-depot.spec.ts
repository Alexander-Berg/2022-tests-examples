import selectors from '../../../../src/constants/selectors';

// @see https://testpalm.yandex-team.ru/testcase/courier-105
context('Manager without depots', function () {
  beforeEach(function () {
    cy.preserveCookies();
  });

  before(function () {
    cy.yandexLogin('managerEmpty');
    cy.get(selectors.modal.paranja).should('exist');
    cy.get(selectors.modal.paranja).should('not.exist');
  });

  it('Company settings is not exist', function () {
    cy.get(selectors.sidebar.menu.settingsGroup).click();
    cy.get(selectors.sidebar.menu.company).should('not.exist');
  });

  it('Cant create company', function () {
    cy.get(selectors.sidebar.companySelector.control).click();
    cy.get(selectors.sidebar.companySelector.orgs.dropdown).should('not.exist');
    cy.get(selectors.superUserRights.addCompanyButton).should('not.exist');
  });

  it('Can see list of companies', function () {
    cy.get(selectors.superUserRights.companyDropdown).should('not.exist');
  });

  it('Linked to the right company', function () {
    cy.fixture('company-data').then(({ common }) => {
      cy.get(selectors.sidebar.companySelector.orgs.currentNameDisabled)
        .invoke('text')
        .should('eq', common.companyName);
    });
  });

  it('Option "All depots" is not exist', function () {
    cy.get(selectors.sidebar.companySelector.depots.dropdownItems.allDepots).should('not.exist');
  });

  it('Cant see depots', function () {
    cy.get(selectors.sidebar.companySelector.control).click();
    cy.get(selectors.sidebar.companySelector.depots.dropdown).should('not.exist');
  });

  after(function () {
    cy.clearCookies();
  });
});
