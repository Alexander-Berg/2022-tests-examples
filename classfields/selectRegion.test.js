const mockStore = require('autoru-frontend/mocks/mockStore').default;

jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(),
    };
});

const selectRegion = require('./selectRegion');

it('должен вернуть корректный набор actions, если addressNeeded и точность geoObject.kind !== house', () => {
    const store = mockStore({
        deliverySettings: {
            addressNeeded: true,
            regions: [ { coord: { latitude: 222, longitude: 222 } } ],
            offerID: '111-222',
        },
        sales: {
            items: [ { id: '111', hash: '222', category: 'cars' } ],
        },
    });

    return store.dispatch(selectRegion({ latitude: 111, longitude: 111, name: 'name', value: 'some address', kind: 'locality' })).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        () => {
            expect(store.getActions()).toEqual([ { type: 'SHOW_DELIVERY_SETTINGS_SUGGEST_ERROR', payload: 'Пожалуйста, уточните адрес' } ]);
        },
    );
});

it('должен вернуть корректный набор actions, если точка с новыми координатами уже есть в списке регионов', () => {
    const store = mockStore({
        deliverySettings: {
            regions: [ { coord: { latitude: 111, longitude: 222 } } ],
            offerID: '111-222',
        },
        sales: {
            items: [ { id: '111', hash: '222', category: 'cars' } ],
        },
    });

    return store.dispatch(selectRegion({ latitude: 111, longitude: 222, value: 'some address' })).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        () => {
            expect(store.getActions()).toEqual([ { type: 'SHOW_DELIVERY_SETTINGS_SUGGEST_ERROR', payload: 'Такой адрес уже добавлен' } ]);
        },
    );
});

it('должен вернуть корректный набор actions, если пользователь пытается добавить 11 регион', () => {
    const store = mockStore({
        deliverySettings: {
            regions: Array.from(Array(10).keys()),
            offerIDs: [ '111-222' ],
        },
        sales: {
            items: [
                { id: '111', hash: '222', category: 'cars' },
            ],
        },
    });

    return store.dispatch(selectRegion({ latitude: 111, longitude: 222, value: 'some address', kind: 'house' })).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        () => {
            expect(store.getActions()).toEqual(
                [
                    { type: 'SHOW_DELIVERY_SETTINGS_SUGGEST_ERROR', payload: 'Количество городов должно быть не больше 10' },
                ],
            );
        },
    );
});

it('должен вернуть корректный набор actions, если добавляемый город уже есть в списке регионов', () => {
    const store = mockStore({
        deliverySettings: {
            regions: [ { id: 'region_id' } ],
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
        products_prices_in_regions: [
            {
                product: 'some service',
                price: 100,
            },
        ],
        region_info: {
            id: 'region_id',
        },
    }));

    return store.dispatch(selectRegion({ latitude: 111, longitude: 222, value: 'some address', kind: 'house' }))
        .then(() => {
            expect(store.getActions()).toEqual(
                [
                    { type: 'SHOW_DELIVERY_SETTINGS_LOADER' },
                    { type: 'HIDE_DELIVERY_SETTINGS_LOADER' },
                    { type: 'SHOW_DELIVERY_SETTINGS_SUGGEST_ERROR', payload: 'Такой город уже есть в списке регионов' },
                ],
            );
        });
});

it('должен вернуть корректный набор actions, если такого региона нет в deliverySettings.regions', () => {
    const store = mockStore({
        deliverySettings: {
            regions: [],
            offerIDs: [ '111-222' ],
        },
        sales: {
            items: [ { id: '111', hash: '222', category: 'cars' } ],
        },
    });

    const getResource = require('auto-core/react/lib/gateApi').getResource;
    getResource.mockImplementation(() => Promise.resolve(
        {

            products_prices_in_regions: [
                {
                    product: 'some service',
                    price: 100,
                },
            ],
            region_info: {
                id: 'region_id',
                name: 'name',
            },
        },
    ));

    return store.dispatch(selectRegion({ latitude: 111, longitude: 111, value: 'some address', kind: 'house' }))
        .then(() => {
            expect(store.getActions()).toEqual(
                [
                    { type: 'SHOW_DELIVERY_SETTINGS_LOADER' },
                    { type: 'HIDE_DELIVERY_SETTINGS_LOADER' },
                    { type: 'HIDE_DELIVERY_SETTINGS_SUGGEST_ERROR' },
                    {
                        type: 'ADD_DELIVERY_SETTINGS_REGION',
                        payload: {
                            id: 'region_id',
                            deleted: false,
                            name: 'name',
                            address: 'some address',
                            coord: { latitude: 111, longitude: 111 },
                            offers: {
                                '111-222': [ {
                                    product: 'some service',
                                    price: 100,
                                } ],
                            },
                            regionInfo: {
                                id: 'region_id',
                                name: 'name',
                            },
                        },
                    },
                ],
            );
        });
});

it('должен вернуть корректный набор actions, если адрес доставки совпдает с регионом seller.location.region_info', () => {
    const store = mockStore({
        deliverySettings: {
            regions: [],
            offerIDs: [ 'id1-hash1' ],
        },
        sales: {
            items: [
                {
                    id: 'id1',
                    hash: 'hash1',
                    category: 'cars',
                    seller: {
                        location: {
                            region_info: {
                                id: 'seller_region_info_id',
                            },
                        },
                    },
                },
            ],
        },
    });

    const getResource = require('auto-core/react/lib/gateApi').getResource;
    getResource.mockImplementation(() => Promise.resolve(
        {
            products_prices_in_regions: [
                {
                    product: 'some service',
                    price: 100,
                },
            ],
            region_info: {
                id: 'seller_region_info_id',
                name: 'name',
            },
        },
    ));

    return store.dispatch(selectRegion({ latitude: 111, longitude: 111, value: 'some address', kind: 'house' }))
        .then(() => {
            expect(store.getActions()).toEqual(
                [
                    { type: 'SHOW_DELIVERY_SETTINGS_LOADER' },
                    { type: 'HIDE_DELIVERY_SETTINGS_LOADER' },
                    { type: 'SHOW_DELIVERY_SETTINGS_SUGGEST_ERROR', payload: 'Нельзя доставить объявление в выбранный город' },
                ],
            );
        });
});
