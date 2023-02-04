import { HOST } from 'common/utils/test-utils/common';

export const history = {
    perms: ['AdminAccess'],
    search:
        '?agency_select_policy=3&client_id=5028445&email=v.krasko%40netpeak.net&fax=%2B1234567890&hide_managers=1&intercompany=AM31&is_accurate=1&login=netpeakru&manual_suspect=1&name=Netpeak&phone=%2B38%20063%2080%2040%20690&reliable_cc_payer=1&single_account_number=123456&with_invoices=1&url=http%3A%2F%2Fnetpeak.ua%2F&pn=1&ps=10&manager_code=20869',
    filter: {
        name: 'Netpeak',
        login: 'netpeakru',
        clientId: '5028445',
        singleAccountNumber: '123456',
        agencySelectPolicy: 'AGENCY',
        url: 'http://netpeak.ua/',
        email: 'v.krasko@netpeak.net',
        phone: '+38 063 80 40 690',
        fax: '+1234567890',
        withInvoices: 'true',
        manager: 'Свинцицкий Андрей',
        intercompany: 'AM31',
        isAccurate: true,
        hideManagers: true,
        manualSuspect: true,
        reliableClient: true
    },
    firmIntercompanyList: {
        request: [{ url: `${HOST}/firm/intercompany_list` }],
        response: [
            { cc: 'AM31', is_active: true, label: 'Yandex.Taxi AM' },
            { cc: 'AM32', is_active: true, label: 'Yandex.Taxi Corp AM' },
            { cc: 'AZ35', is_active: true, label: 'Uber Azerbaijan' },
            {
                cc: 'BY11',
                is_active: true,
                label:
                    '\u041e\u041e\u041e \u042f\u043d\u0434\u0435\u043a\u0441 \u0420\u0435\u043a\u043b\u0430\u043c\u0430'
            },
            {
                cc: 'BY12',
                is_active: true,
                label: '\u041e\u041e\u041e \u042f\u043d\u0434\u0435\u043a\u0441\u0411\u0435\u043b'
            },
            {
                cc: 'BY32',
                is_active: true,
                label: '\u041e\u041e\u041e \u0411\u0435\u043b\u0413\u043e \u041a\u043e\u0440\u043f'
            },
            {
                cc: 'BY35',
                is_active: true,
                label:
                    '\u041e\u041e\u041e \u0423\u0431\u0435\u0440 \u0421\u0438\u0441\u0442\u0435\u043c\u0441 \u0411\u0435\u043b'
            },
            { cc: 'CH10', is_active: true, label: 'Yandex Europe AG' },
            { cc: 'CH20', is_active: true, label: 'Yandex.Services AG' },
            { cc: 'CH30', is_active: true, label: 'Yandex Auto.ru AG' },
            { cc: 'CN10', is_active: true, label: 'Yan Jia Ke Si' },
            { cc: 'CY10', is_active: true, label: 'Znanie Company Ltd' },
            { cc: 'CY11', is_active: true, label: 'Znanie Development Company Ltd' },
            { cc: 'DE10', is_active: true, label: 'Yandex Technology GmbH' },
            {
                cc: 'ELIM',
                is_active: true,
                label:
                    '\u0420\u0435\u0437\u0443\u043b\u044c\u0442\u0430\u0442 \u044d\u043b\u0438\u043c\u0438\u043d\u0430\u0446\u0438\u0438'
            },
            { cc: 'FI10', is_active: true, label: 'Yandex Oy' },
            { cc: 'HK10', is_active: true, label: 'SPB Software Ltd.' },
            { cc: 'HK90', is_active: true, label: 'Yandex e-commerce Limited' },
            { cc: 'IL10', is_active: true, label: 'Yandex.Israel' },
            { cc: 'IL31', is_active: true, label: 'Yandex.Go Israel Ltd' },
            {
                cc: 'KZ10',
                is_active: true,
                label:
                    '\u0422\u041e\u041e \u042f\u043d\u0434\u0435\u043a\u0441.\u041a\u0430\u0437\u0430\u0445\u0441\u0442\u0430\u043d'
            },
            {
                cc: 'KZ31',
                is_active: true,
                label:
                    '\u0422\u041e\u041e \u042f\u043d\u0434\u0435\u043a\u0441.\u0422\u0430\u043a\u0441\u0438 \u041a\u0430\u0437\u0430\u0445\u0441\u0442\u0430\u043d'
            },
            {
                cc: 'KZ35',
                is_active: true,
                label:
                    '\u0422\u041e\u041e \u042f\u043d\u0434\u0435\u043a\u0441.\u0422\u0430\u043a\u0441\u0438 \u041a\u043e\u0440\u043f.'
            },
            { cc: 'NL10', is_active: true, label: 'Yandex N.V.' },
            { cc: 'NL11', is_active: true, label: 'Stichting Yandex Conversion' },
            { cc: 'NL12', is_active: true, label: 'Stichting Yandex Equity Incentive' },
            { cc: 'NL20', is_active: true, label: 'Yandex Europe B.V.' },
            { cc: 'NL31', is_active: true, label: 'Yandex.Taxi B.V.' },
            { cc: 'NL32', is_active: true, label: 'Yandex.Taxi Holding B.V.' },
            { cc: 'NL34', is_active: true, label: 'MLU B.V.' },
            { cc: 'NL35', is_active: true, label: 'Uber ML B.V.' },
            { cc: 'NL36', is_active: true, label: 'Uber ML Holdco B.V.' },
            { cc: 'NL38', is_active: true, label: 'MLU Africa B.V.' },
            { cc: 'NL39', is_active: true, label: 'MLU Europe B.V. OLD RON' },
            { cc: 'NL41', is_active: true, label: 'MLU Europe B.V.' },
            { cc: 'NL53', is_active: true, label: 'Yandex.Classifieds Holding B.V.' },
            { cc: 'NL70', is_active: true, label: 'Yandex Media Services B.V.' },
            { cc: 'NL90', is_active: true, label: 'Yandex.Market B.V.' },
            { cc: 'RELP', is_active: true, label: 'Ralated party' },
            { cc: 'RO31', is_active: true, label: 'Yandex.Go S.R.L.' },
            {
                cc: 'RU10',
                is_active: true,
                label: '\u041e\u041e\u041e \u042f\u043d\u0434\u0435\u043a\u0441'
            },
            {
                cc: 'RU11',
                is_active: true,
                label:
                    '\u041e\u041e\u041e \u042f\u043d\u0434\u0435\u043a\u0441.\u0422\u0435\u0445\u043d\u043e\u043b\u043e\u0433\u0438\u0438'
            },
            {
                cc: 'RU12',
                is_active: true,
                label: '\u041e\u041e\u041e \u041d\u0410\u041f\u0410'
            },
            {
                cc: 'RU13',
                is_active: true,
                label:
                    '\u041c\u0424 \u0424\u043e\u043d\u0434 \u043e\u0431\u0449\u0435\u0441\u0442\u0432\u0435\u043d\u043d\u044b\u0445 \u0438\u043d\u0442\u0435\u0440\u0435\u0441\u043e\u0432'
            },
            {
                cc: 'RU14',
                is_active: true,
                label:
                    '\u041e\u041e\u041e \u042f\u043d\u0434\u0435\u043a\u0441.\u041a\u0430\u0440\u0448\u0435\u0440\u0438\u043d\u0433'
            },
            {
                cc: 'RU15',
                is_active: true,
                label:
                    '\u0411\u043b\u0430\u0433\u043e\u0442\u0432\u043e\u0440\u0438\u0442\u0435\u043b\u044c\u043d\u044b\u0439 \u0424\u043e\u043d\u0434 \u041f\u043e\u043c\u043e\u0449\u044c \u0440\u044f\u0434\u043e\u043c'
            },
            {
                cc: 'RU20',
                is_active: true,
                label:
                    '\u041e\u041e\u041e \u042f\u043d\u0434\u0435\u043a\u0441.\u041f\u0440\u043e\u0431\u043a\u0438'
            },
            {
                cc: 'RU21',
                is_active: true,
                label:
                    '\u0421\u043f\u0431 \u0421\u043e\u0444\u0442\u0432\u0435\u0440 \u043b\u0438\u043c\u0438\u0442\u0435\u0434'
            },
            {
                cc: 'RU30',
                is_active: true,
                label:
                    '\u041e\u041e\u041e \u0413\u0418\u0421 \u0422\u0435\u0445\u043d\u043e\u043b\u043e\u0433\u0438\u0438'
            },
            {
                cc: 'RU31',
                is_active: true,
                label:
                    '\u041e\u041e\u041e \u042f\u043d\u0434\u0435\u043a\u0441.\u0422\u0430\u043a\u0441\u0438'
            },
            {
                cc: 'RU32',
                is_active: true,
                label:
                    '\u041e\u041e\u041e \u042f\u043d\u0434\u0435\u043a\u0441.\u0422\u0430\u043a\u0441\u0438 \u0422\u0435\u0445\u043d\u043e\u043b\u043e\u0433\u0438\u0438'
            },
            {
                cc: 'RU33',
                is_active: true,
                label:
                    '\u041e\u041e\u041e \u0422\u0435\u043b\u0435\u0441\u0438\u0441\u0442\u0435\u043c\u044b'
            },
            {
                cc: 'RU34',
                is_active: true,
                label: '\u041e\u041e\u041e \u0411\u0418\u0413\u0424\u0423\u0414'
            },
            {
                cc: 'RU35',
                is_active: true,
                label:
                    '\u041e\u041e\u041e \u0423\u0431\u0435\u0440 \u0422\u0435\u043a\u043d\u043e\u043b\u043e\u0434\u0436\u0438'
            },
            {
                cc: 'RU36',
                is_active: true,
                label:
                    '\u041e\u041e\u041e \u042f\u043d\u0434\u0435\u043a\u0441.\u0414\u0440\u0430\u0439\u0432'
            },
            {
                cc: 'RU37',
                is_active: true,
                label: '\u041e\u041e\u041e \u042f\u043d\u0434\u0435\u043a\u0441.\u0415\u0434\u0430'
            },
            {
                cc: 'RU38',
                is_active: true,
                label:
                    '\u041e\u041e\u041e \u042f\u043d\u0434\u0435\u043a\u0441.\u041b\u0430\u0432\u043a\u0430'
            },
            {
                cc: 'RU39',
                is_active: true,
                label:
                    '\u041e\u041e\u041e \u041b\u0430\u0431\u043e\u0440\u0430\u0442\u043e\u0440\u0438\u044f \u0410\u0432\u0442\u043e\u043f\u0430\u0440\u043a\u043e\u0432'
            },
            {
                cc: 'RU40',
                is_active: true,
                label: '\u041e\u041e\u041e \u042f\u043d\u0434\u0435\u043a\u0441 \u0414\u0426'
            },
            {
                cc: 'RU41',
                is_active: true,
                label:
                    '\u041e\u041e\u041e \u042f\u043d\u0434\u0435\u043a\u0441 \u0414\u0426 \u0412\u043b\u0430\u0434\u0438\u043c\u0438\u0440'
            },
            {
                cc: 'RU42',
                is_active: true,
                label:
                    '\u041e\u041e\u041e \u042f\u043d\u0434\u0435\u043a\u0441.\u0422\u0435\u043b\u0435\u043a\u043e\u043c'
            },
            {
                cc: 'RU43',
                is_active: true,
                label:
                    '\u041e\u041e\u041e \u042f\u043d\u0434\u0435\u043a\u0441.\u041e\u0431\u043b\u0430\u043a\u043e'
            },
            {
                cc: 'RU53',
                is_active: true,
                label:
                    '\u041e\u041e\u041e \u042f\u043d\u0434\u0435\u043a\u0441.\u0412\u0435\u0440\u0442\u0438\u043a\u0430\u043b\u0438'
            },
            {
                cc: 'RU54',
                is_active: true,
                label:
                    '\u041e\u041e\u041e \u042f\u043d\u0434\u0435\u043a\u0441.\u0412\u0435\u0440\u0442\u0438\u043a\u0430\u043b\u0438 \u0422\u0435\u0445\u043d\u043e\u043b\u043e\u0433\u0438\u0438'
            },
            {
                cc: 'RU61',
                is_active: true,
                label: '\u041e\u041e\u041e \u042f\u043d\u0434\u0435\u043a\u0441.\u041e\u0424\u0414'
            },
            {
                cc: 'RU62',
                is_active: true,
                label: '\u041e\u041e\u041e \u0415\u0434\u0430\u0434\u0438\u043b'
            },
            {
                cc: 'RU63',
                is_active: true,
                label:
                    '\u041e\u041e\u041e \u0415\u0434\u0430\u0434\u0438\u043b \u041f\u0440\u043e\u043c\u043e'
            },
            {
                cc: 'RU64',
                is_active: true,
                label:
                    '\u041e\u041e\u041e \u042f\u043d\u0434\u0435\u043a\u0441.\u0418\u0441\u043f\u044b\u0442\u0430\u043d\u0438\u044f'
            },
            {
                cc: 'RU65',
                is_active: true,
                label:
                    '\u041e\u041e\u041e \u0414\u0437\u0435\u043d.\u041f\u043b\u0430\u0442\u0444\u043e\u0440\u043c\u0430'
            },
            {
                cc: 'RU66',
                is_active: true,
                label: '\u041e\u041e\u041e \u041e\u043f\u0442\u0435\u0443\u043c'
            },
            {
                cc: 'RU67',
                is_active: true,
                label: '\u041e\u041e\u041e \u041f\u0430\u0440\u0442\u0438\u044f \u0415\u0434\u044b'
            },
            {
                cc: 'RU68',
                is_active: true,
                label:
                    '\u041e\u041e\u041e \u042f\u043d\u0434\u0435\u043a\u0441.\u0417\u0430\u043f\u0440\u0430\u0432\u043a\u0438'
            },
            {
                cc: 'RU69',
                is_active: true,
                label:
                    '\u041e\u041e\u041e \u042f\u043d\u0434\u0435\u043a\u0441 \u0411\u0435\u0441\u043f\u0438\u043b\u043e\u0442\u043d\u044b\u0435 \u0422\u0435\u0445\u043d\u043e\u043b\u043e\u0433\u0438\u0438'
            },
            {
                cc: 'RU70',
                is_active: true,
                label: '\u041e\u041e\u041e \u041a\u0438\u043d\u043e\u043f\u043e\u0438\u0441\u043a'
            },
            {
                cc: 'RU71',
                is_active: true,
                label:
                    '\u041e\u041e\u041e \u042f\u043d\u0434\u0435\u043a\u0441.\u041c\u0430\u0440\u043a\u0435\u0442 \u041b\u0430\u0431'
            },
            {
                cc: 'RU72',
                is_active: true,
                label:
                    '\u041e\u041e\u041e \u042f\u043d\u0434\u0435\u043a\u0441.\u041c\u0435\u0434\u0438\u0430\u043b\u0430\u0431'
            },
            {
                cc: 'RU73',
                is_active: true,
                label:
                    '\u041e\u041e\u041e \u042f\u043d\u0434\u0435\u043a\u0441.\u0421\u0442\u0443\u0434\u0438\u044f'
            },
            {
                cc: 'RU74',
                is_active: true,
                label:
                    '\u041e\u041e\u041e \u042f\u043d\u0434\u0435\u043a\u0441.\u041c\u0435\u0434\u0438\u0430\u0441\u0435\u0440\u0432\u0438\u0441\u044b'
            },
            {
                cc: 'RU80',
                is_active: true,
                label: '\u0410\u041d\u041e \u0414\u041f\u041e \u0428\u0410\u0414'
            },
            {
                cc: 'RU81',
                is_active: true,
                label:
                    '\u041e\u041e\u041e \u041a\u043b\u0438\u043d\u0438\u043a\u0430 \u042f\u043d\u0434\u0435\u043a\u0441.\u0417\u0434\u043e\u0440\u043e\u0432\u044c\u0435'
            },
            {
                cc: 'RU82',
                is_active: true,
                label:
                    '\u041e\u041e\u041e \u042f\u043d\u0434\u0435\u043a\u0441.\u041f\u0440\u043e\u0441\u0432\u0435\u0449\u0435\u043d\u0438\u0435'
            },
            {
                cc: 'RU83',
                is_active: true,
                label: '\u041e\u041e\u041e \u0417\u043d\u0430\u043d\u0438\u0435'
            },
            {
                cc: 'RU90',
                is_active: true,
                label:
                    '\u041e\u041e\u041e \u042f\u043d\u0434\u0435\u043a\u0441.\u041c\u0430\u0440\u043a\u0435\u0442'
            },
            {
                cc: 'RU91',
                is_active: true,
                label: '\u041e\u041e\u041e \u0422\u0435\u043b\u0435\u043e\u043d'
            },
            {
                cc: 'RU92',
                is_active: true,
                label: '\u041e\u041e\u041e \u0414\u0432\u0438\u0436\u0435\u043d\u0438\u0435'
            },
            {
                cc: 'RU93',
                is_active: true,
                label:
                    '\u041e\u041e\u041e \u041a\u043e\u043b\u043b\u0438-\u0421\u0438\u0431\u0438\u0440\u044c'
            },
            {
                cc: 'RU94',
                is_active: true,
                label:
                    '\u041e\u041e\u041e \u041a\u043b\u0435\u0432\u0435\u0440 \u0422\u043e\u043b\u044c\u044f\u0442\u0442\u0438'
            },
            {
                cc: 'RU95',
                is_active: true,
                label:
                    '\u041e\u041e\u041e \u0417\u0435\u043b\u0435\u043d\u0430\u044f \u041b\u0438\u043d\u0438\u044f \u0412\u043e\u043b\u0433\u043e\u0433\u0440\u0430\u0434'
            },
            {
                cc: 'RU96',
                is_active: true,
                label: '\u041e\u041e\u041e \u041b\u0438\u0434\u0435\u0440-\u0412'
            },
            {
                cc: 'RU97',
                is_active: true,
                label: '\u041e\u041e\u041e \u041a\u0440\u043e\u043d\u043e\u0441'
            },
            {
                cc: 'RU98',
                is_active: true,
                label: '\u041e\u041e\u041e \u042d\u043a\u0441\u043f\u0435\u0440\u0442'
            },
            { cc: 'TR10', is_active: true, label: ' Yandex Reklamcilik Hizmetleri LC' },
            {
                cc: 'UA10',
                is_active: true,
                label:
                    '\u041e\u041e\u041e \u042f\u043d\u0434\u0435\u043a\u0441.\u0423\u043a\u0440\u0430\u0438\u043d\u0430'
            },
            {
                cc: 'UA31',
                is_active: true,
                label:
                    '\u041e\u041e\u041e \u042f\u043d\u0434\u0435\u043a\u0441.\u0422\u0430\u043a\u0441\u0438 \u0423\u043a\u0440\u0430\u0438\u043d\u0430'
            },
            { cc: 'US10', is_active: true, label: 'Yandex Inc.' },
            { cc: 'US20', is_active: true, label: 'SPB Software Inc.' },
            { cc: '0000', is_active: true, label: 'N/A' }
        ]
    },
    manager: {
        request: [{ url: `${HOST}/manager`, data: { manager_code: '20869' } }],
        response: {
            parent_code: 20482,
            id: 20869,
            name:
                '\u0421\u0432\u0438\u043d\u0446\u0438\u0446\u043a\u0438\u0439 \u0410\u043d\u0434\u0440\u0435\u0439',
            parents_names:
                '["\\u042f\\u043d\\u0434\\u0435\\u043a\\u0441", "Search Portal", "\\u041a\\u043e\\u043c\\u043c\\u0435\\u0440\\u0447\\u0435\\u0441\\u043a\\u0438\\u0439 \\u0434\\u0435\\u043f\\u0430\\u0440\\u0442\\u0430\\u043c\\u0435\\u043d\\u0442", "\\u0423\\u043f\\u0440\\u0430\\u0432\\u043b\\u0435\\u043d\\u0438\\u0435 \\u0441\\u0442\\u0440\\u0430\\u0442\\u0435\\u0433\\u0438\\u0438 \\u0438 \\u0440\\u043e\\u0441\\u0442\\u0430", "\\u041f\\u043e\\u0434\\u0440\\u0430\\u0437\\u0434\\u0435\\u043b\\u0435\\u043d\\u0438\\u0435 \\u043f\\u043e \\u0432\\u0437\\u0430\\u0438\\u043c\\u043e\\u0434\\u0435\\u0439\\u0441\\u0442\\u0432\\u0438\\u044e \\u0441 \\u043f\\u0430\\u0440\\u0442\\u043d\\u0435\\u0440\\u0430\\u043c\\u0438 \\u0420\\u0421\\u042f \\u0438 \\u041f\\u043e\\u0438\\u0441\\u043a\\u0430", "The division of monetizing internal services"]'
        }
    },
    clientsList: {
        request: [
            `${HOST}/client/list`,
            {
                agency_select_policy: 'AGENCY',
                client_id: '5028445',
                email: 'v.krasko@netpeak.net',
                fax: '+1234567890',
                hide_managers: true,
                intercompany: 'AM31',
                is_accurate: true,
                login: 'netpeakru',
                manager_code: '20869',
                manual_suspect: true,
                name: 'Netpeak',
                pagination_pn: 1,
                pagination_ps: 10,
                phone: '+38 063 80 40 690',
                reliable_client: true,
                single_account_number: '123456',
                sort_key: 'DT',
                url: 'http://netpeak.ua/',
                with_invoices: 'true'
            },
            false,
            false
        ],
        response: {
            version: { 'yb-snout-api': '3.trunk.7436742' },
            data: { items: [], row_count: 0, total_count: 0 }
        }
    }
};
