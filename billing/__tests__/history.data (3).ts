import { HOST } from '../../../../common/utils/test-utils/common';
import { camelCasePropNames } from '../../../../common/utils';

export const search =
    '?date_from=2013-10-28T00%3A00%3A00&date_to=2013-10-30T00%3A00%3A00&client_id=805242&person_id=1243320&manager_code=20645&date_type=1&commission=0&service_id=7&payment_type=3&contract_eid=24576%2F13&ps=10&pn=1&sf=dt&so=1';

export const filter = {
    contractEid: '24576/13',
    contractEidLike: false,
    contractType: '0',
    dtFrom: '2013-10-28T00:00:00',
    dtTo: '2013-10-30T00:00:00',
    dtType: 'DT',
    paymentType: 'POSTPAY',
    serviceId: '7'
};

export const contracts = {
    request: {
        data: {
            client_id: 805242,
            commission: '0',
            contract_eid: '24576/13',
            contract_eid_like: false,
            date_type: 'DT',
            dt_from: '2013-10-28T00:00:00',
            dt_to: '2013-10-30T00:00:00',
            manager_code: '20645',
            pagination_pn: 1,
            pagination_ps: 10,
            payment_type: 'POSTPAY',
            person_id: 1243320,
            service_id: '7',
            sort_key: 'DT',
            sort_order: 'ASC'
        },
        url: `${HOST}/contract/list`
    },
    response: camelCasePropNames({
        total_row_count: 1,
        items: [
            {
                client_id: 805242,
                is_suspended: '2015-12-11T00:00:00',
                manager_name:
                    '\u0427\u0438\u0441\u0442\u0438\u043a \u0412\u0430\u0440\u0432\u0430\u0440\u0430 \u0412\u0430\u0434\u0438\u043c\u043e\u0432\u043d\u0430',
                services: [
                    '\u0411\u0430\u044f\u043d',
                    '\u0414\u0438\u0440\u0435\u043a\u0442: \u0420\u0435\u043a\u043b\u0430\u043c\u043d\u044b\u0435 \u043a\u0430\u043c\u043f\u0430\u043d\u0438\u0438',
                    '\u041c\u0435\u0434\u0438\u0430\u0441\u0435\u043b\u043b\u0438\u043d\u0433',
                    '\u042f\u043d\u0434\u0435\u043a\u0441: \u0411\u0430\u043d\u043d\u0435\u0440\u043e\u043a\u0440\u0443\u0442\u0438\u043b\u043a\u0430'
                ],
                contract_id: 180298,
                client_name: '\u042e\u043b\u043c\u0430\u0440\u0442',
                manager_code: 20645,
                is_signed: '2013-11-22T00:00:00',
                commission: 0,
                person_id: 1243320,
                is_booked_dt: null,
                firm: 1,
                finish_dt: '2019-10-31T00:00:00',
                person_name: '\u042e\u043b\u043c\u0430\u0440\u0442',
                contract_eid: '24576/13',
                is_booked: false,
                agency_id: null,
                agency_name: null,
                is_faxed: '2013-11-15T00:00:00',
                sent_dt: '2013-11-22T00:00:00',
                is_cancelled: null,
                dt: '2013-10-29T00:00:00',
                payment_type: 3
            }
        ]
    })
};
