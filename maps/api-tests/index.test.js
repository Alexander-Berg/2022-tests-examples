import moment from 'moment';
import shuffle from 'lodash/shuffle';

import config from './config';

import { sendPosition, sendPositionV2 } from './push-positions-request';
import generateRoute from './route-generation';
import { YandexApi } from '../app/lib/api/yandex/api';
import { StatusEnum } from '../app/modules/routes-details/models';

const checkIsEditableOrderStatus = orderStatus => {
  return [StatusEnum.new, StatusEnum.confirmed].includes(orderStatus);
};

// hot fix
jest.mock('../app/index', () => ({}));

jest.mock('mobile-auth-library-react-native', () => ({
  invalidateTokenAsync: () => {},
}));

const STATUSES = {
  NEW: 'new',
  POSTPONED: 'postponed',
  CONFIRMED: 'confirmed',
  FINISHED: 'finished',
  CANCELLED: 'cancelled',
};

let testData = {};

class YandexApiFactory {
  static createWithValidUserToken = () => {
    const yandexApi = YandexApi.createNewInstanceOnlyForTests();
    yandexApi.setApiVersionType(config.apiVersion);
    yandexApi.setToken(config.appUserToken);
    return yandexApi;
  };

  static createWithInvalidUserToken = () => {
    const yandexApi = YandexApi.createNewInstanceOnlyForTests();
    yandexApi.setApiVersionType(config.apiVersion);
    yandexApi.setToken(config.invalidAppUserToken);
    return yandexApi;
  };

  static createWithNotExistUserToken = () => {
    const yandexApi = YandexApi.createNewInstanceOnlyForTests();
    yandexApi.setApiVersionType(config.apiVersion);
    yandexApi.setToken(config.nonexistentToken);
    return yandexApi;
  };

  static createWithUnregisteredUserToken = () => {
    const yandexApi = YandexApi.createNewInstanceOnlyForTests();
    yandexApi.setApiVersionType(config.apiVersion);
    yandexApi.setToken(config.unregisteredToken);
    return yandexApi;
  };
}

const getOrders = async () => {
  const yandexApi = YandexApiFactory.createWithValidUserToken();

  const ordersResult = await yandexApi.fetchOrdersDetails(testData.company.id, testData.route.id);

  return ordersResult.data || [];
};

jest.setTimeout(60 * 1000);

