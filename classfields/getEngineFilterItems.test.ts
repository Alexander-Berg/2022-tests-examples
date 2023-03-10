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

it('???????????? ?????????????? ?????????? ???????????????????? ?? ???????????????????? ?????? ?????????????? ???? ??????????????????, ?????????????????? ?????????????????? ?? ?????????????????????? ????????????????????????????????', () => {
    expect(getEngineFilterItems(state)).toStrictEqual([
        {
            id: '2.0 ?? / 150 ??.??. / ????????????',
            engineType: 'DIESEL',
            power: 150,
            title: '???????????? 2.0 ??, 150 ??.c.',
            value: [ '345', '346' ],
            acceleration: 11,
            displacement: 1968,
            fuel_rate: 5.1,
        },
        {
            id: '400 ??.??. / 294 ?????? / ??????????????',
            engineType: 'ELECTRO',
            power: 400,
            title: '?????????????? / 400 ??.c. / 294 ??????',
            value: [ '123' ],
            acceleration: 7,
            displacement: 0,
            fuel_rate: undefined,
        },
    ]);
});
