import { PAGE_LOADING_SUCCESS } from 'auto-core/react/actionTypes';
import { SET_VIN_CHECK_INPUT_VALUE } from 'auto-core/react/dataDomain/vinCheckInput/types';

import reducer from './reducer';
import type { PageLoadingSuccessActionPayload } from './types';

it('должен вернуть vin в value', () => {
    const payload = {
        router: {
            params: {
                vin_or_license_plate: 'E499EP799',
            } as Partial<PageLoadingSuccessActionPayload['router']>,
        },
    } as PageLoadingSuccessActionPayload;

    const state = reducer(undefined, { type: PAGE_LOADING_SUCCESS, payload });
    expect(state).toEqual({ value: 'E499EP799' });
});

it('должен удалить value на PAGE_LOADING_SUCCESS, если это не VIN', () => {
    const payload = {
        router: {
            params: {} as Partial<PageLoadingSuccessActionPayload['router']>,
        },
    } as PageLoadingSuccessActionPayload;

    const state = reducer({ value: '123' }, { type: PAGE_LOADING_SUCCESS, payload });
    expect(state).toEqual({ value: '' });
});

it('должен вернуть госномер в value', () => {
    const payload = {
        router: {
            params: {
                vin_or_license_plate: 'XTA210510P1394951',
            } as Partial<PageLoadingSuccessActionPayload['router']>,
        },
    } as PageLoadingSuccessActionPayload;

    const state = reducer(undefined, { type: PAGE_LOADING_SUCCESS, payload });
    expect(state).toEqual({ value: 'XTA210510P1394951' });
});

it('должен вернуть пустую строку в value, если в pageParams не вин и не госномер', () => {
    const payload = {
        router: {
            params: {
                vin_or_license_plate: 'ооо моя оборона',
            } as Partial<PageLoadingSuccessActionPayload['router']>,
        },
    } as PageLoadingSuccessActionPayload;

    const state = reducer(undefined, { type: PAGE_LOADING_SUCCESS, payload });
    expect(state).toEqual({ value: '' });
});

it('должен вернуть пустую строку в value, если нет vin_or_license_plate', () => {
    const payload = {
        router: {
            params: {} as Partial<PageLoadingSuccessActionPayload['router']>,
        },
    } as PageLoadingSuccessActionPayload;

    const state = reducer(undefined, { type: PAGE_LOADING_SUCCESS, payload });
    expect(state).toEqual({ value: '' });
});

it('должен обновить value', () => {
    const state = reducer(undefined, { type: SET_VIN_CHECK_INPUT_VALUE, payload: { value: '12' } });
    expect(state).toEqual({ value: '12' });
});
