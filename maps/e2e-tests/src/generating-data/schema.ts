import moment from 'moment';
import { getAccounts } from 'constants/accounts';
import getCurrentBranchName from 'utils/getCurrentBranchName';
import { depotAddressRecord, depotNameRecord, depotNumberRecord } from 'constants/depots';
import { courierNameRecord, courierNumberRecord } from 'constants/couriers';
import { vehicleNameRecord, vehicleNumberRecord } from 'constants/vehicles';
import { zonesNameRecord, zonesColorRecord } from 'constants/zones';
import { routeNumberRecord, sharedRouteNumberRecord } from 'constants/routes';
import time from 'utils/time';
import { OrderStatusesEnum } from 'utils/constants';
import { appUsers } from '../constants/app_users';
import { additionalCompaniesNames, companyNameRecord } from 'constants/companies';
import {
  DepotGenerateSchemaT,
  GenerateSchemaT,
  OrderGenerateSchemaT,
  RoutingModeEnum,
} from 'generate-data';

const accounts = getAccounts();
const branchName = getCurrentBranchName();

const anyOtherDepotNameByIndex: (index: number) => string = (index: number) =>
  `Тестовый склад ${index}`;

const anyOtherDepotNumberByIndex: (index: number) => string = (index: number) => `a-${index}`;

function generateDepots(length: number): Array<DepotGenerateSchemaT> {
  return new Array(length).fill(undefined).map((el, index: number) => ({
    data: {
      address: 'Россия, Санкт-Петербург, Пискаревский проспект, 2к2',
      description: `Тестовый склад ${index}`,
      lat: 59.458606,
      lon: 30.405313,
      name: anyOtherDepotNameByIndex(index),
      number: anyOtherDepotNumberByIndex(index),
      order_service_duration_s: 0,
      service_duration_s: 0,
      time_interval: '09:00 - 20:00',
    },
    users: [
      {
        data: {
          login: accounts.managerMulti,
        },
      },
    ],
  }));
}

function generateOrders(length: number): Array<OrderGenerateSchemaT> {
  return new Array(length).fill(undefined).map((_, index: number) => ({
    data: {
      address: `some adress-${index}`,
      amount: index,
      customer_name: 'customer-lot-of-orders',
      lat: 60.5 - 0.15 * index,
      lon: 30 + 0.15 * index,
      number: (1000 + index).toString(),
      payment_type: 'cash',
      phone: '+7-000-999-99-66',
      route_id: 1,
      time_interval: '00:00 - 23:59',
    },
  }));
}

export enum CompanySlugEnum {
  mvrpView = 'mvrpView',
  importPlanning = 'importPlanning',
  refBookFilled = 'refBookFilled',
  common = 'common',
  A = 'A',
  B = 'B',
  V = 'V',
  G = 'G',
  D = 'D',
  E = 'E',
  someNumbers1 = 'someNumbers1',
  someNumbers2 = 'someNumbers2',
  someNumbers3 = 'someNumbers3',
  someLowerCase = 'someLowerCase',
  someUpperCase = 'someUpperCase',
  someUpperLowerCase = 'someUpperLowerCase',
  someEnglishLowerCase = 'someEnglishLowerCase',
  someEnglishUpperCase = 'someEnglishUpperCase',
  someRussian = 'someRussian',
  shareFromOthersCompany = 'shareFromOthersCompany',
  exportRoutesCompany = 'exportRoutesCompany',
}

export enum CommonCompanySlugEnum {
  appRole = 'appRole',
}

