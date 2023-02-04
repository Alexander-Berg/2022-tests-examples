import { renderHook } from '@testing-library/react-hooks';
import { useSelector, useDispatch } from 'react-redux';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import type { StateCookies } from 'auto-core/react/dataDomain/cookies/types';
import type { StateBunker } from 'auto-core/react/dataDomain/bunker/StateBunker';
import type { StateUser } from 'auto-core/react/dataDomain/user/types';
import userMock from 'auto-core/react/dataDomain/user/mocks';

import type TContext from 'auto-core/types/TContext';

import useResellerPublicProfilePromo, { COOKIE_NAME } from './useResellerPublicProfilePromo';

jest.mock('react-redux', () => {
    const ActualReactRedux = jest.requireActual('react-redux');
    return {
        ...ActualReactRedux,
        useSelector: jest.fn(),
        useDispatch: jest.fn(),
    };
});

type State = {
    cookies: StateCookies;
    bunker: StateBunker;
    user: StateUser;
}

const INITIAL_STATE = {
    cookies: { 'reseller-public-profile-popup-shown': 'true' },
    user: userMock.withAuth(true).withEncryptedUserId('some_encrypted_user_id').withReseller(true).value(),
    bunker: { 'common/reseller_public_profile_onboarding': { isFeatureEnabled: true } },
};

describe('shouldShowBlock', () => {
    it('true, если фича включена, юзер авторизован перекупом, кука не выставлена, есть encrypted_user_id и нет allow_user_offers_show', () => {
        mockRedux();
        const { result } = render();

        expect(result.current.shouldShowBlock).toBe(true);
    });

    it('false, если фича выключена', () => {
        const state = {
            ...INITIAL_STATE,
            bunker: {},
        };
        mockRedux(state);
        const { result } = render();

        expect(result.current.shouldShowBlock).toBe(false);
    });

    it('false, если юзер не перекуп', () => {
        const state = {
            ...INITIAL_STATE,
            user: userMock.withAuth(true).withEncryptedUserId('some_encrypted_user_id').value(),
        };
        mockRedux(state);
        const { result } = render();

        expect(result.current.shouldShowBlock).toBe(false);
    });

    it('false, если выставлена кука', () => {
        const state = {
            ...INITIAL_STATE,
            cookies: { [ COOKIE_NAME ]: 'true' },
        };
        mockRedux(state);
        const { result } = render();

        expect(result.current.shouldShowBlock).toBe(false);
    });

    it('false, если нет encrypted_user_id', () => {
        const state = {
            ...INITIAL_STATE,
            user: userMock.withAuth(true).withReseller(true).value(),
        };
        mockRedux(state);
        const { result } = render();

        expect(result.current.shouldShowBlock).toBe(false);
    });

    it('false, если уже есть allow_user_offers_show', () => {
        const state = {
            ...INITIAL_STATE,
            user: userMock.withAuth(true).withEncryptedUserId('some_encrypted_user_id').withReseller(true).withAllowOffersShow(true).value(),
        };
        mockRedux(state);
        const { result } = render();

        expect(result.current.shouldShowBlock).toBe(false);
    });
});

function render() {
    return renderHook(() => useResellerPublicProfilePromo(contextMock as unknown as TContext));
}

function mockRedux(state: State = INITIAL_STATE) {
    const store = mockStore(state);

    (useDispatch as jest.MockedFunction<typeof useDispatch>).mockReturnValue(
        (...args) => store.dispatch(...args),
    );

    (useSelector as jest.MockedFunction<typeof useSelector>).mockImplementation(
        (selector) => selector(store.getState()),
    );
}
