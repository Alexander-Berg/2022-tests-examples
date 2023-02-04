import { HOST } from 'common/utils/test-utils/common';
import { camelCasePropNames } from 'common/utils/camel-case';

export const history = {
    perms: ['NewUIEarlyAdopter', 'AdminAccess'],
    search: '?contract_id=227180',
    contract: {
        request: [
            {
                url: `${HOST}/contract`,
                data: {
                    contract_eid: undefined,
                    show_details: true,
                    contract_id: 227180,
                    show_zero_attrs: true
                }
            }
        ],
        response: camelCasePropNames({
            contract: {
                export_dt: '2017-11-01T15:34:20',
                is_new_comm: false,
                electronic_reports: [],
                oebs_exportable: true,
                periods: [],
                client_id: 7592838,
                person_id: 2613848,
                type: 'GENERAL',
                id: 227180,
                external_id: '46129/15'
            },
            contract_attributes: [
                {
                    group: 1,
                    name: 'commission',
                    value: 'Авто.ру: Оптовый агентский, премия',
                    highlighted: true,
                    caption: 'Тип договора',
                    type: 'refselect'
                },
                {
                    group: 1,
                    name: 'firm',
                    value: 'ООО «АВТО.РУ Холдинг»',
                    highlighted: false,
                    caption: 'Фирма',
                    type: 'refselect'
                },
                {
                    group: 1,
                    name: 'currency',
                    value: 'RUR',
                    highlighted: false,
                    caption: 'Валюта расчетов',
                    type: 'refselect'
                },
                {
                    group: 1,
                    name: 'bank_details_id',
                    value: '«ИНГ БАНК (ЕВРАЗИЯ) АО» [...5224]',
                    highlighted: false,
                    caption: 'Банк расчетов',
                    type: 'refselect'
                },
                {
                    group: 1,
                    name: 'manager_code',
                    value: 'Афанасьева Ирина Викторовна',
                    highlighted: false,
                    caption: 'Менеджер',
                    type: 'autocomplete'
                },
                {
                    group: 1,
                    name: 'manager_bo_code',
                    value: 'Богунова Виктория Валерьевна',
                    highlighted: false,
                    caption: 'Менеджер БО',
                    type: 'autocomplete'
                },
                {
                    group: 1,
                    name: 'dt',
                    value: '2015-08-19T00:00:00',
                    highlighted: false,
                    caption: 'Дата начала',
                    type: 'date'
                },
                {
                    group: 1,
                    name: 'finish_dt',
                    value: '2016-12-01T00:00:00',
                    highlighted: false,
                    caption: 'Дата окончания',
                    type: 'date'
                },
                {
                    group: 1,
                    name: 'payment_type',
                    value: 'предоплата',
                    highlighted: true,
                    caption: 'Оплата',
                    type: 'refselect'
                },
                {
                    group: 1,
                    name: 'unilateral',
                    value: 'Да',
                    highlighted: false,
                    caption: 'Односторонние акты',
                    type: 'refselect'
                },
                {
                    group: 1,
                    name: 'services',
                    value: 'Авто.Ру',
                    highlighted: false,
                    caption: 'Сервисы',
                    type: 'checkboxes'
                },
                {
                    group: 1,
                    name: 'memo',
                    value: 'Богунова ',
                    highlighted: false,
                    caption: 'Примечание',
                    type: 'text'
                },
                {
                    group: 2,
                    name: 'print_tpl_barcode',
                    value: 27181879,
                    highlighted: false,
                    caption: 'Значение штрих-кода',
                    type: 'input'
                },
                {
                    group: 2,
                    name: 'discount_policy_type',
                    value: 'Авто.ру',
                    highlighted: false,
                    caption: 'Тип скидочной политики',
                    type: 'refselect'
                },
                {
                    group: 2,
                    name: 'wholesale_agent_premium_awards_scale_type',
                    value: 'Авто.ру',
                    highlighted: false,
                    caption: 'Шкала премии',
                    type: 'refselect'
                },
                {
                    group: 2,
                    name: 'link_contract_id',
                    value: [
                        {
                            caption: 268848,
                            val: '46129/15'
                        }
                    ],
                    highlighted: false,
                    caption: 'Связанный договор',
                    type: 'contractinput'
                },
                {
                    group: 2,
                    name: 'autoru_q_plan',
                    value: '1600000',
                    highlighted: false,
                    caption: 'План',
                    type: 'pctinput'
                },
                {
                    group: 3,
                    name: 'is_faxed',
                    value: '2015-08-20T00:00:00',
                    highlighted: false,
                    caption: 'Подписан по факсу',
                    type: 'datecheckbox'
                },
                {
                    group: 3,
                    name: 'is_signed',
                    value: '2015-08-31T00:00:00',
                    highlighted: false,
                    caption: 'Подписан',
                    type: 'datecheckbox'
                },
                {
                    group: 3,
                    name: 'sent_dt',
                    value: '2015-08-19T00:00:00',
                    highlighted: false,
                    caption: 'Отправлен оригинал',
                    type: 'datecheckbox'
                }
            ],
            request_params: {
                show_details: true
            }
        })
    },
    contractCollaterals: {
        request: [
            {
                url: `${HOST}/contract/collaterals`,
                data: {
                    all_collaterals: false,
                    contract_id: 227180,
                    pagination_ps: 10,
                    pagination_pn: 1,
                    sort_key: 'DT',
                    sort_order: 'DESC'
                }
            }
        ],
        response: camelCasePropNames({
            items: [
                {
                    finish_dt: '2016-12-01T00:00:00',
                    collateral_pn: 1,
                    is_booked: null,
                    is_signed: '2016-12-01T00:00:00',
                    id: 275455,
                    collateral_type: 'расторжение договора',
                    num: 'Ф-09',
                    is_archived: null,
                    is_cancelled: null,
                    dt: '2016-12-01T00:00:00',
                    is_faxed: null,
                    memo: 'Создан по задаче PAYSUP-286721'
                },
                {
                    finish_dt: '2018-01-01T00:00:00',
                    is_booked: null,
                    memo:
                        'Тулукова Л.В.\r\nДС на планы 4 кв.\r\n161003-00399 \r\n\r\nскан получен 161118-00225 18.11.2016',
                    is_signed: '2016-12-09T00:00:00',
                    collateral_pn: 2,
                    collateral_type: 'План',
                    num: '08',
                    is_archived: null,
                    sent_dt: '2016-12-13T00:00:00',
                    is_cancelled: null,
                    dt: '2016-10-01T00:00:00',
                    is_faxed: '2016-11-18T00:00:00',
                    id: 261393
                },
                {
                    finish_dt: '2018-01-01T00:00:00',
                    is_booked: null,
                    memo:
                        'Тулукова\r\nДС на доб-ние услуги с 01.09.16г "Премиум объявление" и "Промо бъявление"+исправление опечатки Таб.2 Прил.2 c 01.07.16\r\nтикет 160919-01488 19.09.2016 19:31:26\r\nСкан.получен:\r\n160920-00446 20.09.2016 10:33:36\r\nПовторная отправка 23.11.2016',
                    is_signed: '2016-10-20T00:00:00',
                    collateral_pn: 3,
                    collateral_type: 'прочее',
                    num: '07',
                    is_archived: '2017-11-01T00:00:00',
                    sent_dt: '2016-11-01T00:00:00',
                    is_cancelled: null,
                    dt: '2016-09-01T00:00:00',
                    is_faxed: '2016-09-20T00:00:00',
                    id: 258734
                },
                {
                    finish_dt: '2018-01-01T00:00:00',
                    is_booked: null,
                    memo:
                        'Тулукова Л.В.\r\nДС на планы 3 кв.\r\nот  Игнатова Сергея \r\n 160615-01397 (22.06.16)\r\n\r\nскан.получен 160629-01072 29.06.2016 15:49:11',
                    is_signed: '2016-07-04T00:00:00',
                    collateral_pn: 4,
                    collateral_type: 'прочее',
                    num: '06',
                    is_archived: null,
                    sent_dt: '2016-07-05T00:00:00',
                    is_cancelled: null,
                    dt: '2016-07-01T00:00:00',
                    is_faxed: '2016-06-29T00:00:00',
                    id: 248557
                },
                {
                    finish_dt: '2018-01-01T00:00:00',
                    is_booked: null,
                    memo:
                        'Зяблицкая Е.А.\r\nДС на добавление услуги с 15.04.16г "Поднятие в поиске", шаблон Абрамовой А.CRM: 160510-01286 10.05.2016 13:54:44 \r\nСкан получен____, тикет___',
                    is_signed: '2016-07-04T00:00:00',
                    collateral_pn: 5,
                    collateral_type: 'прочее',
                    num: '05',
                    is_archived: null,
                    sent_dt: '2016-07-05T00:00:00',
                    is_cancelled: null,
                    dt: '2016-05-25T00:00:00',
                    is_faxed: null,
                    id: 238976
                },
                {
                    finish_dt: '2018-01-01T00:00:00',
                    is_booked: null,
                    memo:
                        'Зяблицкая Е.А.Ср 23.03.2016 14:09[Тикет #16032314091847757]\r\nплан от Соколова Бориса [Тикет #16031018491543537]',
                    is_signed: '2016-04-04T00:00:00',
                    collateral_pn: 6,
                    collateral_type: 'продление договора',
                    num: '04',
                    is_archived: null,
                    sent_dt: '2016-04-08T00:00:00',
                    is_cancelled: null,
                    dt: '2016-04-01T00:00:00',
                    is_faxed: '2016-03-25T00:00:00',
                    id: 220860
                },
                {
                    finish_dt: '2016-08-19T00:00:00',
                    is_booked: null,
                    memo:
                        'Тулукова, шаблон Абрамовой Чт 21.01.2016 18:33\r\nСкан ДС получен 28 января 2016, 10:48 (Ticket#15122810302163426)',
                    is_signed: '2016-03-28T00:00:00',
                    collateral_pn: 7,
                    collateral_type: 'прочее',
                    num: '03',
                    is_archived: null,
                    sent_dt: '2016-04-08T00:00:00',
                    is_cancelled: null,
                    dt: '2016-02-01T00:00:00',
                    is_faxed: '2016-01-28T00:00:00',
                    id: 210022
                },
                {
                    finish_dt: '2016-08-19T00:00:00',
                    is_booked: null,
                    memo: 'Богунова\r\nДС на планы январь 2016',
                    is_signed: '2016-01-01T00:00:00',
                    collateral_pn: 8,
                    collateral_type: 'прочее',
                    num: '02',
                    is_archived: null,
                    sent_dt: '2016-01-01T00:00:00',
                    is_cancelled: null,
                    dt: '2016-01-01T00:00:00',
                    is_faxed: null,
                    id: 250000
                },
                {
                    finish_dt: '2016-08-19T00:00:00',
                    is_booked: null,
                    memo: 'Богунова\r\nДС на планы 4 кв. 2015г',
                    is_signed: '2015-10-01T00:00:00',
                    collateral_pn: 9,
                    collateral_type: 'прочее',
                    num: '01',
                    is_archived: null,
                    sent_dt: '2015-10-01T00:00:00',
                    is_cancelled: null,
                    dt: '2015-10-01T00:00:00',
                    is_faxed: null,
                    id: 249999
                }
            ],
            request_params: {
                sort_key: 'dt',
                pagination_ps: 10,
                sort_order: 'desc',
                pagination_pn: 1,
                all_collaterals: false
            },
            total_count: 9
        })
    },
    client: {
        request: [{ url: `${HOST}/client`, data: { client_id: 7592838 } }],
        response: camelCasePropNames({
            manual_suspect_comment: '',
            overdraft_ban: false,
            has_edo: true,
            region_id: null,
            currency_payment: null,
            phone: null,
            is_agency: true,
            parent_agencies: [],
            single_account_number: null,
            domain_check_comment: '',
            direct25: false,
            id: 7592838,
            printable_docs_type: 0,
            city: null,
            full_repayment: true,
            fraud_status: null,
            reliable_cc_payer: 0,
            'client-type': null,
            manual_suspect: 0,
            only_manual_name_update: false,
            deny_overdraft: false,
            internal: false,
            sms_notify: 2,
            type: {
                id: 2
            },
            email: null,
            is_acquiring: null,
            fax: null,
            region_name: null,
            parent_agency_id: 7592838,
            domain_check_status: 0,
            deny_cc: 0,
            client_type_id: 2,
            is_non_resident: false,
            partner_type: '0',
            iso_currency_payment: null,
            name: '4 пикселя +"',
            intercompany: null,
            url: null,
            force_contractless_invoice: false,
            non_resident_currency_payment: null,
            parent_agency_name: '4 пикселя +"',
            fullname: null,
            is_ctype_3: false
        })
    },
    person: {
        request: [{ url: `${HOST}/person`, data: { person_id: 2613848 } }],
        response: camelCasePropNames({
            kbk: null,
            invalid_bankprops: false,
            kbe: null,
            legal_address_district: null,
            postcode: '125167',
            oktmo: null,
            corraccount: null,
            delivery_city: null,
            bank_data: {
                info: null,
                bank_name: 'ПАО АКБ "АВАНГАРД"',
                update_dt: '2020-11-11T04:01:07',
                bik: 44525201,
                cor_acc: '30101810000000000201',
                bank_address: null,
                is_new: false,
                bank_city: 'Москва',
                city: 'Москва',
                corr_account: '30101810000000000201',
                hidden: false,
                swift: 'AVJSRUMMXXX',
                id: 407,
                name: 'ПАО АКБ "АВАНГАРД"'
            },
            kz_in: null,
            post2: null,
            post1: null,
            local_authority_doc_details: null,
            birthplace_region: null,
            hidden: true,
            legal_address_code: null,
            fax: null,
            bik: '044525201',
            legal_address_home: null,
            pingpong_wallet: null,
            dt: '2016-12-22T11:59:54',
            legal_fias_guid: null,
            bank: null,
            kpp: '771401001',
            address_city: null,
            name: 'ООО "4 пикселя +"',
            vip: 0,
            kladr_code: null,
            address_updated: null,
            postaddress: null,
            local_signer_person_name: null,
            vat_payer: null,
            local_legaladdress: null,
            address_code: null,
            oebs_exportable: true,
            ogrn: null,
            street: '1-я улица 8 Марта ул',
            is_batch_supported: true,
            legal_address_gni: null,
            early_docs: 0,
            authority_doc_details: null,
            country_id: null,
            address_district: null,
            address_home: null,
            rn: null,
            email: 'm@4px.ru;docs@4px.ru;generalova.natalya@4px.ru',
            api_version: null,
            local_representative: null,
            person_account: null,
            mname: null,
            bankcity: null,
            export_dt: '2016-12-22T12:00:29',
            legal_address_flat: null,
            iban: null,
            ui_editable: true,
            address: null,
            swift: null,
            kz_kbe: null,
            passport_code: null,
            region: null,
            local_city: null,
            payment_purpose: null,
            bank_type: 0,
            birthday: null,
            passport_birthplace: null,
            local_name: null,
            address_construction: null,
            passport_id: '1120000000026659',
            passport_e: null,
            passport_d: null,
            address_flat: null,
            passport_n: null,
            local_signer_position_name: null,
            legal_address_suffix: null,
            passport_s: null,
            address_postcode: null,
            address_street: null,
            ui_deletable: false,
            city: 'г Москва',
            paypal_wallet: null,
            revise_act_period_type: null,
            operator_uid: null,
            ben_bank: null,
            authority_doc_type: 'Устав',
            fias_guid: 'f77020ac-0f7c-4293-8ced-157aec6f823c',
            is_partner: false,
            type: 'ur_autoru',
            jpc: null,
            webmoney_wallet: null,
            sensible_name: 'Общество с ограниченной ответственностью "4 пикселя +"',
            inn_doc_details: null,
            bank_inn: null,
            memo: null,
            legal_address_street: null,
            phone: '7 (495) 181-16-19',
            local_ben_bank: null,
            pfr: null,
            representative: 'Гусев Михаил',
            client_id: 7592838,
            is_ur: true,
            legal_address_construction: null,
            iik: null,
            need_deal_passport: false,
            corr_swift: null,
            account: '40702810500120031117',
            invoice_count: 209,
            short_signer_person_name: 'Гусев М.Ю.',
            signer_person_gender: 'M',
            legaladdress: '125167, Москва, 1-я улица 8 марта, д. 3 комн. 62',
            us_state: null,
            address_building: null,
            invalid_address: false,
            legal_address_building: null,
            attr_meta_ignored: [],
            legal_address_city: null,
            is_new: false,
            ben_bank_code: null,
            inn: '7708250280',
            il_id: null,
            local_postaddress: null,
            delivery_type: 1,
            id: 2613848,
            rnn: null,
            legal_address_region: null,
            ownership_type: null,
            tax_type: null,
            lname: null,
            address_region: null,
            attr_direct_access: ['_sa_instance_state', '_state', '_do_not_export'],
            other: null,
            fname: null,
            yamoney_wallet: null,
            local_other: null,
            live_signature: 1,
            signer_position_name: 'Генеральный директор',
            ben_account: null,
            address_gni: null,
            address_town: null,
            legal_address_postcode: null,
            birthplace_country: null,
            longname: 'Общество с ограниченной ответственностью "4 пикселя +"',
            local_bank: null,
            signer_person_name: 'Гусев М.Ю.',
            ownership_type_ui: null,
            local_longname: null,
            birthplace_district: null,
            verified_docs: 0,
            legal_sample: '125167, Москва, 1-я улица 8 марта, д. 3 комн. 62',
            payoneer_wallet: null,
            legal_address_town: null,
            true_kz: 0,
            organization: null,
            envelope_address: '1-я улица 8 Марта ул, д.3, комн.62\nг Москва',
            auto_gen: 0,
            postsuffix: 'д.3, комн.62'
        })
    },
    contractPermissions: {
        request: [
            `${HOST}/user/object-permissions`,
            { classname: 'Contract', object_id: 227180 },
            false,
            false
        ],
        response: [
            {
                code: 'OEBSReexportContract',
                id: 11226,
                name:
                    '\u041f\u0435\u0440\u0435\u043f\u043e\u0441\u0442\u0430\u0432\u0438\u0442\u044c \u0434\u043e\u0433\u043e\u0432\u043e\u0440/\u0434\u043e\u043f.\u0441\u043e\u0433\u043b\u0430\u0448\u0435\u043d\u0438\u0435 \u0432 \u043e\u0447\u0435\u0440\u0435\u0434\u044c \u044d\u043a\u0441\u043f\u043e\u0440\u0442\u0430 OEBS'
            }
        ]
    },
    oebs: {
        request: [
            `${HOST}/export/get-state`,
            { classname: 'Contract', queue_type: 'OEBS', object_id: 227180 },
            false,
            false
        ],
        response: camelCasePropNames({
            export_dt: '2017-11-01T15:34:20',
            state: 'EXPORTED'
        })
    }
};
