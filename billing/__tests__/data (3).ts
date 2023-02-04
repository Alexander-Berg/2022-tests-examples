import { Permissions } from 'common/constants';
import { convertDateToMonth } from '../utils';

export const perms = Object.values(Permissions);

export const months = [convertDateToMonth('2019-05-01'), convertDateToMonth('2019-07-01')];

export const initialResponce = {
    data: [
        { id: 1, name: 'service_1', has_aggregation: true },
        { id: 2, name: 'service_2', has_aggregation: true },
        { id: 3, name: 'service_3', has_aggregation: false },
        { id: 4, name: 'service_4', has_aggregation: true },
        { id: 5, name: 'service_5', has_aggregation: false },
        { id: 6, name: 'service_6', has_aggregation: false }
    ]
};

export const serviceMap = {
    1: { id: 1, name: 'service_1', hasAggregation: true },
    2: { id: 2, name: 'service_2', hasAggregation: true },
    3: { id: 3, name: 'service_3', hasAggregation: false },
    4: { id: 4, name: 'service_4', hasAggregation: true },
    5: { id: 5, name: 'service_5', hasAggregation: false },
    6: { id: 6, name: 'service_6', hasAggregation: false }
};

export const serviceItems = [
    { value: 1, content: 'service_1' },
    { value: 2, content: 'service_2' },
    { value: 3, content: 'service_3' },
    { value: 4, content: 'service_4' },
    { value: 5, content: 'service_5' },
    { value: 6, content: 'service_6' }
];

export const nonBlockedStatuses = {
    data: [
        {
            enqueue_dt: '2019-07-02T02:01:11',
            export_dt: '2019-06-28T18:07:19',
            name: 'taxi_aggr_tlog',
            queue: 'PARTNER_COMPL',
            services: [1, 2, 4, 5],
            states: { error: 0, running: 0, success: 30 }
        }
    ]
};

export const blockedStatuses = {
    data: [
        {
            enqueue_dt: '2019-07-02T02:01:11',
            export_dt: '2019-06-28T18:07:19',
            name: 'taxi_aggr_tlog',
            queue: 'PARTNER_COMPL',
            services: [1, 2, 4, 5],
            states: { error: 0, running: 0, success: 30 }
        },
        {
            enqueue_dt: '2019-12-24T13:45:37',
            export_dt: '2019-06-28T15:28:49',
            name: 'taxi',
            queue: 'STAT_AGGREGATOR',
            services: null,
            states: { error: 60, running: 0, success: 0 }
        }
    ]
};
