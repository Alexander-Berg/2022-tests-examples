const mockStore = require('autoru-frontend/mocks/mockStore').default;

jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(),
    };
});

jest.mock('www-cabinet/react/dataDomain/sales/actions/resetDelivery', () => () => ({ type: 'RESET_SALE_DELIVERY' }));
jest.mock('www-cabinet/react/dataDomain/notifier/actions', () => ({
    showErrorNotification: () => ({ type: 'SHOW_ERROR_NOTIFICATION' }),
    showInfoNotification: () => ({ type: 'SHOW_INFO_NOTIFICATION' }),
}));
jest.mock('auto-core/react/lib/gateApi', () => ({ getResource: jest.fn() }));
jest.mock('./hide', () => () => ({ type: 'HIDE_DELIVERY_SETTINGS' }));

const deleteDelivery = require('./deleteDelivery');

it('должен вернуть корректный набор actions, если Promise.resolve', () => {
    const store = mockStore({
        deliverySettings: {
            regions: [],
            offerIDs: [ '111-222' ],
        },
        sales: {
            items: [
                { id: '111', hash: '222', category: 'cars' },
            ],
        },
    });
    const getResource = require('auto-core/react/lib/gateApi').getResource;
    getResource.mockImplementation(() => Promise.resolve({
        result: {
            products_prices_in_regions: [
                {
                    region_id: 'some region_id',
                    product: 'some service',
                    price: 100,
                },
            ],
        },
    }));

    return store.dispatch(deleteDelivery({ dealerId: 20101 }))
        .then(() => {
            expect(store.getActions()).toEqual([
                { type: 'SHOW_DELIVERY_SETTINGS_LOADER' },
                { type: 'RESET_SALE_DELIVERY' },
                { type: 'HIDE_DELIVERY_SETTINGS_LOADER' },
                { type: 'SHOW_INFO_NOTIFICATION' },
                { type: 'HIDE_DELIVERY_SETTINGS' },
            ]);
        });
});

it('должен вернуть корректный набор actions, если Promise.reject', () => {
    const store = mockStore({
        deliverySettings: {
            regions: [],
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

    return store.dispatch(deleteDelivery({ dealerId: 20101 }))
        .then(() => {
            expect(store.getActions()).toEqual([
                { type: 'SHOW_DELIVERY_SETTINGS_LOADER' },
                { type: 'HIDE_DELIVERY_SETTINGS_LOADER' },
                { type: 'SHOW_ERROR_NOTIFICATION' },
            ]);
        });
});
