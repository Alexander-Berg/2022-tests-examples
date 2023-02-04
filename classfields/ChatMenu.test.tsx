import * as React from 'react';
import { shallow } from 'enzyme';
import { Provider } from 'react-redux';
import configureStore from 'redux-mock-store';
import thunk from 'redux-thunk';
const mockStore = configureStore([ thunk ]);

import chatMock from '../../mocks/state/chat.mock';
import { ModelChatType } from '../models';

import type { OwnProps } from './ChatMenu';
import ChatMenu from './ChatMenu';

const defaultState = {
    config: {},
};

let defaultProps: OwnProps;

beforeEach(() => {
    defaultProps = {
        chat: chatMock.value(),
        on_request_hide: jest.fn(),
    };
});

describe('правильно формирует список элементов меню', () => {
    it('для обычной комнаты', () => {
        const wrapper = shallowRenderComponent(defaultProps);
        const items = wrapper.find('.ChatMenu__item').map(item => item.text());

        expect(items).toEqual([
            'Отключить уведомления',
            'Заблокировать собеседника',
            'Удалить диалог',
        ]);
    });

    it('для комнаты тех саппорта', () => {
        defaultProps.chat = chatMock.withChatType(ModelChatType.ROOM_TYPE_TECH_SUPPORT).value();
        const wrapper = shallowRenderComponent(defaultProps);
        const items = wrapper.find('.ChatMenu__item').map(item => item.text());

        expect(items).toEqual([
            'Отключить уведомления',
        ]);
    });

    it('для центра уведомлений', () => {
        defaultProps.chat = chatMock.withNotificationCenter(true).value();
        const wrapper = shallowRenderComponent(defaultProps);
        const items = wrapper.find('.ChatMenu__item').map(item => item.text());

        expect(items).toEqual([
            'Отключить уведомления',
        ]);
    });
});

function shallowRenderComponent(props: OwnProps, state = defaultState) {
    const store = mockStore(state);

    const wrapper = shallow(
        <Provider store={ store }>
            <ChatMenu { ...props }/>
        </Provider>,
    );
    return wrapper.dive().dive();
}
