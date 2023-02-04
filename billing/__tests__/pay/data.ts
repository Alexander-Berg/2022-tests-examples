import { HOST } from 'common/utils/test-utils/common';

export const permsReadonly = ['NewUIEarlyAdopter', 'AdminAccess'];

export const mocks = {
    readonlyObjectPermissions: {
        request: [
            `${HOST}/user/object-permissions`,
            { classname: 'Invoice', object_id: '123' },
            false,
            false
        ],
        response: {
            version: { 'yb-snout-api': '3.95.2' },
            data: [
                {
                    code: 'ViewAllManagerReports',
                    id: 12,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442\u044b \u0434\u043b\u044f \u043c\u0435\u043d\u0435\u0434\u0436\u0435\u0440\u043e\u0432'
                },
                {
                    code: 'deferpays_orders_report__invoice_eid',
                    id: 1013,
                    name:
                        '\u041e\u0442\u0447\u0435\u0442 \u041a\u0440\u0435\u0434\u0438\u0442\u044b - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u0424\u0438\u043a\u0442\u0438\u0432\u043d\u044b\u0439 \u0441\u0447\u0435\u0442'
                },
                {
                    code: 'ui_orders_report__client_id',
                    id: 1008,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u0417\u0430\u043a\u0430\u0437\u044b \u0432 \u041f\u0418 - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 ID \u043a\u043b\u0438\u0435\u043d\u0442\u0430'
                },
                {
                    code: 'SendActs',
                    id: 11323,
                    name:
                        '\u041e\u0442\u043f\u0440\u0430\u0432\u0438\u0442\u044c \u0430\u043a\u0442\u044b \u043f\u043e \u044d\u043b\u0435\u043a\u0442\u0440\u043e\u043d\u043d\u043e\u0439 \u0438\u043b\u0438 \u043e\u0431\u044b\u043d\u043e\u0439 \u043f\u043e\u0447\u0442\u0435'
                },
                {
                    code: 'invoices_report__receipt_sum_1c',
                    id: 1001,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u0421\u0447\u0435\u0442\u0430 - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u0421\u0443\u043c\u043c\u0430 \u043f\u0440\u0438\u0445\u043e\u0434\u0430 \u0438\u0437 1\u0421'
                },
                {
                    code: 'ui_orders_report__manager_code',
                    id: 1009,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u0417\u0430\u043a\u0430\u0437\u044b \u0432 \u041f\u0418 - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u041c\u0435\u043d\u0435\u0434\u0436\u0435\u0440'
                },
                {
                    code: 'assist_payments_report__postauth_status',
                    id: 1003,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u041f\u043b\u0430\u0442\u0435\u0436\u0438 \u043f\u043e Assist - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u0421\u0442\u0430\u0442\u0443\u0441 \u043f\u043e\u0441\u0442-\u0430\u0432\u0442\u043e\u0440\u0438\u0437\u0430\u0446\u0438\u0438'
                },
                {
                    code: 'ViewAllInvoices',
                    id: 1,
                    name:
                        '\u0423\u0441\u0442\u0430\u0440\u0435\u0432\u0448\u0435\u0435, \u043d\u0435 \u0438\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u0442\u044c. \u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u0441\u0447\u0435\u0442\u043e\u0432.'
                },
                {
                    code: 'PersonView',
                    id: 25,
                    name:
                        '\u0423\u0441\u0442\u0430\u0440\u0435\u0432\u0448\u0435\u0435, \u043d\u0435 \u0438\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u0442\u044c. \u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u0430.'
                },
                {
                    code: 'invoices_report__manager_info',
                    id: 1006,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u0421\u0447\u0435\u0442\u0430 - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u041c\u0435\u043d\u0435\u0434\u0436\u0435\u0440'
                },
                {
                    code: 'ViewContracts',
                    id: 11181,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u043e\u0432'
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
                    code: 'invoices_report__client',
                    id: 1002,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u0421\u0447\u0435\u0442\u0430 - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 ID \u043a\u043b\u0438\u0435\u043d\u0442\u0430'
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
                    code: 'consumes_list__consume_qty',
                    id: 1010,
                    name:
                        '\u0421\u043f\u0438\u0441\u043e\u043a \u0437\u0430\u044f\u0432\u043e\u043a - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u041f\u0435\u0440\u0432\u043e\u043d\u0430\u0447\u0430\u043b\u044c\u043d\u043e\u0435 \u043a\u043e\u043b\u0438\u0447\u0435\u0441\u0442\u0432\u043e'
                },
                {
                    code: 'ViewOrders',
                    id: 11201,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u0437\u0430\u043a\u0430\u0437\u043e\u0432'
                },
                {
                    code: 'order_info__order_id',
                    id: 1012,
                    name:
                        '\u0421\u0432\u043e\u0439\u0441\u0442\u0432\u0430 \u0437\u0430\u043a\u0430\u0437\u0430 - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u0412\u043d\u0443\u0442\u0440\u0435\u043d\u043d\u0438\u0439 ID \u0437\u0430\u043a\u0430\u0437\u0430'
                },
                {
                    code: 'ViewPayments',
                    id: 11326,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u043f\u043b\u0430\u0442\u0435\u0436\u0435\u0439'
                },
                {
                    code: 'EDOViewer',
                    id: 60,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u0433\u0440\u0430\u0444\u0438\u043a\u0430 \u042d\u0414\u041e'
                },
                {
                    code: 'ViewInvoices',
                    id: 11105,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u0441\u0447\u0435\u0442\u043e\u0432'
                },
                {
                    code: 'consumes_list__consume_sum',
                    id: 1011,
                    name:
                        '\u0421\u043f\u0438\u0441\u043e\u043a \u0437\u0430\u044f\u0432\u043e\u043a - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 \u041f\u0435\u0440\u0432\u043e\u043d\u0430\u0447\u0430\u043b\u044c\u043d\u0430\u044f \u0441\u0443\u043c\u043c\u0430'
                },
                {
                    code: 'ViewPersons',
                    id: 11322,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u043f\u043b\u0430\u0442\u0435\u043b\u044c\u0449\u0438\u043a\u043e\u0432'
                },
                {
                    code: 'NewUIEarlyAdopter',
                    id: 64,
                    name:
                        '\u0412\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u044c \u0440\u0430\u0431\u043e\u0442\u044b \u0441 \u043d\u043e\u0432\u044b\u043c\u0438 \u0431\u043b\u043e\u043a\u0430\u043c\u0438 \u0438\u043d\u0442\u0435\u0440\u0444\u0435\u0439\u0441\u0430 \u0441\u0430\u0439\u0442\u0430'
                },
                {
                    code: 'ViewClients',
                    id: 11321,
                    name:
                        '\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u043a\u043b\u0438\u0435\u043d\u0442\u043e\u0432'
                },
                {
                    code: 'orders_report__client_id',
                    id: 1005,
                    name:
                        '\u041e\u0442\u0447\u0451\u0442 \u0417\u0430\u043a\u0430\u0437\u044b - \u044d\u043b\u0435\u043c\u0435\u043d\u0442 ID \u043a\u043b\u0438\u0435\u043d\u0442\u0430'
                }
            ]
        }
    }
};
