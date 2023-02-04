import type { FormState } from '../types';

import type { SetValueAction } from './formReducer';
import formReducer, { FormActionTypes } from './formReducer';

describe('SET_VALUE', () => {
    it('если пришло значение null оставит это поле в стейте', () => {
        const state: FormState<'foo', { foo: string }, any> = {
            values: {
                foo: 'bar',
            },
            errors: {},
            touched: {},
            focused: 'foo',
            previousFocused: 'foo',
        };
        const action: SetValueAction<'foo', { foo: string }> = {
            type: FormActionTypes.SET_VALUE,
            payload: {
                fieldName: 'foo',
                value: null,
            },
        };
        const nextState = formReducer(state, action);

        expect(nextState.values).toEqual({
            foo: null,
        });
    });
});
