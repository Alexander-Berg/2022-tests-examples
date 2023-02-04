import * as companySettingsKeyset from '../../../../../src/translations/company-settings';
import forEach from 'lodash/forEach';
import repeat from 'lodash/repeat';

import selectors from '../../../../src/constants/selectors';

const baseReasons =
  'Товар поврежден, Доставка отменена, Неправильный адрес, Неправильно укомплектован, Упаковка повреждена, Товар поврежден, Клиент не согласен';

const reasonsLabel = {
  ru: companySettingsKeyset.ru.presettled_reasons,
  'com.tr': companySettingsKeyset.tr.presettled_reasons,
};

forEach(['ru', 'com.tr'], (tld: 'ru' | 'com.tr') => {
  describe(`Settings: delivery cancel reasons (domain ${tld})`, function () {
    before(function () {
      cy.yandexLogin('admin', { tld });
    });

    it('Enter the page', function () {
      cy.get(selectors.sidebar.menu.settingsGroup).click();
      cy.get(selectors.sidebar.menu.company).click();

      cy.get(selectors.content.settings.view).should('exist');
      cy.get(selectors.settings.labels.presettledReasons).should('have.text', reasonsLabel[tld]);
    });

    it('Type reasons', function () {
      cy.get(selectors.settings.company.submit.common).should('be.disabled');
      cy.get(selectors.settings.values.presettledReasons).clear().type(baseReasons);

      cy.get(selectors.settings.values.presettledReasons).should('have.value', baseReasons);
      cy.get(selectors.settings.company.submit.common).should('not.be.disabled');
    });

    it('Submit the reasons', function () {
      cy.get(selectors.settings.company.submit.common).click();

      cy.get(selectors.settings.company.submit.common).should('be.disabled');
      cy.get(selectors.settings.values.presettledReasons).should('have.value', baseReasons);
    });

    it('Type a long value', function () {
      cy.get(selectors.settings.values.presettledReasons)
        .clear()
        .type(repeat(baseReasons + ', ', 10).slice(0, 1024));
      cy.get(selectors.settings.values.presettledReasons).should(
        'have.value',
        repeat(baseReasons + ', ', 10).slice(0, 1024),
      );
      cy.get(selectors.settings.company.submit.common).should('not.be.disabled');
    });

    it('Submit the long value', function () {
      cy.get(selectors.settings.company.submit.common).click();

      cy.get(selectors.settings.company.submit.common).should('be.disabled');
      cy.get(selectors.settings.values.presettledReasons).should(
        'have.value',
        repeat(baseReasons + ', ', 10).slice(0, 1024),
      );
    });

    it('Clear the reasons', function () {
      cy.get(selectors.settings.values.presettledReasons).clear();
      cy.get(selectors.settings.company.submit.common).should('not.be.disabled');
      cy.get(selectors.settings.company.submit.common).click();
      cy.get(selectors.settings.values.presettledReasons).should('have.value', '');
      cy.get(selectors.settings.company.submit.common).should('be.disabled');
    });
  });
});
