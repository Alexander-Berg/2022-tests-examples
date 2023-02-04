import type { TComplectation } from 'auto-core/react/dataDomain/cardGroupComplectations/types';

import createComplectationFilterItem from './createComplectationFilterItem';

const COMPLECTATIONS = [
    {
        complectation_id: '12345',
        complectation_name: 'Ultimate1',
        option_count: 3,
        price_from: {
            RUR: 25400000,
        },
        price_to: {
            RUR: 30000000,
        },
        tech_info: {
            complectation: {
                id: 12345,
                name: 'Ultimate',
                available_options: [
                    'airbag-front',
                    'abs',
                    'navigation',
                ],
            },
            tech_param: {
                human_name: '1.4 MT',
            },
        },
    },
    {
        complectation_id: '12346',
        complectation_name: 'Ultimate2',
        option_count: 4,
        price_from: {
            RUR: 25100000,
        },
        price_to: {
            RUR: 29000000,
        },
        tech_info: {
            complectation: {
                id: 12346,
                name: 'Ultimate',
                available_options: [
                    'airbag-front',
                    'abs',
                    'cruise_control',
                    'lock',
                ],
            },
            tech_param: {
                human_name: '1.4 AT',
            },
        },
    },
] as unknown as Array<TComplectation>;

let collapsedComplectationGroup;

it('должен вернуть элемент фильтра комплектаций с агрегированными данными по опциям и цене', () => {
    collapsedComplectationGroup = createComplectationFilterItem({ title: 'Все комплектации', value: '', complectations: COMPLECTATIONS });
    expect(collapsedComplectationGroup).toStrictEqual({
        title: 'Все комплектации',
        value: '',
        optionsCount: 2,
        priceFrom: {
            RUR: 25100000,
        },
        availableOptions: [
            'airbag-front',
            'abs',
        ],
        uniqueAvailableOptions: [],
    });
});
