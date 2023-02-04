import { HOST } from 'common/utils/test-utils/common';

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

export const personCategories = {
    request: {
        url: `${HOST}/person/category/list`
    },
    response: [
        { category: 'am_jp', name: 'ID_Legal_entity_AM' },
        { category: 'am_np', name: 'ID_Individual_AM' },
        { category: 'az_ur', name: 'ID_Legal_entity_AZ' }
    ]
};

export const manager = {
    request: {
        data: {
            manager_code: '38325'
        },
        url: `${HOST}/manager`
    },
    response: {
        parent_code: 20482,
        id: 20869,
        name: 'Вася',
        parents_names: '["Поиск", "Search Portal"]'
    }
};

export const history = {
    perms: ['AdminAccess'],
    search:
        '?date_type=1&dt_from=2019-06-24T00%3A00%3A00&dt_to=2019-06-26T00%3A00%3A00&invoice_eid=%D0%91-1782034219-1&payment_status=2&firm_id=1&post_pay_type=3&trouble_type=0&client_id=8637564&person_id=3034649&manager_code=38325&service_id=102&service_order_id=7-17135232&contract_eid=96323%2F18&ct=1&pn=1&ps=10&sf=invoice_dt&so=1',
    filter: {
        dateType: 'INVOICE',
        dateFrom: '2019-06-24T00:00:00',
        dateTo: '2019-06-26T00:00:00',
        invoiceEid: 'Б-1782034219-1',
        paymentStatus: 'TURN_ON',
        firm: 1,
        postPayType: 'FICTITIOUS',
        troubleType: 'NONE',
        service: 102,
        serviceOrderId: '7-17135232',
        contractEid: '96323/18'
    }
};

export const productServiceCode = {
    request: [`${HOST}/product/service-code/list`, undefined, false, false],
    response: {
        version: { 'yb-snout-api': '<UNDEFINED>' },
        data: [
            {
                code: 'APIKEYS_AGENT_MAPS_DISTANCE MATRIX & ROUTER API',
                description:
                    '\u041a\u0430\u0431\u0438\u043d\u0435\u0442 \u0440\u0430\u0437\u0440\u0430\u0431\u043e\u0442\u0447\u0438\u043a\u0430 (659), API \u042f\u043d\u0434\u0435\u043a\u0441.\u041c\u0430\u0440\u0448\u0440\u0443\u0442\u0438\u0437\u0430\u0446\u0438\u044f: \u041c\u0430\u0442\u0440\u0438\u0446\u0430 \u0440\u0430\u0441\u0441\u0442\u043e\u044f\u043d\u0438\u0439'
            },
            {
                code: 'APIKEYS_MAPS_PLACES',
                description:
                    '\u041a\u0430\u0431\u0438\u043d\u0435\u0442 \u0440\u0430\u0437\u0440\u0430\u0431\u043e\u0442\u0447\u0438\u043a\u0430, API \u041f\u043e\u0438\u0441\u043a \u043f\u043e \u043e\u0440\u0433\u0430\u043d\u0438\u0437\u0430\u0446\u0438\u044f\u043c'
            }
        ]
    }
};
export const paysysList = {
    request: [`${HOST}/paysys/list`, undefined, false, false],
    response: {
        version: { 'yb-snout-api': '<UNDEFINED>' },
        data: [
            {
                char_code: 'eu_yt_byn',
                id: 12601145,
                weight: 1000,
                name:
                    '\u0411\u0430\u043d\u043a \u0434\u043b\u044f \u044e\u0440\u0438\u0434\u0438\u0447\u0435\u0441\u043a\u0438\u0445 \u043b\u0438\u0446 \u0431\u0435\u043b. \u0440\u0443\u0431\u043b\u0438 (\u043d\u0435\u0440\u0435\u0437\u0438\u0434\u0435\u043d\u0442\u044b, \u0413\u043e\u043b\u043b\u0430\u043d\u0434\u0438\u044f)'
            },
            {
                char_code: 'eu_yt_eur',
                id: 12601039,
                weight: 2510,
                name:
                    '\u0411\u0430\u043d\u043a \u0434\u043b\u044f \u044e\u0440\u0438\u0434\u0438\u0447\u0435\u0441\u043a\u0438\u0445 \u043b\u0438\u0446 \u0435\u0432\u0440\u043e (\u043d\u0435\u0440\u0435\u0437\u0438\u0434\u0435\u043d\u0442\u044b, \u0413\u043e\u043b\u043b\u0430\u043d\u0434\u0438\u044f)'
            },
            {
                char_code: 'eu_ur_eur',
                id: 12601042,
                weight: 2500,
                name:
                    '\u0411\u0430\u043d\u043a \u0434\u043b\u044f \u044e\u0440\u0438\u0434\u0438\u0447\u0435\u0441\u043a\u0438\u0445 \u043b\u0438\u0446 \u0435\u0432\u0440\u043e (\u0413\u043e\u043b\u043b\u0430\u043d\u0434\u0438\u044f)'
            }
        ]
    }
};

