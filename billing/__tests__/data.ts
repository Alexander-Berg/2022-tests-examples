import { HOST } from 'common/utils/test-utils/common';

export const act124864551 = [
    {
        request: [`${HOST}/act`, { act_id: '124864551', act_eid: undefined }, false, false],
        response: {
            version: { 'yb-snout-api': '3.59.2' },
            data: {
                export_dt: null,
                rows: [
                    {
                        consume: {
                            seqnum: 0,
                            unit_name: '\u0443.\u0435.',
                            discount_pct: '0.00',
                            order: {
                                text: 'Py_Test order 7-1475',
                                nds: 1,
                                service_order_id: 56765022,
                                id: 3432965081,
                                service: {
                                    cc: 'PPC',
                                    id: 7,
                                    name:
                                        '\u0414\u0438\u0440\u0435\u043a\u0442: \u0420\u0435\u043a\u043b\u0430\u043c\u043d\u044b\u0435 \u043a\u0430\u043c\u043f\u0430\u043d\u0438\u0438'
                                }
                            }
                        },
                        price: '30.0000',
                        amount: '1500.00',
                        manager: {
                            manager_code: 20453,
                            name:
                                '\u041f\u0435\u0440\u0430\u043d\u0438\u0434\u0437\u0435 \u041d\u0443\u0433\u0437\u0430\u0440 \u0413\u0435\u043e\u0440\u0433\u0438\u0435\u0432\u0438\u0447'
                        },
                        finish_sum: '1500.00',
                        netting: null,
                        consume_nds: null,
                        act_sum: '1500.00',
                        id: 767176363,
                        quantity: '50.000000'
                    }
                ],
                is_trp: null,
                oebs_exportable: true,
                memo: null,
                is_docs_detailed: false,
                factura: null,
                amount: '1500.00',
                good_debt: '1',
                invoice: {
                    firm: { id: 1 },
                    postpay: false,
                    receipt_sum_1c: '3000.00',
                    total_sum: '3000.00',
                    paysys: { id: 1003 },
                    currency: 'RUR',
                    person: {
                        id: 12344907,
                        name:
                            '\u041e\u041e\u041e "\u041f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a"'
                    },
                    client: {
                        id: 1338518283,
                        name: '\u041e\u041e\u041e "\u041a\u043b\u0438\u0435\u043d\u0442"'
                    },
                    iso_currency: 'RUB',
                    external_id: '\u0411-3310232277-1',
                    id: 118622227
                },
                is_docs_separated: false,
                id: 124864551,
                hidden: false,
                act_sum: '1500.00',
                external_id: '153733125',
                dt: '2020-08-31T00:00:00'
            }
        }
    },
    {
        request: [
            `${HOST}/user/object-permissions`,
            { classname: 'Act', object_id: 124864551 },
            false,
            false
        ],
        response: {
            version: { 'yb-snout-api': '3.59.2' },
            data: [
                {
                    code: 'TerminalView',
                    id: 56,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u0442\u0435\u0440\u043c\u0438\u043d\u0430\u043b\u0430'
                },
                {
                    code: 'EditINN',
                    id: 43,
                    name:
                        '\u0417\u0430\u0432\u0435\u0434\u0435\u043d\u0438\u0435 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u0430 \u0441 \u0415\u0413\u0420\u041f\u041e\u0423 \u0432\u043c\u0435\u0441\u0442\u043e \u0418\u041d\u041d'
                },
                {
                    code: 'TransferUnusedFunds',
                    id: 11121,
                    name:
                        '\u041f\u0435\u0440\u0435\u043d\u043e\u0441 \u0441\u0432\u043e\u0431\u043e\u0434\u043d\u044b\u0445 \u0441\u0440\u0435\u0434\u0441\u0442\u0432'
                },
                {
                    code: 'NotifyOrder',
                    id: 11281,
                    name:
                        '\u041e\u0442\u043f\u0440\u0430\u0432\u0438\u0442\u044c \u043d\u043e\u0442\u0438\u0444\u0438\u043a\u0430\u0446\u0438\u044e \u043f\u043e \u0437\u0430\u043a\u0430\u0437\u0443'
                },
                {
                    code: 'AlterFirmUATaxi',
                    id: 49,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b \u041e\u041e\u041e "\u042f\u043d\u0434\u0435\u043a\u0441.\u0422\u0430\u043a\u0441\u0438 \u0423\u043a\u0440\u0430\u0438\u043d\u0430"'
                },
                {
                    code: 'CloseInvoice',
                    id: 11222,
                    name:
                        '\u0417\u0430\u043a\u0440\u044b\u0442\u0438\u0435 \u0441\u0447\u0451\u0442\u0430'
                },
                {
                    code: 'RepresentClient',
                    id: 2000,
                    name:
                        '\u0414\u0435\u0439\u0441\u0442\u0432\u0438\u044f \u043e\u0442 \u0438\u043c\u0435\u043d\u0438 \u043a\u043b\u0438\u0435\u043d\u0442\u0430'
                },
                {
                    code: 'ViewDevData',
                    id: 11261,
                    name:
                        '\u0414\u043e\u0441\u0442\u0443\u043f \u043a \u0442\u0435\u0445\u043d\u0438\u0447\u0435\u0441\u043a\u043e\u0439 \u0438\u043d\u0444\u043e\u0440\u043c\u0430\u0446\u0438\u0438'
                },
                {
                    code: 'EDOViewer',
                    id: 60,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u0433\u0440\u0430\u0444\u0438\u043a\u0430 \u042d\u0414\u041e'
                },
                {
                    code: 'ClientFraudStatusEdit',
                    id: 58,
                    name:
                        '\u0420\u0435\u0434\u0430\u043a\u0442\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0435 \u043a\u043b\u0438\u0435\u043d\u0442\u0430: \u043f\u0440\u043e\u0441\u0442\u0430\u043d\u043e\u0432\u043a\u0430 \u0444\u043b\u0430\u0433\u0430 deny_cc / fraud_status'
                },
                {
                    code: 'SetClientOverdraftBan',
                    id: 11325,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0438\u0442\u044c \u0431\u0430\u043d \u043e\u0432\u0435\u0440\u0434\u0440\u0430\u0444\u0442\u0430 \u0434\u043b\u044f \u043a\u043b\u0438\u0435\u043d\u0442\u0430'
                },
                {
                    code: 'TransferBetweenClients',
                    id: 11143,
                    name:
                        '\u041f\u0435\u0440\u0435\u043d\u043e\u0441 \u0441\u0440\u0435\u0434\u0441\u0442\u0432 \u043c\u0435\u0436\u0434\u0443 \u0440\u0430\u0437\u043d\u044b\u043c\u0438 \u043a\u043b\u0438\u0435\u043d\u0442\u0430\u043c\u0438'
                },
                {
                    code: 'EditClient',
                    id: 28,
                    name:
                        '\u0420\u0435\u0434\u0430\u043a\u0442\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0435 \u043a\u043b\u0438\u0435\u043d\u0442\u0430'
                },
                {
                    code: 'ViewProduct',
                    id: 11241,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u043f\u0440\u043e\u0434\u0443\u043a\u0442\u043e\u0432'
                },
                {
                    code: 'AlterFirmHK',
                    id: 65,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b Yandex E-commerce Limited'
                },
                {
                    code: 'IssueInvoices',
                    id: 11106,
                    name:
                        '\u0412\u044b\u0441\u0442\u0430\u0432\u043b\u0435\u043d\u0438\u0435 \u0441\u0447\u0435\u0442\u043e\u0432'
                },
                {
                    code: 'CreateRequestsShop',
                    id: 11101,
                    name:
                        '\u0412\u044b\u0441\u0442\u0430\u0432\u043b\u0435\u043d\u0438\u0435 \u0441\u0447\u0435\u0442\u043e\u0432 \u0447\u0435\u0440\u0435\u0437 \u043c\u0430\u0433\u0430\u0437\u0438\u043d'
                },
                {
                    code: 'r_django_fraud_monitoring',
                    id: 62,
                    name:
                        '\u041f\u0440\u0430\u0432\u0430 \u043d\u0430 \u0447\u0442\u0435\u043d\u0438\u0435 Django Fraud Monitoring'
                },
                {
                    code: 'UseAdminPersons',
                    id: 11112,
                    name:
                        '\u0418\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u043d\u0438\u0435 \u043e\u0441\u043e\u0431\u044b\u0445 \u0442\u0438\u043f\u043e\u0432 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u043e\u0432'
                },
                {
                    code: 'OEBSReexportPerson',
                    id: 11225,
                    name:
                        '\u041f\u0435\u0440\u0435\u043f\u043e\u0441\u0442\u0430\u0432\u0438\u0442\u044c \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u0430 \u0432 \u043e\u0447\u0435\u0440\u0435\u0434\u044c \u044d\u043a\u0441\u043f\u043e\u0440\u0442\u0430 OEBS'
                },
                {
                    code: 'AlterFirmNVTaxi',
                    id: 48,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b Yandex.Taxi B.V.'
                },
                {
                    code: 'AdditionalFunctions',
                    id: 47,
                    name:
                        '\u0414\u043e\u043f\u043e\u043b\u043d\u0438\u0442\u0435\u043b\u044c\u043d\u044b\u0435 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438'
                },
                {
                    code: 'ChangeUnusedFundsLock',
                    id: 11227,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0438\u0442\u044c \u043f\u0440\u0438\u0447\u0438\u043d\u0443 \u0441\u043d\u044f\u0442\u0438\u044f \u0431\u0435\u0437\u0437\u0430\u043a\u0430\u0437\u044c\u044f \u0443 \u0441\u0447\u0451\u0442\u0430'
                },
                {
                    code: 'WithdrawConsumesPostpay',
                    id: 66,
                    name:
                        '\u0421\u043d\u044f\u0442\u0438\u0435 \u0437\u0430\u0447\u0438\u0441\u043b\u0435\u043d\u0438\u0439 \u0441 \u043f\u043e\u0441\u0442\u043e\u043f\u043b\u0430\u0442\u043d\u044b\u0445 \u0441\u0447\u0435\u0442\u043e\u0432'
                },
                {
                    code: 'WithdrawConsumesPrepay',
                    id: 67,
                    name:
                        '\u0421\u043d\u044f\u0442\u0438\u0435 \u0437\u0430\u0447\u0438\u0441\u043b\u0435\u043d\u0438\u0439 \u0441 \u043f\u0440\u0435\u0434\u043e\u043f\u043b\u0430\u0442\u043d\u044b\u0445 \u0441\u0447\u0435\u0442\u043e\u0432'
                },
                {
                    code: 'AlterFirmRU',
                    id: 31,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b \u041e\u041e\u041e \u00ab\u042f\u043d\u0434\u0435\u043a\u0441\u00bb'
                },
                {
                    code: 'AlterFirmNVTaxiuber',
                    id: 63,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b Uber ML B.V.'
                },
                {
                    code: 'AlterFirmAMTaxi',
                    id: 53,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b \u041e\u041e\u041e \u00ab\u042f\u043d\u0434\u0435\u043a\u0441.\u0422\u0430\u043a\u0441\u0438 \u0410\u0440\u043c\u0435\u043d\u0438\u044f\u00bb'
                },
                {
                    code: 'DoInvoiceRefunds',
                    id: 4004,
                    name:
                        '\u0421\u043e\u0437\u0434\u0430\u043d\u0438\u0435 \u0432\u043e\u0437\u0432\u0440\u0430\u0442\u043e\u0432 \u043f\u043e \u0441\u0447\u0435\u0442\u0430\u043c'
                },
                {
                    code: 'TerminalEdit',
                    id: 57,
                    name:
                        '\u0420\u0435\u0434\u0430\u043a\u0442\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0435 \u0442\u0435\u0440\u043c\u0438\u043d\u0430\u043b\u0430'
                },
                {
                    code: 'invoices_report__client',
                    id: 1002,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u0421\u0447\u0435\u0442\u0430 - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 ID \u043a\u043b\u0438\u0435\u043d\u0442\u0430'
                },
                {
                    code: 'AlterFirmKZTaxi',
                    id: 51,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b \u041e\u041e\u041e \u00ab\u042f\u043d\u0434\u0435\u043a\u0441.\u0422\u0430\u043a\u0441\u0438 \u041a\u0430\u0437\u0430\u0445\u0441\u0442\u0430\u043d\u00bb'
                },
                {
                    code: 'SetMcbFlags',
                    id: 11324,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0438\u0442\u044c \u043a\u0430\u0442\u0435\u0433\u043e\u0440\u0438\u0438 \u0434\u043b\u044f \u043f\u043b\u043e\u0449\u0430\u0434\u043e\u043a \u043a\u043b\u0438\u0435\u043d\u0442\u043e\u0432'
                },
                {
                    code: 'CreateBankPayments',
                    id: 3,
                    name:
                        '\u0412\u043d\u0435\u0441\u0435\u043d\u0438\u0435 \u043f\u043b\u0430\u0442\u0435\u0436\u0435\u0439 \u0447\u0435\u0440\u0435\u0437 \u0431\u0430\u043d\u043a'
                },
                {
                    code: 'ViewOrders',
                    id: 11201,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u0437\u0430\u043a\u0430\u0437\u043e\u0432'
                },
                {
                    code: 'ViewClients',
                    id: 11321,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u043a\u043b\u0438\u0435\u043d\u0442\u043e\u0432'
                },
                {
                    code: 'PersonExtEdit',
                    id: 46,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0432\u0430\u0436\u043d\u044b\u0445 \u043f\u043e\u043b\u0435\u0439 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u0430'
                },
                {
                    code: 'ViewContracts',
                    id: 11181,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u043e\u0432'
                },
                {
                    code: 'AlterAllInvoices',
                    id: 10,
                    name:
                        '[\u041d\u0435 \u0438\u0441\u043f\u043e\u043b\u044c\u0437\u0443\u0435\u0442\u0441\u044f]\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0441\u0443\u043c\u043c\u044b \u0441\u0447\u0435\u0442\u0430'
                },
                {
                    code: 'LocalNamesMaster',
                    id: 59,
                    name:
                        '\u0420\u0435\u0434\u0430\u043a\u0442\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0435 \u043b\u043e\u043a\u0430\u043b\u0438\u0437\u043e\u0432\u0430\u043d\u043d\u044b\u0445 \u0430\u0442\u0440\u0438\u0431\u0443\u0442\u043e\u0432 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u043e\u0432'
                },
                {
                    code: 'UseRestrictedINN',
                    id: 11301,
                    name:
                        '\u0418\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u043d\u0438\u0435 \u0437\u0430\u043f\u0440\u0435\u0449\u0451\u043d\u043d\u044b\u0445 \u0418\u041d\u041d'
                },
                {
                    code: 'AlterPrintTemplate',
                    id: 55,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0448\u0430\u0431\u043b\u043e\u043d\u0430 \u043f\u0435\u0447\u0430\u0442\u043d\u043e\u0439 \u0444\u043e\u0440\u043c\u044b \u0432 \u0434\u043e\u0433\u043e\u0432\u0440\u0435'
                },
                {
                    code: 'SendActs',
                    id: 11323,
                    name:
                        '\u041e\u0442\u043f\u0440\u0430\u0432\u0438\u0442\u044c \u0430\u043a\u0442\u044b \u043f\u043e \u044d\u043b\u0435\u043a\u0442\u0440\u043e\u043d\u043d\u043e\u0439 \u0438\u043b\u0438 \u043e\u0431\u044b\u043d\u043e\u0439 \u043f\u043e\u0447\u0442\u0435'
                },
                {
                    code: 'CreateClient',
                    id: 27,
                    name:
                        '\u0421\u043e\u0437\u0434\u0430\u043d\u0438\u0435 \u043a\u043b\u0438\u0435\u043d\u0442\u0430'
                },
                {
                    code: 'ViewPersons',
                    id: 11322,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u043e\u0432'
                },
                {
                    code: 'deferpays_orders_report__invoice_eid',
                    id: 1013,
                    name:
                        '\u041e\u0442\u0447\u0435\u0442 \u041a\u0440\u0435\u0434\u0438\u0442\u044b - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u0424\u0438\u043a\u0442\u0438\u0432\u043d\u044b\u0439 \u0441\u0447\u0435\u0442'
                },
                {
                    code: 'AlterFirmUA',
                    id: 32,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b \u041e\u041e\u041e \u00ab\u042f\u043d\u0434\u0435\u043a\u0441.\u0423\u043a\u0440\u0430\u0438\u043d\u0430\u00bb'
                },
                {
                    code: 'GrantAllPermissionsToAll',
                    id: 7,
                    name:
                        '\u041d\u0430\u0437\u043d\u0430\u0447\u0435\u043d\u0438\u0435 \u043f\u043e\u043b\u043d\u043e\u043c\u043e\u0447\u0438\u0439'
                },
                {
                    code: 'consumes_list__consume_qty',
                    id: 1010,
                    name:
                        '\u0421\u043f\u0438\u0441\u043e\u043a \u0437\u0430\u044f\u0432\u043e\u043a - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u041f\u0435\u0440\u0432\u043e\u043d\u0430\u0447\u0430\u043b\u044c\u043d\u043e\u0435 \u043a\u043e\u043b\u0438\u0447\u0435\u0441\u0442\u0432\u043e'
                },
                {
                    code: 'ModifyDiscounts',
                    id: 16,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0441\u043a\u0438\u0434\u043e\u043a'
                },
                {
                    code: 'PayInvoiceExtension',
                    id: 11221,
                    name:
                        '\u041f\u0435\u0440\u0435\u043d\u0435\u0441\u0442\u0438 \u0441\u0440\u043e\u043a \u043e\u043f\u043b\u0430\u0442\u044b \u0441\u0447\u0451\u0442\u0430'
                },
                {
                    code: 'AssistPostauth',
                    id: 11,
                    name:
                        '\u0423\u043f\u0440\u0430\u0432\u043b\u0435\u043d\u0438\u0435 \u043f\u043e\u0441\u0442\u0430\u0432\u0442\u043e\u0440\u0438\u0437\u0430\u0446\u0438\u0435\u0439'
                },
                {
                    code: 'ViewPayments',
                    id: 11326,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u043f\u043b\u0430\u0442\u0435\u0436\u0435\u0439'
                },
                {
                    code: 'TransferFromInvoice',
                    id: 11142,
                    name:
                        '\u041f\u0435\u0440\u0435\u043d\u043e\u0441 \u0441\u0440\u0435\u0434\u0441\u0442\u0432 \u0441\u043e \u0441\u0447\u0435\u0442\u0430'
                },
                {
                    code: 'BillingSupport',
                    id: 1100,
                    name:
                        '\u0421\u0430\u043f\u043f\u043e\u0440\u0442 \u0411\u0438\u043b\u043b\u0438\u043d\u0433\u0430'
                },
                {
                    code: 'PaymentLinksReport',
                    id: 11104,
                    name:
                        '\u041f\u043e\u0438\u0441\u043a \u043f\u043e \u043f\u043b\u0430\u0442\u0435\u0436\u0430\u043c'
                },
                {
                    code: 'ui_orders_report__manager_code',
                    id: 1009,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u0417\u0430\u043a\u0430\u0437\u044b \u0432 \u041f\u0418 - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u041c\u0435\u043d\u0435\u0434\u0436\u0435\u0440'
                },
                {
                    code: 'IssueUSDInvoices',
                    id: 13,
                    name:
                        '\u0412\u044b\u0441\u0442\u0430\u0432\u043b\u044f\u0442\u044c \u0434\u043e\u043b\u043b\u0430\u0440\u043e\u0432\u044b\u0435 \u0441\u0447\u0435\u0442\u0430'
                },
                {
                    code: 'ManageBadDebts',
                    id: 21,
                    name:
                        '\u041f\u0440\u0438\u0437\u043d\u0430\u043d\u0438\u0435 "\u043f\u043b\u043e\u0445\u043e\u0433\u043e" \u0434\u043e\u043b\u0433\u0430'
                },
                {
                    code: 'IssueAnyInvoices',
                    id: 17,
                    name:
                        '\u0414\u043e\u0432\u044b\u0441\u0442\u0430\u0432\u043b\u0435\u043d\u0438\u0435 \u0441\u0447\u0435\u0442\u0430 \u0437\u0430 \u043a\u043e\u0433\u043e \u0443\u0433\u043e\u0434\u043d\u043e'
                },
                {
                    code: 'ManageRBSPayments',
                    id: 19,
                    name:
                        '\u041e\u043f\u0435\u0440\u0430\u0446\u0438\u0438 \u0441 \u043f\u043b\u0430\u0442\u0435\u0436\u0430\u043c\u0438 \u043f\u043e RBS'
                },
                {
                    code: 'consumes_list__consume_sum',
                    id: 1011,
                    name:
                        '\u0421\u043f\u0438\u0441\u043e\u043a \u0437\u0430\u044f\u0432\u043e\u043a - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u041f\u0435\u0440\u0432\u043e\u043d\u0430\u0447\u0430\u043b\u044c\u043d\u0430\u044f \u0441\u0443\u043c\u043c\u0430'
                },
                {
                    code: 'orders_report__client_id',
                    id: 1005,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u0417\u0430\u043a\u0430\u0437\u044b - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 ID \u043a\u043b\u0438\u0435\u043d\u0442\u0430'
                },
                {
                    code: 'PersonView',
                    id: 25,
                    name:
                        '\u0423\u0441\u0442\u0430\u0440\u0435\u0432\u0448\u0435\u0435, \u043d\u0435 \u0438\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u0442\u044c. \u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u0430.'
                },
                { code: 'Reversal', id: 9, name: 'Reversal' },
                {
                    code: 'AlterFirmUS',
                    id: 34,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b Yandex Inc'
                },
                {
                    code: 'SendInvoices',
                    id: 11102,
                    name:
                        '\u041e\u0442\u043f\u0440\u0430\u0432\u043a\u0430 \u0441\u0447\u0435\u0442\u043e\u0432 \u043d\u0430 \u043f\u043e\u0447\u0442\u0443'
                },
                {
                    code: 'invoices_report__manager',
                    id: 1007,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u0421\u0447\u0435\u0442\u0430 - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u0412\u043a\u043b\u044e\u0447\u0435\u043d\u0438\u0435 \u0437\u0430\u043a\u0430\u0437\u0430'
                },
                {
                    code: 'AlterInvoicePaysys',
                    id: 11109,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0441\u043f\u043e\u0441\u043e\u0431\u0430 \u043e\u043f\u043b\u0430\u0442\u044b \u0441\u0447\u0435\u0442\u0430'
                },
                {
                    code: 'ChangeRepaymentsStatus',
                    id: 11103,
                    name:
                        '\u041f\u043e\u0434\u0442\u0432\u0435\u0440\u0436\u0434\u0435\u043d\u0438\u0435 \u0441\u0447\u0435\u0442\u043e\u0432 \u043d\u0430 \u043f\u043e\u0433\u0430\u0448\u0435\u043d\u0438\u0435'
                },
                {
                    code: 'ViewAllInvoices',
                    id: 1,
                    name:
                        '\u0423\u0441\u0442\u0430\u0440\u0435\u0432\u0448\u0435\u0435, \u043d\u0435 \u0438\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u0442\u044c. \u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u0441\u0447\u0435\u0442\u043e\u0432.'
                },
                {
                    code: 'ApexReports',
                    id: 3001,
                    name: '\u0410\u0440\u0435\u0445. \u041e\u0442\u0447\u0435\u0442\u044b'
                },
                {
                    code: 'AlterInvoicePerson',
                    id: 11110,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u0430 \u0441\u0447\u0435\u0442\u0430'
                },
                {
                    code: 'ManagersOperations',
                    id: 23,
                    name:
                        '\u0414\u0435\u0439\u0441\u0442\u0432\u0438\u044f \u043c\u0435\u043d\u0435\u0434\u0436\u0435\u0440\u0430'
                },
                {
                    code: 'OEBSReexportInvoice',
                    id: 11223,
                    name:
                        '\u041f\u0435\u0440\u0435\u043f\u043e\u0441\u0442\u0430\u0432\u0438\u0442\u044c \u0441\u0447\u0451\u0442 \u0432 \u043e\u0447\u0435\u0440\u0435\u0434\u044c \u044d\u043a\u0441\u043f\u043e\u0440\u0442\u0430 OEBS'
                },
                {
                    code: 'AlterInvoiceContract',
                    id: 11111,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0430 \u0441\u0447\u0435\u0442\u0430'
                },
                {
                    code: 'IssueYTInvoices',
                    id: 14,
                    name:
                        '\u0412\u044b\u0441\u0442\u0430\u0432\u043b\u044f\u0442\u044c \u0441\u0447\u0435\u0442\u0430 \u043d\u0430 Yandex Technologies'
                },
                {
                    code: 'AlterFirmTR',
                    id: 41,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b Yandex Turkey'
                },
                {
                    code: 'AlterInvoiceSum',
                    id: 11107,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0441\u0443\u043c\u043c\u044b \u0441\u0447\u0435\u0442\u0430'
                },
                {
                    code: 'CreateCerificatePayments',
                    id: 4,
                    name:
                        '\u0412\u043d\u0435\u0441\u0435\u043d\u0438\u0435 \u043f\u043b\u0430\u0442\u0435\u0436\u0435\u0439 \u0441\u0435\u0440\u0442\u0438\u0444\u0438\u043a\u0430\u0442\u043e\u043c, \u0431\u0430\u0440\u0442\u0435\u0440\u043e\u043c'
                },
                {
                    code: 'assist_payments_report__fraud_dt',
                    id: 1004,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u041f\u043b\u0430\u0442\u0435\u0436\u0438 \u043f\u043e Assist - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u0414\u0430\u0442\u0430 \u043e\u0431\u043d\u0430\u0440\u0443\u0436\u0435\u043d\u0438\u044f \u0444\u0440\u043e\u0434\u0430'
                },
                {
                    code: 'ViewInvoices',
                    id: 11105,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u0441\u0447\u0435\u0442\u043e\u0432'
                },
                {
                    code: 'PersonPostAddressEdit',
                    id: 38,
                    name:
                        '\u0420\u0435\u0434\u0430\u043a\u0442\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0435 \u043b\u044e\u0431\u044b\u0445 \u043f\u043e\u043b\u0435\u0439 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u0430'
                },
                {
                    code: 'invoices_report__manager_info',
                    id: 1006,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u0421\u0447\u0435\u0442\u0430 - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u041c\u0435\u043d\u0435\u0434\u0436\u0435\u0440'
                },
                {
                    code: 'AlterFirmSW',
                    id: 39,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b Yandex Europe AG'
                },
                {
                    code: 'DoInvoiceRefundsTrust',
                    id: 11161,
                    name:
                        '\u0421\u043e\u0437\u0434\u0430\u043d\u0438\u0435 \u0432\u043e\u0437\u0432\u0440\u0430\u0442\u043e\u0432 \u043f\u043e \u0441\u0447\u0435\u0442\u0430\u043c \u0447\u0435\u0440\u0435\u0437 \u0422\u0440\u0430\u0441\u0442'
                },
                {
                    code: 'AdminAccess',
                    id: 0,
                    name:
                        '\u0414\u043e\u0441\u0442\u0443\u043f \u043a \u0430\u0434\u043c\u0438\u043d\u0438\u0441\u0442\u0440\u0430\u0442\u0438\u0432\u043d\u043e\u043c\u0443 \u0440\u0435\u0436\u0438\u043c\u0443'
                },
                {
                    code: 'AlterFirmKZ',
                    id: 33,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b \u0422\u041e\u041e \u00abKazNet Media (\u041a\u0430\u0437\u041d\u0435\u0442 \u041c\u0435\u0434\u0438\u0430)\u00bb'
                },
                {
                    code: 'ViewAllManagerReports',
                    id: 12,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442\u044b \u0434\u043b\u044f \u043c\u0435\u043d\u0435\u0434\u0436\u0435\u0440\u043e\u0432'
                },
                {
                    code: 'PersonEdit',
                    id: 26,
                    name:
                        '\u0420\u0435\u0434\u0430\u043a\u0442\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0435 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u0430'
                },
                {
                    code: 'order_info__order_id',
                    id: 1012,
                    name:
                        '\u0421\u0432\u043e\u0439\u0441\u0442\u0432\u0430 \u0437\u0430\u043a\u0430\u0437\u0430 - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u0412\u043d\u0443\u0442\u0440\u0435\u043d\u043d\u0438\u0439 ID \u0437\u0430\u043a\u0430\u0437\u0430'
                },
                {
                    code: 'ui_orders_report__client_id',
                    id: 1008,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u0417\u0430\u043a\u0430\u0437\u044b \u0432 \u041f\u0418 - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 ID \u043a\u043b\u0438\u0435\u043d\u0442\u0430'
                },
                {
                    code: 'ViewFishes',
                    id: 22,
                    name: '\u0424\u0438\u0448\u043a\u043e\u0432\u0438\u0434\u0435\u0446'
                },
                {
                    code: 'assist_payments_report__postauth_status',
                    id: 1003,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u041f\u043b\u0430\u0442\u0435\u0436\u0438 \u043f\u043e Assist - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u0421\u0442\u0430\u0442\u0443\u0441 \u043f\u043e\u0441\u0442-\u0430\u0432\u0442\u043e\u0440\u0438\u0437\u0430\u0446\u0438\u0438'
                },
                {
                    code: 'DealPassport',
                    id: 30,
                    name: '\u0420\u0430\u0431\u043e\u0442\u0430 \u0441 \u041f\u0421'
                },
                {
                    code: 'EditContracts',
                    id: 20,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u043e\u0432'
                },
                {
                    code: 'invoices_report__receipt_sum_1c',
                    id: 1001,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u0421\u0447\u0435\u0442\u0430 - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u0421\u0443\u043c\u043c\u0430 \u043f\u0440\u0438\u0445\u043e\u0434\u0430 \u0438\u0437 1\u0421'
                },
                {
                    code: 'SubstituteLogin',
                    id: 24,
                    name:
                        '\u041f\u043e\u0434\u043c\u0435\u043d\u0430 \u043b\u043e\u0433\u0438\u043d\u0430'
                },
                {
                    code: 'ViewReports',
                    id: 2,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u043e\u0442\u0447\u0435\u0442\u043e\u0432'
                },
                {
                    code: 'AlterFirmAUTORU',
                    id: 45,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b \u041e\u041e\u041e \u00ab\u0410\u0412\u0422\u041e.\u0420\u0423 \u0425\u043e\u043b\u0434\u0438\u043d\u0433\u00bb'
                },
                {
                    code: 'AlterFirmEU',
                    id: 37,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b Yandex Europe B.V.'
                },
                {
                    code: 'ProcessAllInvoices',
                    id: 8,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0441\u0447\u0435\u0442\u043e\u0432'
                },
                {
                    code: 'RecalculateOverdraft',
                    id: 42,
                    name:
                        '\u041f\u0435\u0440\u0435\u0440\u0430\u0441\u0447\u0435\u0442 \u043e\u0432\u0435\u0440\u0434\u0440\u0430\u0444\u0442\u0430'
                },
                {
                    code: 'SuperVisor',
                    id: 15,
                    name:
                        '\u0423\u0441\u0442\u0430\u0440\u0435\u0432\u0448\u0435\u0435, \u043d\u0435 \u0438\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u0442\u044c. SuperWisard'
                },
                {
                    code: 'TransferFromOrder',
                    id: 11141,
                    name:
                        '\u041f\u0435\u0440\u0435\u043d\u043e\u0441 \u0441\u0440\u0435\u0434\u0441\u0442\u0432 c \u0437\u0430\u043a\u0430\u0437\u0430'
                },
                {
                    code: 'AlterInvoiceDate',
                    id: 11108,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u0430\u0442\u044b \u0441\u0447\u0435\u0442\u0430'
                },
                {
                    code: 'OEBSReexportContract',
                    id: 11226,
                    name:
                        '\u041f\u0435\u0440\u0435\u043f\u043e\u0441\u0442\u0430\u0432\u0438\u0442\u044c \u0434\u043e\u0433\u043e\u0432\u043e\u0440/\u0434\u043e\u043f.\u0441\u043e\u0433\u043b\u0430\u0448\u0435\u043d\u0438\u0435 \u0432 \u043e\u0447\u0435\u0440\u0435\u0434\u044c \u044d\u043a\u0441\u043f\u043e\u0440\u0442\u0430 OEBS'
                }
            ]
        }
    },
    {
        request: [
            `${HOST}/export/get-state`,
            {
                classname: 'Act',
                queue_type: 'OEBS',
                object_id: 124864551
            },
            false,
            false
        ],
        response: {
            version: { 'yb-snout-api': '3.trunk.7228044' },
            data: { export_dt: null, state: 'WAITING' }
        }
    }
];

