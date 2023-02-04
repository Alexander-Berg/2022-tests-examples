const times = require('lodash/times');
const random = require('lodash/random');
const fs = require('fs');

const STATUSES = ['new', 'cancelled', 'finished', 'confirmed', 'postponed'];

const orders = times(10).map(i => {
  const lat = 55.55 + random(0.1, 0.9);
  const lon = 37.55 + random(0.1, 0.9);
  const status = STATUSES[random(0, STATUSES.length - 1)];

  const intervalStart = random(7, 20);
  const intervalEnd = random(intervalStart + 1, 23);
  return {
    number: String(i),
    address: 'Конфетное королевство, переулок Патоки, 3',
    lat,
    lon,
    customer_name: 'Клиент',
    phone: '+7000000',
    time_interval: `${intervalStart}-${intervalEnd}`,
    status,
    route_id: null,
  };
});

fs.writeFile(`${__dirname}/orders.json`, JSON.stringify(orders, null, 2), function (err) {
  if (err) {
    return console.log(err);
  }
  console.log('The file was saved!');
});
