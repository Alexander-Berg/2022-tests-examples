import * as commonKeyset from '../../../../src/translations/common';
import selectors from '../../../src/constants/selectors';
import urls from '../../../src/utils/urls';

context('Company control', function () {
  describe('Search by companies in A', function () {
    before(function () {
      cy.fixture('company-data').then(({ A }) => {
        const link = urls.dashboard.createLink(A.companyId, {});

        cy.yandexLogin('superuser', { link });
        cy.waitForElement(selectors.sidebar.companySelector.toggleControl);

        cy.get(selectors.content.dashboard.view);
        cy.get(selectors.sidebar.companySelector.control).click();
        cy.get(selectors.sidebar.companySelector.orgs.dropdown);
        cy.get(selectors.sidebar.companySelector.orgs.dropdownButton).click({ force: true });
      });
    });

    afterEach(function () {
      cy.get(selectors.sidebar.companySelector.orgs.input).clear({ force: true });
    });

    // @see https://testpalm.yandex-team.ru/testcase/courier-169
    it('Rus', function () {
      cy.get(selectors.sidebar.companySelector.orgs.input).type('тесто', { force: true });
      cy.get(selectors.sidebar.companySelector.orgs.dropdownItems.anyOrgName).contains(/тесто/i);
    });

    // @see https://testpalm.yandex-team.ru/testcase/courier-171
    it('Rus on Upper case', function () {
      cy.get(selectors.sidebar.companySelector.orgs.input).type('ТЕСТО', { force: true });
      cy.get(selectors.sidebar.companySelector.orgs.dropdownItems.anyOrgName).contains(/ТЕСТО/i);
    });

    // @see https://testpalm.yandex-team.ru/testcase/courier-170
    it('Eng', function () {
      cy.get(selectors.sidebar.companySelector.orgs.input).type('meg', { force: true });
      cy.get(selectors.sidebar.companySelector.orgs.dropdownItems.anyOrgName).contains(/meg/i);
    });

    it('Eng on Upper case', function () {
      cy.get(selectors.sidebar.companySelector.orgs.input).type('MEG', { force: true });
      cy.get(selectors.sidebar.companySelector.orgs.dropdownItems.anyOrgName).contains(/MEG/i);
    });

    // @see https://testpalm.yandex-team.ru/testcase/courier-172
    it('Numbers', function () {
      cy.get(selectors.sidebar.companySelector.orgs.input).type('12345', { force: true });
      cy.get(selectors.sidebar.companySelector.orgs.dropdownItems.anyOrgName).contains(/12345/i);
    });

    // @see https://testpalm.yandex-team.ru/testcase/courier-173
    it('Some symbols', function () {
      cy.get(selectors.sidebar.companySelector.orgs.input).type('ЛогистикаF6Ц', { force: true });
      cy.get(selectors.sidebar.companySelector.orgs.dropdownItems.anyOrgName).contains(
        /ЛогистикаF6Ц/i,
      );
    });

    // @see https://testpalm.yandex-team.ru/testcase/courier-175
    it('By company id', function () {
      cy.fixture('company-data').then(({ common }) => {
        cy.get(selectors.sidebar.companySelector.orgs.input).type(common.companyId.toString(), {
          force: true,
        });
        cy.get(selectors.sidebar.companySelector.orgs.dropdownItems.anyOrgName).contains(
          /Грибное Королевство/,
        );
      });
    });

    // @see https://testpalm.yandex-team.ru/testcase/courier-174
    it('Not found', function () {
      cy.get(selectors.sidebar.companySelector.orgs.input).type('sdvn20r8087342c78h832o7x9732', {
        force: true,
      });
      cy.get(selectors.sidebar.companySelector.orgs.dropdownItems.notice)
        .invoke('text')
        .should('eq', commonKeyset.ru.search_notFound);
    });
  });

  describe('Search in common', function () {
    // @see https://testpalm.yandex-team.ru/testcase/courier-44
    it('Count couriers after change company', function () {
      cy.fixture('company-data').then(({ common, A }) => {
        const link = urls.dashboard.createLink(common.companyId, {});

        cy.yandexLogin('superuser', { link });
        cy.waitForElement(selectors.sidebar.companySelector.toggleControl);

        cy.get(selectors.content.dashboard.view);
        cy.get(selectors.sidebar.companySelector.control).click();
        cy.get(selectors.sidebar.companySelector.orgs.dropdown);
        cy.get(selectors.sidebar.companySelector.orgs.input).click({ force: true });
        cy.get(selectors.sidebar.companySelector.orgs.input).type(String(A.companyId), {
          force: true,
        });
        cy.get(selectors.sidebar.companySelector.orgs.dropdownItems.anyOrg).click({ force: true });
        cy.get(selectors.content.dashboard.dayTotalRoutesNumber);
        cy.get(selectors.content.dashboard.dayTotalRoutesNumber).invoke('text').should('eq', '1');
      });
    });
  });
});
