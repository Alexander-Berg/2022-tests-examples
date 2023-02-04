/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/lib/gateApi');
jest.mock('www-cabinet/react/lib/listing/sendMetrika').default;

const changeStatus = require('./changeStatus');

const mockStore = require('autoru-frontend/mocks/mockStore').default;

const state = {
    config: {
        routeName: 'sale',
        customerRole: 'manager',
        client: {
            id: 'client_id',
        },
    },
    sales: {
        items: [
            {
                id: '3230706',
                hash: '438c4bd5',
                category: 'moto',
                actions: {
                    activate: true,
                    archive: true,
                    edit: true,
                    hide: true,
                },
            },
        ],
    },
};

it(
    'должен вызвать gateApi.getResource с корретными параметрами, ' +
    'dispatch корректный набор actions ' +
    'вызвать sendMetrika с корретными параметрами, ' +
    'и вернуть корректный статус, ' +
    'если объявление снимается с продажи', () => {
        const gateApi = require('auto-core/react/lib/gateApi');
        gateApi.getResource = jest.fn(() => Promise.resolve({ status: 'SUCCESS' }));
        const sendMetrika = require('www-cabinet/react/lib/listing/sendMetrika').default;

        const store = mockStore(state);

        return store.dispatch(changeStatus({
            status: 'inactive',
            offerID: '3230706-438c4bd5',
            isGroupOperation: false,
        }))
            .then((data) => {
                expect(gateApi.getResource).toHaveBeenCalledWith('offerHide', {
                    category: 'moto',
                    offerID: '3230706-438c4bd5',
                    dealer_id: 'client_id',
                });

                expect(store.getActions()).toEqual([
                    {
                        type: 'CHANGE_SALE_STATUS',
                        payload: {
                            saleId: '3230706',
                            status: 'inactive',
                            actions: {
                                edit: true,
                                activate: true,
                                hide: false,
                                archive: true,
                            },
                            multiposting: {
                                actions: {
                                    activate: true,
                                    archive: true,
                                    edit: true,
                                    hide: false,
                                },
                                status: 'inactive',
                            },
                        },
                    },
                    {
                        type: 'NOTIFIER_SHOW_MESSAGE',
                        payload: {
                            message: 'Объявление снято с продажи',
                            view: 'success',

                        },
                    },
                ]);
                expect(sendMetrika.mock.calls[0][1]).toEqual([ 'offer', 'status', 'deactivate_offer' ]);
                expect(data).toBe('SUCCESS');
            });
    });

it(
    'должен вызвать gateApi.getResource с корретными параметрами, ' +
    'dispatch корректный набор actions ' +
    'вызвать sendMetrika с корретными параметрами, ' +
    'и вернуть корректный статус, ' +
    'если объявление активируется', () => {
        const gateApi = require('auto-core/react/lib/gateApi');
        gateApi.getResource = jest.fn(() => Promise.resolve({
            status: 'SUCCESS',
        }));
        const sendMetrika = require('www-cabinet/react/lib/listing/sendMetrika').default;

        const store = mockStore(state);

        return store.dispatch(changeStatus({
            status: 'active',
            offerID: '3230706-438c4bd5',
            isGroupOperation: false,
        }))
            .then((data) => {
                expect(gateApi.getResource).toHaveBeenCalledWith('offerActivate', {
                    category: 'moto',
                    offerID: '3230706-438c4bd5',
                    dealer_id: 'client_id',
                });

                expect(store.getActions()).toEqual([

                    {
                        type: 'CHANGE_SALE_STATUS',
                        payload: {
                            saleId: '3230706',
                            status: 'active',
                            actions: {
                                edit: true,
                                activate: false,
                                hide: true,
                                archive: true,
                            },
                            multiposting: {
                                actions: {
                                    activate: false,
                                    archive: true,
                                    edit: true,
                                    hide: true,
                                },
                                status: 'active',
                            },
                        },
                    },

                    {
                        type: 'NOTIFIER_SHOW_MESSAGE',
                        payload: {
                            message: 'Объявление активировано',
                            view: 'success',

                        },
                    },
                ]);
                expect(sendMetrika.mock.calls[0][1]).toEqual([ 'offer', 'status', 'activate_offer' ]);
                expect(data).toBe('SUCCESS');
            });
    });

