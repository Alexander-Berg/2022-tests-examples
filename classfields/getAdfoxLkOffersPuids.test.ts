import { cloneDeep } from 'lodash';

import type { StateConfig } from 'auto-core/react/dataDomain/config/StateConfig';
import type { StateUser } from 'auto-core/react/dataDomain/user/types';
import configMock from 'auto-core/react/dataDomain/config/mocks/config';
import userStateMock from 'auto-core/react/dataDomain/user/mocks';

import getAdfoxLkOffersPuids from './getAdfoxLkOffersPuids';

interface State {
    config: StateConfig;
    user: StateUser;
}

it('должен вернуть { puid10: "4", puid12: "private" }  для /my/all/', () => {
    const state: State = {
        config: cloneDeep(configMock),
        user: userStateMock.withAuth(true).value(),
    };

    state.config.data.pageType = 'sales';
    state.config.data.pageParams = { category: 'all' };

    expect(getAdfoxLkOffersPuids(state)).toEqual({
        puid10: '4',
        puid12: 'private',
        puid23: '',
    });
});

it('должен вернуть { puid10: "2", puid12: "private" }  для /my/trucks/', () => {
    const state: State = {
        config: cloneDeep(configMock),
        user: userStateMock.withAuth(true).value(),
    };

    state.config.data.pageType = 'sales';
    state.config.data.pageParams = { category: 'trucks' };

    expect(getAdfoxLkOffersPuids(state)).toEqual({
        puid10: '2',
        puid12: 'private',
        puid23: '',
    });
});

it('должен вернуть { puid10: "3", puid12: "private" }  для /my/moto/', () => {
    const state: State = {
        config: cloneDeep(configMock),
        user: userStateMock.withAuth(true).value(),
    };

    state.config.data.pageType = 'sales';
    state.config.data.pageParams = { category: 'moto' };

    expect(getAdfoxLkOffersPuids(state)).toEqual({
        puid10: '3',
        puid12: 'private',
        puid23: '',
    });
});

it('должен вернуть { puid10: "1", puid12: "private" }  для /my/cars/', () => {
    const state: State = {
        config: cloneDeep(configMock),
        user: userStateMock.withAuth(true).value(),
    };

    state.config.data.pageType = 'sales';
    state.config.data.pageParams = { category: 'cars' };

    expect(getAdfoxLkOffersPuids(state)).toEqual({
        puid10: '1',
        puid12: 'private',
        puid23: '',
    });
});

it('должен вернуть { puid10: "1", puid12: "private", puid23: "true" } для перекупа на /my/cars/', () => {
    const state: State = {
        config: cloneDeep(configMock),
        user: userStateMock.withAuth(true).withReseller().value(),
    };

    state.config.data.pageType = 'sales';
    state.config.data.pageParams = { category: 'cars' };

    expect(getAdfoxLkOffersPuids(state)).toEqual({
        puid10: '1',
        puid12: 'private',
        puid23: 'true',
    });
});
