import { HOST } from 'common/utils/test-utils/common';

export const intercompanies = {
    request: {
        url: `${HOST}/firm/intercompany_list`
    },
    response: [
        { cc: 'AM31', is_active: true, label: 'Yandex.Taxi AM' },
        { cc: 'AM32', is_active: true, label: 'Yandex.Taxi Corp AM' },
        { cc: 'AZ35', is_active: true, label: 'Uber Azerbaijan' }
    ]
};

export const clients = {
    request: [
        `${HOST}/client/list`,
        {
            agency_select_policy: 'ALL',
            hide_managers: true,
            is_accurate: false,
            login: 'yb-adm',
            pagination_pn: 1,
            pagination_ps: 10
        },
        false,
        false
    ],
    response: {
        version: {
            snout: '1.0.218',
            muzzle: '2.198.41',
            butils: '2.149'
        },
        data: {
            items: [
                {
                    iso_currency_payment: null,
                    managers: [],
                    name: null,
                    phone: null,
                    url: null,
                    creation_dt: '2019-07-24T12:42:46',
                    is_agency: false,
                    partner_type: 0,
                    manual_suspect: false,
                    email: null,
                    id: 108361080
                },
                {
                    iso_currency_payment: null,
                    managers: [],
                    name: 'dfghdgh',
                    phone: null,
                    url: 'zhur@ya.ru',
                    creation_dt: '2007-10-24T17:57:16',
                    is_agency: false,
                    partner_type: 0,
                    manual_suspect: false,
                    email: 'zhur@ya.ru',
                    id: 496740
                },
                {
                    iso_currency_payment: null,
                    managers: [],
                    name: 'Тестовый Аккаунт',
                    phone: null,
                    url: 'yb-adm3@yandex.ru',
                    creation_dt: '2007-10-24T17:53:22',
                    is_agency: false,
                    partner_type: 0,
                    manual_suspect: false,
                    email: 'yb-adm3@yandex.ru',
                    id: 496738
                }
            ],
            row_count: 3,
            total_count: 3
        }
    }
};

export const requests = {
    request: [
        `${HOST}/invoice/requests`,
        {
            from_dt: '2019-07-24T00:00:00',
            to_dt: '2019-07-25T00:00:00',
            client_id: 496738,
            request_id: '123',
            sort_key: 'DT',
            sort_order: 'DESC',
            pagination_pn: 1,
            pagination_ps: 10
        },
        false,
        false
    ],
    response: {
        version: {
            snout: '1.0.224',
            muzzle: '2.200.9',
            butils: '2.149'
        },
        data: {
            total_row_count: 56575,
            items: [
                {
                    oper_login: 'test-agency-rub',
                    client_name: 'тестовое агентство (rub)',
                    oper_name: 'Pupkin Vasily',
                    oper_id: 217695901,
                    client_id: 2981255,
                    request_id: 1859231755,
                    dt: '2019-07-25T18:25:31'
                },
                {
                    oper_login: 'grapl1',
                    client_name: 'grapl1',
                    oper_name: 'a grapl1',
                    oper_id: 4026109922,
                    client_id: 108248018,
                    request_id: 1859231743,
                    dt: '2019-07-25T18:24:32'
                },
                {
                    oper_login: 'pds',
                    client_name: 'pds',
                    oper_name: 'd pds',
                    oper_id: 4026204356,
                    client_id: 108289785,
                    request_id: 1859231740,
                    dt: '2019-07-25T18:24:16'
                }
            ]
        }
    }
};
