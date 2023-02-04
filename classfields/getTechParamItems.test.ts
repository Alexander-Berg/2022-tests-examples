import TECH_PARAM from 'auto-core/react/dataDomain/carsTechOptions/mocks/techParam';

import type { StateCarsTechOptions } from '../types';

import getTechParamItems from './getTechParamItems';

const state = {
    carsTechOptions: {
        data: {
            tech_param: TECH_PARAM,
        },
    } as unknown as StateCarsTechOptions,
};

it('должен вернуть items для tech_param', () => {
    expect(getTechParamItems(state)).toMatchSnapshot();
});

it('должен вернуть пустой items для tech_param, если данных нет', () => {
    expect(getTechParamItems({
        carsTechOptions: {
            data: {
                tech_param: [],
            },
        } as unknown as StateCarsTechOptions,
    })).toEqual([]);
});