export const currentSchema: GenerateSchemaT<CompanySlugEnum> = {
  [CompanySlugEnum.mvrpView]: {
    data: {
      name: `Company for mvrpView-${branchName}`,
      initial_login: accounts.mvrpViewManager,
      sms_enabled: true,
      mark_delivered_enabled: false,
      services: [
        {
          enabled: true,
          name: 'courier',
        },
        {
          enabled: true,
          name: 'mvrp',
        },
      ],
    },
  },
  [CompanySlugEnum.importPlanning]: {
    data: {
      name: `Company for importPlanning-${branchName}`,
      initial_login: accounts.mvrpManager,
      sms_enabled: true,
      mark_delivered_enabled: false,
      services: [
        {
          enabled: true,
          name: 'courier',
        },
        {
          enabled: true,
          name: 'mvrp',
        },
      ],
    },
    vehicles: [
      {
        data: {
          name: 'testmobile',
          number: courierNameRecord.gumba,
          routing_mode: 'driving',
        },
      },
    ],
  },
  [CompanySlugEnum.refBookFilled]: {
    data: {
      name: `Company for refBookFilled-${branchName}`,
      initial_login: accounts.refBookManager,
      sms_enabled: true,
      mark_delivered_enabled: false,
      services: [
        {
          enabled: true,
          name: 'courier',
        },
        {
          enabled: true,
          name: 'mvrp',
        },
      ],
    },
    zones: [
      {
        data: {
          number: zonesNameRecord.constellations2,
          color_edge: zonesColorRecord.blue,
          color_fill: zonesColorRecord.blue,
          polygon: {
            type: 'Polygon',
            coordinates: [
              [
                [37.6214671789982, 55.7527234543328],
                [37.8425670325138, 55.7581445099927],
                [37.8391338049747, 55.8223650820677],
                [37.7217174231388, 55.8860934600023],
                [37.6791454016544, 55.8582976201237],
                [37.6647258459902, 55.841494616599],
                [37.6715923010684, 55.8250708424216],
                [37.6214671789982, 55.7527234543328],
              ],
            ],
          },
        },
      },
      {
        data: {
          number: zonesNameRecord.constellations1,
          color_edge: zonesColorRecord.brass,
          color_fill: zonesColorRecord.brass,
          polygon: {
            type: 'Polygon',
            coordinates: [
              [
                [37.6213440597215, 55.7527175193301],
                [37.5825384283368, 55.7774121720533],
                [37.5286265957802, 55.8013176064587],
                [37.4276897061319, 55.8264972148494],
                [37.3911258328408, 55.8332122346396],
                [37.387005959794, 55.8076986324462],
                [37.3664065945596, 55.7860375497773],
                [37.3718997586221, 55.7240820471537],
                [37.6213440597215, 55.7527175193301],
              ],
            ],
          },
        },
      },
      {
        data: {
          number: zonesNameRecord.constellations3,
          color_edge: zonesColorRecord.magenta,
          color_fill: zonesColorRecord.magenta,
          polygon: {
            type: 'Polygon',
            coordinates: [
              [
                [37.6214771396229, 55.7527146537201],
                [37.4442957765171, 55.7323123578642],
                [37.3718975907871, 55.723981317011],
                [37.4120663529942, 55.6920210389756],
                [37.4326657182286, 55.6621451120649],
                [37.460474861295, 55.6388492814921],
                [37.4927472001622, 55.6110702372716],
                [37.5133465653966, 55.6264191466403],
                [37.5438337389243, 55.6575004714454],
                [37.5746642352059, 55.6895267326787],
                [37.6214771396229, 55.7527146537201],
              ],
            ],
          },
        },
      },
      {
        data: {
          number: zonesNameRecord.testName2,
          color_edge: zonesColorRecord.orange,
          color_fill: zonesColorRecord.orange,
          polygon: {
            type: 'Polygon',
            coordinates: [
              [
                [37.6215089543763, 55.752661309528],
                [37.5751523596662, 55.6890619090916],
                [37.5160459575035, 55.6286644086977],
                [37.4928716716149, 55.6106930145557],
                [37.5110677775719, 55.5961385744746],
                [37.547288328109, 55.5874867056692],
                [37.6019416072723, 55.5744290927678],
                [37.6788459041472, 55.5720948970474],
                [37.7186713436004, 55.5868757834878],
                [37.6215089543763, 55.752661309528],
              ],
            ],
          },
        },
      },
      {
        data: {
          number: zonesNameRecord.testName1,
          color_edge: zonesColorRecord.green,
          color_fill: zonesColorRecord.green,
          polygon: {
            type: 'Polygon',
            coordinates: [
              [
                [37.6215074686309, 55.7527111552922],
                [37.8425846345566, 55.7581153591525],
                [37.8423505049661, 55.7556360766382],
                [37.8406768065408, 55.7287636580473],
                [37.8384758435047, 55.7141830767832],
                [37.8300215206898, 55.6984082063191],
                [37.8296781979359, 55.6891002144753],
                [37.8418661556995, 55.659804307571],
                [37.6215074686309, 55.7527111552922],
              ],
            ],
          },
        },
      },
      {
        data: {
          number: zonesNameRecord.bracket1,
          color_edge: zonesColorRecord.olive,
          color_fill: zonesColorRecord.olive,
          polygon: {
            type: 'Polygon',
            coordinates: [
              [
                [37.6215696113274, 55.7526173179551],
                [37.8402553224515, 55.6588096193547],
                [37.8375087404202, 55.6525982272603],
                [37.7200923585843, 55.5877077283507],
                [37.6215696113274, 55.7526173179551],
              ],
            ],
          },
        },
      },
      {
        data: {
          number: zonesNameRecord.testDigit1,
          color_edge: zonesColorRecord.darkGreen,
          color_fill: zonesColorRecord.darkGreen,
          polygon: {
            type: 'Polygon',
            coordinates: [
              [
                [37.6216979001369, 55.7528176877445],
                [37.6628921300759, 55.8149161315613],
                [37.6704452306618, 55.825546860975],
                [37.6628921300758, 55.8421636103452],
                [37.6703010077103, 55.8494545511189],
                [37.6747642035111, 55.856406972998],
                [37.7207694525346, 55.8867126574519],
                [37.6996897843703, 55.8942868890215],
                [37.6502825298601, 55.8963660976697],
                [37.5864244976336, 55.9102528335874],
                [37.5785280742937, 55.9112170048561],
                [37.5338961162859, 55.9069744711877],
                [37.5230763774756, 55.9034690191728],
                [37.6216979001369, 55.7528176877445],
              ],
            ],
          },
        },
      },
      {
        data: {
          number: zonesNameRecord.testDigit2,
          color_edge: zonesColorRecord.lightBlue,
          color_fill: zonesColorRecord.lightBlue,
          polygon: {
            type: 'Polygon',
            coordinates: [
              [
                [37.570967368356, 55.8274790582914],
                [37.5222886784974, 55.9027669763386],
                [37.4804033025208, 55.8857903309884],
                [37.4433244450989, 55.8819309621407],
                [37.4096788152161, 55.8707366131734],
                [37.3931993230286, 55.8541321291944],
                [37.3925126775208, 55.8452477350493],
                [37.395259259552, 55.8324969981398],
                [37.5264085515442, 55.8035024311125],
                [37.5833269874966, 55.7785559859019],
                [37.6223926402458, 55.752819011975],
                [37.570967368356, 55.8274790582914],
              ],
            ],
          },
        },
      },
      {
        data: {
          number: zonesNameRecord.bracket2,
          color_edge: zonesColorRecord.yellow,
          color_fill: zonesColorRecord.yellow,
          polygon: {
            type: 'Polygon',
            coordinates: [
              [
                [37.6417649968569, 54.2184200693314],
                [37.5943864568178, 54.2228462807232],
                [37.568293927521, 54.1930607662668],
                [37.5792802556459, 54.1648654195561],
                [37.6067460759584, 54.1600299923394],
                [37.645198224396, 54.1733260513497],
                [37.6616777165835, 54.1978923153595],
                [37.6417649968569, 54.2184200693314],
              ],
            ],
          },
        },
      },
      {
        data: {
          number: zonesNameRecord.bracket3,
          color_edge: zonesColorRecord.pink,
          color_fill: zonesColorRecord.pink,
          polygon: {
            type: 'Polygon',
            coordinates: [
              [
                [39.7751725896303, 54.6377812847642],
                [39.7531999333803, 54.6473390851744],
                [39.6632493718569, 54.6604773754807],
                [39.6344102605288, 54.6389761332612],
                [39.6433366521303, 54.6035139614693],
                [39.7020448430483, 54.5881639618927],
                [39.7593797429506, 54.5823812864938],
                [39.8198045476381, 54.5827801181647],
                [39.7964586003725, 54.6246356233899],
                [39.7751725896303, 54.6377812847642],
              ],
            ],
          },
        },
      },
    ],
    vehicles: [
      {
        data: {
          name: 'testmobile',
          number: courierNameRecord.gumba,
          routing_mode: 'truck',
          capacity: {
            depth: 13.6,
            height: 2.6,
            units: 26,
            width: 2.45,
            weight: 22000,
          },
          parameters: {
            height: 2.6,
            length: 13.6,
            width: 2.45,
            max_weight: 20,
          },
          shifts: [
            {
              time_window: {
                end: '21:00',
                start: '09:00',
              },
              balanced_group_id: '123',
              hard_window: false,
              max_duration_s: null,
              maximal_stops: null,
              minimal_stops: null,
              penalty: {
                stop_excess: {
                  per_stop: null,
                },
                stop_lack: {
                  per_stop: null,
                },
              },
            },
          ],
        },
      },
      {
        data: {
          name: vehicleNameRecord.atlantis,
          number: vehicleNumberRecord.rus1,
          routing_mode: 'driving',
          capacity: {
            depth: null,
            height: null,
            units: null,
            width: null,
            weight: 65.7,
          },
        },
      },
      {
        data: {
          name: vehicleNameRecord.apollo,
          number: vehicleNumberRecord.rus2,
          routing_mode: 'driving',
          capacity: {
            depth: null,
            height: null,
            units: null,
            width: null,
            weight: 158,
          },
        },
      },
      {
        data: {
          name: vehicleNameRecord.apollo11,
          number: vehicleNumberRecord.rus3,
          routing_mode: 'driving',
          capacity: {
            depth: null,
            height: null,
            units: null,
            width: null,
            weight: 92,
          },
        },
      },
      {
        data: {
          name: vehicleNameRecord.challenger,
          number: vehicleNumberRecord.rus4,
          routing_mode: 'truck',
          imei: '12345678910',
          capacity: {
            depth: 13.6,
            height: 2.6,
            units: 26,
            width: 2.45,
            weight: 22000,
          },
          parameters: {
            height: 2.6,
            length: 13.6,
            width: 2.45,
            max_weight: 20,
          },
        },
      },
      {
        data: {
          name: vehicleNameRecord.endeavour,
          number: vehicleNumberRecord.rus5,
          routing_mode: 'truck',
          capacity: {
            depth: 12,
            height: 2.6,
            units: 22,
            width: 2.5,
            weight: 20000,
          },
        },
      },
      {
        data: {
          name: vehicleNameRecord.endurance,
          number: vehicleNumberRecord.rus6,
          routing_mode: 'truck',
          capacity: {
            depth: 13.6,
            height: 3,
            units: 33,
            width: 2.45,
            weight: 25000,
          },
        },
      },
      {
        data: {
          name: vehicleNameRecord.spaceShuttle,
          number: vehicleNumberRecord.es1,
          routing_mode: 'truck',
          capacity: {
            depth: 13.6,
            height: 2.6,
            units: 26,
            width: 2.45,
            weight: 22000,
          },
        },
      },
      {
        data: {
          name: vehicleNameRecord.spaceTravel,
          number: vehicleNumberRecord.es2,
          routing_mode: 'walking',
          capacity: {
            depth: null,
            height: null,
            units: null,
            width: null,
            weight: 22,
          },
        },
      },
      {
        data: {
          name: vehicleNameRecord.testPhone1,
          number: vehicleNumberRecord.es3,
          routing_mode: 'walking',
          capacity: {
            depth: null,
            height: null,
            units: null,
            width: null,
            weight: 20,
          },
        },
      },
      {
        data: {
          name: vehicleNameRecord.testPhone2,
          number: vehicleNumberRecord.es4,
          routing_mode: 'walking',
          capacity: {
            depth: null,
            height: null,
            units: null,
            width: null,
            weight: 7.5,
          },
        },
      },
      {
        data: {
          name: vehicleNameRecord.testNumbers1,
          number: vehicleNumberRecord.tr1,
          routing_mode: 'walking',
          capacity: {
            depth: null,
            height: null,
            units: null,
            width: null,
            weight: 15,
          },
        },
      },
      {
        data: {
          name: vehicleNameRecord.testNumbers2,
          number: vehicleNumberRecord.tr2,
          routing_mode: 'transit',
          capacity: {
            depth: null,
            height: null,
            units: 30,
            width: null,
            weight: null,
          },
        },
      },
      {
        data: {
          name: vehicleNameRecord.testNumbers3,
          number: vehicleNumberRecord.tr3,
          routing_mode: 'transit',
          capacity: {
            depth: null,
            height: null,
            units: 100,
            width: null,
            weight: null,
          },
        },
      },
      {
        data: {
          name: vehicleNameRecord.testSymbol1,
          number: vehicleNumberRecord.tr4,
          routing_mode: 'transit',
          capacity: {
            depth: null,
            height: null,
            units: 300,
            width: null,
            weight: null,
          },
        },
      },
      {
        data: {
          name: vehicleNameRecord.testSymbol2,
          number: vehicleNumberRecord.dashSign,
          routing_mode: 'transit',
          capacity: {
            depth: null,
            height: null,
            units: 78,
            width: null,
            weight: null,
          },
        },
      },
    ],
  },
  [CompanySlugEnum.exportRoutesCompany]: {
    data: {
      name: `exportRoutes-company-${branchName}`,
      initial_login: accounts.exportRoutesAdmin,
      services: [
        {
          enabled: true,
          name: 'courier',
        },
        {
          enabled: true,
          name: 'mvrp',
        },
      ],
      mark_delivered_enabled: false,
    },
    users: [
      {
        data: {
          login: accounts.exportRoutesManager,
          role: 'manager',
        },
      },
    ],
  },
  [CompanySlugEnum.common]: {
    data: {
      name: `Грибное Королевство-${branchName}`,
      initial_login: accounts.admin,
      sms_enabled: true,
      mark_delivered_enabled: false,
      services: [
        {
          enabled: true,
          name: 'courier',
        },
        // {
        //   enabled: true,
        //   name: 'mvrp',
        // },
      ],
    },
    users: [
      {
        data: {
          login: accounts.manager,
          role: 'manager',
        },
      },
      {
        data: {
          login: accounts.managerEmpty,
          role: 'manager',
        },
      },
      {
        data: {
          login: accounts.dispatcher,
          role: 'dispatcher',
        },
      },
      {
        data: {
          login: '+799988779999',
          role: 'app',
        },
      },
    ],
    depots: [
      {
        data: {
          address: 'Россия, Санкт-Петербург, Пискаревский проспект, 2к6',
          lat: 59.943547,
          lon: 30.331111,
          name: depotNameRecord.depotLotOfOrders,
          number: depotNumberRecord.depotLotOfOrders,
          order_service_duration_s: 0,
          service_duration_s: 0,
          time_interval: '03:00 - 22:00',
        },
        users: [
          {
            data: {
              login: accounts.manager,
            },
          },
        ],
      },
      {
        data: {
          address: depotAddressRecord.castlePeach,
          lat: 59.958606,
          lon: 30.405313,
          name: depotNameRecord.castlePeach,
          number: depotNumberRecord.castlePeach,
          order_service_duration_s: 0,
          service_duration_s: 0,
          time_interval: '09:00 - 20:00',
        },
        users: [
          {
            data: {
              login: accounts.manager,
            },
          },
          {
            data: {
              login: accounts.dispatcher,
            },
          },
        ],
      },
      {
        data: {
          address: 'Россия, Санкт-Петербург, Пискаревский проспект, 2к2',
          lat: 59.943557,
          lon: 30.331711,
          name: depotNameRecord.yoshisHouse,
          number: depotNumberRecord.yoshisHouse,
          order_service_duration_s: 0,
          service_duration_s: 0,
          time_interval: '09:00 - 20:00',
        },
        users: [
          {
            data: {
              login: accounts.manager,
            },
          },
          {
            data: {
              login: accounts.dispatcher,
            },
          },
        ],
      },
      {
        data: {
          address: 'Россия, Санкт-Петербург, Пискаревский проспект, 2к2',
          lat: 59.933557,
          lon: 30.321711,
          name: depotNameRecord.toadsTent,
          number: depotNumberRecord.toadsTent,
          order_service_duration_s: 0,
          service_duration_s: 0,
          time_interval: '09:00 - 20:00',
        },
        users: [
          {
            data: {
              login: accounts.manager,
            },
          },
          {
            data: {
              login: accounts.dispatcher,
            },
          },
        ],
      },
      {
        data: {
          address: 'Россия, Санкт-Петербург, Пискаревский проспект, 2к2',
          lat: 59.933557,
          lon: 30.321711,
          name: depotNameRecord.englishPub,
          number: depotNumberRecord.englishPub,
          order_service_duration_s: 0,
          service_duration_s: 0,
          time_interval: '09:00 - 20:00',
        },
        users: [
          {
            data: {
              login: accounts.manager,
            },
          },
        ],
      },
      {
        data: {
          address: 'Россия, Санкт-Петербург, Пискаревский проспект, 2к2',
          lat: 59.933557,
          lon: 30.321711,
          name: depotNameRecord.fourwordsallcapslock,
          number: depotNumberRecord.fourwordsallcapslock,
          order_service_duration_s: 0,
          service_duration_s: 0,
          time_interval: '09:00 - 20:00',
        },
        users: [
          {
            data: {
              login: accounts.manager,
            },
          },
        ],
      },
      {
        data: {
          address: 'Россия, Санкт-Петербург, Пискаревский проспект, 2к2',
          lat: 59.933557,
          lon: 30.321711,
          name: depotNameRecord.ONEWORDCAPSLOCK,
          number: depotNumberRecord.ONEWORDCAPSLOCK,
          order_service_duration_s: 0,
          service_duration_s: 0,
          time_interval: '09:00 - 20:00',
        },
        users: [
          {
            data: {
              login: accounts.manager,
            },
          },
        ],
      },
      {
        data: {
          address: 'Россия, Санкт-Петербург, Пискаревский проспект, 2к2',
          lat: 59.933557,
          lon: 30.321711,
          name: depotNameRecord.rocketjump5G,
          number: depotNumberRecord.rocketjump5G,
          order_service_duration_s: 0,
          service_duration_s: 0,
          time_interval: '09:00 - 20:00',
        },
        users: [
          {
            data: {
              login: accounts.manager,
            },
          },
        ],
      },
      {
        data: {
          address: 'Россия, Санкт-Петербург, Пискаревский проспект, 2к2',
          lat: 59.933557,
          lon: 30.321711,
          name: depotNameRecord.some,
          number: depotNumberRecord.some,
          order_service_duration_s: 0,
          service_duration_s: 0,
          time_interval: '09:00 - 20:00',
        },
        users: [
          {
            data: {
              login: accounts.manager,
            },
          },
        ],
      },
      {
        data: {
          address: 'Россия, Санкт-Петербург, Пискаревский проспект, 2к2',
          lat: 59.933557,
          lon: 30.321711,
          name: depotNameRecord.some2,
          number: depotNumberRecord.some2,
          order_service_duration_s: 0,
          service_duration_s: 0,
          time_interval: '09:00 - 20:00',
        },
        users: [
          {
            data: {
              login: accounts.manager,
            },
          },
        ],
      },
      {
        data: {
          address: 'Россия, Санкт-Петербург, Пушкина, Калатушкина',
          lat: 59.933557,
          lon: 30.321711,
          name: depotNameRecord.additional1,
          number: depotNumberRecord.additional1,
          order_service_duration_s: 0,
          service_duration_s: 0,
          time_interval: '09:00 - 20:00',
        },
        users: [
          {
            data: {
              login: accounts.manager,
            },
          },
        ],
      },
      {
        data: {
          address: 'Россия, Санкт-Петербург, Пушкина, Калатушкина',
          lat: 59.933558,
          lon: 30.321711,
          name: depotNameRecord.additional2,
          number: depotNumberRecord.additional2,
          order_service_duration_s: 0,
          service_duration_s: 0,
          time_interval: '09:00 - 20:00',
        },
        users: [
          {
            data: {
              login: accounts.manager,
            },
          },
        ],
      },
      {
        data: {
          address: 'Россия, Санкт-Петербург, Пушкина, Калатушкина',
          lat: 59.933559,
          lon: 30.321711,
          name: depotNameRecord.additional3,
          number: depotNumberRecord.additional3,
          order_service_duration_s: 0,
          service_duration_s: 0,
          time_interval: '09:00 - 20:00',
        },
        users: [
          {
            data: {
              login: accounts.manager,
            },
          },
        ],
      },
      {
        data: {
          address: 'Россия, Санкт-Петербург, Пискаревский проспект, 2к3',
          lat: 59.958606,
          lon: 30.405313,
          name: depotNameRecord.additional4,
          number: depotNumberRecord.additional4,
          order_service_duration_s: 0,
          service_duration_s: 0,
          time_interval: '09:00 - 20:00',
        },
        users: [
          {
            data: {
              login: accounts.manager,
            },
          },
        ],
      },
    ],
    couriers: [
      {
        data: {
          name: courierNameRecord.courierForRemove,
          number: courierNumberRecord.courierForRemove,
          phone: courierNameRecord.courierForRemove,
          sms_enabled: true,
        },
      },
      {
        data: {
          name: courierNameRecord.courierForSearch,
          number: courierNumberRecord.courierForSearch,
          phone: courierNumberRecord.courierForSearch,
          sms_enabled: true,
        },
      },
      {
        data: {
          name: courierNameRecord.gumba,
          number: courierNumberRecord.gumba,
          phone: '+7(000)0000001',
          sms_enabled: true,
        },
      },
      {
        data: {
          name: courierNameRecord.kypa,
          number: courierNumberRecord.kypa,
          phone: '+7(000)0000002',
          sms_enabled: false,
        },
      },
      {
        data: {
          name: courierNameRecord.spaini,
          number: courierNumberRecord.spaini,
          phone: '+7(000)0000003',
          sms_enabled: false,
        },
      },
      {
        data: {
          name: courierNameRecord.john,
          number: courierNumberRecord.john,
          phone: '+7(000)0000004',
          sms_enabled: true,
        },
      },
    ],
    routes: [
      {
        data: {
          courierNumber: courierNumberRecord.gumba,
          depotName: depotNameRecord.depotLotOfOrders,
          date: moment(time.TIME_TODAY)
            .add(+5, 'days')
            .format('YYYY-MM-DD'),
          number: routeNumberRecord.LOT_OF_ORDERS,
        },
        orders: generateOrders(40),
      },
      {
        data: {
          courierNumber: courierNumberRecord.courierForRemove,
          depotName: depotNameRecord.castlePeach,
          date: moment(time.TIME_TODAY).add(-1, 'months').set('date', 15).format('YYYY-MM-DD'),
          number: routeNumberRecord.MONTH_AGO,
        },
        orders: [
          {
            data: {
              address: 'Россия, Санкт-Петербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: '900',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '12:00 - 18:00',
            },
            updateStatus: {
              status: OrderStatusesEnum.finished,
            },
          },
        ],
      },
      {
        data: {
          courierNumber: courierNumberRecord.courierForRemove,
          depotName: depotNameRecord.castlePeach,
          date: moment(time.TIME_TODAY).add(-2, 'days').format('YYYY-MM-DD'),
          number: routeNumberRecord.COURIER_REMOVE,
          routingMode: RoutingModeEnum.DRIVING,
        },
        orders: [
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: 'courier-remove',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '12:00 - 18:00',
            },
          },
        ],
      },
      {
        data: {
          courierNumber: courierNumberRecord.gumba,
          depotName: depotNameRecord.castlePeach,
          date: moment(time.TIME_TODAY).add(-2, 'days').format('YYYY-MM-DD'),
          number: routeNumberRecord.BEFORE_YESTERDAY,
          routingMode: RoutingModeEnum.WALKING,
        },
        orders: [
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: '1',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '12:00 - 18:00',
            },
          },
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: '2',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '12:00 - 18:00',
            },
          },
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: '3',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '12:00 - 18:00',
            },
          },
        ],
      },
      {
        data: {
          courierNumber: courierNumberRecord.gumba,
          depotName: depotNameRecord.castlePeach,
          date: moment(time.TIME_TODAY).add(-1, 'days').format('YYYY-MM-DD'),
          number: routeNumberRecord.YESTERDAY,
          routingMode: RoutingModeEnum.TRANSIT,
        },
        orders: [
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: '4',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '12:00 - 18:00',
              status: 'cancelled',
            },
          },
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: '5',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '12:00 - 18:00',
              status: 'cancelled',
            },
          },
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: '6',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '12:00 - 18:00',
              status: 'cancelled',
            },
          },
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: '7',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '12:00 - 18:00',
            },
          },
        ],
      },
      {
        data: {
          courierNumber: courierNumberRecord.gumba,
          depotName: depotNameRecord.castlePeach,
          date: moment(time.TIME_TODAY).add(0, 'days').format('YYYY-MM-DD'),
          number: routeNumberRecord.TODAY,
          routingMode: RoutingModeEnum.TRUCK,
        },
        orders: [
          {
            data: {
              address: 'Греция, Китнос, Хора 3',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: 'fullAddressSearchTest',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '12:00 - 18:00',
            },
          },
          {
            data: {
              address: 'Россия, Москва, Кутузовский проспект, 1',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: 'citySearchTest',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '12:00 - 18:00',
            },
          },
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: 'phoneNumberTest',
              payment_type: 'cash',
              phone: '+70002222222',
              time_interval: '12:00 - 18:00',
            },
          },
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Shy Guy',
              lat: 60,
              lon: 31,
              number: '11',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '12:00 - 18:00',
            },
          },
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Казанская, 5',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60.1,
              lon: 31.1,
              number: 'streetSearchTest',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '12:00 - 18:00',
            },
          },
        ],
      },
      {
        data: {
          courierNumber: courierNumberRecord.gumba,
          depotName: depotNameRecord.castlePeach,
          date: moment(time.TIME_TODAY).add(1, 'days').format('YYYY-MM-DD'),
          number: routeNumberRecord.TOMORROW,
          routingMode: RoutingModeEnum.DRIVING,
        },
        orders: [
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: '13',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '12:00 - 18:00',
            },
          },
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: '14',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '12:00 - 18:00',
            },
          },
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: '15',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '12:00 - 18:00',
            },
          },
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: '16',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '12:00 - 18:00',
            },
          },
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: '17',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '12:00 - 18:00',
            },
          },
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: '18',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '12:00 - 18:00',
            },
          },
        ],
      },
      {
        data: {
          courierNumber: courierNumberRecord.gumba,
          depotName: depotNameRecord.castlePeach,
          date: moment(time.TIME_TODAY).add(2, 'days').format('YYYY-MM-DD'),
          number: routeNumberRecord.AFTER_TOMORROW,
          routingMode: RoutingModeEnum.TRANSIT,
        },
        orders: [
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: '19',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '12:00 - 18:00',
            },
          },
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: '20',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '12:00 - 18:00',
            },
          },
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: '21',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '12:00 - 18:00',
            },
          },
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: '22',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '12:00 - 18:00',
            },
          },
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: '23',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '12:00 - 18:00',
            },
          },
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: '24',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '12:00 - 18:00',
            },
          },
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: '25',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '12:00 - 18:00',
            },
          },
        ],
      },
      {
        data: {
          courierNumber: courierNumberRecord.gumba,
          depotName: depotNameRecord.castlePeach,
          date: moment(time.TIME_TODAY).add(3, 'days').format('YYYY-MM-DD'),
          number: routeNumberRecord.BIG_ORDER_NUMBER,
          routingMode: RoutingModeEnum.DRIVING,
        },
        orders: [
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: '26',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '12:00 - 18:00',
            },
          },
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: '27',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '12:00 - 18:00',
            },
          },
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: '28',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '12:00 - 18:00',
            },
          },
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: '29',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '12:00 - 18:00',
            },
          },
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: '30',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '12:00 - 18:00',
            },
          },
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: '31',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '12:00 - 18:00',
            },
          },
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: '32',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '12:00 - 18:00',
            },
          },
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: '33',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '12:00 - 18:00',
            },
          },
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: '34',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '12:00 - 18:00',
            },
          },
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: '35',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '12:00 - 18:00',
            },
          },
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: '36',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '12:00 - 18:00',
            },
          },
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: '37',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '12:00 - 18:00',
            },
          },
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: '38',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '12:00 - 18:00',
            },
          },
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: '39',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '12:00 - 18:00',
            },
          },
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: '40',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '12:00 - 18:00',
            },
          },
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: '41',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '12:00 - 18:00',
            },
          },
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: '42',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '12:00 - 18:00',
            },
          },
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: '43',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '12:00 - 18:00',
            },
          },
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: '44',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '12:00 - 18:00',
            },
          },
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: '45',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '12:00 - 18:00',
            },
          },
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: '46',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '12:00 - 18:00',
            },
          },
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: '47',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '12:00 - 18:00',
            },
          },
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: '48',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '12:00 - 18:00',
            },
          },
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: '49',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '12:00 - 18:00',
            },
          },
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: '50',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '12:00 - 18:00',
            },
          },
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: '51',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '12:00 - 18:00',
            },
          },
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: '52',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '12:00 - 18:00',
            },
          },
        ],
      },
      {
        data: {
          courierNumber: courierNumberRecord.kypa,
          depotName: depotNameRecord.castlePeach,
          date: moment(time.TIME_TODAY).add(0, 'days').format('YYYY-MM-DD'),
          number: routeNumberRecord.DIFFERENT_STATUSES,
          routingMode: RoutingModeEnum.TRANSIT,
        },
        orders: [
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              weight: 10,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: 'TESTnew',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '00:00 - 23:59',
            },
            updateStatus: {
              status: OrderStatusesEnum.new,
            },
          },
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              weight: 10,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: 'TESTconfirmed',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '00:00 - 23:59',
              comments: 'Не кантовать!',
            },
            updateStatus: {
              status: OrderStatusesEnum.confirmed,
            },
          },
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              weight: 10,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: 'TESTfinished',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '00:00 - 23:59',
              comments: 'Не кантовать!',
            },
            updateStatus: {
              status: OrderStatusesEnum.finished,
            },
          },
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              weight: 10,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: 'TESTcancelled',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '12:00 - 18:00',
              comments: 'Не кантовать!',
            },
            updateStatus: {
              status: OrderStatusesEnum.cancelled,
            },
          },
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              weight: 10,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: 'TESTpostponed',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '12:00 - 18:00',
            },
            updateStatus: {
              status: OrderStatusesEnum.postponed,
            },
          },
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              weight: 10,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: 'extraTESTfinished',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '00:00 - 00:01',
              comments: 'Не кантовать!',
            },
            updateStatus: {
              status: OrderStatusesEnum.finished,
            },
          },
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              weight: 10,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: 'extraTESTconfirmed',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '00:00 - 00:01',
              comments: 'Не кантовать!',
            },
            updateStatus: {
              status: OrderStatusesEnum.confirmed,
            },
          },
        ],
      },
      {
        data: {
          courierNumber: courierNumberRecord.gumba,
          depotName: depotNameRecord.toadsTent,
          date: moment(time.TIME_TODAY).add(0, 'days').format('YYYY-MM-DD'),
          number: routeNumberRecord.TWO_ADDITIONAL_ORDERS_TODAY,
          routingMode: RoutingModeEnum.DRIVING,
        },
        orders: [
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: 'additional-1',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '00:00 - 00:01',
            },
          },
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: 'additional-2',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '00:00 - 00:01',
            },
          },
        ],
      },
      {
        data: {
          courierNumber: courierNumberRecord.gumba,
          depotName: depotNameRecord.toadsTent,
          date: moment(time.TIME_TODAY).add(0, 'days').format('YYYY-MM-DD'),
          number: routeNumberRecord.TWO_ADDITIONAL_ORDERS_TODAY + 'postfix',
          routingMode: RoutingModeEnum.WALKING,
        },
        orders: [
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: 'additional-5',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '00:00 - 00:01',
            },
          },
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: 'additional-6',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '00:00 - 00:01',
            },
          },
        ],
      },
      {
        data: {
          courierNumber: courierNumberRecord.gumba,
          depotName: depotNameRecord.toadsTent,
          date: moment(time.TIME_TODAY).add(1, 'days').format('YYYY-MM-DD'),
          number: routeNumberRecord.TWO_ADDITIONAL_ORDERS_TOMORROW,
          routingMode: RoutingModeEnum.WALKING,
        },
        orders: [
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: 'additional-3',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '00:00 - 00:01',
            },
          },
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: 'additional-4',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '00:00 - 00:01',
            },
          },
        ],
      },
    ],
    zones: [
      {
        data: {
          number: 'Zone #790',
          color_edge: '#002D89',
          color_fill: '#002D89',
          polygon: {
            type: 'Polygon',
            coordinates: [
              [
                [55.79898423791616, 37.37948059082029],
                [55.74479666447047, 37.372614135742175],
                [55.69906550849885, 37.40145324707028],
                [55.73549976896077, 37.452265014648425],
                [55.79898423791616, 37.37948059082029],
              ],
            ],
          },
        },
      },
      {
        data: {
          number: 'Zone #791',
          color_edge: '#BD9700',
          color_fill: '#BD9700',
          polygon: {
            type: 'Polygon',
            coordinates: [
              [
                [55.712247798068034, 37.389093627929675],
                [55.71767450165061, 37.49346374511716],
                [55.65095106811855, 37.44677185058591],
                [55.712247798068034, 37.389093627929675],
              ],
            ],
          },
        },
      },
      {
        data: {
          number: 'Zone #792',
          color_edge: '#FF03A9',
          color_fill: '#FF03A9',
          polygon: {
            type: 'Polygon',
            coordinates: [
              [
                [55.64706828142592, 37.45363830566405],
                [55.6618208140504, 37.53191589355467],
                [55.61521484413764, 37.55800842285156],
                [55.59422398684426, 37.51131652832031],
                [55.64706828142592, 37.45363830566405],
              ],
            ],
          },
        },
      },
      {
        data: {
          number: 'Zone #793',
          color_edge: '#FF781E',
          color_fill: '#FF781E',
          polygon: {
            type: 'Polygon',
            coordinates: [
              [
                [55.59733445536619, 37.51131652832031],
                [55.611328499454935, 37.5772344970703],
                [55.60005591218282, 37.64521240234373],
                [55.57263829254826, 37.66821502685545],
                [55.57477794755588, 37.603327026367154],
                [55.59733445536619, 37.51131652832031],
              ],
            ],
          },
        },
      },
      {
        data: {
          number: 'Zone #794',
          color_edge: '#002D89',
          color_fill: '#002D89',
          polygon: {
            type: 'Polygon',
            coordinates: [
              [
                [55.577889964005976, 37.60744689941405],
                [55.60977385326631, 37.625299682617175],
                [55.61521484413764, 37.709070434570286],
                [55.591890972930365, 37.72657989501946],
                [55.57633398673154, 37.687784423828056],
                [55.57166568339832, 37.66375183105466],
                [55.577889964005976, 37.60744689941405],
              ],
            ],
          },
        },
      },
      {
        data: {
          number: 'Zone #795',
          color_edge: '#03C7DD',
          color_fill: '#03C7DD',
          polygon: {
            type: 'Polygon',
            coordinates: [
              [
                [55.59577925205347, 37.73516296386717],
                [55.58255752425309, 37.70357727050779],
                [55.61443760614772, 37.69121765136717],
                [55.66570213165896, 37.74477600097656],
                [55.62454049317273, 37.79421447753905],
                [55.59577925205347, 37.73516296386717],
              ],
            ],
          },
        },
      },
      {
        data: {
          number: 'Zone #796',
          color_edge: '#4DFFA7',
          color_fill: '#4DFFA7',
          polygon: {
            type: 'Polygon',
            coordinates: [
              [
                [55.60122219278474, 37.7475225830078],
                [55.65017454171115, 37.702203979492175],
                [55.68510292186746, 37.82992004394529],
                [55.66026817876812, 37.839533081054675],
                [55.64396177368366, 37.825800170898425],
                [55.60122219278474, 37.7475225830078],
              ],
            ],
          },
        },
      },
      {
        data: {
          number: `Zone #2091 (Don't pick me I will be deleted)`,
          color_edge: '#002D89',
          color_fill: '#002D89',
          polygon: {
            type: 'Polygon',
            coordinates: [
              [
                [55.877035910064905, 37.42479919433592],
                [55.90944673782252, 37.556635131835904],
                [55.87163146199932, 37.58135437011717],
                [55.843052542960905, 37.38772033691405],
                [55.877035910064905, 37.42479919433592],
              ],
            ],
          },
        },
      },
      {
        data: {
          number: `Zone #2092 (Don't pick me I will be deleted)`,
          color_edge: '#9703A9',
          color_fill: '#9703A9',
          polygon: {
            type: 'Polygon',
            coordinates: [
              [
                [55.910218092865726, 37.59096740722655],
                [55.881667692909126, 37.72692321777341],
                [55.850778656897674, 37.78734802246091],
                [55.870859336323996, 37.585474243164036],
                [55.910218092865726, 37.59096740722655],
              ],
            ],
          },
        },
      },
      {
        data: {
          number: 'Zone #2093',
          color_edge: '#FF73CB',
          color_fill: '#FF73CB',
          polygon: {
            type: 'Polygon',
            coordinates: [
              [
                [55.852795595080714, 37.78638561576258],
                [55.82961383424189, 37.828957637246965],
                [55.76385676813473, 37.8413172563876],
                [55.810285094950835, 37.71360119193445],
                [55.852795595080714, 37.78638561576258],
              ],
            ],
          },
        },
      },
    ],
  },
  [CompanySlugEnum.someNumbers1]: {
    data: {
      name: additionalCompaniesNames.someNumbers1,
      initial_login: accounts.testCompanyManager0,
      services: [
        {
          enabled: true,
          name: 'courier',
        },
      ],
    },
  },
  [CompanySlugEnum.someNumbers2]: {
    data: {
      name: additionalCompaniesNames.someNumbers2,
      initial_login: accounts.testCompanyManager1,
      services: [
        {
          enabled: true,
          name: 'courier',
        },
      ],
    },
  },
  [CompanySlugEnum.someNumbers3]: {
    data: {
      name: additionalCompaniesNames.someNumbers3,
      initial_login: accounts.testCompanyManager2,
      services: [
        {
          enabled: true,
          name: 'courier',
        },
      ],
    },
  },
  [CompanySlugEnum.someLowerCase]: {
    data: {
      name: additionalCompaniesNames.someLowerCase,
      initial_login: accounts.testCompanyManager3,
      services: [
        {
          enabled: true,
          name: 'courier',
        },
      ],
    },
  },
  [CompanySlugEnum.someUpperCase]: {
    data: {
      name: additionalCompaniesNames.someUpperCase,
      initial_login: accounts.testCompanyManager4,
      services: [
        {
          enabled: true,
          name: 'courier',
        },
      ],
    },
  },
  [CompanySlugEnum.someUpperLowerCase]: {
    data: {
      name: additionalCompaniesNames.someUpperLowerCase,
      initial_login: accounts.testCompanyManager5,
      services: [
        {
          enabled: true,
          name: 'courier',
        },
      ],
    },
  },
  [CompanySlugEnum.someEnglishLowerCase]: {
    data: {
      name: additionalCompaniesNames.someEnglishLowerCase,
      initial_login: accounts.testCompanyManager6,
      services: [
        {
          enabled: true,
          name: 'courier',
        },
      ],
    },
  },
  [CompanySlugEnum.someEnglishUpperCase]: {
    data: {
      name: additionalCompaniesNames.someEnglishUpperCase,
      initial_login: accounts.testCompanyManager7,
      services: [
        {
          enabled: true,
          name: 'courier',
        },
      ],
    },
  },
  [CompanySlugEnum.someRussian]: {
    data: {
      name: additionalCompaniesNames.someRussian,
      initial_login: accounts.testCompanyManager8,
      services: [
        {
          enabled: true,
          name: 'courier',
        },
      ],
    },
  },
  [CompanySlugEnum.A]: {
    data: {
      name: companyNameRecord.A,
      initial_login: accounts.companyAadmin,
      sms_enabled: false,
      services: [
        {
          enabled: true,
          name: 'courier',
        },
      ],
    },
    depots: [
      {
        data: {
          address: 'Россия, Санкт-Петербург, Пискаревский проспект, 2к2',
          description: 'Тестовый склад',
          lat: 59.458606,
          lon: 30.405313,
          name: depotNameRecord.compAVisible,
          number: depotNumberRecord.compAVisible,
          order_service_duration_s: 0,
          service_duration_s: 0,
          time_interval: '09:00 - 20:00',
        },
        users: [],
      },
      {
        data: {
          address: 'Россия, Санкт-Петербург, Пискаревский проспект, 2к2',
          description: 'Тестовый склад',
          lat: 59.943557,
          lon: 30.931711,
          name: depotNameRecord.compANotVisible,
          number: depotNumberRecord.compANotVisible,
          order_service_duration_s: 0,
          service_duration_s: 0,
          time_interval: '09:00 - 20:00',
        },
        users: [],
      },
    ],
    couriers: [
      {
        data: {
          name: courierNameRecord.red,
          number: courierNumberRecord.red,
          phone: '+7(000)0000001',
          sms_enabled: true,
        },
      },
      {
        data: {
          name: courierNameRecord.pink,
          number: courierNumberRecord.pink,
          phone: '+7(000)0000002',
          sms_enabled: false,
        },
      },
    ],

    routes: [
      {
        data: {
          courierNumber: courierNumberRecord.red,
          depotName: depotNameRecord.compAVisible,
          date: moment(time.TIME_TODAY).add(0, 'days').format('YYYY-MM-DD'),
          number: sharedRouteNumberRecord.AToB,
        },
        orders: [
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              shared_with_company_slug: CompanySlugEnum.B,
              number: 'order-shared-A',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '00:01 - 23:59',
            },
          },
        ],
      },
    ],
  },
  [CompanySlugEnum.B]: {
    data: {
      name: companyNameRecord.B,
      initial_login: accounts.companyBadmin,
      sms_enabled: false,
      services: [
        {
          enabled: true,
          name: 'courier',
        },
      ],
    },
    users: [
      {
        data: {
          login: accounts.companyBmanager,
          role: 'manager',
        },
        sharedToCompany: [CompanySlugEnum.A],
      },
    ],
    depots: [
      {
        data: {
          address: 'Россия, Санкт-Петербург, Пискаревский проспект, 2к2',
          description: 'Тестовый склад',
          lat: 59.458606,
          lon: 30.405313,
          name: depotNameRecord.compBVisible,
          number: depotNumberRecord.compBVisible,
          order_service_duration_s: 0,
          service_duration_s: 0,
          time_interval: '09:00 - 20:00',
        },
        users: [],
      },
      {
        data: {
          address: 'Россия, Санкт-Петербург, Пискаревский проспект, 2к2',
          description: 'Тестовый склад',
          lat: 59.943557,
          lon: 30.931711,
          name: depotNameRecord.compBNotVisible,
          number: depotNumberRecord.compBNotVisible,
          order_service_duration_s: 0,
          service_duration_s: 0,
          time_interval: '09:00 - 20:00',
        },
        users: [],
      },
    ],
    couriers: [
      {
        data: {
          name: courierNameRecord.green,
          number: courierNumberRecord.green,
          phone: '+7(000)0000001',
          sms_enabled: true,
        },
      },
      {
        data: {
          name: courierNameRecord.lightGreen,
          number: courierNumberRecord.lightGreen,
          phone: '+7(000)0000002',
          sms_enabled: false,
        },
      },
    ],

    routes: [
      {
        data: {
          courierNumber: courierNumberRecord.green,
          depotName: depotNameRecord.compBVisible,
          date: moment(time.TIME_TODAY).add(0, 'days').format('YYYY-MM-DD'),
          number: sharedRouteNumberRecord.BToV,
        },
        orders: [
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              shared_with_company_slug: CompanySlugEnum.V,
              number: 'order-shared-B',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '00:01 - 23:59',
            },
          },
        ],
      },
    ],
  },
  [CompanySlugEnum.V]: {
    data: {
      name: companyNameRecord.V,
      initial_login: accounts.companyVadmin,
      sms_enabled: false,
      services: [
        {
          enabled: true,
          name: 'courier',
        },
      ],
    },
    users: [
      {
        data: {
          login: accounts.companyVmanager,
          role: 'manager',
        },
        sharedToCompany: [CompanySlugEnum.B],
      },
    ],
    depots: [
      {
        data: {
          address: 'Россия, Санкт-Петербург, Пискаревский проспект, 2к2',
          description: 'Тестовый склад',
          lat: 59.458606,
          lon: 30.405313,
          name: depotNameRecord.compVVisible,
          number: depotNumberRecord.compVVisible,
          order_service_duration_s: 0,
          service_duration_s: 0,
          time_interval: '09:00 - 20:00',
        },
        users: [],
      },
      {
        data: {
          address: 'Россия, Санкт-Петербург, Пискаревский проспект, 2к2',
          description: 'Тестовый склад',
          lat: 59.943557,
          lon: 30.931711,
          name: depotNameRecord.compVNotVisible,
          number: depotNumberRecord.compVNotVisible,
          order_service_duration_s: 0,
          service_duration_s: 0,
          time_interval: '09:00 - 20:00',
        },
        users: [],
      },
    ],
    couriers: [
      {
        data: {
          name: courierNameRecord.orange,
          number: courierNumberRecord.orange,
          phone: '+7(000)0000001',
          sms_enabled: true,
        },
      },
      {
        data: {
          name: courierNameRecord.сarrot,
          number: courierNumberRecord.сarrot,
          phone: '+7(000)0000002',
          sms_enabled: false,
        },
      },
    ],
    routes: [
      {
        data: {
          courierNumber: courierNumberRecord.orange,
          depotName: depotNameRecord.compVVisible,
          date: moment(time.TIME_TODAY).add(0, 'days').format('YYYY-MM-DD'),
          number: sharedRouteNumberRecord.BToV,
        },
        orders: [
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              shared_with_company_slug: CompanySlugEnum.shareFromOthersCompany,
              number: 'order-shared-V1',
              payment_type: 'cash',
              phone: '+70000000000',
              time_interval: '00:01 - 23:59',
            },
          },
        ],
      },
    ],
  },
  [CompanySlugEnum.G]: {
    data: {
      name: companyNameRecord.G,
      initial_login: accounts.companyGadmin,
      sms_enabled: false,
      services: [
        {
          enabled: true,
          name: 'courier',
        },
      ],
    },
    depots: [
      {
        data: {
          address: 'Россия, Санкт-Петербург, Пискаревский проспект, 2к2',
          description: 'Тестовый склад',
          lat: 59.458606,
          lon: 30.405313,
          name: depotNameRecord.compGVisible,
          number: depotNumberRecord.compGVisible,
          order_service_duration_s: 0,
          service_duration_s: 0,
          time_interval: '09:00 - 20:00',
        },
        users: [],
      },
      {
        data: {
          address: 'Россия, Санкт-Петербург, Пискаревский проспект, 2к2',
          description: 'Тестовый склад',
          lat: 59.943557,
          lon: 30.931711,
          name: depotNameRecord.compGNotVisible,
          number: depotNumberRecord.compGNotVisible,
          order_service_duration_s: 0,
          service_duration_s: 0,
          time_interval: '09:00 - 20:00',
        },
        users: [],
      },
    ],
    couriers: [
      {
        data: {
          name: courierNameRecord.blue,
          number: courierNumberRecord.blue,
          phone: '+7(000)0000001',
          sms_enabled: true,
        },
      },
      {
        data: {
          name: courierNameRecord.lightBlue,
          number: courierNumberRecord.lightBlue,
          phone: '+7(000)0000002',
          sms_enabled: false,
        },
      },
    ],
  },
  [CompanySlugEnum.D]: {
    data: {
      name: companyNameRecord.D,
      initial_login: accounts.companyDadmin,
      sms_enabled: false,
      services: [
        {
          enabled: true,
          name: 'courier',
        },
      ],
    },
    depots: [
      {
        data: {
          address: 'Россия, Санкт-Петербург, Пискаревский проспект, 2к2',
          description: 'Тестовый склад',
          lat: 59.458606,
          lon: 30.405313,
          name: depotNameRecord.compDVisible,
          number: depotNumberRecord.compDVisible,
          order_service_duration_s: 0,
          service_duration_s: 0,
          time_interval: '09:00 - 20:00',
        },
        users: [],
      },
      {
        data: {
          address: 'Россия, Санкт-Петербург, Пискаревский проспект, 2к2',
          description: 'Тестовый склад',
          lat: 59.943557,
          lon: 30.931711,
          name: depotNameRecord.compDNotVisible,
          number: depotNumberRecord.compDNotVisible,
          order_service_duration_s: 0,
          service_duration_s: 0,
          time_interval: '09:00 - 20:00',
        },
        users: [],
      },
    ],
    couriers: [
      {
        data: {
          name: courierNameRecord.brown,
          number: courierNumberRecord.brown,
          phone: '+7(000)0000001',
          sms_enabled: true,
        },
      },
      {
        data: {
          name: courierNameRecord.vinous,
          number: courierNumberRecord.vinous,
          phone: '+7(000)0000002',
          sms_enabled: false,
        },
      },
    ],
  },
  [CompanySlugEnum.E]: {
    data: {
      name: companyNameRecord.E,
      initial_login: accounts.companyEadmin,
      sms_enabled: false,
      services: [
        {
          enabled: true,
          name: 'courier',
        },
      ],
    },
    users: [
      {
        data: {
          login: accounts.companyEmanager,
          role: 'manager',
        },
      },
    ],
    depots: [
      {
        data: {
          address: 'Россия, Санкт-Петербург, Пискаревский проспект, 2к2',
          description: 'Тестовый склад',
          lat: 59.458606,
          lon: 30.405313,
          name: depotNameRecord.compEVisible,
          number: depotNumberRecord.compEVisible,
          order_service_duration_s: 0,
          service_duration_s: 0,
          time_interval: '09:00 - 20:00',
        },
        users: [
          {
            data: {
              login: accounts.companyEmanager,
            },
          },
        ],
      },
    ],
    couriers: [
      {
        data: {
          name: courierNameRecord.courierWithSingleRoute,
          number: courierNumberRecord.courierWithSingleRoute,
          phone: courierNumberRecord.courierWithSingleRoute,
          sms_enabled: true,
        },
      },
    ],

    routes: [
      {
        data: {
          courierNumber: courierNumberRecord.courierWithSingleRoute,
          depotName: depotNameRecord.compEVisible,
          date: time.TIME_TODAY,
          number: routeNumberRecord.TODAY,
        },
        orders: [
          {
            data: {
              address: 'Россия, Санкт-Пеетербург, Пискаревский проспект, 2к2',
              amount: 100,
              customer_name: 'Bowser',
              lat: 60,
              lon: 31,
              number: '555',
              payment_type: 'cash',
              phone: '+70000000000',
              shared_with_company_slug: CompanySlugEnum.shareFromOthersCompany,
              time_interval: '10:00 - 18:00',
            },
          },
        ],
      },
    ],
  },
  [CompanySlugEnum.shareFromOthersCompany]: {
    data: {
      name: companyNameRecord.shareFromOthersCompany,
      initial_login: accounts.adminMulti,
      sms_enabled: false,
      services: [
        {
          enabled: true,
          name: 'courier',
        },
      ],
    },
    users: [
      {
        data: {
          login: accounts.managerMulti,
          role: 'admin',
        },
        sharedToCompany: [CompanySlugEnum.E],
      },
    ],
    couriers: [
      {
        data: {
          name: courierNameRecord.courierWithSingleRoute,
          number: courierNumberRecord.courierWithSingleRoute,
          phone: courierNumberRecord.courierWithSingleRoute,
          sms_enabled: true,
        },
      },
    ],
    depots: generateDepots(10),
  },
};

export const commonCompaniesSchema: GenerateSchemaT<CommonCompanySlugEnum> = {
  [CommonCompanySlugEnum.appRole]: {
    data: {
      name: `YTEST-App_role`,
      initial_login: accounts.appRoleAdmin,
      sms_enabled: true,
      mark_delivered_enabled: false,
      services: [
        {
          enabled: true,
          name: 'courier',
        },
      ],
    },
    users: [
      {
        data: {
          login: accounts.appRoleManager,
          role: 'manager',
        },
      },
      ...appUsers.map(newUser => ({
        data: newUser,
      })),
    ],
  },
};
