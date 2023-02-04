import { HOST } from 'common/utils/test-utils/common';

export const currencies = {
    request: {
        url: `${HOST}/currency/list`
    },
    response: [
        { isoCode: 'AED', isoNumCode: 784 },
        { isoCode: 'AMD', isoNumCode: 51 },
        { isoCode: 'AUD', isoNumCode: 643 }
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

export const personTypes = {
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
            manager_code: 23446
        },
        url: `${HOST}/manager`
    },
    response: {
        parent_code: 23446,
        id: 20869,
        name: 'Разинков Юрий Юрьевич',
        parents_names: '["Поиск", "Search Portal"]'
    }
};

export const history = {
    perms: ['NewUIEarlyAdopter', 'AdminAccess'],
    search:
        '?external_id=109173593&factura=20191130224403&invoice_eid=Б-1835232111-1&contract_eid=33798%2F15&act_dt_from=2019-11-29T00%3A00%3A00&act_dt_to=2019-12-01T00%3A00%3A00&manager_code=23446&service_id=120&firm_id=1&currency_num_code=643&client_id=2124001&person_id=2410718&ct=1&pn=1&ps=10&sf=act_dt&so=1',
    filter: {
        externalId: '109173593',
        factura: '20191130224403',
        eid: 'Б-1835232111-1',
        contractEid: '33798/15',
        service: 120,
        firm: 1,
        dtFrom: '2019-11-29T00:00:00',
        dtTo: '2019-12-01T00:00:00',
        currencyCode: 643,
        client: 'Мегагруп',
        person: 'Рекмала',
        manager: 'Разинков Юрий Юрьевич'
    }
};

export const acts = {
    request: [
        `${HOST}/act/list`,
        {
            factura: '20191130224403',
            act_eid: '109173593',
            invoice_eid: 'Б-1835232111-1',
            contract_eid: '33798/15',
            dt_from: '2019-11-29T00:00:00',
            dt_to: '2019-12-01T00:00:00',
            manager_code: 23446,
            service_id: 120,
            firm_id: 1,
            client_id: 2124001,
            person_id: 2410718,
            currency_code: 'AUD',
            pagination_pn: 1,
            pagination_ps: 10,
            sort_key: 'ACT_DT',
            sort_order: 'DESC',
            show_totals: true
        },
        false,
        false
    ],
    response: {
        version: { snout: 'UNKNOWN', muzzle: 'UNKNOWN', butils: 'UNKNOWN' },
        data: {
            total_row_count: 1,
            gtotals: {
                nds_pct: '20.00',
                paid_amount: '8515.85',
                amount_nds: '1419.31',
                row_count: 1,
                amount: '8515.85',
                invoice_amount: '1980000.00'
            },
            items: [
                {
                    act_id: 110296784,
                    nds_pct: '20.00',
                    factura: '20191130224403',
                    person_type: 'ur',
                    paysys_certificate: '0',
                    contract_id: 204646,
                    paid_amount: '8515.85',
                    invoice_eid: '\u0411-1835232111-1',
                    overdraft: false,
                    act_dt: '2019-11-30T00:00:00',
                    invoice_amount: '1980000.00',
                    person_id: 2410718,
                    invoice_closed: false,
                    person_name: '\u0420\u0435\u043a\u043c\u0430\u043b\u0430',
                    contract_eid: '33798/15',
                    paysys_cc: 'ur',
                    amount_nds: '1419.31',
                    firm_id: 1,
                    client_id: 2124001,
                    invoice_currency: 'RUR',
                    our_fault: null,
                    payment_term_dt: null,
                    paysys_name:
                        '\u0411\u0430\u043d\u043a \u0434\u043b\u044f \u044e\u0440\u0438\u0434\u0438\u0447\u0435\u0441\u043a\u0438\u0445 \u043b\u0438\u0446',
                    currency: 'RUR',
                    invoice_id: 99198120,
                    passport_id: 170417119,
                    credit: false,
                    amount: '8515.85',
                    external_id: '109173593',
                    client_name: '\u041c\u0435\u0433\u0430\u0433\u0440\u0443\u043f'
                }
            ],
            totals: {
                nds_pct: '20.00',
                paid_amount: '8515.85',
                amount_nds: '1419.31',
                row_count: 1,
                amount: '8515.85',
                invoice_amount: '1980000.00'
            }
        }
    }
};

export const client = {
    request: [`${HOST}/client`, { client_id: 2124001 }, false, false],
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
    request: [`${HOST}/person`, { person_id: 2410718 }, false, false],
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

export const managerList = {
    version: { snout: 'UNKNOWN', muzzle: 'UNKNOWN', butils: 'UNKNOWN' },
    data: {
        parent_code: null,
        id: 23446,
        name:
            '\u0420\u0430\u0437\u0438\u043d\u043a\u043e\u0432 \u042e\u0440\u0438\u0439 \u042e\u0440\u044c\u0435\u0432\u0438\u0447',
        parents_names: '[]'
    }
};

export const intercompanyList = {
    version: { snout: 'UNKNOWN', muzzle: 'UNKNOWN', butils: 'UNKNOWN' },
    data: [
        { cc: 'AM31', is_active: true, label: 'Yandex.Taxi AM' },
        { cc: 'AM32', is_active: true, label: 'Yandex.Taxi Corp AM' },
        { cc: 'AZ35', is_active: true, label: 'Uber Azerbaijan' }
    ]
};
