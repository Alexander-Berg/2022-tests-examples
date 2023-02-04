const mockStore = require('autoru-frontend/mocks/mockStore').default;

jest.mock('www-cabinet/react/dataDomain/sales/actions/updateDelivery', () => () => ({ type: 'UPDATE_SALE_DELIVERY' }));
jest.mock('www-cabinet/react/dataDomain/notifier/actions', () => ({
    showErrorNotification: () => ({ type: 'SHOW_ERROR_NOTIFICATION' }),
    showInfoNotification: () => ({ type: 'SHOW_INFO_NOTIFICATION' }),
}));
jest.mock('auto-core/react/lib/gateApi', () => ({ getResource: jest.fn() }));
jest.mock('./hide', () => () => ({ type: 'HIDE_DELIVERY_SETTINGS' }));

const saveDelivery = require('./saveDelivery');

it('должен вернуть корректный массив actions, если Promise.resolve', () => {
    const store = mockStore({
        deliverySettings: {
            regions: [
                { coord: 'some coord1', address: 'address1', deleted: true },
                { coord: 'some coord2', address: 'address2', deleted: false },
            ],
            offerIDs: [ '111-222' ],
        },
        sales: {
            items: [
                { id: '111', hash: '222', category: 'cars' },
            ],
        },
    });

    const getResource = require('auto-core/react/lib/gateApi').getResource;
    getResource.mockImplementation(() => Promise.resolve());

    return store.dispatch(saveDelivery({ dealerId: undefined })).then(() => {
        expect(store.getActions()).toEqual([
            { type: 'SHOW_DELIVERY_SETTINGS_LOADER' },
            { type: 'UPDATE_SALE_DELIVERY' },
            { type: 'HIDE_DELIVERY_SETTINGS_LOADER' },
            { type: 'SHOW_INFO_NOTIFICATION' },
            { type: 'HIDE_DELIVERY_SETTINGS' },
        ]);
    });
});

it('должен вернуть корректный массив actions, если Promise.reject', () => {
    const store = mockStore({
        deliverySettings: {
            regions: [
                { coord: 'some coord1', address: 'address1', deleted: true },
                { coord: 'some coord2', address: 'address2', deleted: false },
            ],
            offerIDs: [ '111-222' ],
        },
        sales: {
            items: [
                { id: '111', hash: '222', category: 'cars' },
            ],
        },
    });

    const getResource = require('auto-core/react/lib/gateApi').getResource;
    getResource.mockImplementation(() => Promise.reject());

    return store.dispatch(saveDelivery({ dealerId: undefined })).then(() => {
        expect(store.getActions()).toEqual([
            { type: 'SHOW_DELIVERY_SETTINGS_LOADER' },
            { type: 'HIDE_DELIVERY_SETTINGS_LOADER' },
            { type: 'SHOW_ERROR_NOTIFICATION' },
        ]);
    });
});
