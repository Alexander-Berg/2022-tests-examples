import * as usersSettingsKeyset from '../../../../../src/translations/users-settings';
import selectors from '../../../../src/constants/selectors';
import urls from '../../../../src/utils/urls';

const createEmail = (login: string): string => `${login}@yandex.ru`;

// TODO: should take entities from /translations
const asserts = {
  managersTitle: usersSettingsKeyset.ru.titles_managers,
  managerAdvice: usersSettingsKeyset.ru.addHint_managers,
  managerCheckValidEmail: usersSettingsKeyset.ru.notValidEmail,
  managerWarningExistUser: usersSettingsKeyset.ru.duplicatedLogin,
};
// TODO: should add cypress command to delete manager or do it in some other way
const deleteManager = (email: string): void => {
  cy.get(selectors.users.user).contains(email).click();
  cy.get(selectors.user.deleteBtn).click();
  cy.get(selectors.modal.dialog.submit).click();
};

const BRANCH_NAME = Cypress.env('BRANCH_NAME');
const taskNumber = BRANCH_NAME.match(/\d+/)?.[0] ?? '';

const VALID_EMAIL = `vodka${taskNumber}@yandex.ru`;
const VALID_LOGIN = `balalayka${taskNumber}`;
const VALID_EMAILS = `matroshka${taskNumber}@yandex.ru,pelmeny${taskNumber}@yandex.ru`;
const INVALID_EMAIL = '@boba.ru';

const EMAIL_FROM_OTHER_COMPANY = `yndx-must-del-after-test${taskNumber}@yandex.ru`;

context('Add a new manager', () => {
  before(() => {
    cy.yandexLogin('admin');
  });

  it('should open a manager list', () => {
    cy.get(selectors.sidebar.menu.settingsGroup).click();
    cy.get(selectors.sidebar.menu.users).click();
    cy.get(selectors.users.title).should('include.text', asserts.managersTitle); // TODO: should take title from /translations
  });

  it('should add a valid email', () => {
    cy.get(selectors.users.addInput).type(VALID_EMAIL).type('{enter}');
    cy.get(selectors.users.userList).contains(VALID_EMAIL);

    cy.get(selectors.users.usersHint).should('have.text', asserts.managerAdvice);
  });

  it('should add a manager by a valid login', () => {
    cy.get(selectors.users.addInput).clear();
    cy.get(selectors.users.addInput).type(VALID_LOGIN);
    cy.get(selectors.users.addBtn).click();
    cy.get(selectors.users.userList).contains(createEmail(VALID_LOGIN));
  });

  it('should add a some manager by a valid emails', () => {
    cy.get(selectors.users.addInput).clear();
    cy.get(selectors.users.addInput).type(VALID_EMAILS).type('{enter}');
    VALID_EMAILS.split(',').forEach(email => {
      cy.get(selectors.users.userList).contains(email);
    });
  });

  it('should not add a manager by an invalid email', () => {
    cy.get(selectors.users.addInput).clear();
    cy.get(selectors.users.addInput).type(INVALID_EMAIL).type('{enter}');
    cy.get(selectors.users.addInput).should('contain.value', INVALID_EMAIL);
    cy.get(selectors.users.error)
      .should('exist')
      .should('contain.text', asserts.managerCheckValidEmail);
    cy.get(selectors.users.addBtn).should('be.disabled');
  });

  it('should not add a manager by an exist email', () => {
    cy.get(selectors.users.addInput).clear();
    cy.get(selectors.users.addInput).type(VALID_EMAIL).type('{enter}');
    cy.get(selectors.users.addInput).should('contain.value', VALID_EMAIL);
    cy.get(selectors.users.error)
      .should('exist')
      .should('contain.text', asserts.managerWarningExistUser);
    cy.get(selectors.users.addBtn).should('be.disabled');
  });

  it('should not add a manager by an exist email', () => {
    cy.get(selectors.users.addInput).clear();
    cy.get(selectors.users.addInput).type(VALID_EMAIL).type('{enter}');
    cy.get(selectors.users.addInput).should('contain.value', VALID_EMAIL);
    cy.get(selectors.users.error)
      .should('exist')
      .should('contain.text', asserts.managerWarningExistUser);
    cy.get(selectors.users.addBtn).should('be.disabled');
  });

  // @see https://testpalm.yandex-team.ru/courier/testcases/598
  it('should not add a manager by an exist email in another case', () => {
    const caseChangedValidEmail = `${VALID_EMAIL.slice(0, 3).toUpperCase()}${VALID_EMAIL.slice(3)}`;
    cy.get(selectors.users.addInput).clear();
    cy.get(selectors.users.addInput).type(caseChangedValidEmail).type('{enter}');
    cy.get(selectors.users.addInput).should('contain.value', caseChangedValidEmail);
    cy.get(selectors.users.error)
      .should('exist')
      .should('contain.text', asserts.managerWarningExistUser);
    cy.get(selectors.users.addBtn).should('be.disabled');
  });

  after(() => {
    deleteManager(VALID_EMAIL);
    deleteManager(createEmail(VALID_LOGIN));

    VALID_EMAILS.split(',').forEach(email => {
      deleteManager(email);
    });
  });
});

context(
  'Testcase  verifies the impossibility of adding an email that already exists in another company',
  () => {
    before(() => {
      cy.fixture('company-data').then(({ common }) => {
        const link = urls.dashboard.createLink(common.companyId, {});
        cy.yandexLogin('admin', { link });
      });
      cy.get(selectors.sidebar.menu.settingsGroup).click();
      cy.get(selectors.sidebar.menu.users).click();
      cy.get(selectors.users.title).should('include.text', asserts.managersTitle);

      cy.get(selectors.users.addInput).type(EMAIL_FROM_OTHER_COMPANY).type('{enter}');
      cy.get(selectors.users.userList).contains(EMAIL_FROM_OTHER_COMPANY);

      cy.get(selectors.users.usersHint).should('have.text', asserts.managerAdvice);
    });

    it('should not add same email in second company', () => {
      cy.fixture('company-data').then(({ shareFromOthersCompany }) => {
        const link = urls.dashboard.createLink(shareFromOthersCompany.companyId, {});
        cy.yandexLogin('adminMulti', { link });
      });
    });

    it('should open a manager list', () => {
      cy.get(selectors.sidebar.menu.settingsGroup).click();
      cy.get(selectors.sidebar.menu.users).click();
      cy.get(selectors.users.title).should('include.text', asserts.managersTitle);
    });

    it('should not add same email to second company', () => {
      cy.get(selectors.users.addInput).type(EMAIL_FROM_OTHER_COMPANY).type('{enter}');
      cy.get(selectors.modal.errorPopup.view).should('exist');
      cy.get(selectors.modal.errorPopup.closeButton).click();
    });

    it('should not add same email to second company in retry', () => {
      cy.get(selectors.managers.searchItem).last().realHover();
      cy.get(selectors.managers.retryButton).click();
      cy.get(selectors.modal.errorPopup.view).should('exist');
    });

    after(() => {
      cy.fixture('company-data').then(({ common }) => {
        const link = urls.dashboard.createLink(common.companyId, {});
        cy.yandexLogin('admin', { link });
      });
      cy.get(selectors.sidebar.menu.settingsGroup).click();
      cy.get(selectors.sidebar.menu.users).click();
      cy.get(selectors.users.title).should('include.text', asserts.managersTitle);
      deleteManager(EMAIL_FROM_OTHER_COMPANY);
    });
  },
);
