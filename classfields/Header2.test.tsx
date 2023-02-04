jest.mock('auto-core/react/components/common/Ad/Ad', () => () => null);
jest.mock('auto-core/react/components/mobile/HeaderNavMenu/HeaderNavMenu', () => () => null);

import React from 'react';
import { render } from '@testing-library/react';
import type { RouteInfo } from '@vertis/susanin-react/build/redux/StateRouter';
import userEvent from '@testing-library/user-event';

import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';

import configMock from 'auto-core/react/dataDomain/config/mock';
import userMock from 'auto-core/react/dataDomain/user/mocks';

import Header2 from './Header2';

const ContextProvider = createContextProvider(contextMock);

it('должен закрыть вкладку, если есть window.opener', () => {
    const _close = window.close;
    window.opener = {};
    window.close = jest.fn();
    renderComponent();
    const returnButton = document.querySelector('.Header2__return-link') as Element;
    userEvent.click(returnButton);
    expect(window.close).toHaveBeenCalled();
    window.opener = null;
    window.close = _close;
});

it('должен перейти на предыдущую страницу, если нет window.opener', () => {
    const _back = window.history.back;
    window.opener = null;
    window.history.back = jest.fn();
    renderComponent();
    const returnButton = document.querySelector('.Header2__return-link') as Element;
    userEvent.click(returnButton);
    expect(window.history.back).toHaveBeenCalled();
    window.history.back = _back;
});

class Header2WithBackButton extends Header2 {
    getButtonsInfo() {
        return {
            buttons: [],
            returnButton: {
                back: true,
            },
        };
    }
}

function renderComponent() {
    return render(
        <ContextProvider>
            <Header2WithBackButton
                config={ configMock.value().data }
                pageCategory="cars"
                currentRoute={{ name: 'card' } as RouteInfo}
                previousRoute={{ name: 'listing' } as RouteInfo}
                listingSearchID="123"
                renderStickyTopBanner={ false }
                stickyTopBannerTimeout={ 1000 }
                user={ userMock.value().data }
                setHeaderHeight={ jest.fn() }
                toggleNavMenu={ jest.fn() }
            />
        </ContextProvider>,
    );
}
