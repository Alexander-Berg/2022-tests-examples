import * as authKeyset from '../../../../src/translations/auth';
import * as errorPanelKeyset from '../../../../src/translations/error-panel';
import * as supportFormKeyset from '../../../../src/translations/support-form';

import selectors from '../../../src/constants/selectors';

const EXCEL_FILE_NAME = 'planningWithError.xlsx';

const errorDialog = {
  heading: {
    ru: errorPanelKeyset.ru.title,
    'com.tr': errorPanelKeyset.tr.title,
  },
  supportPage: {
    text: {
      ru: errorPanelKeyset.ru.supportLinkText,
      'com.tr': errorPanelKeyset.tr.supportLinkText,
    },
    href: {
      ru: errorPanelKeyset.ru.supportLink,
      'com.tr': authKeyset.en.createCompanySuccess_vrcDocLink,
    },
  },
  formButton: {
    text: {
      ru: errorPanelKeyset.ru.supportButton,
      'com.tr': "Teknik Destek Ekibi'ne yaz",
    },
    href: {
      ru: supportFormKeyset.ru.formLink,
      'com.tr': supportFormKeyset.tr.formLink,
    },
  },
};

// @see https://testpalm.yandex-team.ru/courier/testcases/728

const langs = ['ru', 'com.tr'] as const;

const testCreator = (lang: typeof langs[number]) => (): void => {
  before(() => {
    cy.yandexLogin('mvrpManager', { tld: lang });
    cy.preserveCookies();
    cy.clearLocalforage();
    cy.openAndCloseVideo();
  });

  beforeEach(() => {
    cy.window().then(win => {
      cy.stub(win, 'open', url => {
        expect(url).includes(errorDialog.formButton.href[lang]);
      });
    });
  });

  it('should be opened on error', () => {
    cy.get(selectors.content.mvrp.fileInput).attachFile(EXCEL_FILE_NAME);
    cy.get(selectors.modal.errorPopup.view).as('dialog').should('be.visible');
    cy.get('@dialog')
      .find(selectors.modal.errorPopup.heading)
      .should('be.visible')
      .and('have.text', errorDialog.heading[lang]);
    cy.get('@dialog').find(selectors.modal.errorPopup.messageTitle).should('not.be.empty');
  });

  it('should display link to support page', () => {
    cy.get(selectors.modal.errorPopup.view).as('dialog');

    cy.get('@dialog')
      .find(selectors.modal.errorPopup.helpLink)
      .should('be.visible')
      .and('have.text', errorDialog.supportPage.text[lang])
      .and('have.attr', 'href', errorDialog.supportPage.href[lang])
      .and('have.attr', 'target', '_blank');
  });

  it('should display button-link to tech support request form', () => {
    cy.get(selectors.modal.errorPopup.view).as('dialog');

    cy.get('@dialog')
      .find(selectors.modal.errorPopup.formButton)
      .should('be.visible')
      .and('have.text', errorDialog.formButton.text[lang])
      .click();
  });
};

for (const lang of langs) {
  describe(`Error dialog ${lang}`, testCreator(lang));
}
