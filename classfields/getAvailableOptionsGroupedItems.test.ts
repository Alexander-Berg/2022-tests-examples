import equipmentDictionaryMock from 'auto-core/react/dataDomain/equipmentDictionary/mocks/dictionary';
import type { TStateListing } from 'auto-core/react/dataDomain/listing/TStateListing';
import type { StateCardGroupComplectations } from 'auto-core/react/dataDomain/cardGroupComplectations/types';

import getAvailableOptionsGroupedItems from './getAvailableOptionsGroupedItems';

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
                        tech_param: '22222',
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
                            available_options: [ 'xenon', '12-inch-wheels' ],
                        },
                    },
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
                            id: '22222',
                            engine_type: 'ELECTRO',
                            transmission: 'AUTOMATIC',
                            gear_type: 'ALL_WHEEL_DRIVE',
                        },
                        complectation: {
                            available_options: [ 'ptf', 'light-cleaner' ],
                        },
                    },
                },
            ],
        },
    } as unknown as StateCardGroupComplectations,
};

it('должен вернуть сгруппированные базовые опции для комплектации, имя которой соответствует поисковым параметрам', () => {
    expect(getAvailableOptionsGroupedItems(state)).toStrictEqual([
        {
            groupName: 'Обзор',
            options: [
                { code: 'xenon', group: 'Обзор', name: 'Ксеноновые/Биксеноновые фары' },
            ],
        },
        {
            groupName: 'Элементы экстерьера',
            options: [
                { code: '12-inch-wheels', group: 'Элементы экстерьера', name: 'Легкосплавные диски 12"' },
            ],
        },
    ]);
});
