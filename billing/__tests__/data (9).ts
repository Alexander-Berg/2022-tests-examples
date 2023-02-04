import { HOST } from 'common/utils/test-utils/common';

export const history = {
    perms: ['NewUIEarlyAdopter', 'AdminAccess'],
    search:
        '?dt_from=2018-06-12T00%3A00%3A00&dt_to=2018-06-14T00%3A00%3A00&agency_id=5028445&client_id=42889276&service_cc=adfox&payment_status=2&service_order_id=7-35354856&pn=1&ps=10&sf=order_dt&so=1',
    filter: {
        dtFrom: '2018-06-12T00:00:00',
        dtTo: '2018-06-14T00:00:00',
        agency: 'Netpeak',
        client: 'flagmanamur.ru',
        serviceOrderId: '7-35354856',
        paymentStatus: 'TURN_ON',
        service: 'adfox'
    }
};

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

export const services = {
    request: {
        url: `${HOST}/service/list`,
        data: {}
    },
    response: [
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
        },
        {
            cc: 'bug_bounty',
            url_orders: null,
            in_contract: true,
            id: 207,
            name: 'Bug bounty'
        }
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
                    id: 5028445
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
                    id: 42889276
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

export const orders = {
    request: [
        `${HOST}/order/list`,
        {
            agency_id: 5028445,
            client_id: 42889276,
            from_dt: '2018-06-12T00:00:00',
            pagination_pn: 1,
            pagination_ps: 10,
            payment_status: 'TURN_ON',
            service_cc: 'adfox',
            service_id: '7',
            service_order_id: '35354856',
            sort_key: 'ORDER_DT',
            sort_order: 'DESC',
            to_dt: '2018-06-14T00:00:00'
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
            orders_total: 3,
            order_list: [
                {
                    agency: 'ООО «Сало»',
                    root_order_service_order_id: null,
                    service_name: 'Дзен Продажи',
                    is_agency: false,
                    root_order_eid: '-',
                    tag: null,
                    service_order_id: 1234,
                    order_eid: '561-1234',
                    unit: 'у.е.',
                    type_rate: 1,
                    consumes_count: 0,
                    remain_sum: '0.00',
                    text: 'Контент-маркетинг в Дзене',
                    order_dt: '2019-07-10T18:23:52',
                    order_id: 1394249377,
                    agency_id: 56295407,
                    service_orders_url: null,
                    client_id: 62630490,
                    root_order_service_cc: null,
                    service_code: '509735',
                    remain_qty: '0.000000',
                    service_cc: 'zen_sales',
                    completion_qty: '0.000000',
                    product_fullname: 'Контент-маркетинг в Дзене',
                    client: 'Worx Russia',
                    service_id: 561,
                    consume_qty: '0.000000',
                    passport_id: '127449004'
                },
                {
                    agency: null,
                    root_order_service_order_id: null,
                    service_name: 'Яндекс.Взгляд',
                    is_agency: false,
                    root_order_eid: '-',
                    tag: null,
                    service_order_id: 1234,
                    order_eid: '103-1234',
                    unit: 'штуки',
                    type_rate: 1,
                    consumes_count: 1,
                    remain_sum: '14000.00',
                    text: 'Информационные услуги «Яндекс.Взгляд»',
                    order_dt: '2019-06-07T18:10:43',
                    order_id: 1356779036,
                    agency_id: null,
                    service_orders_url: null,
                    client_id: 61060619,
                    root_order_service_cc: null,
                    service_code: '508996',
                    remain_qty: '0.000000',
                    service_cc: 'surveys',
                    completion_qty: '200.000000',
                    product_fullname: 'Информационные услуги «Яндекс.Взгляд»',
                    client: 'ООО "РУФОРМ"',
                    service_id: 103,
                    consume_qty: '200.000000',
                    passport_id: '1130000037421782'
                },
                {
                    agency: null,
                    root_order_service_order_id: null,
                    service_name: 'Кабинет Разработчика',
                    is_agency: false,
                    root_order_eid: '-',
                    tag: null,
                    service_order_id: 1234,
                    order_eid: '129-1234',
                    unit: 'RUB',
                    type_rate: 1,
                    consumes_count: 1,
                    remain_sum: '1396000.00',
                    text:
                        'Минимальное вознаграждение за предоставление права использования Базы данных Яндекс.Карты посредством JS API за весь срок действия лицензии',
                    order_dt: '2019-05-17T14:21:03',
                    order_id: 1329873676,
                    agency_id: null,
                    service_orders_url: null,
                    client_id: 32159275,
                    root_order_service_cc: null,
                    service_code: '508206',
                    remain_qty: '0.000000',
                    service_cc: 'apikeys',
                    completion_qty: '1396000.000000',
                    product_fullname:
                        'Минимальное вознаграждение за предоставление права использования Базы данных Яндекс.Карты посредством JS API за весь срок действия лицензии',
                    client: 'Тинькофф Банк',
                    service_id: 129,
                    consume_qty: '1396000.000000',
                    passport_id: null
                }
            ],
            request: {
                ps: 20,
                order_dt_to: null,
                service_cc: null,
                payment_status: 0,
                order_dt_from: null,
                service_order_id: 1234,
                agency_id: null,
                client_id: null,
                pn: 1
            }
        }
    }
};

export const client42889276 = {
    request: [`${HOST}/client`, { client_id: 42889276 }, false, false],
    response: {
        version: { snout: 'UNKNOWN', muzzle: 'UNKNOWN', butils: 'UNKNOWN' },
        data: {
            manual_suspect_comment: null,
            overdraft_ban: false,
            direct25: false,
            region_id: 225,
            currency_payment: null,
            is_agency: false,
            parent_agencies: [{ id: 5028445, name: 'Netpeak' }],
            single_account_number: null,
            domain_check_comment: '',
            has_edo: false,
            intercompany: null,
            id: 42889276,
            printable_docs_type: 0,
            domain_check_status: 0,
            full_repayment: true,
            fraud_status: null,
            reliable_cc_payer: 0,
            'client-type': null,
            deny_overdraft: null,
            only_manual_name_update: null,
            manual_suspect: 0,
            internal: false,
            sms_notify: 2,
            type: { id: 0 },
            email: 'point.netpeak@gmail.com',
            is_acquiring: false,
            fax: '-',
            region_name: '\u0420\u043e\u0441\u0441\u0438\u044f',
            parent_agency_id: 5028445,
            city: '\u0421\u0430\u043c\u0430\u0440\u0430',
            deny_cc: 0,
            client_type_id: 0,
            is_non_resident: false,
            phone: '380948311520',
            name: 'flagmanamur.ru',
            partner_type: '0',
            url: '-',
            force_contractless_invoice: false,
            non_resident_currency_payment: null,
            parent_agency_name: 'Netpeak',
            fullname: null,
            is_ctype_3: false
        }
    }
};

export const client5028445 = {
    request: [`${HOST}/client`, { client_id: 5028445 }, false, false],
    response: {
        version: { snout: 'UNKNOWN', muzzle: 'UNKNOWN', butils: 'UNKNOWN' },
        data: {
            manual_suspect_comment: null,
            overdraft_ban: false,
            direct25: false,
            region_id: 225,
            currency_payment: null,
            is_agency: true,
            parent_agencies: [],
            single_account_number: null,
            domain_check_comment: null,
            has_edo: true,
            intercompany: null,
            id: 5028445,
            printable_docs_type: 0,
            domain_check_status: 0,
            full_repayment: true,
            fraud_status: null,
            reliable_cc_payer: 0,
            'client-type': null,
            deny_overdraft: null,
            only_manual_name_update: true,
            manual_suspect: 0,
            internal: false,
            sms_notify: 2,
            type: { id: 2 },
            email: 'v.krasko@netpeak.net',
            is_acquiring: null,
            fax: null,
            region_name: '\u0420\u043e\u0441\u0441\u0438\u044f',
            parent_agency_id: 5028445,
            city: '\u0421\u0430\u043c\u0430\u0440\u0430',
            deny_cc: 0,
            client_type_id: 2,
            is_non_resident: false,
            phone: '+38 063 80 40 690',
            name: 'Netpeak',
            partner_type: '0',
            url: 'http://netpeak.ua/',
            force_contractless_invoice: false,
            non_resident_currency_payment: null,
            parent_agency_name: 'Netpeak',
            fullname: null,
            is_ctype_3: false
        }
    }
};
