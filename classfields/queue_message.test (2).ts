import { ModelConfig, ModelState, ModelUser, ModelXiva } from '../models';

import queue_message from './queue_message';

import chat_mock from '../../mocks/state/chat.mock';
import message_mock from '../../mocks/state/message.mock';
import presets_mock from '../../mocks/state/presets.mock';
import { ActionTypes } from '../actionTypes';
import { ActionQueueMessage } from '../actions';

let initialState: ModelState;

beforeEach(() => {
    initialState = {
        inited: true,
        connection: 'connected',
        resyncing: null,
        visible: true,
        focused: true,
        debug: false,
        chat_id: null,
        gallery: null,
        config: {
            metrika: { counter_id: 'metrika_id' },
            platform: 'desktop',
            client: 'autoru',
            page_params: {},
        } as Partial<ModelConfig> as ModelConfig,
        xiva: {} as Partial<ModelXiva> as ModelXiva,
        user: {} as Partial<ModelUser> as ModelUser,
        chat_list: [],
        loaded_rooms_num: 0,
        payment_modal: {},
        loading_rooms: false,
        bunker: {
            'common/chat_preset_messages': presets_mock.value(),
        },
    };
});

describe('поле loaded_rooms_num', () => {

    beforeEach(() => {
        initialState.chat_list = [
            chat_mock.withId('chat-01').withMessages([
                message_mock.withId('message-11').withRoomId('chat-01').value(),
            ]).value(),
            chat_mock.withId('chat-02').withMessages([]).value(),
        ];
        initialState.loaded_rooms_num = 1;
    });

    it('если в чате не было сообщений обновит кол-во загруженных чатов', () => {
        const action: ActionQueueMessage = {
            type: ActionTypes.QUEUE_MESSAGE,
            payload: {
                queue_message: message_mock.withId('message-21').withRoomId('chat-02').withText('foo').value(),
            },
        };

        const result = queue_message(initialState, action);
        expect(result.loaded_rooms_num).toBe(2);
    });

    it('если в чате были сообщения не будет трогать кол-во загруженных чатов', () => {
        const action: ActionQueueMessage = {
            type: ActionTypes.QUEUE_MESSAGE,
            payload: {
                queue_message: message_mock.withId('message-12').withRoomId('chat-01').withText('foo').value(),
            },
        };

        const result = queue_message(initialState, action);
        expect(result.loaded_rooms_num).toBe(1);
    });
});
