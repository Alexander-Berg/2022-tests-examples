import selectors from '../../../../src/constants/selectors';
import urls from '../../../../src/utils/urls';
import testData from '../../../fixtures/testData.json';

describe('Company selector: manager', () => {
  // @see https://testpalm.yandex-team.ru/courier/testcases/448
  before(() => {
    cy.preserveCookies();
    cy.fixture('company-data').then(({ shareFromOthersCompany }) => {
      const link = urls.dashboard.createLink(shareFromOthersCompany.companyId, {});
      cy.yandexLogin('managerMulti', { link });
      cy.waitForElement(selectors.sidebar.companySelector.toggleControl);
    });
  });
  after(function () {
    cy.clearCookies();
  });

  it('Settings is presented in sidebar', () => {
    cy.get(selectors.sidebar.menu.settingsGroup).should('exist');
  });

  it('Choose depot', () => {
    cy.get(selectors.sidebar.companySelector.depots.currentDepotAddress)
      .invoke('text')
      .then(prevAddress => {
        cy.get(selectors.sidebar.companySelector.block).then(el => {
          if (el[0].matches(selectors.sidebar.companySelector.blockClosed)) {
            cy.get(selectors.sidebar.companySelector.control).click();
          }
        });

        cy.get(selectors.sidebar.companySelector.depots.dropdownItems.anyInactiveDepot)
          .first()
          .click({ force: true });

        cy.get(selectors.sidebar.companySelector.blockClosed);

        cy.get(selectors.sidebar.companySelector.depots.currentDepotAddress)
          .invoke('text')
          .should('not.eq', prevAddress);
      });
  });

  describe('Choose company', () => {
    before(() => {
      cy.fixture('company-data').then(({ V }) => {
        cy.get(selectors.sidebar.companySelector.block).then(el => {
          if (el[0].matches(selectors.sidebar.companySelector.blockClosed)) {
            cy.get(selectors.sidebar.companySelector.control).click();
          }
        });

        cy.get(
          selectors.sidebar.companySelector.orgs.dropdownItems.getInactiveOrg(`${V.companyId}`),
        )
          .first()
          .click({ force: true });
      });
    });

    it('Name of active company in selector', () => {
      cy.fixture('company-data').then(({ V }) => {
        cy.get(selectors.sidebar.companySelector.orgs.currentName)
          .invoke('text')
          .should('eq', V.companyName);
      });
    });

    it('Inactive company is in list', () => {
      cy.fixture('company-data').then(({ shareFromOthersCompany }) => {
        cy.get(
          selectors.sidebar.companySelector.orgs.dropdownItems.getInactiveOrg(
            `${shareFromOthersCompany.companyId}`,
          ),
        )
          .find(selectors.sidebar.companySelector.orgs.dropdownItems.name)
          .invoke('text')
          .should('eq', shareFromOthersCompany.companyName);
      });
    });

    it('Available depot visible', () => {
      cy.get(selectors.sidebar.companySelector.depots.dropdownItems.allDepots).then($el => {
        let isElementExist = false;
        $el.each(function () {
          if (this.innerText === testData.depotNameRecord.compVVisible) {
            isElementExist = true;
          }
        });
        cy.wrap(isElementExist).should('eq', true);
      });
    });

    it('Settings is not presented in sidebar', () => {
      cy.get(selectors.sidebar.menu.settingsGroup).should('not.exist');
    });
  });
});
