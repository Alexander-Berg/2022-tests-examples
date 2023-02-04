const moment = require('moment');
const fetchApi = require('./api');
const ordersData = require('../data/orders.json');
const config = require('../config');

const generateRouteNumber = date => `test-${date}-${(Math.random() * 100) ^ 0}`;

const createRoute = async (routeNumber, date) => {
  const routeData = [
    {
      courier_id: config.testData.courierUser.id,
      date,
      depot_id: config.testData.depot.id,
      number: routeNumber,
    },
  ];

  return await fetchApi(
    'POST',
    `api/v1/companies/${config.testData.company.id}/routes-batch`,
    routeData,
  );
};

const getRoute = async routeNumber => {
  const routes = await fetchApi(
    'GET',
    `api/v1/companies/${config.testData.company.id}/routes?number=${routeNumber}`,
  );

  if (!routes.length) {
    throw new Error("Can't get route");
  }

  return routes[0];
};

const createOrders = routeId => {
  const ordersToCreate = ordersData.map(order => ({ ...order, route_id: routeId }));
  return fetchApi(
    'POST',
    `api/v1/companies/${config.testData.company.id}/orders-batch`,
    ordersToCreate,
  );
};

module.exports = async () => {
  const date = moment().format('YYYY-MM-DD');
  const routeNumber = generateRouteNumber(date);

  await createRoute(routeNumber, date);

  const route = await getRoute(routeNumber);

  await createOrders(route.id);

  return getRoute(routeNumber);
};
