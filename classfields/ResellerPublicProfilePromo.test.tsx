import React from 'react';
import { Provider, useSelector, useDispatch } from 'react-redux';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import userMock from 'auto-core/react/dataDomain/user/mocks';

import ResellerPublicProfilePromo from './ResellerPublicProfilePromo';

jest.mock('react-redux', () => {
    const ActualReactRedux = jest.requireActual('react-redux');
    return {
        ...ActualReactRedux,
        useSelector: jest.fn(),
        useDispatch: jest.fn(),
    };
});

function mockRedux(state = {}) {
    const store = mockStore(state);

    (useDispatch as jest.MockedFunction<typeof useDispatch>).mockReturnValue(
        (...args) => store.dispatch(...args),
    );

    (useSelector as jest.MockedFunction<typeof useSelector>).mockImplementation(
        (selector) => selector(store.getState()),
    );

    return store;
}

const INITIAL_STATE = {
    cookies: { 'reseller-public-profile-popup-shown': 'true' },
    user: userMock.withAuth(true).withEncryptedUserId('some_encrypted_user_id').withReseller(true).value(),
    bunker: { 'common/reseller_public_profile_onboarding': { isFeatureEnabled: true } },
};
const Context = createContextProvider(contextMock);

it('отправит метрику на показ баннера', () => {
    render(getComponentWithWrapper(mockRedux(INITIAL_STATE)));

    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'reseller_promo_banner', 'show' ]);
});

it('при клике на кнопку подробнее отправит метрику, сотрет куку и откроет попап', async() => {
    const store = mockRedux(INITIAL_STATE);
    render(getComponentWithWrapper(store));

    const detailsButton = await screen.findByText(/подробнее/i);

    userEvent.click(detailsButton);

    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'reseller_promo_banner', 'click' ]);
    expect(store.getActions()).toEqual([
        { type: 'AUTO_POPUP_RESET' },
        { type: 'COOKIES_CHANGE', payload: {} },
        {
            type: 'AUTO_POPUP_OPEN',
            payload: { data: { encryptedUserId: 'some_encrypted_user_id' }, id: 'reseller_public_profile_popup' },
        },
    ]);
});

it('при клике на крестик отправит метрику и проставит куку', async() => {
    const store = mockRedux(INITIAL_STATE);
    render(getComponentWithWrapper(store));

    const buttons = await screen.findAllByRole('button');

    userEvent.click(buttons[1]);

    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'reseller_promo_banner', 'close' ]);
    expect(store.getActions()).toEqual([
        { type: 'COOKIES_CHANGE', payload: {} },
    ]);
});

function getComponentWithWrapper(store: any) {
    return (
        <Provider store={ store }>
            <Context>
                <ResellerPublicProfilePromo/>
            </Context>
        </Provider>
    );
}
