import selectors from '../../../src/constants/selectors';
import { courierNameRecord, courierNumberRecord } from '../../../src/constants/couriers';
import { routeNumberRecord } from '../../../src/constants/routes';

import { nanoid } from 'nanoid';
import map from 'lodash/map';
import times from 'lodash/times';
import subSeconds from 'date-fns/subSeconds';
import formatISO from 'date-fns/formatISO';
import { expect } from 'chai';

const depotLat = 59.958606;
const depotLon = 30.405313;

const POSITIONS_LENGTH = 2;

const DIFF_THRESHOLD = 2;

const getDiff = (a: number, b: number): number => {
  return Math.abs(a - b);
};

const data = map(times(POSITIONS_LENGTH), i => {
  const time = subSeconds(new Date(), POSITIONS_LENGTH - i);
  return {
    id: nanoid(),
    lat: depotLat - 0.01 * (i + 1),
    lon: depotLon - 0.01 * (i + 1),
    route_id: routeNumberRecord.TODAY,
    courier_id: courierNumberRecord.gumba,
    server_time: +time,
    server_time_iso: formatISO(time),
    time_iso: formatISO(time),
    imei: null,
    imei_str: null,
    accuracy: 100.0,
  };
});

const moveHandle = (x: number, y: number): void => {
  cy.get(selectors.content.courier.route.map.common.sliderHandle)
    .trigger('mousedown', { button: 0, scrollBehavior: false })
    .trigger('mousemove', { pageX: x, pageY: y, scrollBehavior: false })
    .trigger('mouseup', { scrollBehavior: false });
};

const courierShouldBeCentered = (): void => {
  cy.get(selectors.content.courier.route.map.yandex.courierPosition)
    .closest(selectors.content.courier.route.map.yandex.placemark)
    .then(([$placemark]) => {
      const { left, top } = $placemark.getBoundingClientRect();
      getOffsetsInsideMap(left, top).then(sizes => {
        expect(getDiff(sizes.left, sizes.right)).lte(DIFF_THRESHOLD);
        expect(getDiff(sizes.top, sizes.bottom)).lte(DIFF_THRESHOLD);
      });
    });
};

const courierShouldBeNotCentered = (): void => {
  cy.get(selectors.content.courier.route.map.yandex.courierPosition)
    .should('be.visible')
    .closest(selectors.content.courier.route.map.yandex.placemark)
    .then(([$placemark]) => {
      const { left, top } = $placemark.getBoundingClientRect();
      getOffsetsInsideMap(left, top).then(sizes => {
        expect(getDiff(sizes.left, sizes.right)).gt(DIFF_THRESHOLD);
        expect(getDiff(sizes.top, sizes.bottom)).gt(DIFF_THRESHOLD);
      });
    });
};

const moveMapViewRight = (diff: number): void => {
  cy.get(selectors.content.courier.route.map.yandex.eventsLayer).then(([$map]) => {
    const sizes = $map.getBoundingClientRect();
    const gap = 10;
    const startX = sizes.left + gap;

    cy.get(selectors.content.courier.route.map.yandex.eventsLayer)
      .trigger('mousemove', {
        pageX: startX,
        scrollBehavior: false,
      })
      .trigger('mousedown', {
        button: 0,
        scrollBehavior: false,
      })
      .trigger('mousemove', {
        pageX: startX + diff,
      })
      .trigger('mouseup', { scrollBehavior: false });
  });
};

const getOffsetsInsideMap = (
  left: number,
  top: number,
): Cypress.Chainable<{ top: number; right: number; bottom: number; left: number }> => {
  return cy.get(selectors.content.courier.route.map.yandex.eventsLayer).then(([$map]) => {
    const mapSizes = $map.getBoundingClientRect();
    return {
      top: top - mapSizes.top,
      bottom: mapSizes.bottom - top,
      left: left - mapSizes.left,
      right: mapSizes.right - left,
    };
  });
};

