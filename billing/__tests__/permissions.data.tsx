import { HOST } from '../../../../common/utils/test-utils/common';
import { fullPerms } from './data';

export const noChangeRepaymentsStatus = {
    // Сделано для большего соответствия реальной ситуации. Механизм такой, что чекбокс блокируется в зависимости от ответа сервера
    perms: fullPerms.filter(perm => perm !== 'ChangeRepaymentsStatus'),
    clientId: 134809929, // test_repayment_1_order
    deferpayId: 13742871,
    client: {
        request: [`${HOST}/client`, { client_id: 134809929 }, false, false],
        response: {
            version: { snout: '1.0.327', muzzle: 'UNKNOWN', butils: '2.177' },
            data: {
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
                id: 134809929,
                printable_docs_type: 0,
                domain_check_status: 0,
                full_repayment: true,
                fraud_status: null,
                reliable_cc_payer: 0,
                'client-type': null,
                deny_overdraft: null,
                only_manual_name_update: null,
                manual_suspect: 0,
                internal: false,
                sms_notify: 2,
                type: { id: 0 },
                email: 'client@in-fo.ru',
                is_acquiring: false,
                fax: '912',
                region_name: null,
                parent_agency_id: null,
                city: '\u0411\u0430\u0442\u0442',
                deny_cc: 0,
                client_type_id: 0,
                is_non_resident: false,
                phone: '911',
                name: '\u041e\u041e\u041e "\u041a\u043b\u0438\u0435\u043d\u0442"',
                partner_type: '0',
                url: 'http://client.info/',
                force_contractless_invoice: false,
                non_resident_currency_payment: null,
                parent_agency_name: null,
                fullname: null,
                is_ctype_3: false
            }
        }
    },
    deferpayContracts: {
        request: [`${HOST}/deferpay/contracts`, { client_id: 134809929 }, false, false],
        response: {
            version: { snout: '1.0.327', muzzle: 'UNKNOWN', butils: '2.177' },
            data: [{ type: 'GENERAL', external_id: '1297422/19', id: 4245196 }]
        }
    },
    deferpayList: {
        request: [
            `${HOST}/deferpay/list`,
            {
                pagination_pn: 1,
                contract_id: 0,
                sort_key: 'ISSUE_DT',
                payment_status: 'UNDEFINED',
                pagination_ps: 10,
                sort_order: 'DESC',
                client_id: 134809929
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
                        client_logins: '',
                        deferpay_id: 13742871,
                        text: 'Py_Test order 7-1475',
                        qty: '50.000000',
                        repayment_status_id: 5,
                        issue_dt: '2020-06-08T12:31:53',
                        order_eid: '7-54592284',
                        unit: '\u0443.\u0435.',
                        paysys_nds: '1.00',
                        type_rate: 1,
                        contract_id: 4245196,
                        client_name: '\u041e\u041e\u041e "\u041a\u043b\u0438\u0435\u043d\u0442"',
                        discount_pct: '0.00',
                        repayment_invoice_eid: '\u0411-2825508874-1',
                        person_id: 12049283,
                        price: '30.000000',
                        repayment_dt: null,
                        person_name:
                            '\u041e\u041e\u041e "\u041f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a"',
                        status_id: 0,
                        paysys_currency: 'RUR',
                        sum_e: '1500.00',
                        person_repr: 'tPLLK',
                        agency_name: null,
                        client_id: 134809929,
                        service_group_id: 7,
                        order_client_name:
                            '\u041e\u041e\u041e "\u041a\u043b\u0438\u0435\u043d\u0442"',
                        paysys_id: 1003,
                        order_client_id: 134809929,
                        person_phone: '+7 812 3990776',
                        invoice_id: 113942022,
                        service_cc: 'PPC',
                        person_email: 'm-SC@qCWF.rKU',
                        repayment_invoice_id: 113942319,
                        invoice_eid: '\u0411-2825498082-1',
                        sum_nodiscnt: '1500.00',
                        service_order_id: 54592284,
                        service_id: 7
                    }
                ]
            }
        }
    }
};

