import selectors from '../../../../src/constants/selectors';
import * as userSettingsKeyset from '../../../../../src/translations/users-settings';
import map from 'lodash/map';

const phone = '+79051102365';

const texts = {
  title: userSettingsKeyset.ru.titles_couriers,
  addButtonText: userSettingsKeyset.ru.add,
  successfullyAdded: userSettingsKeyset.ru.userAdded,
  removeButtonText: userSettingsKeyset.ru.delete,
  userAlreadyExists: userSettingsKeyset.ru.duplicatedCourierLogin,
};

context('Mobile app', () => {
  before(() => {
    cy.preserveCookies();
    cy.yandexLogin('manager');
    cy.get(selectors.sidebar.menu.settingsGroup).click();
    cy.get(selectors.sidebar.menu.couriersSettings).click();
    cy.get(selectors.users.user).then($users => {
      cy.wrap($users.length).as('initialLength');
      cy.wrap(map($users, $user => $user.textContent)).as('initialUsers');
      cy.get(selectors.users.addInput).type(phone);
      cy.get(selectors.users.addBtn).click();

      cy.get(selectors.users.title).should('be.visible').and('have.text', texts.title);
      cy.get(selectors.users.userList).should('be.visible');
      cy.get(selectors.users.search).should('be.visible');
      cy.get(selectors.users.addBtn)
        .should('be.visible')
        .and('be.disabled')
        .and('have.text', texts.addButtonText);
      cy.get(selectors.users.user).should('have.length', $users.length + 1);
      cy.get(`[alt="${texts.successfullyAdded}"]`).should('have.length', 1);
    });
  });

  context('Couriers', () => {
    afterEach(function () {
      cy.get(selectors.users.addInput).clear();
      cy.get(selectors.users.user).each(($user, index) => {
        // removing added on previous step users
        if (index > this.initialLength) {
          cy.wait(500);
          cy.wrap($user).contains('button', texts.removeButtonText).click({ force: true });
          cy.get(selectors.modal.dialog.submit).click();
        }
      });
    });

    it('should display couriers and not contain duplicates(1)', function () {
      const phones =
        '+79098881619,+79098881620,+79098881621,+79098881622,+79098881623,+79098881621';
      cy.get(selectors.users.addInput).type(phones);
      cy.get(selectors.users.addBtn).click();
      cy.get(selectors.users.user)
        .should('have.length', this.initialLength + 6)
        .should('be.visible');
      cy.get(`[alt="${texts.successfullyAdded}"]`).should('have.length', 6);
      cy.get(selectors.users.addBtn).and('be.disabled');
      cy.get(selectors.users.addInput).and('not.contain.value');
    });

    it('should display couriers and not contain duplicates(2)', function () {
      const phones = '+79051102365,+79098881620,+79098881621,+79098881622,+79098881623';
      const infoText = '1 номеров уже есть в системе, добавим 4 новых(-ый)';
      cy.get(selectors.users.addInput).type(phones);
      cy.get(selectors.users.info).should('have.text', infoText);
      cy.get(selectors.users.addBtn).click();
      cy.get(selectors.users.user)
        .should('have.length', this.initialLength + 5)
        .should('be.visible');
      cy.get(`[alt="${texts.successfullyAdded}"]`).should('have.length', 5);
      cy.get(selectors.users.addBtn).and('be.disabled');
      cy.get(selectors.users.addInput).and('not.contain.value');
    });

    it('should display couriers and not contain duplicates(3)', function () {
      const phones =
        '+79098881619,+79098881620,+79098881621,+79098881622,+79098881623,+79098881621,+79098881624,+79098881625,+79098881626,+79098881621,+79098881627,+79098881628,+79098881622';
      cy.get(selectors.users.addInput).type(phones);
      cy.get(selectors.users.addBtn).click();
      cy.get(selectors.users.user)
        .should('have.length', this.initialLength + 11)
        .should('be.visible');
      cy.get(`[alt="${texts.successfullyAdded}"]`).should('have.length', 11);
      cy.get(selectors.users.addBtn).and('be.disabled');
      cy.get(selectors.users.addInput).and('not.contain.value');
    });

    it('should show error on adding existing courier', () => {
      cy.get(selectors.users.addInput).type(phone);
      cy.get(selectors.users.addBtn).and('be.disabled');
      cy.get(selectors.users.error).should('have.text', texts.userAlreadyExists);
    });
  });

  context('Couriers + Monitoring > Couriers tab', () => {
    it('should display couriers and not contain duplicates', function () {
      const phones = '+79098881619,79098881621,89098881621,+79098881622,+79098881623,+79098881621';
      cy.get(selectors.users.addInput).type(phones);
      cy.get(selectors.users.addBtn).click();
      cy.get(selectors.users.user)
        .should('have.length', this.initialLength + 5)
        .should('be.visible');
      cy.get(`[alt="${texts.successfullyAdded}"]`).should('have.length', 5);
      cy.get(selectors.users.addBtn).and('be.disabled');
      cy.get(selectors.users.addInput).and('not.contain.value');
    });

    it('should add new courier in monitoring couriers tab', () => {
      cy.intercept('**/users', { times: 1 }, req => {
        req.continue(res => {
          expect(res.statusCode).eq(422);
        });
      }).as('users');

      cy.get(selectors.sidebar.menu.monitoringGroup).click();
      cy.get(selectors.sidebar.menu.couriers).click();
      cy.get(selectors.content.couriers.newCourier.number).type('test');
      cy.get(selectors.content.couriers.newCourier.phone).type('89098881621');
      cy.get(selectors.content.couriers.newCourier.submitButton).click();
      cy.wait('@users');
      cy.get(selectors.content.couriers.search.lineName)
        .eq(0)
        .should('have.text', 'test')
        // removing after success test
        .closest(selectors.content.couriers.search.line)
        .find(selectors.content.couriers.search.lineRemove)
        .click();
      cy.get(selectors.modal.dialog.submit).click();
    });

    it('should show courier after search', () => {
      cy.get(selectors.sidebar.menu.couriersSettings).click();

      cy.get(selectors.users.search).type('1621');
      cy.get(selectors.users.user).should('have.length', 1).should('be.visible');
    });

    after(function () {
      // removing users
      cy.wait(1000);
      cy.get(selectors.users.search).clear();
      cy.get(selectors.users.user).each($user => {
        if (!this.initialUsers.includes($user.text())) {
          cy.wrap($user).contains('button', texts.removeButtonText).click({ force: true });
          cy.get(selectors.modal.dialog.submit).click();
        }
      });
    });
  });
});
