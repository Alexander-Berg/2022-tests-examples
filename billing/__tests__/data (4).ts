import { HOST } from 'common/utils/test-utils/common';
import { camelCasePropNames } from 'common/utils';

export const services = {
    request: {
        data: {},
        url: `${HOST}/service/list`
    },
    response: camelCasePropNames([
        {
            cc: 'adfox',
            url_orders: 'http://adfox.ru',
            in_contract: true,
            id: 102,
            name: 'ADFox.ru'
        },
        {
            cc: 'afisha_moviepass',
            url_orders: null,
            in_contract: true,
            id: 617,
            name: 'Afisha.MoviePass'
        }
    ])
};

export const enums = {
    request: {
        data: {
            enum_code: 'CONTRACTS'
        },
        url: `${HOST}/common/enums`
    },
    response: camelCasePropNames([
        {
            cc: 'adfox',
            url_orders: 'http://adfox.ru',
            in_contract: true,
            id: 102,
            name: 'ADFox.ru'
        },
        {
            cc: 'afisha_moviepass',
            url_orders: null,
            in_contract: true,
            id: 617,
            name: 'Afisha.MoviePass'
        }
    ])
};

export const intercompanies = {
    request: {
        url: `${HOST}/firm/intercompany_list`
    },
    response: camelCasePropNames([
        {
            cc: 'adfox',
            url_orders: 'http://adfox.ru',
            in_contract: true,
            id: 102,
            name: 'ADFox.ru'
        },
        {
            cc: 'afisha_moviepass',
            url_orders: null,
            in_contract: true,
            id: 617,
            name: 'Afisha.MoviePass'
        }
    ])
};

export const personTypes = {
    request: {
        url: `${HOST}/person/category/list`
    },
    response: camelCasePropNames([
        {
            category: 'am_jp',
            name: 'ID_Legal_entity_AM'
        },
        {
            category: 'am_np',
            name: 'ID_Individual_AM'
        },
        {
            category: 'az_ur',
            name: 'ID_Legal_entity_AZ'
        }
    ])
};

export const contracts = {
    request: {
        data: {
            agency_id: 496740,
            client_id: 496740,
            commission: '1',
            contract_eid: '1234',
            contract_eid_like: false,
            date_type: 'DT',
            dt_from: '2020-06-02T00:00:00',
            dt_to: '2020-06-03T00:00:00',
            pagination_pn: 1,
            pagination_ps: 10,
            person_id: 58383,
            service_id: 'apiKeys',
            sort_key: 'DT',
            sort_order: 'ASC'
        },
        url: `${HOST}/contract/list`
    },
    response: camelCasePropNames({
        items: [
            {
                client_id: 7919406,
                is_suspended: null,
                manager_name: '\u0414\u0430\u043d\u0438\u043b\u0438',
                services: [
                    '\u0420\u0430\u0437\u043e\u0432\u044b\u0435',
                    '\u043f\u0440\u043e\u0434\u0430\u0436\u0438'
                ],
                contract_id: 234161,
                client_name: '\u0410\u041e "\u0410\u0421\u0422\u0415\u0420\u041e\u0421"',
                manager_code: 23643,
                is_signed: '2015-11-20T00:00:00',
                commission: 0,
                person_id: 2760627,
                is_booked_dt: null,
                firm: 1,
                finish_dt: null,
                person_name: '\u0410\u0421\u0422\u0415\u0420\u041e\u0421',
                contract_eid: 'ACT-2015/10/BAC00006464',
                is_booked: false,
                agency_id: null,
                agency_name: null,
                is_faxed: null,
                sent_dt: '2015-11-30T00:00:00',
                is_cancelled: null,
                dt: '2015-10-21T00:00:00',
                payment_type: 2
            },
            {
                client_id: 30367516,
                is_suspended: '2018-01-01T00:00:00',
                manager_name: '\u0411\u0430\u0445\u0448\u0438\u044f\u043d',
                services: [
                    '\u042f\u043d\u0434\u0435\u043a\u0441.\u0417\u0434\u043e\u0440\u043e\u0432\u044c\u0435',
                    '\u0420\u0430\u0437\u043e\u0432\u044b\u0435',
                    '\u043f\u0440\u043e\u0434\u0430\u0436\u0438'
                ],
                contract_id: 267550,
                client_name: '\u0417\u0410\u041e',
                manager_code: 28748,
                is_signed: '2016-12-23T00:00:00',
                commission: 0,
                person_id: 3651747,
                is_booked_dt: null,
                firm: 1,
                finish_dt: null,
                person_name: '\u042e\u041c\u0421',
                contract_eid: 'AG 102/16',
                is_booked: false,
                agency_id: null,
                agency_name: null,
                is_faxed: null,
                sent_dt: '2016-12-26T00:00:00',
                is_cancelled: null,
                dt: '2016-12-01T00:00:00',
                payment_type: 3
            }
        ],
        total_row_count: 2
    })
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

export const persons = {
    request: [
        `${HOST}/person/list`,
        {
            pagination_pn: 1,
            pagination_ps: 10,
            person_id: 1337,
            vip_only: false
        },
        false,
        false
    ],
    response: {
        version: {
            snout: '1.0.221',
            muzzle: 'UNKNOWN',
            butils: '2.149'
        },
        data: {
            total_row_count: 1,
            items: [
                {
                    kpp: null,
                    invoice_count: 0,
                    name: 'Quarta-Networks, Inc.',
                    client_name: 'Comstar',
                    email: null,
                    inn: null,
                    client_id: 3323,
                    is_partner: false,
                    hidden: true,
                    type: 'ur',
                    id: 58384
                },
                {
                    kpp: null,
                    invoice_count: 0,
                    name: 'Web Design, Ltd',
                    client_name: 'Comstar',
                    email: null,
                    inn: null,
                    client_id: 3323,
                    is_partner: false,
                    hidden: true,
                    type: 'ur',
                    id: 58383
                }
            ]
        }
    }
};
