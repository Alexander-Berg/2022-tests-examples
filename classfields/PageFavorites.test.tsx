import React from 'react';
import { shallow } from 'enzyme';
import { Provider } from 'react-redux';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import configStateMock from 'auto-core/react/dataDomain/config/mock';
import favoritesStateMock from 'auto-core/react/dataDomain/favorites/mocks';
import { subscriptionStateMock } from 'auto-core/react/dataDomain/subscriptions/mocks';
import userStateMock from 'auto-core/react/dataDomain/user/mocks';

import type { MobileAppState } from 'www-mobile/react/MobileAppState';

import PageFavorites, { TABS } from './PageFavorites';

let initialState: Partial<MobileAppState>;

beforeEach(() => {
    initialState = {
        config: configStateMock.withPageParams({ category: 'cars' }).value(),
        favorites: favoritesStateMock.value(),
        subscriptions: subscriptionStateMock.value(),
        user: userStateMock.value(),
    };
});

describe('при переключении табов', () => {
    it('поменяет урл в бро', () => {
        const page = shallowRenderComponent({ initialState });
        const tabs = page.find('.PageFavorites__tabs RadioGroup');

        tabs.simulate('change', TABS.SEARCHES);

        expect(contextMock.replaceState).toHaveBeenCalledTimes(1);
        expect(contextMock.replaceState).toHaveBeenCalledWith('link/searches/?', { loadData: true });

        tabs.simulate('change', TABS.OFFERS);

        expect(contextMock.replaceState).toHaveBeenCalledTimes(2);
        expect(contextMock.replaceState).toHaveBeenLastCalledWith('link/like/?', { loadData: true });
    });
});

function shallowRenderComponent({ initialState }: { initialState: Partial<MobileAppState> }) {
    const ContextProvider = createContextProvider(contextMock);
    const store = mockStore(initialState);

    const page = shallow(
        <ContextProvider>
            <Provider store={ store }>
                <PageFavorites/>
            </Provider>
        </ContextProvider>,
    );

    return page.dive().dive().dive();
}
