jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn().mockResolvedValue({}),
    };
});

import type { ThunkMockStore } from 'autoru-frontend/mocks/mockStore';
import mockStore from 'autoru-frontend/mocks/mockStore';

import type { StateFormFields } from 'auto-core/react/dataDomain/formFields/types';

import { changeForm } from './form';

interface State {
    formFields: StateFormFields;
}

let store: ThunkMockStore<State>;

// Это копия набора из GarageFormFieldsSelectorCurrent/formFieldsOptions.ts
// на котором изначально был написан тест
const fields = [
    {
        name: 'license_plate',
        placeholder: 'Госномер',
    },
    {
        name: 'vin',
        placeholder: 'VIN',
    },
    {
        name: 'mark',
        clearFields: [ 'model' ],
        component: () => null,
    },
    {
        name: 'model',
        clearFields: [ 'year' ],
        component: () => null,
    },
    {
        name: 'year',
        placeholder: 'Год',
        clearFields: [ 'super_gen', 'buyYear', 'buyMonth' ],
        options: [],
    },
    {
        name: 'fake_spoiler',
        component: () => null,
    },
    {
        name: 'super_gen',
        placeholder: 'Поколение',
        clearFields: [ 'body_type' ],
        options: [],
    },
    {
        name: 'body_type',
        placeholder: 'Тип кузова',
        clearFields: [ 'engine_type' ],
        options: [],
    },
    {
        name: 'engine_type',
        placeholder: 'Двигатель',
        clearFields: [ 'gear_type' ],
        options: [],
    },
    {
        name: 'gear_type',
        placeholder: 'Привод',
        clearFields: [ 'transmission_full' ],
        options: [],
    },
    {
        name: 'transmission_full',
        placeholder: 'Коробка передач',
        clearFields: [ 'tech_param' ],
        options: [],
    },
    {
        name: 'tech_param',
        placeholder: 'Модификация',
        clearFields: [ 'complectation' ],
        options: [],
    },
    {
        name: 'complectation',
        component: () => null,
    },
    {
        name: 'color',
        options: [],
        clearFields: [],
        component: () => null,
    },
    {
        name: 'owners_number',
        placeholder: 'Владельцев по ПТС',
        clearFields: [],
        component: () => null,
    },
    {
        name: 'owner',
        placeholder: 'Дата приобретения',
        component: () => null,
    },
];

describe('changeForm', () => {
    beforeEach(() => {
        store = mockStore({
            formFields: {
                data: {},
                isPending: false,
                isCollapsed: false,
                formErrorText: '',
                needActionBeforeSubmit: false,
            },
        });
    });

    it('при смене year должен поменять year, сбросить поля ниже и перезапросить крошки', async() => {
        await store.dispatch(changeForm(fields, 'year', 2020));

        expect(store.getActions()).toEqual([
            {
                type: 'CHANGE_FORM',
                payload: {
                    fields: {
                        year: { value: 2020 },
                    },
                    clearFields: [
                        'super_gen',
                        'body_type',
                        'engine_type',
                        'gear_type',
                        'transmission_full',
                        'tech_param',
                        'complectation',
                        'buyYear',
                        'buyMonth',
                    ],
                },
            },
            {
                type: 'BREADCRUMBS_PUBLICAPI_PENDING',
            },
            {
                type: 'LOAD_TECH_OPTIONS_PENDING',
                invalidateLevels: [
                    'super_gen',
                    'body_type',
                    'engine_type',
                    'gear_type',
                    'transmission_full',
                    'tech_param',
                    'complectations',
                ],
            },
            { type: 'BREADCRUMBS_PUBLICAPI_MERGE_RESOLVED', payload: {} },
            { type: 'LOAD_TECH_OPTIONS_RESOLVED', payload: {} },
        ]);
    });

    it('при смене vin должен поменять vin и не запрашивать крошки', async() => {
        await store.dispatch(changeForm(fields, 'vin', '1'));

        expect(store.getActions()).toEqual([
            {
                type: 'CHANGE_FORM',
                payload: {
                    fields: {
                        vin: { value: '1' },
                    },
                    clearFields: [],
                },
            },
        ]);
    });
});
