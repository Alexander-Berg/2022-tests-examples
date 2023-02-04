import { HOST } from 'common/utils/test-utils/common';

export const payOffOne = {
    clientId: 134809929, //test_fictive_1_order
    deferpayId: 13742871,
    actionName: 'PAY_OFF',
    actionDate: '2020-07-02T00:00:00',
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
                        repayment_status_id: null,
                        issue_dt: '2020-06-08T12:31:53',
                        order_eid: '7-54592284',
                        unit: '\u0443.\u0435.',
                        paysys_nds: '1.00',
                        type_rate: 1,
                        contract_id: 4245196,
                        client_name: '\u041e\u041e\u041e "\u041a\u043b\u0438\u0435\u043d\u0442"',
                        discount_pct: '0.00',
                        repayment_invoice_eid: null,
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
                        repayment_invoice_id: null,
                        invoice_eid: '\u0411-2825498082-1',
                        sum_nodiscnt: '1500.00',
                        service_order_id: 54592284,
                        service_id: 7
                    }
                ]
            }
        }
    },
    action: {
        request: [
            `${HOST}/deferpay/action/repayment-invoice`,
            {
                deferpay_ids: '13742871',
                invoice_dt: '2020-07-02T00:00:00',
                _csrf: 'csrf'
            },
            false
        ],
        response: {
            version: { snout: '1.0.327', muzzle: 'UNKNOWN', butils: '2.177' },
            data: { invoice_id: 113942319 }
        }
    }
};

export const confirmOne = {
    clientId: 134809934, // test_repayment_1_order
    deferpayId: 13742874,
    actionName: 'CONFIRM',
    client: {
        request: [`${HOST}/client`, { client_id: 134809934 }, false, false],
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
                id: 134809934,
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
        request: [`${HOST}/deferpay/contracts`, { client_id: 134809934 }, false, false],
        response: {
            version: { snout: '1.0.327', muzzle: 'UNKNOWN', butils: '2.177' },
            data: [{ type: 'GENERAL', external_id: '1297425/19', id: 4245199 }]
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
                client_id: 134809934
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
                        deferpay_id: 13742874,
                        text: 'Py_Test order 7-1475',
                        qty: '50.000000',
                        repayment_status_id: 5,
                        issue_dt: '2020-06-08T12:32:17',
                        order_eid: '7-54592287',
                        unit: '\u0443.\u0435.',
                        paysys_nds: '1.00',
                        type_rate: 1,
                        contract_id: 4245199,
                        client_name: '\u041e\u041e\u041e "\u041a\u043b\u0438\u0435\u043d\u0442"',
                        discount_pct: '0.00',
                        repayment_invoice_eid: '\u0411-2825498099-1',
                        person_id: 12049286,
                        price: '30.000000',
                        repayment_dt: null,
                        person_name:
                            '\u041e\u041e\u041e "\u041f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a"',
                        status_id: 0,
                        paysys_currency: 'RUR',
                        sum_e: '1500.00',
                        person_repr: 'tPLLK',
                        agency_name: null,
                        client_id: 134809934,
                        service_group_id: 7,
                        order_client_name:
                            '\u041e\u041e\u041e "\u041a\u043b\u0438\u0435\u043d\u0442"',
                        paysys_id: 1003,
                        order_client_id: 134809934,
                        person_phone: '+7 812 3990776',
                        invoice_id: 113942025,
                        service_cc: 'PPC',
                        person_email: 'm-SC@qCWF.rKU',
                        repayment_invoice_id: 113942026,
                        invoice_eid: '\u0411-2825498096-1',
                        sum_nodiscnt: '1500.00',
                        service_order_id: 54592287,
                        service_id: 7
                    }
                ]
            }
        }
    },
    action: {
        request: [
            `${HOST}/deferpay/action/confirm-invoices`,
            {
                deferpay_ids: '13742874',
                _csrf: 'csrf'
            },
            false
        ],
        response: {
            version: {
                snout: '1.0.327',
                muzzle: 'UNKNOWN',
                butils: '2.177'
            },
            data: null
        }
    }
};

