import urls from '../../../src/utils/urls';
import selectors from '../../../src/constants/selectors';
import moment from 'moment';

// @see https://testpalm.yandex-team.ru/testcase/courier-277
describe('Managers - Search', () => {
  before(() => {
    cy.fixture('company-data').then(({ common }) => {
      const date = moment(new Date()).add(-2, 'days').format(urls.dashboard.dateFormat);
      const link = urls.dashboard.createLink(common.companyId, { date });
      cy.yandexLogin('admin', { link });
    });

    cy.get(selectors.sidebar.menu.settingsGroup).click();
    cy.get(selectors.sidebar.settingsGroup.managers).click();
  });

  describe('Element on page', () => {
    it('should show basic elements on managers page', () => {
      cy.get(selectors.managers.search).should('exist');
      cy.get(selectors.managers.emailInput).should('exist');
      cy.get(selectors.managers.searchItem).should('have.length', 4);
    });
  });

  describe('Search by manager', function () {
    it('should show emails which contain "y" in name', () => {
      cy.get(selectors.managers.search).type('y');
      cy.get(selectors.managers.searchItem).should('have.length', 4);
    });

    it('should show empty search result', () => {
      cy.get(selectors.managers.search).type('abc');
      cy.get(selectors.managers.searchItem).should('have.length', 0);
    });

    it('should show only one email when type "mng" in search', () => {
      cy.get(selectors.managers.search).type('mng');
      cy.get(selectors.managers.searchItem).should('have.length', 1);
    });

    it('should show 4 emails when type branchNumber in search', () => {
      const BRANCH_NAME = Cypress.env('BRANCH_NAME') as string;
      const taskNumber = BRANCH_NAME.match(/\d+/)?.[0] ?? '';

      cy.get(selectors.managers.search).type(taskNumber);
      cy.get(selectors.managers.searchItem).should('have.length', 4);
    });

    it('should show only one email when type full email in search', () => {
      cy.fixture('testData').then(({ accounts }) => {
        const managerEmptyEmail = `${accounts.managerEmpty}@yandex.ru`;
        cy.get(selectors.managers.search).type(managerEmptyEmail);
        cy.get(selectors.managers.searchItem).should('have.length', 1);
        cy.get(selectors.managers.email).invoke('text').should('eq', managerEmptyEmail);
      });
    });

    afterEach(() => {
      cy.get(selectors.managers.clearIcon).click();
    });
  });
});
