jest.mock('react-redux', () => {
    const ActualReactRedux = jest.requireActual('react-redux');
    return {
        ...ActualReactRedux,
        useSelector: jest.fn(),
        useDispatch: jest.fn(),
    };
});
import React from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import '@testing-library/jest-dom';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import ResellerPublicProfilePromoTooltip from './ResellerPublicProfilePromoTooltip';

const INITIAL_STATE = {
    state: { isResellerPublicPromoTooltipShowed: true },
};
const Context = createContextProvider(contextMock);

it('отправит метрику на показ тултипа', () => {
    mockRedux();
    render(getComponentWithWrapper());

    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'reseller_promo_tooltip', 'show' ]);
});

describe('клик на кнопку настройки', () => {
    it('по клику отправит метрику и скроет тултип', async() => {
        mockRedux();
        render(getComponentWithWrapper());

        const settingsButton = await screen.findByText(/настройки/i);

        userEvent.click(settingsButton);

        const settingsButtonAfterClick = await screen.queryByText('/настройки/i');
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'settings_link', 'click', 'tooltip' ]);
        expect(settingsButtonAfterClick).not.toBeInTheDocument();
    });
});

describe('клик на крестик', () => {
    it('по клику отправит метрику и скроет тултип', async() => {
        mockRedux();
        render(getComponentWithWrapper());

        const closeButton = await screen.findByRole('button');

        userEvent.click(closeButton);

        const settingsButton = await screen.queryByText('/настройки/i');
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'reseller_promo_tooltip', 'close' ]);
        expect(settingsButton).not.toBeInTheDocument();
    });
});

class TestComponent extends React.PureComponent {
    anchorRef = React.createRef<HTMLSpanElement>();

    render() {
        return (
            <>
                <span ref={ this.anchorRef }>
                    anchor
                </span>
                <ResellerPublicProfilePromoTooltip
                    anchor={ this.anchorRef }
                />
            </>
        );
    }
}

function getComponentWithWrapper() {
    return (
        <Context>
            <TestComponent/>
        </Context>
    );
}

function mockRedux(state = INITIAL_STATE) {
    const store = mockStore(state);

    (useDispatch as jest.MockedFunction<typeof useDispatch>).mockReturnValue(
        (...args) => store.dispatch(...args),
    );

    (useSelector as jest.MockedFunction<typeof useSelector>).mockImplementation(
        (selector) => selector(store.getState()),
    );
}
