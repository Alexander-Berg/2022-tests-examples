import selectors from '../../../../src/constants/selectors';
import urls from '../../../../src/utils/urls';
import moment from 'moment';

context('Page for user without access', function () {
  beforeEach(function () {
    cy.preserveCookies();
  });

  before(function () {
    cy.fixture('company-data').then(({ common }) => {
      const date = moment(new Date()).add(-2, 'days').format(urls.dashboard.dateFormat);
      const link = urls.dashboard.createLink(common.companyId, { date });
      cy.yandexLogin('temp', { link });
    });
  });

  it('Suggest to change account', function () {
    cy.get(selectors.noAccess.button).should('be.visible');
  });

  after(function () {
    cy.clearCookies();
  });
});