export const deleteOne = {
    clientId: 134809938, //test_repayment_1_order
    deferpayId: 13742875,
    actionName: 'DELETE',
    client: {
        request: [`${HOST}/client`, { client_id: 134809938 }, false, false],
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
                id: 134809938,
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
        request: [`${HOST}/deferpay/contracts`, { client_id: 134809938 }, false, false],
        response: {
            version: { snout: '1.0.327', muzzle: 'UNKNOWN', butils: '2.177' },
            data: [{ type: 'GENERAL', external_id: '1297426/19', id: 4245200 }]
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
                client_id: 134809938
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
                        deferpay_id: 13742875,
                        text: 'Py_Test order 7-1475',
                        qty: '50.000000',
                        repayment_status_id: 5,
                        issue_dt: '2020-06-08T12:32:33',
                        order_eid: '7-54592288',
                        unit: '\u0443.\u0435.',
                        paysys_nds: '1.00',
                        type_rate: 1,
                        contract_id: 4245200,
                        client_name: '\u041e\u041e\u041e "\u041a\u043b\u0438\u0435\u043d\u0442"',
                        discount_pct: '0.00',
                        repayment_invoice_eid: '\u0411-2825498102-1',
                        person_id: 12049287,
                        price: '30.000000',
                        repayment_dt: null,
                        person_name:
                            '\u041e\u041e\u041e "\u041f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a"',
                        status_id: 0,
                        paysys_currency: 'RUR',
                        sum_e: '1500.00',
                        person_repr: 'tPLLK',
                        agency_name: null,
                        client_id: 134809938,
                        service_group_id: 7,
                        order_client_name:
                            '\u041e\u041e\u041e "\u041a\u043b\u0438\u0435\u043d\u0442"',
                        paysys_id: 1003,
                        order_client_id: 134809938,
                        person_phone: '+7 812 3990776',
                        invoice_id: 113942027,
                        service_cc: 'PPC',
                        person_email: 'm-SC@qCWF.rKU',
                        repayment_invoice_id: 113942028,
                        invoice_eid: '\u0411-2825498100-1',
                        sum_nodiscnt: '1500.00',
                        service_order_id: 54592288,
                        service_id: 7
                    }
                ]
            }
        }
    },
    action: {
        request: [
            `${HOST}/deferpay/action/decline-invoices`,
            {
                deferpay_ids: '13742875',
                _csrf: 'csrf'
            },
            false
        ],
        response: {
            version: {
                snout: '1.0.327',
                muzzle: 'UNKNOWN',
                butils: '2.177'
            },
            data: null
        }
    }
};

