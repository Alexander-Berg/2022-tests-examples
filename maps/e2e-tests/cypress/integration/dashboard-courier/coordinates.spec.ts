import * as courierMapKeyset from '../../../../src/translations/courier-map';
import * as companySettingsKeyset from '../../../../src/translations/company-settings';
import * as activeCourierKeyset from '../../../../src/translations/active-courier';
import * as courierDetailsKeyset from '../../../../src/translations/courier-details';
import selectors from '../../../src/constants/selectors';
import { courierNameRecord } from '../../../src/constants/couriers';
import forEach from 'lodash/forEach';

const courierDetailsLabels = {
  login: {
    ru: courierDetailsKeyset.ru.login,
    com: courierDetailsKeyset.en.login,
    'com.tr': courierDetailsKeyset.tr.login,
    es: courierDetailsKeyset.esLa.login,
  },
  phone: {
    ru: activeCourierKeyset.ru.phone,
    'com.tr': activeCourierKeyset.tr.phone,
    com: activeCourierKeyset.en.phone,
    es: activeCourierKeyset.esLa.phone,
  },
  smsTumbler: {
    ru: companySettingsKeyset.ru.sms_label,
    'com.tr': companySettingsKeyset.tr.sms_label,
    com: courierDetailsKeyset.en.sendSmsToClients,
    es: courierDetailsKeyset.esLa.sendSmsToClients,
  },
};

const tooltipText = {
  ru: courierMapKeyset.ru.pointCoords,
  'com.tr': courierMapKeyset.tr.pointCoords,
  com: courierMapKeyset.en.pointCoords,
  es: courierMapKeyset.esLa.pointCoords,
};

const routeMapDetailsLabels = {
  builtTheRoute: {
    ru: courierMapKeyset.ru.routeBuiltAt,
    'com.tr': courierMapKeyset.tr.routeBuiltAt,
    com: courierMapKeyset.en.routeBuiltAt,
    es: courierMapKeyset.esLa.routeBuiltAt,
  },
  sentCoordinates: {
    ru: courierMapKeyset.ru.coordsSentAt,
    'com.tr': courierMapKeyset.tr.coordsSentAt,
    com: courierMapKeyset.en.coordsSentAt,
    es: courierMapKeyset.esLa.coordsSentAt,
  },
  timeZone: {
    ru: courierMapKeyset.ru.localTimeZone,
    'com.tr': courierMapKeyset.tr.localTimeZone,
    com: courierMapKeyset.en.localTimeZone,
    es: courierMapKeyset.esLa.localTimeZone,
  },
};

const getOffsetsInsideMap = (
  left: number,
  top: number,
): Cypress.Chainable<{ top: number; right: number; bottom: number; left: number }> => {
  return cy.get(selectors.content.courier.route.map.common.container).then(([$map]) => {
    const mapSizes = $map.getBoundingClientRect();
    return {
      top: top - mapSizes.top,
      bottom: mapSizes.bottom - top,
      left: left - mapSizes.left,
      right: mapSizes.right - left,
    };
  });
};

const shouldTooltipBeOpenedOnPosition = (
  clickPosition: { left: number; top: number },
  domain: SupportedTld,
  key: SupportedTld | 'es',
): void => {
  if (domain === 'ru') {
    cy.get(selectors.content.courier.route.map.yandex.tooltip)
      .should('be.visible')
      .and('have.length', 1);
    cy.get(selectors.content.courier.route.map.yandex.tooltip)
      .invoke('text')
      .then(text => {
        const valueMatches = text.match(/\d{1,2}\.\d{5}/g);
        expect(text).include(tooltipText[key]);
        expect(valueMatches).to.be.instanceOf(Array);
        expect(valueMatches).have.length(2);
      });
    cy.get(selectors.content.courier.route.map.yandex.tooltipPoint).then(([$element]) => {
      const { left, top } = $element.getBoundingClientRect();
      getOffsetsInsideMap(left, top).then(({ left, top }) => {
        expect(getDiff(left, clickPosition.left)).lte(DIFF_THESHOLD);
        expect(getDiff(top, clickPosition.top)).lte(DIFF_THESHOLD);
      });
    });
  } else {
    cy.get(selectors.content.courier.route.map.google.tooltip)
      .should('be.visible')
      .and('have.length', 1);
    cy.get(selectors.content.courier.route.map.google.tooltip)
      .invoke('text')
      .then(text => {
        const valueMatches = text.match(/\d{1,2}\.\d{5}/g);
        expect(text).include(tooltipText[key]);
        expect(valueMatches).to.be.instanceOf(Array);
        expect(valueMatches).have.length(2);
      });
    cy.get(selectors.content.courier.route.map.google.tooltipPoint).then(([$element]) => {
      const { left, top } = $element.getBoundingClientRect();
      getOffsetsInsideMap(left, top).then(({ left, top }) => {
        expect(getDiff(left, clickPosition.left)).lte(DIFF_THESHOLD);
        expect(getDiff(top, clickPosition.top)).lte(DIFF_THESHOLD);
      });
    });
  }
};

const DIFF_THESHOLD = 2;

const getDiff = (a: number, b: number): number => {
  return Math.abs(a - b);
};

