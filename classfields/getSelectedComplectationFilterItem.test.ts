import type { StateCardGroupComplectations } from 'auto-core/react/dataDomain/cardGroupComplectations/types';
import type { TStateListing } from 'auto-core/react/dataDomain/listing/TStateListing';

import getSelectedComplectationFilterItem from './getSelectedComplectationFilterItem';

const CARD_GROUP_COMPLECTATIONS = {
    data: {
        complectations: [
            {
                complectation_name: 'Test',
                colors_hex: [ '926547', '000000' ],
                price_from: {
                    RUR: 1000000,
                },
                price_to: {
                    RUR: 2000000,
                },
                tech_info: {
                    tech_param: {
                        id: '00000',
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
                        engine_type: 'ELECTRO',
                        id: '22222',
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
} as unknown as StateCardGroupComplectations;

it('должен вернуть элемент фильтра комплектаций, значение которого соответствует поисковым параметрам', () => {
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
                            complectation_name: 'Test1',
                        },
                    ],
                },
            },
        } as TStateListing,
        cardGroupComplectations: CARD_GROUP_COMPLECTATIONS,
    };

    expect(getSelectedComplectationFilterItem(state)).toStrictEqual(
        {
            title: 'Test1',
            value: 'Test1',
            optionsCount: 2,
            priceFrom: {
                RUR: 1000000,
            },
            availableOptions: [ 'option1', 'option2' ],
            uniqueAvailableOptions: [],
        },
    );
});
