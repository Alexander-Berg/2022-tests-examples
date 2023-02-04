import type { StateCardGroupComplectations } from 'auto-core/react/dataDomain/cardGroupComplectations/types';

import getEngineFilterItems from './getEngineFilterItems';

const state = {
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
    } as unknown as StateCardGroupComplectations,
};

it('должен вернуть набор информации о двигателях для фильтра по двигателю, объединяя двигатели с одинаковыми характеристиками', () => {
    expect(getEngineFilterItems(state)).toStrictEqual([
        {
            id: '2.0 л / 150 л.с. / Дизель',
            engineType: 'DIESEL',
            power: 150,
            title: 'Дизель 2.0 л, 150 л.c.',
            value: [ '345', '346' ],
            acceleration: 11,
            displacement: 1968,
            fuel_rate: 5.1,
        },
        {
            id: '400 л.с. / 294 кВт / Электро',
            engineType: 'ELECTRO',
            power: 400,
            title: 'Электро / 400 л.c. / 294 кВт',
            value: [ '123' ],
            acceleration: 7,
            displacement: 0,
            fuel_rate: undefined,
        },
    ]);
});
