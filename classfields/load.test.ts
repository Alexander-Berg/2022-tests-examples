jest.mock('auto-core/react/lib/gateApi', () => ({
    getResource: jest.fn().mockResolvedValue({}),
}));

import { cloneDeep } from 'lodash';

import type { ThunkMockStore } from 'autoru-frontend/mocks/mockStore';
import mockStore from 'autoru-frontend/mocks/mockStore';

import gateApi from 'auto-core/react/lib/gateApi';
import mockFormFieldsCarsEvaluation from 'auto-core/react/dataDomain/formFields/mocks/carsEvaluation';

import type { State } from './load';
import load from './load';

let store: ThunkMockStore<State>;

it('должен запросить следующий уровень вариантов выбора', async() => {
    // Мокаем стадию выбора: марка, модель, год, кузов, поколение.
    const formFields = cloneDeep(mockFormFieldsCarsEvaluation);
    delete formFields.data.engine_type;
    delete formFields.data.gear_type;
    delete formFields.data.tech_param;
    delete formFields.data.transmission_full;
    store = mockStore({ formFields });

    // Выбираем "двигатель".
    await store.dispatch(load(
        'engine_type',
        'GASOLINE',
        { parent_category: 'cars' },
    ));

    expect(store.getActions()).toEqual([
        {
            type: 'LOAD_TECH_OPTIONS_PENDING',
            invalidateLevels: [ 'gear_type', 'transmission_full', 'tech_param', 'complectations' ],
        },
        {
            type: 'LOAD_TECH_OPTIONS_RESOLVED',
            payload: {},
        },
    ]);

    expect(gateApi.getResource).toHaveBeenCalledTimes(1);
    expect(gateApi.getResource).toHaveBeenCalledWith('catalogSuggestOptions', {
        body_type: 'SEDAN',
        engine_type: 'GASOLINE',
        mark: 'FORD',
        model: 'ECOSPORT',
        super_gen: '20104320',
        year: '2020',
    });
});

it('должен перемапить некоторые названия при запросе', async() => {
    // Мокаем последнюю стадию выбора tech_params
    const formFields = cloneDeep(mockFormFieldsCarsEvaluation);
    delete formFields.data.tech_param;
    store = mockStore({ formFields });

    // Выбираем
    await store.dispatch(load(
        'tech_param',
        '20104325',
        { parent_category: 'cars' },
    ));

    expect(store.getActions()).toEqual([
        {
            type: 'LOAD_TECH_OPTIONS_PENDING',
            invalidateLevels: [ 'complectations' ],
        },
        {
            type: 'LOAD_TECH_OPTIONS_RESOLVED',
            payload: {},
        },
    ]);

    expect(gateApi.getResource).toHaveBeenCalledTimes(1);
    expect(gateApi.getResource).toHaveBeenCalledWith('catalogSuggestOptions', {
        body_type: 'SEDAN',
        engine_type: 'GASOLINE',
        gear_type: 'FORWARD_CONTROL',
        mark: 'FORD',
        model: 'ECOSPORT',
        super_gen: '20104320',
        year: '2020',
        transmission: 'ROBOT',
        tech_param_id: '20104325',
    });
});
