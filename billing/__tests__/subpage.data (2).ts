import { HOST } from 'common/utils/test-utils/common';
import { camelCasePropNames } from 'common/utils/camel-case';

export const mocks = {
    personforms: {
        request: ['/static/personforms/personforms.json', undefined, false, false, false],
        response: {
            'person-forms': {
                'ur-addr-types': {
                    item: [
                        {
                            text: 'ID_manual_address',
                            id: '3',
                            hidden: 'hidden'
                        },
                        {
                            text: 'ID_PO_box_radio_item',
                            id: '0'
                        },
                        {
                            text: 'ID_on_address',
                            id: '1'
                        },
                        {
                            text: 'ID_Plain_address',
                            id: '2',
                            hidden: 'hidden'
                        }
                    ]
                },
                'legal-addr-types': {
                    item: [
                        {
                            text: 'ID_from_directory',
                            id: '1'
                        },
                        {
                            text: 'ID_without_directory',
                            id: '2'
                        }
                    ]
                },
                'ua-tax-types': {
                    item: [
                        {
                            text: 'ID_not_selected',
                            id: ''
                        },
                        {
                            text: 'ID_UA_tax_general',
                            id: '1'
                        },
                        {
                            text: 'ID_UA_tax_unified',
                            id: '2'
                        }
                    ]
                },
                'pay-types': {
                    item: [
                        {
                            text: 'ID_IBAN',
                            id: '0'
                        },
                        {
                            text: 'ID_Settlement_account',
                            id: '1'
                        },
                        {
                            text: 'ID_Other',
                            id: '2'
                        }
                    ]
                },
                'pay-types-yt': {
                    item: [
                        {
                            text: 'ID_IBAN',
                            id: '0'
                        },
                        {
                            text: 'ID_Settlement_account',
                            id: '1'
                        }
                    ]
                },
                'pay-types-select': {
                    item: [
                        {
                            text: 'ID_not_selected',
                            id: ''
                        },
                        {
                            text: 'ID_IBAN',
                            id: '0'
                        },
                        {
                            text: 'ID_Settlement_account',
                            id: '1'
                        },
                        {
                            text: 'ID_Other',
                            id: '2'
                        }
                    ]
                },
                'pay-types-yt-select': {
                    item: [
                        {
                            text: 'ID_not_selected',
                            id: ''
                        },
                        {
                            text: 'ID_IBAN',
                            id: '0'
                        },
                        {
                            text: 'ID_Settlement_account',
                            id: '1'
                        }
                    ]
                },
                'ua-types': {
                    item: [
                        {
                            text: 'ID_UA_ur',
                            id: '0'
                        },
                        {
                            text: 'ID_UA_ip',
                            id: '1'
                        }
                    ]
                },
                'vat-payer-types': {
                    item: [
                        {
                            text: 'ID_Vat_payer_types_is_not_payer',
                            id: '0'
                        },
                        {
                            text: 'ID_Vat_payer_types_is_payer',
                            id: '1'
                        }
                    ]
                },
                states: {
                    item: [
                        {
                            text: 'ID_not_selected',
                            id: ''
                        },
                        {
                            text: 'Alabama',
                            id: 'AL'
                        },
                        {
                            text: 'Alaska',
                            id: 'AK'
                        },
                        {
                            text: 'Arizona',
                            id: 'AZ'
                        },
                        {
                            text: 'Arkansas',
                            id: 'AR'
                        },
                        {
                            text: 'California',
                            id: 'CA'
                        },
                        {
                            text: 'Colorado',
                            id: 'CO'
                        },
                        {
                            text: 'Connecticut',
                            id: 'CT'
                        },
                        {
                            text: 'Delaware',
                            id: 'DE'
                        },
                        {
                            text: 'Florida',
                            id: 'FL'
                        },
                        {
                            text: 'Georgia',
                            id: 'GA'
                        },
                        {
                            text: 'Hawaii',
                            id: 'HI'
                        },
                        {
                            text: 'Idaho',
                            id: 'ID'
                        },
                        {
                            text: 'Illinois',
                            id: 'IL'
                        },
                        {
                            text: 'Indiana',
                            id: 'IN'
                        },
                        {
                            text: 'Iowa',
                            id: 'IA'
                        },
                        {
                            text: 'Kansas',
                            id: 'KS'
                        },
                        {
                            text: 'Kentucky',
                            id: 'KY'
                        },
                        {
                            text: 'Louisiana',
                            id: 'LA'
                        },
                        {
                            text: 'Maine',
                            id: 'ME'
                        },
                        {
                            text: 'Maryland',
                            id: 'MD'
                        },
                        {
                            text: 'Massachusetts',
                            id: 'MA'
                        },
                        {
                            text: 'Michigan',
                            id: 'MI'
                        },
                        {
                            text: 'Minnesota',
                            id: 'MN'
                        },
                        {
                            text: 'Mississippi',
                            id: 'MS'
                        },
                        {
                            text: 'Missouri',
                            id: 'MO'
                        },
                        {
                            text: 'Montana',
                            id: 'MT'
                        },
                        {
                            text: 'Nebraska',
                            id: 'NE'
                        },
                        {
                            text: 'Nevada',
                            id: 'NV'
                        },
                        {
                            text: 'New Hampshire',
                            id: 'NH'
                        },
                        {
                            text: 'New Jersey',
                            id: 'NJ'
                        },
                        {
                            text: 'New Mexico',
                            id: 'NM'
                        },
                        {
                            text: 'New York',
                            id: 'NY'
                        },
                        {
                            text: 'North Carolina',
                            id: 'NC'
                        },
                        {
                            text: 'North Dakota',
                            id: 'ND'
                        },
                        {
                            text: 'Ohio',
                            id: 'OH'
                        },
                        {
                            text: 'Oklahoma',
                            id: 'OK'
                        },
                        {
                            text: 'Oregon',
                            id: 'OR'
                        },
                        {
                            text: 'Pennsylvania',
                            id: 'PA'
                        },
                        {
                            text: 'Rhode Island',
                            id: 'RI'
                        },
                        {
                            text: 'South Carolina',
                            id: 'SC'
                        },
                        {
                            text: 'South Dakota',
                            id: 'SD'
                        },
                        {
                            text: 'Tennessee',
                            id: 'TN'
                        },
                        {
                            text: 'Texas',
                            id: 'TX'
                        },
                        {
                            text: 'Utah',
                            id: 'UT'
                        },
                        {
                            text: 'Vermont',
                            id: 'VT'
                        },
                        {
                            text: 'Virginia',
                            id: 'VA'
                        },
                        {
                            text: 'Washington',
                            id: 'WA'
                        },
                        {
                            text: 'West Virginia',
                            id: 'WV'
                        },
                        {
                            text: 'Wisconsin',
                            id: 'WI'
                        },
                        {
                            text: 'Wyoming',
                            id: 'WY'
                        },
                        {
                            text: 'Outside USA',
                            id: 'ZZ'
                        }
                    ]
                },
                'delivery-type-options': {
                    item: [
                        {
                            text: 'ID_not_selected_mail',
                            id: '0'
                        },
                        {
                            text: 'ID_mail',
                            id: '1'
                        },
                        {
                            text: 'ID_courier_yandex',
                            id: '2'
                        },
                        {
                            text: 'ID_courier_own',
                            id: '3'
                        },
                        {
                            text: 'VIP',
                            id: '4'
                        }
                    ]
                },
                'delivery-cities': {
                    item: [
                        {
                            text: 'ID_Moscow',
                            id: ''
                        },
                        {
                            text: 'ID_Saint_petersburg',
                            id: 'SPB'
                        },
                        {
                            text: 'ID_Yekaterinburg',
                            id: 'EKT'
                        },
                        {
                            text: 'ID_Novosibirsk',
                            id: 'NSK'
                        },
                        {
                            text: 'ID_Kazan',
                            id: 'KZN'
                        },
                        {
                            text: 'ID_Rostov_on_Don',
                            id: 'RND'
                        },
                        {
                            text: 'ID_Kiev',
                            id: 'KIV'
                        },
                        {
                            text: 'ID_Odessa',
                            id: 'ODS'
                        }
                    ]
                },
                gender: {
                    item: [
                        {
                            text: 'ID_not_selected',
                            id: 'X'
                        },
                        {
                            text: 'ID_male',
                            id: 'M'
                        },
                        {
                            text: 'ID_female',
                            id: 'W'
                        }
                    ]
                },
                'authority-doc-type': {
                    item: [
                        'ID_not_selected',
                        {
                            text: 'ID_Statute',
                            id: 'Устав'
                        },
                        {
                            text: 'ID_Power_of_attorney',
                            id: 'Доверенность'
                        },
                        {
                            text: 'ID_Order_',
                            id: 'Приказ'
                        },
                        {
                            text: 'ID_Disposal',
                            id: 'Распоряжение'
                        },
                        {
                            text: 'ID_Provision_about_branch',
                            id: 'Положение о филиале'
                        },
                        {
                            text: 'ID_Certificate_of_registration',
                            id: 'Свидетельство о регистрации'
                        },
                        {
                            text: 'ID_Contract',
                            id: 'Договор'
                        },
                        {
                            text: 'ID_Record',
                            id: 'Протокол'
                        },
                        {
                            text: 'ID_Decision',
                            id: 'Решение'
                        }
                    ]
                },
                positions: {
                    item: [
                        {
                            id: ''
                        },
                        {
                            text: 'Генеральный директор',
                            id: 'Генеральный директор'
                        },
                        {
                            text: 'Директор',
                            id: 'Директор'
                        },
                        {
                            text: 'Индивидуальный Предприниматель',
                            id: 'Индивидуальный Предприниматель'
                        },
                        {
                            text: 'Исполнительный директор',
                            id: 'Исполнительный директор'
                        },
                        {
                            text: 'Заместитель Генерального директора',
                            id: 'Заместитель Генерального директора'
                        },
                        {
                            text: 'Президент',
                            id: 'Президент'
                        },
                        {
                            text: 'Коммерческий директор',
                            id: 'Коммерческий директор'
                        },
                        {
                            text: 'Председатель Правления',
                            id: 'Председатель Правления'
                        },
                        {
                            text: 'Финансовый директор',
                            id: 'Финансовый директор'
                        },
                        {
                            text: 'Ректор',
                            id: 'Ректор'
                        },
                        {
                            text: 'Заместитель Председателя Правления',
                            id: 'Заместитель Председателя Правления'
                        },
                        {
                            text: 'Управляющий',
                            id: 'Управляющий'
                        },
                        {
                            text: 'Директор по маркетингу и развитию',
                            id: 'Директор по маркетингу и развитию'
                        },
                        {
                            text: 'General Director',
                            id: 'General Director'
                        },
                        {
                            text: 'Financial Director',
                            id: 'Financial Director'
                        },
                        {
                            text: 'President',
                            id: 'President'
                        }
                    ]
                },
                'bank-type-options': {
                    item: [
                        {
                            text: 'ID_not_selected',
                            id: '0'
                        },
                        {
                            text: 'ID_sberbank',
                            id: '1'
                        },
                        {
                            text: 'ID_other_bank',
                            id: '2'
                        },
                        {
                            text: 'ID_Yandex_money',
                            id: '3'
                        },
                        {
                            text: 'ID_webmoney',
                            id: '4'
                        },
                        {
                            text: 'ID_paypal',
                            id: '5'
                        },
                        {
                            text: 'ID_payoneer',
                            id: '7'
                        },
                        {
                            text: 'ID_pingpong',
                            id: '8'
                        }
                    ]
                },
                'bank-type-ur-selfemployed-options': {
                    item: [
                        {
                            text: 'ID_not_selected',
                            id: '0'
                        },
                        {
                            text: 'ID_Bank',
                            id: '2'
                        },
                        {
                            text: 'ID_fast_payment_system',
                            id: '10'
                        }
                    ]
                },
                'bank-type-ua-options': {
                    item: [
                        {
                            text: 'ID_not_selected',
                            id: '0'
                        },
                        {
                            text: 'ID_Yandex_money',
                            id: '3'
                        },
                        {
                            text: 'ID_webmoney',
                            id: '4'
                        },
                        {
                            text: 'ID_paypal',
                            id: '5'
                        },
                        {
                            text: 'ID_pingpong',
                            id: '8'
                        },
                        {
                            text: 'ID_Privat_card',
                            id: '9'
                        }
                    ]
                },
                'bank-type-hk-options': {
                    item: [
                        {
                            text: 'ID_not_selected',
                            id: '0'
                        },
                        {
                            text: 'ID_webmoney',
                            id: '4'
                        },
                        {
                            text: 'ID_paypal',
                            id: '5'
                        },
                        {
                            text: 'ID_payoneer',
                            id: '7'
                        },
                        {
                            text: 'ID_pingpong',
                            id: '8'
                        }
                    ]
                },
                'bank-type-sw_ytph-partner-options': {
                    item: [
                        {
                            text: 'ID_not_selected',
                            id: '0'
                        },
                        {
                            text: 'ID_sberbank',
                            id: '1'
                        },
                        {
                            text: 'ID_other_bank',
                            id: '2'
                        },
                        {
                            text: 'ID_Yandex_money',
                            id: '3'
                        },
                        {
                            text: 'ID_paypal',
                            id: '5'
                        },
                        {
                            text: 'ID_payoneer',
                            id: '7'
                        }
                    ]
                },
                'ownership-type-options': {
                    item: [
                        {
                            text: 'ID_not_selected',
                            id: ''
                        },
                        {
                            text: 'Орган¸Ð·Ð°ÑÐ¸Ñ',
                            id: 'ORGANIZATION'
                        },
                        {
                            text: 'Ð¤Ð¸Ð·Ð¸ÑÐµÑÐºÐ¾Ðµ Ð»Ð¸ÑÐ¾',
                            id: 'PERSON'
                        },
                        {
                            text: 'ÐÐ½Ð´Ð¸Ð²Ð¸Ð´ÑÐ°Ð»ÑÐ½ÑÐ¹ Ð¿ÑÐµÐ´Ð¿ÑÐ¸Ð½Ð¸Ð¼Ð°ÑÐµÐ»Ñ',
                            id: 'INDIVIDUAL'
                        }
                    ]
                },
                'ownership-type-euyt-options': {
                    item: [
                        {
                            text: 'ID_not_selected',
                            id: ''
                        },
                        {
                            text: 'ÐÑÐ³Ð°Ð½Ð¸Ð·Ð°ÑÐ¸Ñ',
                            id: 'ORGANIZATION'
                        },
                        {
                            text: 'ÐÐ½Ð´Ð¸Ð²Ð¸Ð´ÑÐ°Ð»ÑÐ½ÑÐ¹ Ð¿ÑÐµÐ´Ð¿ÑÐ¸Ð½Ð¸Ð¼Ð°ÑÐµÐ»Ñ',
                            id: 'INDIVIDUAL'
                        }
                    ]
                },
                'revise-act-options': {
                    item: [
                        {
                            text: 'ID_not_selected',
                            id: ''
                        },
                        {
                            text: 'ID_monthly',
                            id: '1'
                        },
                        {
                            text: 'ID_quarterly',
                            id: '2'
                        },
                        {
                            text: 'ID_year_end',
                            id: '3'
                        }
                    ]
                },
                block: [
                    {
                        threaded: 'no',
                        timeout: '350000',
                        nameref: 'MuzzleServantRef',
                        method: 'get_edo_types',
                        param: {
                            text: 'skip',
                            type: 'StateArg',
                            as: 'Long',
                            default: '1'
                        }
                    },
                    {
                        threaded: 'no',
                        timeout: '350000',
                        nameref: 'MuzzleServantRef',
                        method: 'check_perm',
                        param: [
                            {
                                type: 'State'
                            },
                            {
                                text: 'passport_id',
                                type: 'StateArg',
                                as: 'LongLong'
                            },
                            {
                                text: 'AdminAccess',
                                type: 'String'
                            },
                            {
                                text: '0',
                                type: 'Long'
                            }
                        ]
                    },
                    {
                        threaded: 'no',
                        timeout: '350000',
                        nameref: 'MuzzleServantRef',
                        method: 'check_perm',
                        param: [
                            {
                                type: 'State'
                            },
                            {
                                text: 'passport_id',
                                type: 'StateArg',
                                as: 'LongLong'
                            },
                            {
                                text: 'PersonPostAddressEdit',
                                type: 'String'
                            },
                            {
                                text: '0',
                                type: 'Long'
                            }
                        ]
                    },
                    {
                        threaded: 'no',
                        timeout: '350000',
                        nameref: 'MuzzleServantRef',
                        method: 'check_perm',
                        param: [
                            {
                                type: 'State'
                            },
                            {
                                text: 'passport_id',
                                type: 'StateArg',
                                as: 'LongLong'
                            },
                            {
                                text: 'LocalNamesMaster',
                                type: 'String'
                            },
                            {
                                text: '0',
                                type: 'Long'
                            }
                        ]
                    },
                    {
                        threaded: 'no',
                        timeout: '350000',
                        nameref: 'MuzzleServantRef',
                        method: 'is_from_crimea',
                        param: [
                            {
                                text: 'skip',
                                type: 'StateArg',
                                as: 'Long',
                                default: '1'
                            },
                            {
                                type: 'State'
                            }
                        ]
                    }
                ]
            }
        }
    },
    ytPersonform: {
        request: ['/static/personforms/json/yt.json', undefined, false, false, false],
        response: {
            'person-details': {
                type: 'yt',
                'is-partner': '0',
                caption: 'ID_Legal_entity',
                name: 'ID_Nonresident',
                'details-block': [
                    {
                        title: 'ID_Contact_details',
                        detail: [
                            {
                                id: 'name',
                                locked: 'flex',
                                caption: 'ID_Organisation_name',
                                type: 'text',
                                size: '256',
                                'required-mark': '',
                                'check-type': 'not_empty'
                            },
                            {
                                id: 'country-id',
                                caption: 'ID_Region_Code',
                                locked: 'flex',
                                type: 'select1',
                                'edit-only': '',
                                'required-mark': '',
                                datasource: 'regions',
                                hint: 'ID_Region_Code_hint',
                                'check-type': 'not_empty'
                            },
                            {
                                id: 'phone',
                                caption: 'ID_Telephone',
                                type: 'text',
                                size: '32',
                                'required-mark': '',
                                'check-type': 'not_empty',
                                hint: 'ID_with_country_and_area_code'
                            },
                            {
                                id: 'fax',
                                caption: 'ID_Fax',
                                type: 'text',
                                size: '32'
                            },
                            {
                                id: 'email',
                                caption: 'ID_Email',
                                type: 'text',
                                size: '256',
                                'required-mark': '',
                                'check-type': 'email'
                            },
                            {
                                id: 'representative',
                                caption: 'ID_Contact',
                                type: 'text',
                                size: '128'
                            }
                        ]
                    },
                    {
                        title: 'ID_Address',
                        detail: [
                            {
                                id: 'postcode',
                                caption: 'ID_Postcode',
                                type: 'text',
                                size: '32'
                            },
                            {
                                id: 'address',
                                caption: 'ID_Post_address',
                                type: 'text',
                                'required-mark': '',
                                'check-type': 'not_empty',
                                size: '256'
                            },
                            {
                                'admin-only': '',
                                id: 'invalid-address',
                                type: 'checkbox',
                                caption: 'ID_Invalid_address'
                            }
                        ]
                    },
                    {
                        title: 'ID_Payment_details',
                        detail: [
                            {
                                id: 'invalid-bankprops',
                                caption: 'ID_Invalid_bankprops',
                                type: 'checkbox',
                                'admin-only': ''
                            },
                            {
                                id: 'longname',
                                locked: 'flex',
                                caption: 'ID_Organizations_full_name',
                                type: 'text',
                                size: '256',
                                'required-mark': '',
                                'check-type': 'not_empty',
                                sample: 'Yandex LLC',
                                hint: 'ID_business_type_abbreviation'
                            },
                            {
                                id: 'legal-address-postcode',
                                caption: 'ID_Postcode_legal',
                                type: 'text',
                                size: '32'
                            },
                            {
                                id: 'legaladdress',
                                locked: 'flex',
                                caption: 'ID_Legal_address',
                                type: 'textarea',
                                size: '256',
                                'required-mark': '',
                                'check-type': 'not_empty'
                            },
                            {
                                id: 'bank',
                                caption: 'ID_Bank_name',
                                type: 'text',
                                size: '128',
                                hint: 'ID_wo_quotation_marks_and_acronyms'
                            },
                            {
                                id: 'account',
                                caption: 'ID_Settlement_account',
                                type: 'text',
                                size: '32'
                            }
                        ]
                    },
                    {
                        title: '',
                        'admin-only': '',
                        detail: [
                            {
                                id: 'delivery-type',
                                caption: 'ID_Delivery_type',
                                source: 'delivery-type-options',
                                type: 'select1'
                            },
                            {
                                id: 'delivery-city',
                                'backoffice-only': '',
                                caption: 'ID_Servicing_office',
                                source: 'delivery-cities',
                                type: 'select1'
                            },
                            {
                                id: 'live-signature',
                                caption: 'ID_Live_signature',
                                type: 'checkbox'
                            },
                            {
                                id: 'signer-person-name',
                                caption: 'ID_Signers_full_name',
                                type: 'text',
                                size: '100'
                            },
                            {
                                id: 'signer-person-gender',
                                caption: 'ID_Signers_gender',
                                type: 'select1',
                                source: 'gender'
                            },
                            {
                                id: 'signer-position-name',
                                caption: 'ID_Signers_position',
                                type: 'flexselect1',
                                size: '100',
                                source: 'positions'
                            },
                            {
                                id: 'authority-doc-type',
                                caption: 'ID_Authority',
                                type: 'select1',
                                source: 'authority-doc-type'
                            },
                            {
                                id: 'authority-doc-details',
                                caption: 'ID_Authority__details',
                                type: 'text',
                                size: '512'
                            },
                            {
                                id: 'vip',
                                'backoffice-only': '',
                                type: 'checkbox',
                                caption: 'ID_VIP'
                            },
                            {
                                id: 'early-docs',
                                'backoffice-only': '',
                                type: 'checkbox',
                                caption: 'ID_Early_Docs_shipment'
                            }
                        ]
                    }
                ]
            }
        }
    },
    person: {
        request: [{ url: `${HOST}/person`, data: { person_id: '19031126' } }],
        response: camelCasePropNames({
            allow_archive: true,
            kbk: null,
            tva_number: null,
            kbe: null,
            legal_address_district: null,
            postcode: '123456',
            oktmo: null,
            corraccount: null,
            delivery_city: null,
            bank_data: null,
            kz_in: null,
            post2: null,
            post1: null,
            local_authority_doc_details: null,
            birthplace_region: null,
            hidden: false,
            legal_address_code: null,
            fax: '+7(321)654-87-09',
            bik: null,
            legal_address_home: null,
            pingpong_wallet: null,
            dt: '2021-12-09T18:35:18',
            legal_fias_guid: null,
            bank: '\u0411\u0430\u0431\u0430\u043d\u043a',
            kpp: null,
            address_city: null,
            name: '\u042f\u043d\u0434\u0435\u043a\u0441',
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
            street: null,
            is_batch_supported: true,
            legal_address_gni: null,
            early_docs: 0,
            authority_doc_details: '\u041e\u0441\u043d\u043e\u0432\u0430\u043d\u0438\u0435',
            country_id: '225',
            address_district: null,
            address_home: null,
            rn: null,
            email: 'yndx-enovikov11@yandex.ru',
            api_version: null,
            local_representative: null,
            person_account: null,
            mname: null,
            bankcity: null,
            export_dt: null,
            legal_address_flat: null,
            iban: null,
            ui_editable: false,
            address:
                '\u0433. \u041c\u043e\u0441\u043a\u0432\u0430, \u0443\u043b. \u041b\u044c\u0432\u0430 \u0422\u043e\u043b\u0441\u0442\u043e\u0433\u043e, \u0434. 16',
            swift: null,
            kz_kbe: null,
            passport_code: null,
            region: '225',
            local_city: null,
            payment_purpose: null,
            bank_type: 0,
            birthday: null,
            passport_birthplace: null,
            local_name: null,
            address_construction: null,
            passport_id: '1120000000159908',
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
            city: null,
            paypal_wallet: null,
            revise_act_period_type: null,
            operator_uid: null,
            ben_bank: null,
            authority_doc_type:
                '\u041f\u043e\u043b\u043e\u0436\u0435\u043d\u0438\u0435 \u043e \u0444\u0438\u043b\u0438\u0430\u043b\u0435',
            fias_guid: null,
            is_partner: false,
            type: 'yt',
            jpc: null,
            webmoney_wallet: null,
            sensible_name: 'Yandex',
            inn_doc_details: null,
            bank_inn: null,
            memo: null,
            legal_address_street: null,
            phone: '+7(123)456-78-90',
            fps_bank: null,
            pfr: null,
            representative:
                '\u041c\u0438\u0441\u0442\u0435\u0440 \u041f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a',
            client_id: 1354815216,
            invalid_bankprops: false,
            legal_address_construction: null,
            fps_phone: null,
            iik: null,
            need_deal_passport: true,
            corr_swift: null,
            account: null,
            invoice_count: 0,
            short_signer_person_name: null,
            signer_person_gender: 'X',
            legaladdress:
                '\u0433. \u041c\u043e\u0441\u043a\u0432\u0430, \u0443\u043b. \u041b\u044c\u0432\u0430 \u0422\u043e\u043b\u0441\u0442\u043e\u0433\u043e, \u0434. 16',
            us_state: null,
            address_building: null,
            invalid_address: false,
            legal_address_building: null,
            attr_meta_ignored: [],
            legal_address_city: null,
            is_new: false,
            ben_bank_code: null,
            inn: null,
            il_id: null,
            local_postaddress: null,
            delivery_type: 0,
            id: 19031126,
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
            is_ur: true,
            live_signature: 0,
            signer_position_name: '\u041f\u0440\u0435\u0437\u0438\u0434\u0435\u043d\u0442',
            ben_account: null,
            address_gni: null,
            address_town: null,
            legal_address_postcode: '123456',
            birthplace_country: null,
            longname: 'Yandex',
            local_bank: null,
            signer_person_name: null,
            ownership_type_ui: null,
            local_longname: null,
            birthplace_district: null,
            verified_docs: 0,
            legal_sample:
                '\u0433. \u041c\u043e\u0441\u043a\u0432\u0430, \u0443\u043b. \u041b\u044c\u0432\u0430 \u0422\u043e\u043b\u0441\u0442\u043e\u0433\u043e, \u0434. 16',
            local_ben_bank: null,
            payoneer_wallet: null,
            legal_address_town: null,
            true_kz: 0,
            organization: null,
            envelope_address: '',
            auto_gen: 0,
            postsuffix: null
        })
    },
    banks: {
        request: [{ url: `${HOST}/person/fps-banks`, data: { front_id: 10 } }],
        response: camelCasePropNames({
            total_count: 38,
            items: [
                {
                    cc: 'ROSSIYA',
                    hidden: 0,
                    name: '\u0410\u0411 \u0420\u041e\u0421\u0421\u0418\u042f'
                },
                {
                    cc: 'ABSOLUT',
                    hidden: 0,
                    name: '\u0410\u0431\u0441\u043e\u043b\u044e\u0442 \u0411\u0430\u043d\u043a'
                },
                {
                    cc: 'AK BARS',
                    hidden: 0,
                    name: '\u0410\u043a \u0411\u0430\u0440\u0441 \u0411\u0430\u043d\u043a'
                },
                {
                    cc: 'ALFA',
                    hidden: 0,
                    name: '\u0410\u043b\u044c\u0444\u0430 \u0411\u0430\u043d\u043a'
                },
                {
                    cc: 'AVANGARD',
                    hidden: 0,
                    name:
                        '\u0411\u0430\u043d\u043a \u0410\u0412\u0410\u041d\u0413\u0410\u0420\u0414'
                },
                {
                    cc: 'ACCEPT',
                    hidden: 0,
                    name: '\u0411\u0430\u043d\u043a \u0410\u043a\u0446\u0435\u043f\u0442'
                },
                {
                    cc: 'RRDB',
                    hidden: 0,
                    name: '\u0411\u0430\u043d\u043a \u0412\u0411\u0420\u0420'
                },
                {
                    cc: 'PSCB',
                    hidden: 0,
                    name: '\u0411\u0430\u043d\u043a \u041f\u0421\u041a\u0411'
                },
                {
                    cc: 'DEVELOPMENT CAPITAL',
                    hidden: 0,
                    name:
                        '\u0411\u0430\u043d\u043a \u0420\u0430\u0437\u0432\u0438\u0442\u0438\u0435-\u0421\u0442\u043e\u043b\u0438\u0446\u0430'
                },
                {
                    cc: 'RESO CREDIT',
                    hidden: 0,
                    name:
                        '\u0411\u0430\u043d\u043a \u0420\u0415\u0421\u041e \u041a\u0440\u0435\u0434\u0438\u0442'
                },
                {
                    cc: 'RUSSIAN STANDARD',
                    hidden: 0,
                    name:
                        '\u0411\u0430\u043d\u043a \u0420\u0443\u0441\u0441\u043a\u0438\u0439 \u0421\u0442\u0430\u043d\u0434\u0430\u0440\u0442'
                },
                {
                    cc: 'FINAM',
                    hidden: 0,
                    name: '\u0411\u0430\u043d\u043a \u0424\u0418\u041d\u0410\u041c'
                },
                {
                    cc: 'OTKRITIE',
                    hidden: 0,
                    name:
                        '\u0411\u0430\u043d\u043a \u0424\u041a \u041e\u0442\u043a\u0440\u044b\u0442\u0438\u0435'
                },
                {
                    cc: 'banka',
                    hidden: 1,
                    name:
                        '\u0411\u0430\u043d\u041a\u0410 3\u0445-\u043b\u0438\u0442\u0440\u043e\u0432\u0430\u044f'
                },
                {
                    cc: 'BYSTROBANK',
                    hidden: 0,
                    name: '\u0411\u044b\u0441\u0442\u0440\u043e\u0411\u0430\u043d\u043a'
                },
                { cc: 'VTB', hidden: 0, name: '\u0412\u0422\u0411' },
                {
                    cc: 'GAZENERGOBANK',
                    hidden: 0,
                    name:
                        '\u0413\u0430\u0437\u044d\u043d\u0435\u0440\u0433\u043e\u0431\u0430\u043d\u043a'
                },
                {
                    cc: 'MODULBANK',
                    hidden: 0,
                    name:
                        '\u041a\u0411 \u041c\u043e\u0434\u0443\u043b\u044c\u0431\u0430\u043d\u043a'
                },
                {
                    cc: 'PLATINA',
                    hidden: 0,
                    name: '\u041a\u0411 \u041f\u041b\u0410\u0422\u0418\u041d\u0410'
                },
                {
                    cc: 'KHLYNOV',
                    hidden: 0,
                    name: '\u041a\u0411 \u0425\u043b\u044b\u043d\u043e\u0432'
                },
                { cc: 'KS BANK', hidden: 0, name: '\u041a\u0421 \u0411\u0410\u041d\u041a' },
                { cc: 'MONETA', hidden: 0, name: '\u041c\u041e\u041d\u0415\u0422\u0410' },
                {
                    cc: 'CREDIT BANK OF MOSCOW',
                    hidden: 0,
                    name:
                        '\u041c\u043e\u0441\u043a\u043e\u0432\u0441\u043a\u0438\u0439 \u041a\u0440\u0435\u0434\u0438\u0442\u043d\u044b\u0439 \u0411\u0430\u043d\u043a'
                },
                { cc: 'MSP', hidden: 0, name: '\u041c\u0421\u041f \u0411\u0430\u043d\u043a' },
                {
                    cc: 'MTS Bank',
                    hidden: 0,
                    name: '\u041c\u0422\u0421-\u0411\u0430\u043d\u043a'
                },
                {
                    cc: 'POST BANK',
                    hidden: 0,
                    name: '\u041f\u043e\u0447\u0442\u0430 \u0411\u0430\u043d\u043a'
                },
                {
                    cc: 'RAIFFEISEN',
                    hidden: 0,
                    name:
                        '\u0420\u0430\u0439\u0444\u0444\u0430\u0439\u0437\u0435\u043d\u0431\u0430\u043d\u043a'
                },
                {
                    cc: 'ROSBANK',
                    hidden: 0,
                    name: '\u0420\u043e\u0441\u0431\u0430\u043d\u043a'
                },
                {
                    cc: 'ROSSELKHOZBANK',
                    hidden: 0,
                    name:
                        '\u0420\u043e\u0441\u0441\u0435\u043b\u044c\u0445\u043e\u0437\u0431\u0430\u043d\u043a'
                },
                {
                    cc: 'RFS',
                    hidden: 0,
                    name:
                        '\u0420\u0443\u0441\u0441\u043a\u043e\u0435 \u0444\u0438\u043d\u0430\u043d\u0441\u043e\u0432\u043e\u0435 \u043e\u0431\u0449\u0435\u0441\u0442\u0432\u043e'
                },
                {
                    cc: 'sber',
                    hidden: 1,
                    name:
                        '\u0421\u0431\u0435\u0440\u0431\u0430\u043d\u043a \u0434\u043b\u044f \u0431\u0430\u0431\u0443\u0441\u0435\u043a'
                },
                {
                    cc: 'SIBSOCBANK',
                    hidden: 0,
                    name: '\u0421\u0418\u0411\u0421\u041e\u0426\u0411\u0410\u041d\u041a'
                },
                {
                    cc: 'TINKOFF',
                    hidden: 0,
                    name:
                        '\u0422\u0438\u043d\u044c\u043a\u043e\u0444\u0444 \u0411\u0430\u043d\u043a'
                },
                {
                    cc: 'TOCHKA',
                    hidden: 0,
                    name:
                        '\u0422\u041e\u0427\u041a\u0410 (\u0424\u041a \u041e\u0422\u041a\u0420\u042b\u0422\u0418\u0415)'
                },
                {
                    cc: 'KHAKAS MUNICIPAL',
                    hidden: 0,
                    name:
                        '\u0425\u0430\u043a\u0430\u0441\u0441\u043a\u0438\u0439 \u043c\u0443\u043d\u0438\u0446\u0438\u043f\u0430\u043b\u044c\u043d\u044b\u0439 \u0431\u0430\u043d\u043a'
                },
                {
                    cc: 'EXPOBANK',
                    hidden: 0,
                    name: '\u042d\u043a\u0441\u043f\u043e\u0431\u0430\u043d\u043a'
                },
                { cc: 'ELPLAT', hidden: 0, name: '\u042d\u041b\u041f\u041b\u0410\u0422' },
                { cc: 'YOOMONEY', hidden: 0, name: '\u042e\u041c\u0430\u043d\u0438' }
            ]
        })
    },
    regions: {
        request: [
            {
                url: `${HOST}/geo/regions`,
                data: { lang: 'ru', region_type: 'COUNTRIES' }
            }
        ],
        response: camelCasePropNames({
            ru: [
                { id: 29386, name: '\u0410\u0431\u0445\u0430\u0437\u0438\u044f' },
                { id: 211, name: '\u0410\u0432\u0441\u0442\u0440\u0430\u043b\u0438\u044f' },
                { id: 113, name: '\u0410\u0432\u0441\u0442\u0440\u0438\u044f' },
                {
                    id: 167,
                    name: '\u0410\u0437\u0435\u0440\u0431\u0430\u0439\u0434\u0436\u0430\u043d'
                },
                { id: 10054, name: '\u0410\u043b\u0431\u0430\u043d\u0438\u044f' },
                { id: 20826, name: '\u0410\u043b\u0436\u0438\u0440' },
                {
                    id: 21553,
                    name:
                        '\u0410\u043c\u0435\u0440\u0438\u043a\u0430\u043d\u0441\u043a\u0438\u0435 \u0412\u0438\u0440\u0433\u0438\u043d\u0441\u043a\u0438\u0435 \u043e\u0441\u0442\u0440\u043e\u0432\u0430'
                },
                { id: 21182, name: '\u0410\u043d\u0433\u043e\u043b\u0430' },
                { id: 10088, name: '\u0410\u043d\u0434\u043e\u0440\u0440\u0430' },
                {
                    id: 20856,
                    name:
                        '\u0410\u043d\u0442\u0438\u0433\u0443\u0430 \u0438 \u0411\u0430\u0440\u0431\u0443\u0434\u0430'
                },
                { id: 93, name: '\u0410\u0440\u0433\u0435\u043d\u0442\u0438\u043d\u0430' },
                { id: 168, name: '\u0410\u0440\u043c\u0435\u043d\u0438\u044f' },
                { id: 21536, name: '\u0410\u0440\u0443\u0431\u0430' },
                {
                    id: 10090,
                    name: '\u0410\u0444\u0433\u0430\u043d\u0438\u0441\u0442\u0430\u043d'
                },
                {
                    id: 21325,
                    name:
                        '\u0411\u0430\u0433\u0430\u043c\u0441\u043a\u0438\u0435 \u043e\u0441\u0442\u0440\u043e\u0432\u0430'
                },
                { id: 10091, name: '\u0411\u0430\u043d\u0433\u043b\u0430\u0434\u0435\u0448' },
                { id: 21019, name: '\u0411\u0430\u0440\u0431\u0430\u0434\u043e\u0441' },
                { id: 10532, name: '\u0411\u0430\u0445\u0440\u0435\u0439\u043d' },
                { id: 149, name: '\u0411\u0435\u043b\u0430\u0440\u0443\u0441\u044c' },
                { id: 21544, name: '\u0411\u0435\u043b\u0438\u0437' },
                { id: 114, name: '\u0411\u0435\u043b\u044c\u0433\u0438\u044f' },
                { id: 20869, name: '\u0411\u0435\u043d\u0438\u043d' },
                {
                    id: 21546,
                    name:
                        '\u0411\u0435\u0440\u043c\u0443\u0434\u0441\u043a\u0438\u0435 \u041e\u0441\u0442\u0440\u043e\u0432\u0430'
                },
                { id: 115, name: '\u0411\u043e\u043b\u0433\u0430\u0440\u0438\u044f' },
                { id: 10015, name: '\u0411\u043e\u043b\u0438\u0432\u0438\u044f' },
                {
                    id: 10057,
                    name:
                        '\u0411\u043e\u0441\u043d\u0438\u044f \u0438 \u0413\u0435\u0440\u0446\u0435\u0433\u043e\u0432\u0438\u043d\u0430'
                },
                { id: 21239, name: '\u0411\u043e\u0442\u0441\u0432\u0430\u043d\u0430' },
                { id: 94, name: '\u0411\u0440\u0430\u0437\u0438\u043b\u0438\u044f' },
                {
                    id: 21559,
                    name:
                        '\u0411\u0440\u0438\u0442\u0430\u043d\u0441\u043a\u0438\u0435 \u0412\u0438\u0440\u0433\u0438\u043d\u0441\u043a\u0438\u0435 \u043e\u0441\u0442\u0440\u043e\u0432\u0430'
                },
                { id: 20274, name: '\u0411\u0440\u0443\u043d\u0435\u0439' },
                {
                    id: 21165,
                    name: '\u0411\u0443\u0440\u043a\u0438\u043d\u0430-\u0424\u0430\u0441\u043e'
                },
                { id: 21214, name: '\u0411\u0443\u0440\u0443\u043d\u0434\u0438' },
                { id: 21550, name: '\u0411\u0443\u0442\u0430\u043d' },
                { id: 21556, name: '\u0412\u0430\u043d\u0443\u0430\u0442\u0443' },
                { id: 21359, name: '\u0412\u0430\u0442\u0438\u043a\u0430\u043d' },
                {
                    id: 102,
                    name:
                        '\u0412\u0435\u043b\u0438\u043a\u043e\u0431\u0440\u0438\u0442\u0430\u043d\u0438\u044f'
                },
                { id: 116, name: '\u0412\u0435\u043d\u0433\u0440\u0438\u044f' },
                { id: 21184, name: '\u0412\u0435\u043d\u0435\u0441\u0443\u044d\u043b\u0430' },
                {
                    id: 21562,
                    name:
                        '\u0412\u043e\u0441\u0442\u043e\u0447\u043d\u044b\u0439 \u0422\u0438\u043c\u043e\u0440'
                },
                { id: 10093, name: '\u0412\u044c\u0435\u0442\u043d\u0430\u043c' },
                { id: 21137, name: '\u0413\u0430\u0431\u043e\u043d' },
                { id: 21321, name: '\u0413\u0430\u0438\u0442\u0438' },
                { id: 21477, name: '\u0413\u0430\u0439\u0430\u043d\u0430' },
                { id: 21010, name: '\u0413\u0430\u043c\u0431\u0438\u044f' },
                { id: 20802, name: '\u0413\u0430\u043d\u0430' },
                { id: 20968, name: '\u0413\u0432\u0430\u0442\u0435\u043c\u0430\u043b\u0430' },
                { id: 20818, name: '\u0413\u0432\u0438\u043d\u0435\u044f' },
                {
                    id: 21143,
                    name: '\u0413\u0432\u0438\u043d\u0435\u044f-\u0411\u0438\u0441\u0430\u0443'
                },
                { id: 96, name: '\u0413\u0435\u0440\u043c\u0430\u043d\u0438\u044f' },
                { id: 10089, name: '\u0413\u0438\u0431\u0440\u0430\u043b\u0442\u0430\u0440' },
                { id: 21175, name: '\u0413\u043e\u043d\u0434\u0443\u0440\u0430\u0441' },
                { id: 21426, name: '\u0413\u0440\u0435\u043d\u0430\u0434\u0430' },
                {
                    id: 21567,
                    name: '\u0413\u0440\u0435\u043d\u043b\u0430\u043d\u0434\u0438\u044f'
                },
                { id: 246, name: '\u0413\u0440\u0435\u0446\u0438\u044f' },
                { id: 169, name: '\u0413\u0440\u0443\u0437\u0438\u044f' },
                { id: 20747, name: '\u0413\u0443\u0430\u043c' },
                { id: 203, name: '\u0414\u0430\u043d\u0438\u044f' },
                {
                    id: 20762,
                    name:
                        '\u0414\u0435\u043c\u043e\u043a\u0440\u0430\u0442\u0438\u0447\u0435\u0441\u043a\u0430\u044f \u0420\u0435\u0441\u043f\u0443\u0431\u043b\u0438\u043a\u0430 \u041a\u043e\u043d\u0433\u043e'
                },
                { id: 21475, name: '\u0414\u0436\u0438\u0431\u0443\u0442\u0438' },
                { id: 20746, name: '\u0414\u043e\u043c\u0438\u043d\u0438\u043a\u0430' },
                {
                    id: 20917,
                    name:
                        '\u0414\u043e\u043c\u0438\u043d\u0438\u043a\u0430\u043d\u0441\u043a\u0430\u044f \u0420\u0435\u0441\u043f\u0443\u0431\u043b\u0438\u043a\u0430'
                },
                { id: 1056, name: '\u0415\u0433\u0438\u043f\u0435\u0442' },
                { id: 21196, name: '\u0417\u0430\u043c\u0431\u0438\u044f' },
                { id: 20954, name: '\u0417\u0438\u043c\u0431\u0430\u0431\u0432\u0435' },
                { id: 181, name: '\u0418\u0437\u0440\u0430\u0438\u043b\u044c' },
                { id: 994, name: '\u0418\u043d\u0434\u0438\u044f' },
                { id: 10095, name: '\u0418\u043d\u0434\u043e\u043d\u0435\u0437\u0438\u044f' },
                { id: 10535, name: '\u0418\u043e\u0440\u0434\u0430\u043d\u0438\u044f' },
                { id: 20572, name: '\u0418\u0440\u0430\u043a' },
                { id: 10536, name: '\u0418\u0440\u0430\u043d' },
                { id: 10063, name: '\u0418\u0440\u043b\u0430\u043d\u0434\u0438\u044f' },
                { id: 10064, name: '\u0418\u0441\u043b\u0430\u043d\u0434\u0438\u044f' },
                { id: 204, name: '\u0418\u0441\u043f\u0430\u043d\u0438\u044f' },
                { id: 205, name: '\u0418\u0442\u0430\u043b\u0438\u044f' },
                { id: 21551, name: '\u0419\u0435\u043c\u0435\u043d' },
                { id: 21326, name: '\u041a\u0430\u0431\u043e-\u0412\u0435\u0440\u0434\u0435' },
                { id: 159, name: '\u041a\u0430\u0437\u0430\u0445\u0441\u0442\u0430\u043d' },
                {
                    id: 21570,
                    name:
                        '\u041a\u0430\u0439\u043c\u0430\u043d\u043e\u0432\u044b \u043e\u0441\u0442\u0440\u043e\u0432\u0430'
                },
                { id: 20975, name: '\u041a\u0430\u043c\u0431\u043e\u0434\u0436\u0430' },
                { id: 20736, name: '\u041a\u0430\u043c\u0435\u0440\u0443\u043d' },
                { id: 95, name: '\u041a\u0430\u043d\u0430\u0434\u0430' },
                { id: 21486, name: '\u041a\u0430\u0442\u0430\u0440' },
                { id: 21223, name: '\u041a\u0435\u043d\u0438\u044f' },
                { id: 20574, name: '\u041a\u0438\u043f\u0440' },
                { id: 207, name: '\u041a\u0438\u0440\u0433\u0438\u0437\u0438\u044f' },
                { id: 21572, name: '\u041a\u0438\u0440\u0438\u0431\u0430\u0442\u0438' },
                { id: 134, name: '\u041a\u0438\u0442\u0430\u0439' },
                { id: 21191, name: '\u041a\u043e\u043b\u0443\u043c\u0431\u0438\u044f' },
                {
                    id: 21297,
                    name:
                        '\u041a\u043e\u043c\u043e\u0440\u0441\u043a\u0438\u0435 \u043e\u0441\u0442\u0440\u043e\u0432\u0430'
                },
                { id: 21131, name: '\u041a\u043e\u0441\u0442\u0430-\u0420\u0438\u043a\u0430' },
                {
                    id: 20733,
                    name: '\u041a\u043e\u0442-\u0434\u2019\u0418\u0432\u0443\u0430\u0440'
                },
                { id: 10017, name: '\u041a\u0443\u0431\u0430' },
                { id: 10537, name: '\u041a\u0443\u0432\u0435\u0439\u0442' },
                { id: 21538, name: '\u041a\u044e\u0440\u0430\u0441\u0430\u043e' },
                { id: 20972, name: '\u041b\u0430\u043e\u0441' },
                { id: 206, name: '\u041b\u0430\u0442\u0432\u0438\u044f' },
                { id: 21261, name: '\u041b\u0435\u0441\u043e\u0442\u043e' },
                { id: 21278, name: '\u041b\u0438\u0431\u0435\u0440\u0438\u044f' },
                { id: 10538, name: '\u041b\u0438\u0432\u0430\u043d' },
                { id: 10023, name: '\u041b\u0438\u0432\u0438\u044f' },
                { id: 117, name: '\u041b\u0438\u0442\u0432\u0430' },
                {
                    id: 10067,
                    name: '\u041b\u0438\u0445\u0442\u0435\u043d\u0448\u0442\u0435\u0439\u043d'
                },
                {
                    id: 21203,
                    name: '\u041b\u044e\u043a\u0441\u0435\u043c\u0431\u0443\u0440\u0433'
                },
                { id: 21241, name: '\u041c\u0430\u0432\u0440\u0438\u043a\u0438\u0439' },
                {
                    id: 21349,
                    name: '\u041c\u0430\u0432\u0440\u0438\u0442\u0430\u043d\u0438\u044f'
                },
                {
                    id: 20854,
                    name: '\u041c\u0430\u0434\u0430\u0433\u0430\u0441\u043a\u0430\u0440'
                },
                { id: 21151, name: '\u041c\u0430\u043b\u0430\u0432\u0438' },
                { id: 10097, name: '\u041c\u0430\u043b\u0430\u0439\u0437\u0438\u044f' },
                { id: 21004, name: '\u041c\u0430\u043b\u0438' },
                { id: 10098, name: '\u041c\u0430\u043b\u044c\u0434\u0438\u0432\u044b' },
                { id: 10069, name: '\u041c\u0430\u043b\u044c\u0442\u0430' },
                { id: 10020, name: '\u041c\u0430\u0440\u043e\u043a\u043a\u043e' },
                { id: 101521, name: '\u041c\u0430\u0440\u0442\u0438\u043d\u0438\u043a\u0430' },
                {
                    id: 21578,
                    name:
                        '\u041c\u0430\u0440\u0448\u0430\u043b\u043b\u043e\u0432\u044b \u043e\u0441\u0442\u0440\u043e\u0432\u0430'
                },
                { id: 20271, name: '\u041c\u0435\u043a\u0441\u0438\u043a\u0430' },
                { id: 21235, name: '\u041c\u043e\u0437\u0430\u043c\u0431\u0438\u043a' },
                { id: 208, name: '\u041c\u043e\u043b\u0434\u043e\u0432\u0430' },
                { id: 10070, name: '\u041c\u043e\u043d\u0430\u043a\u043e' },
                { id: 10099, name: '\u041c\u043e\u043d\u0433\u043e\u043b\u0438\u044f' },
                {
                    id: 37176,
                    name: '\u041c\u043e\u043d\u0442\u0441\u0435\u0440\u0440\u0430\u0442'
                },
                { id: 10100, name: '\u041c\u044c\u044f\u043d\u043c\u0430' },
                { id: 21217, name: '\u041d\u0430\u043c\u0438\u0431\u0438\u044f' },
                { id: 21582, name: '\u041d\u0430\u0443\u0440\u0443' },
                { id: 10101, name: '\u041d\u0435\u043f\u0430\u043b' },
                { id: 21339, name: '\u041d\u0438\u0433\u0435\u0440' },
                { id: 20741, name: '\u041d\u0438\u0433\u0435\u0440\u0438\u044f' },
                {
                    id: 118,
                    name: '\u041d\u0438\u0434\u0435\u0440\u043b\u0430\u043d\u0434\u044b'
                },
                { id: 21231, name: '\u041d\u0438\u043a\u0430\u0440\u0430\u0433\u0443\u0430' },
                { id: 98542, name: '\u041d\u0438\u0443\u044d' },
                {
                    id: 139,
                    name:
                        '\u041d\u043e\u0432\u0430\u044f \u0417\u0435\u043b\u0430\u043d\u0434\u0438\u044f'
                },
                {
                    id: 21584,
                    name:
                        '\u041d\u043e\u0432\u0430\u044f \u041a\u0430\u043b\u0435\u0434\u043e\u043d\u0438\u044f'
                },
                { id: 119, name: '\u041d\u043e\u0440\u0432\u0435\u0433\u0438\u044f' },
                { id: 98539, name: '\u041d\u043e\u0440\u0444\u043e\u043b\u043a' },
                {
                    id: 210,
                    name:
                        '\u041e\u0431\u044a\u0435\u0434\u0438\u043d\u0451\u043d\u043d\u044b\u0435 \u0410\u0440\u0430\u0431\u0441\u043a\u0438\u0435 \u042d\u043c\u0438\u0440\u0430\u0442\u044b'
                },
                { id: 21586, name: '\u041e\u043c\u0430\u043d' },
                {
                    id: 21574,
                    name: '\u041e\u0441\u0442\u0440\u043e\u0432\u0430 \u041a\u0443\u043a\u0430'
                },
                { id: 10102, name: '\u041f\u0430\u043a\u0438\u0441\u0442\u0430\u043d' },
                { id: 21589, name: '\u041f\u0430\u043b\u0430\u0443' },
                { id: 98552, name: '\u041f\u0430\u043b\u0435\u0441\u0442\u0438\u043d\u0430' },
                { id: 21299, name: '\u041f\u0430\u043d\u0430\u043c\u0430' },
                {
                    id: 20739,
                    name:
                        '\u041f\u0430\u043f\u0443\u0430-\u041d\u043e\u0432\u0430\u044f \u0413\u0432\u0438\u043d\u0435\u044f'
                },
                { id: 20992, name: '\u041f\u0430\u0440\u0430\u0433\u0432\u0430\u0439' },
                { id: 21156, name: '\u041f\u0435\u0440\u0443' },
                { id: 120, name: '\u041f\u043e\u043b\u044c\u0448\u0430' },
                {
                    id: 10074,
                    name: '\u041f\u043e\u0440\u0442\u0443\u0433\u0430\u043b\u0438\u044f'
                },
                {
                    id: 20764,
                    name: '\u041f\u0443\u044d\u0440\u0442\u043e-\u0420\u0438\u043a\u043e'
                },
                {
                    id: 21198,
                    name:
                        '\u0420\u0435\u0441\u043f\u0443\u0431\u043b\u0438\u043a\u0430 \u041a\u043e\u043d\u0433\u043e'
                },
                { id: 225, name: '\u0420\u043e\u0441\u0441\u0438\u044f' },
                { id: 21371, name: '\u0420\u0443\u0430\u043d\u0434\u0430' },
                { id: 10077, name: '\u0420\u0443\u043c\u044b\u043d\u0438\u044f' },
                { id: 84, name: '\u0421\u0428\u0410' },
                { id: 20769, name: '\u0421\u0430\u043b\u044c\u0432\u0430\u0434\u043e\u0440' },
                { id: 20860, name: '\u0421\u0430\u043c\u043e\u0430' },
                { id: 20790, name: '\u0421\u0430\u043d-\u041c\u0430\u0440\u0438\u043d\u043e' },
                {
                    id: 21199,
                    name:
                        '\u0421\u0430\u043d-\u0422\u043e\u043c\u0435 \u0438 \u041f\u0440\u0438\u043d\u0441\u0438\u043f\u0438'
                },
                {
                    id: 10540,
                    name:
                        '\u0421\u0430\u0443\u0434\u043e\u0432\u0441\u043a\u0430\u044f \u0410\u0440\u0430\u0432\u0438\u044f'
                },
                {
                    id: 20789,
                    name:
                        '\u0421\u0430\u0445\u0430\u0440\u0441\u043a\u0430\u044f \u0410\u0440\u0430\u0431\u0441\u043a\u0430\u044f \u0414\u0435\u043c\u043e\u043a\u0440\u0430\u0442\u0438\u0447\u0435\u0441\u043a\u0430\u044f \u0420\u0435\u0441\u043f\u0443\u0431\u043b\u0438\u043a\u0430'
                },
                {
                    id: 10104,
                    name:
                        '\u0421\u0435\u0432\u0435\u0440\u043d\u0430\u044f \u041a\u043e\u0440\u0435\u044f'
                },
                {
                    id: 10068,
                    name:
                        '\u0421\u0435\u0432\u0435\u0440\u043d\u0430\u044f \u041c\u0430\u043a\u0435\u0434\u043e\u043d\u0438\u044f'
                },
                {
                    id: 10022,
                    name:
                        '\u0421\u0435\u0439\u0448\u0435\u043b\u044c\u0441\u043a\u0438\u0435 \u043e\u0441\u0442\u0440\u043e\u0432\u0430'
                },
                { id: 21441, name: '\u0421\u0435\u043d\u0435\u0433\u0430\u043b' },
                {
                    id: 20754,
                    name:
                        '\u0421\u0435\u043d\u0442-\u0412\u0438\u043d\u0441\u0435\u043d\u0442 \u0438 \u0413\u0440\u0435\u043d\u0430\u0434\u0438\u043d\u044b'
                },
                {
                    id: 21042,
                    name:
                        '\u0421\u0435\u043d\u0442-\u041a\u0438\u0442\u0441 \u0438 \u041d\u0435\u0432\u0438\u0441'
                },
                { id: 21395, name: '\u0421\u0435\u043d\u0442-\u041b\u044e\u0441\u0438\u044f' },
                { id: 180, name: '\u0421\u0435\u0440\u0431\u0438\u044f' },
                { id: 10105, name: '\u0421\u0438\u043d\u0433\u0430\u043f\u0443\u0440' },
                {
                    id: 109724,
                    name: '\u0421\u0438\u043d\u0442-\u041c\u0430\u0440\u0442\u0435\u043d'
                },
                { id: 10542, name: '\u0421\u0438\u0440\u0438\u044f' },
                { id: 121, name: '\u0421\u043b\u043e\u0432\u0430\u043a\u0438\u044f' },
                { id: 122, name: '\u0421\u043b\u043e\u0432\u0435\u043d\u0438\u044f' },
                {
                    id: 20915,
                    name:
                        '\u0421\u043e\u043b\u043e\u043c\u043e\u043d\u043e\u0432\u044b \u043e\u0441\u0442\u0440\u043e\u0432\u0430'
                },
                { id: 21227, name: '\u0421\u043e\u043c\u0430\u043b\u0438' },
                { id: 20957, name: '\u0421\u0443\u0434\u0430\u043d' },
                { id: 21344, name: '\u0421\u0443\u0440\u0438\u043d\u0430\u043c' },
                {
                    id: 21219,
                    name: '\u0421\u044c\u0435\u0440\u0440\u0430-\u041b\u0435\u043e\u043d\u0435'
                },
                {
                    id: 209,
                    name: '\u0422\u0430\u0434\u0436\u0438\u043a\u0438\u0441\u0442\u0430\u043d'
                },
                { id: 995, name: '\u0422\u0430\u0438\u043b\u0430\u043d\u0434' },
                { id: 29385, name: '\u0422\u0430\u0439\u0432\u0430\u043d\u044c' },
                { id: 21208, name: '\u0422\u0430\u043d\u0437\u0430\u043d\u0438\u044f' },
                { id: 21171, name: '\u0422\u043e\u0433\u043e' },
                { id: 21599, name: '\u0422\u043e\u043d\u0433\u0430' },
                {
                    id: 21187,
                    name:
                        '\u0422\u0440\u0438\u043d\u0438\u0434\u0430\u0434 \u0438 \u0422\u043e\u0431\u0430\u0433\u043e'
                },
                { id: 21601, name: '\u0422\u0443\u0432\u0430\u043b\u0443' },
                { id: 10024, name: '\u0422\u0443\u043d\u0438\u0441' },
                {
                    id: 170,
                    name: '\u0422\u0443\u0440\u043a\u043c\u0435\u043d\u0438\u0441\u0442\u0430\u043d'
                },
                { id: 983, name: '\u0422\u0443\u0440\u0446\u0438\u044f' },
                {
                    id: 21595,
                    name:
                        '\u0422\u0451\u0440\u043a\u0441 \u0438 \u041a\u0430\u0439\u043a\u043e\u0441'
                },
                { id: 21230, name: '\u0423\u0433\u0430\u043d\u0434\u0430' },
                {
                    id: 171,
                    name: '\u0423\u0437\u0431\u0435\u043a\u0438\u0441\u0442\u0430\u043d'
                },
                { id: 187, name: '\u0423\u043a\u0440\u0430\u0438\u043d\u0430' },
                { id: 21289, name: '\u0423\u0440\u0443\u0433\u0432\u0430\u0439' },
                {
                    id: 21580,
                    name:
                        '\u0424\u0435\u0434\u0435\u0440\u0430\u0442\u0438\u0432\u043d\u044b\u0435 \u0428\u0442\u0430\u0442\u044b \u041c\u0438\u043a\u0440\u043e\u043d\u0435\u0437\u0438\u0438'
                },
                { id: 10030, name: '\u0424\u0438\u0434\u0436\u0438' },
                { id: 10108, name: '\u0424\u0438\u043b\u0438\u043f\u043f\u0438\u043d\u044b' },
                { id: 123, name: '\u0424\u0438\u043d\u043b\u044f\u043d\u0434\u0438\u044f' },
                {
                    id: 101519,
                    name:
                        '\u0424\u043e\u043b\u043a\u043b\u0435\u043d\u0434\u0441\u043a\u0438\u0435 \u043e\u0441\u0442\u0440\u043e\u0432\u0430'
                },
                { id: 124, name: '\u0424\u0440\u0430\u043d\u0446\u0438\u044f' },
                {
                    id: 21451,
                    name:
                        '\u0424\u0440\u0430\u043d\u0446\u0443\u0437\u0441\u043a\u0430\u044f \u0413\u0432\u0438\u0430\u043d\u0430'
                },
                {
                    id: 21330,
                    name:
                        '\u0424\u0440\u0430\u043d\u0446\u0443\u0437\u0441\u043a\u0430\u044f \u041f\u043e\u043b\u0438\u043d\u0435\u0437\u0438\u044f'
                },
                { id: 10083, name: '\u0425\u043e\u0440\u0432\u0430\u0442\u0438\u044f' },
                {
                    id: 21007,
                    name:
                        '\u0426\u0435\u043d\u0442\u0440\u0430\u043b\u044c\u043d\u043e\u0430\u0444\u0440\u0438\u043a\u0430\u043d\u0441\u043a\u0430\u044f \u0440\u0435\u0441\u043f\u0443\u0431\u043b\u0438\u043a\u0430'
                },
                { id: 21331, name: '\u0427\u0430\u0434' },
                {
                    id: 21610,
                    name: '\u0427\u0435\u0440\u043d\u043e\u0433\u043e\u0440\u0438\u044f'
                },
                { id: 125, name: '\u0427\u0435\u0445\u0438\u044f' },
                { id: 20862, name: '\u0427\u0438\u043b\u0438' },
                { id: 126, name: '\u0428\u0432\u0435\u0439\u0446\u0430\u0440\u0438\u044f' },
                { id: 127, name: '\u0428\u0432\u0435\u0446\u0438\u044f' },
                { id: 10109, name: '\u0428\u0440\u0438-\u041b\u0430\u043d\u043a\u0430' },
                { id: 20785, name: '\u042d\u043a\u0432\u0430\u0434\u043e\u0440' },
                {
                    id: 21045,
                    name:
                        '\u042d\u043a\u0432\u0430\u0442\u043e\u0440\u0438\u0430\u043b\u044c\u043d\u0430\u044f \u0413\u0432\u0438\u043d\u0435\u044f'
                },
                { id: 20989, name: '\u042d\u0440\u0438\u0442\u0440\u0435\u044f' },
                { id: 21251, name: '\u042d\u0441\u0432\u0430\u0442\u0438\u043d\u0438' },
                { id: 179, name: '\u042d\u0441\u0442\u043e\u043d\u0438\u044f' },
                { id: 20768, name: '\u042d\u0444\u0438\u043e\u043f\u0438\u044f' },
                { id: 10021, name: '\u042e\u0410\u0420' },
                {
                    id: 135,
                    name: '\u042e\u0436\u043d\u0430\u044f \u041a\u043e\u0440\u0435\u044f'
                },
                {
                    id: 29387,
                    name: '\u042e\u0436\u043d\u0430\u044f \u041e\u0441\u0435\u0442\u0438\u044f'
                },
                {
                    id: 108137,
                    name: '\u042e\u0436\u043d\u044b\u0439 \u0421\u0443\u0434\u0430\u043d'
                },
                { id: 10013, name: '\u042f\u043c\u0430\u0439\u043a\u0430' },
                { id: 137, name: '\u042f\u043f\u043e\u043d\u0438\u044f' }
            ]
        })
    },
    hidePerson: {
        request: [{ url: 'http://snout-test/person/hide-person', data: { person_id: 19031126 } }]
    }
};

export const perms = [];
