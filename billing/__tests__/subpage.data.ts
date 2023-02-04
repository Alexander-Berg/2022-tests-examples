import { HOST } from 'common/utils/test-utils/common';

const personResponse = {
    kbk: null,
    tvaNumber: null,
    kbe: null,
    legalAddressDistrict: null,
    postcode: null,
    oktmo: null,
    corraccount: null,
    deliveryCity: null,
    bankData: null,
    kzIn: null,
    post2: null,
    post1: null,
    localAuthorityDocDetails: null,
    birthplaceRegion: null,
    hidden: false,
    legalAddressCode: null,
    fax: null,
    bik: null,
    legalAddressHome: null,
    pingpongWallet: null,
    dt: '2021-11-15T15:41:28',
    legalFiasGuid: null,
    bank: null,
    kpp: null,
    addressCity: null,
    name:
        '\u0418\u0432\u0430\u043d\u043e\u0432 \u0418\u0432\u0430\u043d \u0418\u0432\u0430\u043d\u043e\u0432\u0438\u0447',
    vip: 0,
    kladrCode: null,
    addressUpdated: null,
    postaddress: null,
    localSignerPersonName: null,
    vatPayer: null,
    localLegaladdress: null,
    addressCode: null,
    oebsExportable: true,
    ogrn: null,
    street: null,
    isBatchSupported: true,
    legalAddressGni: null,
    earlyDocs: 0,
    authorityDocDetails: null,
    countryId: null,
    addressDistrict: null,
    addressHome: null,
    rn: null,
    email: 'balanceassessors@yandex.ru',
    apiVersion: null,
    localRepresentative: null,
    personAccount: null,
    mname: '\u0418\u0432\u0430\u043d\u043e\u0432\u0438\u0447',
    bankcity: null,
    exportDt: null,
    legalAddressFlat: null,
    iban: null,
    uiEditable: true,
    address: null,
    swift: null,
    kzKbe: null,
    passportCode: null,
    region: null,
    localCity: null,
    paymentPurpose: null,
    bankType: 0,
    birthday: null,
    passportBirthplace: null,
    localName: null,
    addressConstruction: null,
    passportId: '739250701',
    passportE: null,
    passportD: null,
    hasEdo: false,
    addressFlat: null,
    passportN: null,
    localSignerPositionName: null,
    legalAddressSuffix: null,
    passportS: null,
    addressPostcode: null,
    addressStreet: null,
    uiDeletable: true,
    city: null,
    paypalWallet: null,
    reviseActPeriodType: null,
    operatorUid: null,
    benBank: null,
    authorityDocType: null,
    fiasGuid: null,
    isPartner: false,
    type: 'ph',
    jpc: null,
    webmoneyWallet: null,
    sensibleName:
        '\u0418\u0432\u0430\u043d\u043e\u0432 \u0418\u0432\u0430\u043d \u0418\u0432\u0430\u043d\u043e\u0432\u0438\u0447',
    innDocDetails: null,
    bankInn: null,
    memo: null,
    legalAddressStreet: null,
    phone: '+7 905 1234567',
    fpsBank: null,
    pfr: null,
    representative: null,
    clientId: 1354268005,
    invalidBankprops: false,
    legalAddressConstruction: null,
    fpsPhone: null,
    iik: null,
    needDealPassport: false,
    corrSwift: null,
    account: null,
    invoiceCount: 1,
    shortSignerPersonName: null,
    signerPersonGender: null,
    legaladdress: null,
    usState: null,
    addressBuilding: null,
    invalidAddress: false,
    legalAddressBuilding: null,
    attrMetaIgnored: [],
    legalAddressCity: null,
    isNew: false,
    benBankCode: null,
    inn: null,
    ilId: null,
    localPostaddress: null,
    deliveryType: 0,
    id: 17672240,
    rnn: null,
    legalAddressRegion: null,
    ownershipType: null,
    taxType: null,
    lname: '\u0418\u0432\u0430\u043d\u043e\u0432',
    addressRegion: null,
    attrDirectAccess: ['_sa_instance_state', '_state', '_do_not_export'],
    other: null,
    fname: '\u0418\u0432\u0430\u043d',
    yamoneyWallet: null,
    localOther: null,
    isUr: false,
    liveSignature: 0,
    signerPositionName: null,
    benAccount: null,
    addressGni: null,
    addressTown: null,
    legalAddressPostcode: null,
    birthplaceCountry: null,
    longname: null,
    localBank: null,
    signerPersonName: null,
    ownershipTypeUi: 'PERSON',
    localLongname: null,
    birthplaceDistrict: null,
    verifiedDocs: 0,
    legalSample: null,
    localBenBank: null,
    payoneerWallet: null,
    legalAddressTown: null,
    trueKz: 0,
    organization: null,
    envelopeAddress: '',
    autoGen: 0,
    postsuffix: null
};

