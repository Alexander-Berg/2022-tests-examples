import * as courierMapKeyset from '../../../../src/translations/courier-map';
import * as companySettingsKeyset from '../../../../src/translations/company-settings';
import * as activeCourierKeyset from '../../../../src/translations/active-courier';
import * as courierDetailsKeyset from '../../../../src/translations/courier-details';
import selectors from '../../../src/constants/selectors';
import { courierNameRecord } from '../../../src/constants/couriers';

import dateFnsFormat from '../../../src/utils/date-fns-format';
import { GeoObject } from 'yandex-maps';
import {
  stubCourierRouteCoordinates,
  data,
  POSITIONS_LENGTH,
} from '../shared/courier-route-coordinates.spec';

const courierDetailsLabels = {
  login: courierDetailsKeyset.ru.login,
  phone: activeCourierKeyset.ru.phone,
  smsTumbler: companySettingsKeyset.ru.sms_label,
};

type PositionsData = {
  x?: number;
  y?: number;
  timeText: string;
};

const MAX_DIFF = 0.00001;

const positionData: Array<PositionsData> = [
  {
    timeText: dateFnsFormat(data[0].time_iso, 'd MMMM HH:mm:ss'),
  },
  {
    timeText: dateFnsFormat(data[1].time_iso, 'd MMMM HH:mm:ss'),
  },
  {
    timeText: dateFnsFormat(data[2].time_iso, 'd MMMM HH:mm:ss'),
  },
];

const routeMapDetailsLabels = {
  builtTheRoute: courierMapKeyset.ru.routeBuiltAt,
  sentCoordinates: courierMapKeyset.ru.coordsSentAt,
  timeZone: courierMapKeyset.ru.localTimeZone,
};

const passedTrackColor = '#198cff';

const shouldMatchPosition = (expected: PositionsData): void => {
  cy.get(selectors.content.courier.route.map.yandex.courierPosition).then($icon => {
    cy.get(selectors.content.courier.route.map.common.time)
      .invoke('text')
      .then(timeText => {
        const sizes = $icon[0].getBoundingClientRect();
        const x = sizes.left;
        const y = sizes.top;

        expect({ x, y, timeText }).deep.equal(expected);
      });
  });
};

const moveHandle = (x: number, y: number): void => {
  cy.get(selectors.content.courier.route.map.common.sliderHandle)
    .trigger('mousedown', { button: 0, scrollBehavior: false })
    .trigger('mousemove', { pageX: x, pageY: y, scrollBehavior: false })
    .trigger('mouseup', { scrollBehavior: false })
    .then(([$handle]) => {
      const { left, top, width, height } = $handle.getBoundingClientRect();
      expect(Math.abs(left - x)).lte(width);
      expect(Math.abs(top - y)).lte(height);
    });
};

const shouldMatchTrackColors = (
  color: string,
  expectedBounds: [{ lon: number; lat: number }, { lon: number; lat: number }],
): void => {
  cy.window().then(win => {
    win.map.geoObjects.each((object: GeoObject) => {
      if (object.constructor.name === 'Polyline') {
        const bounds = object.geometry?.getBounds();
        if (!bounds) {
          return;
        }
        if ((object.options.get('strokeColor', {}) as any) === color) {
          expect(Math.abs(expectedBounds[0].lat - bounds[0][0])).lte(MAX_DIFF);
          expect(Math.abs(expectedBounds[0].lon - bounds[0][1])).lte(MAX_DIFF);
          expect(Math.abs(expectedBounds[1].lat - bounds[1][0])).lte(MAX_DIFF);
          expect(Math.abs(expectedBounds[1].lon - bounds[1][1])).lte(MAX_DIFF);
        }
      }
    });
  });
};

