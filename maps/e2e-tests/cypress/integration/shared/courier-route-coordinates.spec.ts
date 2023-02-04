import map from 'lodash/map';
import times from 'lodash/times';
import { courierNumberRecord } from '../../../src/constants/couriers';
import { routeNumberRecord } from '../../../src/constants/routes';
import { nanoid } from 'nanoid';

import subSeconds from 'date-fns/subSeconds';
import formatISO from 'date-fns/formatISO';

const depotLat = 59.958606;
const depotLon = 30.405313;
export const POSITIONS_LENGTH = 3;

export const data = map(times(POSITIONS_LENGTH), i => {
  const time = subSeconds(new Date(), POSITIONS_LENGTH - i);
  return {
    id: nanoid(),
    lat: depotLat + 0.01 * (i + 1),
    lon: depotLon + 0.01 * (i + 1),
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

export const stubCourierRouteCoordinates = (): void => {
  beforeEach(() => {
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
};
