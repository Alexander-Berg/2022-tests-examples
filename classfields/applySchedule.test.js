/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(),
    };
});

const MockDate = require('mockdate');
const _ = require('lodash');

const gateApi = require('auto-core/react/lib/gateApi');
const mockStore = require('autoru-frontend/mocks/mockStore').default;

const applySchedule = require('./applySchedule');

MockDate.set('2019-08-03', -180);

const state = {
    config: {
        routeName: 'sale',
        customerRole: 'client',
    },
    sales: {
        items: [
            {
                id: '1091189848',
                hash: '21914653',
                category: 'cars',
            },
        ],
    },
};

it('должен вызвать gateApi.getResource с корретными параметрами и вернуть корректный набор actions, ' +
    'если .response.status === SUCCESS', () => {
    const store = mockStore(state);

    gateApi.getResource.mockImplementation(() => Promise.resolve({
        response: {
            status: 'SUCCESS',
        },
    }));

    return store.dispatch(applySchedule({
        offerID: '1091189848-21914653',
        time: '00:00',
        weekdays: [ 1, 3 ],
    }))
        .then(() => {
            expect(gateApi.getResource).toHaveBeenCalledWith('putBillingSchedules', {
                offerId: '1091189848-21914653',
                category: 'cars',
                timezone: '+03:00',
                product: 'all_sale_fresh',
                schedule_type: 'ONCE_AT_TIME',
                time: '00:00',
                weekdays: [ 1, 3 ],
            });

            expect(store.getActions()).toEqual([

                {
                    type: 'APPLIED_SERVICE_SCHEDULE',
                    payload: {
                        saleId: '1091189848',
                        params: {
                            time: '00:00',
                            weekdays: [ 1, 3 ],
                        },
                        service: 'all_sale_fresh',
                        schedule_type: 'ONCE_AT_TIME',
                    },
                },

                {
                    type: 'NOTIFIER_SHOW_MESSAGE',
                    payload: {
                        message: 'Автоприменение услуги «Поднятие в поиске» включено',
                        view: 'success',
                    },
                },
            ]);
        });
});

it('должен делать роллбэк к предыдущему состоянию, если бэкенд ответил ошибкой при кейсе, когда не было автоприменений', () => {
    const store = mockStore(state);

    gateApi.getResource.mockImplementation(() => Promise.reject());

    return store.dispatch(applySchedule({
        offerID: '1091189848-21914653',
        time: '00:00',
        weekdays: [ 1, 3 ],
    }))
        .then(() => {
            expect(store.getActions()[0]).toEqual(
                {
                    type: 'RESETTED_SERVICE_SCHEDULE',
                    payload: {
                        saleId: '1091189848',
                        service: 'all_sale_fresh',
                    },
                },
            );
        });
});

it('должен делать роллбэк к предыдущему состоянию, если бэкенд ответил ошибкой при кейсе, когда было подключено автоприменение', () => {
    const stateClone = _.cloneDeep(state);

    stateClone.sales.items[0].service_schedules = {
        products: {
            all_sale_fresh: {
                schedule_type: 'ONCE_AT_TIME',
                once_at_time: {
                    time: '14:00',
                    weekdays: [ 3 ],
                },
            },
        },
    };

    const store = mockStore(stateClone);

    gateApi.getResource.mockImplementation(() => Promise.reject());

    return store.dispatch(applySchedule({
        offerID: '1091189848-21914653',
        time: '00:00',
        weekdays: [ 1, 3 ],
    }))
        .then(() => {
            expect(store.getActions()[0]).toEqual(
                {
                    type: 'APPLIED_SERVICE_SCHEDULE',
                    payload: {
                        saleId: '1091189848',
                        service: 'all_sale_fresh',
                        schedule_type: 'ONCE_AT_TIME',
                        params: {
                            time: '14:00',
                            weekdays: [ 3 ],
                        },
                    },
                },
            );
        });
});