export const client = {
    request: [`${HOST}/client`, { client_id: 8637564 }, false, false],
    response: {
        version: { snout: 'UNKNOWN', muzzle: 'UNKNOWN', butils: 'UNKNOWN' },
        data: {
            manual_suspect_comment: '',
            overdraft_ban: false,
            direct25: false,
            region_id: 225,
            currency_payment: null,
            is_agency: true,
            parent_agencies: [],
            single_account_number: null,
            domain_check_comment: null,
            has_edo: false,
            intercompany: null,
            id: 2124001,
            printable_docs_type: 0,
            domain_check_status: 0,
            full_repayment: true,
            fraud_status: null,
            reliable_cc_payer: 0,
            'client-type': null,
            deny_overdraft: null,
            only_manual_name_update: false,
            manual_suspect: 0,
            internal: false,
            sms_notify: 2,
            type: { id: null },
            email: 'vlmegagroup@gmail.com',
            is_acquiring: null,
            fax: null,
            region_name: '\u0420\u043e\u0441\u0441\u0438\u044f',
            parent_agency_id: 2124001,
            city:
                '\u0421\u0430\u043d\u043a\u0442-\u041f\u0435\u0442\u0435\u0440\u0431\u0443\u0440\u0433',
            deny_cc: 0,
            client_type_id: null,
            is_non_resident: false,
            phone: '+7 (812) 448 39 58',
            name: '\u041c\u0435\u0433\u0430\u0433\u0440\u0443\u043f',
            partner_type: '0',
            url: null,
            force_contractless_invoice: false,
            non_resident_currency_payment: null,
            parent_agency_name: '\u041c\u0435\u0433\u0430\u0433\u0440\u0443\u043f',
            fullname: null,
            is_ctype_3: false
        }
    }
};

