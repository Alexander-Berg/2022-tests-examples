/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import type { StateRouter } from '@vertis/susanin-react';
import React from 'react';
import { Provider } from 'react-redux';
import { shallow } from 'enzyme';
import querystring from 'querystring';

import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import mockStore from 'autoru-frontend/mocks/mockStore';

import type { TStateState } from 'auto-core/react/dataDomain/state/TStateState';
import type { StateGeo } from 'auto-core/react/dataDomain/geo/StateGeo';
import type { StateConfig } from 'auto-core/react/dataDomain/config/StateConfig';
import configStateMock from 'auto-core/react/dataDomain/config/mock';
import geoStateMock from 'auto-core/react/dataDomain/geo/mocks/geo.mock';
import routerStateMock from 'auto-core/react/dataDomain/router/mock';

import Header from './Header';

const Context = createContextProvider({
    ...contextMock,
    link: (routeName: string, routeParams: Record<string, any>) => {
        return `${ routeName }/${ querystring.stringify(routeParams) }`;
    },
});

interface State {
    ads: { data: any };
    config: StateConfig;
    geo: StateGeo;
    router: StateRouter;
    state: TStateState;
}

it('кнопка назад на странице "о модели"', () => {
    const state = getState();
    state.router = routerStateMock.withCurrentRoute({
        name: 'card-group-about',
        params: {
            category: 'cars',
            section: 'new',
            catalog_filter: [
                { mark: 'MAZDA', model: '3', generation: '21514240', configuration: '21514294' },
            ],
        },
    }).value();

    const wrapper = shallow(
        <Context>
            <Provider store={ mockStore(state) }>
                <Header/>
            </Provider>
        </Context>,
    ).dive().dive().dive();

    expect(wrapper.find('.Header2__return-link').props()).toHaveProperty('url', 'card-group/category=cars&section=new&catalog_filter=');
});

function getState(): State {
    return {
        ads: { data: { } },
        config: configStateMock.value(),
        geo: geoStateMock,
        state: { authModal: {} },
        router: routerStateMock.value(),
    };
}
