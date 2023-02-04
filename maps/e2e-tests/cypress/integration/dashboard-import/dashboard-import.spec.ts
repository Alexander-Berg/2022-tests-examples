import * as authKeyset from '../../../../src/translations/auth';
import * as supportFormKeyset from '../../../../src/translations/support-form';
import * as errorPanelKeyset from '../../../../src/translations/error-panel';
import * as importKeyset from '../../../../src/translations/import';
import { addressInputOnMapClearSharedSpec } from '../shared/address-input-clear.spec';
import forEach from 'lodash/forEach';
import selectors from '../../../src/constants/selectors';

// @see https://testpalm.yandex-team.ru/courier/testcases/551
context('Import on dashboard - address input', function () {
  before(() => {
    cy.yandexLogin('admin');
    cy.get(selectors.content.dashboard.importRoutes).click();
    cy.get(selectors.modal.importRoutes.sidebar.fileInput)
      .attachFile('importTemplate.xlsx')
      .wait(300)
      .get(selectors.modal.importRoutes.loader)
      .should('not.exist');

    cy.get(selectors.modal.importRoutes.sidebar.tab)
      .contains(importKeyset.ru.tabTitles_map)
      .click();
  });

  addressInputOnMapClearSharedSpec();
});

context('Import on dashboard - support', function () {
  // @see https://testpalm.yandex-team.ru/courier/testcases/499
  const domains = [
    {
      tld: 'ru' as SupportedTld,
      errorText: 'Не удалось распознать заголовок в листе Options',
      supportFormButtonText: errorPanelKeyset.ru.supportButton,
      supportFormUrl: supportFormKeyset.ru.formLink,
      supportFormTitle: 'Обращение в службу поддержки Яндекс Маршрутизации',
      supportLinkText: errorPanelKeyset.ru.supportLinkText,
      supportLink: errorPanelKeyset.ru.supportLink,
    },
    {
      tld: 'com.tr' as SupportedTld,
      errorText: 'Options listesinde başlık tanınamadı',
      supportFormButtonText: "Teknik Destek Ekibi'ne yaz",
      supportFormUrl: supportFormKeyset.tr.formLink,
      supportFormTitle: 'RouteQ support',
      supportLinkText: errorPanelKeyset.tr.supportLinkText,
      supportLink: authKeyset.tr.createCompanySuccess_vrcDocLink,
    },
  ];

  forEach(domains, parameters => {
    const baseUrl = Cypress.env('BASE_URL').replace('ru', parameters.tld);

    describe(
      `${parameters.tld}: Contacting support from an errors panel`,
      { baseUrl },
      function () {
        beforeEach(function () {
          cy.preserveCookies();
        });

        before(function () {
          cy.yandexLogin('manager', { tld: parameters.tld });
        });

        it('should open routes import modal', function () {
          cy.get(selectors.content.dashboard.importRoutes).click();

          cy.get(selectors.modal.importRoutes.view).should('exist');
          cy.get(selectors.modal.importRoutes.tabs.settings).should('exist');
          cy.get(selectors.modal.importRoutes.sidebar.submitImport).should('be.disabled');
        });

        it('should open errors panel when uploading file', function () {
          cy.get(selectors.modal.importRoutes.sidebar.fileInput).attachFile('routesWithError.xlsx');

          cy.get(selectors.modal.importRoutes.loader).should('exist');
          // a moment later
          cy.get(selectors.modal.importRoutes.view).should('not.exist');
          cy.get(selectors.modal.errorPopup.view).should('exist');
          cy.get(selectors.modal.errorPopup.log).should('contain.text', parameters.errorText);
          cy.get(selectors.modal.errorPopup.supportButton)
            .should('exist')
            .and('contain.text', parameters.supportFormButtonText);
          cy.get(selectors.modal.errorPopup.supportLink)
            .should('exist')
            .and('contain.text', parameters.supportLinkText);
        });

        it('should open page with support form on button click', function () {
          cy.window().then(win => {
            cy.stub(win, 'open').as('windowOpen');
          });

          cy.get(selectors.modal.errorPopup.supportButton).click();

          cy.get('@windowOpen').should(
            'be.calledWithMatch',
            (url: string) => url.startsWith(parameters.supportFormUrl),
            '_blank',
          );

          cy.request(parameters.supportFormUrl)
            .its('body')
            .should('include', parameters.supportFormTitle);
        });

        it('should contain link to help', function () {
          cy.get(selectors.modal.errorPopup.supportLink)
            .should('exist')
            .and('have.attr', 'href', parameters.supportLink)
            .and('have.attr', 'target', '_blank');
        });
      },
    );
  });
});
