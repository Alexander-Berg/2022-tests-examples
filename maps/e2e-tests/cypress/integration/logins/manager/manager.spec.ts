import * as depotSettingsKeyset from '../../../../../src/translations/depot-settings';
import selectors from '../../../../src/constants/selectors';

import { depotNameRecord } from '../../../../src/constants/depots';
import { some } from 'lodash';

context('Manager', function () {
  beforeEach(function () {
    cy.preserveCookies();
  });

  before(function () {
    cy.yandexLogin('manager');
    cy.get(selectors.modal.paranja).should('exist');
    cy.get(selectors.modal.paranja).should('not.exist');
  });

  it('Company settings is not exist', function () {
    cy.get(selectors.sidebar.menu.settingsGroup).click();
    cy.get(selectors.sidebar.menu.company).should('not.exist');
  });

  it('Cant create company', function () {
    cy.get(selectors.sidebar.companySelector.control).click();
    cy.get(selectors.sidebar.companySelector.orgs.dropdown).should('exist');
    cy.get(selectors.superUserRights.addCompanyButton).should('not.exist');
  });

  it('Can see list of companies', function () {
    cy.get(selectors.sidebar.companySelector.orgs.dropdownItems.notice).should('exist');
  });

  describe('Depots', function () {
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
  });

  after(function () {
    cy.clearCookies();
  });
});
