import mockdate from 'mockdate';

import type {
    ChangeFormAction,
    FormCheckResultAction,
    StateFormFields,
} from 'auto-core/react/dataDomain/formFields/types';
import { PAGE_LOADING_SUCCESS } from 'auto-core/react/actionTypes';
import { CHANGE_FORM, FORM_CHECK_RESULT } from 'auto-core/react/dataDomain/formFields/types';

import reducer from './reducer';

let state: StateFormFields;

describe('CHANGE_FORM', () => {
    let changeFormAction: ChangeFormAction;
    beforeEach(() => {
        changeFormAction = {
            type: CHANGE_FORM,
            payload: {
                fields: { mark: { value: 'AUDI' } },
                clearFields: [ 'model', 'year' ],
            },
        };
        state = reducer(undefined, { type: PAGE_LOADING_SUCCESS, payload: {} });
    });

    it('должен записать значение в стор', () => {
        expect(reducer(state, changeFormAction)).toMatchObject({
            data: {
                mark: { value: 'AUDI', touched: true },
            },
        });
    });

    it('должен удалить значения из стора согласно payload.clearFields', () => {
        // заполняем state данными
        state = reducer(state, {
            type: CHANGE_FORM,
            payload: {
                fields: {
                    mark: { value: 'AUDI' },
                    model: { value: 'A4' },
                    year: { value: '2020' },
                },
            },
        });
        state = reducer(state, changeFormAction);

        expect(state.data.mark).toEqual({ value: 'AUDI', touched: true });
        expect(state.data.model).toBeUndefined();
        expect(state.data.year).toBeUndefined();
    });
});

describe('FORM_CHECK_RESULT', () => {
    let changeFormAction: FormCheckResultAction;
    beforeEach(() => {
        mockdate.set('2021-02-02T00:00:00Z');
        changeFormAction = {
            type: FORM_CHECK_RESULT,
            payload: {
                fields: { mark: { value: 'AUDI' } },
                clearFields: [ 'model', 'year' ],
            },
        };
        state = reducer(undefined, { type: PAGE_LOADING_SUCCESS, payload: {} });
    });

    it('должен записать значение в стор и обновить formCheckTimestamp', () => {
        expect(reducer(state, changeFormAction)).toMatchObject({
            data: {
                mark: { value: 'AUDI', touched: true },
            },
            formCheckTimestamp: 1612224000000,
        });
    });
});
