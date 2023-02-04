jest.mock('./resetRegions', () => (payload) => ({ type: 'RESET_DELIVERY_SETTINGS_REGIONS', payload }));

const show = require('./show');
const mockStore = require('autoru-frontend/mocks/mockStore').default;

it('должен вернуть корректный набор actions, если offerIDs.length === 1 и у оффера нет региона доставки', () => {
    const store = mockStore({
        sales: {
            items: [
                { id: 'id1', hash: 'hash1', category: 'cars' },
            ],
            delivery_info: {},
        },
    });

    store.dispatch(show({ offerIDs: [ 'id1-hash1' ] }));
    expect(store.getActions()).toEqual([
        { type: 'SHOW_DELIVERY_SETTINGS', payload: { offerIDs: [ 'id1-hash1' ], addressNeeded: true } },
    ]);
});

it('должен вернуть корректный набор actions, если offerIDs.length === 1 и у оффера есть регион доставки', () => {
    const store = mockStore({
        sales: {
            items: [
                {
                    id: 'id1',
                    hash: 'hash1',
                    category: 'trucks',
                    delivery_info: {
                        delivery_regions: [
                            {
                                location: {
                                    address: 'some address',
                                    coord: 'some coords',
                                    region_info: {
                                        id: 'region_info_id',
                                        name: 'some name',
                                    },
                                },
                                products: [
                                    { product: 'product1', price: 200 },
                                    { product: 'product2', price: 300 },
                                ],
                            },
                        ],
                    },
                },
            ],
        },
    });

    store.dispatch(show({ offerIDs: [ 'id1-hash1' ] }));
    expect(store.getActions()).toEqual(
        [ { type: 'SHOW_DELIVERY_SETTINGS', payload: { offerIDs: [ 'id1-hash1' ], addressNeeded: false } },
            {
                type: 'RESET_DELIVERY_SETTINGS_REGIONS', payload: [ {
                    id: 'region_info_id',
                    name: 'some name',
                    coord: 'some coords',
                    address: 'some address',
                    deleted: false,
                    offers: {
                        'id1-hash1': [
                            { product: 'product1', price: 200 },
                            { product: 'product2', price: 300 },
                        ],
                    },
                    regionInfo: {
                        id: 'region_info_id',
                        name: 'some name',
                    },
                } ],
            },
        ],
    );
});

it('должен вернуть корректный набор actions, если offerIDs.length > 1 и у офферов совпадают регионы доставки', () => {
    const store = mockStore({
        sales: {
            items: [
                {
                    id: 'id1',
                    hash: 'hash1',
                    category: 'trucks',
                    delivery_info: {
                        delivery_regions: [
                            {
                                location: {
                                    address: 'some address',
                                    coord: 'some coords',
                                    region_info: {
                                        id: 'region_info_id',
                                        name: 'some name',
                                    },
                                },
                                products: 'products 1',
                            },
                        ],
                    },
                },
                {
                    id: 'id2',
                    hash: 'hash2',
                    category: 'trucks',
                    delivery_info: {
                        delivery_regions: [
                            {
                                location: {
                                    address: 'some address',
                                    coord: 'some coords',
                                    region_info: {
                                        id: 'region_info_id',
                                        name: 'some name',
                                    },
                                },
                                products: 'products 2',
                            },
                        ],
                    },
                },
            ],
        },
    });

    store.dispatch(show({ offerIDs: [ 'id1-hash1', 'id2-hash2' ] }));
    expect(store.getActions()).toEqual([
        { type: 'SHOW_DELIVERY_SETTINGS', payload: { offerIDs: [ 'id1-hash1', 'id2-hash2' ], addressNeeded: false } },
        {
            type: 'RESET_DELIVERY_SETTINGS_REGIONS', payload: [ {
                id: 'region_info_id',
                name: 'some name',
                coord: 'some coords',
                address: 'some address',
                deleted: false,
                offers: {
                    'id1-hash1': 'products 1',
                    'id2-hash2': 'products 2',
                },
                regionInfo: {
                    name: 'some name',
                    id: 'region_info_id',
                },
            } ],
        },
    ]);
});

it('должен вернуть корректный набор actions, если offerIDs.length > 1 и у офферов не совпадают регионы доставки', () => {
    const store = mockStore({
        sales: {
            items: [
                {
                    id: 'id1',
                    hash: 'hash1',
                    category: 'trucks',
                    delivery_info: {
                        delivery_regions: [
                            {
                                location: {
                                    address: 'some address 1',
                                    coords: 'some coords 1',
                                    region_info: {
                                        id: 'region_info_id1',
                                        name: 'some name 1',
                                    },
                                },
                                products: 'products 1',
                            },
                        ],
                    },
                },
                {
                    id: 'id2',
                    hash: 'hash2',
                    category: 'trucks',
                    delivery_info: {
                        delivery_regions: [
                            {
                                location: {
                                    address: 'some address 2',
                                    coords: 'some coords 2',
                                    region_info: {
                                        id: 'region_info_id2',
                                        name: 'some name 2',
                                    },
                                },
                                products: 'products 2',
                            },
                            {
                                location: {
                                    address: 'some address 3',
                                    coords: 'some coords 3',
                                    region_info: {
                                        id: 'region_info_id1',
                                        name: 'some name 3',
                                    },
                                },
                                products: 'products 3',
                            },
                        ],
                    },
                },
            ],
        },
    });

    store.dispatch(show({ offerIDs: [ 'id1-hash1', 'id2-hash2' ] }));
    expect(store.getActions()).toEqual([
        { type: 'SHOW_DELIVERY_SETTINGS', payload: { offerIDs: [ 'id1-hash1', 'id2-hash2' ], addressNeeded: false } },
    ]);
});