export const act124863520 = [
    {
        request: [`${HOST}/act`, { act_id: '124863520', act_eid: undefined }, false, false],
        response: {
            version: { 'yb-snout-api': '3.59.2' },
            data: {
                export_dt: null,
                rows: [
                    {
                        consume: {
                            seqnum: 0,
                            unit_name: '\u0443.\u0435.',
                            discount_pct: '0.00',
                            order: {
                                text: 'Py_Test order 7-1475',
                                nds: 1,
                                service_order_id: 56763978,
                                id: 3432957278,
                                service: {
                                    cc: 'PPC',
                                    id: 7,
                                    name:
                                        '\u0414\u0438\u0440\u0435\u043a\u0442: \u0420\u0435\u043a\u043b\u0430\u043c\u043d\u044b\u0435 \u043a\u0430\u043c\u043f\u0430\u043d\u0438\u0438'
                                }
                            }
                        },
                        price: '30.0000',
                        amount: '1500.00',
                        manager: {
                            manager_code: 20453,
                            name:
                                '\u041f\u0435\u0440\u0430\u043d\u0438\u0434\u0437\u0435 \u041d\u0443\u0433\u0437\u0430\u0440 \u0413\u0435\u043e\u0440\u0433\u0438\u0435\u0432\u0438\u0447'
                        },
                        finish_sum: '1500.00',
                        netting: null,
                        consume_nds: null,
                        act_sum: '1500.00',
                        id: 767166297,
                        quantity: '50.000000'
                    }
                ],
                is_trp: true,
                oebs_exportable: true,
                memo: null,
                is_docs_detailed: false,
                factura: null,
                amount: '1500.00',
                good_debt: '0',
                invoice: {
                    firm: { id: 1 },
                    postpay: false,
                    receipt_sum_1c: '3000.00',
                    total_sum: '3000.00',
                    paysys: { id: 1003 },
                    currency: 'RUR',
                    person: {
                        id: 12342769,
                        name:
                            '\u041e\u041e\u041e "\u041f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a"'
                    },
                    client: {
                        id: 1338514760,
                        name: '\u041e\u041e\u041e "\u041a\u043b\u0438\u0435\u043d\u0442"'
                    },
                    iso_currency: 'RUB',
                    external_id: '\u0411-3310227498-1',
                    id: 118620115
                },
                is_docs_separated: false,
                id: 124863520,
                hidden: false,
                act_sum: '1500.00',
                external_id: '153731994',
                dt: '2020-08-31T00:00:00'
            }
        }
    },
    {
        request: [
            `${HOST}/user/object-permissions`,
            { classname: 'Act', object_id: 124863520 },
            false,
            false
        ],
        response: {
            version: { 'yb-snout-api': '3.59.2' },
            data: [
                {
                    code: 'consumes_list__consume_qty',
                    id: 1010,
                    name:
                        '\u0421\u043f\u0438\u0441\u043e\u043a \u0437\u0430\u044f\u0432\u043e\u043a - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u041f\u0435\u0440\u0432\u043e\u043d\u0430\u0447\u0430\u043b\u044c\u043d\u043e\u0435 \u043a\u043e\u043b\u0438\u0447\u0435\u0441\u0442\u0432\u043e'
                },
                {
                    code: 'SendActs',
                    id: 11323,
                    name:
                        '\u041e\u0442\u043f\u0440\u0430\u0432\u0438\u0442\u044c \u0430\u043a\u0442\u044b \u043f\u043e \u044d\u043b\u0435\u043a\u0442\u0440\u043e\u043d\u043d\u043e\u0439 \u0438\u043b\u0438 \u043e\u0431\u044b\u043d\u043e\u0439 \u043f\u043e\u0447\u0442\u0435'
                },
                {
                    code: 'invoices_report__client',
                    id: 1002,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u0421\u0447\u0435\u0442\u0430 - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 ID \u043a\u043b\u0438\u0435\u043d\u0442\u0430'
                },
                {
                    code: 'SubstituteLogin',
                    id: 24,
                    name:
                        '\u041f\u043e\u0434\u043c\u0435\u043d\u0430 \u043b\u043e\u0433\u0438\u043d\u0430'
                },
                {
                    code: 'IssueAnyInvoices',
                    id: 17,
                    name:
                        '\u0414\u043e\u0432\u044b\u0441\u0442\u0430\u0432\u043b\u0435\u043d\u0438\u0435 \u0441\u0447\u0435\u0442\u0430 \u0437\u0430 \u043a\u043e\u0433\u043e \u0443\u0433\u043e\u0434\u043d\u043e'
                },
                {
                    code: 'ViewFishes',
                    id: 22,
                    name: '\u0424\u0438\u0448\u043a\u043e\u0432\u0438\u0434\u0435\u0446'
                },
                {
                    code: 'ViewAllManagerReports',
                    id: 12,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442\u044b \u0434\u043b\u044f \u043c\u0435\u043d\u0435\u0434\u0436\u0435\u0440\u043e\u0432'
                },
                {
                    code: 'ViewPersons',
                    id: 11322,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u043e\u0432'
                },
                {
                    code: 'UseAdminPersons',
                    id: 11112,
                    name:
                        '\u0418\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u043d\u0438\u0435 \u043e\u0441\u043e\u0431\u044b\u0445 \u0442\u0438\u043f\u043e\u0432 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u043e\u0432'
                },
                {
                    code: 'AlterFirmAUTORU',
                    id: 45,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b \u041e\u041e\u041e \u00ab\u0410\u0412\u0422\u041e.\u0420\u0423 \u0425\u043e\u043b\u0434\u0438\u043d\u0433\u00bb'
                },
                {
                    code: 'invoices_report__receipt_sum_1c',
                    id: 1001,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u0421\u0447\u0435\u0442\u0430 - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u0421\u0443\u043c\u043c\u0430 \u043f\u0440\u0438\u0445\u043e\u0434\u0430 \u0438\u0437 1\u0421'
                },
                {
                    code: 'ViewProduct',
                    id: 11241,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u043f\u0440\u043e\u0434\u0443\u043a\u0442\u043e\u0432'
                },
                {
                    code: 'PersonEdit',
                    id: 26,
                    name:
                        '\u0420\u0435\u0434\u0430\u043a\u0442\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0435 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u0430'
                },
                {
                    code: 'deferpays_orders_report__invoice_eid',
                    id: 1013,
                    name:
                        '\u041e\u0442\u0447\u0435\u0442 \u041a\u0440\u0435\u0434\u0438\u0442\u044b - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u0424\u0438\u043a\u0442\u0438\u0432\u043d\u044b\u0439 \u0441\u0447\u0435\u0442'
                },
                {
                    code: 'IssueYTInvoices',
                    id: 14,
                    name:
                        '\u0412\u044b\u0441\u0442\u0430\u0432\u043b\u044f\u0442\u044c \u0441\u0447\u0435\u0442\u0430 \u043d\u0430 Yandex Technologies'
                },
                {
                    code: 'BillingSupport',
                    id: 1100,
                    name:
                        '\u0421\u0430\u043f\u043f\u043e\u0440\u0442 \u0411\u0438\u043b\u043b\u0438\u043d\u0433\u0430'
                },
                {
                    code: 'order_info__order_id',
                    id: 1012,
                    name:
                        '\u0421\u0432\u043e\u0439\u0441\u0442\u0432\u0430 \u0437\u0430\u043a\u0430\u0437\u0430 - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u0412\u043d\u0443\u0442\u0440\u0435\u043d\u043d\u0438\u0439 ID \u0437\u0430\u043a\u0430\u0437\u0430'
                },
                {
                    code: 'AssistPostauth',
                    id: 11,
                    name:
                        '\u0423\u043f\u0440\u0430\u0432\u043b\u0435\u043d\u0438\u0435 \u043f\u043e\u0441\u0442\u0430\u0432\u0442\u043e\u0440\u0438\u0437\u0430\u0446\u0438\u0435\u0439'
                },
                {
                    code: 'RepresentClient',
                    id: 2000,
                    name:
                        '\u0414\u0435\u0439\u0441\u0442\u0432\u0438\u044f \u043e\u0442 \u0438\u043c\u0435\u043d\u0438 \u043a\u043b\u0438\u0435\u043d\u0442\u0430'
                },
                {
                    code: 'PersonPostAddressEdit',
                    id: 38,
                    name:
                        '\u0420\u0435\u0434\u0430\u043a\u0442\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0435 \u043b\u044e\u0431\u044b\u0445 \u043f\u043e\u043b\u0435\u0439 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u0430'
                },
                {
                    code: 'NotifyOrder',
                    id: 11281,
                    name:
                        '\u041e\u0442\u043f\u0440\u0430\u0432\u0438\u0442\u044c \u043d\u043e\u0442\u0438\u0444\u0438\u043a\u0430\u0446\u0438\u044e \u043f\u043e \u0437\u0430\u043a\u0430\u0437\u0443'
                },
                {
                    code: 'IssueUSDInvoices',
                    id: 13,
                    name:
                        '\u0412\u044b\u0441\u0442\u0430\u0432\u043b\u044f\u0442\u044c \u0434\u043e\u043b\u043b\u0430\u0440\u043e\u0432\u044b\u0435 \u0441\u0447\u0435\u0442\u0430'
                },
                {
                    code: 'AlterFirmNVTaxi',
                    id: 48,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b Yandex.Taxi B.V.'
                },
                {
                    code: 'ManagersOperations',
                    id: 23,
                    name:
                        '\u0414\u0435\u0439\u0441\u0442\u0432\u0438\u044f \u043c\u0435\u043d\u0435\u0434\u0436\u0435\u0440\u0430'
                },
                {
                    code: 'CreateBankPayments',
                    id: 3,
                    name:
                        '\u0412\u043d\u0435\u0441\u0435\u043d\u0438\u0435 \u043f\u043b\u0430\u0442\u0435\u0436\u0435\u0439 \u0447\u0435\u0440\u0435\u0437 \u0431\u0430\u043d\u043a'
                },
                {
                    code: 'ViewDevData',
                    id: 11261,
                    name:
                        '\u0414\u043e\u0441\u0442\u0443\u043f \u043a \u0442\u0435\u0445\u043d\u0438\u0447\u0435\u0441\u043a\u043e\u0439 \u0438\u043d\u0444\u043e\u0440\u043c\u0430\u0446\u0438\u0438'
                },
                {
                    code: 'consumes_list__consume_sum',
                    id: 1011,
                    name:
                        '\u0421\u043f\u0438\u0441\u043e\u043a \u0437\u0430\u044f\u0432\u043e\u043a - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u041f\u0435\u0440\u0432\u043e\u043d\u0430\u0447\u0430\u043b\u044c\u043d\u0430\u044f \u0441\u0443\u043c\u043c\u0430'
                },
                {
                    code: 'ModifyDiscounts',
                    id: 16,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0441\u043a\u0438\u0434\u043e\u043a'
                },
                {
                    code: 'SuperVisor',
                    id: 15,
                    name:
                        '\u0423\u0441\u0442\u0430\u0440\u0435\u0432\u0448\u0435\u0435, \u043d\u0435 \u0438\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u0442\u044c. SuperWisard'
                },
                {
                    code: 'assist_payments_report__fraud_dt',
                    id: 1004,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u041f\u043b\u0430\u0442\u0435\u0436\u0438 \u043f\u043e Assist - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u0414\u0430\u0442\u0430 \u043e\u0431\u043d\u0430\u0440\u0443\u0436\u0435\u043d\u0438\u044f \u0444\u0440\u043e\u0434\u0430'
                },
                {
                    code: 'PersonExtEdit',
                    id: 46,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0432\u0430\u0436\u043d\u044b\u0445 \u043f\u043e\u043b\u0435\u0439 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u0430'
                },
                {
                    code: 'EditContracts',
                    id: 20,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u043e\u0432'
                },
                {
                    code: 'assist_payments_report__postauth_status',
                    id: 1003,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u041f\u043b\u0430\u0442\u0435\u0436\u0438 \u043f\u043e Assist - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u0421\u0442\u0430\u0442\u0443\u0441 \u043f\u043e\u0441\u0442-\u0430\u0432\u0442\u043e\u0440\u0438\u0437\u0430\u0446\u0438\u0438'
                },
                { code: 'Reversal', id: 9, name: 'Reversal' },
                {
                    code: 'AlterFirmUATaxi',
                    id: 49,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b \u041e\u041e\u041e "\u042f\u043d\u0434\u0435\u043a\u0441.\u0422\u0430\u043a\u0441\u0438 \u0423\u043a\u0440\u0430\u0438\u043d\u0430"'
                },
                {
                    code: 'orders_report__client_id',
                    id: 1005,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u0417\u0430\u043a\u0430\u0437\u044b - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 ID \u043a\u043b\u0438\u0435\u043d\u0442\u0430'
                },
                {
                    code: 'AlterAllInvoices',
                    id: 10,
                    name:
                        '[\u041d\u0435 \u0438\u0441\u043f\u043e\u043b\u044c\u0437\u0443\u0435\u0442\u0441\u044f]\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0441\u0443\u043c\u043c\u044b \u0441\u0447\u0435\u0442\u0430'
                },
                {
                    code: 'ui_orders_report__client_id',
                    id: 1008,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u0417\u0430\u043a\u0430\u0437\u044b \u0432 \u041f\u0418 - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 ID \u043a\u043b\u0438\u0435\u043d\u0442\u0430'
                },
                {
                    code: 'SendInvoices',
                    id: 11102,
                    name:
                        '\u041e\u0442\u043f\u0440\u0430\u0432\u043a\u0430 \u0441\u0447\u0435\u0442\u043e\u0432 \u043d\u0430 \u043f\u043e\u0447\u0442\u0443'
                },
                {
                    code: 'ViewOrders',
                    id: 11201,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u0437\u0430\u043a\u0430\u0437\u043e\u0432'
                },
                {
                    code: 'AlterFirmAMTaxi',
                    id: 53,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b \u041e\u041e\u041e \u00ab\u042f\u043d\u0434\u0435\u043a\u0441.\u0422\u0430\u043a\u0441\u0438 \u0410\u0440\u043c\u0435\u043d\u0438\u044f\u00bb'
                },
                {
                    code: 'PersonView',
                    id: 25,
                    name:
                        '\u0423\u0441\u0442\u0430\u0440\u0435\u0432\u0448\u0435\u0435, \u043d\u0435 \u0438\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u0442\u044c. \u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u0430.'
                },
                {
                    code: 'CreateCerificatePayments',
                    id: 4,
                    name:
                        '\u0412\u043d\u0435\u0441\u0435\u043d\u0438\u0435 \u043f\u043b\u0430\u0442\u0435\u0436\u0435\u0439 \u0441\u0435\u0440\u0442\u0438\u0444\u0438\u043a\u0430\u0442\u043e\u043c, \u0431\u0430\u0440\u0442\u0435\u0440\u043e\u043c'
                },
                {
                    code: 'EditClient',
                    id: 28,
                    name:
                        '\u0420\u0435\u0434\u0430\u043a\u0442\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0435 \u043a\u043b\u0438\u0435\u043d\u0442\u0430'
                },
                {
                    code: 'SetMcbFlags',
                    id: 11324,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0438\u0442\u044c \u043a\u0430\u0442\u0435\u0433\u043e\u0440\u0438\u0438 \u0434\u043b\u044f \u043f\u043b\u043e\u0449\u0430\u0434\u043e\u043a \u043a\u043b\u0438\u0435\u043d\u0442\u043e\u0432'
                },
                {
                    code: 'ViewReports',
                    id: 2,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u043e\u0442\u0447\u0435\u0442\u043e\u0432'
                },
                {
                    code: 'AlterFirmEU',
                    id: 37,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b Yandex Europe B.V.'
                },
                {
                    code: 'TransferFromInvoice',
                    id: 11142,
                    name:
                        '\u041f\u0435\u0440\u0435\u043d\u043e\u0441 \u0441\u0440\u0435\u0434\u0441\u0442\u0432 \u0441\u043e \u0441\u0447\u0435\u0442\u0430'
                },
                {
                    code: 'OEBSReexportPerson',
                    id: 11225,
                    name:
                        '\u041f\u0435\u0440\u0435\u043f\u043e\u0441\u0442\u0430\u0432\u0438\u0442\u044c \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u0430 \u0432 \u043e\u0447\u0435\u0440\u0435\u0434\u044c \u044d\u043a\u0441\u043f\u043e\u0440\u0442\u0430 OEBS'
                },
                {
                    code: 'AlterFirmTR',
                    id: 41,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b Yandex Turkey'
                },
                {
                    code: 'AlterFirmHK',
                    id: 65,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b Yandex E-commerce Limited'
                },
                {
                    code: 'DealPassport',
                    id: 30,
                    name: '\u0420\u0430\u0431\u043e\u0442\u0430 \u0441 \u041f\u0421'
                },
                {
                    code: 'GrantAllPermissionsToAll',
                    id: 7,
                    name:
                        '\u041d\u0430\u0437\u043d\u0430\u0447\u0435\u043d\u0438\u0435 \u043f\u043e\u043b\u043d\u043e\u043c\u043e\u0447\u0438\u0439'
                },
                {
                    code: 'ui_orders_report__manager_code',
                    id: 1009,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u0417\u0430\u043a\u0430\u0437\u044b \u0432 \u041f\u0418 - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u041c\u0435\u043d\u0435\u0434\u0436\u0435\u0440'
                },
                {
                    code: 'EditINN',
                    id: 43,
                    name:
                        '\u0417\u0430\u0432\u0435\u0434\u0435\u043d\u0438\u0435 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u0430 \u0441 \u0415\u0413\u0420\u041f\u041e\u0423 \u0432\u043c\u0435\u0441\u0442\u043e \u0418\u041d\u041d'
                },
                {
                    code: 'AdminAccess',
                    id: 0,
                    name:
                        '\u0414\u043e\u0441\u0442\u0443\u043f \u043a \u0430\u0434\u043c\u0438\u043d\u0438\u0441\u0442\u0440\u0430\u0442\u0438\u0432\u043d\u043e\u043c\u0443 \u0440\u0435\u0436\u0438\u043c\u0443'
                },
                {
                    code: 'ViewContracts',
                    id: 11181,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u043e\u0432'
                },
                {
                    code: 'RecalculateOverdraft',
                    id: 42,
                    name:
                        '\u041f\u0435\u0440\u0435\u0440\u0430\u0441\u0447\u0435\u0442 \u043e\u0432\u0435\u0440\u0434\u0440\u0430\u0444\u0442\u0430'
                },
                {
                    code: 'OEBSReexportInvoice',
                    id: 11223,
                    name:
                        '\u041f\u0435\u0440\u0435\u043f\u043e\u0441\u0442\u0430\u0432\u0438\u0442\u044c \u0441\u0447\u0451\u0442 \u0432 \u043e\u0447\u0435\u0440\u0435\u0434\u044c \u044d\u043a\u0441\u043f\u043e\u0440\u0442\u0430 OEBS'
                },
                {
                    code: 'DoInvoiceRefundsTrust',
                    id: 11161,
                    name:
                        '\u0421\u043e\u0437\u0434\u0430\u043d\u0438\u0435 \u0432\u043e\u0437\u0432\u0440\u0430\u0442\u043e\u0432 \u043f\u043e \u0441\u0447\u0435\u0442\u0430\u043c \u0447\u0435\u0440\u0435\u0437 \u0422\u0440\u0430\u0441\u0442'
                },
                {
                    code: 'TransferBetweenClients',
                    id: 11143,
                    name:
                        '\u041f\u0435\u0440\u0435\u043d\u043e\u0441 \u0441\u0440\u0435\u0434\u0441\u0442\u0432 \u043c\u0435\u0436\u0434\u0443 \u0440\u0430\u0437\u043d\u044b\u043c\u0438 \u043a\u043b\u0438\u0435\u043d\u0442\u0430\u043c\u0438'
                },
                {
                    code: 'invoices_report__manager_info',
                    id: 1006,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u0421\u0447\u0435\u0442\u0430 - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u041c\u0435\u043d\u0435\u0434\u0436\u0435\u0440'
                },
                {
                    code: 'CreateClient',
                    id: 27,
                    name:
                        '\u0421\u043e\u0437\u0434\u0430\u043d\u0438\u0435 \u043a\u043b\u0438\u0435\u043d\u0442\u0430'
                },
                {
                    code: 'CloseInvoice',
                    id: 11222,
                    name:
                        '\u0417\u0430\u043a\u0440\u044b\u0442\u0438\u0435 \u0441\u0447\u0451\u0442\u0430'
                },
                {
                    code: 'DoInvoiceRefunds',
                    id: 4004,
                    name:
                        '\u0421\u043e\u0437\u0434\u0430\u043d\u0438\u0435 \u0432\u043e\u0437\u0432\u0440\u0430\u0442\u043e\u0432 \u043f\u043e \u0441\u0447\u0435\u0442\u0430\u043c'
                },
                {
                    code: 'AlterFirmKZTaxi',
                    id: 51,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b \u041e\u041e\u041e \u00ab\u042f\u043d\u0434\u0435\u043a\u0441.\u0422\u0430\u043a\u0441\u0438 \u041a\u0430\u0437\u0430\u0445\u0441\u0442\u0430\u043d\u00bb'
                },
                {
                    code: 'IssueInvoices',
                    id: 11106,
                    name:
                        '\u0412\u044b\u0441\u0442\u0430\u0432\u043b\u0435\u043d\u0438\u0435 \u0441\u0447\u0435\u0442\u043e\u0432'
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
                    code: 'AlterInvoiceContract',
                    id: 11111,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0430 \u0441\u0447\u0435\u0442\u0430'
                },
                {
                    code: 'WithdrawConsumesPostpay',
                    id: 66,
                    name:
                        '\u0421\u043d\u044f\u0442\u0438\u0435 \u0437\u0430\u0447\u0438\u0441\u043b\u0435\u043d\u0438\u0439 \u0441 \u043f\u043e\u0441\u0442\u043e\u043f\u043b\u0430\u0442\u043d\u044b\u0445 \u0441\u0447\u0435\u0442\u043e\u0432'
                },
                {
                    code: 'AlterInvoicePaysys',
                    id: 11109,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0441\u043f\u043e\u0441\u043e\u0431\u0430 \u043e\u043f\u043b\u0430\u0442\u044b \u0441\u0447\u0435\u0442\u0430'
                },
                {
                    code: 'OEBSReexportContract',
                    id: 11226,
                    name:
                        '\u041f\u0435\u0440\u0435\u043f\u043e\u0441\u0442\u0430\u0432\u0438\u0442\u044c \u0434\u043e\u0433\u043e\u0432\u043e\u0440/\u0434\u043e\u043f.\u0441\u043e\u0433\u043b\u0430\u0448\u0435\u043d\u0438\u0435 \u0432 \u043e\u0447\u0435\u0440\u0435\u0434\u044c \u044d\u043a\u0441\u043f\u043e\u0440\u0442\u0430 OEBS'
                },
                {
                    code: 'ViewInvoices',
                    id: 11105,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u0441\u0447\u0435\u0442\u043e\u0432'
                },
                {
                    code: 'AlterInvoiceDate',
                    id: 11108,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u0430\u0442\u044b \u0441\u0447\u0435\u0442\u0430'
                },
                {
                    code: 'UseRestrictedINN',
                    id: 11301,
                    name:
                        '\u0418\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u043d\u0438\u0435 \u0437\u0430\u043f\u0440\u0435\u0449\u0451\u043d\u043d\u044b\u0445 \u0418\u041d\u041d'
                },
                {
                    code: 'SetClientOverdraftBan',
                    id: 11325,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0438\u0442\u044c \u0431\u0430\u043d \u043e\u0432\u0435\u0440\u0434\u0440\u0430\u0444\u0442\u0430 \u0434\u043b\u044f \u043a\u043b\u0438\u0435\u043d\u0442\u0430'
                },
                {
                    code: 'AlterFirmUS',
                    id: 34,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b Yandex Inc'
                },
                {
                    code: 'ChangeUnusedFundsLock',
                    id: 11227,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0438\u0442\u044c \u043f\u0440\u0438\u0447\u0438\u043d\u0443 \u0441\u043d\u044f\u0442\u0438\u044f \u0431\u0435\u0437\u0437\u0430\u043a\u0430\u0437\u044c\u044f \u0443 \u0441\u0447\u0451\u0442\u0430'
                },
                {
                    code: 'AlterPrintTemplate',
                    id: 55,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0448\u0430\u0431\u043b\u043e\u043d\u0430 \u043f\u0435\u0447\u0430\u0442\u043d\u043e\u0439 \u0444\u043e\u0440\u043c\u044b \u0432 \u0434\u043e\u0433\u043e\u0432\u0440\u0435'
                },
                {
                    code: 'AlterFirmNVTaxiuber',
                    id: 63,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b Uber ML B.V.'
                },
                {
                    code: 'AlterInvoicePerson',
                    id: 11110,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u0430 \u0441\u0447\u0435\u0442\u0430'
                },
                {
                    code: 'invoices_report__manager',
                    id: 1007,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u0421\u0447\u0435\u0442\u0430 - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u0412\u043a\u043b\u044e\u0447\u0435\u043d\u0438\u0435 \u0437\u0430\u043a\u0430\u0437\u0430'
                },
                {
                    code: 'AlterFirmKZ',
                    id: 33,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b \u0422\u041e\u041e \u00abKazNet Media (\u041a\u0430\u0437\u041d\u0435\u0442 \u041c\u0435\u0434\u0438\u0430)\u00bb'
                },
                {
                    code: 'TerminalEdit',
                    id: 57,
                    name:
                        '\u0420\u0435\u0434\u0430\u043a\u0442\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0435 \u0442\u0435\u0440\u043c\u0438\u043d\u0430\u043b\u0430'
                },
                {
                    code: 'EDOViewer',
                    id: 60,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u0433\u0440\u0430\u0444\u0438\u043a\u0430 \u042d\u0414\u041e'
                },
                {
                    code: 'AlterFirmUA',
                    id: 32,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b \u041e\u041e\u041e \u00ab\u042f\u043d\u0434\u0435\u043a\u0441.\u0423\u043a\u0440\u0430\u0438\u043d\u0430\u00bb'
                },
                {
                    code: 'TransferFromOrder',
                    id: 11141,
                    name:
                        '\u041f\u0435\u0440\u0435\u043d\u043e\u0441 \u0441\u0440\u0435\u0434\u0441\u0442\u0432 c \u0437\u0430\u043a\u0430\u0437\u0430'
                },
                {
                    code: 'ViewAllInvoices',
                    id: 1,
                    name:
                        '\u0423\u0441\u0442\u0430\u0440\u0435\u0432\u0448\u0435\u0435, \u043d\u0435 \u0438\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u0442\u044c. \u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u0441\u0447\u0435\u0442\u043e\u0432.'
                },
                {
                    code: 'ManageRBSPayments',
                    id: 19,
                    name:
                        '\u041e\u043f\u0435\u0440\u0430\u0446\u0438\u0438 \u0441 \u043f\u043b\u0430\u0442\u0435\u0436\u0430\u043c\u0438 \u043f\u043e RBS'
                },
                {
                    code: 'PayInvoiceExtension',
                    id: 11221,
                    name:
                        '\u041f\u0435\u0440\u0435\u043d\u0435\u0441\u0442\u0438 \u0441\u0440\u043e\u043a \u043e\u043f\u043b\u0430\u0442\u044b \u0441\u0447\u0451\u0442\u0430'
                },
                {
                    code: 'AdditionalFunctions',
                    id: 47,
                    name:
                        '\u0414\u043e\u043f\u043e\u043b\u043d\u0438\u0442\u0435\u043b\u044c\u043d\u044b\u0435 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438'
                },
                {
                    code: 'TerminalView',
                    id: 56,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u0442\u0435\u0440\u043c\u0438\u043d\u0430\u043b\u0430'
                },
                {
                    code: 'ChangeRepaymentsStatus',
                    id: 11103,
                    name:
                        '\u041f\u043e\u0434\u0442\u0432\u0435\u0440\u0436\u0434\u0435\u043d\u0438\u0435 \u0441\u0447\u0435\u0442\u043e\u0432 \u043d\u0430 \u043f\u043e\u0433\u0430\u0448\u0435\u043d\u0438\u0435'
                },
                {
                    code: 'ProcessAllInvoices',
                    id: 8,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0441\u0447\u0435\u0442\u043e\u0432'
                },
                {
                    code: 'AlterFirmSW',
                    id: 39,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b Yandex Europe AG'
                },
                {
                    code: 'r_django_fraud_monitoring',
                    id: 62,
                    name:
                        '\u041f\u0440\u0430\u0432\u0430 \u043d\u0430 \u0447\u0442\u0435\u043d\u0438\u0435 Django Fraud Monitoring'
                },
                {
                    code: 'TransferUnusedFunds',
                    id: 11121,
                    name:
                        '\u041f\u0435\u0440\u0435\u043d\u043e\u0441 \u0441\u0432\u043e\u0431\u043e\u0434\u043d\u044b\u0445 \u0441\u0440\u0435\u0434\u0441\u0442\u0432'
                },
                {
                    code: 'ManageBadDebts',
                    id: 21,
                    name:
                        '\u041f\u0440\u0438\u0437\u043d\u0430\u043d\u0438\u0435 "\u043f\u043b\u043e\u0445\u043e\u0433\u043e" \u0434\u043e\u043b\u0433\u0430'
                },
                {
                    code: 'AlterInvoiceSum',
                    id: 11107,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0441\u0443\u043c\u043c\u044b \u0441\u0447\u0435\u0442\u0430'
                },
                {
                    code: 'ApexReports',
                    id: 3001,
                    name: '\u0410\u0440\u0435\u0445. \u041e\u0442\u0447\u0435\u0442\u044b'
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
                    code: 'CreateRequestsShop',
                    id: 11101,
                    name:
                        '\u0412\u044b\u0441\u0442\u0430\u0432\u043b\u0435\u043d\u0438\u0435 \u0441\u0447\u0435\u0442\u043e\u0432 \u0447\u0435\u0440\u0435\u0437 \u043c\u0430\u0433\u0430\u0437\u0438\u043d'
                },
                {
                    code: 'OEBSReexportAct',
                    id: 11224,
                    name:
                        '\u041f\u0435\u0440\u0435\u043f\u043e\u0441\u0442\u0430\u0432\u0438\u0442\u044c \u0430\u043a\u0442 \u0432 \u043e\u0447\u0435\u0440\u0435\u0434\u044c \u044d\u043a\u0441\u043f\u043e\u0440\u0442\u0430 OEBS'
                },
                {
                    code: 'WithdrawConsumesPrepay',
                    id: 67,
                    name:
                        '\u0421\u043d\u044f\u0442\u0438\u0435 \u0437\u0430\u0447\u0438\u0441\u043b\u0435\u043d\u0438\u0439 \u0441 \u043f\u0440\u0435\u0434\u043e\u043f\u043b\u0430\u0442\u043d\u044b\u0445 \u0441\u0447\u0435\u0442\u043e\u0432'
                },
                {
                    code: 'ClientFraudStatusEdit',
                    id: 58,
                    name:
                        '\u0420\u0435\u0434\u0430\u043a\u0442\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0435 \u043a\u043b\u0438\u0435\u043d\u0442\u0430: \u043f\u0440\u043e\u0441\u0442\u0430\u043d\u043e\u0432\u043a\u0430 \u0444\u043b\u0430\u0433\u0430 deny_cc / fraud_status'
                },
                {
                    code: 'AlterFirmRU',
                    id: 31,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b \u041e\u041e\u041e \u00ab\u042f\u043d\u0434\u0435\u043a\u0441\u00bb'
                }
            ]
        }
    },
    {
        request: [
            `${HOST}/export/get-state`,
            {
                classname: 'Act',
                queue_type: 'OEBS',
                object_id: 124863520
            },
            false,
            false
        ],
        response: {
            version: { 'yb-snout-api': '3.59.2' },
            data: { export_dt: null, state: 'WAITING' }
        }
    }
];

