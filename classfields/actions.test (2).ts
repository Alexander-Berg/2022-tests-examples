jest.mock('./lib/metrika', () => {
    return {
        reach_goal: jest.fn(),
        send_page_event: jest.fn(),
    };
});
jest.mock('./lib/request');

import { Store } from 'redux';
import _ from 'lodash';
import configureStore, { MockStore } from 'redux-mock-store';
import thunk from 'redux-thunk';

import chat_mock from '../mocks/state/chat.mock';

import presets_mock from '../mocks/state/presets.mock';

import { ActionTypes } from './actionTypes';
import {
    IModelChat,
    IModelChatMessage,
    ModelChatType,
    IModelConfig,
    IModelPresets,
    IModelState,
    IModelUser,
    IModelXiva,
} from './models';
import { reach_goal, send_page_event } from './lib/metrika';
import {
    action_load_more_chat_rooms,
    action_post_message,
    action_resync,
    action_show_phones,
    IActionBootstrapped,
    IActionOpenChat,
    IActionResynced,
} from './actions';

import request from './lib/request';

import { createAppStore } from './store';

const request_mock = request as jest.MockedFunction<typeof request>;
const request_promise_success = Promise.resolve();
request_mock.mockReturnValue(request_promise_success);

const METRIKA_ID = 'metrika_id';

const defaultState: IModelState = {
    inited: true,
    connection: 'connected',
    resyncing: null,
    visible: true,
    focused: true,
    debug: false,
    chat_id: null,
    gallery: null,
    config: ({
        metrika: { counter_id: METRIKA_ID },
        platform: 'desktop',
        page_params: {},
    } as Partial<IModelConfig>) as IModelConfig,
    presets: presets_mock.value(),
    xiva: ({} as Partial<IModelXiva>) as IModelXiva,
    user: ({} as Partial<IModelUser>) as IModelUser,
    chat_list: [],
    page: 0,
    totalPages: 2,
    loading_rooms: false,
};

