import * as couriersKeyset from '../../../../src/translations/couriers';
import * as courierRouteKeyset from '../../../../src/translations/courier-route';
import selectors from '../../../src/constants/selectors';
import { courierNameRecord } from '../../../src/constants/couriers';
import forEach from 'lodash/forEach';

const changeRouteDropdown = {
  button: {
    ru: courierRouteKeyset.ru.changeCourierDropdown_title,
    'com.tr': courierRouteKeyset.tr.changeCourierDropdown_title,
  },
  options: {
    changeCourierOnTracker: {
      ru: courierRouteKeyset.ru.changeCourierDropdown_tracker,
      'com.tr': courierRouteKeyset.tr.changeCourierDropdown_tracker,
    },
  },
};

const link = {
  href: {
    ru: couriersKeyset.ru.title_modalChangeRouteTracker_helpLink,
    'com.tr': couriersKeyset.en.title_modalChangeRouteTracker_helpLink,
  },
  text: {
    'com.tr': couriersKeyset.tr.title_trackerNumber_hint,
    ru: couriersKeyset.ru.title_trackerNumber_hint,
  },
};

// @see https://testpalm.yandex-team.ru/testcase/courier-589
const testCreator = (domain: Extract<SupportedTld, 'ru' | 'com.tr'>): ReturnType<typeof context> =>
  context(`Move on track dialog (${domain})`, () => {
    before(() => {
      cy.preserveCookies();
      cy.yandexLogin('manager', { tld: domain });
      cy.get(selectors.sidebar.menu.monitoringGroup).click();
      cy.get(selectors.sidebar.menu.dashboard).click();
    });

    it('should be reachable from dashboard', function () {
      cy.get(selectors.content.dashboard.couriers.table.courierNames)
        .contains(courierNameRecord.gumba)
        .click();
      cy.get(selectors.content.courier.route.changeRouteDropdown.button)
        .should('have.text', changeRouteDropdown.button[domain])
        .should('be.visible');
      cy.get(selectors.content.courier.route.table).should('be.visible');
      cy.get(selectors.content.courier.route.map.common.container).should('be.visible');

      cy.get(selectors.content.courier.route.changeRouteDropdown.button).click();
      cy.get(selectors.content.courier.route.changeRouteDropdown.options.changeCourierOnTracker)
        .should('have.text', changeRouteDropdown.options.changeCourierOnTracker[domain])
        .should('be.visible')
        .click();

      cy.get(selectors.modal.moveRoute.moveRouteOnTracker.dialog).should('be.visible');
    });

    it('should contain help link', function () {
      cy.get(selectors.modal.moveRoute.moveRouteOnTracker.dialog).as('dialog');
      cy.get('@dialog')
        .find(selectors.modal.moveRoute.helpLink)
        .should('be.visible')
        .and('have.text', link.text[domain])
        .and('have.attr', 'target', '_blank')
        .and('have.attr', 'href', link.href[domain]);
    });
  });

forEach(['ru', 'com.tr'] as const, domain => {
  testCreator(domain);
});
