jest.mock('auto-core/react/lib/gateApi');
const updateDelivery = require('./updateDelivery');
const deliveryRegion = require('../mock/deliveryRegion');

it('должен вызвать корректный набор actions', () => {
    const dispatch = jest.fn();

    updateDelivery('1091257820', '1091257820-0e660c35', [ deliveryRegion ], {})(dispatch);
    expect(dispatch.mock.calls).toEqual([
        [ {
            type: 'UPDATE_DELIVERY',
            payload: {
                saleId: '1091257820',
                deliveryRegions: [ {
                    location: {
                        federal_subject_id: '10716',
                        address: 'Россия, Московская область, Балашиха, Первомайская улица, 1',
                        coord: {
                            latitude: 55.79190826,
                            longitude: 37.93818665,
                        },
                        region_info: {
                            id: '10716',
                            name: 'Балашиха',
                            genitive: 'Балашихи',
                            dative: 'Балашихе',
                            accusative: 'Балашиху',
                            prepositional: 'Балашихе',
                            preposition: 'в',
                            latitude: 55.796339,
                            longitude: 37.938199,
                            parent_ids: [
                                '10716',
                                '116705',
                                '1',
                                '3',
                                '225',
                                '10001',
                                '10000',
                            ],
                        },
                    },
                    products: [
                        {
                            product: 'placement',
                            price: 10,
                            region_id: 10716,
                            product_old_name: 'all_sale_activate',
                        },
                        {
                            product: 'boost',
                            price: 300,
                            region_id: 10716,
                            product_old_name: 'all_sale_fresh',
                        },
                    ],
                } ],
                productPrices: {},
            },
        } ],
    ]);
});