it('API tests', async () => {
  try {
    let route = config.testData.route;
    if (!route) {
      console.info('Generate route');
      route = await generateRoute();
      console.info('Route successfully generated');
    } else {
      console.info('Use existing route');
    }

    testData = {
      ...config.testData,
      route,
    };

    console.log('testData: ', JSON.stringify(testData));
  } catch (error) {
    console.log('error test data generator: ', error);
    throw error;
  }

  const runTestCase = async (title, test) => {
    console.log(title);

    await test();
  };

  console.log('check: /couriers?number=[courier.number]');
  await runTestCase('unknown courier', async () => {
    const yandexApi = YandexApiFactory.createWithValidUserToken();

    const courierUserResult = await yandexApi.fetchCouriersByLogin(
      testData.company.id,
      testData.unknownCourierLogin,
    );

    expect(courierUserResult.data).toBeFalsy();
    expect(courierUserResult.error).toBeTruthy();
    expect(courierUserResult.error.message).toEqual('Courier not found');
  });

  await runTestCase('exist courier', async () => {
    const yandexApi = YandexApiFactory.createWithValidUserToken();

    const courierUserResult = await yandexApi.fetchCouriersByLogin(
      testData.courierUser.company_id,
      testData.courierUser.number,
    );

    expect(courierUserResult.error).toBeFalsy();
    expect(courierUserResult.data).toBeTruthy();

    const { number } = courierUserResult.data[0];

    expect(number).toEqual(testData.courierUser.number);
  });

  console.log('check: /companies/{id}');
  await runTestCase('exist company', async () => {
    const yandexApi = YandexApiFactory.createWithValidUserToken();

    const companiesResult = await yandexApi.fetchCompanies();

    expect(companiesResult.error).toBeFalsy();
    expect(companiesResult.data).toBeTruthy();
    expect(companiesResult.data.length >= 1).toEqual(true);

    const company = companiesResult.data.find(company => company.id === testData.user.company_id);
    expect(company.name).toEqual(testData.company.name);

    // check necessary company settings
    expect(company.optimal_order_sequence_enabled).toEqual(true);
  });

  console.log('check: /current_user');
  await runTestCase("courier doesn't exist", async () => {
    const yandexApi = YandexApiFactory.createWithNotExistUserToken();

    const courierResult = await yandexApi.fetchCourierUser();

    expect(courierResult.data).toBeFalsy();
    expect(courierResult.error).toBeTruthy();
  });

  await runTestCase("courier exists, but doesn't linked to company", async () => {
    const yandexApi = YandexApiFactory.createWithUnregisteredUserToken();

    const courierUserResult = await yandexApi.fetchCourierUser();

    expect(courierUserResult.error).toBeFalsy();
    expect(courierUserResult.data).toBeTruthy();
    expect(!!courierUserResult.data.login).toEqual(false);
    expect(!!courierUserResult.data.passportUser).toEqual(true);
  });

  await runTestCase('courier exists', async () => {
    const yandexApi = YandexApiFactory.createWithValidUserToken();

    const courierUserResult = await yandexApi.fetchCourierUser();

    expect(courierUserResult.error).toBeFalsy();
    expect(courierUserResult.data).toBeTruthy();
    expect(courierUserResult.data?.login).toEqual(testData.user.login);
    expect(courierUserResult.data?.company_users?.[0]?.company_id).toEqual(
      testData.user.company_id,
    );
  });

  console.log('check: /couriers/${courierID}/routes');
  await runTestCase('exist route', async () => {
    const yandexApi = YandexApiFactory.createWithValidUserToken();

    const fetchRoutesResult = await yandexApi.fetchAllRoutes(testData.route.courier_id);

    expect(fetchRoutesResult.error).toBeFalsy();
    expect(fetchRoutesResult.data).toBeTruthy();
    expect(fetchRoutesResult.data.length > 0).toEqual(true);

    const targetRoute = fetchRoutesResult.data.find(
      route => route.number === testData.route.number,
    );

    expect(!!targetRoute).toEqual(true);
    expect(targetRoute.date).toEqual(testData.route.date);
  });

  console.log('check: companies/${companyID}/order-details?route_id={route_id}');
  await runTestCase('fetch orders', async () => {
    const orders = await getOrders();

    expect(!!orders).toEqual(true);
    expect(orders.length > 0).toEqual(true);
  });

  console.log('check: PATCH /couriers/${courier.id}/routes/${route.id}/orders/${order.id}');
  await runTestCase('change status', async () => {
    const orders = await getOrders();

    const firstNewOrder = orders.find(order => order.status === STATUSES.NEW);

    expect(!!firstNewOrder).toEqual(true);

    const statusesQueue = [
      STATUSES.CONFIRMED,
      STATUSES.POSTPONED,
      STATUSES.CONFIRMED,
      STATUSES.FINISHED,
      STATUSES.CANCELLED,
      STATUSES.NEW,
    ];

    const yandexApi = YandexApiFactory.createWithValidUserToken();

    for (const targetStatus of statusesQueue) {
      const changedOrderResult = await yandexApi.editOrder(
        testData.route.courier_id,
        testData.route.id,
        firstNewOrder.order_id,
        { status: targetStatus },
      );

      expect(changedOrderResult.error).toBeFalsy();
      expect(changedOrderResult.data).toBeTruthy();
      expect(changedOrderResult.data.status).toEqual(targetStatus);
    }
  });

  await runTestCase('change comments', async () => {
    const orders = await getOrders();

    const firstConfirmedOrder = orders.find(order => order.status === STATUSES.CONFIRMED);

    expect(!!firstConfirmedOrder).toEqual(true);

    const testComment = 'test comment ' + Math.random();

    const yandexApi = YandexApiFactory.createWithValidUserToken();

    const changedOrderResult = await yandexApi.editOrder(
      testData.route.courier_id,
      testData.route.id,
      firstConfirmedOrder.order_id,
      { comments: testComment },
    );

    expect(changedOrderResult.error).toBeFalsy();
    expect(changedOrderResult.data).toBeTruthy();
    expect(changedOrderResult.data.comments).toEqual(testComment);
  });

  await runTestCase('change time interval', async () => {
    const orders = await getOrders();

    const firstConfirmedOrder = orders.find(order => order.status === STATUSES.CONFIRMED);

    expect(!!firstConfirmedOrder).toEqual(true);

    const testTimeInterval = '12:00-14:00';

    const yandexApi = YandexApiFactory.createWithValidUserToken();

    const changedOrderResult = await yandexApi.editOrder(
      testData.route.courier_id,
      testData.route.id,
      firstConfirmedOrder.order_id,
      { time_interval: testTimeInterval },
    );

    console.log('changedOrderResult.error', changedOrderResult.error);

    expect(changedOrderResult.error).toBeFalsy();
    expect(changedOrderResult.data).toBeTruthy();
    const hasChangedTimeInterval =
      changedOrderResult.data.time_interval !== firstConfirmedOrder.time_interval;
    expect(hasChangedTimeInterval).toEqual(true);
  });

  console.log('check: /couriers/${courierID}/routes/${routeID}/predict-eta');
  await runTestCase('check current ETAs', async () => {
    const sourceOrders = await getOrders();

    const sourceSequence = sourceOrders.map(order => order.order_id);

    const time = moment().format();

    const yandexApi = YandexApiFactory.createWithValidUserToken();

    const ordersEtaResult = await yandexApi.predictOrdersEta(
      testData.route.courier_id,
      testData.route.id,
      time,
      testData.currentLocation,
      sourceSequence,
    );

    expect(ordersEtaResult.error).toBeFalsy();
    expect(ordersEtaResult.data).toBeTruthy();
    expect(!!ordersEtaResult.data.route_end).toEqual(true);
    expect(ordersEtaResult.data.route.length > 0).toEqual(true);
  });

  console.log('check: /companies/${companyID}/routes/${routeID}/order-sequence');
  await runTestCase('save orders sequence', async () => {
    const sourceOrders = await getOrders();

    const notEditableSourceSequence = sourceOrders
      .filter(order => !checkIsEditableOrderStatus(order.status))
      .map(order => order.order_id);
    const editableSourceSequence = sourceOrders
      .filter(order => checkIsEditableOrderStatus(order.status))
      .map(order => order.order_id);
    const randomSequence = shuffle(editableSourceSequence);

    const yandexApi = YandexApiFactory.createWithValidUserToken();

    await yandexApi.changeOrdersSequence(testData.route.company_id, testData.route.id, [
      ...notEditableSourceSequence,
      ...randomSequence,
    ]);

    const savedRandomOrders = await getOrders();
    const savedRandomSequence = savedRandomOrders
      .filter(order => checkIsEditableOrderStatus(order.status))
      .map(order => order.order_id);

    expect(savedRandomSequence).toEqual(randomSequence);
  });

  await runTestCase('find optimal orders sequence', async () => {
    const time = moment().format();

    const yandexApi = YandexApiFactory.createWithValidUserToken();

    const optimalRouteResult = await yandexApi.findOptimalOrdersSequence(
      testData.route.courier_id,
      testData.route.id,
      time,
      testData.currentLocation,
    );

    expect(optimalRouteResult.error).toBeFalsy();
    expect(optimalRouteResult.data).toBeTruthy();
    expect(!!optimalRouteResult.data.route_end).toEqual(true);
    expect(optimalRouteResult.data.route.length > 0).toEqual(true);
  });

  console.log('check: /couriers/${courierId}/routes/${routeId}/push-positions');

  await runTestCase('push position', async () => {
    const status = await sendPosition(
      testData.route.courier_id,
      testData.route.id,
      testData.currentLocation,
    );

    expect(status).toEqual(200);
  });

  console.log('check: /couriers/${courierId}/routes/${routeId}/push-positions-v2');
  await runTestCase('push position', async () => {
    const status = await sendPositionV2(
      testData.route.courier_id,
      testData.route.id,
      testData.currentLocation,
    );

    expect(status).toEqual(200);
  });

  console.log('check: companies/${companyId}/depots/${depotId}');
  await runTestCase('fetch depot', async () => {
    const yandexApi = YandexApiFactory.createWithValidUserToken();

    const fetchDepotResult = await yandexApi.fetchDepot(testData.company.id, testData.depot.id);

    expect(fetchDepotResult.error).toBeFalsy();
    expect(fetchDepotResult.data).toBeTruthy();
  });
});
