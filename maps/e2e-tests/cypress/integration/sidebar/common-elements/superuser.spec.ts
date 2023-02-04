import selectors from '../../../../src/constants/selectors';
import urls from '../../../../src/utils/urls';
import { isScrollable } from '../../../../src/utils/elements';
import { forEach } from 'lodash';

describe('Company selector: superuser', () => {
  // @see https://testpalm.yandex-team.ru/courier/testcases/448
  before(() => {
    cy.preserveCookies();

    cy.fixture('company-data').then(({ shareFromOthersCompany }) => {
      const link = urls.dashboard.createLink(shareFromOthersCompany.companyId, {});
      cy.yandexLogin('superuser', { link });
      cy.waitForElement(selectors.sidebar.companySelector.toggleControl, { timeout: 10000 });
    });
  });

  after(function () {
    cy.clearCookies();
  });

  it('Open company selector', () => {
    cy.get(selectors.sidebar.companySelector.block).then(el => {
      if (el[0].matches(selectors.sidebar.companySelector.blockClosed)) {
        cy.get(selectors.sidebar.companySelector.control).click();
      }
    });

    cy.get(selectors.sidebar.companySelector.orgs.input).should('exist');
    cy.get(selectors.sidebar.companySelector.orgs.dropdownButton).should('exist');
    cy.get(selectors.sidebar.companySelector.orgs.dropdown).should('exist');

    cy.get(selectors.sidebar.companySelector.depots.title).should('exist');
    cy.get(selectors.sidebar.companySelector.depots.dropdownList).should('exist');
  });

  it('Show all orgs', () => {
    cy.get(selectors.sidebar.companySelector.block).then(el => {
      if (el[0].matches(selectors.sidebar.companySelector.blockClosed)) {
        cy.get(selectors.sidebar.companySelector.control).click();
      }
    });

    cy.get(selectors.sidebar.companySelector.orgs.dropdownList).then($el => {
      cy.wrap($el.innerHeight()).then(height => {
        cy.get(selectors.sidebar.companySelector.orgs.dropdownButton).click({ force: true }); // open

        cy.get(selectors.sidebar.companySelector.orgs.dropdownList).then($el => {
          cy.wrap(height).should('be.lt', $el.innerHeight());
        });
      });
    });

    cy.get(selectors.sidebar.companySelector.orgs.dropdownList).then($el => {
      cy.wrap($el.innerHeight()).then(height => {
        cy.get(selectors.sidebar.companySelector.orgs.dropdownButton).click({ force: true }); // close

        cy.get(selectors.sidebar.companySelector.orgs.dropdownList).then($el => {
          cy.wrap(height).should('be.gt', $el.innerHeight());
        });
      });
    });
  });

  describe('Company selector search', () => {
    // @see https://testpalm.yandex-team.ru/courier/testcases/450
    before(() => {
      cy.fixture('company-data').then(({ common }) => {
        const link = urls.dashboard.createLink(common.companyId, {});
        cy.yandexLogin('superuser', { link });
        cy.waitForElement(selectors.sidebar.companySelector.toggleControl, { timeout: 10000 });
        cy.get(selectors.sidebar.companySelector.control).click({ force: true });
      });
    });

    after(function () {
      cy.clearCookies();
    });

    it('Companies section is visible', () => {
      cy.get(selectors.sidebar.companySelector.orgs.search.input).should('exist');
      cy.get(selectors.sidebar.companySelector.orgs.dropdownItems.list).then(list => {
        expect(isScrollable(list[0])).eq(true);
      });
      cy.get(selectors.sidebar.companySelector.orgs.showAll).should('exist');
    });

    it('Depots section is visible', () => {
      cy.get(selectors.sidebar.companySelector.depots.search.input).should('exist');
      cy.get(selectors.sidebar.companySelector.depots.title).should('exist');
      cy.get(selectors.sidebar.companySelector.depots.dropdownItems.list).then(list => {
        expect(isScrollable(list[0])).eq(true);
      });
      cy.get(selectors.sidebar.companySelector.depots.dropdownItems.allDepots).should('exist');
    });

    it('Search for companies', () => {
      cy.get(selectors.sidebar.companySelector.orgs.search.input).clear({ force: true });
      cy.get(selectors.sidebar.companySelector.orgs.search.input).type('YTEST-122', {
        force: true,
      });

      cy.get(selectors.sidebar.companySelector.orgs.search.clearIcon).should('exist');
      cy.get(selectors.sidebar.companySelector.orgs.dropdownItems.anyOrgName).should(
        'contain.text',
        'YTEST-122',
      );
      cy.get(selectors.sidebar.companySelector.orgs.showAll).should('not.exist');

      cy.get(selectors.sidebar.companySelector.orgs.dropdownItems.list).then(list => {
        expect(isScrollable(list[0])).eq(false);
      });
    });

    it('Search input is empty after clicking clear icon', () => {
      cy.get(selectors.sidebar.companySelector.orgs.search.clearIcon).click({ force: true });

      cy.get(selectors.sidebar.companySelector.orgs.search.input).should('have.value', '');
      cy.get(selectors.sidebar.companySelector.orgs.dropdownItems.list).then(list => {
        expect(isScrollable(list[0])).eq(true);
      });
      cy.get(selectors.sidebar.companySelector.orgs.showAll).should('exist');
    });

    it('Search for other companies', () => {
      cy.get(selectors.sidebar.companySelector.orgs.search.input).clear({ force: true });
      cy.get(selectors.sidebar.companySelector.orgs.search.input).type('6555', { force: true });

      cy.get(selectors.sidebar.companySelector.orgs.search.clearIcon).should('exist');
      cy.get(selectors.sidebar.companySelector.orgs.dropdownItems.anyOrg).should(
        'contain.text',
        '6555',
      );

      cy.get(selectors.sidebar.companySelector.orgs.dropdownItems.list).then(list => {
        expect(isScrollable(list[0])).eq(false);
      });
    });

    it('Clear input after second search', () => {
      cy.get(selectors.sidebar.companySelector.orgs.search.clearIcon).click({ force: true });

      cy.get(selectors.sidebar.companySelector.orgs.search.input).should('have.value', '');
      cy.get(selectors.sidebar.companySelector.orgs.dropdownItems.list).then(list => {
        expect(isScrollable(list[0])).eq(true);
      });
      cy.get(selectors.sidebar.companySelector.orgs.showAll).should('exist');
    });

    it('Search for depots by address', () => {
      cy.fixture('testData').then(({ depotNameRecord }) => {
        cy.waitForElement(selectors.sidebar.companySelector.toggleControl, { timeout: 10000 });
        cy.get(selectors.sidebar.companySelector.depots.search.input)
          .type('пуш', { force: true })
          .wait(100)
          .then(() => {
            forEach(
              [
                depotNameRecord.additional1,
                depotNameRecord.additional2,
                depotNameRecord.additional3,
              ],
              depot => {
                cy.get(selectors.sidebar.companySelector.depots.dropdownItems.anyDepot).should(
                  'contain.text',
                  depot,
                );
              },
            );
          });

        cy.get(selectors.sidebar.companySelector.depots.search.clearIcon).should('exist');
        cy.get(selectors.sidebar.companySelector.depots.dropdownItems.list).then(list => {
          expect(isScrollable(list[0])).eq(false);
        });
      });
    });

    it('Clear depots search input', () => {
      cy.get(selectors.sidebar.companySelector.depots.search.clearIcon).click({ force: true });

      cy.get(selectors.sidebar.companySelector.depots.search.input).should('have.value', '');
      cy.get(selectors.sidebar.companySelector.depots.dropdownItems.list).then(list => {
        expect(isScrollable(list[0])).eq(true);
      });
    });

    it('Not found icon is visible when search for nonexistent depots', () => {
      cy.get(selectors.sidebar.companySelector.depots.search.input).clear({ force: true });
      cy.get(selectors.sidebar.companySelector.depots.search.input).type('кккк', { force: true });

      cy.get(selectors.sidebar.companySelector.depots.search.input).should('have.value', 'кккк');
      cy.get(selectors.sidebar.companySelector.depots.search.notFound).should('exist');
    });

    it('Clear depots search input after not found', () => {
      cy.get(selectors.sidebar.companySelector.depots.search.clearIcon).click({ force: true });

      cy.get(selectors.sidebar.companySelector.depots.search.input).should('have.value', '');
      cy.get(selectors.sidebar.companySelector.depots.dropdownItems.list).then(list => {
        expect(isScrollable(list[0])).eq(true);
      });
    });

    it('Not found icon and depots list are visible when search for nonexistent companies', () => {
      cy.get(selectors.sidebar.companySelector.orgs.search.input).clear({ force: true });
      cy.get(selectors.sidebar.companySelector.orgs.search.input)
        .click({ force: true })
        .type('кккк', { force: true });

      cy.get(selectors.sidebar.companySelector.orgs.search.input).should('have.value', 'кккк');
      cy.get(selectors.sidebar.companySelector.orgs.search.clearIcon).should('exist');
      cy.get(selectors.sidebar.companySelector.orgs.search.notFound).should('exist');

      cy.get(selectors.sidebar.companySelector.depots.dropdownItems.anyDepot).should('exist');
      cy.get(selectors.sidebar.companySelector.depots.dropdownItems.list).then(list => {
        expect(isScrollable(list[0])).eq(true);
      });
    });
  });
});
