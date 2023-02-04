import React from 'react';
import { shallow } from 'enzyme';
import { Provider } from 'react-redux';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import configStateMock from 'auto-core/react/dataDomain/config/mock';

import panoramaExteriorMock from 'auto-core/models/panoramaExterior/mocks';

import type { AppState } from '../../AppState';

import PanoramaAdmin from './PanoramaAdmin';

let initialState: AppState;

beforeEach(() => {
    initialState = {
        config: configStateMock.value(),
        panorama: {
            type: 'exterior',
            data: panoramaExteriorMock.value(),
        },
    };

    contextMock.logVasEvent.mockClear();
});

it('добавит к панораме флаг владельца если он есть в урле', () => {
    initialState.config = configStateMock.withPageParams({ can_edit: 'true' }).value();
    const page = shallowRenderComponent({ initialState });
    const panorama = page.find('Connect(PanoramaExterior)');

    expect(panorama.prop('isOwner')).toBe(true);
});

it('не добавит к панораме флаг владельца если его нет в урле', () => {
    const page = shallowRenderComponent({ initialState });
    const panorama = page.find('Connect(PanoramaExterior)');

    expect(panorama.prop('isOwner')).toBe(false);
});

function shallowRenderComponent({ initialState }: { initialState: AppState }) {
    const ContextProvider = createContextProvider(contextMock);
    const store = mockStore(initialState);

    const page = shallow(
        <ContextProvider>
            <Provider store={ store }>
                <PanoramaAdmin/>
            </Provider>
        </ContextProvider>,
    );

    return page.dive().dive().dive();
}
