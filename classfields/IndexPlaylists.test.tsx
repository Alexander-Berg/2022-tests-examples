import React from 'react';
import { Provider } from 'react-redux';
import { shallow } from 'enzyme';

import type { ThunkMockStore } from 'autoru-frontend/mocks/mockStore';
import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import mockStore from 'autoru-frontend/mocks/mockStore';

import type { StateIndexPlaylists } from 'auto-core/react/dataDomain/indexPlaylists/StateIndexPlaylists';
import mockIndexPlaylists from 'auto-core/react/dataDomain/indexPlaylists/mock/indexPlaylists';

import IndexPlaylists from './IndexPlaylists';

const Context = createContextProvider(contextMock);

interface State {
    indexPlaylists: StateIndexPlaylists;
}

let state: ThunkMockStore<State>;

it('рисует компонент, если есть плейлист', () => {
    state = mockStore({ indexPlaylists: mockIndexPlaylists });
    const wrapper = render(state);

    expect(wrapper.find('.IndexPlaylists__item')).toHaveLength(4);
});

it('не рисует компонент, если нет плейлиста', () => {
    state = mockStore({ indexPlaylists: [] });
    const wrapper = render(state);

    expect(wrapper.type()).toBeNull();
});

function render(store: typeof state) {
    return shallow(
        <Context>
            <Provider store={ store }>
                <IndexPlaylists/>
            </Provider>
        </Context>,
    ).dive().dive().dive();
}
