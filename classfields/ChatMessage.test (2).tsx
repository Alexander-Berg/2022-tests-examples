import * as React from 'react';
import * as _ from 'lodash';

import { shallow } from 'enzyme';
import { Provider } from 'react-redux';
import configureStore from 'redux-mock-store';
import thunk from 'redux-thunk';

import configMock from '../../../mocks/state/config.mock';

const mockStore = configureStore([ thunk ]);
import ChatMessage from './ChatMessage';
import ChatMessageLocation from './ChatMessageLocation/ChatMessageLocation';
import ExternalLinkWarningWidget from '../Widget/ExternalLinkWarningWidget/ExternalLinkWarningWidget';

const defaultState = {
    config: configMock.value(),
};

import {
    ModelChatMessageContentType,
    ModelChatMessagePropertiesType,
    ModelChatType,
} from '../../models';

const { TEXT_PLAIN } = ModelChatMessageContentType;
const { ROOM_TYPE_OFFER } = ModelChatType;

let originalWindowPostMessage: (message: any, targetOrigin: string, transfer?: Array<Transferable>) => void;

beforeEach(() => {
    originalWindowPostMessage = window.postMessage;

    window.postMessage = jest.fn();
});

afterEach(() => {
    window.postMessage = originalWindowPostMessage;
});

const defaultProps = {
    chat: {
        type: ROOM_TYPE_OFFER,
        id: '777',
        source: undefined,
        created: '',
        users: [
            {
                id: '',
                is_me: false,
                room_last_read: '01-01-2020 13:00',
            },
        ],
        me: '',
        owner: '',
        messages: [],
        has_unread: false,
        url: '',
        muted: false,
        blocked_by_me: false,
        blocked_by_someone: false,
        loading: false,
        sending: false,
        all_loaded: true,
        has_loaded: true,
        chat_only: true,
    },
    message: {
        id: '',
        room_id: '',
        author: '',
        user: {
            id: '',
            is_me: true,
        },
        created: '01-01-2020 12:00',
        payload: { content_type: TEXT_PLAIN, value: '' },
        properties: {},
    },
};


it('должен отобразить сообщение с картой, если передано сообщение, содержащее урл на карту', () => {
    const message = {
        ...defaultProps.message,
        payload: {
            value: 'https://yandex.ru/maps/?mode=whatshere&whatshere[point]=37.499173,55.755780&whatshere',
            content_type: ModelChatMessageContentType.TEXT_PLAIN,
        },
    };
    const tree = shallowRenderComponent(message);

    expect(tree.find(ChatMessageLocation)).toHaveLength(1);
});

it('не должен отобразить сообщение с картой, если сообщение, не содержит урл на карту', () => {
    const message = {
        ...defaultProps.message,
        payload: {
            value: 'Какой-то текст',
            content_type: ModelChatMessageContentType.TEXT_PLAIN,
        },
    };
    const tree = shallowRenderComponent(message);

    expect(tree.find(ChatMessageLocation)).toHaveLength(0);
});

describe('при клике на сообщение', () => {
    it('если это не сообщение о звонке, не отправит сообщение на window', () => {
        const tree = shallowRenderComponent();
        tree.simulate('click');

        expect(window.postMessage).toHaveBeenCalledTimes(0);
    });

    it('если это сообщение о звонке и это десктоп, не отправит сообщение на window', () => {
        const message = _.cloneDeep(defaultProps.message);
        message.user.is_me = false;
        message.properties = {
            type: ModelChatMessagePropertiesType.CALL_INFO,
            call_info: {
                duration: 0,
            },
        };

        const tree = shallowRenderComponent(message, undefined, defaultState);
        tree.simulate('click');

        expect(window.postMessage).toHaveBeenCalledTimes(0);
    });

    it('если это сообщение о звонке и это мобилка, отправит сообщение на window', () => {
        const message = _.cloneDeep(defaultProps.message);
        message.user.is_me = false;
        message.properties = {
            type: ModelChatMessagePropertiesType.CALL_INFO,
            call_info: {
                duration: 0,
            },
        };
        const state = {
            config: configMock.withPlatform('mobile').value(),
        };

        const tree = shallowRenderComponent(message, undefined, state);
        tree.simulate('click');

        expect(window.postMessage).toHaveBeenCalledTimes(1);
        expect(window.postMessage).toHaveBeenCalledWith({ source: 'chat', action: 'open_phone_popup' }, '*');
    });
});

describe('сообщения с ссылками', () => {
    const defaultMessage = {
        ...defaultProps.message,
        room_id: '333',
        id: '111',
        user: { ...defaultProps.message.user, is_me: false },
    };

    it('должен отобразить предупреждение если есть ссылка на внешний ресурс, скрыть его по нажатию и записать значение в ls', () => {
        const message = {
            ...defaultMessage,
            payload: {
                value: 'Я отправляю ссылку на pornhub.com',
                content_type: ModelChatMessageContentType.TEXT_PLAIN,
            },
        };
        const tree = shallowRenderComponent(message);
        const externalLinkMessage = tree.find(ExternalLinkWarningWidget);

        expect(externalLinkMessage).toHaveLength(1);

        externalLinkMessage.simulate('_message_hide');
        expect(tree.find(ExternalLinkWarningWidget)).toHaveLength(0);
    });

    it('должен отобразить обычное сообщение с кликабельными ссылками на авто.ру и яндекс', () => {
        const message = {
            ...defaultMessage,
            payload: {
                value: 'Я отправляю ссылки на auto.ru/cars/all и yandex.ru/help/me/please',
                content_type: ModelChatMessageContentType.TEXT_PLAIN,
            },
        };
        const tree = shallowRenderComponent(message);

        expect(tree.find('a')).toHaveLength(2);
    });

    it('должен отобразить предупреждение если есть ссылка на внешний ресурс и на авто.ру а после закрытия покажет кликабельную ссылку на авто.ру', () => {
        const message = {
            ...defaultMessage,
            payload: {
                value: 'Я отправляю ссылки на auto.ru/cars/all и pronhub.com',
                content_type: ModelChatMessageContentType.TEXT_PLAIN,
            },
        };
        const tree = shallowRenderComponent(message);
        const externalLinkMessage = tree.find(ExternalLinkWarningWidget);

        expect(externalLinkMessage).toHaveLength(1);

        externalLinkMessage.simulate('_message_hide');
        expect(tree.find(ExternalLinkWarningWidget)).toHaveLength(0);
        expect(tree.find('a')).toHaveLength(1);
    });
});

function shallowRenderComponent(message = defaultProps.message, chat = defaultProps.chat, state = defaultState) {
    const store = mockStore(state);

    const wrapper = shallow(
        <Provider store={ store }>
            <ChatMessage
                message={ message }
                chat={ chat }
                external_link_ls_data={ [] }
                is_last_message={ false }
                is_last_of_group
            />
        </Provider>,
    );
    return wrapper.dive().dive();
}
