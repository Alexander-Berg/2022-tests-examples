const fetch = require('node-fetch');
const https = require('https');

const config = require('./config');

export const sendPosition = async (courierId, routeId, currentLocation) => {
  const agent = new https.Agent({
    rejectUnauthorized: false,
  });

  const body = JSON.stringify({
    positions: [
      {
        accuracy: 1,
        time: Date.now(),
        ...currentLocation,
      },
    ],
  });

  const response = await fetch(
    `${config.endpoint}/api/v1/couriers/${courierId}/routes/${routeId}/push-positions`,
    {
      headers: {
        Authorization: `Auth ${config.appUserToken}`,
        'User-Agent': `com.yandex.courier vTestApi`,
        'Content-type': 'application/json',
      },
      method: 'POST',
      body,
      agent,
    },
  );

  return response.status;
};

export const sendPositionV2 = async (courierId, routeId, currentLocation) => {
  const agent = new https.Agent({
    rejectUnauthorized: false,
  });

  const body = JSON.stringify({
    positions: [
      {
        is_moving: true,
        uuid: 'd52026c8-6495-4257-812b-e9a23ee50657',
        // 'timestamp': '2019-10-02T12:12:12.929Z',
        timestampMeta: {
          time: Date.now(),
          systemTime: Date.now(),
          systemClockElaspsedRealtime: 704359583,
          elapsedRealtime: 704359340,
        },
        odometer: 384,
        coords: {
          ...currentLocation,
          accuracy: 1,
          speed: 0.98,
          heading: 107.39,
          altitude: 162.4,
        },
        activity: {
          type: 'still',
          confidence: 100,
        },
        battery: {
          is_charging: true,
          level: 0.5,
        },
        extras: {},
      },
    ],
  });

  const response = await fetch(
    `${config.endpoint}/api/v1/couriers/${courierId}/routes/${routeId}/push-positions-v2`,
    {
      headers: {
        Authorization: `Auth ${config.appUserToken}`,
        'User-Agent': `com.yandex.courier vTestApi`,
        'Content-type': 'application/json',
      },
      method: 'POST',
      body,
      agent,
    },
  );

  return response.status;
};
