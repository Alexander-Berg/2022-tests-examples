const getCardGroupFilterValues = require('./getCardGroupFilterValues');

it('должен достать значения фильтров из поисковых параметров листинга', () => {
    const state = {
        listing: {
            data: {
                search_parameters: {
                    catalog_filter: [
                        {
                            mark: 'A',
                            model: 'B',
                            generation: '666',
                            configuration: '666',
                            tech_param: '123',
                            complectation_name: 'Test',
                        },
                        {
                            mark: 'A',
                            model: 'B',
                            generation: '666',
                            configuration: '666',
                            tech_param: '345',
                            complectation_name: 'Test',
                        },
                        {
                            mark: 'A',
                            model: 'B',
                            generation: '666',
                            configuration: '666',
                            tech_param: '346',
                            complectation_name: 'Test',
                        },
                    ],
                    transmission: [ 'AUTOMATIC' ],
                    gear_type: [ 'ALL_WHEEL_DRIVE' ],
                    color: [ '926547' ],
                    catalog_equipment: [ 'automatic-lighting-control' ],
                    sort: 'fresh_relevance_1-desc',
                    in_stock: 'IN_STOCK',
                    year_to: 2019,
                    search_tag: [ 'compact' ],
                    price_from: 1000000,
                    price_to: 9999999,
                    has_video: true,
                    online_view: true,
                },
            },
        },
        cardGroupComplectations: {
            data: {
                complectations: [
                    {
                        tech_info: {
                            tech_param: {
                                id: '123',
                                displacement: 0,
                                engine_type: 'ELECTRO',
                                power: 400,
                                power_kvt: 294,
                                acceleration: 7,
                            },
                        },
                    },
                    {
                        tech_info: {
                            tech_param: {
                                id: '345',
                                displacement: 1968,
                                engine_type: 'DIESEL',
                                power: 150,
                                power_kvt: 110,
                                acceleration: 11,
                                fuel_rate: 5.1,
                            },
                        },
                    },
                    {
                        tech_info: {
                            tech_param: {
                                id: '346',
                                displacement: 1968,
                                engine_type: 'DIESEL',
                                power: 150,
                                power_kvt: 110,
                                acceleration: 11,
                                fuel_rate: 5.1,
                            },
                        },
                    },
                ],
            },
        },
    };

    expect(getCardGroupFilterValues(state)).toEqual({
        complectation_name: 'Test',
        tech_param_id: [ '345,346', '123' ],
        transmission: [ 'AUTOMATIC' ],
        gear_type: [ 'ALL_WHEEL_DRIVE' ],
        color: [ '926547' ],
        catalog_equipment: [ 'automatic-lighting-control' ],
        selected_complectation_names: [ 'Test' ],
        sort: 'fresh_relevance_1-desc',
        in_stock: 'IN_STOCK',
        year_to: 2019,
        search_tag: [ 'compact' ],
        price_from: 1000000,
        price_to: 9999999,
        has_video: true,
        online_view: true,
    });
});

// eslint-disable-next-line max-len
it('должен вернуть пустые массивы в качестве значений фильтров для модификации, трансмиссии, привода, цветов, опций и search_tag, если нет соответствующих значений поисковых параметров', () => {
    const state = {
        cardGroupComplectations: { data: {} },
        listing: {
            data: {
                search_parameters: {
                    catalog_filter: [
                        {
                            mark: 'A',
                            model: 'B',
                            generation: '666',
                            configuration: '666',
                        },
                    ],
                },
            },
        },
    };

    expect(getCardGroupFilterValues(state)).toMatchObject({
        tech_param_id: [],
        transmission: [],
        gear_type: [],
        color: [],
        catalog_equipment: [],
        search_tag: [],
    });
});