export const act124863523 = [
    {
        request: [`${HOST}/act`, { act_id: '124863523', act_eid: undefined }, false, false],
        response: {
            version: { 'yb-snout-api': '3.59.2' },
            data: {
                export_dt: null,
                rows: [
                    {
                        consume: {
                            seqnum: 0,
                            unit_name: '\u0443.\u0435.',
                            discount_pct: '0.00',
                            order: {
                                text: 'Py_Test order 7-1475',
                                nds: 1,
                                service_order_id: 56764022,
                                id: 3432957417,
                                service: {
                                    cc: 'PPC',
                                    id: 7,
                                    name:
                                        '\u0414\u0438\u0440\u0435\u043a\u0442: \u0420\u0435\u043a\u043b\u0430\u043c\u043d\u044b\u0435 \u043a\u0430\u043c\u043f\u0430\u043d\u0438\u0438'
                                }
                            }
                        },
                        price: '0.4100',
                        amount: '20.50',
                        manager: {
                            manager_code: 20453,
                            name:
                                '\u041f\u0435\u0440\u0430\u043d\u0438\u0434\u0437\u0435 \u041d\u0443\u0433\u0437\u0430\u0440 \u0413\u0435\u043e\u0440\u0433\u0438\u0435\u0432\u0438\u0447'
                        },
                        finish_sum: '20.50',
                        netting: null,
                        consume_nds: null,
                        act_sum: '20.50',
                        id: 767166302,
                        quantity: '50.000000'
                    }
                ],
                is_trp: null,
                oebs_exportable: true,
                memo: null,
                is_docs_detailed: false,
                factura: null,
                amount: '20.50',
                good_debt: '0',
                invoice: {
                    firm: { id: 4 },
                    postpay: false,
                    receipt_sum_1c: '123.00',
                    total_sum: '123.00',
                    paysys: { id: 1028 },
                    currency: 'USD',
                    person: { id: 12342892, name: 'USA legal Payer' },
                    client: {
                        id: 1338514888,
                        name: '\u041e\u041e\u041e "\u041a\u043b\u0438\u0435\u043d\u0442"'
                    },
                    iso_currency: 'USD',
                    external_id: 'US-3310227702-1',
                    id: 118620173
                },
                is_docs_separated: false,
                id: 124863523,
                hidden: false,
                act_sum: '20.50',
                external_id: '153731997',
                dt: '2020-08-31T00:00:00'
            }
        }
    },
    {
        request: [
            `${HOST}/user/object-permissions`,
            { classname: 'Act', object_id: 124863523 },
            false,
            false
        ],
        response: {
            version: { 'yb-snout-api': '3.59.2' },
            data: [
                {
                    code: 'ViewInvoices',
                    id: 11105,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u0441\u0447\u0435\u0442\u043e\u0432'
                },
                {
                    code: 'TransferBetweenClients',
                    id: 11143,
                    name:
                        '\u041f\u0435\u0440\u0435\u043d\u043e\u0441 \u0441\u0440\u0435\u0434\u0441\u0442\u0432 \u043c\u0435\u0436\u0434\u0443 \u0440\u0430\u0437\u043d\u044b\u043c\u0438 \u043a\u043b\u0438\u0435\u043d\u0442\u0430\u043c\u0438'
                },
                {
                    code: 'ClientFraudStatusEdit',
                    id: 58,
                    name:
                        '\u0420\u0435\u0434\u0430\u043a\u0442\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0435 \u043a\u043b\u0438\u0435\u043d\u0442\u0430: \u043f\u0440\u043e\u0441\u0442\u0430\u043d\u043e\u0432\u043a\u0430 \u0444\u043b\u0430\u0433\u0430 deny_cc / fraud_status'
                },
                {
                    code: 'AlterFirmTR',
                    id: 41,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b Yandex Turkey'
                },
                {
                    code: 'IssueInvoices',
                    id: 11106,
                    name:
                        '\u0412\u044b\u0441\u0442\u0430\u0432\u043b\u0435\u043d\u0438\u0435 \u0441\u0447\u0435\u0442\u043e\u0432'
                },
                {
                    code: 'PersonEdit',
                    id: 26,
                    name:
                        '\u0420\u0435\u0434\u0430\u043a\u0442\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0435 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u0430'
                },
                {
                    code: 'WithdrawConsumesPrepay',
                    id: 67,
                    name:
                        '\u0421\u043d\u044f\u0442\u0438\u0435 \u0437\u0430\u0447\u0438\u0441\u043b\u0435\u043d\u0438\u0439 \u0441 \u043f\u0440\u0435\u0434\u043e\u043f\u043b\u0430\u0442\u043d\u044b\u0445 \u0441\u0447\u0435\u0442\u043e\u0432'
                },
                {
                    code: 'AlterFirmSW',
                    id: 39,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b Yandex Europe AG'
                },
                {
                    code: 'CloseInvoice',
                    id: 11222,
                    name:
                        '\u0417\u0430\u043a\u0440\u044b\u0442\u0438\u0435 \u0441\u0447\u0451\u0442\u0430'
                },
                {
                    code: 'DoInvoiceRefunds',
                    id: 4004,
                    name:
                        '\u0421\u043e\u0437\u0434\u0430\u043d\u0438\u0435 \u0432\u043e\u0437\u0432\u0440\u0430\u0442\u043e\u0432 \u043f\u043e \u0441\u0447\u0435\u0442\u0430\u043c'
                },
                {
                    code: 'OEBSReexportAct',
                    id: 11224,
                    name:
                        '\u041f\u0435\u0440\u0435\u043f\u043e\u0441\u0442\u0430\u0432\u0438\u0442\u044c \u0430\u043a\u0442 \u0432 \u043e\u0447\u0435\u0440\u0435\u0434\u044c \u044d\u043a\u0441\u043f\u043e\u0440\u0442\u0430 OEBS'
                },
                {
                    code: 'ApexReports',
                    id: 3001,
                    name: '\u0410\u0440\u0435\u0445. \u041e\u0442\u0447\u0435\u0442\u044b'
                },
                {
                    code: 'ViewProduct',
                    id: 11241,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u043f\u0440\u043e\u0434\u0443\u043a\u0442\u043e\u0432'
                },
                {
                    code: 'DoInvoiceRefundsTrust',
                    id: 11161,
                    name:
                        '\u0421\u043e\u0437\u0434\u0430\u043d\u0438\u0435 \u0432\u043e\u0437\u0432\u0440\u0430\u0442\u043e\u0432 \u043f\u043e \u0441\u0447\u0435\u0442\u0430\u043c \u0447\u0435\u0440\u0435\u0437 \u0422\u0440\u0430\u0441\u0442'
                },
                {
                    code: 'SetClientOverdraftBan',
                    id: 11325,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0438\u0442\u044c \u0431\u0430\u043d \u043e\u0432\u0435\u0440\u0434\u0440\u0430\u0444\u0442\u0430 \u0434\u043b\u044f \u043a\u043b\u0438\u0435\u043d\u0442\u0430'
                },
                {
                    code: 'OEBSReexportInvoice',
                    id: 11223,
                    name:
                        '\u041f\u0435\u0440\u0435\u043f\u043e\u0441\u0442\u0430\u0432\u0438\u0442\u044c \u0441\u0447\u0451\u0442 \u0432 \u043e\u0447\u0435\u0440\u0435\u0434\u044c \u044d\u043a\u0441\u043f\u043e\u0440\u0442\u0430 OEBS'
                },
                {
                    code: 'SendInvoices',
                    id: 11102,
                    name:
                        '\u041e\u0442\u043f\u0440\u0430\u0432\u043a\u0430 \u0441\u0447\u0435\u0442\u043e\u0432 \u043d\u0430 \u043f\u043e\u0447\u0442\u0443'
                },
                {
                    code: 'AdminAccess',
                    id: 0,
                    name:
                        '\u0414\u043e\u0441\u0442\u0443\u043f \u043a \u0430\u0434\u043c\u0438\u043d\u0438\u0441\u0442\u0440\u0430\u0442\u0438\u0432\u043d\u043e\u043c\u0443 \u0440\u0435\u0436\u0438\u043c\u0443'
                },
                {
                    code: 'SetMcbFlags',
                    id: 11324,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0438\u0442\u044c \u043a\u0430\u0442\u0435\u0433\u043e\u0440\u0438\u0438 \u0434\u043b\u044f \u043f\u043b\u043e\u0449\u0430\u0434\u043e\u043a \u043a\u043b\u0438\u0435\u043d\u0442\u043e\u0432'
                },
                {
                    code: 'TransferUnusedFunds',
                    id: 11121,
                    name:
                        '\u041f\u0435\u0440\u0435\u043d\u043e\u0441 \u0441\u0432\u043e\u0431\u043e\u0434\u043d\u044b\u0445 \u0441\u0440\u0435\u0434\u0441\u0442\u0432'
                },
                {
                    code: 'AlterInvoiceDate',
                    id: 11108,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u0430\u0442\u044b \u0441\u0447\u0435\u0442\u0430'
                },
                {
                    code: 'AlterFirmEU',
                    id: 37,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b Yandex Europe B.V.'
                },
                {
                    code: 'AlterFirmUS',
                    id: 34,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b Yandex Inc'
                },
                {
                    code: 'AlterFirmKZ',
                    id: 33,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b \u0422\u041e\u041e \u00abKazNet Media (\u041a\u0430\u0437\u041d\u0435\u0442 \u041c\u0435\u0434\u0438\u0430)\u00bb'
                },
                {
                    code: 'SendActs',
                    id: 11323,
                    name:
                        '\u041e\u0442\u043f\u0440\u0430\u0432\u0438\u0442\u044c \u0430\u043a\u0442\u044b \u043f\u043e \u044d\u043b\u0435\u043a\u0442\u0440\u043e\u043d\u043d\u043e\u0439 \u0438\u043b\u0438 \u043e\u0431\u044b\u043d\u043e\u0439 \u043f\u043e\u0447\u0442\u0435'
                },
                {
                    code: 'AlterInvoicePaysys',
                    id: 11109,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0441\u043f\u043e\u0441\u043e\u0431\u0430 \u043e\u043f\u043b\u0430\u0442\u044b \u0441\u0447\u0435\u0442\u0430'
                },
                {
                    code: 'ViewPersons',
                    id: 11322,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u043e\u0432'
                },
                {
                    code: 'AlterInvoiceContract',
                    id: 11111,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0430 \u0441\u0447\u0435\u0442\u0430'
                },
                {
                    code: 'AlterFirmUA',
                    id: 32,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b \u041e\u041e\u041e \u00ab\u042f\u043d\u0434\u0435\u043a\u0441.\u0423\u043a\u0440\u0430\u0438\u043d\u0430\u00bb'
                },
                {
                    code: 'AlterInvoicePerson',
                    id: 11110,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u0430 \u0441\u0447\u0435\u0442\u0430'
                },
                {
                    code: 'RepresentClient',
                    id: 2000,
                    name:
                        '\u0414\u0435\u0439\u0441\u0442\u0432\u0438\u044f \u043e\u0442 \u0438\u043c\u0435\u043d\u0438 \u043a\u043b\u0438\u0435\u043d\u0442\u0430'
                },
                {
                    code: 'ViewClients',
                    id: 11321,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u043a\u043b\u0438\u0435\u043d\u0442\u043e\u0432'
                },
                {
                    code: 'TransferFromOrder',
                    id: 11141,
                    name:
                        '\u041f\u0435\u0440\u0435\u043d\u043e\u0441 \u0441\u0440\u0435\u0434\u0441\u0442\u0432 c \u0437\u0430\u043a\u0430\u0437\u0430'
                },
                {
                    code: 'ChangeRepaymentsStatus',
                    id: 11103,
                    name:
                        '\u041f\u043e\u0434\u0442\u0432\u0435\u0440\u0436\u0434\u0435\u043d\u0438\u0435 \u0441\u0447\u0435\u0442\u043e\u0432 \u043d\u0430 \u043f\u043e\u0433\u0430\u0448\u0435\u043d\u0438\u0435'
                },
                {
                    code: 'UseRestrictedINN',
                    id: 11301,
                    name:
                        '\u0418\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u043d\u0438\u0435 \u0437\u0430\u043f\u0440\u0435\u0449\u0451\u043d\u043d\u044b\u0445 \u0418\u041d\u041d'
                },
                {
                    code: 'CreateClient',
                    id: 27,
                    name:
                        '\u0421\u043e\u0437\u0434\u0430\u043d\u0438\u0435 \u043a\u043b\u0438\u0435\u043d\u0442\u0430'
                },
                {
                    code: 'PayInvoiceExtension',
                    id: 11221,
                    name:
                        '\u041f\u0435\u0440\u0435\u043d\u0435\u0441\u0442\u0438 \u0441\u0440\u043e\u043a \u043e\u043f\u043b\u0430\u0442\u044b \u0441\u0447\u0451\u0442\u0430'
                },
                {
                    code: 'UseAdminPersons',
                    id: 11112,
                    name:
                        '\u0418\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u043d\u0438\u0435 \u043e\u0441\u043e\u0431\u044b\u0445 \u0442\u0438\u043f\u043e\u0432 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u043e\u0432'
                },
                {
                    code: 'NotifyOrder',
                    id: 11281,
                    name:
                        '\u041e\u0442\u043f\u0440\u0430\u0432\u0438\u0442\u044c \u043d\u043e\u0442\u0438\u0444\u0438\u043a\u0430\u0446\u0438\u044e \u043f\u043e \u0437\u0430\u043a\u0430\u0437\u0443'
                },
                {
                    code: 'CreateRequestsShop',
                    id: 11101,
                    name:
                        '\u0412\u044b\u0441\u0442\u0430\u0432\u043b\u0435\u043d\u0438\u0435 \u0441\u0447\u0435\u0442\u043e\u0432 \u0447\u0435\u0440\u0435\u0437 \u043c\u0430\u0433\u0430\u0437\u0438\u043d'
                },
                {
                    code: 'ManagersOperations',
                    id: 23,
                    name:
                        '\u0414\u0435\u0439\u0441\u0442\u0432\u0438\u044f \u043c\u0435\u043d\u0435\u0434\u0436\u0435\u0440\u0430'
                },
                {
                    code: 'ViewDevData',
                    id: 11261,
                    name:
                        '\u0414\u043e\u0441\u0442\u0443\u043f \u043a \u0442\u0435\u0445\u043d\u0438\u0447\u0435\u0441\u043a\u043e\u0439 \u0438\u043d\u0444\u043e\u0440\u043c\u0430\u0446\u0438\u0438'
                },
                {
                    code: 'TransferFromInvoice',
                    id: 11142,
                    name:
                        '\u041f\u0435\u0440\u0435\u043d\u043e\u0441 \u0441\u0440\u0435\u0434\u0441\u0442\u0432 \u0441\u043e \u0441\u0447\u0435\u0442\u0430'
                },
                {
                    code: 'AlterFirmNVTaxi',
                    id: 48,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b Yandex.Taxi B.V.'
                },
                {
                    code: 'DealPassport',
                    id: 30,
                    name: '\u0420\u0430\u0431\u043e\u0442\u0430 \u0441 \u041f\u0421'
                },
                {
                    code: 'PersonPostAddressEdit',
                    id: 38,
                    name:
                        '\u0420\u0435\u0434\u0430\u043a\u0442\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0435 \u043b\u044e\u0431\u044b\u0445 \u043f\u043e\u043b\u0435\u0439 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u0430'
                },
                {
                    code: 'OEBSReexportContract',
                    id: 11226,
                    name:
                        '\u041f\u0435\u0440\u0435\u043f\u043e\u0441\u0442\u0430\u0432\u0438\u0442\u044c \u0434\u043e\u0433\u043e\u0432\u043e\u0440/\u0434\u043e\u043f.\u0441\u043e\u0433\u043b\u0430\u0448\u0435\u043d\u0438\u0435 \u0432 \u043e\u0447\u0435\u0440\u0435\u0434\u044c \u044d\u043a\u0441\u043f\u043e\u0440\u0442\u0430 OEBS'
                },
                {
                    code: 'AlterFirmRU',
                    id: 31,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b \u041e\u041e\u041e \u00ab\u042f\u043d\u0434\u0435\u043a\u0441\u00bb'
                },
                {
                    code: 'ViewContracts',
                    id: 11181,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u043e\u0432'
                },
                {
                    code: 'AlterFirmHK',
                    id: 65,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b Yandex E-commerce Limited'
                },
                {
                    code: 'AdditionalFunctions',
                    id: 47,
                    name:
                        '\u0414\u043e\u043f\u043e\u043b\u043d\u0438\u0442\u0435\u043b\u044c\u043d\u044b\u0435 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438'
                },
                {
                    code: 'EditClient',
                    id: 28,
                    name:
                        '\u0420\u0435\u0434\u0430\u043a\u0442\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0435 \u043a\u043b\u0438\u0435\u043d\u0442\u0430'
                },
                {
                    code: 'PaymentLinksReport',
                    id: 11104,
                    name:
                        '\u041f\u043e\u0438\u0441\u043a \u043f\u043e \u043f\u043b\u0430\u0442\u0435\u0436\u0430\u043c'
                },
                {
                    code: 'TerminalView',
                    id: 56,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u0442\u0435\u0440\u043c\u0438\u043d\u0430\u043b\u0430'
                },
                {
                    code: 'PersonView',
                    id: 25,
                    name:
                        '\u0423\u0441\u0442\u0430\u0440\u0435\u0432\u0448\u0435\u0435, \u043d\u0435 \u0438\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u0442\u044c. \u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u0430.'
                },
                {
                    code: 'WithdrawConsumesPostpay',
                    id: 66,
                    name:
                        '\u0421\u043d\u044f\u0442\u0438\u0435 \u0437\u0430\u0447\u0438\u0441\u043b\u0435\u043d\u0438\u0439 \u0441 \u043f\u043e\u0441\u0442\u043e\u043f\u043b\u0430\u0442\u043d\u044b\u0445 \u0441\u0447\u0435\u0442\u043e\u0432'
                },
                {
                    code: 'SubstituteLogin',
                    id: 24,
                    name:
                        '\u041f\u043e\u0434\u043c\u0435\u043d\u0430 \u043b\u043e\u0433\u0438\u043d\u0430'
                },
                {
                    code: 'AlterFirmAUTORU',
                    id: 45,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b \u041e\u041e\u041e \u00ab\u0410\u0412\u0422\u041e.\u0420\u0423 \u0425\u043e\u043b\u0434\u0438\u043d\u0433\u00bb'
                },
                {
                    code: 'assist_payments_report__fraud_dt',
                    id: 1004,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u041f\u043b\u0430\u0442\u0435\u0436\u0438 \u043f\u043e Assist - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u0414\u0430\u0442\u0430 \u043e\u0431\u043d\u0430\u0440\u0443\u0436\u0435\u043d\u0438\u044f \u0444\u0440\u043e\u0434\u0430'
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
                    code: 'invoices_report__receipt_sum_1c',
                    id: 1001,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u0421\u0447\u0435\u0442\u0430 - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u0421\u0443\u043c\u043c\u0430 \u043f\u0440\u0438\u0445\u043e\u0434\u0430 \u0438\u0437 1\u0421'
                },
                {
                    code: 'AlterPrintTemplate',
                    id: 55,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0448\u0430\u0431\u043b\u043e\u043d\u0430 \u043f\u0435\u0447\u0430\u0442\u043d\u043e\u0439 \u0444\u043e\u0440\u043c\u044b \u0432 \u0434\u043e\u0433\u043e\u0432\u0440\u0435'
                },
                {
                    code: 'CreateCerificatePayments',
                    id: 4,
                    name:
                        '\u0412\u043d\u0435\u0441\u0435\u043d\u0438\u0435 \u043f\u043b\u0430\u0442\u0435\u0436\u0435\u0439 \u0441\u0435\u0440\u0442\u0438\u0444\u0438\u043a\u0430\u0442\u043e\u043c, \u0431\u0430\u0440\u0442\u0435\u0440\u043e\u043c'
                },
                {
                    code: 'AlterFirmNVTaxiuber',
                    id: 63,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b Uber ML B.V.'
                },
                {
                    code: 'BillingSupport',
                    id: 1100,
                    name:
                        '\u0421\u0430\u043f\u043f\u043e\u0440\u0442 \u0411\u0438\u043b\u043b\u0438\u043d\u0433\u0430'
                },
                {
                    code: 'AlterFirmUATaxi',
                    id: 49,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b \u041e\u041e\u041e "\u042f\u043d\u0434\u0435\u043a\u0441.\u0422\u0430\u043a\u0441\u0438 \u0423\u043a\u0440\u0430\u0438\u043d\u0430"'
                },
                {
                    code: 'TerminalEdit',
                    id: 57,
                    name:
                        '\u0420\u0435\u0434\u0430\u043a\u0442\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0435 \u0442\u0435\u0440\u043c\u0438\u043d\u0430\u043b\u0430'
                },
                {
                    code: 'consumes_list__consume_sum',
                    id: 1011,
                    name:
                        '\u0421\u043f\u0438\u0441\u043e\u043a \u0437\u0430\u044f\u0432\u043e\u043a - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u041f\u0435\u0440\u0432\u043e\u043d\u0430\u0447\u0430\u043b\u044c\u043d\u0430\u044f \u0441\u0443\u043c\u043c\u0430'
                },
                {
                    code: 'ChangeUnusedFundsLock',
                    id: 11227,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0438\u0442\u044c \u043f\u0440\u0438\u0447\u0438\u043d\u0443 \u0441\u043d\u044f\u0442\u0438\u044f \u0431\u0435\u0437\u0437\u0430\u043a\u0430\u0437\u044c\u044f \u0443 \u0441\u0447\u0451\u0442\u0430'
                },
                {
                    code: 'AlterInvoiceSum',
                    id: 11107,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0441\u0443\u043c\u043c\u044b \u0441\u0447\u0435\u0442\u0430'
                },
                {
                    code: 'ManageRBSPayments',
                    id: 19,
                    name:
                        '\u041e\u043f\u0435\u0440\u0430\u0446\u0438\u0438 \u0441 \u043f\u043b\u0430\u0442\u0435\u0436\u0430\u043c\u0438 \u043f\u043e RBS'
                },
                {
                    code: 'ui_orders_report__manager_code',
                    id: 1009,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u0417\u0430\u043a\u0430\u0437\u044b \u0432 \u041f\u0418 - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u041c\u0435\u043d\u0435\u0434\u0436\u0435\u0440'
                },
                {
                    code: 'IssueUSDInvoices',
                    id: 13,
                    name:
                        '\u0412\u044b\u0441\u0442\u0430\u0432\u043b\u044f\u0442\u044c \u0434\u043e\u043b\u043b\u0430\u0440\u043e\u0432\u044b\u0435 \u0441\u0447\u0435\u0442\u0430'
                },
                {
                    code: 'EditINN',
                    id: 43,
                    name:
                        '\u0417\u0430\u0432\u0435\u0434\u0435\u043d\u0438\u0435 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u0430 \u0441 \u0415\u0413\u0420\u041f\u041e\u0423 \u0432\u043c\u0435\u0441\u0442\u043e \u0418\u041d\u041d'
                },
                {
                    code: 'ViewFishes',
                    id: 22,
                    name: '\u0424\u0438\u0448\u043a\u043e\u0432\u0438\u0434\u0435\u0446'
                },
                {
                    code: 'AlterFirmAMTaxi',
                    id: 53,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b \u041e\u041e\u041e \u00ab\u042f\u043d\u0434\u0435\u043a\u0441.\u0422\u0430\u043a\u0441\u0438 \u0410\u0440\u043c\u0435\u043d\u0438\u044f\u00bb'
                },
                {
                    code: 'AssistPostauth',
                    id: 11,
                    name:
                        '\u0423\u043f\u0440\u0430\u0432\u043b\u0435\u043d\u0438\u0435 \u043f\u043e\u0441\u0442\u0430\u0432\u0442\u043e\u0440\u0438\u0437\u0430\u0446\u0438\u0435\u0439'
                },
                {
                    code: 'OEBSReexportPerson',
                    id: 11225,
                    name:
                        '\u041f\u0435\u0440\u0435\u043f\u043e\u0441\u0442\u0430\u0432\u0438\u0442\u044c \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u0430 \u0432 \u043e\u0447\u0435\u0440\u0435\u0434\u044c \u044d\u043a\u0441\u043f\u043e\u0440\u0442\u0430 OEBS'
                },
                {
                    code: 'ViewAllInvoices',
                    id: 1,
                    name:
                        '\u0423\u0441\u0442\u0430\u0440\u0435\u0432\u0448\u0435\u0435, \u043d\u0435 \u0438\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u0442\u044c. \u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u0441\u0447\u0435\u0442\u043e\u0432.'
                },
                {
                    code: 'invoices_report__manager_info',
                    id: 1006,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u0421\u0447\u0435\u0442\u0430 - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u041c\u0435\u043d\u0435\u0434\u0436\u0435\u0440'
                },
                { code: 'Reversal', id: 9, name: 'Reversal' },
                {
                    code: 'AlterFirmKZTaxi',
                    id: 51,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b \u041e\u041e\u041e \u00ab\u042f\u043d\u0434\u0435\u043a\u0441.\u0422\u0430\u043a\u0441\u0438 \u041a\u0430\u0437\u0430\u0445\u0441\u0442\u0430\u043d\u00bb'
                },
                {
                    code: 'RecalculateOverdraft',
                    id: 42,
                    name:
                        '\u041f\u0435\u0440\u0435\u0440\u0430\u0441\u0447\u0435\u0442 \u043e\u0432\u0435\u0440\u0434\u0440\u0430\u0444\u0442\u0430'
                },
                {
                    code: 'ui_orders_report__client_id',
                    id: 1008,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u0417\u0430\u043a\u0430\u0437\u044b \u0432 \u041f\u0418 - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 ID \u043a\u043b\u0438\u0435\u043d\u0442\u0430'
                },
                {
                    code: 'ViewAllManagerReports',
                    id: 12,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442\u044b \u0434\u043b\u044f \u043c\u0435\u043d\u0435\u0434\u0436\u0435\u0440\u043e\u0432'
                },
                {
                    code: 'GrantAllPermissionsToAll',
                    id: 7,
                    name:
                        '\u041d\u0430\u0437\u043d\u0430\u0447\u0435\u043d\u0438\u0435 \u043f\u043e\u043b\u043d\u043e\u043c\u043e\u0447\u0438\u0439'
                },
                {
                    code: 'ProcessAllInvoices',
                    id: 8,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0441\u0447\u0435\u0442\u043e\u0432'
                },
                {
                    code: 'ViewPayments',
                    id: 11326,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u043f\u043b\u0430\u0442\u0435\u0436\u0435\u0439'
                },
                {
                    code: 'ManageBadDebts',
                    id: 21,
                    name:
                        '\u041f\u0440\u0438\u0437\u043d\u0430\u043d\u0438\u0435 "\u043f\u043b\u043e\u0445\u043e\u0433\u043e" \u0434\u043e\u043b\u0433\u0430'
                },
                {
                    code: 'invoices_report__client',
                    id: 1002,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u0421\u0447\u0435\u0442\u0430 - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 ID \u043a\u043b\u0438\u0435\u043d\u0442\u0430'
                },
                {
                    code: 'SuperVisor',
                    id: 15,
                    name:
                        '\u0423\u0441\u0442\u0430\u0440\u0435\u0432\u0448\u0435\u0435, \u043d\u0435 \u0438\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u0442\u044c. SuperWisard'
                },
                {
                    code: 'PersonExtEdit',
                    id: 46,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0432\u0430\u0436\u043d\u044b\u0445 \u043f\u043e\u043b\u0435\u0439 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u0430'
                },
                {
                    code: 'consumes_list__consume_qty',
                    id: 1010,
                    name:
                        '\u0421\u043f\u0438\u0441\u043e\u043a \u0437\u0430\u044f\u0432\u043e\u043a - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u041f\u0435\u0440\u0432\u043e\u043d\u0430\u0447\u0430\u043b\u044c\u043d\u043e\u0435 \u043a\u043e\u043b\u0438\u0447\u0435\u0441\u0442\u0432\u043e'
                },
                {
                    code: 'ViewReports',
                    id: 2,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u043e\u0442\u0447\u0435\u0442\u043e\u0432'
                },
                {
                    code: 'invoices_report__manager',
                    id: 1007,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u0421\u0447\u0435\u0442\u0430 - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u0412\u043a\u043b\u044e\u0447\u0435\u043d\u0438\u0435 \u0437\u0430\u043a\u0430\u0437\u0430'
                },
                {
                    code: 'assist_payments_report__postauth_status',
                    id: 1003,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u041f\u043b\u0430\u0442\u0435\u0436\u0438 \u043f\u043e Assist - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u0421\u0442\u0430\u0442\u0443\u0441 \u043f\u043e\u0441\u0442-\u0430\u0432\u0442\u043e\u0440\u0438\u0437\u0430\u0446\u0438\u0438'
                },
                {
                    code: 'orders_report__client_id',
                    id: 1005,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u0417\u0430\u043a\u0430\u0437\u044b - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 ID \u043a\u043b\u0438\u0435\u043d\u0442\u0430'
                },
                {
                    code: 'ModifyDiscounts',
                    id: 16,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0441\u043a\u0438\u0434\u043e\u043a'
                },
                {
                    code: 'AlterAllInvoices',
                    id: 10,
                    name:
                        '[\u041d\u0435 \u0438\u0441\u043f\u043e\u043b\u044c\u0437\u0443\u0435\u0442\u0441\u044f]\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0441\u0443\u043c\u043c\u044b \u0441\u0447\u0435\u0442\u0430'
                },
                {
                    code: 'deferpays_orders_report__invoice_eid',
                    id: 1013,
                    name:
                        '\u041e\u0442\u0447\u0435\u0442 \u041a\u0440\u0435\u0434\u0438\u0442\u044b - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u0424\u0438\u043a\u0442\u0438\u0432\u043d\u044b\u0439 \u0441\u0447\u0435\u0442'
                },
                {
                    code: 'r_django_fraud_monitoring',
                    id: 62,
                    name:
                        '\u041f\u0440\u0430\u0432\u0430 \u043d\u0430 \u0447\u0442\u0435\u043d\u0438\u0435 Django Fraud Monitoring'
                },
                {
                    code: 'EDOViewer',
                    id: 60,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u0433\u0440\u0430\u0444\u0438\u043a\u0430 \u042d\u0414\u041e'
                },
                {
                    code: 'LocalNamesMaster',
                    id: 59,
                    name:
                        '\u0420\u0435\u0434\u0430\u043a\u0442\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0435 \u043b\u043e\u043a\u0430\u043b\u0438\u0437\u043e\u0432\u0430\u043d\u043d\u044b\u0445 \u0430\u0442\u0440\u0438\u0431\u0443\u0442\u043e\u0432 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u043e\u0432'
                },
                {
                    code: 'ViewOrders',
                    id: 11201,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u0437\u0430\u043a\u0430\u0437\u043e\u0432'
                },
                {
                    code: 'IssueAnyInvoices',
                    id: 17,
                    name:
                        '\u0414\u043e\u0432\u044b\u0441\u0442\u0430\u0432\u043b\u0435\u043d\u0438\u0435 \u0441\u0447\u0435\u0442\u0430 \u0437\u0430 \u043a\u043e\u0433\u043e \u0443\u0433\u043e\u0434\u043d\u043e'
                },
                {
                    code: 'EditContracts',
                    id: 20,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u043e\u0432'
                },
                {
                    code: 'CreateBankPayments',
                    id: 3,
                    name:
                        '\u0412\u043d\u0435\u0441\u0435\u043d\u0438\u0435 \u043f\u043b\u0430\u0442\u0435\u0436\u0435\u0439 \u0447\u0435\u0440\u0435\u0437 \u0431\u0430\u043d\u043a'
                }
            ]
        }
    },
    {
        request: [
            `${HOST}/export/get-state`,
            {
                classname: 'Act',
                queue_type: 'OEBS',
                object_id: 124863523
            },
            false,
            false
        ],
        response: {
            version: { 'yb-snout-api': '3.59.2' },
            data: { export_dt: null, state: 'WAITING' }
        }
    }
];

