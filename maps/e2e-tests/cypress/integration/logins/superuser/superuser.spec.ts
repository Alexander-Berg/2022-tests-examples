import * as companySettingsKeyset from '../../../../../src/translations/company-settings';
import selectors from '../../../../src/constants/selectors';
import urls from '../../../../src/utils/urls';

context('Superuser', function () {
  beforeEach(function () {
    cy.preserveCookies();
  });

  before(function () {
    cy.fixture('company-data').then(({ A }) => {
      const link = urls.dashboard.createLink(A.companyId, {});
      cy.yandexLogin('superuser', { link });
      cy.get(selectors.modal.paranja).should('exist');
      cy.get(selectors.modal.paranja).should('not.exist');
      cy.waitForElement(selectors.sidebar.companySelector.toggleControl);
    });
  });

  it('Service settings', function () {
    cy.get(selectors.sidebar.menu.settingsGroup).click();
    cy.get(selectors.sidebar.menu.company).click();
    cy.get(selectors.settings.labels.availableServices)
      .invoke('text')
      .should('eq', companySettingsKeyset.ru.availableServices);
  });

  it('Button create company is exist', function () {
    cy.get(selectors.sidebar.companySelector.control).click();
    cy.get(selectors.sidebar.companySelector.orgs.dropdown).should('exist');
    cy.get(selectors.superUserRights.addCompanyButton).should('exist');
  });

  it('Companies dropdown is exist', function () {
    cy.get(selectors.superUserRights.companyDropdown).should('exist');
  });

  after(function () {
    cy.clearCookies();
  });
});
