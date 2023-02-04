import * as React from 'react';
import { shallow } from 'enzyme';
import { Provider } from 'react-redux';
import configureStore from 'redux-mock-store';
import thunk from 'redux-thunk';
const mockStore = configureStore([ thunk ]);

import chatMock from '../../mocks/state/chat.mock';
import userMock from '../../mocks/state/user.mock';
import { ModelChatType } from '../models';

import ChatHead from './ChatHead';
import type { OwnProps } from './ChatHead';

const defaultState = {
    config: {},
};

let defaultProps: OwnProps;

beforeEach(() => {
    defaultProps = {
        chat: chatMock.value(),
        current_user: userMock.value(),
    };
});

describe('статус пользователя', () => {
    it('покажет для обычной комнаты', () => {
        const wrapper = shallowRenderComponent(defaultProps);
        const status = wrapper.find('.ChatHead__status');

        expect(status.isEmptyRender()).toBe(false);
    });

    it('не покажет для комнаты тех саппорта', () => {
        defaultProps.chat = chatMock.withChatType(ModelChatType.ROOM_TYPE_TECH_SUPPORT).value();
        const wrapper = shallowRenderComponent(defaultProps);
        const status = wrapper.find('.ChatHead__status');

        expect(status.isEmptyRender()).toBe(true);
    });

    it('не покажет для центра уведомлений', () => {
        defaultProps.chat = chatMock.withNotificationCenter(true).value();
        const wrapper = shallowRenderComponent(defaultProps);
        const status = wrapper.find('.ChatHead__status');

        expect(status.isEmptyRender()).toBe(true);
    });
});

describe('кнопка позвонить', () => {
    it('покажет для обычной комнаты', () => {
        const wrapper = shallowRenderComponent(defaultProps);
        const phoneButton = wrapper.find('.ChatHead__phone-button');

        expect(phoneButton.isEmptyRender()).toBe(false);
    });

    it('не покажет для комнаты тех саппорта', () => {
        defaultProps.chat = chatMock.withChatType(ModelChatType.ROOM_TYPE_TECH_SUPPORT).value();
        const wrapper = shallowRenderComponent(defaultProps);
        const phoneButton = wrapper.find('.ChatHead__phone-button');

        expect(phoneButton.isEmptyRender()).toBe(true);
    });

    it('не покажет для центра уведомлений', () => {
        defaultProps.chat = chatMock.withNotificationCenter(true).value();
        const wrapper = shallowRenderComponent(defaultProps);
        const phoneButton = wrapper.find('.ChatHead__phone-button');

        expect(phoneButton.isEmptyRender()).toBe(true);
    });
});

function shallowRenderComponent(props: OwnProps, state = defaultState) {
    const store = mockStore(state);

    const wrapper = shallow(
        <Provider store={ store }>
            <ChatHead { ...props }/>
        </Provider>,
    );
    return wrapper.dive().dive();
}
