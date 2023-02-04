import _ from 'lodash';

import type { StateCardGroupComplectations } from 'auto-core/react/dataDomain/cardGroupComplectations/types';
import type { TStateListing } from 'auto-core/react/dataDomain/listing/TStateListing';
import type { StateEquipmentFilters } from 'auto-core/react/dataDomain/equipmentFilters/StateEquipmentFilters';

import getAdditionalOptionsFilterItems from './getAdditionalOptionsFilterItems';

const STATE = {
    equipmentFilters: {
        data: {
            categories: [
                {
                    name: 'Обзор',
                    groups: [
                        {
                            name: 'Фары',
                            options: [
                                {
                                    code: 'xenon',
                                    offers_count: 1,
                                    full_name: 'Ксеноновые фары',
                                },
                                {
                                    code: 'laser-lights',
                                    offers_count: 1,
                                    full_name: 'Лазерные фары',
                                },
                                {
                                    code: 'led-lights',
                                    offers_count: 2,
                                    full_name: 'Светодиодные фары',
                                },
                            ],
                        },
                    ],
                },
            ],
        },
    } as StateEquipmentFilters,
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
                            available_options: [ 'xenon' ],
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

it('должен вернуть дополнительные опции для комплектации, имя которой соответствует поисковым параметрам', () => {
    expect(getAdditionalOptionsFilterItems(STATE)).toStrictEqual([
        {
            code: 'laser-lights',
            name: 'Лазерные фары',
            group: 'Фары',
        },
        {
            name: 'Светодиодные фары',
            code: 'led-lights',
            group: 'Фары',
        },
    ]);
});

describe('обновление значений фильтра только при изменении списка актуальных опций', () => {
    let state: typeof STATE;
    beforeEach(() => {
        state = _.cloneDeep(STATE);
    });

    it('должен вернуть прежнее значение, если не изменился список актуальных опций', () => {
        const previousResult = getAdditionalOptionsFilterItems(state);
        const nextState = { ...state };
        nextState.listing = {
            data: {
                search_parameters: {
                    catalog_filter: [
                        {
                            mark: 'A',
                            model: 'B',
                            generation: '666',
                            configuration: '666',
                            tech_param: '22222',
                        },
                    ],
                },
            },
        } as TStateListing;
        const newResult = getAdditionalOptionsFilterItems(nextState);
        expect(previousResult).toStrictEqual(newResult);
    });

    it('должен вернуть обновленное значение, если изменился список актуальных опций', () => {
        const previousResult = getAdditionalOptionsFilterItems(state);
        const nextState = { ...state };
        nextState.equipmentFilters = {} as StateEquipmentFilters;
        const newResult = getAdditionalOptionsFilterItems(nextState);
        expect(previousResult).not.toEqual(newResult);
    });
});
