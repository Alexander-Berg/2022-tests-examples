import selectors from '../../../src/constants/selectors';

import urls from '../../../src/utils/urls';

// @see https://testpalm.yandex-team.ru/testcase/courier-30
context('Datepicker', function () {
  before(function () {
    cy.fixture('company-data').then(({ common }) => {
      const date = '2018-06-14';
      const link = urls.dashboard.createLink(common.companyId, { date });

      cy.yandexLogin('manager', { link });

      cy.get(selectors.content.dashboard.view);
    });
  });

  describe('Dashboard.', function () {
    it('Change date', function () {
      cy.get(selectors.content.dashboard.dayTitle).click();
      cy.get(selectors.datePicker.daysGrid.days['15']).click();
      cy.get(selectors.content.dashboard.dayTitle);
      cy.get(selectors.content.dashboard.dayTitle).invoke('text').should('eq', '15 июня');
    });
  });
});