context('Courier timeline slider', () => {
  before(() => {
    cy.preserveCookies();
    cy.yandexLogin('manager');
    cy.get(selectors.sidebar.menu.monitoringGroup).click();
    cy.get(selectors.sidebar.menu.dashboard).click();
    cy.get(selectors.content.dashboard.couriers.table.courierNames)
      .contains(courierNameRecord.gumba)
      .click();
  });

  stubCourierRouteCoordinates();

  beforeEach(() => {
    cy.get(selectors.content.courier.route.map.common.slider).then($handle => {
      const { left, width, top } = $handle[0].getBoundingClientRect();
      cy.wrap(left).as('handleStart');
      cy.wrap(left + width).as('handleEnd');
      cy.wrap(top).as('handleOffsetTop');
    });
  });

  it('should display map parts and time slider on initial render', () => {
    cy.get(selectors.content.courier.route.map.yandex.courierPosition)
      .should('be.visible')
      .then($icon => {
        const sizes = $icon[0].getBoundingClientRect();
        positionData[POSITIONS_LENGTH - 1].x = sizes.left;
        positionData[POSITIONS_LENGTH - 1].y = sizes.top;
      });
    cy.get(selectors.content.courier.route.map.yandex.activeOrder).should('be.visible');

    cy.get(selectors.content.courier.route.map.common.time).should(
      'have.text',
      positionData[POSITIONS_LENGTH - 1].timeText,
    );
  });

  it('should change courier position and time after move to start', function () {
    moveHandle(this.handleStart, this.handleOffsetTop);
    shouldMatchTrackColors(passedTrackColor, [data[0], data[0]]);
    cy.get(selectors.content.courier.route.map.yandex.courierPosition)
      .should('be.visible')
      .then($icon => {
        const sizes = $icon[0].getBoundingClientRect();
        positionData[0].x = sizes.left;
        positionData[0].y = sizes.top;

        expect(positionData[0].x).not.equal(positionData[POSITIONS_LENGTH - 1].x);
        expect(positionData[0].y).not.equal(positionData[POSITIONS_LENGTH - 1].y);
      });
    cy.get(selectors.content.courier.route.map.common.time).should(
      'have.text',
      positionData[0].timeText,
    );
  });

  it('should change courier position and time after move to middle position', function () {
    const middleX = this.handleStart + (this.handleEnd - this.handleStart) / 2;
    moveHandle(middleX, this.handleOffsetTop);
    shouldMatchTrackColors(passedTrackColor, [data[0], data[1]]);
    cy.get(selectors.content.courier.route.map.yandex.courierPosition)
      .should('be.visible')
      .then($icon => {
        const sizes = $icon[0].getBoundingClientRect();
        positionData[1].x = sizes.left;
        positionData[1].y = sizes.top;

        expect(positionData[1].x).not.equal(positionData[POSITIONS_LENGTH - 1].x);
        expect(positionData[1].y).not.equal(positionData[POSITIONS_LENGTH - 1].y);

        expect(positionData[1].x).not.equal(positionData[0].x);
        expect(positionData[1].y).not.equal(positionData[0].y);
      });
    cy.get(selectors.content.courier.route.map.common.time).should(
      'have.text',
      positionData[1].timeText,
    );
  });

  it('should change courier position and time after move to end', function () {
    moveHandle(this.handleEnd, this.handleOffsetTop);
    shouldMatchTrackColors(passedTrackColor, [data[0], data[POSITIONS_LENGTH - 1]]);
    shouldMatchPosition(positionData[POSITIONS_LENGTH - 1]);
  });

  it('should change courier position and time after prev button click', () => {
    cy.get(selectors.content.courier.route.map.common.prevCourierPositionButton).click({
      scrollBehavior: false,
    });
    shouldMatchTrackColors(passedTrackColor, [data[0], data[1]]);
    shouldMatchPosition(positionData[1]);
  });

  it('should change courier position and time after next button click', () => {
    cy.get(selectors.content.courier.route.map.common.nextCourierPositionButton).click({
      scrollBehavior: false,
    });
    shouldMatchTrackColors(passedTrackColor, [data[0], data[POSITIONS_LENGTH - 1]]);
    shouldMatchPosition(positionData[POSITIONS_LENGTH - 1]);
  });

  it('should highlight handle after click', () => {
    cy.get(selectors.content.courier.route.map.common.sliderHandle)
      .click({ scrollBehavior: false })
      .should('have.css', 'boxShadow', 'rgba(26, 140, 255, 0.4) 0px 0px 0px 6px');
  });

  it('should change courier position and time after left arrow press', () => {
    cy.get('body').type('{leftArrow}', { scrollBehavior: false });
    shouldMatchTrackColors(passedTrackColor, [data[0], data[1]]);
    shouldMatchPosition(positionData[1]);
  });

  it('should change courier position and time after right arrow press', () => {
    cy.get('body').type('{rightArrow}', { scrollBehavior: false });
    shouldMatchTrackColors(passedTrackColor, [data[0], data[POSITIONS_LENGTH - 1]]);
    shouldMatchPosition(positionData[POSITIONS_LENGTH - 1]);
  });
});
