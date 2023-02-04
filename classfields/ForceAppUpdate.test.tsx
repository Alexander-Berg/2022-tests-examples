import React from 'react';
import { shallow } from 'enzyme';
import { Provider } from 'react-redux';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import ForceAppUpdate from './ForceAppUpdate';

const store = mockStore({
    config: {
        data: {
            browser: { OSFamily: 'iOS' },
        },
    },
});
const Context = createContextProvider(contextMock);

it('правильно отправляет метрику на маунт', () => {
    shallow(
        <Context>
            <Provider store={ store }>
                <ForceAppUpdate/>
            </Provider>
        </Context>,
    ).dive().dive().dive();

    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'update-app', 'show' ]);
});

it('правильно отправляет метрику на клик', () => {
    const wrapper = shallow(
        <Context>
            <Provider store={ store }>
                <ForceAppUpdate/>
            </Provider>
        </Context>,
    ).dive().dive().dive();

    wrapper.find('.ForceAppUpdate__button').at(0).simulate('click');

    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'update-app', 'click' ]);
});
