import { HOST } from 'common/utils/test-utils/common';

import { filterValues } from './constants';

export const services = {
    request: {
        url: `${HOST}/service/list`,
        data: {}
    },
    response: [
        {
            cc: 'adfox',
            urlOrders: 'http://adfox.ru',
            inContract: true,
            id: 102,
            name: 'ADFox.ru'
        },
        {
            cc: 'afisha_moviepass',
            urlOrders: null,
            inContract: true,
            id: 617,
            name: 'Afisha.MoviePass'
        }
    ]
};

export const firms = {
    request: {
        url: `${HOST}/firm/list`
    },
    response: [
        { id: -1, label: 'Фирма не определена' },
        { id: 1, label: 'ООО «Яндекс»' },
        { id: 2, label: 'ООО «Яндекс.Украина»' },
        { id: 3, label: 'ТОО «KazNet Media (КазНет Медиа)»' }
    ]
};

export const processings = {
    request: {
        url: `${HOST}/processing/list`
    },
    response: [
        { cc: 'yamoney_test21', id: 50001, name: 'Yandex.Money Schema-T21 (Prod. Passport)' },
        { cc: 'yamoney_test12', id: 50002, name: 'Yandex.Money Schema-T12 (Test Passport)' },
        {
            cc: 'yamoney_test12_mc',
            id: 50004,
            name: 'Yandex.Money Fake Mobile Commerce (Test Passport)'
        }
    ]
};

export const paymentMethods = {
    request: {
        url: `${HOST}/payment_method/list`
    },
    response: [
        { cc: 'bank', id: 1001, name: 'Bank Payment' },
        { cc: 'card', id: 1101, name: 'Credit Card' },
        { cc: 'card_payout', id: 1102, name: 'Credit Card Payout' }
    ]
};

export const intercompanies = {
    request: {
        url: `${HOST}/firm/intercompany_list`
    },
    response: [
        {
            cc: 'adfox',
            urlOrders: 'http://adfox.ru',
            inContract: true,
            id: 102,
            name: 'ADFox.ru'
        },
        {
            cc: 'afisha_moviepass',
            urlOrders: null,
            inContract: true,
            id: 617,
            name: 'Afisha.MoviePass'
        }
    ]
};

export const currencies = {
    request: {
        url: `${HOST}/currency/list`
    },
    response: [
        { isoCode: 'AED', isoNumCode: 784 },
        { isoCode: 'AMD', isoNumCode: 51 },
        { isoCode: 'AUD', isoNumCode: 36 }
    ]
};

