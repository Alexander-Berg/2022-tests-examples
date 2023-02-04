import React from 'react';
import userEvent from '@testing-library/user-event';
import { render } from '@testing-library/react';
import { Provider } from 'react-redux';

import '@testing-library/jest-dom';
import applyUseSelectorMock from 'autoru-frontend/jest/unit/applyUseSelectorMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import type { StateAddOfferNavigateModal } from 'auto-core/react/dataDomain/addOfferNavigate/types';
import type { StateConfig } from 'auto-core/react/dataDomain/config/StateConfig';
import type { StateCookies } from 'auto-core/react/dataDomain/cookies/types';
import configStateMock from 'auto-core/react/dataDomain/config/mock';

import AddOfferNavigateModal from './AddOfferNavigateModal';

interface AppState {
    config: StateConfig;
    addOfferNavigateModal: StateAddOfferNavigateModal;
    cookie: StateCookies;
}

const INITIAL_STORE: Partial<AppState> = {
    addOfferNavigateModal: {
        hasAnimation: true,
        isVisible: true,
        placeToCall: {},
    },
    config: configStateMock.value(),
    cookie: {},
};

describe('отсылаем метрику', () => {

    it('при показе модала', async() => {
        await renderComponent(INITIAL_STORE);

        expect(contextMock.metrika.sendParams).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.sendParams).toHaveBeenCalledWith([ 'fromWebToApp', 'app-install-banner', 'add_offer_bottomsheet', 'show' ]);
    });

    it('при закрытии модала', async() => {
        const { getByRole } = await renderComponent(INITIAL_STORE);

        const popup = getByRole('dialog');
        const closer = popup.getElementsByClassName('Modal__closer')[0];
        userEvent.click(closer);
        expect(contextMock.metrika.sendParams).toHaveBeenLastCalledWith([ 'fromWebToApp', 'app-install-banner', 'add_offer_bottomsheet', 'close' ]);
    });
    it('при переходе в апп', async() => {
        const { getByText } = await renderComponent(INITIAL_STORE);

        const btn = getByText('Открыть');
        userEvent.click(btn);

        const url = (window.location.assign as jest.Mock).mock.calls[0][0];
        expect(url.startsWith('https://sb76.adj.st/add?adjust_deeplink=autoru%3A%2F%2Fapp%2Fadd')).toBe(true);

        expect(contextMock.metrika.sendParams).toHaveBeenCalledWith([ 'fromWebToApp', 'app-install-banner', 'add_offer_bottomsheet', 'click_app' ]);
        expect(contextMock.metrika.sendParams).toHaveBeenLastCalledWith([ 'fromWebToApp', 'app-install-banner', 'add_offer_bottomsheet', 'close' ]);

    });
    it('при переходе в поффер', async() => {
        const { getByText } = await renderComponent(INITIAL_STORE);

        const btn = getByText('Продолжить');
        userEvent.click(btn);

        expect(window.location.assign).toHaveBeenCalledWith('linkDesktop/form/?form_type=add&category=cars&section=used');

        expect(contextMock.metrika.sendParams).toHaveBeenCalledWith([ 'fromWebToApp', 'app-install-banner', 'add_offer_bottomsheet', 'click_browser' ]);
        expect(contextMock.metrika.sendParams).toHaveBeenLastCalledWith([ 'fromWebToApp', 'app-install-banner', 'add_offer_bottomsheet', 'close' ]);

    });
});

it('если не пришла категория из pageParams, ставим дефлотную cars', async() => {
    const state = {
        ...INITIAL_STORE,
        config: configStateMock.withPageParams({}).value(),
    } as unknown as AppState;

    const { getByText } = await renderComponent(state);

    const btn = getByText('Продолжить');
    userEvent.click(btn);

    expect(window.location.assign).toHaveBeenCalledWith('linkDesktop/form/?form_type=add&category=cars&section=used');
});

async function renderComponent(state: Partial<AppState>) {
    const ContextProvider = createContextProvider(contextMock);
    const store = mockStore(state);
    const { mockUseDispatch, mockUseSelector } = applyUseSelectorMock();

    mockUseSelector(state);
    mockUseDispatch(store);

    return await render(
        <ContextProvider>
            <Provider store={ store }>
                <AddOfferNavigateModal/>
            </Provider>
        </ContextProvider>,
    );
}