export const mocks = {
    client: {
        request: {
            url: `${HOST}/client`,
            data: {
                client_id: undefined
            }
        },
        response: {
            id: 114321624
        }
    },
    creatableCategories: {
        request: {
            url: `${HOST}/person/creatable-categories`,
            data: { client_id: '114321624', legal_entity: false, is_partner: false }
        },
        response: [
            {
                category: 'ph',
                caption: 'ID_Individual_ex',
                name: 'ID_Individual',
                is_partner: true,
                legal_entity: false
            },
            {
                category: 'ph',
                caption: 'ID_Individual_ex',
                name: 'ID_Individual',
                is_partner: false,
                legal_entity: false
            }
        ]
    },
    personCategory: {
        request: {
            url: `${HOST}/person/person-category`,
            data: { personId: '234' }
        },
        response: {
            category: 'ph',
            caption: 'ID_Individual_ex',
            name: 'ID_Individual',
            is_partner: true,
            legal_entity: false
        }
    },
    person: {
        request: {
            url: `${HOST}/person`,
            data: {
                person_id: '234'
            }
        },
        response: personResponse
    },
    partnerPerson: {
        request: {
            url: `${HOST}/person`,
            data: {
                person_id: '234'
            }
        },
        response: {
            ...personResponse,
            isPartner: true
        }
    },
    personForms: {
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
                            text: 'Организация',
                            id: 'ORGANIZATION'
                        },
                        {
                            text: 'Физическое лицо',
                            id: 'PERSON'
                        },
                        {
                            text: 'Индивидуальный предприниматель',
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
                            text: 'Организация',
                            id: 'ORGANIZATION'
                        },
                        {
                            text: 'Индивидуальный предприниматель',
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
    personDetails: {
        request: ['/static/personforms/json/ph-partner.json', undefined, false, false, false],
        response: {
            'person-details': {
                type: 'ph',
                'is-partner': '0',
                caption: 'ID_Individual_ex',
                name: 'ID_Individual',
                'details-block': [
                    {
                        detail: [
                            {
                                id: 'lname',
                                caption: 'ID_Last_name',
                                type: 'text',
                                size: '128',
                                'required-mark': '',
                                'check-type': 'not_empty'
                            },
                            {
                                id: 'fname',
                                caption: 'ID_First_name',
                                type: 'text',
                                size: '128',
                                'required-mark': '',
                                'check-type': 'not_empty'
                            },
                            {
                                id: 'mname',
                                caption: 'ID_Middle_name',
                                type: 'text',
                                size: '128',
                                'required-mark': '',
                                'check-type': 'not_empty'
                            }
                        ]
                    },
                    {
                        title: '',
                        detail: [
                            {
                                id: 'phone',
                                caption: 'ID_Telephone',
                                type: 'text',
                                size: '32',
                                'required-mark': '',
                                'check-type': 'not_empty',
                                sample: '+7 495 123-45-67'
                            },
                            {
                                id: 'fax',
                                caption: 'ID_Fax',
                                type: 'text',
                                size: '32',
                                sample: '+7 495 321-54-76'
                            },
                            {
                                id: 'email',
                                caption: 'ID_Email',
                                type: 'text',
                                size: '256',
                                'required-mark': '',
                                'check-type': 'email'
                            }
                        ]
                    },
                    {
                        title: '',
                        detail: [
                            {
                                id: 'country-id',
                                caption: 'ID_Region_Code',
                                locked: 'flex',
                                type: 'select1',
                                'edit-only': '',
                                datasource: 'regions',
                                hint: 'ID_Region_Code_hint'
                            },
                            {
                                id: 'postcode',
                                caption: 'ID_Postcode',
                                type: 'text',
                                size: '32',
                                sample: '111033'
                            },
                            {
                                id: 'city',
                                caption: 'ID_City',
                                type: 'text',
                                size: '128',
                                sample: 'ID_Moscow'
                            },
                            {
                                id: 'postaddress',
                                caption: 'ID_Post_address',
                                type: 'text',
                                size: '256',
                                sample: 'ID_street_addr_hint2'
                            },
                            {
                                'admin-only': '',
                                id: 'invalid-address',
                                type: 'checkbox',
                                caption: 'ID_Invalid_address'
                            },
                            {
                                type: 'confirmation-checkbox',
                                id: 'agree',
                                'detail-text': 'ID_Personal_info_agreement',
                                'check-type': 'confirmed',
                                'required-mark': ''
                            }
                        ]
                    },
                    {
                        title: 'ID_Payment_details',
                        detail: [
                            {
                                'admin-only': '',
                                id: 'invalid-bankprops',
                                type: 'checkbox',
                                caption: 'ID_Invalid_bankprops'
                            },
                            {
                                id: 'bik',
                                caption: 'ID_Bank_ID',
                                type: 'text',
                                size: '9',
                                'check-type': 'check_bik',
                                sample: '047712345'
                            },
                            {
                                id: 'account',
                                caption: 'ID_Settlement_account',
                                type: 'text',
                                size: '20',
                                'check-type': 'check_account'
                            },
                            {
                                id: 'corraccount',
                                caption: 'ID_Correspondent_account',
                                type: 'text',
                                size: '32'
                            },
                            {
                                id: 'bank',
                                caption: 'ID_Bank_name',
                                type: 'text',
                                size: '128',
                                hint: 'ID_wo_quotation_marks_and_acronyms'
                            },
                            {
                                id: 'bankcity',
                                caption: 'ID_Bank_address',
                                type: 'text',
                                size: '128'
                            }
                        ]
                    },
                    {
                        detail: {
                            id: 'payment-purpose',
                            caption: 'ID_payment_purpose',
                            type: 'text',
                            size: '210',
                            'backoffice-only': ''
                        }
                    }
                ]
            }
        }
    },
    countries: {
        request: {
            url: `${HOST}/geo/regions`,
            data: {
                lang: 'ru',
                region_type: 'COUNTRIES'
            }
        },
        response: {
            ru: [
                { id: 29386, name: 'Абхазия' },
                { id: 211, name: 'Австралия' }
            ]
        }
    },
    setPerson: {
        request: {
            url: `${HOST}/person/set-person`,
            data: {
                client_id: 1354268005,
                is_partner: false,
                person_type: 'ph',
                mode: undefined,
                data: {
                    lname: 'Иванов',
                    fname: 'Иван',
                    mname: 'Иванович',
                    phone: '+7 905 1234567',
                    invalid_address: false,
                    invalid_bankprops: false,
                    payment_purpose: '',
                    fax: '',
                    email: 'balanceassessors@yandex.ru',
                    postcode: '',
                    city: '',
                    postaddress: '',
                    agree: false,
                    bik: '',
                    account: '',
                    corraccount: '',
                    bank: '',
                    bankcity: '',
                    country_id: ''
                },
                person_id: 17672240
            },
            isJSON: true
        },
        response: {
            kbk: null,
            tvaNumber: null,
            kbe: null,
            legalAddressDistrict: null,
            postcode: null,
            oktmo: null,
            corraccount: null,
            deliveryCity: null,
            bankData: null,
            kzIn: null,
            post2: null,
            post1: null,
            localAuthorityDocDetails: null,
            birthplaceRegion: null,
            hidden: false,
            legalAddressCode: null,
            fax: null,
            bik: null,
            legalAddressHome: null,
            pingpongWallet: null,
            dt: '2021-11-15T15:41:28',
            legalFiasGuid: null,
            bank: null,
            kpp: null,
            addressCity: null,
            name:
                '\u0418\u0432\u0430\u043d\u043e\u0432 \u0418\u0432\u0430\u043d \u0418\u0432\u0430\u043d\u043e\u0432\u0438\u0447',
            vip: 0,
            kladrCode: null,
            addressUpdated: null,
            postaddress: null,
            localSignerPersonName: null,
            vatPayer: null,
            localLegaladdress: null,
            addressCode: null,
            oebsExportable: true,
            ogrn: null,
            street: null,
            isBatchSupported: true,
            legalAddressGni: null,
            earlyDocs: 0,
            authorityDocDetails: null,
            countryId: null,
            addressDistrict: null,
            addressHome: null,
            rn: null,
            email: 'balanceassessors@yandex.ru',
            apiVersion: null,
            localRepresentative: null,
            personAccount: null,
            mname: '\u0418\u0432\u0430\u043d\u043e\u0432\u0438\u0447',
            bankcity: null,
            exportDt: null,
            legalAddressFlat: null,
            iban: null,
            uiEditable: true,
            address: null,
            swift: null,
            kzKbe: null,
            passportCode: null,
            region: null,
            localCity: null,
            paymentPurpose: null,
            bankType: 0,
            birthday: null,
            passportBirthplace: null,
            localName: null,
            addressConstruction: null,
            passportId: '739250701',
            passportE: null,
            passportD: null,
            hasEdo: false,
            addressFlat: null,
            passportN: null,
            localSignerPositionName: null,
            legalAddressSuffix: null,
            passportS: null,
            addressPostcode: null,
            addressStreet: null,
            uiDeletable: true,
            city: null,
            paypalWallet: null,
            reviseActPeriodType: null,
            operatorUid: null,
            benBank: null,
            authorityDocType: null,
            fiasGuid: null,
            isPartner: false,
            type: 'ph',
            jpc: null,
            webmoneyWallet: null,
            sensibleName:
                '\u0418\u0432\u0430\u043d\u043e\u0432 \u0418\u0432\u0430\u043d \u0418\u0432\u0430\u043d\u043e\u0432\u0438\u0447',
            innDocDetails: null,
            bankInn: null,
            memo: null,
            legalAddressStreet: null,
            phone: '+7 905 1234567',
            fpsBank: null,
            pfr: null,
            representative: null,
            clientId: 1354268005,
            invalidBankprops: false,
            legalAddressConstruction: null,
            fpsPhone: null,
            iik: null,
            needDealPassport: false,
            corrSwift: null,
            account: null,
            invoiceCount: 1,
            shortSignerPersonName: null,
            signerPersonGender: null,
            legaladdress: null,
            usState: null,
            addressBuilding: null,
            invalidAddress: false,
            legalAddressBuilding: null,
            attrMetaIgnored: [],
            legalAddressCity: null,
            isNew: false,
            benBankCode: null,
            inn: null,
            ilId: null,
            localPostaddress: null,
            deliveryType: 0,
            id: 17672240,
            rnn: null,
            legalAddressRegion: null,
            ownershipType: null,
            taxType: null,
            lname: '\u0418\u0432\u0430\u043d\u043e\u0432',
            addressRegion: null,
            attrDirectAccess: ['_sa_instance_state', '_state', '_do_not_export'],
            other: null,
            fname: '\u0418\u0432\u0430\u043d',
            yamoneyWallet: null,
            localOther: null,
            isUr: false,
            liveSignature: 0,
            signerPositionName: null,
            benAccount: null,
            addressGni: null,
            addressTown: null,
            legalAddressPostcode: null,
            birthplaceCountry: null,
            longname: null,
            localBank: null,
            signerPersonName: null,
            ownershipTypeUi: 'PERSON',
            localLongname: null,
            birthplaceDistrict: null,
            verifiedDocs: 0,
            legalSample: null,
            localBenBank: null,
            payoneerWallet: null,
            legalAddressTown: null,
            trueKz: 0,
            organization: null,
            envelopeAddress: '',
            autoGen: 0,
            postsuffix: null
        }
    }
};