export const confirmThree = {
    clientId: 134809940, //test_repayment_3_orders
    deferpayId: 13742876,
    actionName: 'CONFIRM',
    client: {
        request: [`${HOST}/client`, { client_id: 134809940 }, false, false],
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
                id: 134809940,
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
        request: [`${HOST}/deferpay/contracts`, { client_id: 134809940 }, false, false],
        response: {
            version: { snout: '1.0.327', muzzle: 'UNKNOWN', butils: '2.177' },
            data: [{ type: 'GENERAL', external_id: '1297427/19', id: 4245201 }]
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
                client_id: 134809940
            },
            false,
            false
        ],
        response: {
            version: { snout: '1.0.327', muzzle: 'UNKNOWN', butils: '2.177' },
            data: {
                total_row_count: 3,
                items: [
                    {
                        client_logins: '',
                        deferpay_id: 13742876,
                        text: 'Py_Test order 7-1475',
                        qty: '50.000000',
                        repayment_status_id: 5,
                        issue_dt: '2020-06-08T12:32:48',
                        order_eid: '7-54592289',
                        unit: '\u0443.\u0435.',
                        paysys_nds: '1.00',
                        type_rate: 1,
                        contract_id: 4245201,
                        client_name: '\u041e\u041e\u041e "\u041a\u043b\u0438\u0435\u043d\u0442"',
                        discount_pct: '0.00',
                        repayment_invoice_eid: '\u0411-2825498107-1',
                        person_id: 12049288,
                        price: '30.000000',
                        repayment_dt: null,
                        person_name:
                            '\u041e\u041e\u041e "\u041f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a"',
                        status_id: 0,
                        paysys_currency: 'RUR',
                        sum_e: '1500.00',
                        person_repr: 'tPLLK',
                        agency_name: null,
                        client_id: 134809940,
                        service_group_id: 7,
                        order_client_name:
                            '\u041e\u041e\u041e "\u041a\u043b\u0438\u0435\u043d\u0442"',
                        paysys_id: 1003,
                        order_client_id: 134809940,
                        person_phone: '+7 812 3990776',
                        invoice_id: 113942029,
                        service_cc: 'PPC',
                        person_email: 'm-SC@qCWF.rKU',
                        repayment_invoice_id: 113942030,
                        invoice_eid: '\u0411-2825498106-1',
                        sum_nodiscnt: '1500.00',
                        service_order_id: 54592289,
                        service_id: 7
                    },
                    {
                        client_logins: '',
                        deferpay_id: 13742876,
                        text: 'Py_Test order 7-1475',
                        qty: '50.000000',
                        repayment_status_id: 5,
                        issue_dt: '2020-06-08T12:32:48',
                        order_eid: '7-54592290',
                        unit: '\u0443.\u0435.',
                        paysys_nds: '1.00',
                        type_rate: 1,
                        contract_id: 4245201,
                        client_name: '\u041e\u041e\u041e "\u041a\u043b\u0438\u0435\u043d\u0442"',
                        discount_pct: '0.00',
                        repayment_invoice_eid: '\u0411-2825498107-1',
                        person_id: 12049288,
                        price: '30.000000',
                        repayment_dt: null,
                        person_name:
                            '\u041e\u041e\u041e "\u041f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a"',
                        status_id: 0,
                        paysys_currency: 'RUR',
                        sum_e: '1500.00',
                        person_repr: 'tPLLK',
                        agency_name: null,
                        client_id: 134809940,
                        service_group_id: 7,
                        order_client_name:
                            '\u041e\u041e\u041e "\u041a\u043b\u0438\u0435\u043d\u0442"',
                        paysys_id: 1003,
                        order_client_id: 134809940,
                        person_phone: '+7 812 3990776',
                        invoice_id: 113942029,
                        service_cc: 'PPC',
                        person_email: 'm-SC@qCWF.rKU',
                        repayment_invoice_id: 113942030,
                        invoice_eid: '\u0411-2825498106-1',
                        sum_nodiscnt: '1500.00',
                        service_order_id: 54592290,
                        service_id: 7
                    },
                    {
                        client_logins: '',
                        deferpay_id: 13742876,
                        text: 'Py_Test order 7-1475',
                        qty: '50.000000',
                        repayment_status_id: 5,
                        issue_dt: '2020-06-08T12:32:48',
                        order_eid: '7-54592291',
                        unit: '\u0443.\u0435.',
                        paysys_nds: '1.00',
                        type_rate: 1,
                        contract_id: 4245201,
                        client_name: '\u041e\u041e\u041e "\u041a\u043b\u0438\u0435\u043d\u0442"',
                        discount_pct: '0.00',
                        repayment_invoice_eid: '\u0411-2825498107-1',
                        person_id: 12049288,
                        price: '30.000000',
                        repayment_dt: null,
                        person_name:
                            '\u041e\u041e\u041e "\u041f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a"',
                        status_id: 0,
                        paysys_currency: 'RUR',
                        sum_e: '1500.00',
                        person_repr: 'tPLLK',
                        agency_name: null,
                        client_id: 134809940,
                        service_group_id: 7,
                        order_client_name:
                            '\u041e\u041e\u041e "\u041a\u043b\u0438\u0435\u043d\u0442"',
                        paysys_id: 1003,
                        order_client_id: 134809940,
                        person_phone: '+7 812 3990776',
                        invoice_id: 113942029,
                        service_cc: 'PPC',
                        person_email: 'm-SC@qCWF.rKU',
                        repayment_invoice_id: 113942030,
                        invoice_eid: '\u0411-2825498106-1',
                        sum_nodiscnt: '1500.00',
                        service_order_id: 54592291,
                        service_id: 7
                    }
                ]
            }
        }
    },
    action: {
        request: [
            `${HOST}/deferpay/action/confirm-invoices`,
            {
                deferpay_ids: '13742876',
                _csrf: 'csrf'
            },
            false
        ],
        response: {
            version: { snout: '1.0.327', muzzle: 'UNKNOWN', butils: '2.177' },
            data: null
        }
    }
};