export const noIssueInvoices = {
    perms: fullPerms.filter(perm => perm !== 'IssueInvoices'),
    clientId: 134835573, // test_repayment_1_order
    deferpayId: 13746091,
    client: {
        request: [`${HOST}/client`, { client_id: 134835573 }, false, false],
        response: {
            version: { snout: '1.0.327', muzzle: 'UNKNOWN', butils: '2.177' },
            data: {
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
                id: 134835573,
                printable_docs_type: 0,
                domain_check_status: 0,
                full_repayment: true,
                fraud_status: null,
                reliable_cc_payer: 0,
                'client-type': null,
                deny_overdraft: null,
                only_manual_name_update: null,
                manual_suspect: 0,
                internal: false,
                sms_notify: 2,
                type: { id: 0 },
                email: 'client@in-fo.ru',
                is_acquiring: false,
                fax: '912',
                region_name: null,
                parent_agency_id: null,
                city: '\u0411\u0430\u0442\u0442',
                deny_cc: 0,
                client_type_id: 0,
                is_non_resident: false,
                phone: '911',
                name: '\u041e\u041e\u041e "\u041a\u043b\u0438\u0435\u043d\u0442"',
                partner_type: '0',
                url: 'http://client.info/',
                force_contractless_invoice: false,
                non_resident_currency_payment: null,
                parent_agency_name: null,
                fullname: null,
                is_ctype_3: false
            }
        }
    },
    deferpayContracts: {
        request: [`${HOST}/deferpay/contracts`, { client_id: 134835573 }, false, false],
        response: {
            version: { snout: '1.0.327', muzzle: 'UNKNOWN', butils: '2.177' },
            data: [{ type: 'GENERAL', external_id: '1306321/19', id: 4255930 }]
        }
    },
    deferpayList: {
        request: [
            `${HOST}/deferpay/list`,
            {
                pagination_pn: 1,
                contract_id: 0,
                sort_key: 'ISSUE_DT',
                payment_status: 'UNDEFINED',
                pagination_ps: 10,
                sort_order: 'DESC',
                client_id: 134835573
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
                        client_logins: '',
                        deferpay_id: 13746091,
                        text: 'Py_Test order 7-1475',
                        qty: '50.000000',
                        repayment_status_id: null,
                        issue_dt: '2020-06-09T11:20:37',
                        order_eid: '7-54597605',
                        unit: '\u0443.\u0435.',
                        paysys_nds: '1.00',
                        type_rate: 1,
                        contract_id: 4255930,
                        client_name: '\u041e\u041e\u041e "\u041a\u043b\u0438\u0435\u043d\u0442"',
                        discount_pct: '0.00',
                        repayment_invoice_eid: null,
                        person_id: 12064379,
                        price: '30.000000',
                        repayment_dt: null,
                        person_name:
                            '\u041e\u041e\u041e "\u041f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a"',
                        status_id: 0,
                        paysys_currency: 'RUR',
                        sum_e: '1500.00',
                        person_repr: 'tPLLK',
                        agency_name: null,
                        client_id: 134835573,
                        service_group_id: 7,
                        order_client_name:
                            '\u041e\u041e\u041e "\u041a\u043b\u0438\u0435\u043d\u0442"',
                        paysys_id: 1003,
                        order_client_id: 134835573,
                        person_phone: '+7 812 3990776',
                        invoice_id: 113961116,
                        service_cc: 'PPC',
                        person_email: 'm-SC@qCWF.rKU',
                        repayment_invoice_id: null,
                        invoice_eid: '\u0411-2825574616-1',
                        sum_nodiscnt: '1500.00',
                        service_order_id: 54597605,
                        service_id: 7
                    }
                ]
            }
        }
    }
};

export const noViewPersons = {
    perms: fullPerms.filter(perm => perm !== 'ViewPersons'),
    clientId: 134835573, // test_repayment_1_order
    client: {
        request: [`${HOST}/client`, { client_id: 134835573 }, false, false],
        response: {
            version: { snout: '1.0.327', muzzle: 'UNKNOWN', butils: '2.177' },
            data: {
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
                id: 134835573,
                printable_docs_type: 0,
                domain_check_status: 0,
                full_repayment: true,
                fraud_status: null,
                reliable_cc_payer: 0,
                'client-type': null,
                deny_overdraft: null,
                only_manual_name_update: null,
                manual_suspect: 0,
                internal: false,
                sms_notify: 2,
                type: { id: 0 },
                email: 'client@in-fo.ru',
                is_acquiring: false,
                fax: '912',
                region_name: null,
                parent_agency_id: null,
                city: '\u0411\u0430\u0442\u0442',
                deny_cc: 0,
                client_type_id: 0,
                is_non_resident: false,
                phone: '911',
                name: '\u041e\u041e\u041e "\u041a\u043b\u0438\u0435\u043d\u0442"',
                partner_type: '0',
                url: 'http://client.info/',
                force_contractless_invoice: false,
                non_resident_currency_payment: null,
                parent_agency_name: null,
                fullname: null,
                is_ctype_3: false
            }
        }
    },
    deferpayContracts: {
        request: [`${HOST}/deferpay/contracts`, { client_id: 134835573 }, false, false],
        response: {
            version: { snout: '1.0.327', muzzle: 'UNKNOWN', butils: '2.177' },
            data: [{ type: 'GENERAL', external_id: '1306321/19', id: 4255930 }]
        }
    },
    deferpayList: {
        request: [
            `${HOST}/deferpay/list`,
            {
                pagination_pn: 1,
                contract_id: 0,
                sort_key: 'ISSUE_DT',
                payment_status: 'UNDEFINED',
                pagination_ps: 10,
                sort_order: 'DESC',
                client_id: 134835573
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
                        client_logins: '',
                        deferpay_id: 13746091,
                        text: 'Py_Test order 7-1475',
                        qty: '50.000000',
                        repayment_status_id: null,
                        issue_dt: '2020-06-09T11:20:37',
                        order_eid: '7-54597605',
                        unit: '\u0443.\u0435.',
                        paysys_nds: '1.00',
                        type_rate: 1,
                        contract_id: 4255930,
                        client_name: '\u041e\u041e\u041e "\u041a\u043b\u0438\u0435\u043d\u0442"',
                        discount_pct: '0.00',
                        repayment_invoice_eid: null,
                        person_id: 12064379,
                        price: '30.000000',
                        repayment_dt: null,
                        person_name:
                            '\u041e\u041e\u041e "\u041f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a"',
                        status_id: 0,
                        paysys_currency: 'RUR',
                        sum_e: '1500.00',
                        person_repr: 'tPLLK',
                        agency_name: null,
                        client_id: 134835573,
                        service_group_id: 7,
                        order_client_name:
                            '\u041e\u041e\u041e "\u041a\u043b\u0438\u0435\u043d\u0442"',
                        paysys_id: 1003,
                        order_client_id: 134835573,
                        person_phone: '+7 812 3990776',
                        invoice_id: 113961116,
                        service_cc: 'PPC',
                        person_email: 'm-SC@qCWF.rKU',
                        repayment_invoice_id: null,
                        invoice_eid: '\u0411-2825574616-1',
                        sum_nodiscnt: '1500.00',
                        service_order_id: 54597605,
                        service_id: 7
                    }
                ]
            }
        }
    }
};
