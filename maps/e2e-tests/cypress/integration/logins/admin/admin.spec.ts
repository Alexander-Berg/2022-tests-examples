import * as depotSettingsKeyset from '../../../../../src/translations/depot-settings';
import * as depotsKeyset from '../../../../../src/translations/depots';
import * as companySettingsKeyset from '../../../../../src/translations/company-settings';
import selectors from '../../../../src/constants/selectors';

import { depotNameRecord } from '../../../../src/constants/depots';
import { forEach, size, some, times, toString } from 'lodash';

// @see https://testpalm.yandex-team.ru/testcase/courier-111
context('Admin', function () {
  before(function () {
    cy.yandexLogin('admin');
    cy.get(selectors.modal.paranja).should('exist');
    cy.get(selectors.modal.paranja).should('not.exist');
  });

  // @see https://testpalm.yandex-team.ru/testcase/courier-72
  it('Go to the company settings', function () {
    cy.fixture('testData').then(() => {
      cy.get(selectors.content.dashboard.view);
      cy.get(selectors.content.dashboard.couriers.table.todayRow).click();
      cy.get(selectors.content.couriers.singleCourier.courierDetails.settingsLink).click();
      cy.get(selectors.content.settings.view);
    });
  });

  describe('Company select', function () {
    it('Cant create company', function () {
      cy.get(selectors.sidebar.companySelector.control).click();
      cy.get(selectors.sidebar.companySelector.orgs.dropdown).should('not.be.visible');
      cy.get(selectors.superUserRights.addCompanyButton).should('not.exist');
    });

    it('Can see list of companies', function () {
      cy.get(selectors.sidebar.companySelector.orgs.dropdownItems.notice).should('exist');
    });

    after(function () {
      cy.get(selectors.sidebar.companySelector.control).click();
    });
  });

  describe('Company settings', function () {
    before(function () {
      cy.get(selectors.sidebar.menu.settingsGroup).click();
      cy.get(selectors.sidebar.menu.company).click();
    });

    it('Company settings is exist', function () {
      cy.get(selectors.sidebar.menu.settingsGroup).click();
      cy.get(selectors.sidebar.menu.company).should('exist');
    });

    it('Have field "apikey"', function () {
      cy.get(selectors.settings.labels.apikey).should('contain', companySettingsKeyset.ru.apiKey);
    });

    it('Cant see available services', function () {
      cy.get(selectors.settings.sections.availableServices).should('not.exist');
    });

    after(function () {
      cy.get(selectors.sidebar.menu.settingsGroup).click();
    });
  });

  // https://testpalm.yandex-team.ru/testcase/courier-475
  describe('presettled_reasons', () => {
    it('Have field "presettled_reasons"', function () {
      cy.get(selectors.settings.labels.presettledReasons).should('exist');
    });

    it('Can save value "presettled_reasons"', function () {
      const presettledReasons = ['someData', 'someData2']
        .map(row => `${row}${Math.random()}`)
        .join(', ');

      cy.get(selectors.settings.values.presettledReasons).clear().type(presettledReasons);
      cy.get(selectors.settings.company.submit.common).click();
      cy.get(selectors.settings.company.chips.common).should('exist');
      cy.get(selectors.settings.values.presettledReasons).should('have.value', presettledReasons);
      cy.get(selectors.settings.company.submit.common).should('have.attr', 'disabled', 'disabled');
    });

    it('Can save new big value "presettled_reasons"', function () {
      const templateReason = toString(Math.random());
      const templateReasonSize = size(templateReason);
      const maxFieldSize = 1024;
      const maxReasonsCount = maxFieldSize / templateReasonSize;

      const presettledReasons = times(maxReasonsCount, () => Math.random())
        .join(', ')
        .slice(0, maxFieldSize);

      cy.get(selectors.settings.values.presettledReasons)
        .clear()
        .type(presettledReasons, { delay: 0.1 });
      cy.get(selectors.settings.company.submit.common).click();
      cy.get(selectors.settings.company.chips.common).should('exist');
      cy.get(selectors.settings.values.presettledReasons).should('have.value', presettledReasons);
      cy.get(selectors.settings.company.submit.common).should('have.attr', 'disabled', 'disabled');
    });

    it('Clear "presettled_reasons"', function () {
      cy.get(selectors.settings.values.presettledReasons).clear();
      cy.get(selectors.settings.company.submit.common).click();
      cy.get(selectors.settings.company.chips.common).should('exist');
      cy.get(selectors.settings.values.presettledReasons).should('have.value', '');
      cy.get(selectors.settings.company.submit.common).should('have.attr', 'disabled', 'disabled');
    });
  });

  describe('Depots', function () {
    before(function () {
      cy.get(selectors.sidebar.companySelector.control).click();
      cy.get(selectors.sidebar.companySelector.panel);
    });

    it('Can see depots dropdown', function () {
      cy.get(selectors.managerRights.depotDropdown).should('exist');
    });

    it('Option "All depots" is exist"', function () {
      cy.get(selectors.sidebar.companySelector.depots.dropdownItems.allDepots).should('exist');
    });

    it('Valid list of depots', function () {
      const depotsSet = new Set([
        depotSettingsKeyset.ru.allDepots,
        depotNameRecord.castlePeach,
        depotNameRecord.yoshisHouse,
        depotNameRecord.toadsTent,
        depotNameRecord.englishPub,
        depotNameRecord.fourwordsallcapslock,
        depotNameRecord.ONEWORDCAPSLOCK,
        depotNameRecord.rocketjump5G,
        depotNameRecord.some,
        depotNameRecord.some2,
        depotNameRecord.additional1,
        depotNameRecord.additional2,
        depotNameRecord.additional3,
        depotNameRecord.additional4,
      ]);

      cy.get(selectors.sidebar.companySelector.depots.dropdownItems.anyDepot).then($items => {
        const sidebarDepots = $items.children();
        depotsSet.forEach(depotName => {
          const hasDepotInSidebar = some(sidebarDepots, child => depotName === child.textContent);

          if (!hasDepotInSidebar) {
            throw new Error('cant find depot');
          }
        });
      });
    });

    after(function () {
      cy.get(selectors.sidebar.companySelector.control).click();
    });
  });
});
