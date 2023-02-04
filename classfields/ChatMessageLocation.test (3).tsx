import React from 'react';

import { shallow } from 'enzyme';

import { Provider } from 'react-redux';

import ChatMessageLocation from './ChatMessageLocation';

import configureStore from 'redux-mock-store';
import thunk from 'redux-thunk';
const mockStore = configureStore([ thunk ]);

const defaultState = {
    config: {},
};

it('должен построить правильный урл для статической карты ', () => {
    const store = mockStore(defaultState);

    const tree = shallow(
        <Provider store={ store }>
            <ChatMessageLocation
                lat={ 59.02 }
                lng={ 54.65 }
                i_am_sender={ false }
            />
        </Provider>,
    ).dive().dive();

    const expectedUrl = 'https://static-maps.yandex.ru/1.x/?lang=Ru-ru&l=map&ll=54.65,59.02&pt=54.65,59.02,comma&z=16';
    expect(tree.find('img').prop('src')).toBe(expectedUrl);
});
