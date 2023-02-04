import { HOST } from 'common/utils/test-utils/common';

export const perms = ['NewUIEarlyAdopter', 'AdminAccess', 'CreateBankPayments'];
export const mocks = {
    invoice: {
        request: [`${HOST}/invoice`, { invoice_id: '123' }, false, false],
        response: {
            version: { 'yb-snout-api': '3.trunk.7820184' },
            data: {
                person: {
                    kbk: null,
                    'bank-type': '0',
                    'local-bank': null,
                    'corr-swift': null,
                    'payment-purpose': null,
                    'ui-deletable': '1',
                    'legal-address-suffix': null,
                    kbe: null,
                    'invalid-address': '0',
                    'local-longname': null,
                    postcode: '191025',
                    oktmo: null,
                    'address-gni': null,
                    'live-signature': '1',
                    'bank-data': {
                        info: null,
                        city: '\u041c\u043e\u0441\u043a\u0432\u0430',
                        'bank-city': '\u041c\u043e\u0441\u043a\u0432\u0430',
                        bik: '044525440',
                        name:
                            '\u041e\u041e\u041e \u041a\u0411 "\u042d\u041a\u041e-\u0418\u041d\u0412\u0415\u0421\u0422"',
                        swift: null,
                        id: '2992',
                        'bank-address': null,
                        'cor-acc': '30101810145250000440',
                        hidden: '0',
                        'corr-account': '30101810145250000440',
                        'update-dt': '2020-12-13T04:00:43',
                        'bank-name':
                            '\u041e\u041e\u041e \u041a\u0411 "\u042d\u041a\u041e-\u0418\u041d\u0412\u0415\u0421\u0422"'
                    },
                    'birthplace-country': null,
                    'local-other': null,
                    'birthplace-district': null,
                    post2: '',
                    post1: '',
                    'true-kz': '0',
                    'need-deal-passport': '0',
                    bank: null,
                    'address-street': null,
                    hidden: '0',
                    'us-state': null,
                    fax: '+7 812 5696286',
                    rnn: null,
                    'ben-account': null,
                    'address-building': null,
                    'legal-address-home': null,
                    'short-signer-person-name': 'Signer R.',
                    'legal-address-code': null,
                    'signer-person-gender': 'W',
                    dt: '2021-01-19T04:00:58',
                    'country-id': '225',
                    'address-code': null,
                    kpp: '767726208',
                    name:
                        '\u042e\u0440. \u043b\u0438\u0446\u043e \u0438\u043b\u0438 \u041f\u0411\u041e\u042e\u041bqdZd \u0420\u0410\u041e \u00ab\u0418\u0432\u0430\u043d\u043e\u0432\u00bb',
                    'sensible-name': '000 WBXG',
                    'delivery-type': '4',
                    'revise-act-period-type': null,
                    'legal-address-building': null,
                    'legal-address-region': null,
                    fname: null,
                    'kladr-code': null,
                    postaddress: null,
                    'inn-doc-details': null,
                    ogrn: '379956466494603',
                    corraccount: null,
                    'il-id': null,
                    street: null,
                    'local-signer-position-name': null,
                    'vat-payer': null,
                    'address-postcode': null,
                    'passport-code': null,
                    'authority-doc-details': 'g',
                    'ben-bank': null,
                    email: 'm-SC@qCWF.rKU',
                    'signer-person-name': 'Signer RR',
                    'webmoney-wallet': null,
                    'legal-address-flat': null,
                    'passport-birthplace': null,
                    mname: null,
                    bankcity: null,
                    'address-town': null,
                    iban: null,
                    'local-signer-person-name': null,
                    address: '\u0423\u043b\u0438\u0446\u0430 4',
                    swift: null,
                    'ben-bank-code': null,
                    'yamoney-wallet': null,
                    fias: '<balance.mapper.fias.Fias object at 0x7f36687f8550>',
                    region: null,
                    'address-district': null,
                    'kz-kbe': null,
                    'invalid-bankprops': '0',
                    'auto-gen': '0',
                    vip: '1',
                    'pingpong-wallet': null,
                    'legal-sample': 'Avenue 5',
                    'local-name': null,
                    'local-ben-bank': null,
                    'is-partner': '0',
                    'kz-in': null,
                    city:
                        '\u0433 \u0421\u0430\u043d\u043a\u0442-\u041f\u0435\u0442\u0435\u0440\u0431\u0443\u0440\u0433',
                    'ownership-type-ui': 'ORGANIZATION',
                    'ownership-type': null,
                    'passport-id': '16571028',
                    'birthplace-region': null,
                    'address-construction': null,
                    'address-home': null,
                    'payoneer-wallet': null,
                    type: 'ur',
                    'legal-address-district': null,
                    'legal-address-town': null,
                    'early-docs': '0',
                    'paypal-wallet': null,
                    'local-legaladdress': null,
                    memo: null,
                    'oebs-exportable': '1',
                    'client-id': '188189111',
                    phone: '+7 812 3990776',
                    'person-account': null,
                    representative: 'tPLLK',
                    iik: null,
                    'passport-n': null,
                    account: '40702810947490078251',
                    'passport-e': null,
                    'passport-d': null,
                    'bank-inn': null,
                    'tax-type': null,
                    'address-city': null,
                    'api-version': '0',
                    legaladdress: 'Avenue 5',
                    'passport-s': null,
                    'legal-address-gni': null,
                    'is-ur': '1',
                    jpc: null,
                    'delivery-city': 'NSK',
                    inn: '7861287465',
                    'signer-position-name': 'President',
                    'legal-fias-guid': null,
                    'local-representative': null,
                    'address-region': null,
                    id: '13804645',
                    bik: '044525440',
                    'ui-editable': '1',
                    'address-flat': null,
                    'legal-address-postcode': '666666',
                    lname: null,
                    'authority-doc-type':
                        '\u0421\u0432\u0438\u0434\u0435\u0442\u0435\u043b\u044c\u0441\u0442\u0432\u043e \u043e \u0440\u0435\u0433\u0438\u0441\u0442\u0440\u0430\u0446\u0438\u0438',
                    other: null,
                    'invoice-count': '3',
                    'legal-address-street': null,
                    'local-postaddress': null,
                    birthday: null,
                    'local-authority-doc-details': null,
                    longname: '000 WBXG',
                    'verified-docs': '0',
                    'export-dt': null,
                    rn: null,
                    'envelope-address':
                        '\u0430/\u044f \u041a\u043b\u0430\u0434\u0440 0\n\u0433 \u0421\u0430\u043d\u043a\u0442-\u041f\u0435\u0442\u0435\u0440\u0431\u0443\u0440\u0433',
                    'address-updated': null,
                    'fias-guid': 'c2deb16a-0330-4f05-821f-1d09c93331e6',
                    pfr: null,
                    organization: null,
                    'legal-address-construction': null,
                    'local-city': null,
                    'legal-address-city': null,
                    postsuffix: '\u0430/\u044f \u041a\u043b\u0430\u0434\u0440 0'
                },
                'consume-amount': '0.00',
                'upd-contract-dt': null,
                'pay-extensions': [],
                'bank-details': {
                    bankaccount: '30101810300000000545',
                    account: '40702810600014307627',
                    'bank-id': '2007',
                    weight: '0',
                    corrbank: null,
                    'needs-alert': '0',
                    corrbankcode: null,
                    prefer: '3',
                    'iso-currency': 'RUB',
                    'name-suffix': null,
                    'firm-id': '1',
                    currency: 'RUR',
                    corriban: null,
                    corrbin: null,
                    bankcode: '044525545',
                    'oebs-code': '\u042e\u043d\u0438\u043a\u0440\u0435\u0434\u0438\u0442',
                    bankaddress:
                        ', 119034, \u0433. \u041c\u043e\u0441\u043a\u0432\u0430, \u041f\u0440\u0435\u0447\u0438\u0441\u0442\u0435\u043d\u0441\u043a\u0430\u044f \u043d\u0430\u0431., \u0434. 9',
                    id: '61',
                    bank:
                        '\u0410\u041e \u042e\u043d\u0438\u043a\u0440\u0435\u0434\u0438\u0442 \u0411\u0430\u043d\u043a'
                },
                'full-render': '0',
                'unused-funds-possible-transfers': [],
                'payment-purpose': '\u0411-3561148159-1',
                'can-be-closed': '0',
                'consume-sum': '0',
                'is-stale': '0',
                'status-id': '0',
                'agency-discount-pct': '0',
                'fast-payment': '0',
                'wmr-payment': {},
                'is-docs-detailed': '0',
                'unused-funds-in-invoice-currency': '0',
                'repaid-by': {},
                'is-docs-separated': '0',
                id: '123',
                endbuyer: null,
                'invoice-orders': [
                    {
                        'amount-nds': '0',
                        'product-fullname':
                            '\u041c\u0438\u043d\u0438\u043c\u0430\u043b\u044c\u043d\u043e\u0435 \u0432\u043e\u0437\u043d\u0430\u0433\u0440\u0430\u0436\u0434\u0435\u043d\u0438\u0435 \u0437\u0430 \u043f\u0440\u0435\u0434\u043e\u0441\u0442\u0430\u0432\u043b\u0435\u043d\u0438\u0435 \u043f\u0440\u0430\u0432\u0430 \u0438\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u043d\u0438\u044f \u0411\u0430\u0437\u044b \u0434\u0430\u043d\u043d\u044b\u0445 \u042f\u043d\u0434\u0435\u043a\u0441.\u041a\u0430\u0440\u0442\u044b \u043f\u043e\u0441\u0440\u0435\u0434\u0441\u0442\u0432\u043e\u043c JS API \u0437\u0430 \u0432\u0435\u0441\u044c \u0441\u0440\u043e\u043a \u0434\u0435\u0439\u0441\u0442\u0432\u0438\u044f \u043b\u0438\u0446\u0435\u043d\u0437\u0438\u0438',
                        seqnum: null,
                        price: '1',
                        'discount-pct': '0',
                        precision: '6',
                        'type-rate': '1',
                        'product-type-rate': '1',
                        amount: '850000',
                        client: {
                            id: '188189111',
                            name: 'balance_test 2021-01-19 04:00:48.529666'
                        },
                        'amount-no-discount': '850000',
                        'amount-nsp': '0',
                        text:
                            '\u041c\u0438\u043d\u0438\u043c\u0430\u043b\u044c\u043d\u043e\u0435 \u0432\u043e\u0437\u043d\u0430\u0433\u0440\u0430\u0436\u0434\u0435\u043d\u0438\u0435 \u0437\u0430 \u043f\u0440\u0435\u0434\u043e\u0441\u0442\u0430\u0432\u043b\u0435\u043d\u0438\u0435 \u043f\u0440\u0430\u0432\u0430 \u0438\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u043d\u0438\u044f \u0411\u0430\u0437\u044b \u0434\u0430\u043d\u043d\u044b\u0445 \u042f\u043d\u0434\u0435\u043a\u0441.\u041a\u0430\u0440\u0442\u044b \u043f\u043e\u0441\u0440\u0435\u0434\u0441\u0442\u0432\u043e\u043c JS API \u0437\u0430 \u0432\u0435\u0441\u044c \u0441\u0440\u043e\u043a \u0434\u0435\u0439\u0441\u0442\u0432\u0438\u044f \u043b\u0438\u0446\u0435\u043d\u0437\u0438\u0438',
                        'product-type-cc': 'money',
                        'effective-sum': '850000',
                        nds: null,
                        'unit-name': '\u0434\u0435\u043d\u044c\u0433\u0438',
                        'price-wo-nds': '1',
                        order: {
                            'agency-id': null,
                            'service-order-id': '277142',
                            id: '3678860929',
                            service: { cc: 'apikeys', id: '129', name: '129' },
                            'service-id': '129'
                        },
                        unit: null,
                        quantity: '850000'
                    },
                    {
                        'amount-nds': '0.0000000000',
                        amount: '850000.0000000000',
                        'effective-sum': '850000.0000000000',
                        'amount-no-discount': '850000',
                        'amount-nsp': '0.0000000000'
                    }
                ],
                postpay: '0',
                crossfirm: '0',
                'contractless-offer-type': '38',
                'payment-term-dt': null,
                'offer-type': '0',
                overdraft: '0',
                'dt-written-out': '3 \u0444\u0435\u0432\u0440\u0430\u043b\u044f 2021 \u0433.',
                'paysys-id': '1003',
                'is-trp': '0',
                'market-postpay': '0',
                suspect: '1',
                'matching-contracts': [{ 'external-id': '1660693/21', id: '2857872' }],
                'client-discount-proofs': [],
                'external-id': '\u0411-3561148159-1',
                type: 'prepayment',
                totals: {
                    'amount-nds': '0.0000000000',
                    amount: '850000.0000000000',
                    'effective-sum': '850000.0000000000',
                    'amount-no-discount': '850000',
                    'amount-nsp': '0.0000000000'
                },
                firm: { 'is-bu-mode': null, id: '1' },
                'total-sum': '850000',
                'oebs-exportable': '1',
                'effective-sum': '850000',
                'ym-ident': {},
                'unused-funds-lock': '0',
                'service-code': null,
                'amount-to-pay': '1850000.00',
                'available-invoice-transfer-sum': '850000.00',
                'is-alterable': {
                    date: '0',
                    pcp: '1',
                    contract: '1',
                    'pcp-by-client': '1',
                    sum: '0'
                },
                'export-dt': null,
                'internal-rate': '1',
                payable: '1',
                'can-correct-receipt': '0',
                paysys: {
                    'invoice-sendable': '1',
                    instant: '0',
                    name:
                        '\u0411\u0430\u043d\u043a \u0434\u043b\u044f \u044e\u0440\u0438\u0434\u0438\u0447\u0435\u0441\u043a\u0438\u0445 \u043b\u0438\u0446',
                    certificate: '0',
                    cc: 'ur',
                    'payment-method': { cc: 'bank', name: 'Bank Payment' },
                    currency: 'RUR',
                    id: '1003',
                    'person-category': { category: 'ur', 'region-id': '225', ur: '1' }
                },
                'total-act-sum-1c': null,
                'fictive-invoices': null,
                dt: '2021-02-03T00:00:00',
                'sum-written-out':
                    '\u0412\u043e\u0441\u0435\u043c\u044c\u0441\u043e\u0442 \u043f\u044f\u0442\u044c\u0434\u0435\u0441\u044f\u0442 \u0442\u044b\u0441\u044f\u0447 \u0440\u0443\u0431\u043b\u0435\u0439 00 \u043a\u043e\u043f\u0435\u0435\u043a',
                'can-manual-turn-on': '1',
                'has-paysys-alert': '0',
                'direct-payment': '0',
                'payment-suspend-dt': null,
                'total-act-sum': '0',
                hidden: '0',
                'qiwi-payment': {},
                'register-rows': [],
                currency: 'RUR',
                'request-id': '3561148159',
                'can-withdraw': '0',
                'agency-discount-proof': null,
                request: { 'is-unmoderated': '0', 'overdraft-service': null, id: '3561148159' },
                manager: null,
                contract: {
                    'link-contract-id': null,
                    commission: '0',
                    dt: '2021-02-03T00:00:00',
                    'external-id': '1660693/21',
                    id: '2857872'
                },
                credit: '0',
                chargenote: '0',
                client: {
                    name: 'balance_test 2021-01-19 04:00:48.529666',
                    'manual-suspect': '0',
                    'is-agency': '0',
                    suspect: '0',
                    'sms-notify': '2',
                    id: '188189111'
                },
                'currency-rate': '1',
                'can-withdraw-partial': '1',
                'receipt-sum': '0',
                'amount-extra': '0',
                nds: '1',
                'money-product': '1',
                'nds-pct': '0',
                'used-funds-in-invoice-currency': '0',
                'receipt-sum-1c': '0'
            }
        }
    },
    personforms: {
        request: ['/static/personforms/personforms.json', undefined, false, false, false],
        response: {
            'person-forms': {
                'is-postbox-types': {
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
    edoTypes: {
        request: [`${HOST}/edo/types`, undefined, true, false],
        response: {
            version: { 'yb-snout-api': '3.trunk.7820184' },
            data: [
                { text: 'ID_not_selected', id: null },
                { text: 'ID_EDO_1', id: '1' },
                { text: 'ID_EDO_2', id: '2' }
            ]
        }
    },
    serviceCodeList: {
        request: [`${HOST}/product/service-code/list`, undefined, false, false],
        response: {
            version: { 'yb-snout-api': '3.trunk.7820184' },
            data: [
                {
                    code: 'APIKEYS_MAPS_DISTANCE MATRIX&ROUTER_API_DOLLAR',
                    description:
                        '\u041c\u0430\u0442\u0440\u0438\u0446\u0430 \u0440\u0430\u0441\u0441\u0442\u043e\u044f\u043d\u0438\u0439 \u0438 \u043f\u043e\u0441\u0442\u0440\u043e\u0435\u043d\u0438\u0435 \u043c\u0430\u0440\u0448\u0440\u0443\u0442\u0430, APIKEYS \u0432 \u0434\u043e\u043b\u043b\u0430\u0440\u0430\u0445'
                },
                {
                    code: 'APIKEYS_MAPS_DISTANCE MATRIX&ROUTER_API_EURO',
                    description:
                        '\u041c\u0430\u0442\u0440\u0438\u0446\u0430 \u0440\u0430\u0441\u0441\u0442\u043e\u044f\u043d\u0438\u0439 \u0438 \u043f\u043e\u0441\u0442\u0440\u043e\u0435\u043d\u0438\u0435 \u043c\u0430\u0440\u0448\u0440\u0443\u0442\u0430, APIKEYS \u0432 \u0435\u0432\u0440\u043e'
                },
                {
                    code: 'APIKEYS_VRP_VEHICLES_MONITORING_VEHICLES',
                    description:
                        '\u041a\u0430\u0431\u0438\u043d\u0435\u0442 \u0440\u0430\u0437\u0440\u0430\u0431\u043e\u0442\u0447\u0438\u043a\u0430 (642), API \u041f\u043b\u0430\u043d\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u044f \u0438 \u041c\u043e\u043d\u0438\u0442\u043e\u0440\u0438\u043d\u0433\u0430'
                },
                {
                    code: 'APIKEYS_VRP_VEHICLES',
                    description:
                        '\u041a\u0430\u0431\u0438\u043d\u0435\u0442 \u0440\u0430\u0437\u0440\u0430\u0431\u043e\u0442\u0447\u0438\u043a\u0430 (642), API \u041f\u043b\u0430\u043d\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u044f, \u043f\u0435\u0440\u0435\u0440\u0430\u0441\u0445\u043e\u0434'
                },
                {
                    code: 'APIKEYS_MONITORING_VEHICLES',
                    description:
                        '\u041a\u0430\u0431\u0438\u043d\u0435\u0442 \u0440\u0430\u0437\u0440\u0430\u0431\u043e\u0442\u0447\u0438\u043a\u0430 (642), API \u041c\u043e\u043d\u0438\u0442\u043e\u0440\u0438\u043d\u0433\u0430, \u043f\u0435\u0440\u0435\u0440\u0430\u0441\u0445\u043e\u0434'
                },
                {
                    code: 'APIKEYS_MAPS',
                    description:
                        '\u041a\u0430\u0431\u0438\u043d\u0435\u0442 \u0440\u0430\u0437\u0440\u0430\u0431\u043e\u0442\u0447\u0438\u043a\u0430, API \u042f\u043d\u0434\u0435\u043a\u0441.\u041a\u0430\u0440\u0442'
                },
                {
                    code: 'APIKEYS_AGENT_MAPS_PLACES',
                    description:
                        '\u041a\u0430\u0431\u0438\u043d\u0435\u0442 \u0440\u0430\u0437\u0440\u0430\u0431\u043e\u0442\u0447\u0438\u043a\u0430 (659), API \u041f\u043e\u0438\u0441\u043a \u043f\u043e \u043e\u0440\u0433\u0430\u043d\u0438\u0437\u0430\u0446\u0438\u044f\u043c'
                },
                {
                    code: 'APIKEYS_AGENT_MAPS_MAPKIT',
                    description:
                        '\u041a\u0430\u0431\u0438\u043d\u0435\u0442 \u0440\u0430\u0437\u0440\u0430\u0431\u043e\u0442\u0447\u0438\u043a\u0430 (659), MapKit Mobile SDK'
                },
                {
                    code: 'DEPOSITION',
                    description:
                        '\u041f\u043e\u043f\u043e\u043b\u043d\u0435\u043d\u0438\u0435 \u0434\u0435\u043f\u043e\u0437\u0438\u0442\u043d\u043e\u0433\u043e \u043b\u0438\u0446\u0435\u0432\u043e\u0433\u043e \u0441\u0447\u0451\u0442\u0430'
                },
                {
                    code: 'APIKEYS_AGENT_MAPS_DISTANCE MATRIX & ROUTER API',
                    description:
                        '\u041a\u0430\u0431\u0438\u043d\u0435\u0442 \u0440\u0430\u0437\u0440\u0430\u0431\u043e\u0442\u0447\u0438\u043a\u0430 (659), API \u042f\u043d\u0434\u0435\u043a\u0441.\u041c\u0430\u0440\u0448\u0440\u0443\u0442\u0438\u0437\u0430\u0446\u0438\u044f: \u041c\u0430\u0442\u0440\u0438\u0446\u0430 \u0440\u0430\u0441\u0441\u0442\u043e\u044f\u043d\u0438\u0439'
                },
                {
                    code: 'APIKEYS_MAPS_PLACES',
                    description:
                        '\u041a\u0430\u0431\u0438\u043d\u0435\u0442 \u0440\u0430\u0437\u0440\u0430\u0431\u043e\u0442\u0447\u0438\u043a\u0430, API \u041f\u043e\u0438\u0441\u043a \u043f\u043e \u043e\u0440\u0433\u0430\u043d\u0438\u0437\u0430\u0446\u0438\u044f\u043c'
                },
                {
                    code: 'APIKEYS_MAPS_STATIK',
                    description:
                        '\u041a\u0430\u0431\u0438\u043d\u0435\u0442 \u0440\u0430\u0437\u0440\u0430\u0431\u043e\u0442\u0447\u0438\u043a\u0430, Static API \u042f\u043d\u0434\u0435\u043a\u0441.\u041a\u0430\u0440\u0442'
                },
                {
                    code: 'APIKEYS_MAPS_MAPKIT',
                    description:
                        '\u041a\u0430\u0431\u0438\u043d\u0435\u0442 \u0440\u0430\u0437\u0440\u0430\u0431\u043e\u0442\u0447\u0438\u043a\u0430, MapKit - \u043c\u043e\u0431\u0438\u043b\u044c\u043d\u044b\u0439 SDK'
                },
                {
                    code: 'APIKEYS_MAPS_DISTANCE MATRIX & ROUTER API',
                    description:
                        '\u041a\u0430\u0431\u0438\u043d\u0435\u0442 \u0440\u0430\u0437\u0440\u0430\u0431\u043e\u0442\u0447\u0438\u043a\u0430, \u041c\u0430\u0442\u0440\u0438\u0446\u0430 \u0420\u0430\u0441\u0441\u0442\u043e\u044f\u043d\u0438\u0439 \u0438 \u041f\u043e\u0441\u0442\u0440\u043e\u0435\u043d\u0438\u0435 \u041c\u0430\u0440\u0448\u0440\u0443\u0442\u0430'
                },
                {
                    code: 'APIKEYS_SPEECHKIT_CLOUD',
                    description:
                        '\u041a\u0430\u0431\u0438\u043d\u0435\u0442 \u0440\u0430\u0437\u0440\u0430\u0431\u043e\u0442\u0447\u0438\u043a\u0430, API SpeechKit Cloud'
                },
                {
                    code: 'APIKEYS_AGENT_MAPS',
                    description:
                        '\u041a\u0430\u0431\u0438\u043d\u0435\u0442 \u0440\u0430\u0437\u0440\u0430\u0431\u043e\u0442\u0447\u0438\u043a\u0430 (659), JavaScript API \u0438 HTTP \u0413\u0435\u043e\u043a\u043e\u0434\u0435\u0440'
                },
                {
                    code: 'APIKEYS_AGENT_MAPS_STATIC',
                    description:
                        '\u041a\u0430\u0431\u0438\u043d\u0435\u0442 \u0440\u0430\u0437\u0440\u0430\u0431\u043e\u0442\u0447\u0438\u043a\u0430 (659), Static API \u042f\u043d\u0434\u0435\u043a\u0441.\u041a\u0430\u0440\u0442'
                },
                { code: 'PENALTY', description: '\u0428\u0442\u0440\u0430\u0444' },
                {
                    code: 'APIKEYS_MARKET',
                    description:
                        '\u0410\u041f\u0418 \u041c\u0430\u0440\u043a\u0435\u0442\u0430 \u0432 \u041a\u0430\u0431\u0438\u043d\u0435\u0442\u0435 \u0440\u0430\u0437\u0440\u0430\u0431\u043e\u0442\u0447\u0438\u043a\u0430'
                },
                {
                    code: 'AGENT_REWARD',
                    description:
                        '\u0410\u0433\u0435\u043d\u0442\u0441\u043a\u043e\u0435 \u0432\u043e\u0437\u043d\u0430\u0433\u0440\u0430\u0436\u0434\u0435\u043d\u0438\u0435'
                },
                {
                    code: 'YANDEX_SERVICE',
                    description:
                        '\u041e\u0441\u043d\u043e\u0432\u043d\u0430\u044f \u0443\u0441\u043b\u0443\u0433\u0430 \u042f\u043d\u0434\u0435\u043a\u0441\u0430'
                },
                {
                    code: 'YANDEX_SERVICE_WO_VAT',
                    description:
                        '\u0423\u0441\u043b\u0443\u0433\u0430 \u042f\u043d\u0434\u0435\u043a\u0441\u0430, \u043d\u0435 \u043e\u0431\u043b\u0430\u0433\u0430\u0435\u043c\u0430\u044f \u043d\u0430\u043b\u043e\u0433\u043e\u043c'
                },
                {
                    code: 'APIKEYS_MAPS_DOLLAR',
                    description:
                        'JavaScript API \u0438 HTTP \u0413\u0435\u043e\u043a\u043e\u0434\u0435\u0440, APIKEYS \u0432 \u0434\u043e\u043b\u043b\u0430\u0440\u0430\u0445'
                },
                {
                    code: 'APIKEYS_MAPS_EURO',
                    description:
                        'JavaScript API \u0438 HTTP \u0413\u0435\u043e\u043a\u043e\u0434\u0435\u0440, APIKEYS \u0432 \u0435\u0432\u0440\u043e'
                },
                {
                    code: 'APIKEYS_MAPS_STATIC_DOLLAR',
                    description:
                        'Static API, APIKEYS \u0432 \u0434\u043e\u043b\u043b\u0430\u0440\u0430\u0445'
                },
                {
                    code: 'APIKEYS_MAPS_STATIC_EURO',
                    description: 'Static API, APIKEYS \u0432 \u0435\u0432\u0440\u043e'
                },
                {
                    code: 'APIKEYS_MAPS_PLACES_DOLLAR',
                    description:
                        '\u041f\u043e\u0438\u0441\u043a \u043f\u043e \u043e\u0440\u0433\u0430\u043d\u0438\u0437\u0430\u0446\u0438\u044f\u043c, APIKEYS \u0432 \u0434\u043e\u043b\u043b\u0430\u0440\u0430\u0445'
                },
                {
                    code: 'APIKEYS_MAPS_PLACES_EURO',
                    description:
                        '\u041f\u043e\u0438\u0441\u043a \u043f\u043e \u043e\u0440\u0433\u0430\u043d\u0438\u0437\u0430\u0446\u0438\u044f\u043c, APIKEYS \u0432 \u0435\u0432\u0440\u043e'
                },
                {
                    code: 'APIKEYS_MAPS_DISTANCE MATRIX',
                    description:
                        '\u041c\u0430\u0442\u0440\u0438\u0446\u0430 \u0440\u0430\u0441\u0441\u0442\u043e\u044f\u043d\u0438\u0439 \u0438 \u043f\u043e\u0441\u0442\u0440\u043e\u0435\u043d\u0438\u0435 \u043c\u0430\u0440\u0448\u0440\u0443\u0442\u0430, APIKEYS \u0432 \u0434\u043e\u043b\u043b\u0430\u0440\u0430\u0445'
                },
                {
                    code: 'APIKEYS_MAPS_MAPKIT_EURO',
                    description: 'MapKit, APIKEYS \u0432 \u0435\u0432\u0440\u043e'
                }
            ]
        }
    },
    objectPermissions: {
        request: [
            `${HOST}/user/object-permissions`,
            { classname: 'Invoice', object_id: '123' },
            false,
            false
        ],
        response: {
            version: { 'yb-snout-api': '3.trunk.7820184' },
            data: [
                {
                    code: 'ViewDevData',
                    id: 11261,
                    name:
                        '\u0414\u043e\u0441\u0442\u0443\u043f \u043a \u0442\u0435\u0445\u043d\u0438\u0447\u0435\u0441\u043a\u043e\u0439 \u0438\u043d\u0444\u043e\u0440\u043c\u0430\u0446\u0438\u0438'
                },
                {
                    code: 'IssueYTInvoices',
                    id: 14,
                    name:
                        '\u0412\u044b\u0441\u0442\u0430\u0432\u043b\u044f\u0442\u044c \u0441\u0447\u0435\u0442\u0430 \u043d\u0430 Yandex Technologies'
                },
                {
                    code: 'order_info__order_id',
                    id: 1012,
                    name:
                        '\u0421\u0432\u043e\u0439\u0441\u0442\u0432\u0430 \u0437\u0430\u043a\u0430\u0437\u0430 - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u0412\u043d\u0443\u0442\u0440\u0435\u043d\u043d\u0438\u0439 ID \u0437\u0430\u043a\u0430\u0437\u0430'
                },
                {
                    code: 'AlterAllInvoices',
                    id: 10,
                    name:
                        '[\u041d\u0435 \u0438\u0441\u043f\u043e\u043b\u044c\u0437\u0443\u0435\u0442\u0441\u044f]\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0441\u0443\u043c\u043c\u044b \u0441\u0447\u0435\u0442\u0430'
                },
                {
                    code: 'RepresentClient',
                    id: 2000,
                    name:
                        '\u0414\u0435\u0439\u0441\u0442\u0432\u0438\u044f \u043e\u0442 \u0438\u043c\u0435\u043d\u0438 \u043a\u043b\u0438\u0435\u043d\u0442\u0430'
                },
                {
                    code: 'ViewPersons',
                    id: 11322,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u043e\u0432'
                },
                {
                    code: 'SendActs',
                    id: 11323,
                    name:
                        '\u041e\u0442\u043f\u0440\u0430\u0432\u0438\u0442\u044c \u0430\u043a\u0442\u044b \u043f\u043e \u044d\u043b\u0435\u043a\u0442\u0440\u043e\u043d\u043d\u043e\u0439 \u0438\u043b\u0438 \u043e\u0431\u044b\u043d\u043e\u0439 \u043f\u043e\u0447\u0442\u0435'
                },
                {
                    code: 'ChangeUnusedFundsLock',
                    id: 11227,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0438\u0442\u044c \u043f\u0440\u0438\u0447\u0438\u043d\u0443 \u0441\u043d\u044f\u0442\u0438\u044f \u0431\u0435\u0437\u0437\u0430\u043a\u0430\u0437\u044c\u044f \u0443 \u0441\u0447\u0451\u0442\u0430'
                },
                {
                    code: 'PersonEdit',
                    id: 26,
                    name:
                        '\u0420\u0435\u0434\u0430\u043a\u0442\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0435 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u0430'
                },
                {
                    code: 'SetMcbFlags',
                    id: 11324,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0438\u0442\u044c \u043a\u0430\u0442\u0435\u0433\u043e\u0440\u0438\u0438 \u0434\u043b\u044f \u043f\u043b\u043e\u0449\u0430\u0434\u043e\u043a \u043a\u043b\u0438\u0435\u043d\u0442\u043e\u0432'
                },
                {
                    code: 'AlterInvoicePerson',
                    id: 11110,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u0430 \u0441\u0447\u0435\u0442\u0430'
                },
                {
                    code: 'ManageRBSPayments',
                    id: 19,
                    name:
                        '\u041e\u043f\u0435\u0440\u0430\u0446\u0438\u0438 \u0441 \u043f\u043b\u0430\u0442\u0435\u0436\u0430\u043c\u0438 \u043f\u043e RBS'
                },
                {
                    code: 'SuperVisor',
                    id: 15,
                    name:
                        '\u0423\u0441\u0442\u0430\u0440\u0435\u0432\u0448\u0435\u0435, \u043d\u0435 \u0438\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u0442\u044c. SuperWisard'
                },
                {
                    code: 'AdminAccess',
                    id: 0,
                    name:
                        '\u0414\u043e\u0441\u0442\u0443\u043f \u043a \u0430\u0434\u043c\u0438\u043d\u0438\u0441\u0442\u0440\u0430\u0442\u0438\u0432\u043d\u043e\u043c\u0443 \u0440\u0435\u0436\u0438\u043c\u0443'
                },
                {
                    code: 'CreateBankPayments',
                    id: 3,
                    name:
                        '\u0412\u043d\u0435\u0441\u0435\u043d\u0438\u0435 \u043f\u043b\u0430\u0442\u0435\u0436\u0435\u0439 \u0447\u0435\u0440\u0435\u0437 \u0431\u0430\u043d\u043a'
                },
                {
                    code: 'PersonView',
                    id: 25,
                    name:
                        '\u0423\u0441\u0442\u0430\u0440\u0435\u0432\u0448\u0435\u0435, \u043d\u0435 \u0438\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u0442\u044c. \u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u0430.'
                },
                {
                    code: 'CreateRequestsShop',
                    id: 11101,
                    name:
                        '\u0412\u044b\u0441\u0442\u0430\u0432\u043b\u0435\u043d\u0438\u0435 \u0441\u0447\u0435\u0442\u043e\u0432 \u0447\u0435\u0440\u0435\u0437 \u043c\u0430\u0433\u0430\u0437\u0438\u043d'
                },
                {
                    code: 'AlterFirmUA',
                    id: 32,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b \u041e\u041e\u041e \u00ab\u042f\u043d\u0434\u0435\u043a\u0441.\u0423\u043a\u0440\u0430\u0438\u043d\u0430\u00bb'
                },
                {
                    code: 'AssistPostauth',
                    id: 11,
                    name:
                        '\u0423\u043f\u0440\u0430\u0432\u043b\u0435\u043d\u0438\u0435 \u043f\u043e\u0441\u0442\u0430\u0432\u0442\u043e\u0440\u0438\u0437\u0430\u0446\u0438\u0435\u0439'
                },
                {
                    code: 'ViewProduct',
                    id: 11241,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u043f\u0440\u043e\u0434\u0443\u043a\u0442\u043e\u0432'
                },
                {
                    code: 'EditClient',
                    id: 28,
                    name:
                        '\u0420\u0435\u0434\u0430\u043a\u0442\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0435 \u043a\u043b\u0438\u0435\u043d\u0442\u0430'
                },
                { code: 'Reversal', id: 9, name: 'Reversal' },
                {
                    code: 'DealPassport',
                    id: 30,
                    name: '\u0420\u0430\u0431\u043e\u0442\u0430 \u0441 \u041f\u0421'
                },
                {
                    code: 'AlterInvoiceContract',
                    id: 11111,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0430 \u0441\u0447\u0435\u0442\u0430'
                },
                {
                    code: 'ui_orders_report__manager_code',
                    id: 1009,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u0417\u0430\u043a\u0430\u0437\u044b \u0432 \u041f\u0418 - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u041c\u0435\u043d\u0435\u0434\u0436\u0435\u0440'
                },
                {
                    code: 'UseAdminPersons',
                    id: 11112,
                    name:
                        '\u0418\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u043d\u0438\u0435 \u043e\u0441\u043e\u0431\u044b\u0445 \u0442\u0438\u043f\u043e\u0432 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u043e\u0432'
                },
                {
                    code: 'CreateClient',
                    id: 27,
                    name:
                        '\u0421\u043e\u0437\u0434\u0430\u043d\u0438\u0435 \u043a\u043b\u0438\u0435\u043d\u0442\u0430'
                },
                {
                    code: 'deferpays_orders_report__invoice_eid',
                    id: 1013,
                    name:
                        '\u041e\u0442\u0447\u0435\u0442 \u041a\u0440\u0435\u0434\u0438\u0442\u044b - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u0424\u0438\u043a\u0442\u0438\u0432\u043d\u044b\u0439 \u0441\u0447\u0435\u0442'
                },
                {
                    code: 'ViewClients',
                    id: 11321,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u043a\u043b\u0438\u0435\u043d\u0442\u043e\u0432'
                },
                {
                    code: 'ViewPayments',
                    id: 11326,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u043f\u043b\u0430\u0442\u0435\u0436\u0435\u0439'
                },
                {
                    code: 'ManagersOperations',
                    id: 23,
                    name:
                        '\u0414\u0435\u0439\u0441\u0442\u0432\u0438\u044f \u043c\u0435\u043d\u0435\u0434\u0436\u0435\u0440\u0430'
                },
                {
                    code: 'OEBSReexportPerson',
                    id: 11225,
                    name:
                        '\u041f\u0435\u0440\u0435\u043f\u043e\u0441\u0442\u0430\u0432\u0438\u0442\u044c \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u0430 \u0432 \u043e\u0447\u0435\u0440\u0435\u0434\u044c \u044d\u043a\u0441\u043f\u043e\u0440\u0442\u0430 OEBS'
                },
                {
                    code: 'UseCorrectionTemplate',
                    id: 11341,
                    name:
                        '\u0418\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u043d\u0438\u0435 \u043a\u043e\u0440\u0440\u0435\u043a\u0442\u0438\u0440\u043e\u0432\u043e\u043a'
                },
                {
                    code: 'CreateCerificatePayments',
                    id: 4,
                    name:
                        '\u0412\u043d\u0435\u0441\u0435\u043d\u0438\u0435 \u043f\u043b\u0430\u0442\u0435\u0436\u0435\u0439 \u0441\u0435\u0440\u0442\u0438\u0444\u0438\u043a\u0430\u0442\u043e\u043c, \u0431\u0430\u0440\u0442\u0435\u0440\u043e\u043c'
                },
                {
                    code: 'IssueUSDInvoices',
                    id: 13,
                    name:
                        '\u0412\u044b\u0441\u0442\u0430\u0432\u043b\u044f\u0442\u044c \u0434\u043e\u043b\u043b\u0430\u0440\u043e\u0432\u044b\u0435 \u0441\u0447\u0435\u0442\u0430'
                },
                {
                    code: 'PersonPostAddressEdit',
                    id: 38,
                    name:
                        '\u0420\u0435\u0434\u0430\u043a\u0442\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0435 \u043b\u044e\u0431\u044b\u0445 \u043f\u043e\u043b\u0435\u0439 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u0430'
                },
                {
                    code: 'PatchInvoiceContract',
                    id: 11361,
                    name:
                        '\u041f\u0440\u0438\u0432\u044f\u0437\u043a\u0430/\u043e\u0442\u0432\u044f\u0437\u043a\u0430 \u0441\u0447\u0435\u0442\u043e\u0432 \u043a \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443'
                },
                {
                    code: 'consumes_list__consume_sum',
                    id: 1011,
                    name:
                        '\u0421\u043f\u0438\u0441\u043e\u043a \u0437\u0430\u044f\u0432\u043e\u043a - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u041f\u0435\u0440\u0432\u043e\u043d\u0430\u0447\u0430\u043b\u044c\u043d\u0430\u044f \u0441\u0443\u043c\u043c\u0430'
                },
                {
                    code: 'AlterFirmUATaxi',
                    id: 49,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b \u041e\u041e\u041e "\u042f\u043d\u0434\u0435\u043a\u0441.\u0422\u0430\u043a\u0441\u0438 \u0423\u043a\u0440\u0430\u0438\u043d\u0430"'
                },
                {
                    code: 'ModifyDiscounts',
                    id: 16,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0441\u043a\u0438\u0434\u043e\u043a'
                },
                {
                    code: 'OEBSReexportAct',
                    id: 11224,
                    name:
                        '\u041f\u0435\u0440\u0435\u043f\u043e\u0441\u0442\u0430\u0432\u0438\u0442\u044c \u0430\u043a\u0442 \u0432 \u043e\u0447\u0435\u0440\u0435\u0434\u044c \u044d\u043a\u0441\u043f\u043e\u0440\u0442\u0430 OEBS'
                },
                {
                    code: 'NotifyOrder',
                    id: 11281,
                    name:
                        '\u041e\u0442\u043f\u0440\u0430\u0432\u0438\u0442\u044c \u043d\u043e\u0442\u0438\u0444\u0438\u043a\u0430\u0446\u0438\u044e \u043f\u043e \u0437\u0430\u043a\u0430\u0437\u0443'
                },
                {
                    code: 'BillingSupport',
                    id: 1100,
                    name:
                        '\u0421\u0430\u043f\u043f\u043e\u0440\u0442 \u0411\u0438\u043b\u043b\u0438\u043d\u0433\u0430'
                },
                {
                    code: 'ViewAllManagerReports',
                    id: 12,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442\u044b \u0434\u043b\u044f \u043c\u0435\u043d\u0435\u0434\u0436\u0435\u0440\u043e\u0432'
                },
                {
                    code: 'invoices_report__manager',
                    id: 1007,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u0421\u0447\u0435\u0442\u0430 - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u0412\u043a\u043b\u044e\u0447\u0435\u043d\u0438\u0435 \u0437\u0430\u043a\u0430\u0437\u0430'
                },
                {
                    code: 'consumes_list__consume_qty',
                    id: 1010,
                    name:
                        '\u0421\u043f\u0438\u0441\u043e\u043a \u0437\u0430\u044f\u0432\u043e\u043a - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u041f\u0435\u0440\u0432\u043e\u043d\u0430\u0447\u0430\u043b\u044c\u043d\u043e\u0435 \u043a\u043e\u043b\u0438\u0447\u0435\u0441\u0442\u0432\u043e'
                },
                {
                    code: 'AlterFirmRU',
                    id: 31,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b \u041e\u041e\u041e \u00ab\u042f\u043d\u0434\u0435\u043a\u0441\u00bb'
                },
                {
                    code: 'invoices_report__manager_info',
                    id: 1006,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u0421\u0447\u0435\u0442\u0430 - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u041c\u0435\u043d\u0435\u0434\u0436\u0435\u0440'
                },
                {
                    code: 'EditContracts',
                    id: 20,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u043e\u0432'
                },
                {
                    code: 'WithdrawConsumesPostpay',
                    id: 66,
                    name:
                        '\u0421\u043d\u044f\u0442\u0438\u0435 \u0437\u0430\u0447\u0438\u0441\u043b\u0435\u043d\u0438\u0439 \u0441 \u043f\u043e\u0441\u0442\u043e\u043f\u043b\u0430\u0442\u043d\u044b\u0445 \u0441\u0447\u0435\u0442\u043e\u0432'
                },
                {
                    code: 'ui_orders_report__client_id',
                    id: 1008,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u0417\u0430\u043a\u0430\u0437\u044b \u0432 \u041f\u0418 - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 ID \u043a\u043b\u0438\u0435\u043d\u0442\u0430'
                },
                {
                    code: 'AlterFirmNVTaxiuber',
                    id: 63,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b Uber ML B.V.'
                },
                {
                    code: 'TransferUnusedFunds',
                    id: 11121,
                    name:
                        '\u041f\u0435\u0440\u0435\u043d\u043e\u0441 \u0441\u0432\u043e\u0431\u043e\u0434\u043d\u044b\u0445 \u0441\u0440\u0435\u0434\u0441\u0442\u0432'
                },
                {
                    code: 'SubstituteLogin',
                    id: 24,
                    name:
                        '\u041f\u043e\u0434\u043c\u0435\u043d\u0430 \u043b\u043e\u0433\u0438\u043d\u0430'
                },
                {
                    code: 'OEBSReexportInvoice',
                    id: 11223,
                    name:
                        '\u041f\u0435\u0440\u0435\u043f\u043e\u0441\u0442\u0430\u0432\u0438\u0442\u044c \u0441\u0447\u0451\u0442 \u0432 \u043e\u0447\u0435\u0440\u0435\u0434\u044c \u044d\u043a\u0441\u043f\u043e\u0440\u0442\u0430 OEBS'
                },
                {
                    code: 'NewUIEarlyAdopter',
                    id: 64,
                    name:
                        '\u0412\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u044c \u0440\u0430\u0431\u043e\u0442\u044b \u0441 \u043d\u043e\u0432\u044b\u043c\u0438 \u0431\u043b\u043e\u043a\u0430\u043c\u0438 \u0438\u043d\u0442\u0435\u0440\u0444\u0435\u0439\u0441\u0430 \u0441\u0430\u0439\u0442\u0430'
                },
                {
                    code: 'AlterFirmUS',
                    id: 34,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b Yandex Inc'
                },
                {
                    code: 'AlterFirmKZTaxi',
                    id: 51,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b \u041e\u041e\u041e \u00ab\u042f\u043d\u0434\u0435\u043a\u0441.\u0422\u0430\u043a\u0441\u0438 \u041a\u0430\u0437\u0430\u0445\u0441\u0442\u0430\u043d\u00bb'
                },
                {
                    code: 'AlterFirmEU',
                    id: 37,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b Yandex Europe B.V.'
                },
                {
                    code: 'AlterFirmHK',
                    id: 65,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b Yandex E-commerce Limited'
                },
                {
                    code: 'AlterFirmKZ',
                    id: 33,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b \u0422\u041e\u041e \u00abKazNet Media (\u041a\u0430\u0437\u041d\u0435\u0442 \u041c\u0435\u0434\u0438\u0430)\u00bb'
                },
                {
                    code: 'UseRestrictedINN',
                    id: 11301,
                    name:
                        '\u0418\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u043d\u0438\u0435 \u0437\u0430\u043f\u0440\u0435\u0449\u0451\u043d\u043d\u044b\u0445 \u0418\u041d\u041d'
                },
                {
                    code: 'invoices_report__receipt_sum_1c',
                    id: 1001,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u0421\u0447\u0435\u0442\u0430 - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u0421\u0443\u043c\u043c\u0430 \u043f\u0440\u0438\u0445\u043e\u0434\u0430 \u0438\u0437 1\u0421'
                },
                {
                    code: 'WithdrawConsumesPrepay',
                    id: 67,
                    name:
                        '\u0421\u043d\u044f\u0442\u0438\u0435 \u0437\u0430\u0447\u0438\u0441\u043b\u0435\u043d\u0438\u0439 \u0441 \u043f\u0440\u0435\u0434\u043e\u043f\u043b\u0430\u0442\u043d\u044b\u0445 \u0441\u0447\u0435\u0442\u043e\u0432'
                },
                {
                    code: 'DoInvoiceRefunds',
                    id: 4004,
                    name:
                        '\u0421\u043e\u0437\u0434\u0430\u043d\u0438\u0435 \u0432\u043e\u0437\u0432\u0440\u0430\u0442\u043e\u0432 \u043f\u043e \u0441\u0447\u0435\u0442\u0430\u043c'
                },
                {
                    code: 'SetClientOverdraftBan',
                    id: 11325,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0438\u0442\u044c \u0431\u0430\u043d \u043e\u0432\u0435\u0440\u0434\u0440\u0430\u0444\u0442\u0430 \u0434\u043b\u044f \u043a\u043b\u0438\u0435\u043d\u0442\u0430'
                },
                {
                    code: 'ChangeRepaymentsStatus',
                    id: 11103,
                    name:
                        '\u041f\u043e\u0434\u0442\u0432\u0435\u0440\u0436\u0434\u0435\u043d\u0438\u0435 \u0441\u0447\u0435\u0442\u043e\u0432 \u043d\u0430 \u043f\u043e\u0433\u0430\u0448\u0435\u043d\u0438\u0435'
                },
                {
                    code: 'SendInvoices',
                    id: 11102,
                    name:
                        '\u041e\u0442\u043f\u0440\u0430\u0432\u043a\u0430 \u0441\u0447\u0435\u0442\u043e\u0432 \u043d\u0430 \u043f\u043e\u0447\u0442\u0443'
                },
                {
                    code: 'assist_payments_report__fraud_dt',
                    id: 1004,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u041f\u043b\u0430\u0442\u0435\u0436\u0438 \u043f\u043e Assist - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u0414\u0430\u0442\u0430 \u043e\u0431\u043d\u0430\u0440\u0443\u0436\u0435\u043d\u0438\u044f \u0444\u0440\u043e\u0434\u0430'
                },
                {
                    code: 'AlterFirmTR',
                    id: 41,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b Yandex Turkey'
                },
                {
                    code: 'ViewInvoices',
                    id: 11105,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u0441\u0447\u0435\u0442\u043e\u0432'
                },
                {
                    code: 'EditINN',
                    id: 43,
                    name:
                        '\u0417\u0430\u0432\u0435\u0434\u0435\u043d\u0438\u0435 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u0430 \u0441 \u0415\u0413\u0420\u041f\u041e\u0423 \u0432\u043c\u0435\u0441\u0442\u043e \u0418\u041d\u041d'
                },
                {
                    code: 'IssueInvoices',
                    id: 11106,
                    name:
                        '\u0412\u044b\u0441\u0442\u0430\u0432\u043b\u0435\u043d\u0438\u0435 \u0441\u0447\u0435\u0442\u043e\u0432'
                },
                {
                    code: 'ViewContracts',
                    id: 11181,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u043e\u0432'
                },
                {
                    code: 'GrantAllPermissionsToAll',
                    id: 7,
                    name:
                        '\u041d\u0430\u0437\u043d\u0430\u0447\u0435\u043d\u0438\u0435 \u043f\u043e\u043b\u043d\u043e\u043c\u043e\u0447\u0438\u0439'
                },
                {
                    code: 'TransferFromInvoice',
                    id: 11142,
                    name:
                        '\u041f\u0435\u0440\u0435\u043d\u043e\u0441 \u0441\u0440\u0435\u0434\u0441\u0442\u0432 \u0441\u043e \u0441\u0447\u0435\u0442\u0430'
                },
                {
                    code: 'ApexReports',
                    id: 3001,
                    name: '\u0410\u0440\u0435\u0445. \u041e\u0442\u0447\u0435\u0442\u044b'
                },
                {
                    code: 'AlterInvoiceSum',
                    id: 11107,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0441\u0443\u043c\u043c\u044b \u0441\u0447\u0435\u0442\u0430'
                },
                {
                    code: 'AdditionalFunctions',
                    id: 47,
                    name:
                        '\u0414\u043e\u043f\u043e\u043b\u043d\u0438\u0442\u0435\u043b\u044c\u043d\u044b\u0435 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438'
                },
                {
                    code: 'RecalculateOverdraft',
                    id: 42,
                    name:
                        '\u041f\u0435\u0440\u0435\u0440\u0430\u0441\u0447\u0435\u0442 \u043e\u0432\u0435\u0440\u0434\u0440\u0430\u0444\u0442\u0430'
                },
                {
                    code: 'AlterInvoiceDate',
                    id: 11108,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u0430\u0442\u044b \u0441\u0447\u0435\u0442\u0430'
                },
                {
                    code: 'DoInvoiceRefundsTrust',
                    id: 11161,
                    name:
                        '\u0421\u043e\u0437\u0434\u0430\u043d\u0438\u0435 \u0432\u043e\u0437\u0432\u0440\u0430\u0442\u043e\u0432 \u043f\u043e \u0441\u0447\u0435\u0442\u0430\u043c \u0447\u0435\u0440\u0435\u0437 \u0422\u0440\u0430\u0441\u0442'
                },
                {
                    code: 'TerminalView',
                    id: 56,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u0442\u0435\u0440\u043c\u0438\u043d\u0430\u043b\u0430'
                },
                {
                    code: 'AlterFirmSW',
                    id: 39,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b Yandex Europe AG'
                },
                {
                    code: 'TransferFromOrder',
                    id: 11141,
                    name:
                        '\u041f\u0435\u0440\u0435\u043d\u043e\u0441 \u0441\u0440\u0435\u0434\u0441\u0442\u0432 c \u0437\u0430\u043a\u0430\u0437\u0430'
                },
                {
                    code: 'ViewReports',
                    id: 2,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u043e\u0442\u0447\u0435\u0442\u043e\u0432'
                },
                {
                    code: 'AlterPrintTemplate',
                    id: 55,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0448\u0430\u0431\u043b\u043e\u043d\u0430 \u043f\u0435\u0447\u0430\u0442\u043d\u043e\u0439 \u0444\u043e\u0440\u043c\u044b \u0432 \u0434\u043e\u0433\u043e\u0432\u0440\u0435'
                },
                {
                    code: 'TransferBetweenClients',
                    id: 11143,
                    name:
                        '\u041f\u0435\u0440\u0435\u043d\u043e\u0441 \u0441\u0440\u0435\u0434\u0441\u0442\u0432 \u043c\u0435\u0436\u0434\u0443 \u0440\u0430\u0437\u043d\u044b\u043c\u0438 \u043a\u043b\u0438\u0435\u043d\u0442\u0430\u043c\u0438'
                },
                {
                    code: 'IssueAnyInvoices',
                    id: 17,
                    name:
                        '\u0414\u043e\u0432\u044b\u0441\u0442\u0430\u0432\u043b\u0435\u043d\u0438\u0435 \u0441\u0447\u0435\u0442\u0430 \u0437\u0430 \u043a\u043e\u0433\u043e \u0443\u0433\u043e\u0434\u043d\u043e'
                },
                {
                    code: 'AlterFirmAMTaxi',
                    id: 53,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b \u041e\u041e\u041e \u00ab\u042f\u043d\u0434\u0435\u043a\u0441.\u0422\u0430\u043a\u0441\u0438 \u0410\u0440\u043c\u0435\u043d\u0438\u044f\u00bb'
                },
                {
                    code: 'assist_payments_report__postauth_status',
                    id: 1003,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u041f\u043b\u0430\u0442\u0435\u0436\u0438 \u043f\u043e Assist - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u0421\u0442\u0430\u0442\u0443\u0441 \u043f\u043e\u0441\u0442-\u0430\u0432\u0442\u043e\u0440\u0438\u0437\u0430\u0446\u0438\u0438'
                },
                {
                    code: 'OEBSReexportContract',
                    id: 11226,
                    name:
                        '\u041f\u0435\u0440\u0435\u043f\u043e\u0441\u0442\u0430\u0432\u0438\u0442\u044c \u0434\u043e\u0433\u043e\u0432\u043e\u0440/\u0434\u043e\u043f.\u0441\u043e\u0433\u043b\u0430\u0448\u0435\u043d\u0438\u0435 \u0432 \u043e\u0447\u0435\u0440\u0435\u0434\u044c \u044d\u043a\u0441\u043f\u043e\u0440\u0442\u0430 OEBS'
                },
                {
                    code: 'TerminalEdit',
                    id: 57,
                    name:
                        '\u0420\u0435\u0434\u0430\u043a\u0442\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0435 \u0442\u0435\u0440\u043c\u0438\u043d\u0430\u043b\u0430'
                },
                {
                    code: 'ClientFraudStatusEdit',
                    id: 58,
                    name:
                        '\u0420\u0435\u0434\u0430\u043a\u0442\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0435 \u043a\u043b\u0438\u0435\u043d\u0442\u0430: \u043f\u0440\u043e\u0441\u0442\u0430\u043d\u043e\u0432\u043a\u0430 \u0444\u043b\u0430\u0433\u0430 deny_cc / fraud_status'
                },
                {
                    code: 'AlterFirmNVTaxi',
                    id: 48,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b Yandex.Taxi B.V.'
                },
                {
                    code: 'PayInvoiceExtension',
                    id: 11221,
                    name:
                        '\u041f\u0435\u0440\u0435\u043d\u0435\u0441\u0442\u0438 \u0441\u0440\u043e\u043a \u043e\u043f\u043b\u0430\u0442\u044b \u0441\u0447\u0451\u0442\u0430'
                },
                {
                    code: 'ManageBadDebts',
                    id: 21,
                    name:
                        '\u041f\u0440\u0438\u0437\u043d\u0430\u043d\u0438\u0435 "\u043f\u043b\u043e\u0445\u043e\u0433\u043e" \u0434\u043e\u043b\u0433\u0430'
                },
                {
                    code: 'AlterFirmAUTORU',
                    id: 45,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b \u041e\u041e\u041e \u00ab\u0410\u0412\u0422\u041e.\u0420\u0423 \u0425\u043e\u043b\u0434\u0438\u043d\u0433\u00bb'
                },
                {
                    code: 'ViewOrders',
                    id: 11201,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u0437\u0430\u043a\u0430\u0437\u043e\u0432'
                },
                {
                    code: 'orders_report__client_id',
                    id: 1005,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u0417\u0430\u043a\u0430\u0437\u044b - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 ID \u043a\u043b\u0438\u0435\u043d\u0442\u0430'
                },
                {
                    code: 'CloseInvoice',
                    id: 11222,
                    name:
                        '\u0417\u0430\u043a\u0440\u044b\u0442\u0438\u0435 \u0441\u0447\u0451\u0442\u0430'
                },
                {
                    code: 'ProcessAllInvoices',
                    id: 8,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0441\u0447\u0435\u0442\u043e\u0432'
                },
                {
                    code: 'ViewAllInvoices',
                    id: 1,
                    name:
                        '\u0423\u0441\u0442\u0430\u0440\u0435\u0432\u0448\u0435\u0435, \u043d\u0435 \u0438\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u0442\u044c. \u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u0441\u0447\u0435\u0442\u043e\u0432.'
                },
                {
                    code: 'LocalNamesMaster',
                    id: 59,
                    name:
                        '\u0420\u0435\u0434\u0430\u043a\u0442\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0435 \u043b\u043e\u043a\u0430\u043b\u0438\u0437\u043e\u0432\u0430\u043d\u043d\u044b\u0445 \u0430\u0442\u0440\u0438\u0431\u0443\u0442\u043e\u0432 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u043e\u0432'
                },
                {
                    code: 'PaymentLinksReport',
                    id: 11104,
                    name:
                        '\u041f\u043e\u0438\u0441\u043a \u043f\u043e \u043f\u043b\u0430\u0442\u0435\u0436\u0430\u043c'
                },
                {
                    code: 'PersonExtEdit',
                    id: 46,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0432\u0430\u0436\u043d\u044b\u0445 \u043f\u043e\u043b\u0435\u0439 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u0430'
                },
                {
                    code: 'EDOViewer',
                    id: 60,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u0433\u0440\u0430\u0444\u0438\u043a\u0430 \u042d\u0414\u041e'
                },
                {
                    code: 'invoices_report__client',
                    id: 1002,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u0421\u0447\u0435\u0442\u0430 - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 ID \u043a\u043b\u0438\u0435\u043d\u0442\u0430'
                },
                {
                    code: 'r_django_fraud_monitoring',
                    id: 62,
                    name:
                        '\u041f\u0440\u0430\u0432\u0430 \u043d\u0430 \u0447\u0442\u0435\u043d\u0438\u0435 Django Fraud Monitoring'
                },
                {
                    code: 'ViewFishes',
                    id: 22,
                    name: '\u0424\u0438\u0448\u043a\u043e\u0432\u0438\u0434\u0435\u0446'
                },
                {
                    code: 'AlterInvoicePaysys',
                    id: 11109,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0441\u043f\u043e\u0441\u043e\u0431\u0430 \u043e\u043f\u043b\u0430\u0442\u044b \u0441\u0447\u0435\u0442\u0430'
                }
            ]
        }
    },
    urPersonforms: {
        request: ['/static/personforms/json/ur.json', undefined, false, false, false],
        response: {
            'person-details': {
                'xmlns:xi': 'http://www.w3.org/2001/XInclude',
                type: 'ur',
                'is-partner': '0',
                caption: 'ID_Legal_entity',
                name: 'ID_Legal_entity_or_Indiv_entrepr',
                'details-block': [
                    {
                        title: 'ID_General_information',
                        detail: [
                            {
                                id: 'lname',
                                caption: 'ID_Last_name',
                                type: 'text',
                                size: '128',
                                'required-mark': '',
                                selfemployed: '',
                                'check-type': 'not_empty'
                            },
                            {
                                id: 'fname',
                                caption: 'ID_First_name',
                                type: 'text',
                                size: '128',
                                'required-mark': '',
                                selfemployed: '',
                                'check-type': 'not_empty'
                            },
                            {
                                id: 'mname',
                                caption: 'ID_Middle_name',
                                type: 'text',
                                size: '128',
                                'required-mark': '',
                                selfemployed: '',
                                'check-type': 'not_empty'
                            },
                            {
                                id: 'inn',
                                caption: 'ID_Tax_Identification_Number_INN',
                                type: 'text',
                                'required-mark': '',
                                locked: '',
                                'check-type': 'check_inn',
                                size: '12',
                                sample: '7712345678'
                            },
                            {
                                id: 'name',
                                caption: 'ID_Name_of_organisation',
                                type: 'text',
                                size: '256',
                                'required-mark': '',
                                'not-selfemployed': '',
                                locked: 'flex',
                                'check-type': 'not_empty',
                                sample: 'ID_Yandex',
                                hint: 'ID_wo_quotation_marks_and_acronyms'
                            },
                            {
                                id: 'longname',
                                caption: 'ID_Organizations_full_name',
                                type: 'text',
                                size: '256',
                                'required-mark': '',
                                'not-selfemployed': '',
                                locked: 'flex',
                                'check-type': 'not_empty',
                                sample: 'ID_Yandex_Ltd',
                                hint: 'ID_business_type_abbreviation'
                            },
                            {
                                id: 'kpp',
                                caption: 'ID_Tax_Code_KPP',
                                type: 'text',
                                'required-mark': '',
                                size: '9',
                                sample: '771234567',
                                'check-type': 'balance.jointpersons.checkKpp',
                                tip: 'ID_Person_form_tip_kpp_required_for_closing_documents'
                            },
                            {
                                id: 'ogrn',
                                caption: 'ID_OGRN',
                                type: 'text',
                                'check-type': 'check_ogrn',
                                sample: '1234567890123'
                            },
                            {
                                id: 'phone',
                                caption: 'ID_Telephone',
                                type: 'text',
                                size: '32',
                                'required-mark': '',
                                'check-type': 'not_empty',
                                sample: '+7 495 123-45-67',
                                hint: 'ID_with_country_and_area_code'
                            },
                            {
                                id: 'email',
                                caption: 'ID_Email',
                                type: 'text',
                                size: '256',
                                'required-mark': '',
                                'check-type': 'email',
                                tip: 'ID_Electronic_copies_of_account_documents_w'
                            },
                            {
                                id: 'fax',
                                caption: 'ID_Fax',
                                type: 'text',
                                size: '32',
                                sample: '+7 495 321-54-76'
                            },
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
                                id: 'representative',
                                caption: 'ID_Contact',
                                type: 'text',
                                size: '128'
                            },
                            {
                                id: 'revise-act-period-type',
                                'backoffice-only': '',
                                type: 'select1',
                                source: 'revise-act-options',
                                caption: 'ID_Request_revise_act'
                            }
                        ]
                    },
                    {
                        title: 'ID_Legal_address',
                        detail: {
                            id: 'legal-addr-type',
                            caption: 'ID_Input_selection',
                            type: 'radio',
                            'required-mark': '',
                            source: 'legal-addr-types',
                            locked: 'flex',
                            'edit-only': '',
                            'check-type': 'radio'
                        }
                    },
                    {
                        id: 'legal_plain_address',
                        hidden: '',
                        detail: [
                            {
                                id: 'legaladdress',
                                caption: 'ID_Legal_address',
                                type: 'textarea',
                                locked: 'flex',
                                size: '256',
                                'required-mark': '',
                                'edit-only': '',
                                'check-type': 'not_empty'
                            },
                            {
                                id: 'legal-plain-code',
                                name: 'legal-address-code',
                                type: 'hidden'
                            },
                            {
                                id: 'legal-plain-guid',
                                name: 'legal-fias-guid',
                                type: 'hidden'
                            },
                            {
                                id: 'legal-plain-city',
                                name: 'legal-address-city',
                                type: 'hidden'
                            },
                            {
                                id: 'legal-plain-street',
                                name: 'legal-address-street',
                                type: 'hidden'
                            },
                            {
                                id: 'legal-plain-home',
                                name: 'legal-address-home',
                                type: 'hidden'
                            },
                            {
                                id: 'legal-plain-postcode',
                                name: 'legal-address-postcode',
                                type: 'hidden'
                            }
                        ]
                    },
                    {
                        id: 'legal_fias',
                        hidden: '',
                        detail: [
                            {
                                id: 'legal-address-city',
                                caption: 'ID_Region',
                                type: 'text',
                                'required-mark': '',
                                'edit-only': '',
                                locked: 'flex',
                                'check-type': 'legal_fias_city',
                                size: '128',
                                sample: 'ID_Moscow'
                            },
                            {
                                id: 'legal-address-street',
                                caption: 'ID_Street',
                                type: 'text',
                                'required-mark': '',
                                'edit-only': '',
                                'check-type': 'legal_fias_street',
                                size: '50',
                                locked: 'flex',
                                sample: 'ID_Lva_Tolstogo_ul'
                            },
                            {
                                id: 'legal-address-postcode',
                                caption: 'ID_Postcode',
                                type: 'text',
                                'required-mark': '',
                                'check-type': 'check_digit_length',
                                size: '6',
                                'edit-only': '',
                                locked: 'flex',
                                sample: '119021'
                            },
                            {
                                id: 'legal-address-home',
                                caption: 'ID_House_block_number',
                                type: 'text',
                                'required-mark': '',
                                'edit-only': '',
                                'check-type': 'not_empty',
                                size: '100',
                                sample: 'ID_street_addr_hint3',
                                locked: 'flex',
                                hint: 'ID_enter_house_building_etc'
                            },
                            {
                                id: 'legal-sample',
                                type: 'textarea',
                                caption: 'ID_Legal_address',
                                disabled: ''
                            },
                            {
                                id: 'legal-address',
                                name: 'legaladdress',
                                type: 'hidden'
                            },
                            {
                                id: 'legal-fias-guid',
                                type: 'hidden'
                            }
                        ]
                    },
                    {
                        title: 'ID_Post_address',
                        detail: {
                            'admin-only': '',
                            id: 'invalid-address',
                            type: 'checkbox',
                            caption: 'ID_Invalid_address'
                        }
                    },
                    {
                        detail: [
                            {
                                id: 'is-postbox',
                                caption: 'ID_Delivery_selection',
                                type: 'radio',
                                'required-mark': '',
                                source: 'is-postbox-types',
                                'check-type': 'radio',
                                tip: 'ID_address_contact_support_note',
                                'not-required-for-selfemployed': ''
                            },
                            {
                                id: 'kladr-code',
                                type: 'hidden'
                            }
                        ]
                    },
                    {
                        id: 'ur_address',
                        hidden: '',
                        detail: [
                            {
                                id: 'city',
                                caption: 'ID_Region',
                                type: 'text',
                                'required-mark': {
                                    'not-selfemployed': 'true'
                                },
                                'edit-only': '',
                                'check-type': 'fias_city',
                                size: '128',
                                sample: 'ID_Moscow',
                                'not-required-for-selfemployed': ''
                            },
                            {
                                id: 'street',
                                caption: 'ID_Street',
                                type: 'text',
                                'required-mark': '',
                                'edit-only': '',
                                'check-type': 'fias_street',
                                size: '50',
                                sample: 'ID_Lva_Tolstogo_ul',
                                'not-required-for-selfemployed': ''
                            },
                            {
                                id: 'postcode',
                                caption: 'ID_Postcode',
                                type: 'text',
                                'required-mark': '',
                                'check-type': 'check_digit_length',
                                size: '6',
                                sample: '119021',
                                'not-required-for-selfemployed': ''
                            },
                            {
                                id: 'postsuffix',
                                caption: 'ID_House_block_number',
                                type: 'text',
                                'required-mark': '',
                                'edit-only': '',
                                'check-type': 'not_empty',
                                size: '100',
                                sample: 'ID_street_addr_hint3',
                                hint: 'ID_enter_house_building_etc',
                                'not-required-for-selfemployed': ''
                            },
                            {
                                id: 'fias-guid',
                                type: 'hidden'
                            }
                        ]
                    },
                    {
                        id: 'ur_postbox',
                        hidden: '',
                        detail: [
                            {
                                id: 'postcode-simple',
                                name: 'postcode',
                                caption: 'ID_Postcode',
                                type: 'text',
                                'required-mark': '',
                                'edit-only': '',
                                'check-type': 'check_digit_length',
                                size: '6',
                                sample: '111033',
                                'not-required-for-selfemployed': ''
                            },
                            {
                                id: 'postbox',
                                name: 'postsuffix',
                                caption: 'ID_PO_box',
                                type: 'text',
                                'required-mark': '',
                                'check-type': 'not_empty',
                                'edit-only': '',
                                size: '20',
                                sample: 'ID_PO_box_sample',
                                'not-required-for-selfemployed': ''
                            },
                            {
                                id: 'street-box',
                                name: 'street',
                                type: 'hidden'
                            },
                            {
                                id: 'fias-guid',
                                name: 'fias-guid',
                                type: 'hidden'
                            }
                        ]
                    },
                    {
                        id: 'envelope_sample',
                        title: 'ID_newline',
                        hidden: '',
                        detail: {
                            id: 'envelope-address',
                            type: 'textarea',
                            caption: 'ID_Post_address',
                            disabled: '',
                            hint: 'ID_this_is_how_your_address_will_appear',
                            tip: 'ID_Original_account_documents_will_be_sent'
                        }
                    },
                    {
                        title: 'ID_Payment_details_and_additional_information',
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
                                id: 'bank-data/bank-name',
                                caption: 'ID_Bank_name',
                                'show-only': ''
                            },
                            {
                                id: 'bank-data/bank-city',
                                caption: 'ID_Bank_location',
                                'show-only': ''
                            },
                            {
                                id: 'bank-data/corr-account',
                                caption: 'ID_Correspondent_account',
                                'show-only': ''
                            }
                        ]
                    },
                    {
                        title: '',
                        'admin-only': '',
                        detail: [
                            {
                                id: 'address',
                                caption: 'ID_Shipment_address',
                                type: 'text',
                                size: '256',
                                hint: 'ID_City_Street_addr_hint'
                            },
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
                                size: '100'
                            },
                            {
                                id: 'vip',
                                'backoffice-only': '',
                                type: 'checkbox',
                                caption: 'ID_VIP'
                            }
                        ]
                    },
                    {
                        title: 'ID_state_institutions',
                        detail: [
                            {
                                id: 'kbk',
                                caption: 'ID_kbk',
                                type: 'text',
                                size: '20',
                                'check-type': 'check_kbk'
                            },
                            {
                                id: 'oktmo',
                                caption: 'ID_oktmo',
                                type: 'text',
                                size: '11',
                                'check-type': 'check_oktmo'
                            },
                            {
                                id: 'payment-purpose',
                                caption: 'ID_payment_purpose',
                                type: 'text',
                                size: '210',
                                'backoffice-only': ''
                            }
                        ]
                    }
                ]
            }
        }
    },
    invoiceActs: {
        request: [
            `${HOST}/invoice/acts`,
            {
                invoice_id: '123',
                get_totals: true,
                acts_pn: 1,
                acts_ps: 10,
                sort_key: 'DT',
                sort_order: 'DESC'
            },
            false,
            false
        ],
        response: {
            version: { 'yb-snout-api': '3.trunk.7820184' },
            data: {
                acts_totals: {
                    unpaid_sum: '0.00',
                    amount: '0.00',
                    invoice_receipt_sum: '0.00',
                    amount_nds: '0.00',
                    bad_debt_acts_count: 0
                },
                ps: 10,
                acts: [],
                pn: 1,
                sz: 0
            }
        }
    },
    invoiceConsumes: {
        request: [
            'http://snout-test/invoice/consumes',
            {
                invoice_id: '123',
                initial_load: true,
                consumes_ps: 10,
                consumes_pn: 1,
                sort_key: 'DT',
                sort_order: 'DESC'
            },
            false,
            false
        ],
        response: {
            version: { 'yb-snout-api': '3.trunk.7820184' },
            data: {
                consumes_list: [],
                consumes_totals: {
                    'completion-sum': '0',
                    'current-amount': '0.00',
                    'act-sum': '0',
                    'completion-amount': '0.00',
                    'act-amount': '0.00',
                    'current-sum': '0',
                    'bonus-qty': '0'
                },
                pagination_pn: 1,
                pagination_ps: 10,
                invoice_internal_rate: '1RUB/FISH',
                invoice_currency_rate: '1',
                pagination_sz: 0
            }
        }
    },
    withdraw: {
        request: [
            'http://snout-test/order/withdraw/from-orders',
            {
                order_id: undefined,
                service_id: undefined,
                service_order_id_prefix: undefined,
                client_id: '188189111'
            },
            false,
            false
        ],
        response: {
            version: { 'yb-snout-api': '3.trunk.7820184' },
            data: [
                {
                    type_rate: null,
                    order_eid: '129-277142',
                    order_id: 3678860929,
                    text:
                        '\u041c\u0438\u043d\u0438\u043c\u0430\u043b\u044c\u043d\u043e\u0435 \u0432\u043e\u0437\u043d\u0430\u0433\u0440\u0430\u0436\u0434\u0435\u043d\u0438\u0435 \u0437\u0430 \u043f\u0440\u0435\u0434\u043e\u0441\u0442\u0430\u0432\u043b\u0435\u043d\u0438\u0435 \u043f\u0440\u0430\u0432\u0430 \u0438\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u043d\u0438\u044f \u0411\u0430\u0437\u044b \u0434\u0430\u043d\u043d\u044b\u0445 \u042f\u043d\u0434\u0435\u043a\u0441.\u041a\u0430\u0440\u0442\u044b \u043f\u043e\u0441\u0440\u0435\u0434\u0441\u0442\u0432\u043e\u043c JS API \u0437\u0430 \u0432\u0435\u0441\u044c \u0441\u0440\u043e\u043a \u0434\u0435\u0439\u0441\u0442\u0432\u0438\u044f \u043b\u0438\u0446\u0435\u043d\u0437\u0438\u0438',
                    client_name: 'balance_test 2021-01-19 04:00:48.529666',
                    service_cc: 'apikeys',
                    product_name:
                        '\u041c\u0438\u043d\u0438\u043c\u0430\u043b\u044c\u043d\u043e\u0435 \u0432\u043e\u0437\u043d\u0430\u0433\u0440\u0430\u0436\u0434\u0435\u043d\u0438\u0435 \u0437\u0430 \u043f\u0440\u0435\u0434\u043e\u0441\u0442\u0430\u0432\u043b\u0435\u043d\u0438\u0435 \u043f\u0440\u0430\u0432\u0430 \u0438\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u043d\u0438\u044f \u0411\u0430\u0437\u044b \u0434\u0430\u043d\u043d\u044b\u0445 \u042f\u043d\u0434\u0435\u043a\u0441.\u041a\u0430\u0440\u0442\u044b \u043f\u043e\u0441\u0440\u0435\u0434\u0441\u0442\u0432\u043e\u043c JS API \u0437\u0430 \u0432\u0435\u0441\u044c \u0441\u0440\u043e\u043a \u0434\u0435\u0439\u0441\u0442\u0432\u0438\u044f \u043b\u0438\u0446\u0435\u043d\u0437\u0438\u0438',
                    last_touch_dt: '2021-01-19',
                    service_order_id: 277142,
                    client_id: 188189111,
                    price_wo_nds: '0.000000',
                    service_id: 129,
                    order_dt: '2021-01-19',
                    price: '0.000000',
                    unit: null,
                    order_client_id: 188189111
                }
            ]
        }
    },
    invoiceOperations: {
        request: [
            'http://snout-test/invoice/operations',
            {
                invoice_id: '123',
                limit: 25,
                offset: 0,
                sort_key: 'DEFAULT',
                sort_order: 'DESC'
            },
            false,
            false
        ],
        response: {
            version: { 'yb-snout-api': '3.trunk.7820184' },
            data: { has_next: true, operation: [], has_prev_pages: 0, has_next_pages: 0 }
        }
    },
    exportState: {
        request: [
            'http://snout-test/export/get-state',
            {
                classname: 'Invoice',
                queue_type: 'OEBS',
                object_id: '123'
            },
            false,
            false
        ],
        response: {
            version: { 'yb-snout-api': '3.trunk.7820184' },
            data: { export_dt: null, state: 'WAITING' }
        }
    },
    oebsData: {
        request: ['http://snout-test/invoice/oebs-data', { invoice_id: '123' }, true, false],
        response: {
            version: { 'yb-snout-api': '3.trunk.7820184' },
            data: {
                oebs_nn: { total_row_count: 0, items: [] },
                payments1c: { total_row_count: 0, items: [] },
                oebs_factura: { total_row_count: 0, items: [] }
            }
        }
    }
};