export const act124856267 = [
    {
        request: [`${HOST}/act`, { act_id: '124856267', act_eid: undefined }, false, false],
        response: {
            version: { 'yb-snout-api': '3.59.2' },
            data: {
                export_dt: null,
                rows: [
                    {
                        consume: {
                            seqnum: 0,
                            unit_name: '\u0443.\u0435.',
                            discount_pct: '0.00',
                            order: {
                                text: 'Py_Test order 7-1475',
                                nds: 1,
                                service_order_id: 56755523,
                                id: 3432881513,
                                service: {
                                    cc: 'PPC',
                                    id: 7,
                                    name:
                                        '\u0414\u0438\u0440\u0435\u043a\u0442: \u0420\u0435\u043a\u043b\u0430\u043c\u043d\u044b\u0435 \u043a\u0430\u043c\u043f\u0430\u043d\u0438\u0438'
                                }
                            }
                        },
                        price: '30.0000',
                        amount: '1500.00',
                        manager: {
                            manager_code: 20453,
                            name:
                                '\u041f\u0435\u0440\u0430\u043d\u0438\u0434\u0437\u0435 \u041d\u0443\u0433\u0437\u0430\u0440 \u0413\u0435\u043e\u0440\u0433\u0438\u0435\u0432\u0438\u0447'
                        },
                        finish_sum: '1500.00',
                        netting: null,
                        consume_nds: null,
                        act_sum: '1500.00',
                        id: 767148672,
                        quantity: '50.000000'
                    }
                ],
                is_trp: null,
                oebs_exportable: true,
                memo: null,
                is_docs_detailed: false,
                factura: null,
                amount: '1500.00',
                good_debt: '0',
                invoice: {
                    firm: { id: 1 },
                    postpay: false,
                    receipt_sum_1c: '3000.00',
                    total_sum: '3000.00',
                    paysys: { id: 1003 },
                    currency: 'RUR',
                    person: {
                        id: 12323099,
                        name:
                            '\u041e\u041e\u041e "\u041f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a"'
                    },
                    client: {
                        id: 1338478719,
                        name: '\u041e\u041e\u041e "\u041a\u043b\u0438\u0435\u043d\u0442"'
                    },
                    iso_currency: 'RUB',
                    external_id: '\u0411-3310164865-1',
                    id: 118597946
                },
                is_docs_separated: false,
                id: 124856267,
                hidden: true,
                act_sum: '1500.00',
                external_id: '153724761',
                dt: '2020-08-31T00:00:00'
            }
        }
    },
    {
        request: [
            `${HOST}/user/object-permissions`,
            { classname: 'Act', object_id: 124856267 },
            false,
            false
        ],
        response: {
            version: { 'yb-snout-api': '3.59.2' },
            data: [
                {
                    code: 'EditClient',
                    id: 28,
                    name:
                        '\u0420\u0435\u0434\u0430\u043a\u0442\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0435 \u043a\u043b\u0438\u0435\u043d\u0442\u0430'
                },
                {
                    code: 'CreateRequestsShop',
                    id: 11101,
                    name:
                        '\u0412\u044b\u0441\u0442\u0430\u0432\u043b\u0435\u043d\u0438\u0435 \u0441\u0447\u0435\u0442\u043e\u0432 \u0447\u0435\u0440\u0435\u0437 \u043c\u0430\u0433\u0430\u0437\u0438\u043d'
                },
                {
                    code: 'ViewInvoices',
                    id: 11105,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u0441\u0447\u0435\u0442\u043e\u0432'
                },
                {
                    code: 'ChangeUnusedFundsLock',
                    id: 11227,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0438\u0442\u044c \u043f\u0440\u0438\u0447\u0438\u043d\u0443 \u0441\u043d\u044f\u0442\u0438\u044f \u0431\u0435\u0437\u0437\u0430\u043a\u0430\u0437\u044c\u044f \u0443 \u0441\u0447\u0451\u0442\u0430'
                },
                {
                    code: 'SendInvoices',
                    id: 11102,
                    name:
                        '\u041e\u0442\u043f\u0440\u0430\u0432\u043a\u0430 \u0441\u0447\u0435\u0442\u043e\u0432 \u043d\u0430 \u043f\u043e\u0447\u0442\u0443'
                },
                {
                    code: 'ViewFishes',
                    id: 22,
                    name: '\u0424\u0438\u0448\u043a\u043e\u0432\u0438\u0434\u0435\u0446'
                },
                {
                    code: 'OEBSReexportAct',
                    id: 11224,
                    name:
                        '\u041f\u0435\u0440\u0435\u043f\u043e\u0441\u0442\u0430\u0432\u0438\u0442\u044c \u0430\u043a\u0442 \u0432 \u043e\u0447\u0435\u0440\u0435\u0434\u044c \u044d\u043a\u0441\u043f\u043e\u0440\u0442\u0430 OEBS'
                },
                {
                    code: 'ManageBadDebts',
                    id: 21,
                    name:
                        '\u041f\u0440\u0438\u0437\u043d\u0430\u043d\u0438\u0435 "\u043f\u043b\u043e\u0445\u043e\u0433\u043e" \u0434\u043e\u043b\u0433\u0430'
                },
                {
                    code: 'AlterInvoicePerson',
                    id: 11110,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u0430 \u0441\u0447\u0435\u0442\u0430'
                },
                {
                    code: 'NotifyOrder',
                    id: 11281,
                    name:
                        '\u041e\u0442\u043f\u0440\u0430\u0432\u0438\u0442\u044c \u043d\u043e\u0442\u0438\u0444\u0438\u043a\u0430\u0446\u0438\u044e \u043f\u043e \u0437\u0430\u043a\u0430\u0437\u0443'
                },
                {
                    code: 'UseAdminPersons',
                    id: 11112,
                    name:
                        '\u0418\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u043d\u0438\u0435 \u043e\u0441\u043e\u0431\u044b\u0445 \u0442\u0438\u043f\u043e\u0432 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u043e\u0432'
                },
                {
                    code: 'ChangeRepaymentsStatus',
                    id: 11103,
                    name:
                        '\u041f\u043e\u0434\u0442\u0432\u0435\u0440\u0436\u0434\u0435\u043d\u0438\u0435 \u0441\u0447\u0435\u0442\u043e\u0432 \u043d\u0430 \u043f\u043e\u0433\u0430\u0448\u0435\u043d\u0438\u0435'
                },
                {
                    code: 'CreateBankPayments',
                    id: 3,
                    name:
                        '\u0412\u043d\u0435\u0441\u0435\u043d\u0438\u0435 \u043f\u043b\u0430\u0442\u0435\u0436\u0435\u0439 \u0447\u0435\u0440\u0435\u0437 \u0431\u0430\u043d\u043a'
                },
                {
                    code: 'AlterInvoiceDate',
                    id: 11108,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u0430\u0442\u044b \u0441\u0447\u0435\u0442\u0430'
                },
                {
                    code: 'AlterAllInvoices',
                    id: 10,
                    name:
                        '[\u041d\u0435 \u0438\u0441\u043f\u043e\u043b\u044c\u0437\u0443\u0435\u0442\u0441\u044f]\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0441\u0443\u043c\u043c\u044b \u0441\u0447\u0435\u0442\u0430'
                },
                {
                    code: 'OEBSReexportInvoice',
                    id: 11223,
                    name:
                        '\u041f\u0435\u0440\u0435\u043f\u043e\u0441\u0442\u0430\u0432\u0438\u0442\u044c \u0441\u0447\u0451\u0442 \u0432 \u043e\u0447\u0435\u0440\u0435\u0434\u044c \u044d\u043a\u0441\u043f\u043e\u0440\u0442\u0430 OEBS'
                },
                {
                    code: 'AlterFirmRU',
                    id: 31,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b \u041e\u041e\u041e \u00ab\u042f\u043d\u0434\u0435\u043a\u0441\u00bb'
                },
                {
                    code: 'PaymentLinksReport',
                    id: 11104,
                    name:
                        '\u041f\u043e\u0438\u0441\u043a \u043f\u043e \u043f\u043b\u0430\u0442\u0435\u0436\u0430\u043c'
                },
                {
                    code: 'AlterInvoiceContract',
                    id: 11111,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0430 \u0441\u0447\u0435\u0442\u0430'
                },
                {
                    code: 'UseRestrictedINN',
                    id: 11301,
                    name:
                        '\u0418\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u043d\u0438\u0435 \u0437\u0430\u043f\u0440\u0435\u0449\u0451\u043d\u043d\u044b\u0445 \u0418\u041d\u041d'
                },
                {
                    code: 'DealPassport',
                    id: 30,
                    name: '\u0420\u0430\u0431\u043e\u0442\u0430 \u0441 \u041f\u0421'
                },
                {
                    code: 'SendActs',
                    id: 11323,
                    name:
                        '\u041e\u0442\u043f\u0440\u0430\u0432\u0438\u0442\u044c \u0430\u043a\u0442\u044b \u043f\u043e \u044d\u043b\u0435\u043a\u0442\u0440\u043e\u043d\u043d\u043e\u0439 \u0438\u043b\u0438 \u043e\u0431\u044b\u043d\u043e\u0439 \u043f\u043e\u0447\u0442\u0435'
                },
                {
                    code: 'AssistPostauth',
                    id: 11,
                    name:
                        '\u0423\u043f\u0440\u0430\u0432\u043b\u0435\u043d\u0438\u0435 \u043f\u043e\u0441\u0442\u0430\u0432\u0442\u043e\u0440\u0438\u0437\u0430\u0446\u0438\u0435\u0439'
                },
                {
                    code: 'BillingSupport',
                    id: 1100,
                    name:
                        '\u0421\u0430\u043f\u043f\u043e\u0440\u0442 \u0411\u0438\u043b\u043b\u0438\u043d\u0433\u0430'
                },
                {
                    code: 'OEBSReexportContract',
                    id: 11226,
                    name:
                        '\u041f\u0435\u0440\u0435\u043f\u043e\u0441\u0442\u0430\u0432\u0438\u0442\u044c \u0434\u043e\u0433\u043e\u0432\u043e\u0440/\u0434\u043e\u043f.\u0441\u043e\u0433\u043b\u0430\u0448\u0435\u043d\u0438\u0435 \u0432 \u043e\u0447\u0435\u0440\u0435\u0434\u044c \u044d\u043a\u0441\u043f\u043e\u0440\u0442\u0430 OEBS'
                },
                {
                    code: 'CloseInvoice',
                    id: 11222,
                    name:
                        '\u0417\u0430\u043a\u0440\u044b\u0442\u0438\u0435 \u0441\u0447\u0451\u0442\u0430'
                },
                {
                    code: 'IssueInvoices',
                    id: 11106,
                    name:
                        '\u0412\u044b\u0441\u0442\u0430\u0432\u043b\u0435\u043d\u0438\u0435 \u0441\u0447\u0435\u0442\u043e\u0432'
                },
                {
                    code: 'PersonView',
                    id: 25,
                    name:
                        '\u0423\u0441\u0442\u0430\u0440\u0435\u0432\u0448\u0435\u0435, \u043d\u0435 \u0438\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u0442\u044c. \u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u0430.'
                },
                {
                    code: 'PayInvoiceExtension',
                    id: 11221,
                    name:
                        '\u041f\u0435\u0440\u0435\u043d\u0435\u0441\u0442\u0438 \u0441\u0440\u043e\u043a \u043e\u043f\u043b\u0430\u0442\u044b \u0441\u0447\u0451\u0442\u0430'
                },
                {
                    code: 'IssueYTInvoices',
                    id: 14,
                    name:
                        '\u0412\u044b\u0441\u0442\u0430\u0432\u043b\u044f\u0442\u044c \u0441\u0447\u0435\u0442\u0430 \u043d\u0430 Yandex Technologies'
                },
                {
                    code: 'ViewClients',
                    id: 11321,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u043a\u043b\u0438\u0435\u043d\u0442\u043e\u0432'
                },
                {
                    code: 'PersonEdit',
                    id: 26,
                    name:
                        '\u0420\u0435\u0434\u0430\u043a\u0442\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0435 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u0430'
                },
                {
                    code: 'AlterInvoiceSum',
                    id: 11107,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0441\u0443\u043c\u043c\u044b \u0441\u0447\u0435\u0442\u0430'
                },
                {
                    code: 'ViewDevData',
                    id: 11261,
                    name:
                        '\u0414\u043e\u0441\u0442\u0443\u043f \u043a \u0442\u0435\u0445\u043d\u0438\u0447\u0435\u0441\u043a\u043e\u0439 \u0438\u043d\u0444\u043e\u0440\u043c\u0430\u0446\u0438\u0438'
                },
                {
                    code: 'EDOViewer',
                    id: 60,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u0433\u0440\u0430\u0444\u0438\u043a\u0430 \u042d\u0414\u041e'
                },
                {
                    code: 'TransferFromOrder',
                    id: 11141,
                    name:
                        '\u041f\u0435\u0440\u0435\u043d\u043e\u0441 \u0441\u0440\u0435\u0434\u0441\u0442\u0432 c \u0437\u0430\u043a\u0430\u0437\u0430'
                },
                {
                    code: 'ViewOrders',
                    id: 11201,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u0437\u0430\u043a\u0430\u0437\u043e\u0432'
                },
                {
                    code: 'ViewPayments',
                    id: 11326,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u043f\u043b\u0430\u0442\u0435\u0436\u0435\u0439'
                },
                {
                    code: 'consumes_list__consume_qty',
                    id: 1010,
                    name:
                        '\u0421\u043f\u0438\u0441\u043e\u043a \u0437\u0430\u044f\u0432\u043e\u043a - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u041f\u0435\u0440\u0432\u043e\u043d\u0430\u0447\u0430\u043b\u044c\u043d\u043e\u0435 \u043a\u043e\u043b\u0438\u0447\u0435\u0441\u0442\u0432\u043e'
                },
                {
                    code: 'SetClientOverdraftBan',
                    id: 11325,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0438\u0442\u044c \u0431\u0430\u043d \u043e\u0432\u0435\u0440\u0434\u0440\u0430\u0444\u0442\u0430 \u0434\u043b\u044f \u043a\u043b\u0438\u0435\u043d\u0442\u0430'
                },
                {
                    code: 'SubstituteLogin',
                    id: 24,
                    name:
                        '\u041f\u043e\u0434\u043c\u0435\u043d\u0430 \u043b\u043e\u0433\u0438\u043d\u0430'
                },
                {
                    code: 'ViewContracts',
                    id: 11181,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u043e\u0432'
                },
                {
                    code: 'CreateClient',
                    id: 27,
                    name:
                        '\u0421\u043e\u0437\u0434\u0430\u043d\u0438\u0435 \u043a\u043b\u0438\u0435\u043d\u0442\u0430'
                },
                {
                    code: 'ViewProduct',
                    id: 11241,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u043f\u0440\u043e\u0434\u0443\u043a\u0442\u043e\u0432'
                },
                {
                    code: 'DoInvoiceRefundsTrust',
                    id: 11161,
                    name:
                        '\u0421\u043e\u0437\u0434\u0430\u043d\u0438\u0435 \u0432\u043e\u0437\u0432\u0440\u0430\u0442\u043e\u0432 \u043f\u043e \u0441\u0447\u0435\u0442\u0430\u043c \u0447\u0435\u0440\u0435\u0437 \u0422\u0440\u0430\u0441\u0442'
                },
                {
                    code: 'GrantAllPermissionsToAll',
                    id: 7,
                    name:
                        '\u041d\u0430\u0437\u043d\u0430\u0447\u0435\u043d\u0438\u0435 \u043f\u043e\u043b\u043d\u043e\u043c\u043e\u0447\u0438\u0439'
                },
                {
                    code: 'AlterInvoicePaysys',
                    id: 11109,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0441\u043f\u043e\u0441\u043e\u0431\u0430 \u043e\u043f\u043b\u0430\u0442\u044b \u0441\u0447\u0435\u0442\u0430'
                },
                {
                    code: 'WithdrawConsumesPrepay',
                    id: 67,
                    name:
                        '\u0421\u043d\u044f\u0442\u0438\u0435 \u0437\u0430\u0447\u0438\u0441\u043b\u0435\u043d\u0438\u0439 \u0441 \u043f\u0440\u0435\u0434\u043e\u043f\u043b\u0430\u0442\u043d\u044b\u0445 \u0441\u0447\u0435\u0442\u043e\u0432'
                },
                {
                    code: 'SetMcbFlags',
                    id: 11324,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0438\u0442\u044c \u043a\u0430\u0442\u0435\u0433\u043e\u0440\u0438\u0438 \u0434\u043b\u044f \u043f\u043b\u043e\u0449\u0430\u0434\u043e\u043a \u043a\u043b\u0438\u0435\u043d\u0442\u043e\u0432'
                },
                {
                    code: 'ViewAllManagerReports',
                    id: 12,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442\u044b \u0434\u043b\u044f \u043c\u0435\u043d\u0435\u0434\u0436\u0435\u0440\u043e\u0432'
                },
                {
                    code: 'PersonExtEdit',
                    id: 46,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0432\u0430\u0436\u043d\u044b\u0445 \u043f\u043e\u043b\u0435\u0439 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u0430'
                },
                {
                    code: 'assist_payments_report__postauth_status',
                    id: 1003,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u041f\u043b\u0430\u0442\u0435\u0436\u0438 \u043f\u043e Assist - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u0421\u0442\u0430\u0442\u0443\u0441 \u043f\u043e\u0441\u0442-\u0430\u0432\u0442\u043e\u0440\u0438\u0437\u0430\u0446\u0438\u0438'
                },
                {
                    code: 'AlterFirmUATaxi',
                    id: 49,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b \u041e\u041e\u041e "\u042f\u043d\u0434\u0435\u043a\u0441.\u0422\u0430\u043a\u0441\u0438 \u0423\u043a\u0440\u0430\u0438\u043d\u0430"'
                },
                {
                    code: 'OEBSReexportPerson',
                    id: 11225,
                    name:
                        '\u041f\u0435\u0440\u0435\u043f\u043e\u0441\u0442\u0430\u0432\u0438\u0442\u044c \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u0430 \u0432 \u043e\u0447\u0435\u0440\u0435\u0434\u044c \u044d\u043a\u0441\u043f\u043e\u0440\u0442\u0430 OEBS'
                },
                {
                    code: 'ui_orders_report__client_id',
                    id: 1008,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u0417\u0430\u043a\u0430\u0437\u044b \u0432 \u041f\u0418 - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 ID \u043a\u043b\u0438\u0435\u043d\u0442\u0430'
                },
                {
                    code: 'ProcessAllInvoices',
                    id: 8,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0441\u0447\u0435\u0442\u043e\u0432'
                },
                {
                    code: 'AlterFirmAUTORU',
                    id: 45,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b \u041e\u041e\u041e \u00ab\u0410\u0412\u0422\u041e.\u0420\u0423 \u0425\u043e\u043b\u0434\u0438\u043d\u0433\u00bb'
                },
                {
                    code: 'EditINN',
                    id: 43,
                    name:
                        '\u0417\u0430\u0432\u0435\u0434\u0435\u043d\u0438\u0435 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u0430 \u0441 \u0415\u0413\u0420\u041f\u041e\u0423 \u0432\u043c\u0435\u0441\u0442\u043e \u0418\u041d\u041d'
                },
                {
                    code: 'ViewReports',
                    id: 2,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u043e\u0442\u0447\u0435\u0442\u043e\u0432'
                },
                {
                    code: 'AlterFirmKZ',
                    id: 33,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b \u0422\u041e\u041e \u00abKazNet Media (\u041a\u0430\u0437\u041d\u0435\u0442 \u041c\u0435\u0434\u0438\u0430)\u00bb'
                },
                {
                    code: 'AlterFirmAMTaxi',
                    id: 53,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b \u041e\u041e\u041e \u00ab\u042f\u043d\u0434\u0435\u043a\u0441.\u0422\u0430\u043a\u0441\u0438 \u0410\u0440\u043c\u0435\u043d\u0438\u044f\u00bb'
                },
                {
                    code: 'AlterFirmKZTaxi',
                    id: 51,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b \u041e\u041e\u041e \u00ab\u042f\u043d\u0434\u0435\u043a\u0441.\u0422\u0430\u043a\u0441\u0438 \u041a\u0430\u0437\u0430\u0445\u0441\u0442\u0430\u043d\u00bb'
                },
                {
                    code: 'consumes_list__consume_sum',
                    id: 1011,
                    name:
                        '\u0421\u043f\u0438\u0441\u043e\u043a \u0437\u0430\u044f\u0432\u043e\u043a - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u041f\u0435\u0440\u0432\u043e\u043d\u0430\u0447\u0430\u043b\u044c\u043d\u0430\u044f \u0441\u0443\u043c\u043c\u0430'
                },
                {
                    code: 'invoices_report__manager',
                    id: 1007,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u0421\u0447\u0435\u0442\u0430 - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u0412\u043a\u043b\u044e\u0447\u0435\u043d\u0438\u0435 \u0437\u0430\u043a\u0430\u0437\u0430'
                },
                {
                    code: 'TransferBetweenClients',
                    id: 11143,
                    name:
                        '\u041f\u0435\u0440\u0435\u043d\u043e\u0441 \u0441\u0440\u0435\u0434\u0441\u0442\u0432 \u043c\u0435\u0436\u0434\u0443 \u0440\u0430\u0437\u043d\u044b\u043c\u0438 \u043a\u043b\u0438\u0435\u043d\u0442\u0430\u043c\u0438'
                },
                {
                    code: 'AlterFirmEU',
                    id: 37,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b Yandex Europe B.V.'
                },
                {
                    code: 'TransferUnusedFunds',
                    id: 11121,
                    name:
                        '\u041f\u0435\u0440\u0435\u043d\u043e\u0441 \u0441\u0432\u043e\u0431\u043e\u0434\u043d\u044b\u0445 \u0441\u0440\u0435\u0434\u0441\u0442\u0432'
                },
                {
                    code: 'ui_orders_report__manager_code',
                    id: 1009,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u0417\u0430\u043a\u0430\u0437\u044b \u0432 \u041f\u0418 - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u041c\u0435\u043d\u0435\u0434\u0436\u0435\u0440'
                },
                {
                    code: 'AlterFirmSW',
                    id: 39,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b Yandex Europe AG'
                },
                {
                    code: 'AdminAccess',
                    id: 0,
                    name:
                        '\u0414\u043e\u0441\u0442\u0443\u043f \u043a \u0430\u0434\u043c\u0438\u043d\u0438\u0441\u0442\u0440\u0430\u0442\u0438\u0432\u043d\u043e\u043c\u0443 \u0440\u0435\u0436\u0438\u043c\u0443'
                },
                {
                    code: 'AdditionalFunctions',
                    id: 47,
                    name:
                        '\u0414\u043e\u043f\u043e\u043b\u043d\u0438\u0442\u0435\u043b\u044c\u043d\u044b\u0435 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438'
                },
                {
                    code: 'DoInvoiceRefunds',
                    id: 4004,
                    name:
                        '\u0421\u043e\u0437\u0434\u0430\u043d\u0438\u0435 \u0432\u043e\u0437\u0432\u0440\u0430\u0442\u043e\u0432 \u043f\u043e \u0441\u0447\u0435\u0442\u0430\u043c'
                },
                {
                    code: 'AlterFirmNVTaxi',
                    id: 48,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b Yandex.Taxi B.V.'
                },
                { code: 'Reversal', id: 9, name: 'Reversal' },
                {
                    code: 'IssueAnyInvoices',
                    id: 17,
                    name:
                        '\u0414\u043e\u0432\u044b\u0441\u0442\u0430\u0432\u043b\u0435\u043d\u0438\u0435 \u0441\u0447\u0435\u0442\u0430 \u0437\u0430 \u043a\u043e\u0433\u043e \u0443\u0433\u043e\u0434\u043d\u043e'
                },
                {
                    code: 'ClientFraudStatusEdit',
                    id: 58,
                    name:
                        '\u0420\u0435\u0434\u0430\u043a\u0442\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0435 \u043a\u043b\u0438\u0435\u043d\u0442\u0430: \u043f\u0440\u043e\u0441\u0442\u0430\u043d\u043e\u0432\u043a\u0430 \u0444\u043b\u0430\u0433\u0430 deny_cc / fraud_status'
                },
                {
                    code: 'invoices_report__manager_info',
                    id: 1006,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u0421\u0447\u0435\u0442\u0430 - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u041c\u0435\u043d\u0435\u0434\u0436\u0435\u0440'
                },
                {
                    code: 'order_info__order_id',
                    id: 1012,
                    name:
                        '\u0421\u0432\u043e\u0439\u0441\u0442\u0432\u0430 \u0437\u0430\u043a\u0430\u0437\u0430 - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u0412\u043d\u0443\u0442\u0440\u0435\u043d\u043d\u0438\u0439 ID \u0437\u0430\u043a\u0430\u0437\u0430'
                },
                {
                    code: 'AlterFirmUS',
                    id: 34,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b Yandex Inc'
                },
                {
                    code: 'RecalculateOverdraft',
                    id: 42,
                    name:
                        '\u041f\u0435\u0440\u0435\u0440\u0430\u0441\u0447\u0435\u0442 \u043e\u0432\u0435\u0440\u0434\u0440\u0430\u0444\u0442\u0430'
                },
                {
                    code: 'ModifyDiscounts',
                    id: 16,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0441\u043a\u0438\u0434\u043e\u043a'
                },
                {
                    code: 'ManageRBSPayments',
                    id: 19,
                    name:
                        '\u041e\u043f\u0435\u0440\u0430\u0446\u0438\u0438 \u0441 \u043f\u043b\u0430\u0442\u0435\u0436\u0430\u043c\u0438 \u043f\u043e RBS'
                },
                {
                    code: 'AlterFirmUA',
                    id: 32,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b \u041e\u041e\u041e \u00ab\u042f\u043d\u0434\u0435\u043a\u0441.\u0423\u043a\u0440\u0430\u0438\u043d\u0430\u00bb'
                },
                {
                    code: 'invoices_report__client',
                    id: 1002,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u0421\u0447\u0435\u0442\u0430 - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 ID \u043a\u043b\u0438\u0435\u043d\u0442\u0430'
                },
                {
                    code: 'AlterFirmHK',
                    id: 65,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b Yandex E-commerce Limited'
                },
                {
                    code: 'WithdrawConsumesPostpay',
                    id: 66,
                    name:
                        '\u0421\u043d\u044f\u0442\u0438\u0435 \u0437\u0430\u0447\u0438\u0441\u043b\u0435\u043d\u0438\u0439 \u0441 \u043f\u043e\u0441\u0442\u043e\u043f\u043b\u0430\u0442\u043d\u044b\u0445 \u0441\u0447\u0435\u0442\u043e\u0432'
                },
                {
                    code: 'TerminalView',
                    id: 56,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u0442\u0435\u0440\u043c\u0438\u043d\u0430\u043b\u0430'
                },
                {
                    code: 'orders_report__client_id',
                    id: 1005,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u0417\u0430\u043a\u0430\u0437\u044b - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 ID \u043a\u043b\u0438\u0435\u043d\u0442\u0430'
                },
                {
                    code: 'ViewAllInvoices',
                    id: 1,
                    name:
                        '\u0423\u0441\u0442\u0430\u0440\u0435\u0432\u0448\u0435\u0435, \u043d\u0435 \u0438\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u0442\u044c. \u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u0441\u0447\u0435\u0442\u043e\u0432.'
                },
                {
                    code: 'EditContracts',
                    id: 20,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u043e\u0432'
                },
                {
                    code: 'assist_payments_report__fraud_dt',
                    id: 1004,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u041f\u043b\u0430\u0442\u0435\u0436\u0438 \u043f\u043e Assist - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u0414\u0430\u0442\u0430 \u043e\u0431\u043d\u0430\u0440\u0443\u0436\u0435\u043d\u0438\u044f \u0444\u0440\u043e\u0434\u0430'
                },
                {
                    code: 'CreateCerificatePayments',
                    id: 4,
                    name:
                        '\u0412\u043d\u0435\u0441\u0435\u043d\u0438\u0435 \u043f\u043b\u0430\u0442\u0435\u0436\u0435\u0439 \u0441\u0435\u0440\u0442\u0438\u0444\u0438\u043a\u0430\u0442\u043e\u043c, \u0431\u0430\u0440\u0442\u0435\u0440\u043e\u043c'
                },
                {
                    code: 'LocalNamesMaster',
                    id: 59,
                    name:
                        '\u0420\u0435\u0434\u0430\u043a\u0442\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0435 \u043b\u043e\u043a\u0430\u043b\u0438\u0437\u043e\u0432\u0430\u043d\u043d\u044b\u0445 \u0430\u0442\u0440\u0438\u0431\u0443\u0442\u043e\u0432 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u043e\u0432'
                },
                {
                    code: 'ApexReports',
                    id: 3001,
                    name: '\u0410\u0440\u0435\u0445. \u041e\u0442\u0447\u0435\u0442\u044b'
                },
                {
                    code: 'TransferFromInvoice',
                    id: 11142,
                    name:
                        '\u041f\u0435\u0440\u0435\u043d\u043e\u0441 \u0441\u0440\u0435\u0434\u0441\u0442\u0432 \u0441\u043e \u0441\u0447\u0435\u0442\u0430'
                },
                {
                    code: 'AlterFirmNVTaxiuber',
                    id: 63,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b Uber ML B.V.'
                },
                {
                    code: 'invoices_report__receipt_sum_1c',
                    id: 1001,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u0421\u0447\u0435\u0442\u0430 - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u0421\u0443\u043c\u043c\u0430 \u043f\u0440\u0438\u0445\u043e\u0434\u0430 \u0438\u0437 1\u0421'
                },
                {
                    code: 'ViewPersons',
                    id: 11322,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u043e\u0432'
                },
                {
                    code: 'deferpays_orders_report__invoice_eid',
                    id: 1013,
                    name:
                        '\u041e\u0442\u0447\u0435\u0442 \u041a\u0440\u0435\u0434\u0438\u0442\u044b - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u0424\u0438\u043a\u0442\u0438\u0432\u043d\u044b\u0439 \u0441\u0447\u0435\u0442'
                },
                {
                    code: 'r_django_fraud_monitoring',
                    id: 62,
                    name:
                        '\u041f\u0440\u0430\u0432\u0430 \u043d\u0430 \u0447\u0442\u0435\u043d\u0438\u0435 Django Fraud Monitoring'
                },
                {
                    code: 'RepresentClient',
                    id: 2000,
                    name:
                        '\u0414\u0435\u0439\u0441\u0442\u0432\u0438\u044f \u043e\u0442 \u0438\u043c\u0435\u043d\u0438 \u043a\u043b\u0438\u0435\u043d\u0442\u0430'
                },
                {
                    code: 'TerminalEdit',
                    id: 57,
                    name:
                        '\u0420\u0435\u0434\u0430\u043a\u0442\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0435 \u0442\u0435\u0440\u043c\u0438\u043d\u0430\u043b\u0430'
                },
                {
                    code: 'PersonPostAddressEdit',
                    id: 38,
                    name:
                        '\u0420\u0435\u0434\u0430\u043a\u0442\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0435 \u043b\u044e\u0431\u044b\u0445 \u043f\u043e\u043b\u0435\u0439 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u0430'
                },
                {
                    code: 'AlterFirmTR',
                    id: 41,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b Yandex Turkey'
                },
                {
                    code: 'SuperVisor',
                    id: 15,
                    name:
                        '\u0423\u0441\u0442\u0430\u0440\u0435\u0432\u0448\u0435\u0435, \u043d\u0435 \u0438\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u0442\u044c. SuperWisard'
                },
                {
                    code: 'AlterPrintTemplate',
                    id: 55,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0448\u0430\u0431\u043b\u043e\u043d\u0430 \u043f\u0435\u0447\u0430\u0442\u043d\u043e\u0439 \u0444\u043e\u0440\u043c\u044b \u0432 \u0434\u043e\u0433\u043e\u0432\u0440\u0435'
                },
                {
                    code: 'IssueUSDInvoices',
                    id: 13,
                    name:
                        '\u0412\u044b\u0441\u0442\u0430\u0432\u043b\u044f\u0442\u044c \u0434\u043e\u043b\u043b\u0430\u0440\u043e\u0432\u044b\u0435 \u0441\u0447\u0435\u0442\u0430'
                },
                {
                    code: 'ManagersOperations',
                    id: 23,
                    name:
                        '\u0414\u0435\u0439\u0441\u0442\u0432\u0438\u044f \u043c\u0435\u043d\u0435\u0434\u0436\u0435\u0440\u0430'
                }
            ]
        }
    },
    {
        request: [
            `${HOST}/export/get-state`,
            {
                classname: 'Act',
                queue_type: 'OEBS',
                object_id: 124856267
            },
            false,
            false
        ],
        response: {
            version: { 'yb-snout-api': '3.59.2' },
            data: { export_dt: null, state: 'WAITING' }
        }
    }
];