describe('action_post_message', () => {
    let testStore: Store;
    beforeEach(() => {
        testStore = createAppStore();

        const actionBootstrapped: IActionBootstrapped = {
            type: ActionTypes.BOOTSTRAPPED,
            payload: {
                config: ({
                    metrika: { counter_id: METRIKA_ID },
                    platform: 'desktop',
                } as Partial<IModelConfig>) as IModelConfig,
                presets: ({} as Partial<IModelPresets>) as IModelPresets,
                xiva: ({} as Partial<IModelXiva>) as IModelXiva,
                user: ({} as Partial<IModelUser>) as IModelUser,
            },
        };
        testStore.dispatch(actionBootstrapped);

        const actionResynced: IActionResynced = {
            type: ActionTypes.RESYNCED,
            payload: {
                chat_list: [({ id: 'foo', messages: [], users: [] } as Partial<IModelChat>) as IModelChat],
                page: 0,
                totalPages: 2,
            },
        };
        testStore.dispatch(actionResynced);
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        request_mock.mockImplementation(async (params: any) => {
            if (params.path === 'chat/message_add') {
                return Promise.resolve({ message: {} });
            }

            return Promise.resolve('foo');
        });
    });

    describe('метрики на отправку сообщений', () => {
        it('должен отправить цель "CHAT_SEND_MESSAGE_DESKTOP" после отправки сообщения из десктопа', () => {
            const actionOpenChat: IActionOpenChat = {
                type: ActionTypes.OPEN_CHAT,
                payload: {
                    chat_id: 'foo',
                },
            };
            testStore.dispatch(actionOpenChat);

            // не понимаю почему, но dispatch не возвращает промис :(
            // @ts-expect-error incorrect redux-thunk types
            testStore.dispatch(action_post_message('foo', 'goal from desktop'));

            return new Promise<void>((resolve) => {
                setTimeout(() => {
                    expect(reach_goal).toHaveBeenCalledWith(METRIKA_ID, 'CHAT_SEND_MESSAGE_DESKTOP');
                    resolve();
                }, 100);
            });
        });

        it('должен отправить параметр визита "chat_send_message" после отправки сообщения', () => {
            const actionOpenChat: IActionOpenChat = {
                type: ActionTypes.OPEN_CHAT,
                payload: {
                    chat_id: 'foo',
                },
            };
            testStore.dispatch(actionOpenChat);

            // не понимаю почему, но dispatch не возвращает промис :(
            // @ts-expect-error incorrect redux-thunk types
            testStore.dispatch(action_post_message('foo', 'event from desktop'));

            return new Promise<void>((resolve) => {
                setTimeout(() => {
                    expect(send_page_event).toHaveBeenCalledWith(METRIKA_ID, ['chat_send_message']);
                    resolve();
                }, 100);
            });
        });
    });

    describe('метрики на отправку первого сообщения', () => {
        it('должен отправить цели после отправки первого сообщения в чат', () => {
            const actionOpenChat: IActionOpenChat = {
                type: ActionTypes.OPEN_CHAT,
                payload: {
                    chat_id: 'foo',
                    metrics: {
                        startDialog: [{ name: 'startGoal1' }, { name: 'startGoal2', params: { start2: 'goal2' } }],
                        startCall: [{ name: 'startGoal3' }, { name: 'startGoal4', params: { start2: 'goal2' } }],
                    },
                },
            };
            testStore.dispatch(actionOpenChat);

            // не понимаю почему, но dispatch не возвращает промис :(
            // @ts-expect-error incorrect redux-thunk types
            testStore.dispatch(action_post_message('foo', 'hello'));

            return new Promise<void>((resolve) => {
                setTimeout(() => {
                    expect(reach_goal).toHaveBeenCalledWith(METRIKA_ID, 'startGoal1', undefined);
                    expect(reach_goal).toHaveBeenCalledWith(METRIKA_ID, 'startGoal2', { start2: 'goal2' });
                    expect(reach_goal).not.toHaveBeenCalledWith(METRIKA_ID, 'startGoal3', undefined);
                    expect(reach_goal).not.toHaveBeenCalledWith(METRIKA_ID, 'startGoal4', { start2: 'goal2' });
                    resolve();
                }, 100);
            });
        });

        it('не должен отправить цели, если это не первое сообщение в чате', () => {
            const actionResynced: IActionResynced = {
                type: ActionTypes.RESYNCED,
                payload: {
                    chat_list: [
                        ({
                            id: 'foo',
                            messages: [({ id: '1' } as Partial<IModelChatMessage>) as IModelChatMessage],
                            users: [],
                        } as Partial<IModelChat>) as IModelChat,
                    ],
                    page: 0,
                    totalPages: 2,
                },
            };
            testStore.dispatch(actionResynced);

            const actionOpenChat: IActionOpenChat = {
                type: ActionTypes.OPEN_CHAT,
                payload: {
                    chat_id: 'foo',
                    metrics: {
                        startDialog: [{ name: 'startGoal1' }, { name: 'startGoal2', params: { start2: 'goal2' } }],
                        startCall: [{ name: 'startGoal3' }, { name: 'startGoal4', params: { start2: 'goal2' } }],
                    },
                },
            };
            testStore.dispatch(actionOpenChat);

            // не понимаю почему, но dispatch не возвращает промис :(
            // @ts-expect-error incorrect redux-thunk types
            testStore.dispatch(action_post_message('foo', 'hello'));

            return new Promise<void>((resolve) => {
                setTimeout(() => {
                    expect(reach_goal).not.toHaveBeenCalledWith(METRIKA_ID, 'startGoal1', undefined);
                    expect(reach_goal).not.toHaveBeenCalledWith(METRIKA_ID, 'startGoal2', { start2: 'goal2' });
                    expect(reach_goal).not.toHaveBeenCalledWith(METRIKA_ID, 'startGoal3', undefined);
                    expect(reach_goal).not.toHaveBeenCalledWith(METRIKA_ID, 'startGoal4', { start2: 'goal2' });
                    resolve();
                }, 100);
            });
        });
    });

    describe('метрики на кнопку звонка', () => {
        it('должен отправить цели при нажатии на кнопку звонка в чате', () => {
            const actionOpenChat: IActionOpenChat = {
                type: ActionTypes.OPEN_CHAT,
                payload: {
                    chat_id: 'foo',
                    metrics: {
                        startDialog: [{ name: 'startGoal1' }, { name: 'startGoal2', params: { start2: 'goal2' } }],
                        startCall: [{ name: 'startGoal3' }, { name: 'startGoal4', params: { start2: 'goal2' } }],
                    },
                },
            };

            testStore.dispatch(actionOpenChat);
            // @ts-expect-error incorrect redux-thunk types
            testStore.dispatch(action_show_phones());

            return new Promise<void>((resolve) => {
                setTimeout(() => {
                    expect(reach_goal).not.toHaveBeenCalledWith(METRIKA_ID, 'startGoal1', undefined);
                    expect(reach_goal).not.toHaveBeenCalledWith(METRIKA_ID, 'startGoal2', { start2: 'goal2' });
                    expect(reach_goal).toHaveBeenCalledWith(METRIKA_ID, 'startGoal3', undefined);
                    expect(reach_goal).toHaveBeenCalledWith(METRIKA_ID, 'startGoal4', { start2: 'goal2' });
                    resolve();
                }, 100);
            });
        });
    });
});

