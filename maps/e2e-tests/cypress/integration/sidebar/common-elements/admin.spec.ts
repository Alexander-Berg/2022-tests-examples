import selectors from '../../../../src/constants/selectors';
import urls from '../../../../src/utils/urls';
import { depotAddressRecord, depotNameRecord } from '../../../../src/constants/depots';

describe('Company selector: admin', () => {
  before(() => {
    cy.fixture('company-data').then(({ common }) => {
      const link = urls.dashboard.createLink(common.companyId, {});
      cy.yandexLogin('admin', { link });
      cy.waitForElement(selectors.sidebar.companySelector.toggleControl);
    });
  });

  after(function () {
    cy.clearCookies();
  });

  describe('Company is not an observer of other companies` orders', () => {
    // @see https://testpalm.yandex-team.ru/courier/testcases/438
    it('Contains depots', () => {
      cy.get(selectors.sidebar.companySelector.control).click();

      cy.get(selectors.sidebar.companySelector.depots.title).should('exist');
      cy.get(selectors.sidebar.companySelector.depots.dropdownItems.allDepots).should('exist');
      cy.get(selectors.sidebar.companySelector.depots.dropdownItems.secondDepot).should('exist');
    });

    it('Not contains orgs', () => {
      cy.get(selectors.sidebar.companySelector.orgs.dropdownItems.anyOrg).should('not.exist');
    });

    it('Show selected depot address', () => {
      cy.get(selectors.sidebar.companySelector.depots.search.input).type(
        depotNameRecord.castlePeach,
        { force: true },
      );
      cy.get(selectors.sidebar.companySelector.depots.dropdownItems.firstDepot).click({
        force: true,
      });

      cy.get(selectors.sidebar.companySelector.blockClosed).should('exist');
      cy.get(selectors.sidebar.companySelector.depots.currentDepotAddress).should(
        'have.text',
        depotAddressRecord.castlePeach,
      );
    });
  });
});
