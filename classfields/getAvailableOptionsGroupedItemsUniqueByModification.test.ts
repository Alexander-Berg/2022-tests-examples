import equipmentDictionaryMock from 'auto-core/react/dataDomain/equipmentDictionary/mocks/dictionary';
import type { TStateListing } from 'auto-core/react/dataDomain/listing/TStateListing';
import type { StateCardGroupComplectations } from 'auto-core/react/dataDomain/cardGroupComplectations/types';

import getAvailableOptionsGroupedItemsUniqueByModification from './getAvailableOptionsGroupedItemsUniqueByModification';

const state = {
    equipmentDictionary: equipmentDictionaryMock,
    listing: {
        data: {
            search_parameters: {
                catalog_filter: [
                    {
                        mark: 'A',
                        model: 'B',
                        generation: '666',
                        configuration: '666',
                        complectation_name: 'Test',
                    },
                ],
            },
        },
    } as TStateListing,
    cardGroupComplectations: {
        data: {
            complectations: [
                {
                    complectation_name: 'Test',
                    tech_info: {
                        tech_param: {
                            id: '00000',
                            engine_type: 'ELECTRO',
                            human_name: '1.4 AT',
                        },
                        complectation: {
                            available_options: [ 'xenon', 'ptf', '12-inch-wheels' ],
                        },
                    },
                    offer_count: 10,
                },
                {
                    complectation_name: 'Test',
                    tech_info: {
                        tech_param: {
                            id: '22222',
                            engine_type: 'ELECTRO',
                            human_name: '1.4 MT',
                        },
                        complectation: {
                            available_options: [ 'xenon', 'ptf', 'light-cleaner' ],
                        },
                    },
                    offer_count: 10,
                },
            ],
        },
    } as unknown as StateCardGroupComplectations,
};

it('должен вернуть опции, уникальные для отдельных модификаций, сгруппированные по группе опций', () => {
    expect(getAvailableOptionsGroupedItemsUniqueByModification(state)).toStrictEqual({
        Обзор: [
            {
                modification: '1.4 MT',
                options: [
                    {
                        code: 'light-cleaner',
                        group: 'Обзор',
                        name: 'Омыватель фар',
                    },
                ],
                prefix: 'Только для модификации',
            },
        ],
        'Элементы экстерьера': [
            {
                modification: '1.4 AT',
                options: [
                    {
                        code: '12-inch-wheels',
                        group: 'Элементы экстерьера',
                        name: 'Легкосплавные диски 12"',
                    },
                ],
                prefix: 'Только для модификации',
            },
        ],
    });
});