export const act124863507 = [
    {
        request: [`${HOST}/act`, { act_id: '124863507', act_eid: undefined }, false, false],
        response: {
            version: { 'yb-snout-api': '3.59.2' },
            data: {
                export_dt: null,
                rows: [
                    {
                        consume: {
                            seqnum: 0,
                            unit_name: '\u0443.\u0435.',
                            discount_pct: '0.00',
                            order: {
                                text: 'Py_Test order 7-1475',
                                nds: 1,
                                service_order_id: 56763986,
                                id: 3432956413,
                                service: {
                                    cc: 'PPC',
                                    id: 7,
                                    name:
                                        '\u0414\u0438\u0440\u0435\u043a\u0442: \u0420\u0435\u043a\u043b\u0430\u043c\u043d\u044b\u0435 \u043a\u0430\u043c\u043f\u0430\u043d\u0438\u0438'
                                }
                            }
                        },
                        price: '30.0000',
                        amount: '1500.00',
                        manager: {
                            manager_code: 20453,
                            name:
                                '\u041f\u0435\u0440\u0430\u043d\u0438\u0434\u0437\u0435 \u041d\u0443\u0433\u0437\u0430\u0440 \u0413\u0435\u043e\u0440\u0433\u0438\u0435\u0432\u0438\u0447'
                        },
                        finish_sum: '1500.00',
                        netting: null,
                        consume_nds: null,
                        act_sum: '1500.00',
                        id: 767166284,
                        quantity: '50.000000'
                    }
                ],
                is_trp: null,
                oebs_exportable: true,
                memo: null,
                is_docs_detailed: false,
                factura: null,
                amount: '1500.00',
                good_debt: '0',
                invoice: {
                    firm: { id: 1 },
                    postpay: false,
                    receipt_sum_1c: '3000.00',
                    total_sum: '3000.00',
                    paysys: { id: 1003 },
                    currency: 'RUR',
                    person: {
                        id: 12342396,
                        name:
                            '\u041e\u041e\u041e "\u041f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a"'
                    },
                    client: {
                        id: 1338514298,
                        name: '\u041e\u041e\u041e "\u041a\u043b\u0438\u0435\u043d\u0442"'
                    },
                    iso_currency: 'RUB',
                    external_id: '\u0411-3310226367-1',
                    id: 118619410
                },
                is_docs_separated: false,
                id: 124863507,
                hidden: false,
                act_sum: '1500.00',
                external_id: '153731981',
                dt: '2020-08-31T00:00:00'
            }
        }
    },
    {
        request: [
            `${HOST}/user/object-permissions`,
            { classname: 'Act', object_id: 124863507 },
            false,
            false
        ],
        response: {
            version: { 'yb-snout-api': '3.59.2' },
            data: [
                {
                    code: 'invoices_report__receipt_sum_1c',
                    id: 1001,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u0421\u0447\u0435\u0442\u0430 - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u0421\u0443\u043c\u043c\u0430 \u043f\u0440\u0438\u0445\u043e\u0434\u0430 \u0438\u0437 1\u0421'
                },
                { code: 'Reversal', id: 9, name: 'Reversal' },
                {
                    code: 'PersonPostAddressEdit',
                    id: 38,
                    name:
                        '\u0420\u0435\u0434\u0430\u043a\u0442\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0435 \u043b\u044e\u0431\u044b\u0445 \u043f\u043e\u043b\u0435\u0439 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u0430'
                },
                {
                    code: 'ViewAllInvoices',
                    id: 1,
                    name:
                        '\u0423\u0441\u0442\u0430\u0440\u0435\u0432\u0448\u0435\u0435, \u043d\u0435 \u0438\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u0442\u044c. \u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u0441\u0447\u0435\u0442\u043e\u0432.'
                },
                {
                    code: 'NotifyOrder',
                    id: 11281,
                    name:
                        '\u041e\u0442\u043f\u0440\u0430\u0432\u0438\u0442\u044c \u043d\u043e\u0442\u0438\u0444\u0438\u043a\u0430\u0446\u0438\u044e \u043f\u043e \u0437\u0430\u043a\u0430\u0437\u0443'
                },
                {
                    code: 'CreateCerificatePayments',
                    id: 4,
                    name:
                        '\u0412\u043d\u0435\u0441\u0435\u043d\u0438\u0435 \u043f\u043b\u0430\u0442\u0435\u0436\u0435\u0439 \u0441\u0435\u0440\u0442\u0438\u0444\u0438\u043a\u0430\u0442\u043e\u043c, \u0431\u0430\u0440\u0442\u0435\u0440\u043e\u043c'
                },
                {
                    code: 'AlterFirmUA',
                    id: 32,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b \u041e\u041e\u041e \u00ab\u042f\u043d\u0434\u0435\u043a\u0441.\u0423\u043a\u0440\u0430\u0438\u043d\u0430\u00bb'
                },
                {
                    code: 'consumes_list__consume_qty',
                    id: 1010,
                    name:
                        '\u0421\u043f\u0438\u0441\u043e\u043a \u0437\u0430\u044f\u0432\u043e\u043a - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u041f\u0435\u0440\u0432\u043e\u043d\u0430\u0447\u0430\u043b\u044c\u043d\u043e\u0435 \u043a\u043e\u043b\u0438\u0447\u0435\u0441\u0442\u0432\u043e'
                },
                {
                    code: 'AlterFirmKZ',
                    id: 33,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b \u0422\u041e\u041e \u00abKazNet Media (\u041a\u0430\u0437\u041d\u0435\u0442 \u041c\u0435\u0434\u0438\u0430)\u00bb'
                },
                {
                    code: 'ManagersOperations',
                    id: 23,
                    name:
                        '\u0414\u0435\u0439\u0441\u0442\u0432\u0438\u044f \u043c\u0435\u043d\u0435\u0434\u0436\u0435\u0440\u0430'
                },
                {
                    code: 'IssueAnyInvoices',
                    id: 17,
                    name:
                        '\u0414\u043e\u0432\u044b\u0441\u0442\u0430\u0432\u043b\u0435\u043d\u0438\u0435 \u0441\u0447\u0435\u0442\u0430 \u0437\u0430 \u043a\u043e\u0433\u043e \u0443\u0433\u043e\u0434\u043d\u043e'
                },
                {
                    code: 'OEBSReexportAct',
                    id: 11224,
                    name:
                        '\u041f\u0435\u0440\u0435\u043f\u043e\u0441\u0442\u0430\u0432\u0438\u0442\u044c \u0430\u043a\u0442 \u0432 \u043e\u0447\u0435\u0440\u0435\u0434\u044c \u044d\u043a\u0441\u043f\u043e\u0440\u0442\u0430 OEBS'
                },
                {
                    code: 'EditContracts',
                    id: 20,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u043e\u0432'
                },
                {
                    code: 'ui_orders_report__manager_code',
                    id: 1009,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u0417\u0430\u043a\u0430\u0437\u044b \u0432 \u041f\u0418 - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u041c\u0435\u043d\u0435\u0434\u0436\u0435\u0440'
                },
                {
                    code: 'SetClientOverdraftBan',
                    id: 11325,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0438\u0442\u044c \u0431\u0430\u043d \u043e\u0432\u0435\u0440\u0434\u0440\u0430\u0444\u0442\u0430 \u0434\u043b\u044f \u043a\u043b\u0438\u0435\u043d\u0442\u0430'
                },
                {
                    code: 'TransferFromOrder',
                    id: 11141,
                    name:
                        '\u041f\u0435\u0440\u0435\u043d\u043e\u0441 \u0441\u0440\u0435\u0434\u0441\u0442\u0432 c \u0437\u0430\u043a\u0430\u0437\u0430'
                },
                {
                    code: 'ManageBadDebts',
                    id: 21,
                    name:
                        '\u041f\u0440\u0438\u0437\u043d\u0430\u043d\u0438\u0435 "\u043f\u043b\u043e\u0445\u043e\u0433\u043e" \u0434\u043e\u043b\u0433\u0430'
                },
                {
                    code: 'SubstituteLogin',
                    id: 24,
                    name:
                        '\u041f\u043e\u0434\u043c\u0435\u043d\u0430 \u043b\u043e\u0433\u0438\u043d\u0430'
                },
                {
                    code: 'ViewAllManagerReports',
                    id: 12,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442\u044b \u0434\u043b\u044f \u043c\u0435\u043d\u0435\u0434\u0436\u0435\u0440\u043e\u0432'
                },
                {
                    code: 'PersonView',
                    id: 25,
                    name:
                        '\u0423\u0441\u0442\u0430\u0440\u0435\u0432\u0448\u0435\u0435, \u043d\u0435 \u0438\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u0442\u044c. \u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u0430.'
                },
                {
                    code: 'AlterFirmEU',
                    id: 37,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b Yandex Europe B.V.'
                },
                {
                    code: 'ViewReports',
                    id: 2,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u043e\u0442\u0447\u0435\u0442\u043e\u0432'
                },
                {
                    code: 'ModifyDiscounts',
                    id: 16,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0441\u043a\u0438\u0434\u043e\u043a'
                },
                {
                    code: 'AlterFirmUS',
                    id: 34,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b Yandex Inc'
                },
                {
                    code: 'invoices_report__manager_info',
                    id: 1006,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u0421\u0447\u0435\u0442\u0430 - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u041c\u0435\u043d\u0435\u0434\u0436\u0435\u0440'
                },
                {
                    code: 'IssueYTInvoices',
                    id: 14,
                    name:
                        '\u0412\u044b\u0441\u0442\u0430\u0432\u043b\u044f\u0442\u044c \u0441\u0447\u0435\u0442\u0430 \u043d\u0430 Yandex Technologies'
                },
                {
                    code: 'AlterFirmUATaxi',
                    id: 49,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b \u041e\u041e\u041e "\u042f\u043d\u0434\u0435\u043a\u0441.\u0422\u0430\u043a\u0441\u0438 \u0423\u043a\u0440\u0430\u0438\u043d\u0430"'
                },
                {
                    code: 'OEBSReexportPerson',
                    id: 11225,
                    name:
                        '\u041f\u0435\u0440\u0435\u043f\u043e\u0441\u0442\u0430\u0432\u0438\u0442\u044c \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u0430 \u0432 \u043e\u0447\u0435\u0440\u0435\u0434\u044c \u044d\u043a\u0441\u043f\u043e\u0440\u0442\u0430 OEBS'
                },
                {
                    code: 'SendInvoices',
                    id: 11102,
                    name:
                        '\u041e\u0442\u043f\u0440\u0430\u0432\u043a\u0430 \u0441\u0447\u0435\u0442\u043e\u0432 \u043d\u0430 \u043f\u043e\u0447\u0442\u0443'
                },
                {
                    code: 'ViewProduct',
                    id: 11241,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u043f\u0440\u043e\u0434\u0443\u043a\u0442\u043e\u0432'
                },
                {
                    code: 'ManageRBSPayments',
                    id: 19,
                    name:
                        '\u041e\u043f\u0435\u0440\u0430\u0446\u0438\u0438 \u0441 \u043f\u043b\u0430\u0442\u0435\u0436\u0430\u043c\u0438 \u043f\u043e RBS'
                },
                {
                    code: 'AssistPostauth',
                    id: 11,
                    name:
                        '\u0423\u043f\u0440\u0430\u0432\u043b\u0435\u043d\u0438\u0435 \u043f\u043e\u0441\u0442\u0430\u0432\u0442\u043e\u0440\u0438\u0437\u0430\u0446\u0438\u0435\u0439'
                },
                {
                    code: 'OEBSReexportContract',
                    id: 11226,
                    name:
                        '\u041f\u0435\u0440\u0435\u043f\u043e\u0441\u0442\u0430\u0432\u0438\u0442\u044c \u0434\u043e\u0433\u043e\u0432\u043e\u0440/\u0434\u043e\u043f.\u0441\u043e\u0433\u043b\u0430\u0448\u0435\u043d\u0438\u0435 \u0432 \u043e\u0447\u0435\u0440\u0435\u0434\u044c \u044d\u043a\u0441\u043f\u043e\u0440\u0442\u0430 OEBS'
                },
                {
                    code: 'ViewDevData',
                    id: 11261,
                    name:
                        '\u0414\u043e\u0441\u0442\u0443\u043f \u043a \u0442\u0435\u0445\u043d\u0438\u0447\u0435\u0441\u043a\u043e\u0439 \u0438\u043d\u0444\u043e\u0440\u043c\u0430\u0446\u0438\u0438'
                },
                {
                    code: 'ViewFishes',
                    id: 22,
                    name: '\u0424\u0438\u0448\u043a\u043e\u0432\u0438\u0434\u0435\u0446'
                },
                {
                    code: 'CreateClient',
                    id: 27,
                    name:
                        '\u0421\u043e\u0437\u0434\u0430\u043d\u0438\u0435 \u043a\u043b\u0438\u0435\u043d\u0442\u0430'
                },
                {
                    code: 'AlterAllInvoices',
                    id: 10,
                    name:
                        '[\u041d\u0435 \u0438\u0441\u043f\u043e\u043b\u044c\u0437\u0443\u0435\u0442\u0441\u044f]\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0441\u0443\u043c\u043c\u044b \u0441\u0447\u0435\u0442\u0430'
                },
                {
                    code: 'AdminAccess',
                    id: 0,
                    name:
                        '\u0414\u043e\u0441\u0442\u0443\u043f \u043a \u0430\u0434\u043c\u0438\u043d\u0438\u0441\u0442\u0440\u0430\u0442\u0438\u0432\u043d\u043e\u043c\u0443 \u0440\u0435\u0436\u0438\u043c\u0443'
                },
                {
                    code: 'assist_payments_report__postauth_status',
                    id: 1003,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u041f\u043b\u0430\u0442\u0435\u0436\u0438 \u043f\u043e Assist - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u0421\u0442\u0430\u0442\u0443\u0441 \u043f\u043e\u0441\u0442-\u0430\u0432\u0442\u043e\u0440\u0438\u0437\u0430\u0446\u0438\u0438'
                },
                {
                    code: 'GrantAllPermissionsToAll',
                    id: 7,
                    name:
                        '\u041d\u0430\u0437\u043d\u0430\u0447\u0435\u043d\u0438\u0435 \u043f\u043e\u043b\u043d\u043e\u043c\u043e\u0447\u0438\u0439'
                },
                {
                    code: 'DealPassport',
                    id: 30,
                    name: '\u0420\u0430\u0431\u043e\u0442\u0430 \u0441 \u041f\u0421'
                },
                {
                    code: 'ui_orders_report__client_id',
                    id: 1008,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u0417\u0430\u043a\u0430\u0437\u044b \u0432 \u041f\u0418 - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 ID \u043a\u043b\u0438\u0435\u043d\u0442\u0430'
                },
                {
                    code: 'invoices_report__client',
                    id: 1002,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u0421\u0447\u0435\u0442\u0430 - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 ID \u043a\u043b\u0438\u0435\u043d\u0442\u0430'
                },
                {
                    code: 'assist_payments_report__fraud_dt',
                    id: 1004,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u041f\u043b\u0430\u0442\u0435\u0436\u0438 \u043f\u043e Assist - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u0414\u0430\u0442\u0430 \u043e\u0431\u043d\u0430\u0440\u0443\u0436\u0435\u043d\u0438\u044f \u0444\u0440\u043e\u0434\u0430'
                },
                {
                    code: 'PayInvoiceExtension',
                    id: 11221,
                    name:
                        '\u041f\u0435\u0440\u0435\u043d\u0435\u0441\u0442\u0438 \u0441\u0440\u043e\u043a \u043e\u043f\u043b\u0430\u0442\u044b \u0441\u0447\u0451\u0442\u0430'
                },
                {
                    code: 'ProcessAllInvoices',
                    id: 8,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0441\u0447\u0435\u0442\u043e\u0432'
                },
                {
                    code: 'AlterFirmRU',
                    id: 31,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b \u041e\u041e\u041e \u00ab\u042f\u043d\u0434\u0435\u043a\u0441\u00bb'
                },
                {
                    code: 'AlterFirmKZTaxi',
                    id: 51,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b \u041e\u041e\u041e \u00ab\u042f\u043d\u0434\u0435\u043a\u0441.\u0422\u0430\u043a\u0441\u0438 \u041a\u0430\u0437\u0430\u0445\u0441\u0442\u0430\u043d\u00bb'
                },
                {
                    code: 'consumes_list__consume_sum',
                    id: 1011,
                    name:
                        '\u0421\u043f\u0438\u0441\u043e\u043a \u0437\u0430\u044f\u0432\u043e\u043a - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u041f\u0435\u0440\u0432\u043e\u043d\u0430\u0447\u0430\u043b\u044c\u043d\u0430\u044f \u0441\u0443\u043c\u043c\u0430'
                },
                {
                    code: 'orders_report__client_id',
                    id: 1005,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u0417\u0430\u043a\u0430\u0437\u044b - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 ID \u043a\u043b\u0438\u0435\u043d\u0442\u0430'
                },
                {
                    code: 'IssueUSDInvoices',
                    id: 13,
                    name:
                        '\u0412\u044b\u0441\u0442\u0430\u0432\u043b\u044f\u0442\u044c \u0434\u043e\u043b\u043b\u0430\u0440\u043e\u0432\u044b\u0435 \u0441\u0447\u0435\u0442\u0430'
                },
                {
                    code: 'UseRestrictedINN',
                    id: 11301,
                    name:
                        '\u0418\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u043d\u0438\u0435 \u0437\u0430\u043f\u0440\u0435\u0449\u0451\u043d\u043d\u044b\u0445 \u0418\u041d\u041d'
                },
                {
                    code: 'CreateBankPayments',
                    id: 3,
                    name:
                        '\u0412\u043d\u0435\u0441\u0435\u043d\u0438\u0435 \u043f\u043b\u0430\u0442\u0435\u0436\u0435\u0439 \u0447\u0435\u0440\u0435\u0437 \u0431\u0430\u043d\u043a'
                },
                {
                    code: 'deferpays_orders_report__invoice_eid',
                    id: 1013,
                    name:
                        '\u041e\u0442\u0447\u0435\u0442 \u041a\u0440\u0435\u0434\u0438\u0442\u044b - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u0424\u0438\u043a\u0442\u0438\u0432\u043d\u044b\u0439 \u0441\u0447\u0435\u0442'
                },
                {
                    code: 'PersonEdit',
                    id: 26,
                    name:
                        '\u0420\u0435\u0434\u0430\u043a\u0442\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0435 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u0430'
                },
                {
                    code: 'ApexReports',
                    id: 3001,
                    name: '\u0410\u0440\u0435\u0445. \u041e\u0442\u0447\u0435\u0442\u044b'
                },
                {
                    code: 'ChangeUnusedFundsLock',
                    id: 11227,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0438\u0442\u044c \u043f\u0440\u0438\u0447\u0438\u043d\u0443 \u0441\u043d\u044f\u0442\u0438\u044f \u0431\u0435\u0437\u0437\u0430\u043a\u0430\u0437\u044c\u044f \u0443 \u0441\u0447\u0451\u0442\u0430'
                },
                {
                    code: 'ViewClients',
                    id: 11321,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u043a\u043b\u0438\u0435\u043d\u0442\u043e\u0432'
                },
                {
                    code: 'TransferBetweenClients',
                    id: 11143,
                    name:
                        '\u041f\u0435\u0440\u0435\u043d\u043e\u0441 \u0441\u0440\u0435\u0434\u0441\u0442\u0432 \u043c\u0435\u0436\u0434\u0443 \u0440\u0430\u0437\u043d\u044b\u043c\u0438 \u043a\u043b\u0438\u0435\u043d\u0442\u0430\u043c\u0438'
                },
                {
                    code: 'CloseInvoice',
                    id: 11222,
                    name:
                        '\u0417\u0430\u043a\u0440\u044b\u0442\u0438\u0435 \u0441\u0447\u0451\u0442\u0430'
                },
                {
                    code: 'OEBSReexportInvoice',
                    id: 11223,
                    name:
                        '\u041f\u0435\u0440\u0435\u043f\u043e\u0441\u0442\u0430\u0432\u0438\u0442\u044c \u0441\u0447\u0451\u0442 \u0432 \u043e\u0447\u0435\u0440\u0435\u0434\u044c \u044d\u043a\u0441\u043f\u043e\u0440\u0442\u0430 OEBS'
                },
                {
                    code: 'EditClient',
                    id: 28,
                    name:
                        '\u0420\u0435\u0434\u0430\u043a\u0442\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0435 \u043a\u043b\u0438\u0435\u043d\u0442\u0430'
                },
                {
                    code: 'UseAdminPersons',
                    id: 11112,
                    name:
                        '\u0418\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u043d\u0438\u0435 \u043e\u0441\u043e\u0431\u044b\u0445 \u0442\u0438\u043f\u043e\u0432 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u043e\u0432'
                },
                {
                    code: 'SendActs',
                    id: 11323,
                    name:
                        '\u041e\u0442\u043f\u0440\u0430\u0432\u0438\u0442\u044c \u0430\u043a\u0442\u044b \u043f\u043e \u044d\u043b\u0435\u043a\u0442\u0440\u043e\u043d\u043d\u043e\u0439 \u0438\u043b\u0438 \u043e\u0431\u044b\u043d\u043e\u0439 \u043f\u043e\u0447\u0442\u0435'
                },
                {
                    code: 'AlterFirmHK',
                    id: 65,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b Yandex E-commerce Limited'
                },
                {
                    code: 'ChangeRepaymentsStatus',
                    id: 11103,
                    name:
                        '\u041f\u043e\u0434\u0442\u0432\u0435\u0440\u0436\u0434\u0435\u043d\u0438\u0435 \u0441\u0447\u0435\u0442\u043e\u0432 \u043d\u0430 \u043f\u043e\u0433\u0430\u0448\u0435\u043d\u0438\u0435'
                },
                {
                    code: 'TransferUnusedFunds',
                    id: 11121,
                    name:
                        '\u041f\u0435\u0440\u0435\u043d\u043e\u0441 \u0441\u0432\u043e\u0431\u043e\u0434\u043d\u044b\u0445 \u0441\u0440\u0435\u0434\u0441\u0442\u0432'
                },
                {
                    code: 'AlterInvoiceSum',
                    id: 11107,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0441\u0443\u043c\u043c\u044b \u0441\u0447\u0435\u0442\u0430'
                },
                {
                    code: 'ViewInvoices',
                    id: 11105,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u0441\u0447\u0435\u0442\u043e\u0432'
                },
                {
                    code: 'AlterInvoiceDate',
                    id: 11108,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u0430\u0442\u044b \u0441\u0447\u0435\u0442\u0430'
                },
                {
                    code: 'RecalculateOverdraft',
                    id: 42,
                    name:
                        '\u041f\u0435\u0440\u0435\u0440\u0430\u0441\u0447\u0435\u0442 \u043e\u0432\u0435\u0440\u0434\u0440\u0430\u0444\u0442\u0430'
                },
                {
                    code: 'AdditionalFunctions',
                    id: 47,
                    name:
                        '\u0414\u043e\u043f\u043e\u043b\u043d\u0438\u0442\u0435\u043b\u044c\u043d\u044b\u0435 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438'
                },
                {
                    code: 'ViewOrders',
                    id: 11201,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u0437\u0430\u043a\u0430\u0437\u043e\u0432'
                },
                {
                    code: 'AlterFirmNVTaxiuber',
                    id: 63,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b Uber ML B.V.'
                },
                {
                    code: 'WithdrawConsumesPrepay',
                    id: 67,
                    name:
                        '\u0421\u043d\u044f\u0442\u0438\u0435 \u0437\u0430\u0447\u0438\u0441\u043b\u0435\u043d\u0438\u0439 \u0441 \u043f\u0440\u0435\u0434\u043e\u043f\u043b\u0430\u0442\u043d\u044b\u0445 \u0441\u0447\u0435\u0442\u043e\u0432'
                },
                {
                    code: 'invoices_report__manager',
                    id: 1007,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u0421\u0447\u0435\u0442\u0430 - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u0412\u043a\u043b\u044e\u0447\u0435\u043d\u0438\u0435 \u0437\u0430\u043a\u0430\u0437\u0430'
                },
                {
                    code: 'ClientFraudStatusEdit',
                    id: 58,
                    name:
                        '\u0420\u0435\u0434\u0430\u043a\u0442\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0435 \u043a\u043b\u0438\u0435\u043d\u0442\u0430: \u043f\u0440\u043e\u0441\u0442\u0430\u043d\u043e\u0432\u043a\u0430 \u0444\u043b\u0430\u0433\u0430 deny_cc / fraud_status'
                },
                {
                    code: 'AlterFirmAMTaxi',
                    id: 53,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b \u041e\u041e\u041e \u00ab\u042f\u043d\u0434\u0435\u043a\u0441.\u0422\u0430\u043a\u0441\u0438 \u0410\u0440\u043c\u0435\u043d\u0438\u044f\u00bb'
                },
                {
                    code: 'AlterFirmAUTORU',
                    id: 45,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b \u041e\u041e\u041e \u00ab\u0410\u0412\u0422\u041e.\u0420\u0423 \u0425\u043e\u043b\u0434\u0438\u043d\u0433\u00bb'
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
                    code: 'AlterInvoicePaysys',
                    id: 11109,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0441\u043f\u043e\u0441\u043e\u0431\u0430 \u043e\u043f\u043b\u0430\u0442\u044b \u0441\u0447\u0435\u0442\u0430'
                },
                {
                    code: 'AlterFirmSW',
                    id: 39,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b Yandex Europe AG'
                },
                {
                    code: 'SetMcbFlags',
                    id: 11324,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0438\u0442\u044c \u043a\u0430\u0442\u0435\u0433\u043e\u0440\u0438\u0438 \u0434\u043b\u044f \u043f\u043b\u043e\u0449\u0430\u0434\u043e\u043a \u043a\u043b\u0438\u0435\u043d\u0442\u043e\u0432'
                },
                {
                    code: 'AlterPrintTemplate',
                    id: 55,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0448\u0430\u0431\u043b\u043e\u043d\u0430 \u043f\u0435\u0447\u0430\u0442\u043d\u043e\u0439 \u0444\u043e\u0440\u043c\u044b \u0432 \u0434\u043e\u0433\u043e\u0432\u0440\u0435'
                },
                {
                    code: 'PersonExtEdit',
                    id: 46,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0432\u0430\u0436\u043d\u044b\u0445 \u043f\u043e\u043b\u0435\u0439 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u0430'
                },
                {
                    code: 'SuperVisor',
                    id: 15,
                    name:
                        '\u0423\u0441\u0442\u0430\u0440\u0435\u0432\u0448\u0435\u0435, \u043d\u0435 \u0438\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u0442\u044c. SuperWisard'
                },
                {
                    code: 'DoInvoiceRefunds',
                    id: 4004,
                    name:
                        '\u0421\u043e\u0437\u0434\u0430\u043d\u0438\u0435 \u0432\u043e\u0437\u0432\u0440\u0430\u0442\u043e\u0432 \u043f\u043e \u0441\u0447\u0435\u0442\u0430\u043c'
                },
                {
                    code: 'WithdrawConsumesPostpay',
                    id: 66,
                    name:
                        '\u0421\u043d\u044f\u0442\u0438\u0435 \u0437\u0430\u0447\u0438\u0441\u043b\u0435\u043d\u0438\u0439 \u0441 \u043f\u043e\u0441\u0442\u043e\u043f\u043b\u0430\u0442\u043d\u044b\u0445 \u0441\u0447\u0435\u0442\u043e\u0432'
                },
                {
                    code: 'AlterFirmTR',
                    id: 41,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b Yandex Turkey'
                },
                {
                    code: 'r_django_fraud_monitoring',
                    id: 62,
                    name:
                        '\u041f\u0440\u0430\u0432\u0430 \u043d\u0430 \u0447\u0442\u0435\u043d\u0438\u0435 Django Fraud Monitoring'
                },
                {
                    code: 'EDOViewer',
                    id: 60,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u0433\u0440\u0430\u0444\u0438\u043a\u0430 \u042d\u0414\u041e'
                },
                {
                    code: 'IssueInvoices',
                    id: 11106,
                    name:
                        '\u0412\u044b\u0441\u0442\u0430\u0432\u043b\u0435\u043d\u0438\u0435 \u0441\u0447\u0435\u0442\u043e\u0432'
                },
                {
                    code: 'TerminalEdit',
                    id: 57,
                    name:
                        '\u0420\u0435\u0434\u0430\u043a\u0442\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0435 \u0442\u0435\u0440\u043c\u0438\u043d\u0430\u043b\u0430'
                },
                {
                    code: 'EditINN',
                    id: 43,
                    name:
                        '\u0417\u0430\u0432\u0435\u0434\u0435\u043d\u0438\u0435 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u0430 \u0441 \u0415\u0413\u0420\u041f\u041e\u0423 \u0432\u043c\u0435\u0441\u0442\u043e \u0418\u041d\u041d'
                },
                {
                    code: 'AlterFirmNVTaxi',
                    id: 48,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b Yandex.Taxi B.V.'
                },
                {
                    code: 'DoInvoiceRefundsTrust',
                    id: 11161,
                    name:
                        '\u0421\u043e\u0437\u0434\u0430\u043d\u0438\u0435 \u0432\u043e\u0437\u0432\u0440\u0430\u0442\u043e\u0432 \u043f\u043e \u0441\u0447\u0435\u0442\u0430\u043c \u0447\u0435\u0440\u0435\u0437 \u0422\u0440\u0430\u0441\u0442'
                },
                {
                    code: 'CreateRequestsShop',
                    id: 11101,
                    name:
                        '\u0412\u044b\u0441\u0442\u0430\u0432\u043b\u0435\u043d\u0438\u0435 \u0441\u0447\u0435\u0442\u043e\u0432 \u0447\u0435\u0440\u0435\u0437 \u043c\u0430\u0433\u0430\u0437\u0438\u043d'
                },
                {
                    code: 'ViewContracts',
                    id: 11181,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u043e\u0432'
                },
                {
                    code: 'TransferFromInvoice',
                    id: 11142,
                    name:
                        '\u041f\u0435\u0440\u0435\u043d\u043e\u0441 \u0441\u0440\u0435\u0434\u0441\u0442\u0432 \u0441\u043e \u0441\u0447\u0435\u0442\u0430'
                },
                {
                    code: 'LocalNamesMaster',
                    id: 59,
                    name:
                        '\u0420\u0435\u0434\u0430\u043a\u0442\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0435 \u043b\u043e\u043a\u0430\u043b\u0438\u0437\u043e\u0432\u0430\u043d\u043d\u044b\u0445 \u0430\u0442\u0440\u0438\u0431\u0443\u0442\u043e\u0432 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u043e\u0432'
                },
                {
                    code: 'order_info__order_id',
                    id: 1012,
                    name:
                        '\u0421\u0432\u043e\u0439\u0441\u0442\u0432\u0430 \u0437\u0430\u043a\u0430\u0437\u0430 - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u0412\u043d\u0443\u0442\u0440\u0435\u043d\u043d\u0438\u0439 ID \u0437\u0430\u043a\u0430\u0437\u0430'
                },
                {
                    code: 'PaymentLinksReport',
                    id: 11104,
                    name:
                        '\u041f\u043e\u0438\u0441\u043a \u043f\u043e \u043f\u043b\u0430\u0442\u0435\u0436\u0430\u043c'
                },
                {
                    code: 'AlterInvoicePerson',
                    id: 11110,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u0430 \u0441\u0447\u0435\u0442\u0430'
                },
                {
                    code: 'AlterInvoiceContract',
                    id: 11111,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0430 \u0441\u0447\u0435\u0442\u0430'
                },
                {
                    code: 'ViewPayments',
                    id: 11326,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u043f\u043b\u0430\u0442\u0435\u0436\u0435\u0439'
                },
                {
                    code: 'TerminalView',
                    id: 56,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u0442\u0435\u0440\u043c\u0438\u043d\u0430\u043b\u0430'
                }
            ]
        }
    },
    {
        request: [
            `${HOST}/export/get-state`,
            {
                classname: 'Act',
                queue_type: 'OEBS',
                object_id: 124863507
            },
            false,
            false
        ],
        response: {
            version: { 'yb-snout-api': '3.59.2' },
            data: { export_dt: null, state: 'WAITING' }
        }
    }
];

