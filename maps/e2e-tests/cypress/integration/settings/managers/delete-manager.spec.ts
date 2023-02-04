import * as managersSettingsKeyset from '../../../../../src/translations/managers-settings';
import selectors from '../../../../src/constants/selectors';

context('Delete manager', () => {
  before(() => {
    cy.yandexLogin('admin');
  });

  const checkRole = (role: string): void => {
    cy.get(userSelector.roleRadio)
      .contains(role)
      .closest(userSelector.checkedRadioInput)
      .should('exist');
  };

  const testManagerEmail = 'hello123@yandex.ru';

  const usersSelector = selectors.users;
  const userSelector = selectors.user;

  it('should add new manager', () => {
    cy.get(selectors.sidebar.menu.settingsGroup)
      .click()
      .get(selectors.sidebar.menu.users)
      .click()
      .get(usersSelector.addInput)
      .type(testManagerEmail)
      .get(usersSelector.addBtn)
      .click()
      .wait(500);
  });

  it('should open manager page', () => {
    cy.get(usersSelector.user)
      .contains(testManagerEmail)
      .click()
      .then(() => {
        checkRole(managersSettingsKeyset.ru.role_manager);
      });
  });

  it('should open and close delete modal', () => {
    cy.get(userSelector.deleteBtn)
      .click()
      .get(selectors.modal.dialog.cancel)
      .click()
      .get(selectors.modal.dialog.title)
      .should('not.exist');
  });

  it('should delete user', () => {
    cy.get(userSelector.deleteBtn)
      .click()
      .get(selectors.modal.dialog.submit)
      .click()
      .get(usersSelector.user)
      .contains(testManagerEmail)
      .should('not.exist');
  });

  it('should show own account', () => {
    cy.fixture('testData').then(({ accounts }) => {
      cy.get(usersSelector.user)
        .contains(accounts.admin)
        .click()
        .then(() => checkRole(managersSettingsKeyset.ru.role_admin))
        .get(userSelector.info.sectionDepotsWarning)
        .should('exist')
        .get(userSelector.info.forbiddenSelfChanges)
        .should('exist');
    });
  });

  it('should have warnings', () => {
    checkRole(managersSettingsKeyset.ru.role_admin);
    cy.get(userSelector.info.sectionDepotsWarning).should('exist');
    cy.get(userSelector.info.forbiddenSelfChanges).should('exist');
  });

  it('should not be able to delete yourself', () => {
    const BUTTON_DISABLE_CLASS = 'button3_disabled';
    cy.get(userSelector.deleteBtn).should('have.class', BUTTON_DISABLE_CLASS);
    cy.get(userSelector.info.forbiddenSelfDelete).should('exist');
  });
});
