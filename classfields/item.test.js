jest.mock('auto-core/lib/core/isMobileApp');
const _ = require('lodash');

const prepareSubscriptionItem = require('./item');

const item = {
    id: '123',
    title: 'Все марки автомобилей',
    category: 'CARS',
    params: {
        cars_params: { body_type_group: [], seats_group: 'ANY_SEATS', engine_group: [] },
        currency: 'RUR',
        has_image: true,
        in_stock: 'ANY_STOCK',
        state_group: 'ALL',
        exchange_group: 'NO_EXCHANGE',
        seller_group: [ 'ANY_SELLER' ],
        damage_group: 'NOT_BEATEN',
        owners_count_group: 'ANY_COUNT',
        owning_time_group: 'ANY_TIME',
        customs_state_group: 'CLEARED',
    },
};

const trucksItem = {
    params: {
        grouping_id: '',
        trucks_params: {
            trucks_category: 'LCV',
            light_truck_type: [
                'MINIBUS',
            ],
        },
    },
    view: {
        applied_filter_count: 1,
    },
    relevance: 1,
};

const isMobileApp = require('auto-core/lib/core/isMobileApp');

describe('desktop', () => {
    beforeEach(() => {
        isMobileApp.mockImplementation(() => false);
    });

    it('должен правильно подготовить данные о подписке', () => {
        const preparedItem = prepareSubscriptionItem(item, { config: {}, req: {} }, {});
        expect(preparedItem).toEqual({
            category: 'CARS',
            id: '123',
            params: {
                rid: '',
                section: 'all',
            },
            paramsDescription: {
                moreCount: 0,
                paramsInfo: [],
            },
            title: 'Все марки автомобилей',
            url: 'https://autoru_frontend.base_domain/cars/all/?geo_id=',
        });
    });

    it('должен правильно обработать саджест категории trucks', () => {
        const preparedItem = prepareSubscriptionItem(trucksItem, { config: {}, req: {} }, {});
        expect(preparedItem).toEqual(
            {
                params: {
                    light_truck_type: [
                        'MINIBUS',
                    ],
                    rid: '',
                    trucks_category: 'LCV',
                },
                paramsDescription: {
                    moreCount: 0,
                    paramsInfo: [
                        {
                            val: 'С пробегом',
                            key: 'section',
                        },
                        {
                            label: 'Тип кузова',
                            val: 'MINIBUS',
                            key: 'light_truck_type',
                        },
                    ],
                },
                relevance: 1,
                url: 'https://autoru_frontend.base_domain/lcv/all/?light_truck_type=MINIBUS&geo_id=',
                view: {
                    applied_filter_count: 1,
                },
            },
        );
    });

    it('должен формирует урл у подписки на листинг дилера', () => {
        const modifiedItem = _.cloneDeep(item);
        modifiedItem.params.dealer_id = 'dealer-id';
        modifiedItem.view = {
            salon: {
                code: 'dealer-slug',
                is_official: true,
            },
        };

        const preparedItem = prepareSubscriptionItem(modifiedItem, { config: {}, req: {} }, {});
        expect(preparedItem.url).toEqual('https://autoru_frontend.base_domain/diler-oficialniy/cars/all/dealer-slug/');
    });
});

describe('mobile', () => {
    beforeEach(() => {
        isMobileApp.mockImplementation(() => true);
    });

    it('должен правильно подготовить данные о подписке в мобилке', () => {
        const preparedItem = prepareSubscriptionItem(item, { config: {}, req: {} }, {});
        expect(preparedItem).toEqual({
            category: 'CARS',
            id: '123',
            params: {
                rid: '',
                section: 'all',
            },
            paramsDescription: {
                moreCount: 0,
                paramsInfo: [],
            },
            title: 'Все марки автомобилей',
            url: 'https://autoru_frontend.base_domain/cars/all/?geo_id=',
        });
    });

    it('должен правильно обработать саджест категории trucks', () => {
        const preparedItem = prepareSubscriptionItem(trucksItem, { config: {}, req: {} }, {});
        expect(preparedItem).toEqual(
            {
                params: {
                    light_truck_type: [
                        'MINIBUS',
                    ],
                    rid: '',
                    trucks_category: 'LCV',
                },
                paramsDescription: {
                    moreCount: 0,
                    paramsInfo: [
                        {
                            val: 'С пробегом',
                            key: 'section',
                        },
                        {
                            label: 'Тип кузова',
                            val: 'MINIBUS',
                            key: 'light_truck_type',
                        },
                    ],
                },
                relevance: 1,
                url: 'https://autoru_frontend.base_domain/lcv/all/?geo_id=&body_key=MINIBUS',
                view: {
                    applied_filter_count: 1,
                },
            },
        );
    });
});