// @see https://testpalm.yandex-team.ru/courier/testcases/342
context('Courier page', () => {
  before(() => {
    cy.preserveCookies();
    cy.yandexLogin('manager');
    cy.get(selectors.sidebar.menu.monitoringGroup).click();
    cy.get(selectors.sidebar.menu.dashboard).click();
    cy.get(selectors.content.dashboard.couriers.table.courierNames)
      .contains(courierNameRecord.gumba)
      .click();
  });

  before(() => {
    cy.intercept(
      {
        pathname: /.*companies\/\d+\/courier-position\/\d+\/routes\/\d+/,
        times: 1,
      },
      {
        body: data,
      },
    );
  });

  context('Courier on map', () => {
    before(() => {
      cy.wait(1000);
      cy.get(selectors.content.courier.route.map.yandex.scaleLine)
        .invoke('text')
        .as('scale')
        .as('initialScale');
    });

    before(() => {
      cy.get(selectors.content.courier.route.map.common.slider).then($handle => {
        const { left, width, top } = $handle[0].getBoundingClientRect();
        cy.wrap(left).as('handleStart');
        cy.wrap(left + width).as('handleEnd');
        cy.wrap(top).as('handleOffsetTop');
      });
    });

    it('should be not visible after map view shift', function () {
      moveMapViewRight(3000);
      cy.get(selectors.content.courier.route.map.yandex.courierPosition).should('not.be.visible');
    });

    it('should be visible and centered after "show on map" button click', function () {
      cy.get(selectors.content.courier.route.map.common.courierButton).click({
        scrollBehavior: false,
      });
      cy.wait(300);
      cy.get(selectors.content.courier.route.map.yandex.scaleLine).should('have.text', this.scale);

      courierShouldBeCentered();
    });

    it('should have new position after time slider move', function () {
      moveHandle(this.handleStart, this.handleOffsetTop);
      cy.wait(300);
      courierShouldBeNotCentered();
    });

    it('should be visible and centered after "show on map" button click', function () {
      cy.get(selectors.content.courier.route.map.common.courierButton).click({
        scrollBehavior: false,
      });
      cy.wait(300);
      cy.get(selectors.content.courier.route.map.yandex.scaleLine).should('have.text', this.scale);
      courierShouldBeCentered();
    });

    it('should be not visible after map view shift and "zoom plus" button click', function () {
      moveMapViewRight(3000);
      cy.get(selectors.content.courier.route.map.yandex.courierPosition).should('not.be.visible');
      cy.wait(300);
      cy.get(selectors.content.courier.route.map.yandex.zoomPlus).trigger('click');
      cy.get(selectors.content.courier.route.map.yandex.scaleLine)
        .invoke('text')
        .should('not.equal', this.scale);
      cy.get(selectors.content.courier.route.map.yandex.scaleLine).invoke('text').as('scale');
    });

    it('should be visible and centered after "show on map" button click', function () {
      cy.get(selectors.content.courier.route.map.common.courierButton).click({
        scrollBehavior: false,
      });
      cy.wait(300);
      cy.get(selectors.content.courier.route.map.yandex.scaleLine).should('have.text', this.scale);
      courierShouldBeCentered();
    });

    it('should have new position and initial zoom after map view shift and "zoom minus" button click', function () {
      moveMapViewRight(200);
      cy.wait(300);
      cy.get(selectors.content.courier.route.map.yandex.zoomMinus).trigger('click');
      cy.get(selectors.content.courier.route.map.yandex.scaleLine).should(
        'have.text',
        this.initialScale,
      );
      cy.get(selectors.content.courier.route.map.yandex.courierPosition).should('be.visible');
    });

    it('should be visible and centered after "show on map" button click', function () {
      cy.get(selectors.content.courier.route.map.common.courierButton).click({
        scrollBehavior: false,
      });
      cy.wait(300);
      courierShouldBeCentered();
    });
  });
});