describe('action_load_more_chat_rooms', () => {
    const mockStore = configureStore([thunk]);
    let store: typeof MockStore;
    const request_result = [chat_mock.withId('id__04').value(), chat_mock.withId('id__05').value()];

    beforeEach(() => {
        const initialState = {
            ...defaultState,
            chat_list: [
                chat_mock.withId('id__01').value(),
                chat_mock.withId('id__02').value(),
                chat_mock.withId('id__03').value(),
                chat_mock.withId('id__04').value(),
                chat_mock.withId('id__05').value(),
            ],
            loaded_rooms_num: 3,
        };

        request_mock.mockReturnValueOnce(Promise.resolve(request_result));

        store = mockStore(initialState);
    });

    it('создаст экшен LOAD_MORE_CHAT_ROOMS', () => {
        store.dispatch(action_load_more_chat_rooms());

        const expectedAction = { type: ActionTypes.LOAD_MORE_CHAT_ROOMS };
        expect(store.getActions()[0]).toEqual(expectedAction);
    });

    it('вызовет ресурс с правильными параметрами', () => {
        store.dispatch(action_load_more_chat_rooms());

        expect(request_mock).toHaveBeenCalledTimes(1);
        expect(request_mock.mock.calls[0]).toMatchSnapshot();
    });

    it('создаст экшен LOADED_MORE_CHAT_ROOMS после получения ответа', () => {
        return store.dispatch(action_load_more_chat_rooms()).then(() => {
            const expectedAction = {
                type: ActionTypes.LOADED_MORE_CHAT_ROOMS,
                payload: {
                    chat_list: request_result,
                    loaded_rooms_num: 5,
                },
            };
            expect(store.getActions()[1]).toEqual(expectedAction);
        });
    });
});

describe('action_resync', () => {
    const mockStore = configureStore([thunk]);
    let store: typeof MockStore;
    const rooms_request_result = _.range(15).map((id) => chat_mock.withId(`id__${id}`).value());

    beforeEach(() => {
        const initialState = _.cloneDeep(defaultState);

        store = mockStore(initialState);
    });

    it('запросит все комнаты сразу', () => {
        request_mock.mockReturnValueOnce(Promise.resolve(rooms_request_result));

        store.dispatch(action_resync());

        expect(request_mock).toHaveBeenCalledTimes(1);
        expect(request_mock).toHaveBeenLastCalledWith({
            path: 'chat/chat_list',
        });
    });

    it('положит в стор полный массив комнат', () => {
        request_mock.mockReturnValueOnce(Promise.resolve(rooms_request_result));

        return store.dispatch(action_resync()).then(() => {
            const expectedAction = {
                type: ActionTypes.RESYNCED,
                payload: {
                    chat_list: rooms_request_result,
                    loaded_rooms_num: 15,
                },
            };

            expect(store.getActions()[1]).toEqual(expectedAction);
        });
    });

    it('не будет добавлять в стор комнаты без сообщений, если это не тех саппорт конечно', () => {
        const tech_support_room = chat_mock
            .withId('id__tech')
            .withMessages([])
            .withChatType(ModelChatType.ROOM_TYPE_TECH_SUPPORT)
            .value();
        const rooms_with_messages = _.range(14).map((id) => chat_mock.withId(`id__${id}`).value());
        const rooms_without_messages = _.range(5).map((id) => chat_mock.withId(`id__${id}`).withMessages([]).value());

        const rooms_request_result = [...rooms_without_messages, tech_support_room, ...rooms_with_messages];
        request_mock.mockReturnValueOnce(Promise.resolve(rooms_request_result));

        return store.dispatch(action_resync()).then(() => {
            const expectedAction = {
                type: ActionTypes.RESYNCED,
                payload: {
                    chat_list: [tech_support_room, ...rooms_with_messages],
                    loaded_rooms_num: 15,
                },
            };

            expect(store.getActions()[1]).toEqual(expectedAction);
        });
    });
});