export const deleteThree = {
    clientId: 134809944, //test_repayment_3_orders
    deferpayId: 13742877,
    actionName: 'DELETE',
    client: {
        request: [`${HOST}/client`, { client_id: 134809944 }, false, false],
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
                id: 134809944,
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
        request: [`${HOST}/deferpay/contracts`, { client_id: 134809944 }, false, false],
        response: {
            version: { snout: '1.0.327', muzzle: 'UNKNOWN', butils: '2.177' },
            data: [{ type: 'GENERAL', external_id: '1297428/19', id: 4245202 }]
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
                client_id: 134809944
            },
            false,
            false
        ],
        response: {
            version: { snout: '1.0.327', muzzle: 'UNKNOWN', butils: '2.177' },
            data: {
                total_row_count: 3,
                items: [
                    {
                        client_logins: '',
                        deferpay_id: 13742877,
                        text: 'Py_Test order 7-1475',
                        qty: '50.000000',
                        repayment_status_id: 5,
                        issue_dt: '2020-06-08T12:33:03',
                        order_eid: '7-54592294',
                        unit: '\u0443.\u0435.',
                        paysys_nds: '1.00',
                        type_rate: 1,
                        contract_id: 4245202,
                        client_name: '\u041e\u041e\u041e "\u041a\u043b\u0438\u0435\u043d\u0442"',
                        discount_pct: '0.00',
                        repayment_invoice_eid: '\u0411-2825498110-1',
                        person_id: 12049289,
                        price: '30.000000',
                        repayment_dt: null,
                        person_name:
                            '\u041e\u041e\u041e "\u041f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a"',
                        status_id: 0,
                        paysys_currency: 'RUR',
                        sum_e: '1500.00',
                        person_repr: 'tPLLK',
                        agency_name: null,
                        client_id: 134809944,
                        service_group_id: 7,
                        order_client_name:
                            '\u041e\u041e\u041e "\u041a\u043b\u0438\u0435\u043d\u0442"',
                        paysys_id: 1003,
                        order_client_id: 134809944,
                        person_phone: '+7 812 3990776',
                        invoice_id: 113942031,
                        service_cc: 'PPC',
                        person_email: 'm-SC@qCWF.rKU',
                        repayment_invoice_id: 113942032,
                        invoice_eid: '\u0411-2825498109-1',
                        sum_nodiscnt: '1500.00',
                        service_order_id: 54592294,
                        service_id: 7
                    },
                    {
                        client_logins: '',
                        deferpay_id: 13742877,
                        text: 'Py_Test order 7-1475',
                        qty: '50.000000',
                        repayment_status_id: 5,
                        issue_dt: '2020-06-08T12:33:03',
                        order_eid: '7-54592293',
                        unit: '\u0443.\u0435.',
                        paysys_nds: '1.00',
                        type_rate: 1,
                        contract_id: 4245202,
                        client_name: '\u041e\u041e\u041e "\u041a\u043b\u0438\u0435\u043d\u0442"',
                        discount_pct: '0.00',
                        repayment_invoice_eid: '\u0411-2825498110-1',
                        person_id: 12049289,
                        price: '30.000000',
                        repayment_dt: null,
                        person_name:
                            '\u041e\u041e\u041e "\u041f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a"',
                        status_id: 0,
                        paysys_currency: 'RUR',
                        sum_e: '1500.00',
                        person_repr: 'tPLLK',
                        agency_name: null,
                        client_id: 134809944,
                        service_group_id: 7,
                        order_client_name:
                            '\u041e\u041e\u041e "\u041a\u043b\u0438\u0435\u043d\u0442"',
                        paysys_id: 1003,
                        order_client_id: 134809944,
                        person_phone: '+7 812 3990776',
                        invoice_id: 113942031,
                        service_cc: 'PPC',
                        person_email: 'm-SC@qCWF.rKU',
                        repayment_invoice_id: 113942032,
                        invoice_eid: '\u0411-2825498109-1',
                        sum_nodiscnt: '1500.00',
                        service_order_id: 54592293,
                        service_id: 7
                    },
                    {
                        client_logins: '',
                        deferpay_id: 13742877,
                        text: 'Py_Test order 7-1475',
                        qty: '50.000000',
                        repayment_status_id: 5,
                        issue_dt: '2020-06-08T12:33:03',
                        order_eid: '7-54592292',
                        unit: '\u0443.\u0435.',
                        paysys_nds: '1.00',
                        type_rate: 1,
                        contract_id: 4245202,
                        client_name: '\u041e\u041e\u041e "\u041a\u043b\u0438\u0435\u043d\u0442"',
                        discount_pct: '0.00',
                        repayment_invoice_eid: '\u0411-2825498110-1',
                        person_id: 12049289,
                        price: '30.000000',
                        repayment_dt: null,
                        person_name:
                            '\u041e\u041e\u041e "\u041f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a"',
                        status_id: 0,
                        paysys_currency: 'RUR',
                        sum_e: '1500.00',
                        person_repr: 'tPLLK',
                        agency_name: null,
                        client_id: 134809944,
                        service_group_id: 7,
                        order_client_name:
                            '\u041e\u041e\u041e "\u041a\u043b\u0438\u0435\u043d\u0442"',
                        paysys_id: 1003,
                        order_client_id: 134809944,
                        person_phone: '+7 812 3990776',
                        invoice_id: 113942031,
                        service_cc: 'PPC',
                        person_email: 'm-SC@qCWF.rKU',
                        repayment_invoice_id: 113942032,
                        invoice_eid: '\u0411-2825498109-1',
                        sum_nodiscnt: '1500.00',
                        service_order_id: 54592292,
                        service_id: 7
                    }
                ]
            }
        }
    },
    action: {
        request: [
            `${HOST}/deferpay/action/decline-invoices`,
            {
                deferpay_ids: '13742877',
                _csrf: 'csrf'
            },
            false
        ],
        response: {
            version: { snout: '1.0.327', muzzle: 'UNKNOWN', butils: '2.177' },
            data: null
        }
    }
};

