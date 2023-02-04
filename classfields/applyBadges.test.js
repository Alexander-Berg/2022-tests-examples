/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */
jest.mock('www-cabinet/react/lib/listing/sendMetrika').default;
jest.mock('auto-core/react/lib/gateApi');
global.metrika = {
    reachGoal: jest.fn(),
};

const mockStore = require('autoru-frontend/mocks/mockStore').default;

const applyBadges = require('./applyBadges');

const state = {
    config: {
        routeName: 'sale',
        customerRole: 'manager',
        client: {
            id: '20101',
        },
    },
    sales: {
        items: [
            {
                id: '16064346',
                hash: 'fdba6bea',
                category: 'TRUCKS',
            },
        ],
    },
};

it('должен вызвать gateApi.getPage с корретными параметрами, ' +
    'dispatch корректный набор actions, ' +
    'вызвать sendMetrics с корретными параметрами, ' +
    'если услуга успешно применена (.response.status === SUCCESS)', () => {
    const gateApi = require('auto-core/react/lib/gateApi');
    gateApi.getResource = jest.fn(() => Promise.resolve({
        response: {
            status: 'SUCCESS',
        },
    }));
    const sendMetrics = require('www-cabinet/react/lib/listing/sendMetrika').default;

    const store = mockStore(state);

    return store.dispatch(
        applyBadges({
            offerID: '16064346-fdba6bea',
            badges: [ 'Без пробега по РФ' ],
        }),
    )
        .then(() => {
            expect(gateApi.getResource).toHaveBeenCalledWith('applyProducts', {
                category: 'trucks',
                offerID: '16064346-fdba6bea',
                dealer_id: '20101',
                products: [
                    { code: 'all_sale_badge', badges: [ 'Без пробега по РФ' ] },
                ],
            });

            expect(store.getActions()).toEqual([

                {
                    type: 'HIDE_BADGES_SETTINGS',
                },
                {

                    type: 'APPLY_SERVICE_PENDING',
                    payload: {
                        service: 'badge',
                        saleId: '16064346',
                    },
                },
                {
                    type: 'UPDATE_BADGES',
                    payload: {
                        service: 'badge',
                        saleId: '16064346',
                        badges: [
                            'Без пробега по РФ',
                        ],
                    },
                },
                {
                    type: 'NOTIFIER_SHOW_MESSAGE',
                    payload: {
                        message: 'Услуга применена',
                        view: 'success',
                    },
                },
            ]);
            expect(sendMetrics.mock.calls[0][1]).toEqual([ 'offer', 'vas', 'badge', 'apply' ]);
        });
});

it('должен вызвать gateApi.getPage с корретными параметрами, ' +
    'dispatch корректный набор actions, ' +
    'если услуга успешно применена (.response.status === SUCCESS) и badges = []', () => {

    const gateApi = require('auto-core/react/lib/gateApi');
    gateApi.getResource = jest.fn(() => Promise.resolve({
        response: {
            status: 'SUCCESS',
        },
    }));

    const store = mockStore(state);

    return store.dispatch(applyBadges({
        offerID: '16064346-fdba6bea',
        badges: [],
    }))
        .then(() => {
            expect(gateApi.getResource).toHaveBeenCalledWith('deleteProducts', {
                category: 'trucks',
                offerID: '16064346-fdba6bea',
                dealer_id: '20101',
                product: 'all_sale_badge',
            });

            expect(store.getActions()).toEqual([

                {
                    type: 'HIDE_BADGES_SETTINGS',
                },
                {
                    type: 'APPLY_SERVICE_PENDING',
                    payload: {
                        service: 'badge',
                        saleId: '16064346',
                    },
                },
                {
                    type: 'UPDATE_BADGES',
                    payload: {
                        service: 'badge',
                        saleId: '16064346',
                        badges: [],
                    },
                },
                {
                    type: 'NOTIFIER_SHOW_MESSAGE',
                    payload: {
                        message: 'Услуга отменена',
                        view: 'success',
                    },
                },
            ]);
        });
});

it('должен вызвать gateApi.getPage с корретными параметрами, ' +
    'dispatch корректный набор actions, ' +
    'вызвать sendMetrics с корретными параметрами, ' +
    'если сервер ответил ошибкой', () => {
    const gateApi = require('auto-core/react/lib/gateApi');
    gateApi.getResource = jest.fn(() => Promise.reject());
    const sendMetrics = require('www-cabinet/react/lib/listing/sendMetrika').default;

    const store = mockStore(state);

    return store.dispatch(applyBadges({
        offerID: '16064346-fdba6bea',
        badges: [ 'Без пробега по РФ' ],
    }))
        .then(() => {
            expect(gateApi.getResource).toHaveBeenCalledWith('applyProducts', {
                category: 'trucks',
                offerID: '16064346-fdba6bea',
                dealer_id: '20101',
                products: [
                    { code: 'all_sale_badge', badges: [ 'Без пробега по РФ' ] },
                ],
            });

            expect(store.getActions()).toEqual([

                {
                    type: 'HIDE_BADGES_SETTINGS',
                },
                {
                    type: 'APPLY_SERVICE_REJECTED',
                    payload: {
                        service: 'badge',
                        saleId: '16064346',
                    },
                },
                {
                    type: 'NOTIFIER_SHOW_MESSAGE',
                    payload: {
                        message: 'Произошла ошибка, попробуйте ещё раз',
                        view: 'error',
                    },
                },
            ]);
            expect(sendMetrics.mock.calls[0][1]).toEqual([ 'offer', 'vas', 'badge', 'error' ]);
        });
});

it('должен вызвать sendMetrics с корретными параметрами, ' +
    'вызвать sendMetrics с корретными параметрами, ' +
    'если сервер ответил ошибкой о недостатке денег', () => {
    const gateApi = require('auto-core/react/lib/gateApi');
    gateApi.getResource = jest.fn(() => Promise.reject('NOT_ENOUGH_FUNDS_ON_ACCOUNT'));
    const sendMetrics = require('www-cabinet/react/lib/listing/sendMetrika').default;

    const store = mockStore(state);

    return store.dispatch(applyBadges({
        offerID: '16064346-fdba6bea',
        badges: [ 'Без пробега по РФ' ],
    }))
        .then(() => {
            expect(sendMetrics.mock.calls[0][1]).toEqual([ 'offer', 'vas', 'badge', 'not_enough_money' ]);
        });
});