it(
    'должен вызвать gateApi.getResource с корретными параметрами, ' +
    'dispatch корректный набор actions ' +
    'вызвать sendMetrika с корретными параметрами, ' +
    'и вернуть корректный статус, ' +
    'если объявление удаляется', () => {
        const gateApi = require('auto-core/react/lib/gateApi');
        gateApi.getResource = jest.fn(() => Promise.resolve({
            status: 'SUCCESS',
        }));
        const sendMetrika = require('www-cabinet/react/lib/listing/sendMetrika').default;

        const store = mockStore(state);

        return store.dispatch(changeStatus({
            status: 'removed',
            offerID: '3230706-438c4bd5',
            isGroupOperation: false,
        }))
            .then((data) => {
                expect(gateApi.getResource).toHaveBeenCalledWith('offerDelete', {
                    category: 'moto',
                    offerID: '3230706-438c4bd5',
                    dealer_id: 'client_id',
                });

                expect(store.getActions()).toEqual([

                    {
                        type: 'CHANGE_SALE_STATUS',
                        payload: {
                            saleId: '3230706',
                            status: 'removed',
                            actions: {
                                edit: false,
                                activate: false,
                                hide: false,
                                archive: false,
                            },
                            multiposting: {
                                actions: {
                                    activate: false,
                                    archive: false,
                                    edit: false,
                                    hide: false,
                                },
                                status: 'removed',
                            },
                        },
                    },

                    {
                        type: 'NOTIFIER_SHOW_MESSAGE',
                        payload: {
                            message: 'Объявление удалено',
                            view: 'success',
                        },
                    },
                ]);

                expect(sendMetrika.mock.calls[0][1]).toEqual([ 'offer', 'status', 'delete_offer' ]);
                expect(data).toBe('SUCCESS');
            });
    });

it('должен вызвать gateApi.getResource, ' +
    'dispatch корректный набор actions и вернуть корректный статус ' +
    'если gateApi.getPage вернул Promise.reject', () => {
    const gateApi = require('auto-core/react/lib/gateApi');
    gateApi.getResource = jest.fn(() => Promise.reject());

    const store = mockStore(state);

    return store.dispatch(changeStatus({
        status: 'removed',
        offerID: '3230706-438c4bd5',
        isGroupOperation: false,
    }))
        .then((data) => {
            expect(gateApi.getResource).toHaveBeenCalled();

            expect(store.getActions()).toEqual([
                {
                    type: 'NOTIFIER_SHOW_MESSAGE',
                    payload: {
                        message: 'Произошла ошибка, попробуйте ещё раз',
                        view: 'error',
                    },
                },
            ]);

            expect(data).toBe('FAILED');
        });
});

it('не должен dispatch { type: SHOW_NOTIFICATION,... } ' +
    'если gateApi.getResource вернул SUCCESS и isGroupOperation', () => {
    const gateApi = require('auto-core/react/lib/gateApi');
    gateApi.getResource = jest.fn(() => Promise.resolve({ status: 'SUCCESS' }));

    const store = mockStore(state);

    return store.dispatch(changeStatus({
        status: 'removed',
        offerID: '3230706-438c4bd5',
        isGroupOperation: true,
    }))
        .then(() => {
            expect(store.getActions()[1]).toBeUndefined();
        });
});

it('не должен dispatch { type: SHOW_NOTIFICATION_ERROR,... } ' +
    'если gateApi.getResource вернул Promise.reject', () => {
    const gateApi = require('auto-core/react/lib/gateApi');
    gateApi.getResource = jest.fn(() => Promise.reject());

    const store = mockStore(state);

    return store.dispatch(changeStatus({
        status: 'removed',
        offerID: '3230706-438c4bd5',
        isGroupOperation: true,
    }))
        .then(() => {
            expect(store.getActions()[0]).toBeUndefined();
        });
});
