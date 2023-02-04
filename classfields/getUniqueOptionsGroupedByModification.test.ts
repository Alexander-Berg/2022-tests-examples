import type { TComplectation } from 'auto-core/react/dataDomain/cardGroupComplectations/types';

import getUniqueOptionsGroupedByModification from './getUniqueOptionsGroupedByModification';

const complectations = [
    {
        tech_info: {
            tech_param: {
                human_name: '1.4 MT',
            },
            complectation: {
                available_options: [ 'option1', 'option2' ],
            },
        },
    },
    {
        tech_info: {
            tech_param: {
                human_name: '1.4 AT',
            },
            complectation: {
                available_options: [ 'option1', 'option2', 'option3' ],
            },
        },
    },
    {
        tech_info: {
            tech_param: {
                human_name: '1.4 AT +',
            },
            complectation: {
                available_options: [ 'option1', 'option2', 'option3' ],
            },
        },
    },
    {
        tech_info: {
            tech_param: {
                human_name: '1.4 AT ++',
            },
            complectation: {
                available_options: [ 'option1', 'option2', 'option3' ],
            },
        },
    },
    {
        tech_info: {
            tech_param: {
                human_name: '1.4 AT +++',
            },
            complectation: {
                available_options: [ 'option1', 'option2', 'option3' ],
            },
        },
    },
] as unknown as Array<TComplectation>;

const commonBaseOptions = [ 'option1' ];

it('должен вернуть список опций, не входящих в список общих опций, сгруппированный по модификациям', () => {
    expect(getUniqueOptionsGroupedByModification(
        complectations,
        commonBaseOptions,
    )).toStrictEqual([
        {
            modification: '1.4 MT',
            options: [ 'option2' ],
            prefix: 'Только для модификации',
        },
        {
            modification: '1.4 MT',
            options: [ 'option2', 'option3' ],
            prefix: 'Кроме модификации',
        },
    ]);
});
