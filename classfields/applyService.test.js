/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */
jest.mock('www-cabinet/react/lib/listing/sendMetrika').default;
jest.mock('auto-core/react/lib/gateApi');

const mockStore = require('autoru-frontend/mocks/mockStore').default;

const applyService = require('./applyService');

const state = {
    config: {
        routeName: 'sale',
        customerRole: 'client',
    },
    sales: {
        items: [
            {
                id: '16064346',
                hash: 'fdba6bea',
                category: 'TRUCKS',
            },
            {
                id: '1091159434',
                hash: '42eaa0ad',
                category: 'CARS',
                section: 'new',
            },

        ],
    },
};

it('должен вызвать gateApi.getPage с корретными параметрами, ' +
    'dispatch корректный набор actions, ' +
    'и вернуть статус операции, ' +
    'если услуга отменяется', () => {
    const gateApi = require('auto-core/react/lib/gateApi');
    gateApi.getResource = jest.fn(() => Promise.resolve({
        response: {
            status: 'SUCCESS',
        },
    }));

    const store = mockStore(state);

    return store.dispatch(applyService({
        service: 'premium',
        offerID: '16064346-fdba6bea',
        isActivate: false,
        isGroupOperation: false,
    }))
        .then((result) => {
            expect(gateApi.getResource).toHaveBeenCalledWith('deleteProducts', {
                category: 'trucks',
                offerID: '16064346-fdba6bea',
                product: 'all_sale_premium',
            });

            expect(store.getActions()).toEqual([

                {
                    type: 'APPLY_SERVICE_PENDING',
                    payload: {
                        service: 'premium',
                        saleId: '16064346',
                    },
                },
                {
                    type: 'APPLY_SERVICE_RESOLVED',
                    payload: {
                        service: 'premium',
                        saleId: '16064346',
                        active: false,
                        isGroupOperation: false,
                    },
                },
                {
                    type: 'NOTIFIER_SHOW_MESSAGE',
                    payload: {
                        message: 'Услуга успешно отменена',
                        view: 'success',
                    },
                },
            ]);
            expect(result).toBe('SUCCESS');
        });
});

it('должен вызвать gateApi.getPage с корретными параметрами, ' +
    'dispatch корректный набор actions ' +
    'отправить событие в метрику ' +
    'и вернуть статус операции, ' +
    'если применятеся service = turbo', () => {
    const store = mockStore(state);

    const gateApi = require('auto-core/react/lib/gateApi');
    gateApi.getResource = jest.fn(() => Promise.resolve({
        response: {
            status: 'SUCCESS',
        },
    }));
    const sendMetrics = require('www-cabinet/react/lib/listing/sendMetrika').default;

    return store.dispatch(applyService({
        service: 'turbo',
        offerID: '1091159434-42eaa0ad',
        isActivate: true,
        isGroupOperation: false,
    }))
        .then((result) => {
            expect(gateApi.getResource).toHaveBeenCalledWith('applyProducts', {
                category: 'cars',
                offerID: '1091159434-42eaa0ad',
                products: [
                    { code: 'package_turbo' },
                ],
            });

            expect(store.getActions()).toEqual([

                {
                    type: 'APPLY_SERVICE_PENDING',
                    payload: {
                        service: 'turbo',
                        saleId: '1091159434',
                    },
                },

                {
                    type: 'APPLY_SERVICE_RESOLVED',
                    payload: {
                        service: 'turbo',
                        saleId: '1091159434',
                        active: true,
                        deactivationAllowed: false,
                        activatedBy: 'PACKAGE_TURBO',
                        isGroupOperation: false,
                    },
                },
                {
                    type: 'APPLY_SERVICE_RESOLVED',
                    payload: {
                        service: 'premium',
                        saleId: '1091159434',
                        active: true,
                        deactivationAllowed: false,
                        activatedBy: 'PACKAGE_TURBO',
                        isGroupOperation: false,
                    },
                },
                {
                    type: 'APPLY_SERVICE_RESOLVED',
                    payload: {
                        service: 'spec',
                        saleId: '1091159434',
                        active: true,
                        deactivationAllowed: false,
                        activatedBy: 'PACKAGE_TURBO',
                        isGroupOperation: false,
                    },
                },
                {
                    type: 'NOTIFIER_SHOW_MESSAGE',
                    payload: {
                        message: 'Услуга успешно применена',
                        view: 'success',
                    },
                },
            ]);
            expect(result).toBe('SUCCESS');
            expect(sendMetrics.mock.calls[0][1]).toEqual([ 'offer', 'vas', 'apply', 'turbo', 'button' ]);
        });
});