export const person = {
    request: [`${HOST}/person`, { person_id: 3034649 }, false, false],
    response: {
        version: { snout: 'UNKNOWN', muzzle: 'UNKNOWN', butils: 'UNKNOWN' },
        data: {
            kbk: null,
            invalid_bankprops: false,
            kbe: null,
            legal_address_district: null,
            postcode: '196247',
            oktmo: null,
            corraccount: null,
            delivery_city: null,
            bank_data: {
                info: null,
                bank_name:
                    '\u0424\u0418\u041b\u0418\u0410\u041b "\u0421\u0410\u041d\u041a\u0422-\u041f\u0415\u0422\u0415\u0420\u0411\u0423\u0420\u0413\u0421\u041a\u0418\u0419" \u0410\u041e "\u0410\u041b\u042c\u0424\u0410-\u0411\u0410\u041d\u041a"',
                update_dt: '2020-04-08T04:00:30',
                bik: 44030786,
                cor_acc: '30101810600000000786',
                bank_address: null,
                is_new: false,
                bank_city:
                    '\u0421\u0430\u043d\u043a\u0442-\u041f\u0435\u0442\u0435\u0440\u0431\u0443\u0440\u0433',
                city:
                    '\u0421\u0430\u043d\u043a\u0442-\u041f\u0435\u0442\u0435\u0440\u0431\u0443\u0440\u0433',
                corr_account: '30101810600000000786',
                hidden: false,
                swift: null,
                id: 1129,
                name:
                    '\u0424\u0418\u041b\u0418\u0410\u041b "\u0421\u0410\u041d\u041a\u0422-\u041f\u0415\u0422\u0415\u0420\u0411\u0423\u0420\u0413\u0421\u041a\u0418\u0419" \u0410\u041e "\u0410\u041b\u042c\u0424\u0410-\u0411\u0410\u041d\u041a"'
            },
            kz_in: null,
            post2: null,
            post1: null,
            local_authority_doc_details: null,
            bank: null,
            hidden: false,
            export_dt: '2020-01-20T09:45:16',
            fax: null,
            bik: '044030786',
            legal_address_home: null,
            dt: '2018-11-09T12:10:24',
            legal_fias_guid: null,
            birthplace_region: null,
            kpp: '781001001',
            address_city: null,
            name: '\u0420\u0435\u043a\u043c\u0430\u043b\u0430',
            kladr_code: null,
            address_updated: null,
            postaddress: null,
            local_signer_person_name: null,
            vat_payer: null,
            local_legaladdress: null,
            address_code: null,
            oebs_exportable: true,
            ogrn: null,
            street: '\u041b\u0435\u043d\u0438\u043d\u0441\u043a\u0438\u0439 \u043f\u0440.',
            is_batch_supported: true,
            legal_address_gni: null,
            early_docs: 0,
            authority_doc_details: null,
            country_id: '2',
            address_district: null,
            address_home: null,
            rn: null,
            email: 'zs.milyausha@gmail.com;vorobyevann@megagroup.ru;anastas.beletskaya@gmail.com',
            local_representative: null,
            person_account: null,
            mname: null,
            bankcity: null,
            legal_address_flat: null,
            iban: null,
            ui_editable: true,
            address: null,
            swift: null,
            kz_kbe: null,
            attribute_batch_id: 2685675,
            passport_code: null,
            region: '2',
            local_city: null,
            address_gni: null,
            bank_type: 0,
            birthday: null,
            local_name: null,
            address_construction: null,
            passport_id: '569333733',
            passport_e: null,
            passport_d: null,
            address_flat: null,
            passport_n: null,
            local_signer_position_name: null,
            vip: 0,
            passport_s: null,
            address_postcode: null,
            address_street: null,
            ui_deletable: true,
            city:
                '\u0433 \u0421\u0430\u043d\u043a\u0442-\u041f\u0435\u0442\u0435\u0440\u0431\u0443\u0440\u0433',
            revise_act_period_type: null,
            operator_uid: null,
            ben_bank: null,
            authority_doc_type: '\u0423\u0441\u0442\u0430\u0432',
            fias_guid: 'c2deb16a-0330-4f05-821f-1d09c93331e6',
            is_partner: false,
            type: 'ur',
            jpc: null,
            inn_doc_details: null,
            bank_inn: null,
            memo: null,
            legal_address_street: null,
            phone: '+7 (812) 375-95-15',
            local_ben_bank: null,
            pfr: null,
            representative:
                '\u0410\u0445\u043c\u0430\u0434\u0438\u0435\u0432\u0430 \u041c\u0438\u043b\u0430',
            client_id: 2124001,
            is_ur: true,
            legal_address_construction: null,
            iik: null,
            need_deal_passport: false,
            legal_address_code: null,
            account: '40702810032180001806',
            invoice_count: 549,
            short_signer_person_name: '\u041c\u0438\u043d\u0438\u043d \u0421. \u0412.',
            signer_person_gender: 'M',
            legaladdress:
                '196247, \u0421\u0430\u043d\u043a\u0442-\u041f\u0435\u0442\u0435\u0440\u0431\u0443\u0440\u0433, \u041b\u0435\u043d\u0438\u043d\u0441\u043a\u0438\u0439 \u043f\u0440., \u0434. 151, \u043e\u0444\u0438\u0441 702',
            us_state: null,
            address_building: null,
            invalid_address: false,
            legal_address_building: null,
            attr_meta_ignored: 'set([])',
            legal_address_city: null,
            is_new: false,
            inn: '7810340569',
            local_postaddress: null,
            delivery_type: 0,
            id: 2410718,
            rnn: null,
            legal_address_region: null,
            legal_sample:
                '196247, \u0421\u0430\u043d\u043a\u0442-\u041f\u0435\u0442\u0435\u0440\u0431\u0443\u0440\u0433, \u041b\u0435\u043d\u0438\u043d\u0441\u043a\u0438\u0439 \u043f\u0440., \u0434. 151, \u043e\u0444\u0438\u0441 702',
            tax_type: null,
            lname: null,
            address_region: null,
            attr_direct_access: "set(['_sa_instance_state', '_state', '_do_not_export'])",
            other: null,
            fname: null,
            yamoney_wallet: null,
            local_other: null,
            live_signature: 0,
            signer_position_name:
                '\u0413\u0435\u043d\u0435\u0440\u0430\u043b\u044c\u043d\u044b\u0439 \u0434\u0438\u0440\u0435\u043a\u0442\u043e\u0440',
            ben_account: null,
            payment_purpose: null,
            address_town: null,
            legal_address_postcode: null,
            birthplace_country: null,
            longname: '\u041e\u041e\u041e \u00ab\u0420\u0435\u043a\u043c\u0430\u043b\u0430\u00bb',
            local_bank: null,
            signer_person_name:
                '\u041c\u0438\u043d\u0438\u043d \u0421\u0435\u0440\u0433\u0435\u0439 \u0412\u043b\u0430\u0434\u0438\u043c\u0438\u0440\u043e\u0432\u0438\u0447',
            sensible_name:
                '\u041e\u041e\u041e \u00ab\u0420\u0435\u043a\u043c\u0430\u043b\u0430\u00bb',
            local_longname: null,
            birthplace_district: null,
            verified_docs: 0,
            legal_address_town: null,
            true_kz: 0,
            organization: null,
            envelope_address:
                '\u041b\u0435\u043d\u0438\u043d\u0441\u043a\u0438\u0439 \u043f\u0440., \u0434. 151, \u043e\u0444\u0438\u0441 702\n\u0433 \u0421\u0430\u043d\u043a\u0442-\u041f\u0435\u0442\u0435\u0440\u0431\u0443\u0440\u0433',
            auto_gen: 0,
            postsuffix: '\u0434. 151, \u043e\u0444\u0438\u0441 702'
        }
    }
};

export const invoices = {
    request: [
        `${HOST}/invoice/list`,
        {
            client_id: 8637564,
            contract_eid: '96323/18',
            contract_eid_strict: false,
            firm_id: 1,
            from_dt: '2019-06-24T00:00:00',
            invoice_eid: 'Б-1782034219-1',
            invoice_type: 'INVOICE',
            manager_code: '38325',
            manager_subordinate: false,
            pagination_pn: 1,
            pagination_ps: 10,
            payment_status: 'TURN_ON',
            person_id: 3034649,
            post_pay_type: 'FICTITIOUS',
            service_id: 102,
            service_order_id: '17135232',
            show_totals: true,
            sort_key: 'INVOICE_DT',
            sort_order: 'DESC',
            to_dt: '2019-06-26T00:00:00',
            trouble_type: 'NONE'
        },
        false,
        false
    ],
    response: {
        data: { items: [], row_count: 0, total_count: 0 }
    }
};