export const confirmTwo = {
    clientId: 134809946, //test_repayment_2_invoices
    deferpayIds: [13742879, 13742878],
    actionName: 'CONFIRM',
    client: {
        request: [`${HOST}/client`, { client_id: 134809946 }, false, false],
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
                id: 134809946,
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
        request: [`${HOST}/deferpay/contracts`, { client_id: 134809946 }, false, false],
        response: {
            version: { snout: '1.0.327', muzzle: 'UNKNOWN', butils: '2.177' },
            data: [
                { type: 'GENERAL', external_id: '1297430/19', id: 4245204 },
                { type: 'GENERAL', external_id: '1297429/19', id: 4245203 }
            ]
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
                client_id: 134809946
            },
            false,
            false
        ],
        response: {
            version: { snout: '1.0.327', muzzle: 'UNKNOWN', butils: '2.177' },
            data: {
                total_row_count: 2,
                items: [
                    {
                        client_logins: '',
                        deferpay_id: 13742879,
                        text: 'Py_Test order 7-1475',
                        qty: '50.000000',
                        repayment_status_id: 5,
                        issue_dt: '2020-06-08T12:33:28',
                        order_eid: '7-54592296',
                        unit: '\u0443.\u0435.',
                        paysys_nds: '1.00',
                        type_rate: 1,
                        contract_id: 4245204,
                        client_name: '\u041e\u041e\u041e "\u041a\u043b\u0438\u0435\u043d\u0442"',
                        discount_pct: '0.00',
                        repayment_invoice_eid: '\u0411-2825498118-1',
                        person_id: 12049291,
                        price: '30.000000',
                        repayment_dt: null,
                        person_name:
                            '\u041e\u041e\u041e "\u041f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a"',
                        status_id: 0,
                        paysys_currency: 'RUR',
                        sum_e: '1500.00',
                        person_repr: 'tPLLK',
                        agency_name: null,
                        client_id: 134809946,
                        service_group_id: 7,
                        order_client_name:
                            '\u041e\u041e\u041e "\u041a\u043b\u0438\u0435\u043d\u0442"',
                        paysys_id: 1003,
                        order_client_id: 134809946,
                        person_phone: '+7 812 3990776',
                        invoice_id: 113942035,
                        service_cc: 'PPC',
                        person_email: 'm-SC@qCWF.rKU',
                        repayment_invoice_id: 113942036,
                        invoice_eid: '\u0411-2825498117-1',
                        sum_nodiscnt: '1500.00',
                        service_order_id: 54592296,
                        service_id: 7
                    },
                    {
                        client_logins: '',
                        deferpay_id: 13742878,
                        text: 'Py_Test order 7-1475',
                        qty: '50.000000',
                        repayment_status_id: 5,
                        issue_dt: '2020-06-08T12:33:17',
                        order_eid: '7-54592295',
                        unit: '\u0443.\u0435.',
                        paysys_nds: '1.00',
                        type_rate: 1,
                        contract_id: 4245203,
                        client_name: '\u041e\u041e\u041e "\u041a\u043b\u0438\u0435\u043d\u0442"',
                        discount_pct: '0.00',
                        repayment_invoice_eid: '\u0411-2825498115-1',
                        person_id: 12049290,
                        price: '30.000000',
                        repayment_dt: null,
                        person_name:
                            '\u041e\u041e\u041e "\u041f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a"',
                        status_id: 0,
                        paysys_currency: 'RUR',
                        sum_e: '1500.00',
                        person_repr: 'tPLLK',
                        agency_name: null,
                        client_id: 134809946,
                        service_group_id: 7,
                        order_client_name:
                            '\u041e\u041e\u041e "\u041a\u043b\u0438\u0435\u043d\u0442"',
                        paysys_id: 1003,
                        order_client_id: 134809946,
                        person_phone: '+7 812 3990776',
                        invoice_id: 113942033,
                        service_cc: 'PPC',
                        person_email: 'm-SC@qCWF.rKU',
                        repayment_invoice_id: 113942034,
                        invoice_eid: '\u0411-2825498113-1',
                        sum_nodiscnt: '1500.00',
                        service_order_id: 54592295,
                        service_id: 7
                    }
                ]
            }
        }
    },
    action: {
        request: [
            `${HOST}/deferpay/action/confirm-invoices`,
            {
                deferpay_ids: '13742878,13742879',
                _csrf: 'csrf'
            },
            false
        ],
        response: {
            version: { snout: '1.0.327', muzzle: 'UNKNOWN', butils: '2.177' },
            data: null
        }
    }
};