it('должен вызвать gateApi.getPage с корретными параметрами, ' +
    'dispatch корректный набор actions ' +
    'и вернуть статус операции, ' +
    'если сервер ответил ошибкой (Promise.reject)', () => {
    const store = mockStore(state);

    const gateApi = require('auto-core/react/lib/gateApi');
    gateApi.getResource = jest.fn(() => Promise.reject());
    const sendMetrics = require('www-cabinet/react/lib/listing/sendMetrika').default;

    return store.dispatch(applyService({
        service: 'spec',
        offerID: '1091159434-42eaa0ad',
        isActivate: true,
        isGroupOperation: false,
    }))
        .then((result) => {
            expect(gateApi.getResource).toHaveBeenCalledWith('applyProducts', {
                category: 'cars',
                offerID: '1091159434-42eaa0ad',
                products: [
                    { code: 'all_sale_special' },
                ],
            });

            expect(store.getActions()).toEqual([

                {
                    type: 'APPLY_SERVICE_PENDING',
                    payload: {
                        service: 'spec',
                        saleId: '1091159434',
                    },
                },

                {
                    type: 'APPLY_SERVICE_REJECTED',
                    payload: {
                        service: 'spec',
                        saleId: '1091159434',
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
            expect(sendMetrics.mock.calls[0][1]).toEqual([ 'offer', 'vas', 'error', 'spec', 'button' ]);
            expect(result).toBe('FAILED');
        });
});

it('должен отправить событие в метрику, ' +
    'если сервер ответил с ошибкой о недостатке денег на счете ' +
    'и !isGroupOperation', () => {
    const store = mockStore(state);

    const gateApi = require('auto-core/react/lib/gateApi');
    gateApi.getResource = jest.fn(() => Promise.reject('NOT_ENOUGH_FUNDS_ON_ACCOUNT'));
    const sendMetrics = require('www-cabinet/react/lib/listing/sendMetrika').default;

    return store.dispatch(applyService({
        service: 'spec',
        offerID: '1091159434-42eaa0ad',
        isActivate: true,
        isGroupOperation: false,
    }))
        .then((result) => {
            expect(gateApi.getResource).toHaveBeenCalledWith('applyProducts', {
                category: 'cars',
                offerID: '1091159434-42eaa0ad',
                products: [
                    { code: 'all_sale_special' },
                ],
            });

            expect(sendMetrics.mock.calls[0][1]).toEqual([ 'offer', 'vas', 'not_enough_money', 'spec', 'button' ]);
            expect(result).toBe('FAILED');
        });
});

it('не должен dispatch { type: SHOW_NOTIFICATION, ... }, ' +
    'если response.status = SUCCESS и isGroupOperation = true', () => {
    const store = mockStore(state);

    const gateApi = require('auto-core/react/lib/gateApi');
    gateApi.getResource = jest.fn(() => Promise.resolve({
        status: 'SUCCESS',
    }));

    return store.dispatch(applyService({
        service: 'spec',
        offerID: '1091159434-42eaa0ad',
        isActivate: true,
        isGroupOperation: true,
    }))
        .then(() => {
            expect(gateApi.getResource).toHaveBeenCalledWith('applyProducts', {
                category: 'cars',
                offerID: '1091159434-42eaa0ad',
                products: [
                    { code: 'all_sale_special' },
                ],
            });

            expect(store.getActions()[ 2 ]).toBeUndefined();
        });
});

it('не должен dispatch { type: SHOW_NOTIFICATION_ERROR, ... }, ' +
    'если сервер не ответил (Promise.reject) и isGroupOperation = true', () => {
    const store = mockStore(state);

    const gateApi = require('auto-core/react/lib/gateApi');
    gateApi.getResource = jest.fn(() => Promise.reject());

    return store.dispatch(applyService({
        service: 'spec',
        offerID: '1091159434-42eaa0ad',
        isActivate: true,
        isGroupOperation: true,
    }))
        .then(() => {
            expect(gateApi.getResource).toHaveBeenCalledWith('applyProducts', {
                category: 'cars',
                offerID: '1091159434-42eaa0ad',
                products: [
                    { code: 'all_sale_special' },
                ],
            });

            expect(store.getActions()[ 2 ]).toBeUndefined();
        });
});
