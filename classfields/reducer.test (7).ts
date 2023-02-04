import { PAGE_LOADING_SUCCESS } from 'auto-core/react/actionTypes';

import type { CookiesChangeAction } from './types';
import reducer from './reducer';
import { COOKIES_CHANGE } from './types';

it('should return the initial state', () => {
    expect(reducer(undefined, { type: PAGE_LOADING_SUCCESS, payload: {} })).toEqual({});
});

it('should handle COOKIES_CHANGE', () => {
    const newCookies = { key: 'value' };
    const state = { key1: 'value1', key2: 'value2' };
    const action: CookiesChangeAction = {
        type: COOKIES_CHANGE,
        payload: newCookies,
    };

    expect(reducer(state, action)).toEqual(newCookies);
});