export const deleteTwo = {
    clientId: 134809950, //test_repayment_2_invoices
    deferpayIds: [13742881, 13742880],
    actionName: 'DELETE',
    client: {
        request: [`${HOST}/client`, { client_id: 134809950 }, false, false],
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
                id: 134809950,
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
        request: [`${HOST}/deferpay/contracts`, { client_id: 134809950 }, false, false],
        response: {
            version: { snout: '1.0.327', muzzle: 'UNKNOWN', butils: '2.177' },
            data: [
                { type: 'GENERAL', external_id: '1297431/19', id: 4245205 },
                { type: 'GENERAL', external_id: '1297432/19', id: 4245206 }
            ]
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
                client_id: 134809950
            },
            false,
            false
        ],
        response: {
            version: { snout: '1.0.327', muzzle: 'UNKNOWN', butils: '2.177' },
            data: {
                total_row_count: 2,
                items: [
                    {
                        client_logins: '',
                        deferpay_id: 13742881,
                        text: 'Py_Test order 7-1475',
                        qty: '50.000000',
                        repayment_status_id: 5,
                        issue_dt: '2020-06-08T12:33:54',
                        order_eid: '7-54592298',
                        unit: '\u0443.\u0435.',
                        paysys_nds: '1.00',
                        type_rate: 1,
                        contract_id: 4245206,
                        client_name: '\u041e\u041e\u041e "\u041a\u043b\u0438\u0435\u043d\u0442"',
                        discount_pct: '0.00',
                        repayment_invoice_eid: '\u0411-2825498136-1',
                        person_id: 12049293,
                        price: '30.000000',
                        repayment_dt: null,
                        person_name:
                            '\u041e\u041e\u041e "\u041f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a"',
                        status_id: 0,
                        paysys_currency: 'RUR',
                        sum_e: '1500.00',
                        person_repr: 'tPLLK',
                        agency_name: null,
                        client_id: 134809950,
                        service_group_id: 7,
                        order_client_name:
                            '\u041e\u041e\u041e "\u041a\u043b\u0438\u0435\u043d\u0442"',
                        paysys_id: 1003,
                        order_client_id: 134809950,
                        person_phone: '+7 812 3990776',
                        invoice_id: 113942039,
                        service_cc: 'PPC',
                        person_email: 'm-SC@qCWF.rKU',
                        repayment_invoice_id: 113942040,
                        invoice_eid: '\u0411-2825498133-1',
                        sum_nodiscnt: '1500.00',
                        service_order_id: 54592298,
                        service_id: 7
                    },
                    {
                        client_logins: '',
                        deferpay_id: 13742880,
                        text: 'Py_Test order 7-1475',
                        qty: '50.000000',
                        repayment_status_id: 5,
                        issue_dt: '2020-06-08T12:33:43',
                        order_eid: '7-54592297',
                        unit: '\u0443.\u0435.',
                        paysys_nds: '1.00',
                        type_rate: 1,
                        contract_id: 4245205,
                        client_name: '\u041e\u041e\u041e "\u041a\u043b\u0438\u0435\u043d\u0442"',
                        discount_pct: '0.00',
                        repayment_invoice_eid: '\u0411-2825498128-1',
                        person_id: 12049292,
                        price: '30.000000',
                        repayment_dt: null,
                        person_name:
                            '\u041e\u041e\u041e "\u041f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a"',
                        status_id: 0,
                        paysys_currency: 'RUR',
                        sum_e: '1500.00',
                        person_repr: 'tPLLK',
                        agency_name: null,
                        client_id: 134809950,
                        service_group_id: 7,
                        order_client_name:
                            '\u041e\u041e\u041e "\u041a\u043b\u0438\u0435\u043d\u0442"',
                        paysys_id: 1003,
                        order_client_id: 134809950,
                        person_phone: '+7 812 3990776',
                        invoice_id: 113942037,
                        service_cc: 'PPC',
                        person_email: 'm-SC@qCWF.rKU',
                        repayment_invoice_id: 113942038,
                        invoice_eid: '\u0411-2825498126-1',
                        sum_nodiscnt: '1500.00',
                        service_order_id: 54592297,
                        service_id: 7
                    }
                ]
            }
        }
    },
    action: {
        request: [
            `${HOST}/deferpay/action/decline-invoices`,
            {
                deferpay_ids: '13742880,13742881',
                _csrf: 'csrf'
            },
            false
        ],
        response: {
            version: { snout: '1.0.327', muzzle: 'UNKNOWN', butils: '2.177' },
            data: null
        }
    }
};

