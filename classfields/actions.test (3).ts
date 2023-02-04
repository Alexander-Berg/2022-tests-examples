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

import { ActionTypes } from './actionTypes';
import {
    ModelChatImplementation,
    ModelChatMessage,
    ModelChatType,
    ModelClientType,
    ModelConfig,
    ModelState,
    ModelUser,
    ModelXiva,
} from './models';
import { reach_goal, send_page_event } from './lib/metrika';
import {
    action_load_more_chat_rooms,
    action_open_chat_for_offer,
    action_post_message,
    action_resync,
    action_show_phones,
    ActionBootstrapped,
    ActionOpenChat,
    ActionResynced,
} from './actions';

import request from './lib/request';
import chat_mock from '../mocks/state/chat.mock';
import presets_mock from '../mocks/state/presets.mock';

import { createAppStore } from './store';

const request_mock = request as jest.MockedFunction<typeof request>;
const request_promise_success = Promise.resolve();
request_mock.mockReturnValue(request_promise_success);

const METRIKA_ID = 'metrika_id';

const defaultState: ModelState = {
    inited: true,
    connection: 'connected',
    resyncing: null,
    visible: true,
    focused: true,
    debug: false,
    chat_id: null,
    gallery: null,
    config: {
        metrika: { counter_id: METRIKA_ID },
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

describe('action_post_message', () => {
    let testStore: Store;
    beforeEach(() => {
        testStore = createAppStore();

        const actionBootstrapped: ActionBootstrapped = {
            type: ActionTypes.BOOTSTRAPPED,
            payload: {
                config: {
                    metrika: { counter_id: METRIKA_ID },
                    platform: 'desktop',
                } as Partial<ModelConfig> as ModelConfig,
                xiva: {} as Partial<ModelXiva> as ModelXiva,
                user: {} as Partial<ModelUser> as ModelUser,
                bunker: {},
            },
        };
        testStore.dispatch(actionBootstrapped);

        const actionResynced: ActionResynced = {
            type: ActionTypes.RESYNCED,
            payload: {
                chat_list: [
                    { id: 'foo', messages: [], users: [] } as Partial<ModelChatImplementation> as ModelChatImplementation,
                ],
                loaded_rooms_num: 1,
            },
        };
        testStore.dispatch(actionResynced);
        request_mock.mockImplementation(async(params: any) => {
            if (params.path === 'chat/message_add') {
                return Promise.resolve({ message: {} });
            }

            return Promise.resolve('foo');
        });
    });

    describe('метрики на отправку сообщений', () => {
        it('должен отправить цель "CHAT_SEND_MESSAGE_DESKTOP" после отправки сообщения из десктопа', () => {
            const actionOpenChat: ActionOpenChat = {
                type: ActionTypes.OPEN_CHAT,
                payload: {
                    chat_id: 'foo',
                },
            };
            testStore.dispatch(actionOpenChat);

            // не понимаю почему, но dispatch не возвращает промис :(
            testStore.dispatch(action_post_message('foo', 'goal from desktop'));

            return new Promise((resolve) => {
                setTimeout(() => {
                    expect(reach_goal).toHaveBeenCalledWith(METRIKA_ID, 'CHAT_SEND_MESSAGE_DESKTOP');
                    resolve();
                }, 100);
            });
        });

        it('должен отправить параметр визита "chat_send_message" после отправки сообщения', () => {
            const actionOpenChat: ActionOpenChat = {
                type: ActionTypes.OPEN_CHAT,
                payload: {
                    chat_id: 'foo',
                },
            };
            testStore.dispatch(actionOpenChat);

            // не понимаю почему, но dispatch не возвращает промис :(
            testStore.dispatch(action_post_message('foo', 'event from desktop'));

            return new Promise((resolve) => {
                setTimeout(() => {
                    expect(send_page_event).toHaveBeenCalledWith(METRIKA_ID, [ 'chat_send_message' ]);
                    resolve();
                }, 100);
            });
        });
    });

    describe('метрики на отправку первого сообщения', () => {
        it('должен отправить цели после отправки первого сообщения в чат', () => {
            const actionOpenChat: ActionOpenChat = {
                type: ActionTypes.OPEN_CHAT,
                payload: {
                    chat_id: 'foo',
                    metrics: {
                        startDialog: [
                            { name: 'startGoal1' },
                            { name: 'startGoal2', params: { start2: 'goal2' } },
                        ],
                        startCall: [
                            { name: 'startGoal3' },
                            { name: 'startGoal4', params: { start2: 'goal2' } },
                        ],
                    },
                },
            };
            testStore.dispatch(actionOpenChat);

            // не понимаю почему, но dispatch не возвращает промис :(
            testStore.dispatch(action_post_message('foo', 'hello'));

            return new Promise((resolve) => {
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
            const actionResynced: ActionResynced = {
                type: ActionTypes.RESYNCED,
                payload: {
                    chat_list: [
                        {
                            id: 'foo',
                            messages: [
                                { id: '1' } as Partial<ModelChatMessage> as ModelChatMessage,
                            ],
                            users: [],
                        } as Partial<ModelChatImplementation> as ModelChatImplementation,
                    ],
                    loaded_rooms_num: 1,
                },
            };
            testStore.dispatch(actionResynced);

            const actionOpenChat: ActionOpenChat = {
                type: ActionTypes.OPEN_CHAT,
                payload: {
                    chat_id: 'foo',
                    metrics: {
                        startDialog: [
                            { name: 'startGoal1' },
                            { name: 'startGoal2', params: { start2: 'goal2' } },
                        ],
                        startCall: [
                            { name: 'startGoal3' },
                            { name: 'startGoal4', params: { start2: 'goal2' } },
                        ],
                    },
                },
            };
            testStore.dispatch(actionOpenChat);

            // не понимаю почему, но dispatch не возвращает промис :(
            testStore.dispatch(action_post_message('foo', 'hello'));

            return new Promise((resolve) => {
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
            const actionOpenChat: ActionOpenChat = {
                type: ActionTypes.OPEN_CHAT,
                payload: {
                    chat_id: 'foo',
                    metrics: {
                        startDialog: [
                            { name: 'startGoal1' },
                            { name: 'startGoal2', params: { start2: 'goal2' } },
                        ],
                        startCall: [
                            { name: 'startGoal3' },
                            { name: 'startGoal4', params: { start2: 'goal2' } },
                        ],
                    },
                },
            };

            testStore.dispatch(actionOpenChat);
            testStore.dispatch(action_show_phones());

            return new Promise((resolve) => {
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
    const mockStore = configureStore<ModelState>([ thunk ]);
    let store: MockStore<ModelState>;
    const request_result = [
        chat_mock.withId('id__04').value(),
        chat_mock.withId('id__05').value(),
    ];

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
        return store.dispatch(action_load_more_chat_rooms())
            .then(() => {
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
    describe('для авто', () => {
        const mockStore = configureStore<ModelState>([ thunk ]);
        let store: MockStore<ModelState>;
        const light_request_result = _.range(15).map((id) => chat_mock.withId(`id__${ id }`).withSubject().value());
        const detailed_request_result = _.range(10).map((id) => chat_mock.withId(`id__${ id }`).value());

        beforeEach(() => {
            store = mockStore(defaultState);
        });

        it('сначала сходит в лёгкую ручку списка комнат', () => {
            request_mock.mockReturnValueOnce(Promise.resolve(light_request_result));
            request_mock.mockReturnValueOnce(Promise.resolve(detailed_request_result));

            store.dispatch(action_resync());

            expect(request_mock).toHaveBeenCalledTimes(1);
            expect(request_mock).toHaveBeenLastCalledWith({
                path: 'chat/chat_list_light',
            });
        });

        it('для первых 10 комнат запросит детальную информацию', () => {
            request_mock.mockReturnValueOnce(Promise.resolve(light_request_result));
            request_mock.mockReturnValueOnce(Promise.resolve(detailed_request_result));

            return store.dispatch(action_resync())
                .then(() => {
                    expect(request_mock).toHaveBeenCalledTimes(2);
                    expect(request_mock.mock.calls[1]).toMatchSnapshot();
                });
        });

        it('положит в стор полный массив комнат', () => {
            request_mock.mockReturnValueOnce(Promise.resolve(light_request_result));
            request_mock.mockReturnValueOnce(Promise.resolve(detailed_request_result));

            return store.dispatch(action_resync())
                .then(() => {
                    const expectedAction = {
                        type: ActionTypes.RESYNCED,
                        payload: {
                            chat_list: [
                                ...detailed_request_result,
                                ...light_request_result.slice(10),
                            ],
                            loaded_rooms_num: 10,
                        },
                    };

                    expect(store.getActions()[1]).toEqual(expectedAction);
                });
        });

        it('не будет добавлять в стор комнаты без сообщений, если это не тех саппорт конечно', () => {
            const tech_support_room = chat_mock.withId('id__tech').withSubject().withMessages([]).withChatType(ModelChatType.ROOM_TYPE_TECH_SUPPORT).value();
            const light_rooms_with_messages = _.range(14).map((id) => chat_mock.withId(`id__${ id }`).withSubject().value());
            const light_rooms_without_messages = _.range(5).map((id) => chat_mock.withId(`id__${ id }`).withSubject().withMessages([]).value());
            const detailed_rooms_with_messages = _.range(9).map((id) => chat_mock.withId(`id__${ id }`).value());

            const light_request_result = [
                ...light_rooms_without_messages,
                tech_support_room,
                ...light_rooms_with_messages,
            ];
            const detailed_request_result = [
                tech_support_room,
                ...detailed_rooms_with_messages,
            ];
            request_mock.mockReturnValueOnce(Promise.resolve(light_request_result));
            request_mock.mockReturnValueOnce(Promise.resolve(detailed_request_result));

            return store.dispatch(action_resync())
                .then(() => {
                    const expectedAction = {
                        type: ActionTypes.RESYNCED,
                        payload: {
                            chat_list: [
                                ...detailed_request_result,
                                ...light_rooms_with_messages.slice(9),
                            ],
                            loaded_rooms_num: 10,
                        },
                    };

                    expect(store.getActions()[1]).toEqual(expectedAction);
                });
        });
    });

    describe('для недвиги', () => {
        const mockStore = configureStore<ModelState>([ thunk ]);
        let store: MockStore<ModelState>;
        const rooms_request_result = _.range(15).map((id) => chat_mock.withId(`id__${ id }`).value());

        beforeEach(() => {
            const initialState = _.cloneDeep(defaultState);
            initialState.config.client = ModelClientType.REALTY;

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

            return store.dispatch(action_resync())
                .then(() => {
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
            const tech_support_room = chat_mock.withId('id__tech').withMessages([]).withChatType(ModelChatType.ROOM_TYPE_TECH_SUPPORT).value();
            const rooms_with_messages = _.range(14).map((id) => chat_mock.withId(`id__${ id }`).value());
            const rooms_without_messages = _.range(5).map((id) => chat_mock.withId(`id__${ id }`).withMessages([]).value());

            const rooms_request_result = [
                ...rooms_without_messages,
                tech_support_room,
                ...rooms_with_messages,
            ];
            request_mock.mockReturnValueOnce(Promise.resolve(rooms_request_result));

            return store.dispatch(action_resync())
                .then(() => {
                    const expectedAction = {
                        type: ActionTypes.RESYNCED,
                        payload: {
                            chat_list: [
                                tech_support_room,
                                ...rooms_with_messages,
                            ],
                            loaded_rooms_num: 15,
                        },
                    };

                    expect(store.getActions()[1]).toEqual(expectedAction);
                });
        });
    });
});

describe('action_open_chat_for_offer', () => {
    const mockStore = configureStore<ModelState>([ thunk ]);
    let store: MockStore<ModelState>;

    describe('при наличии флага send_preset_text в параметрах', () => {
        let chat: ModelChatImplementation;

        // кейс если чат не создан протестить не получается
        // ибо все лежит в одном файле и нельзя замокать отдельные экшены, которые меняют стор
        // ограничился только этим пока
        describe('если чат создан', () => {
            beforeEach(() => {
                const preset = defaultState.bunker['common/chat_preset_messages']?.presets.find(({ name }) => name === 'change');
                chat = chat_mock
                    .withId('id__01')
                    // тут пришлось замокать что в очереди уже есть сообщение, так как мы используем мок стейта
                    .withQueuedMessages({ value: preset?.text || '', preset: 'change', properties: { is_send_from_card: true } })
                    .value();
                store = mockStore({
                    ...defaultState,
                    chat_list: [ chat ],
                });

                const post_message_promise = Promise.resolve({ message: {} });
                const load_more_messages_promise = Promise.resolve({ messages: [] });
                const read_chat_promise = Promise.resolve();
                request_mock.mockReturnValueOnce(post_message_promise);
                request_mock.mockReturnValueOnce(load_more_messages_promise);
                request_mock.mockReturnValueOnce(read_chat_promise);
            });

            it('отправит запросы на создание сообщения, чтение чата, загрузку сообщений чата', () => {
                return store.dispatch(action_open_chat_for_offer(
                            chat.source?.category || '',
                            chat.source?.id || '',
                            {
                                startDialog: [ { name: 'dialog_goal' } ],
                                startCall: [ { name: 'call_goal' } ],
                            },
                            { send_preset_text: 'change' },
                ))
                    .then(() => {
                        expect(request_mock).toHaveBeenCalledTimes(4);
                        expect(request_mock.mock.calls).toMatchSnapshot();
                    });
            });

            it('отправит правильную метрику', () => {
                return store.dispatch(action_open_chat_for_offer(
                            chat.source?.category || '',
                            chat.source?.id || '',
                            {
                                startDialog: [ { name: 'dialog_goal' } ],
                                startCall: [ { name: 'call_goal' } ],
                            },
                            { send_preset_text: 'change' },
                ))
                    .then(() => {
                        expect(send_page_event).toHaveBeenCalledTimes(1);
                        expect(send_page_event).toHaveBeenCalledWith('metrika_id', [ 'chat_send_message', 'presets_from_card', 'change' ]);
                    });
            });
        });
    });
});
