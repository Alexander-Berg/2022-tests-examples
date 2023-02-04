import type { StateCardGroupComplectations } from 'auto-core/react/dataDomain/cardGroupComplectations/types';
import type { TStateListing } from 'auto-core/react/dataDomain/listing/TStateListing';

import getComplectationFilterItems from './getComplectationFilterItems';

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
                        tech_param: '22222',
                        complectation_name: 'Test',
                    },
                ],
                transmission: [ 'AUTOMATIC' ],
                gear_type: [ 'ALL_WHEEL_DRIVE' ],
                color: [ '926547' ],
                catalog_equipment: [ 'option1' ],
            },
        },
    } as TStateListing,
    cardGroupComplectations: {
        data: {
            complectations: [
                {
                    complectation_name: 'Test',
                    colors_hex: [ '926547', '000000' ],
                    price_from: {
                        RUR: 1100000,
                    },
                    price_to: {
                        RUR: 2000000,
                    },
                    tech_info: {
                        tech_param: {
                            id: '0000',
                            engine_type: 'ELECTRO',
                            transmission: 'AUTOMATIC',
                            gear_type: 'ALL_WHEEL_DRIVE',
                        },
                        complectation: {
                            available_options: [ 'option1', 'option2' ],
                        },
                    },
                    offer_count: 10,
                },
                {
                    complectation_name: 'Test1',
                    colors_hex: [ '926547', '000000' ],
                    price_from: {
                        RUR: 1000000,
                    },
                    price_to: {
                        RUR: 2000000,
                    },
                    tech_info: {
                        tech_param: {
                            id: '1111',
                            engine_type: 'ELECTRO',
                            transmission: 'AUTOMATIC',
                            gear_type: 'ALL_WHEEL_DRIVE',
                        },
                        complectation: {
                            available_options: [ 'option1', 'option2' ],
                        },
                    },
                    offer_count: 10,
                },
                {
                    complectation_name: 'Test2',
                    colors_hex: [ '926547', '000000' ],
                    price_from: {
                        RUR: 1000000,
                    },
                    price_to: {
                        RUR: 2000000,
                    },
                    tech_info: {
                        tech_param: {
                            id: '2222',
                            engine_type: 'ELECTRO',
                            transmission: 'AUTOMATIC',
                            gear_type: 'ALL_WHEEL_DRIVE',
                        },
                        complectation: {
                            available_options: [ 'option1', 'option2' ],
                        },
                    },
                    offer_count: 10,
                },
                {
                    complectation_name: 'Test3',
                    colors_hex: [ '000000' ],
                    price_from: {
                        RUR: 1000000,
                    },
                    price_to: {
                        RUR: 2000000,
                    },
                    tech_info: {
                        tech_param: {
                            id: '3333',
                            engine_type: 'ELECTRO',
                            transmission: 'AUTOMATIC',
                            gear_type: 'ALL_WHEEL_DRIVE',
                        },
                        complectation: {
                            available_options: [ 'option1', 'option2' ],
                        },
                    },
                    offer_count: 10,
                },
            ],
        },
    } as unknown as StateCardGroupComplectations,
};

it('должен вернуть набор комплектаций для фильтра по комплектациям', () => {
    expect(getComplectationFilterItems(state)).toStrictEqual([
        {
            title: 'Test1',
            value: 'Test1',
            optionsCount: 2,
            priceFrom: { RUR: 1000000 },
            availableOptions: [ 'option1', 'option2' ],
            uniqueAvailableOptions: [],
        },
        {
            title: 'Test2',
            value: 'Test2',
            optionsCount: 2,
            priceFrom: { RUR: 1000000 },
            availableOptions: [ 'option1', 'option2' ],
            uniqueAvailableOptions: [],
        },
        {
            title: 'Test',
            value: 'Test',
            optionsCount: 2,
            priceFrom: { RUR: 1100000 },
            availableOptions: [ 'option1', 'option2' ],
            uniqueAvailableOptions: [],
        },
    ]);
});
