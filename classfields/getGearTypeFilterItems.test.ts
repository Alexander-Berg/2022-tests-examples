import type { StateCardGroupComplectations } from 'auto-core/react/dataDomain/cardGroupComplectations/types';

import getGearTypeFilterItems from './getGearTypeFilterItems';

const state = {
    cardGroupComplectations: {
        data: {
            complectations: [
                {
                    tech_info: {
                        tech_param: {
                            gear_type: 'ALL_WHEEL_DRIVE',
                        },
                    },
                },
                {
                    tech_info: {
                        tech_param: {
                            gear_type: 'ALL_WHEEL_DRIVE',
                        },
                    },
                },
                {
                    tech_info: {
                        tech_param: {
                            gear_type: 'FORWARD_CONTROL',
                        },
                    },
                },
                {
                    tech_info: {
                        tech_param: {
                            gear_type: 'FORWARD_CONTROL',
                        },
                    },
                },
            ],
        },
    } as unknown as StateCardGroupComplectations,
};

it('должен вернуть набор приводов для фильтра по типу привода', () => {
    expect(getGearTypeFilterItems(state)).toStrictEqual([
        {
            title: 'Передний',
            value: 'FORWARD_CONTROL',
        },
        {
            title: 'Полный',
            value: 'ALL_WHEEL_DRIVE',
        },
    ]);
});