// at the moment, it is enough to check the 'ru' and 'com' domains (ymaps and google maps)
const TLDS = ['ru', 'com'] as const;

// @see https://testpalm.yandex-team.ru/courier/testcases/356

forEach(TLDS, tld => {
  describe(`Check tooltips (domain ${tld})`, function () {
    before(() => {
      cy.preserveCookies();
      cy.yandexLogin('manager', { tld });
      cy.get(selectors.sidebar.menu.monitoringGroup).click();
      cy.get(selectors.sidebar.menu.dashboard).click();
      cy.get(selectors.content.dashboard.couriers.table.courierNames)
        .contains(courierNameRecord.gumba)
        .click();
    });

    it('should display courier info on initial render', () => {
      cy.get(selectors.content.courier.name).should('have.text', courierNameRecord.gumba);
      cy.get(selectors.content.courier.date.root).should('be.visible');
      cy.get(selectors.content.courier.details.root).as('courierDetails').should('be.visible');
      cy.get('@courierDetails')
        .find(selectors.content.courier.details.label)
        .eq(0)
        .should('have.text', courierDetailsLabels.login[tld]);
      cy.get('@courierDetails')
        .find(selectors.content.courier.details.value)
        .eq(0)
        .should('not.be.empty');
      cy.get('@courierDetails')
        .find(selectors.content.courier.details.label)
        .eq(1)
        .should('have.text', courierDetailsLabels.phone[tld]);
      cy.get('@courierDetails')
        .find(selectors.content.courier.details.value)
        .eq(1)
        .should('not.be.empty');
      cy.get('@courierDetails')
        .find(selectors.content.courier.details.label)
        .eq(2)
        .should('have.text', courierDetailsLabels.smsTumbler[tld]);
      cy.get('@courierDetails')
        .find(selectors.content.courier.details.value)
        .eq(2)
        .find(selectors.content.courier.details.smsTumbler)
        .should('be.visible');
    });

    it('should display map parts and time slider on initial render', () => {
      if (tld === 'ru') {
        cy.get(selectors.content.courier.route.map.yandex.rulerIcon).should('be.visible');
        cy.get(selectors.content.courier.route.map.yandex.zoomPlus).should('be.visible');
        cy.get(selectors.content.courier.route.map.yandex.zoomMinus).should('be.visible');
        cy.get(selectors.content.courier.route.map.yandex.trafficButton).should('be.visible');
      } else {
        cy.get(selectors.content.courier.route.map.google.zoomPlus('eng')).should('be.visible');
        cy.get(selectors.content.courier.route.map.google.zoomMinus('eng')).should('be.visible');
      }

      cy.get(selectors.content.courier.route.map.common.time).should('be.visible');
      cy.get(selectors.content.courier.route.map.common.resizer).should('be.visible');
      cy.get(selectors.content.courier.route.map.common.details.root)
        .as('mapRouteDetails')
        .should('be.visible');
      cy.get('@mapRouteDetails')
        .find(selectors.content.courier.route.map.common.details.label)
        .eq(0)
        .should('have.text', routeMapDetailsLabels.builtTheRoute[tld]);
      cy.get('@mapRouteDetails')
        .find(selectors.content.courier.route.map.common.details.label)
        .eq(1)
        .should('have.text', routeMapDetailsLabels.sentCoordinates[tld]);
      cy.get('@mapRouteDetails')
        .find(selectors.content.courier.route.map.common.details.label)
        .eq(2)
        .should('have.text', routeMapDetailsLabels.timeZone[tld]);
      cy.get('@mapRouteDetails')
        .find(selectors.content.courier.route.map.common.details.value)
        .eq(2)
        .eq(2)
        .should('not.be.empty');

      cy.get(selectors.content.courier.route.selector.root).should('be.visible');
      cy.get(selectors.content.courier.route.table).should('be.visible');
    });

    context('Coordinates tooltip', () => {
      it('should match click position and contain coordinates data', function () {
        const clickPosition = {
          left: 600,
          top: 300,
        };

        cy.get(selectors.content.courier.route.map.common.container).rightclick(
          clickPosition.left,
          clickPosition.top,
          { scrollBehavior: false },
        );

        shouldTooltipBeOpenedOnPosition(clickPosition, tld, tld);
      });

      it('should be closed on new right click and be opened at new place', function () {
        const clickPosition = {
          left: 800,
          top: 200,
        };

        cy.get(selectors.content.courier.route.map.common.container).rightclick(
          clickPosition.left,
          clickPosition.top,
          { scrollBehavior: false },
        );

        shouldTooltipBeOpenedOnPosition(clickPosition, tld, tld);
      });

      it('should not exist on close', function () {
        if (tld === 'ru') {
          cy.get(selectors.content.courier.route.map.yandex.tooltipClose).click();
          cy.get(selectors.content.courier.route.map.yandex.tooltip).should('not.exist');
        } else {
          cy.get(selectors.content.courier.route.map.google.tooltipClose('eng')).click();
          cy.get(selectors.content.courier.route.map.google.tooltip).should('not.exist');
        }
      });

      after(() => {
        cy.clearCookies();
      });
    });
  });
});
