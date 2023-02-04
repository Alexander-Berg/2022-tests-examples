import type { TComplectation } from 'auto-core/react/dataDomain/cardGroupComplectations/types';
import type { TCardGroupFiltersValues } from 'auto-core/react/dataDomain/cardGroup/types';

import filterComplectations from './filterComplectations';

const complectations: Array<TComplectation> = [
    {
        tech_info: {
            complectation: {
                available_options: [],
                id: '1',
                name: 'One',
            },
            tech_param: {
                id: '111',
                transmission: 'AUTOMATIC',
                gear_type: 'ALL_WHEEL_DRIVE',
            },
        },
        colors_hex: [ '040001', 'CACECB' ],
        offer_count: 10,
    } as unknown as TComplectation,
    {
        tech_info: {
            complectation: {
                available_options: [],
                id: '2',
                name: 'Two',
            },
            tech_param: {
                id: '222',
                transmission: 'AUTOMATIC',
                gear_type: 'ALL_WHEEL_DRIVE',
            },
        },
        colors_hex: [ '040001', 'CACECB' ],
        offer_count: 10,
    } as unknown as TComplectation,
    {
        tech_info: {
            complectation: {
                available_options: [],
                id: '3',
                name: 'Three',
            },
            tech_param: {
                id: '333',
                transmission: 'MECHANICAL',
                gear_type: 'FORWARD_CONTROL',
            },
        },
        colors_hex: [ '040001', 'CACECB' ],
        offer_count: 10,
    } as unknown as TComplectation,
    {
        complectation_id: '4',
        complectation_name: 'Four',
        tech_info: {
            complectation: {
                available_options: [],
                id: '4',
                name: 'Four',
            },
            tech_param: {
                id: '222',
                transmission: 'AUTOMATIC',
                gear_type: 'ALL_WHEEL_DRIVE',
            },
        },
        colors_hex: [ 'CACECB' ],
        offer_count: 10,
    } as unknown as TComplectation,
];

it('должен вернуть комплектации, соответствующие поисковым параметрам', () => {
    const filters = {
        transmission: [ 'AUTOMATIC' ],
        gear_type: [ 'ALL_WHEEL_DRIVE' ],
        color: [ '040001' ],
        tech_param_id: [ '222' ],
    } as TCardGroupFiltersValues;

    expect(filterComplectations(complectations, filters)).toEqual([
        {
            tech_info: {
                complectation: {
                    available_options: [],
                    id: '2',
                    name: 'Two',
                },
                tech_param: {
                    id: '222',
                    transmission: 'AUTOMATIC',
                    gear_type: 'ALL_WHEEL_DRIVE',
                },
            },
            colors_hex: [ '040001', 'CACECB' ],
            offer_count: 10,
        },
    ]);
});

it('должен вернуть комплектации, если в techParams склеенные значения', () => {
    const filters = {
        tech_param_id: [ '111,333' ],
    } as TCardGroupFiltersValues;

    expect(filterComplectations(complectations, filters)).toEqual([
        {
            tech_info: {
                complectation: {
                    available_options: [],
                    id: '1',
                    name: 'One',
                },
                tech_param: {
                    id: '111',
                    transmission: 'AUTOMATIC',
                    gear_type: 'ALL_WHEEL_DRIVE',
                },
            },
            colors_hex: [ '040001', 'CACECB' ],
            offer_count: 10,
        },
        {
            tech_info: {
                complectation: {
                    available_options: [],
                    id: '3',
                    name: 'Three',
                },
                tech_param: {
                    id: '333',
                    transmission: 'MECHANICAL',
                    gear_type: 'FORWARD_CONTROL',
                },
            },
            colors_hex: [ '040001', 'CACECB' ],
            offer_count: 10,
        },
    ]);
});

it('должен вернуть все комплектации, если поисковый запрос пуст', () => {
    const filters = {} as TCardGroupFiltersValues;

    expect(filterComplectations(complectations, filters)).toEqual(complectations);
});