export const payments = {
    request: [
        `${HOST}/payments/list`,
        {
            amount: filterValues.AMOUNT,
            approval_code: filterValues.APPROVAL_CODE,
            card_number: filterValues.CARD_NUMBER,
            currency_code: filterValues.CURRENCY_CODE,
            date_type: filterValues.DATE_TYPE,
            firm_id: filterValues.FIRM_ID,
            from_dt: filterValues.FROM_DT,
            invoice_eid: filterValues.INVOICE_EID,
            pagination_pn: 1,
            pagination_ps: 10,
            passport_id: filterValues.PASSPORT_ID,
            payment_id: filterValues.PAYMENT_ID,
            payment_method: filterValues.PAYMENT_METHOD,
            payment_status: filterValues.PAYMENT_STATUS,
            processing_cc: filterValues.PROCESSING_CC,
            purchase_token: filterValues.PURCHASE_TOKEN,
            register_id: filterValues.REGISTER_ID,
            rrn: filterValues.RRN,
            service_id: filterValues.SERVICE_ID,
            show_totals: filterValues.SHOW_TOTALS,
            sort_key: 'PAYMENT_ID',
            sort_order: 'DESC',
            terminal_id: filterValues.TERMINAL_ID,
            to_dt: filterValues.TO_DT,
            transaction_id: filterValues.TRANSACTION_ID,
            trust_payment_id: filterValues.TRUST_PAYMENT_ID
        },
        false,
        false
    ],
    response: {
        totalRowCount: 3,
        totals: null,
        gtotals: null,
        items: [
            {
                rrn: null,
                balance_payment_id: 2285161374,
                user_account: null,
                payment_status: 'Авторизован',
                terminal_id: 96111102,
                firm_title: 'ООО «Яндекс.Маркет»',
                currency: 'RUR',
                passport_login: null,
                client_name: null,
                register_dt: null,
                user_ip: null,
                service_name: 'Синий Маркет. Платежи',
                payment_dt: '2019-11-16T00:35:59',
                cancel_dt: null,
                payment_method_cc: 'card',
                payment_method_name: 'Credit Card',
                payment_resp_desc: null,
                payment_status_update_dt: '2019-11-16T00:35:59',
                processing_cc: 'yamoney_h2h_emulator',
                create_dt: '2019-11-14T23:59:06',
                register_id: null,
                payment_status_id: 2,
                firm_id: 111,
                postauth_dt: null,
                client_id: null,
                trust_payment_id: null,
                postauth_amount: null,
                purchase_token: null,
                card_holder: null,
                processing_name: 'Yandex.Money New Card API Emulator',
                service_cc: 'blue_market_payments',
                approval_code: null,
                invoice_eid: null,
                amount: '971.00',
                transaction_id: null,
                invoice_id: null,
                service_id: 610,
                passport_id: null
            },
            {
                rrn: 417187,
                balance_payment_id: 2285161377,
                user_account: '411111****1111',
                payment_status: null,
                terminal_id: 96000104,
                firm_title: 'ООО «Яндекс»',
                currency: 'RUR',
                passport_login: 'yandex-team-29331-90089',
                client_name: null,
                register_dt: null,
                user_ip: null,
                service_name: 'Яндекс.Паспорт',
                payment_dt: '2019-11-14T23:59:25',
                cancel_dt: '2019-11-14T23:59:26',
                payment_method_cc: 'card',
                payment_method_name: 'Credit Card',
                payment_resp_desc: 'paid ok',
                payment_status_update_dt: '2019-11-14T23:59:26',
                processing_cc: 'yamoney_h2h_emulator',
                create_dt: '2019-11-14T23:59:00',
                register_id: null,
                payment_status_id: null,
                firm_id: 1,
                postauth_dt: null,
                client_id: null,
                trust_payment_id: '5dcdc014910d394343cfd109',
                postauth_amount: null,
                purchase_token: '55d9f2b7f30eedaf14e6a78312d4366e',
                card_holder: null,
                processing_name: 'Yandex.Money New Card API Emulator',
                service_cc: 'passport',
                approval_code: '303817',
                invoice_eid: null,
                amount: '2.00',
                transaction_id: 'jmnzey4vz23mnfrbpojj',
                invoice_id: null,
                service_id: 138,
                passport_id: 4032146100
            },
            {
                rrn: 979839,
                balance_payment_id: 2285161419,
                user_account: '510000****6704',
                payment_status: 'Поставторизован',
                terminal_id: 96000111,
                firm_title: 'ООО «Яндекс»',
                currency: 'RUR',
                passport_login: 'mjfs',
                client_name: null,
                register_dt: null,
                user_ip: '94.25.233.75',
                service_name: 'Кинопоиск: подписка на амедиатеку',
                payment_dt: '2019-11-14T23:58:16',
                cancel_dt: null,
                payment_method_cc: 'card',
                payment_method_name: 'Credit Card',
                payment_resp_desc: 'paid ok',
                payment_status_update_dt: '2019-11-15T00:08:19',
                processing_cc: 'yamoney_h2h_emulator',
                create_dt: '2019-11-14T23:58:12',
                register_id: null,
                payment_status_id: 3,
                firm_id: 1,
                postauth_dt: '2019-11-15T00:08:19',
                client_id: null,
                trust_payment_id: '5dcdbfe5910d394343cfd107',
                postauth_amount: '5.00',
                purchase_token: 'aeff97b0038f9f6b072a8a8db8e2a70c',
                card_holder: null,
                processing_name: 'Yandex.Money New Card API Emulator',
                service_cc: 'kinopoisk_amediateka',
                approval_code: '338114',
                invoice_eid: null,
                amount: '5.00',
                transaction_id: 'mito967y96mwao9f0k5n',
                invoice_id: null,
                service_id: 635,
                passport_id: 4031984906
            }
        ]
    }
};
