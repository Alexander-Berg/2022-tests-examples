jest.mock('auto-core/react/dataDomain/cookies/actions/set', () => {
    return jest.fn(() => () => ({}));
});

import React from 'react';
import { Provider } from 'react-redux';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';
import applyUseSelectorMock from 'autoru-frontend/jest/unit/applyUseSelectorMock';

import setCookie from 'auto-core/react/dataDomain/cookies/actions/set';

import MarketplacePromoPlate, { COOKIE_NAME } from './MarketplacePromoPlate';

const INITIAL_STATE = {
    cookies: {},
};

it('при клике на крестик проставит куку', async() => {
    const Context = createContextProvider(contextMock);

    const store = mockStore(INITIAL_STATE);

    const { mockUseDispatch, mockUseSelector } = applyUseSelectorMock();
    mockUseSelector(INITIAL_STATE);
    mockUseDispatch(store);

    render(
        <Provider store={ store }>
            <Context>
                <MarketplacePromoPlate/>
            </Context>
        </Provider>,
    );

    const closeButton = await screen.getByLabelText('close');

    userEvent.click(closeButton);

    expect(setCookie).toHaveBeenCalledTimes(1);
    expect(setCookie).toHaveBeenNthCalledWith(1, COOKIE_NAME, 'true', { expires: 14 });
});
