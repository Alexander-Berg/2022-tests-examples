import * as commonKeyset from '../../../../../src/translations/common';
import * as companySettingsKeyset from '../../../../../src/translations/company-settings';
import selectors from '../../../../src/constants/selectors';

const SMS_NEARBY_ORDER = companySettingsKeyset.ru.smsNearbyOrderEta;
const SMS_NEARBY_ORDER_HINT = companySettingsKeyset.ru.smsNearbyOrderEta_hint;
const DEFAULT_SMS_NEARBY_ORDER_TIME = 1800;
const SMS_ENABLED = 'SMS включены для всех курьеров в настройках компании';

context('Toggle tumbler', () => {
  before(() => {
    cy.yandexLogin('admin');

    cy.get(selectors.sidebar.menu.settingsGroup).click();
    cy.get(selectors.sidebar.menu.company).click();

    cy.get(selectors.settings.company.inputs.clientSMSCheckbox).then($el => {
      if ($el.attr('checked')) {
        cy.wrap($el).uncheck();
        cy.get(selectors.settings.company.submit.common).click();
      }
    });

    cy.get(selectors.sidebar.menu.monitoringGroup)
      .click()
      .get(selectors.sidebar.menu.couriers)
      .click();

    cy.get(selectors.content.couriers.search.lineSms).uncheck({ force: true });
  });

  // @see https://testpalm.yandex-team.ru/courier/testcases/351
  it('Enable SMS notifications in settings', () => {
    cy.get(selectors.sidebar.menu.company).click();

    cy.get(selectors.settings.company.inputs.clientSMSCheckbox).check();

    cy.get(selectors.settings.company.inputs.clientSMSCheckbox).should('be.checked');
    cy.get(selectors.settings.company.inputs.intervalSMS)
      .find(selectors.settings.company.inputs.label)
      .should('have.text', SMS_NEARBY_ORDER);
    cy.get(selectors.settings.company.inputs.intervalSMS)
      .find('input')
      .should('have.value', DEFAULT_SMS_NEARBY_ORDER_TIME);
    cy.get(selectors.settings.company.inputs.intervalSMS)
      .find(selectors.settings.company.inputs.hint)
      .should('have.text', SMS_NEARBY_ORDER_HINT);
    cy.get(selectors.settings.company.submit.sms).should('not.be.disabled');
  });

  it('Save enabled SMS notifications', () => {
    cy.get(selectors.settings.company.submit.sms).click();
    cy.get(selectors.settings.company.chips.sms)
      .should('be.visible')
      .should('have.text', companySettingsKeyset.ru.saved);
  });

  it('Courier SMS tumblers', () => {
    cy.get(selectors.sidebar.menu.couriers).click();

    cy.get(selectors.content.couriers.search.lineSms).each($el => {
      cy.wrap($el).should('be.checked');
    });
    cy.get(selectors.content.couriers.hint)
      .should('have.text', SMS_ENABLED)
      .find(commonKeyset.esLa.dateRangeFilter_to)
      .should('have.attr', 'href')
      .and('include', '/depots/all/settings');
  });

  it('Disable SMS notifications in settings', () => {
    cy.get(selectors.sidebar.menu.company).click();

    cy.get(selectors.settings.company.inputs.clientSMSCheckbox).uncheck();

    cy.get(selectors.settings.company.inputs.clientSMSCheckbox).should('not.be.checked');
    cy.get(selectors.settings.company.inputs.intervalSMS)
      .find(selectors.settings.company.inputs.label)
      .should('have.text', SMS_NEARBY_ORDER);
    cy.get(selectors.settings.company.inputs.intervalSMS)
      .find('input')
      .should('have.value', DEFAULT_SMS_NEARBY_ORDER_TIME);
    cy.get(selectors.settings.company.inputs.intervalSMS)
      .find(selectors.settings.company.inputs.hint)
      .should('have.text', SMS_NEARBY_ORDER_HINT);
    cy.get(selectors.settings.company.submit.sms).should('not.be.disabled');
  });

  it('Save disabled SMS notifications', () => {
    cy.get(selectors.settings.company.submit.sms).click();
    cy.get(selectors.settings.company.chips.sms)
      .should('be.visible')
      .should('have.text', companySettingsKeyset.ru.saved);
  });

  it('Courier SMS tumblers', () => {
    cy.get(selectors.sidebar.menu.couriers).click();

    cy.get(selectors.content.couriers.search.lineSms).each($el => {
      cy.wrap($el).should('not.be.checked');
    });
    cy.get(selectors.content.couriers.hint).should('not.exist');
  });
});
