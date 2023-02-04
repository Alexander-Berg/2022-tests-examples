import selectors from '../../../src/constants/selectors';
import forEach from 'lodash/forEach';

const testData = [
  {
    domain: 'ru',
    url: 'maps.yandex.ru',
  },
  {
    domain: 'com',
    url: 'google.com/maps',
  },
  {
    domain: 'com.tr',
    url: 'maps.yandex.com.tr',
  },
  {
    domain: 'com',
    path: 'es',
    url: 'google.com/maps',
  },
] as const;

const testCreator = (domain: SupportedTld, url: string, path?: 'es') => (): void => {
  before(() => {
    cy.preserveCookies();
    cy.yandexLogin('manager', { tld: domain, basePath: path ? `/courier/${path}/` : undefined });
    cy.get(selectors.sidebar.menu.monitoringGroup).click();
    cy.get(selectors.sidebar.menu.orders).click();

    cy.get(selectors.content.orders.tableLoaded).should('exist');
    cy.get(selectors.content.orders.firstOrder).click();
    cy.get(selectors.modal.orderPopup.view).should('exist');
  });

  it(`link in address should open ${url}`, function () {
    cy.get(selectors.modal.orderPopup.addressRow)
      .find('a')
      .should('have.attr', 'href')
      .and('contain', url);
  });
};

forEach(testData, ({ domain, url, path }: { domain: SupportedTld; url: string; path?: 'es' }) => {
  let baseUrl = Cypress.env('BASE_URL').replace('ru', domain);
  if (path) {
    baseUrl = baseUrl.replace('/courier/', `/courier/${path}/`);
  }
  context(`Courier page (${path || domain})`, { baseUrl }, testCreator(domain, url, path));
});
