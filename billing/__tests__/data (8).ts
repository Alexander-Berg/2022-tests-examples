import { HOST } from 'common/utils/test-utils/common';
import { camelCasePropNames as cc } from 'common/utils';

// используют fetchGet

export const regions = {
    request: [`${HOST}/geo/regions`, { lang: 'ru', region_type: 'COUNTRIES' }, false, false],
    response: {
        version: { 'yb-snout-api': '<UNDEFINED>' },
        data: {
            ru: [
                { id: 29386, name: 'Абхазия' },
                { id: 211, name: 'Австралия' }
            ]
        }
    }
};

// используют request

export const currencies = {
    request: { url: `${HOST}/currency/list` },
    response: [
        { iso_code: 'AED', iso_num_code: 784 },
        { iso_code: 'AMD', iso_num_code: 51 }
    ]
};

export const client = {
    request: {
        url: `${HOST}/client`,
        data: { client_id: 12345 }
    },
    response: cc({
        manual_suspect_comment: null,
        overdraft_ban: false,
        direct25: false,
        region_id: null,
        currency_payment: null,
        is_agency: false,
        parent_agencies: [],
        single_account_number: null,
        domain_check_comment: '',
        has_edo: false,
        intercompany: null,
        id: 12345,
        printable_docs_type: 0,
        domain_check_status: 0,
        full_repayment: true,
        fraud_status: { dt: '2020-08-13T13:37:31', type: null, flag: true, desc: null },
        reliable_cc_payer: 0,
        'client-type': null,
        deny_overdraft: false,
        only_manual_name_update: false,
        manual_suspect: 0,
        internal: false,
        sms_notify: 2,
        type: { id: 0 },
        email: null,
        is_acquiring: false,
        fax: null,
        region_name: null,
        parent_agency_id: null,
        city: null,
        deny_cc: 0,
        client_type_id: 0,
        is_non_resident: false,
        phone: null,
        name: '123',
        partner_type: '0',
        url: 'a',
        force_contractless_invoice: false,
        non_resident_currency_payment: null,
        parent_agency_name: null,
        fullname: null,
        is_ctype_3: false
    })
};

export const intercompanies = {
    request: {
        url: `${HOST}/firm/intercompany_list`
    },
    response: cc([
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
