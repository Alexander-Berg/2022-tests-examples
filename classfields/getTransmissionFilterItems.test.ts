import type { StateCardGroupComplectations } from 'auto-core/react/dataDomain/cardGroupComplectations/types';

import getTransmissionFilterItems from './getTransmissionFilterItems';

const state = {
    cardGroupComplectations: {
        data: {
            complectations: [
                {
                    tech_info: {
                        tech_param: {
                            transmission: 'AUTOMATIC',
                        },
                    },
                },
                {
                    tech_info: {
                        tech_param: {
                            transmission: 'AUTOMATIC',
                        },
                    },
                },
                {
                    tech_info: {
                        tech_param: {
                            transmission: 'MECHANICAL',
                        },
                    },
                },
                {
                    tech_info: {
                        tech_param: {
                            transmission: 'ROBOT',
                        },
                    },
                },
            ],
        },
    } as unknown as StateCardGroupComplectations,
};

it('должен вернуть набор трансмиссий для фильтра по типу коробки передач', () => {
    expect(getTransmissionFilterItems(state)).toStrictEqual([
        {
            title: 'Автоматическая',
            value: 'AUTOMATIC',
        },
        {
            title: 'Роботизированная',
            value: 'ROBOT',
        },
        {
            title: 'Механическая',
            value: 'MECHANICAL',
        },
    ]);
});