export const blockWithoutFictive = {
    clientId: 134809952, //test_fictive_and_repayment_invoices
    deferpayId: 13742883,
    disabledDeferpayId: 13742882,

    client: {
        request: [`${HOST}/client`, { client_id: 134809952 }, false, false],
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
                id: 134809952,
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
        request: [`${HOST}/deferpay/contracts`, { client_id: 134809952 }, false, false],
        response: {
            version: { snout: '1.0.327', muzzle: 'UNKNOWN', butils: '2.177' },
            data: [
                { type: 'GENERAL', external_id: '1297434/19', id: 4245208 },
                { type: 'GENERAL', external_id: '1297433/19', id: 4245207 }
            ]
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
                client_id: 134809952
            },
            false,
            false
        ],
        response: {
            version: { snout: '1.0.327', muzzle: 'UNKNOWN', butils: '2.177' },
            data: {
                total_row_count: 2,
                items: [
                    {
                        client_logins: '',
                        deferpay_id: 13742883,
                        text: 'Py_Test order 7-1475',
                        qty: '50.000000',
                        repayment_status_id: 5,
                        issue_dt: '2020-06-08T12:34:17',
                        order_eid: '7-54592300',
                        unit: '\u0443.\u0435.',
                        paysys_nds: '1.00',
                        type_rate: 1,
                        contract_id: 4245208,
                        client_name: '\u041e\u041e\u041e "\u041a\u043b\u0438\u0435\u043d\u0442"',
                        discount_pct: '0.00',
                        repayment_invoice_eid: '\u0411-2825498156-1',
                        person_id: 12049295,
                        price: '30.000000',
                        repayment_dt: null,
                        person_name:
                            '\u041e\u041e\u041e "\u041f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a"',
                        status_id: 0,
                        paysys_currency: 'RUR',
                        sum_e: '1500.00',
                        person_repr: 'tPLLK',
                        agency_name: null,
                        client_id: 134809952,
                        service_group_id: 7,
                        order_client_name:
                            '\u041e\u041e\u041e "\u041a\u043b\u0438\u0435\u043d\u0442"',
                        paysys_id: 1003,
                        order_client_id: 134809952,
                        person_phone: '+7 812 3990776',
                        invoice_id: 113942042,
                        service_cc: 'PPC',
                        person_email: 'm-SC@qCWF.rKU',
                        repayment_invoice_id: 113942043,
                        invoice_eid: '\u0411-2825498155-1',
                        sum_nodiscnt: '1500.00',
                        service_order_id: 54592300,
                        service_id: 7
                    },
                    {
                        client_logins: '',
                        deferpay_id: 13742882,
                        text: 'Py_Test order 7-1475',
                        qty: '50.000000',
                        repayment_status_id: null,
                        issue_dt: '2020-06-08T12:34:07',
                        order_eid: '7-54592299',
                        unit: '\u0443.\u0435.',
                        paysys_nds: '1.00',
                        type_rate: 1,
                        contract_id: 4245207,
                        client_name: '\u041e\u041e\u041e "\u041a\u043b\u0438\u0435\u043d\u0442"',
                        discount_pct: '0.00',
                        repayment_invoice_eid: null,
                        person_id: 12049294,
                        price: '30.000000',
                        repayment_dt: null,
                        person_name:
                            '\u041e\u041e\u041e "\u041f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a"',
                        status_id: 0,
                        paysys_currency: 'RUR',
                        sum_e: '1500.00',
                        person_repr: 'tPLLK',
                        agency_name: null,
                        client_id: 134809952,
                        service_group_id: 7,
                        order_client_name:
                            '\u041e\u041e\u041e "\u041a\u043b\u0438\u0435\u043d\u0442"',
                        paysys_id: 1003,
                        order_client_id: 134809952,
                        person_phone: '+7 812 3990776',
                        invoice_id: 113942041,
                        service_cc: 'PPC',
                        person_email: 'm-SC@qCWF.rKU',
                        repayment_invoice_id: null,
                        invoice_eid: '\u0411-2825498150-1',
                        sum_nodiscnt: '1500.00',
                        service_order_id: 54592299,
                        service_id: 7
                    }
                ]
            }
        }
    }
};

