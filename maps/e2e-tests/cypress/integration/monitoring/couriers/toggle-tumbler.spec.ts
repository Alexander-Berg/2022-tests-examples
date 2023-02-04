import * as courierDetailsKeyset from '../../../../../src/translations/courier-details';
import * as commonKeyset from '../../../../../src/translations/common';
import * as couriersKeyset from '../../../../../src/translations/couriers';
import * as importRoutesKeyset from '../../../../../src/translations/import-routes';
import selectors from '../../../../src/constants/selectors';
import Chainable = Cypress.Chainable;

context('Toggle tumbler', () => {
  before(() => {
    cy.yandexLogin('admin');
  });

  const getCourierByLogin = (login: string): Chainable => {
    return cy
      .get(couriersSelector.search.container)
      .get(couriersSelector.search.lineName)
      .contains(login)
      .closest(couriersSelector.search.line);
  };

  const uncheckSMS = (): void => {
    cy.get(selectors.sidebar.menu.company)
      .click()
      .get(selectors.settings.company.inputs.clientSMSCheckbox)
      .uncheck()
      .get(selectors.settings.company.submit.common)
      .click();
  };
  const checkSMS = (): void => {
    cy.get(selectors.sidebar.menu.company)
      .click()
      .get(selectors.settings.company.inputs.clientSMSCheckbox)
      .check()
      .get(selectors.settings.company.submit.common)
      .click();
  };

  const TITLE = importRoutesKeyset.ru.tabTitles_vehicles;
  const ADD_NEW_COURIER = couriersKeyset.ru.title_modalChangeRouteCourier_addNew;
  const PHONE_INPUT_PLACEHOLDER = commonKeyset.ru.phone_placeholder;
  const TUMBLER_CHECKED_CLASS = 'tumbler_checked_yes';

  const TEST_COURIER_PHONE = '+79631230304';
  const TEST_COURIER_LOGIN = '77766122';
  const SMS_NOTIFICATION_TEXT = courierDetailsKeyset.ru.smsEnabledDescription;

  const couriersSelector = selectors.content.couriers;

  // @see https://testpalm.yandex-team.ru/courier/testcases/321
  it('should turn off auto sms toggle', () => {
    cy.get(selectors.sidebar.menu.settingsGroup).click().then(uncheckSMS);
  });

  it('should open couriers pages', () => {
    cy.get(selectors.sidebar.menu.monitoringGroup)
      .click()
      .get(selectors.sidebar.menu.couriers)
      .click()
      .then(() => {
        cy.get(couriersSelector.title).should('contain.text', TITLE);
        cy.get(couriersSelector.search.input).should('exist');
        cy.get(couriersSelector.search.line).should('exist');
        cy.get(couriersSelector.newCourier.title).should('contain.text', ADD_NEW_COURIER);
        cy.get(couriersSelector.newCourier.phone).should('exist');
        cy.get(couriersSelector.newCourier.number).should('exist');
        cy.get(couriersSelector.newCourier.submitButton).should('be.disabled');
      });
  });

  it('should add courier', () => {
    cy.get(couriersSelector.newCourier.phone)
      .type(TEST_COURIER_PHONE)
      .should('contain.value', TEST_COURIER_PHONE)
      .get(couriersSelector.newCourier.number)
      .type(TEST_COURIER_LOGIN)
      .should('contain.value', TEST_COURIER_LOGIN)
      .get(couriersSelector.newCourier.submitButton)
      .should('be.enabled')
      .click()
      .then(() => {
        cy.get(couriersSelector.newCourier.submitButton).should('be.disabled');
        cy.get(couriersSelector.newCourier.phone)
          .should('contain.value', '')
          .and('have.attr', 'placeholder', PHONE_INPUT_PLACEHOLDER);
        cy.get(couriersSelector.newCourier.number).should('contain.value', '');
        getCourierByLogin(TEST_COURIER_LOGIN).should('exist');
      });
  });

  it('should turn on tumbler', () => {
    getCourierByLogin(TEST_COURIER_LOGIN)
      .find(couriersSelector.search.tumbler)
      .as('tumbler')
      .click()
      .get('@tumbler')
      .should('have.class', TUMBLER_CHECKED_CLASS);
  });

  // @see https://testpalm.yandex-team.ru/courier/testcases/327
  it('open courier with turned on tumbler', () => {
    getCourierByLogin(TEST_COURIER_LOGIN).find(couriersSelector.search.lineLink).click();

    cy.get(couriersSelector.singleCourier.courierDetails.loginValue).should(
      'have.text',
      TEST_COURIER_LOGIN,
    );
    cy.get(couriersSelector.singleCourier.courierDetails.phoneValue).should(
      'have.text',
      TEST_COURIER_PHONE,
    );
    cy.get(couriersSelector.singleCourier.courierName).should('have.text', TEST_COURIER_LOGIN);
    cy.get(couriersSelector.singleCourier.routesList.emptyRoute).should('be.visible');
    cy.get(couriersSelector.singleCourier.courierDetails.smsTumbler.on).should('be.visible');
    cy.get(couriersSelector.singleCourier.courierDetails.info)
      .should('be.visible')
      .should('have.text', SMS_NOTIFICATION_TEXT);
  });

  it('turn off tumbler', () => {
    cy.get(couriersSelector.singleCourier.courierDetails.smsTumbler.on).click();

    cy.get(couriersSelector.singleCourier.courierDetails.smsTumbler.off).should('be.visible');
    cy.get(couriersSelector.singleCourier.courierDetails.info).should('not.exist');
  });

  it('turn off tumbler', () => {
    cy.get(couriersSelector.singleCourier.courierDetails.smsTumbler.off).click();

    cy.get(couriersSelector.singleCourier.courierDetails.smsTumbler.on).should('be.visible');
    cy.get(couriersSelector.singleCourier.courierDetails.info).should('be.visible');
  });

  it('back to couriers list', () => {
    cy.get(selectors.sidebar.menu.monitoringGroup).get(selectors.sidebar.menu.couriers).click();
  });

  it('turn off tumbler', () => {
    getCourierByLogin(TEST_COURIER_LOGIN)
      .find(couriersSelector.search.tumbler)
      .as('tumbler')
      .click()
      .get('@tumbler')
      .should('not.have.class', TUMBLER_CHECKED_CLASS);
  });

  after(() => {
    getCourierByLogin(TEST_COURIER_LOGIN)
      .find(couriersSelector.search.lineRemove)
      .click()
      .get(couriersSelector.removeCourier.submitButton)
      .click()
      .then(checkSMS);
  });
});
