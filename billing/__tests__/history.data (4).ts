import { HOST } from 'common/utils/test-utils/common';

export const history = {
    search:
        '?client_id=410212&payment_status=6&service_cc=adfox&contract_id=204942&service_order_id=7-43203039&issue_dt_from=2019-09-01T00%3A00%3A00&issue_dt_to=2019-09-30T00%3A00%3A00&pn=1&ps=10&sf=issue_dt&so=0',
    filter: {
        agency: 'Форсайт',
        paymentStatus: 'PAID_INVOICE',
        service: '102',
        contract: '204942',
        orderId: '7-43203039',
        dateFrom: '2019-09-01T00:00:00',
        dateTo: '2019-09-30T00:00:00'
    },
    client: {
        request: [`${HOST}/client`, { client_id: 410212 }, false, false],
        response: {
            version: { snout: '1.0.327', muzzle: 'UNKNOWN', butils: '2.177' },
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
                has_edo: true,
                intercompany: null,
                id: 410212,
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
                type: { id: 2 },
                email: 'baraev@forsite.ru',
                is_acquiring: null,
                fax: null,
                region_name: '\u0420\u043e\u0441\u0441\u0438\u044f',
                parent_agency_id: 410212,
                city: '\u0420\u043e\u0441\u0442\u043e\u0432-\u043d\u0430-\u0414\u043e\u043d\u0443',
                deny_cc: 0,
                client_type_id: 2,
                is_non_resident: false,
                phone: '8 (800) 301-01-61, +7 (863) 333-01-21',
                name: '\u0424\u043e\u0440\u0441\u0430\u0439\u0442',
                partner_type: '0',
                url: 'www.forsite.ru',
                force_contractless_invoice: false,
                non_resident_currency_payment: null,
                parent_agency_name: '\u0424\u043e\u0440\u0441\u0430\u0439\u0442',
                fullname: null,
                is_ctype_3: true
            }
        }
    },
    deferpayContracts: {
        request: [`${HOST}/deferpay/contracts`, { client_id: 410212 }, false, false],
        response: {
            version: { snout: '1.0.327', muzzle: 'UNKNOWN', butils: '2.177' },
            data: [{ type: 'GENERAL', external_id: '34054/15', id: 204942 }]
        }
    },
    deferpayList: {
        request: [
            'http://snout-test/deferpay/list',
            {
                pagination_pn: 1,
                contract_id: 204942,
                sort_key: 'ISSUE_DT',
                payment_status: 'PAID_INVOICE',
                service_cc: 'adfox',
                dt_from: '2019-09-01T00:00:00',
                pagination_ps: 10,
                sort_order: 'DESC',
                service_order_id: '7-43203039',
                client_id: 410212,
                dt_to: '2019-09-30T00:00:00'
            },
            false,
            false
        ],
        response: {
            version: { snout: '1.0.327', muzzle: 'UNKNOWN', butils: '2.177' },
            data: {
                total_row_count: 1,
                items: [
                    {
                        client_logins: 'banket-hall-fs',
                        deferpay_id: 11376875,
                        text:
                            '\u041e\u0431\u0449\u0438\u0439 \u0441\u0447\u0435\u0442 (\u0430\u0433\u0435\u043d\u0442\u0441\u043a\u0438\u0439)',
                        qty: '4347.600000',
                        repayment_status_id: 0,
                        issue_dt: '2019-09-02T10:55:25',
                        order_eid: '7-43203039',
                        unit: '\u0434\u0435\u043d\u044c\u0433\u0438',
                        paysys_nds: '1.00',
                        type_rate: 1,
                        contract_id: 204942,
                        client_name: '\u0424\u043e\u0440\u0441\u0430\u0439\u0442',
                        discount_pct: '0.00',
                        repayment_invoice_eid: '\u0411-1891597659-1',
                        person_id: 206796,
                        price: '1.000000',
                        repayment_dt: '2019-10-11T16:24:11',
                        person_name: '\u0424\u043e\u0440\u0441\u0430\u0439\u0442',
                        status_id: 1,
                        paysys_currency: 'RUR',
                        sum_e: '4347.60',
                        person_repr: null,
                        agency_name: '\u0424\u043e\u0440\u0441\u0430\u0439\u0442',
                        client_id: 410212,
                        service_group_id: 7,
                        order_client_name:
                            '\u0411\u0430\u043d\u043a\u0435\u0442 \u0425\u043e\u043b\u043b',
                        paysys_id: 1003,
                        order_client_id: 59703090,
                        person_phone: '+7 (863) 333-01-21',
                        invoice_id: 100391596,
                        service_cc: 'PPC',
                        person_email: 'doc@forsite.ru;adv@forsite.ru;yandex.direct@forsite.ru',
                        repayment_invoice_id: 101927730,
                        invoice_eid: '\u0411-1859217831-1',
                        sum_nodiscnt: '4347.60',
                        service_order_id: 43203039,
                        service_id: 7
                    }
                ]
            }
        }
    }
};
