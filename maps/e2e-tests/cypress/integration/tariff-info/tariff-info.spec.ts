import selectors from '../../../src/constants/selectors';
import moment from 'moment';
import urls from '../../../src/utils/urls';

context('TariffInfo', () => {
  it('should show tariff info when user role is admin', () => {
    cy.fixture('company-data').then(({ common }) => {
      const date = moment(new Date()).add(-2, 'days').format(urls.dashboard.dateFormat);
      const link = urls.dashboard.createLink(common.companyId, { date });
      cy.yandexLogin('admin', { link });
    });
    cy.get(selectors.sidebar.tariffInfo).should('exist');
  });
  it('should show tariff info when user role is manager', () => {
    cy.fixture('company-data').then(({ common }) => {
      const date = moment(new Date()).add(-2, 'days').format(urls.dashboard.dateFormat);
      const link = urls.dashboard.createLink(common.companyId, { date });
      cy.yandexLogin('manager', { link });
    });
    cy.get(selectors.sidebar.tariffInfo).should('exist');
  });
});