export const blockWithFictive = {
    clientId: 134809953, //test_fictive_and_repayment_invoices
    deferpayId: 13742884,
    disabledDeferpayId: 13742885,

    client: {
        request: [`${HOST}/client`, { client_id: 134809953 }, false, false],
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
                id: 134809953,
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
        request: [`${HOST}/deferpay/contracts`, { client_id: 134809953 }, false, false],
        response: {
            version: { snout: '1.0.327', muzzle: 'UNKNOWN', butils: '2.177' },
            data: [
                { type: 'GENERAL', external_id: '1297436/19', id: 4245210 },
                { type: 'GENERAL', external_id: '1297435/19', id: 4245209 }
            ]
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
                client_id: 134809953
            },
            false,
            false
        ],
        response: {
            version: { snout: '1.0.327', muzzle: 'UNKNOWN', butils: '2.177' },
            data: {
                total_row_count: 2,
                items: [
                    {
                        client_logins: '',
                        deferpay_id: 13742885,
                        text: 'Py_Test order 7-1475',
                        qty: '50.000000',
                        repayment_status_id: 5,
                        issue_dt: '2020-06-08T12:34:40',
                        order_eid: '7-54592302',
                        unit: '\u0443.\u0435.',
                        paysys_nds: '1.00',
                        type_rate: 1,
                        contract_id: 4245210,
                        client_name: '\u041e\u041e\u041e "\u041a\u043b\u0438\u0435\u043d\u0442"',
                        discount_pct: '0.00',
                        repayment_invoice_eid: '\u0411-2825498163-1',
                        person_id: 12049297,
                        price: '30.000000',
                        repayment_dt: null,
                        person_name:
                            '\u041e\u041e\u041e "\u041f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a"',
                        status_id: 0,
                        paysys_currency: 'RUR',
                        sum_e: '1500.00',
                        person_repr: 'tPLLK',
                        agency_name: null,
                        client_id: 134809953,
                        service_group_id: 7,
                        order_client_name:
                            '\u041e\u041e\u041e "\u041a\u043b\u0438\u0435\u043d\u0442"',
                        paysys_id: 1003,
                        order_client_id: 134809953,
                        person_phone: '+7 812 3990776',
                        invoice_id: 113942045,
                        service_cc: 'PPC',
                        person_email: 'm-SC@qCWF.rKU',
                        repayment_invoice_id: 113942046,
                        invoice_eid: '\u0411-2825498162-1',
                        sum_nodiscnt: '1500.00',
                        service_order_id: 54592302,
                        service_id: 7
                    },
                    {
                        client_logins: '',
                        deferpay_id: 13742884,
                        text: 'Py_Test order 7-1475',
                        qty: '50.000000',
                        repayment_status_id: null,
                        issue_dt: '2020-06-08T12:34:32',
                        order_eid: '7-54592301',
                        unit: '\u0443.\u0435.',
                        paysys_nds: '1.00',
                        type_rate: 1,
                        contract_id: 4245209,
                        client_name: '\u041e\u041e\u041e "\u041a\u043b\u0438\u0435\u043d\u0442"',
                        discount_pct: '0.00',
                        repayment_invoice_eid: null,
                        person_id: 12049296,
                        price: '30.000000',
                        repayment_dt: null,
                        person_name:
                            '\u041e\u041e\u041e "\u041f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a"',
                        status_id: 0,
                        paysys_currency: 'RUR',
                        sum_e: '1500.00',
                        person_repr: 'tPLLK',
                        agency_name: null,
                        client_id: 134809953,
                        service_group_id: 7,
                        order_client_name:
                            '\u041e\u041e\u041e "\u041a\u043b\u0438\u0435\u043d\u0442"',
                        paysys_id: 1003,
                        order_client_id: 134809953,
                        person_phone: '+7 812 3990776',
                        invoice_id: 113942044,
                        service_cc: 'PPC',
                        person_email: 'm-SC@qCWF.rKU',
                        repayment_invoice_id: null,
                        invoice_eid: '\u0411-2825498160-1',
                        sum_nodiscnt: '1500.00',
                        service_order_id: 54592301,
                        service_id: 7
                    }
                ]
            }
        }
    }
};
