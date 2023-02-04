import type { GenerateSchemaT } from 'generate-data/dist/types/schema';
import { getAccounts } from 'constants/accounts';
import { getShortBranchName } from 'utils/getCurrentBranchName';
import { RoutingModeEnum } from 'generate-data/dist/types/constants';

export const getAddRouteGenerateScheme = (
  branchNameProps?: string,
): GenerateSchemaT<'addRoute'> => {
  const branchName = getShortBranchName(branchNameProps);
  const accounts = getAccounts(branchName);

  return {
    addRoute: {
      data: {
        name: `Company for addRoute-${branchName}`,
        initial_login: accounts.adminAddRoute,
        sms_enabled: true,
        mark_delivered_enabled: false,
        services: [
          {
            enabled: true,
            name: 'courier',
          },
          {
            enabled: false,
            name: 'mvrp',
          },
        ],
      },
      users: [
        {
          data: {
            login: accounts.managerAddRoute,
            role: 'manager',
          },
        },
      ],
      depots: [
        {
          data: {
            address: 'Россия, Санкт-Петербург, Пискаревский проспект, 2к6',
            lat: 59.943547,
            lon: 30.331111,
            name: 'addRouteDepotName1',
            number: 'addRouteDepotNumber1',
            order_service_duration_s: 0,
            service_duration_s: 0,
            time_interval: '03:00 - 22:00',
          },
          users: [
            {
              data: {
                login: accounts.managerAddRoute,
              },
            },
          ],
        },
        {
          data: {
            address: 'Россия, Санкт-Петербург, Пискаревский проспект, 2к6',
            lat: 59.943547,
            lon: 30.331111,
            name: 'addRouteDepotName2',
            number: 'addRouteDepotNumber2',
            order_service_duration_s: 0,
            service_duration_s: 0,
            time_interval: '03:00 - 22:00',
          },
          users: [
            {
              data: {
                login: accounts.managerAddRoute,
              },
            },
          ],
        },
        {
          data: {
            address: 'Россия, Санкт-Петербург, Пискаревский проспект, 2к6',
            lat: 59.943547,
            lon: 30.331111,
            name: 'addRouteDepotName3',
            number: 'addRouteDepotNumber3',
            order_service_duration_s: 0,
            service_duration_s: 0,
            time_interval: '03:00 - 22:00',
          },
          users: [
            {
              data: {
                login: accounts.managerAddRoute,
              },
            },
          ],
        },
      ],
      couriers: [
        {
          data: {
            name: 'addRouteCourierName1',
            number: 'addRouteCourierNumber1',
            phone: '+70000000000',
            sms_enabled: true,
          },
        },
        {
          data: {
            name: 'addRouteCourierName2',
            number: 'addRouteCourierNumber2',
            phone: '+70000000000',
            sms_enabled: true,
          },
        },
        {
          data: {
            name: 'addRouteCourierName3',
            number: 'addRouteCourierNumber3',
            phone: '+70000000000',
            sms_enabled: true,
          },
        },
      ],
      routes: [
        {
          data: {
            courierNumber: 'addRouteCourierNumber1',
            depotName: 'addRouteDepotName1',
            date: '2022-01-01',
            number: 'add_route-some_exist_route',
            routingMode: RoutingModeEnum.WALKING,
          },
          orders: [],
        },
      ],
    },
  };
};