export const act124863508 = [
    {
        request: [`${HOST}/act`, { act_id: '124863508', act_eid: undefined }, false, false],
        response: {
            version: { 'yb-snout-api': '3.59.2' },
            data: {
                export_dt: null,
                rows: [
                    {
                        consume: {
                            seqnum: 0,
                            unit_name: '\u0443.\u0435.',
                            discount_pct: '0.00',
                            order: {
                                text: 'Py_Test order 7-1475',
                                nds: 1,
                                service_order_id: 56763987,
                                id: 3432956424,
                                service: {
                                    cc: 'PPC',
                                    id: 7,
                                    name:
                                        '\u0414\u0438\u0440\u0435\u043a\u0442: \u0420\u0435\u043a\u043b\u0430\u043c\u043d\u044b\u0435 \u043a\u0430\u043c\u043f\u0430\u043d\u0438\u0438'
                                }
                            }
                        },
                        price: '30.0000',
                        amount: '1500.00',
                        manager: {
                            manager_code: 20453,
                            name:
                                '\u041f\u0435\u0440\u0430\u043d\u0438\u0434\u0437\u0435 \u041d\u0443\u0433\u0437\u0430\u0440 \u0413\u0435\u043e\u0440\u0433\u0438\u0435\u0432\u0438\u0447'
                        },
                        finish_sum: '1500.00',
                        netting: null,
                        consume_nds: null,
                        act_sum: '1500.00',
                        id: 767166285,
                        quantity: '50.000000'
                    }
                ],
                is_trp: null,
                oebs_exportable: false,
                memo: null,
                is_docs_detailed: false,
                factura: null,
                amount: '1500.00',
                good_debt: '0',
                invoice: {
                    firm: { id: 1 },
                    postpay: false,
                    receipt_sum_1c: '3000.00',
                    total_sum: '3000.00',
                    paysys: { id: 1003 },
                    currency: 'RUR',
                    person: {
                        id: 12342420,
                        name:
                            '\u041e\u041e\u041e "\u041f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a"'
                    },
                    client: {
                        id: 1338514339,
                        name: '\u041e\u041e\u041e "\u041a\u043b\u0438\u0435\u043d\u0442"'
                    },
                    iso_currency: 'RUB',
                    external_id: '\u0411-3310226484-1',
                    id: 118619305
                },
                is_docs_separated: false,
                id: 124863508,
                hidden: false,
                act_sum: '1500.00',
                external_id: '153731982',
                dt: '2020-08-31T00:00:00'
            }
        }
    },
    {
        request: [
            `${HOST}/user/object-permissions`,
            { classname: 'Act', object_id: 124863508 },
            false,
            false
        ],
        response: {
            version: { 'yb-snout-api': '3.59.2' },
            data: [
                {
                    code: 'invoices_report__manager',
                    id: 1007,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u0421\u0447\u0435\u0442\u0430 - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u0412\u043a\u043b\u044e\u0447\u0435\u043d\u0438\u0435 \u0437\u0430\u043a\u0430\u0437\u0430'
                },
                {
                    code: 'AssistPostauth',
                    id: 11,
                    name:
                        '\u0423\u043f\u0440\u0430\u0432\u043b\u0435\u043d\u0438\u0435 \u043f\u043e\u0441\u0442\u0430\u0432\u0442\u043e\u0440\u0438\u0437\u0430\u0446\u0438\u0435\u0439'
                },
                {
                    code: 'CreateCerificatePayments',
                    id: 4,
                    name:
                        '\u0412\u043d\u0435\u0441\u0435\u043d\u0438\u0435 \u043f\u043b\u0430\u0442\u0435\u0436\u0435\u0439 \u0441\u0435\u0440\u0442\u0438\u0444\u0438\u043a\u0430\u0442\u043e\u043c, \u0431\u0430\u0440\u0442\u0435\u0440\u043e\u043c'
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
                    code: 'ProcessAllInvoices',
                    id: 8,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0441\u0447\u0435\u0442\u043e\u0432'
                },
                {
                    code: 'assist_payments_report__postauth_status',
                    id: 1003,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u041f\u043b\u0430\u0442\u0435\u0436\u0438 \u043f\u043e Assist - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u0421\u0442\u0430\u0442\u0443\u0441 \u043f\u043e\u0441\u0442-\u0430\u0432\u0442\u043e\u0440\u0438\u0437\u0430\u0446\u0438\u0438'
                },
                {
                    code: 'AlterAllInvoices',
                    id: 10,
                    name:
                        '[\u041d\u0435 \u0438\u0441\u043f\u043e\u043b\u044c\u0437\u0443\u0435\u0442\u0441\u044f]\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0441\u0443\u043c\u043c\u044b \u0441\u0447\u0435\u0442\u0430'
                },
                {
                    code: 'GrantAllPermissionsToAll',
                    id: 7,
                    name:
                        '\u041d\u0430\u0437\u043d\u0430\u0447\u0435\u043d\u0438\u0435 \u043f\u043e\u043b\u043d\u043e\u043c\u043e\u0447\u0438\u0439'
                },
                {
                    code: 'AlterInvoiceSum',
                    id: 11107,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0441\u0443\u043c\u043c\u044b \u0441\u0447\u0435\u0442\u0430'
                },
                {
                    code: 'invoices_report__client',
                    id: 1002,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u0421\u0447\u0435\u0442\u0430 - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 ID \u043a\u043b\u0438\u0435\u043d\u0442\u0430'
                },
                {
                    code: 'PersonView',
                    id: 25,
                    name:
                        '\u0423\u0441\u0442\u0430\u0440\u0435\u0432\u0448\u0435\u0435, \u043d\u0435 \u0438\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u0442\u044c. \u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u0430.'
                },
                {
                    code: 'LocalNamesMaster',
                    id: 59,
                    name:
                        '\u0420\u0435\u0434\u0430\u043a\u0442\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0435 \u043b\u043e\u043a\u0430\u043b\u0438\u0437\u043e\u0432\u0430\u043d\u043d\u044b\u0445 \u0430\u0442\u0440\u0438\u0431\u0443\u0442\u043e\u0432 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u043e\u0432'
                },
                {
                    code: 'RepresentClient',
                    id: 2000,
                    name:
                        '\u0414\u0435\u0439\u0441\u0442\u0432\u0438\u044f \u043e\u0442 \u0438\u043c\u0435\u043d\u0438 \u043a\u043b\u0438\u0435\u043d\u0442\u0430'
                },
                {
                    code: 'ManagersOperations',
                    id: 23,
                    name:
                        '\u0414\u0435\u0439\u0441\u0442\u0432\u0438\u044f \u043c\u0435\u043d\u0435\u0434\u0436\u0435\u0440\u0430'
                },
                {
                    code: 'OEBSReexportInvoice',
                    id: 11223,
                    name:
                        '\u041f\u0435\u0440\u0435\u043f\u043e\u0441\u0442\u0430\u0432\u0438\u0442\u044c \u0441\u0447\u0451\u0442 \u0432 \u043e\u0447\u0435\u0440\u0435\u0434\u044c \u044d\u043a\u0441\u043f\u043e\u0440\u0442\u0430 OEBS'
                },
                {
                    code: 'TerminalView',
                    id: 56,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u0442\u0435\u0440\u043c\u0438\u043d\u0430\u043b\u0430'
                },
                {
                    code: 'AlterFirmNVTaxiuber',
                    id: 63,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b Uber ML B.V.'
                },
                {
                    code: 'PersonPostAddressEdit',
                    id: 38,
                    name:
                        '\u0420\u0435\u0434\u0430\u043a\u0442\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0435 \u043b\u044e\u0431\u044b\u0445 \u043f\u043e\u043b\u0435\u0439 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u0430'
                },
                {
                    code: 'ViewDevData',
                    id: 11261,
                    name:
                        '\u0414\u043e\u0441\u0442\u0443\u043f \u043a \u0442\u0435\u0445\u043d\u0438\u0447\u0435\u0441\u043a\u043e\u0439 \u0438\u043d\u0444\u043e\u0440\u043c\u0430\u0446\u0438\u0438'
                },
                {
                    code: 'ViewAllManagerReports',
                    id: 12,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442\u044b \u0434\u043b\u044f \u043c\u0435\u043d\u0435\u0434\u0436\u0435\u0440\u043e\u0432'
                },
                {
                    code: 'CreateBankPayments',
                    id: 3,
                    name:
                        '\u0412\u043d\u0435\u0441\u0435\u043d\u0438\u0435 \u043f\u043b\u0430\u0442\u0435\u0436\u0435\u0439 \u0447\u0435\u0440\u0435\u0437 \u0431\u0430\u043d\u043a'
                },
                {
                    code: 'AlterPrintTemplate',
                    id: 55,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0448\u0430\u0431\u043b\u043e\u043d\u0430 \u043f\u0435\u0447\u0430\u0442\u043d\u043e\u0439 \u0444\u043e\u0440\u043c\u044b \u0432 \u0434\u043e\u0433\u043e\u0432\u0440\u0435'
                },
                {
                    code: 'r_django_fraud_monitoring',
                    id: 62,
                    name:
                        '\u041f\u0440\u0430\u0432\u0430 \u043d\u0430 \u0447\u0442\u0435\u043d\u0438\u0435 Django Fraud Monitoring'
                },
                {
                    code: 'ViewProduct',
                    id: 11241,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u043f\u0440\u043e\u0434\u0443\u043a\u0442\u043e\u0432'
                },
                {
                    code: 'IssueYTInvoices',
                    id: 14,
                    name:
                        '\u0412\u044b\u0441\u0442\u0430\u0432\u043b\u044f\u0442\u044c \u0441\u0447\u0435\u0442\u0430 \u043d\u0430 Yandex Technologies'
                },
                {
                    code: 'SetMcbFlags',
                    id: 11324,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0438\u0442\u044c \u043a\u0430\u0442\u0435\u0433\u043e\u0440\u0438\u0438 \u0434\u043b\u044f \u043f\u043b\u043e\u0449\u0430\u0434\u043e\u043a \u043a\u043b\u0438\u0435\u043d\u0442\u043e\u0432'
                },
                {
                    code: 'ModifyDiscounts',
                    id: 16,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0441\u043a\u0438\u0434\u043e\u043a'
                },
                {
                    code: 'ChangeUnusedFundsLock',
                    id: 11227,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0438\u0442\u044c \u043f\u0440\u0438\u0447\u0438\u043d\u0443 \u0441\u043d\u044f\u0442\u0438\u044f \u0431\u0435\u0437\u0437\u0430\u043a\u0430\u0437\u044c\u044f \u0443 \u0441\u0447\u0451\u0442\u0430'
                },
                {
                    code: 'consumes_list__consume_qty',
                    id: 1010,
                    name:
                        '\u0421\u043f\u0438\u0441\u043e\u043a \u0437\u0430\u044f\u0432\u043e\u043a - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u041f\u0435\u0440\u0432\u043e\u043d\u0430\u0447\u0430\u043b\u044c\u043d\u043e\u0435 \u043a\u043e\u043b\u0438\u0447\u0435\u0441\u0442\u0432\u043e'
                },
                {
                    code: 'OEBSReexportAct',
                    id: 11224,
                    name:
                        '\u041f\u0435\u0440\u0435\u043f\u043e\u0441\u0442\u0430\u0432\u0438\u0442\u044c \u0430\u043a\u0442 \u0432 \u043e\u0447\u0435\u0440\u0435\u0434\u044c \u044d\u043a\u0441\u043f\u043e\u0440\u0442\u0430 OEBS'
                },
                {
                    code: 'AdminAccess',
                    id: 0,
                    name:
                        '\u0414\u043e\u0441\u0442\u0443\u043f \u043a \u0430\u0434\u043c\u0438\u043d\u0438\u0441\u0442\u0440\u0430\u0442\u0438\u0432\u043d\u043e\u043c\u0443 \u0440\u0435\u0436\u0438\u043c\u0443'
                },
                {
                    code: 'assist_payments_report__fraud_dt',
                    id: 1004,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u041f\u043b\u0430\u0442\u0435\u0436\u0438 \u043f\u043e Assist - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u0414\u0430\u0442\u0430 \u043e\u0431\u043d\u0430\u0440\u0443\u0436\u0435\u043d\u0438\u044f \u0444\u0440\u043e\u0434\u0430'
                },
                {
                    code: 'ApexReports',
                    id: 3001,
                    name: '\u0410\u0440\u0435\u0445. \u041e\u0442\u0447\u0435\u0442\u044b'
                },
                { code: 'Reversal', id: 9, name: 'Reversal' },
                {
                    code: 'DoInvoiceRefundsTrust',
                    id: 11161,
                    name:
                        '\u0421\u043e\u0437\u0434\u0430\u043d\u0438\u0435 \u0432\u043e\u0437\u0432\u0440\u0430\u0442\u043e\u0432 \u043f\u043e \u0441\u0447\u0435\u0442\u0430\u043c \u0447\u0435\u0440\u0435\u0437 \u0422\u0440\u0430\u0441\u0442'
                },
                {
                    code: 'ViewAllInvoices',
                    id: 1,
                    name:
                        '\u0423\u0441\u0442\u0430\u0440\u0435\u0432\u0448\u0435\u0435, \u043d\u0435 \u0438\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u0442\u044c. \u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u0441\u0447\u0435\u0442\u043e\u0432.'
                },
                {
                    code: 'OEBSReexportContract',
                    id: 11226,
                    name:
                        '\u041f\u0435\u0440\u0435\u043f\u043e\u0441\u0442\u0430\u0432\u0438\u0442\u044c \u0434\u043e\u0433\u043e\u0432\u043e\u0440/\u0434\u043e\u043f.\u0441\u043e\u0433\u043b\u0430\u0448\u0435\u043d\u0438\u0435 \u0432 \u043e\u0447\u0435\u0440\u0435\u0434\u044c \u044d\u043a\u0441\u043f\u043e\u0440\u0442\u0430 OEBS'
                },
                {
                    code: 'OEBSReexportPerson',
                    id: 11225,
                    name:
                        '\u041f\u0435\u0440\u0435\u043f\u043e\u0441\u0442\u0430\u0432\u0438\u0442\u044c \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u0430 \u0432 \u043e\u0447\u0435\u0440\u0435\u0434\u044c \u044d\u043a\u0441\u043f\u043e\u0440\u0442\u0430 OEBS'
                },
                {
                    code: 'PayInvoiceExtension',
                    id: 11221,
                    name:
                        '\u041f\u0435\u0440\u0435\u043d\u0435\u0441\u0442\u0438 \u0441\u0440\u043e\u043a \u043e\u043f\u043b\u0430\u0442\u044b \u0441\u0447\u0451\u0442\u0430'
                },
                {
                    code: 'TerminalEdit',
                    id: 57,
                    name:
                        '\u0420\u0435\u0434\u0430\u043a\u0442\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0435 \u0442\u0435\u0440\u043c\u0438\u043d\u0430\u043b\u0430'
                },
                {
                    code: 'TransferUnusedFunds',
                    id: 11121,
                    name:
                        '\u041f\u0435\u0440\u0435\u043d\u043e\u0441 \u0441\u0432\u043e\u0431\u043e\u0434\u043d\u044b\u0445 \u0441\u0440\u0435\u0434\u0441\u0442\u0432'
                },
                {
                    code: 'EDOViewer',
                    id: 60,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u0433\u0440\u0430\u0444\u0438\u043a\u0430 \u042d\u0414\u041e'
                },
                {
                    code: 'ViewPayments',
                    id: 11326,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u043f\u043b\u0430\u0442\u0435\u0436\u0435\u0439'
                },
                {
                    code: 'SetClientOverdraftBan',
                    id: 11325,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0438\u0442\u044c \u0431\u0430\u043d \u043e\u0432\u0435\u0440\u0434\u0440\u0430\u0444\u0442\u0430 \u0434\u043b\u044f \u043a\u043b\u0438\u0435\u043d\u0442\u0430'
                },
                {
                    code: 'consumes_list__consume_sum',
                    id: 1011,
                    name:
                        '\u0421\u043f\u0438\u0441\u043e\u043a \u0437\u0430\u044f\u0432\u043e\u043a - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u041f\u0435\u0440\u0432\u043e\u043d\u0430\u0447\u0430\u043b\u044c\u043d\u0430\u044f \u0441\u0443\u043c\u043c\u0430'
                },
                {
                    code: 'ClientFraudStatusEdit',
                    id: 58,
                    name:
                        '\u0420\u0435\u0434\u0430\u043a\u0442\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0435 \u043a\u043b\u0438\u0435\u043d\u0442\u0430: \u043f\u0440\u043e\u0441\u0442\u0430\u043d\u043e\u0432\u043a\u0430 \u0444\u043b\u0430\u0433\u0430 deny_cc / fraud_status'
                },
                {
                    code: 'AlterFirmUS',
                    id: 34,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b Yandex Inc'
                },
                {
                    code: 'PaymentLinksReport',
                    id: 11104,
                    name:
                        '\u041f\u043e\u0438\u0441\u043a \u043f\u043e \u043f\u043b\u0430\u0442\u0435\u0436\u0430\u043c'
                },
                {
                    code: 'ViewInvoices',
                    id: 11105,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u0441\u0447\u0435\u0442\u043e\u0432'
                },
                {
                    code: 'AlterFirmAMTaxi',
                    id: 53,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b \u041e\u041e\u041e \u00ab\u042f\u043d\u0434\u0435\u043a\u0441.\u0422\u0430\u043a\u0441\u0438 \u0410\u0440\u043c\u0435\u043d\u0438\u044f\u00bb'
                },
                {
                    code: 'AlterFirmRU',
                    id: 31,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b \u041e\u041e\u041e \u00ab\u042f\u043d\u0434\u0435\u043a\u0441\u00bb'
                },
                {
                    code: 'TransferFromOrder',
                    id: 11141,
                    name:
                        '\u041f\u0435\u0440\u0435\u043d\u043e\u0441 \u0441\u0440\u0435\u0434\u0441\u0442\u0432 c \u0437\u0430\u043a\u0430\u0437\u0430'
                },
                {
                    code: 'ViewFishes',
                    id: 22,
                    name: '\u0424\u0438\u0448\u043a\u043e\u0432\u0438\u0434\u0435\u0446'
                },
                {
                    code: 'ViewOrders',
                    id: 11201,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u0437\u0430\u043a\u0430\u0437\u043e\u0432'
                },
                {
                    code: 'AlterFirmSW',
                    id: 39,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b Yandex Europe AG'
                },
                {
                    code: 'ViewClients',
                    id: 11321,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u043a\u043b\u0438\u0435\u043d\u0442\u043e\u0432'
                },
                {
                    code: 'CreateClient',
                    id: 27,
                    name:
                        '\u0421\u043e\u0437\u0434\u0430\u043d\u0438\u0435 \u043a\u043b\u0438\u0435\u043d\u0442\u0430'
                },
                {
                    code: 'ViewContracts',
                    id: 11181,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u043e\u0432'
                },
                {
                    code: 'AlterFirmKZTaxi',
                    id: 51,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b \u041e\u041e\u041e \u00ab\u042f\u043d\u0434\u0435\u043a\u0441.\u0422\u0430\u043a\u0441\u0438 \u041a\u0430\u0437\u0430\u0445\u0441\u0442\u0430\u043d\u00bb'
                },
                {
                    code: 'CreateRequestsShop',
                    id: 11101,
                    name:
                        '\u0412\u044b\u0441\u0442\u0430\u0432\u043b\u0435\u043d\u0438\u0435 \u0441\u0447\u0435\u0442\u043e\u0432 \u0447\u0435\u0440\u0435\u0437 \u043c\u0430\u0433\u0430\u0437\u0438\u043d'
                },
                {
                    code: 'EditINN',
                    id: 43,
                    name:
                        '\u0417\u0430\u0432\u0435\u0434\u0435\u043d\u0438\u0435 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u0430 \u0441 \u0415\u0413\u0420\u041f\u041e\u0423 \u0432\u043c\u0435\u0441\u0442\u043e \u0418\u041d\u041d'
                },
                {
                    code: 'PersonExtEdit',
                    id: 46,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0432\u0430\u0436\u043d\u044b\u0445 \u043f\u043e\u043b\u0435\u0439 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u0430'
                },
                {
                    code: 'AlterFirmKZ',
                    id: 33,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b \u0422\u041e\u041e \u00abKazNet Media (\u041a\u0430\u0437\u041d\u0435\u0442 \u041c\u0435\u0434\u0438\u0430)\u00bb'
                },
                {
                    code: 'ui_orders_report__manager_code',
                    id: 1009,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u0417\u0430\u043a\u0430\u0437\u044b \u0432 \u041f\u0418 - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u041c\u0435\u043d\u0435\u0434\u0436\u0435\u0440'
                },
                {
                    code: 'PersonEdit',
                    id: 26,
                    name:
                        '\u0420\u0435\u0434\u0430\u043a\u0442\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0435 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u0430'
                },
                {
                    code: 'AlterFirmUA',
                    id: 32,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b \u041e\u041e\u041e \u00ab\u042f\u043d\u0434\u0435\u043a\u0441.\u0423\u043a\u0440\u0430\u0438\u043d\u0430\u00bb'
                },
                {
                    code: 'UseAdminPersons',
                    id: 11112,
                    name:
                        '\u0418\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u043d\u0438\u0435 \u043e\u0441\u043e\u0431\u044b\u0445 \u0442\u0438\u043f\u043e\u0432 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u043e\u0432'
                },
                {
                    code: 'AlterFirmNVTaxi',
                    id: 48,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b Yandex.Taxi B.V.'
                },
                {
                    code: 'AlterFirmEU',
                    id: 37,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b Yandex Europe B.V.'
                },
                {
                    code: 'TransferBetweenClients',
                    id: 11143,
                    name:
                        '\u041f\u0435\u0440\u0435\u043d\u043e\u0441 \u0441\u0440\u0435\u0434\u0441\u0442\u0432 \u043c\u0435\u0436\u0434\u0443 \u0440\u0430\u0437\u043d\u044b\u043c\u0438 \u043a\u043b\u0438\u0435\u043d\u0442\u0430\u043c\u0438'
                },
                {
                    code: 'SendActs',
                    id: 11323,
                    name:
                        '\u041e\u0442\u043f\u0440\u0430\u0432\u0438\u0442\u044c \u0430\u043a\u0442\u044b \u043f\u043e \u044d\u043b\u0435\u043a\u0442\u0440\u043e\u043d\u043d\u043e\u0439 \u0438\u043b\u0438 \u043e\u0431\u044b\u043d\u043e\u0439 \u043f\u043e\u0447\u0442\u0435'
                },
                {
                    code: 'WithdrawConsumesPostpay',
                    id: 66,
                    name:
                        '\u0421\u043d\u044f\u0442\u0438\u0435 \u0437\u0430\u0447\u0438\u0441\u043b\u0435\u043d\u0438\u0439 \u0441 \u043f\u043e\u0441\u0442\u043e\u043f\u043b\u0430\u0442\u043d\u044b\u0445 \u0441\u0447\u0435\u0442\u043e\u0432'
                },
                {
                    code: 'AlterFirmHK',
                    id: 65,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b Yandex E-commerce Limited'
                },
                {
                    code: 'TransferFromInvoice',
                    id: 11142,
                    name:
                        '\u041f\u0435\u0440\u0435\u043d\u043e\u0441 \u0441\u0440\u0435\u0434\u0441\u0442\u0432 \u0441\u043e \u0441\u0447\u0435\u0442\u0430'
                },
                {
                    code: 'NotifyOrder',
                    id: 11281,
                    name:
                        '\u041e\u0442\u043f\u0440\u0430\u0432\u0438\u0442\u044c \u043d\u043e\u0442\u0438\u0444\u0438\u043a\u0430\u0446\u0438\u044e \u043f\u043e \u0437\u0430\u043a\u0430\u0437\u0443'
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
                    code: 'IssueUSDInvoices',
                    id: 13,
                    name:
                        '\u0412\u044b\u0441\u0442\u0430\u0432\u043b\u044f\u0442\u044c \u0434\u043e\u043b\u043b\u0430\u0440\u043e\u0432\u044b\u0435 \u0441\u0447\u0435\u0442\u0430'
                },
                {
                    code: 'AlterFirmUATaxi',
                    id: 49,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b \u041e\u041e\u041e "\u042f\u043d\u0434\u0435\u043a\u0441.\u0422\u0430\u043a\u0441\u0438 \u0423\u043a\u0440\u0430\u0438\u043d\u0430"'
                },
                {
                    code: 'DealPassport',
                    id: 30,
                    name: '\u0420\u0430\u0431\u043e\u0442\u0430 \u0441 \u041f\u0421'
                },
                {
                    code: 'invoices_report__manager_info',
                    id: 1006,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u0421\u0447\u0435\u0442\u0430 - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u041c\u0435\u043d\u0435\u0434\u0436\u0435\u0440'
                },
                {
                    code: 'ViewReports',
                    id: 2,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u043e\u0442\u0447\u0435\u0442\u043e\u0432'
                },
                {
                    code: 'AlterFirmAUTORU',
                    id: 45,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b \u041e\u041e\u041e \u00ab\u0410\u0412\u0422\u041e.\u0420\u0423 \u0425\u043e\u043b\u0434\u0438\u043d\u0433\u00bb'
                },
                {
                    code: 'ui_orders_report__client_id',
                    id: 1008,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u0417\u0430\u043a\u0430\u0437\u044b \u0432 \u041f\u0418 - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 ID \u043a\u043b\u0438\u0435\u043d\u0442\u0430'
                },
                {
                    code: 'ManageBadDebts',
                    id: 21,
                    name:
                        '\u041f\u0440\u0438\u0437\u043d\u0430\u043d\u0438\u0435 "\u043f\u043b\u043e\u0445\u043e\u0433\u043e" \u0434\u043e\u043b\u0433\u0430'
                },
                {
                    code: 'EditClient',
                    id: 28,
                    name:
                        '\u0420\u0435\u0434\u0430\u043a\u0442\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0435 \u043a\u043b\u0438\u0435\u043d\u0442\u0430'
                },
                {
                    code: 'AlterInvoiceContract',
                    id: 11111,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0430 \u0441\u0447\u0435\u0442\u0430'
                },
                {
                    code: 'BillingSupport',
                    id: 1100,
                    name:
                        '\u0421\u0430\u043f\u043f\u043e\u0440\u0442 \u0411\u0438\u043b\u043b\u0438\u043d\u0433\u0430'
                },
                {
                    code: 'WithdrawConsumesPrepay',
                    id: 67,
                    name:
                        '\u0421\u043d\u044f\u0442\u0438\u0435 \u0437\u0430\u0447\u0438\u0441\u043b\u0435\u043d\u0438\u0439 \u0441 \u043f\u0440\u0435\u0434\u043e\u043f\u043b\u0430\u0442\u043d\u044b\u0445 \u0441\u0447\u0435\u0442\u043e\u0432'
                },
                {
                    code: 'ViewPersons',
                    id: 11322,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u043e\u0432'
                },
                {
                    code: 'AdditionalFunctions',
                    id: 47,
                    name:
                        '\u0414\u043e\u043f\u043e\u043b\u043d\u0438\u0442\u0435\u043b\u044c\u043d\u044b\u0435 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438'
                },
                {
                    code: 'order_info__order_id',
                    id: 1012,
                    name:
                        '\u0421\u0432\u043e\u0439\u0441\u0442\u0432\u0430 \u0437\u0430\u043a\u0430\u0437\u0430 - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u0412\u043d\u0443\u0442\u0440\u0435\u043d\u043d\u0438\u0439 ID \u0437\u0430\u043a\u0430\u0437\u0430'
                },
                {
                    code: 'IssueAnyInvoices',
                    id: 17,
                    name:
                        '\u0414\u043e\u0432\u044b\u0441\u0442\u0430\u0432\u043b\u0435\u043d\u0438\u0435 \u0441\u0447\u0435\u0442\u0430 \u0437\u0430 \u043a\u043e\u0433\u043e \u0443\u0433\u043e\u0434\u043d\u043e'
                },
                {
                    code: 'DoInvoiceRefunds',
                    id: 4004,
                    name:
                        '\u0421\u043e\u0437\u0434\u0430\u043d\u0438\u0435 \u0432\u043e\u0437\u0432\u0440\u0430\u0442\u043e\u0432 \u043f\u043e \u0441\u0447\u0435\u0442\u0430\u043c'
                },
                {
                    code: 'orders_report__client_id',
                    id: 1005,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u0417\u0430\u043a\u0430\u0437\u044b - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 ID \u043a\u043b\u0438\u0435\u043d\u0442\u0430'
                },
                {
                    code: 'AlterFirmTR',
                    id: 41,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u043e\u0432 \u0444\u0438\u0440\u043c\u044b Yandex Turkey'
                },
                {
                    code: 'AlterInvoicePerson',
                    id: 11110,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u0430 \u0441\u0447\u0435\u0442\u0430'
                },
                {
                    code: 'deferpays_orders_report__invoice_eid',
                    id: 1013,
                    name:
                        '\u041e\u0442\u0447\u0435\u0442 \u041a\u0440\u0435\u0434\u0438\u0442\u044b - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u0424\u0438\u043a\u0442\u0438\u0432\u043d\u044b\u0439 \u0441\u0447\u0435\u0442'
                },
                {
                    code: 'SubstituteLogin',
                    id: 24,
                    name:
                        '\u041f\u043e\u0434\u043c\u0435\u043d\u0430 \u043b\u043e\u0433\u0438\u043d\u0430'
                },
                {
                    code: 'IssueInvoices',
                    id: 11106,
                    name:
                        '\u0412\u044b\u0441\u0442\u0430\u0432\u043b\u0435\u043d\u0438\u0435 \u0441\u0447\u0435\u0442\u043e\u0432'
                },
                {
                    code: 'ManageRBSPayments',
                    id: 19,
                    name:
                        '\u041e\u043f\u0435\u0440\u0430\u0446\u0438\u0438 \u0441 \u043f\u043b\u0430\u0442\u0435\u0436\u0430\u043c\u0438 \u043f\u043e RBS'
                },
                {
                    code: 'EditContracts',
                    id: 20,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u043e\u0432'
                },
                {
                    code: 'CloseInvoice',
                    id: 11222,
                    name:
                        '\u0417\u0430\u043a\u0440\u044b\u0442\u0438\u0435 \u0441\u0447\u0451\u0442\u0430'
                },
                {
                    code: 'SuperVisor',
                    id: 15,
                    name:
                        '\u0423\u0441\u0442\u0430\u0440\u0435\u0432\u0448\u0435\u0435, \u043d\u0435 \u0438\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u0442\u044c. SuperWisard'
                },
                {
                    code: 'SendInvoices',
                    id: 11102,
                    name:
                        '\u041e\u0442\u043f\u0440\u0430\u0432\u043a\u0430 \u0441\u0447\u0435\u0442\u043e\u0432 \u043d\u0430 \u043f\u043e\u0447\u0442\u0443'
                },
                {
                    code: 'ChangeRepaymentsStatus',
                    id: 11103,
                    name:
                        '\u041f\u043e\u0434\u0442\u0432\u0435\u0440\u0436\u0434\u0435\u043d\u0438\u0435 \u0441\u0447\u0435\u0442\u043e\u0432 \u043d\u0430 \u043f\u043e\u0433\u0430\u0448\u0435\u043d\u0438\u0435'
                },
                {
                    code: 'AlterInvoicePaysys',
                    id: 11109,
                    name:
                        '\u0418\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0435 \u0441\u043f\u043e\u0441\u043e\u0431\u0430 \u043e\u043f\u043b\u0430\u0442\u044b \u0441\u0447\u0435\u0442\u0430'
                }
            ]
        }
    }
];
