import { NewOrderT, RouteT } from 'generate-data';

export default function (route: Pick<RouteT, 'id'>): Array<NewOrderT> {
  return [
    {
      address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
      amount: 100,
      customer_name: 'Bowser',
      lat: 60,
      lon: 31,
      number: 'additional-3',
      payment_type: 'cash',
      phone: '+70000000000',
      route_id: route.id,
      time_interval: '00:00 - 00:01',
    },
    {
      address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
      amount: 100,
      customer_name: 'Bowser',
      lat: 60,
      lon: 31,
      number: 'additional-4',
      payment_type: 'cash',
      phone: '+70000000000',
      route_id: route.id,
      time_interval: '00:00 - 00:01',
    },
  ];
}
