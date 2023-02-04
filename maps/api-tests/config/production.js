module.exports = {
  testData: {
    // route: {"id":1045202,"company_id":6,"courier_id":5095,"car_id":null,"depot_id":564,"number":"test-2020-01-22-98","date":"2020-01-22","imei":null,"courier_violated_route":false,"route_start":null},
    user: {
      // id: 21990,
      login: 'testcourier2',
      company_id: 6,
      // is_super: false,
      // confirmed_at: '2019-12-18T15:43:21.646102',
      // role: 'app',
    },
    courierUser: {
      id: 5095,
      company_id: 6,
      // user_id: null,
      number: '102-2',
      name: 'Danil',
      // phone: '+79629409879',
      // rating: null,
      // sms_enabled: false
    },
    depot: {
      id: 564,
    },
    company: {
      id: 6,
      name: 'Flash Logistics',
      // logo_url: 'https://avatars.mds.yandex.net/get-switch/41639/flash_1c27fd050b06e13818aad698f15735d8.gif/orig',
      // bg_color: '#bc0303',
      // mark_delivered_enabled: false,
      // mark_delivered_radius: 50,
      // mark_delivered_service_time_coefficient: 0.3,
      // optimal_order_sequence_enabled: true
    },
    unknownCourierLogin: '1337',
    currentLocation: {
      latitude: 55.736945,
      longitude: 37.640238,
    },
    invalidAppUserToken: '111111111111111',
  },
  endpoint: 'https://courier.yandex.ru',
  apiVersion: 'production',
};
