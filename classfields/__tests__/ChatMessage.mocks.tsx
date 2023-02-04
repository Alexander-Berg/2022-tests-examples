import { ModelChatMessageContentType, ModelChatMessagePropertiesType, ModelChatType } from 'view/models';

// eslint-disable-next-line @typescript-eslint/no-var-requires
const merge = require('lodash/merge');

const { TEXT_PLAIN, TEXT_HTML } = ModelChatMessageContentType;
const { ROOM_TYPE_OFFER, ROOM_TYPE_TECH_SUPPORT } = ModelChatType;
const { TECH_SUPPORT_FEEDBACK_REQUEST, TECH_SUPPORT_POLL, ACTIONS, CALL_INFO } = ModelChatMessagePropertiesType;

const initialProps = {
    chat: {
        type: ROOM_TYPE_TECH_SUPPORT,
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
        messages: [
            {
                id: '1',
            },
        ],
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
        id: '1',
        room_id: '',
        author: '',
        user: {
            id: '',
            is_me: true,
        },
        created: '01-01-2020 12:00',
        payload: {
            content_type: TEXT_PLAIN,
            value:
                // eslint-disable-next-line max-len
                'Нужно бежать со всех ног, чтобы только оставаться на месте, а чтобы куда-то попасть, надо бежать как минимум вдвое быстрее!',
        },
    },
    last_of_group: false,
};

const getProps = (overrides = {}) => merge({}, initialProps, overrides);

export default {
    sent: getProps(),
    sent_with_tail: getProps({
        last_of_group: true,
    }),
    received: getProps({
        message: {
            user: {
                is_me: false,
            },
        },
    }),
    received_with_tail: getProps({
        message: {
            user: {
                is_me: false,
            },
        },
        last_of_group: true,
    }),
    brief: getProps({
        message: {
            payload: {
                value: 'Я хочу убить время',
            },
        },
    }),
    queued: getProps({
        chat: {
            type: ROOM_TYPE_OFFER,
        },
        message: {
            status: 'queued',
        },
    }),
    unread: getProps({
        chat: {
            type: ROOM_TYPE_OFFER,
            users: [
                {
                    id: '',
                    is_me: false,
                    room_last_read: null,
                },
            ],
        },
    }),
    read: getProps({
        chat: {
            type: ROOM_TYPE_OFFER,
        },
    }),
    failed: getProps({
        chat: {
            type: ROOM_TYPE_OFFER,
        },
        message: {
            status: 'failed',
        },
    }),
    failed_with_tail: getProps({
        chat: {
            type: ROOM_TYPE_OFFER,
        },
        message: {
            status: 'failed',
        },
        last_of_group: true,
    }),
    presets: getProps({
        message: {
            payload: {
                value: 'Что вам больше всего понравилось?',
            },
            user: {
                is_me: false,
            },
            properties: {
                type: TECH_SUPPORT_FEEDBACK_REQUEST,
                tech_support_feedback: {
                    ttl: 3600,
                    presets: [
                        {
                            id: '1',
                            value: 'Вежливо обращались',
                        },
                        {
                            id: '2',
                            value: 'Быстро ответили',
                        },
                        {
                            id: '3',
                            value: 'Вернули деньги',
                        },
                    ],
                },
            },
        },
    }),
    with_one_preset: getProps({
        message: {
            payload: {
                value: 'Что вам больше всего понравилось?',
            },
            user: {
                is_me: false,
            },
            properties: {
                type: TECH_SUPPORT_FEEDBACK_REQUEST,
                tech_support_feedback: {
                    ttl: 3600,
                    presets: [
                        {
                            id: '1',
                            value: 'Вежливо обращались',
                        },
                    ],
                },
            },
        },
    }),
    bot_commands: getProps({
        message: {
            payload: {
                value: 'Выберите причину обращения',
            },
            user: {
                is_me: false,
            },
            properties: {
                type: ACTIONS,
                keyboard: {
                    buttons: [
                        {
                            id: '1',
                            value: 'Проблема с формой подачи',
                        },
                        {
                            id: '2',
                            value: 'Ничего не работает',
                        },
                    ],
                },
            },
        },
    }),
    with_one_bot_commands: getProps({
        message: {
            payload: {
                value: 'Выберите причину обращения',
            },
            user: {
                is_me: false,
            },
            properties: {
                type: ACTIONS,
                keyboard: {
                    buttons: [
                        {
                            id: '1',
                            value: 'Проблема с формой подачи',
                        },
                    ],
                },
            },
        },
    }),
    poll: getProps({
        message: {
            payload: {
                value: 'Надеемся, мы вам помогли! Будет здорово, если вы поставите нам оценку',
            },
            user: {
                is_me: false,
            },
            properties: {
                type: TECH_SUPPORT_POLL,
                tech_support_poll: {
                    ttl: 3600,
                },
            },
        },
    }),
    poll_selected: getProps({
        message: {
            payload: {
                value: 'Надеемся, мы вам помогли! Будет здорово, если вы поставите нам оценку',
            },
            user: {
                is_me: false,
            },
            properties: {
                type: TECH_SUPPORT_POLL,
                tech_support_poll: {
                    ttl: 3600,
                    selected_rating: 3,
                },
            },
        },
    }),
    html: getProps({
        message: {
            payload: {
                content_type: TEXT_HTML,
                value: '— А куда ты хочешь попасть?<br/>— Мне все равно…<br/>— Тогда все равно, куда и идти.',
            },
        },
    }),
    attachment: getProps({
        message: {
            payload: {
                value: '',
            },
            attachments: [
                {
                    image: {
                        sizes: {
                            '460x460': 'https://avatars.mdst.yandex.net/attachment',
                        },
                    },
                },
            ],
        },
    }),
    attachment_with_tail: getProps({
        message: {
            payload: {
                value: '',
            },
            attachments: [
                {
                    image: {
                        sizes: {
                            '460x460': 'https://avatars.mdst.yandex.net/attachment',
                        },
                    },
                },
            ],
        },
        last_of_group: true,
    }),
    attachment_unread: getProps({
        chat: {
            type: ROOM_TYPE_OFFER,
            users: [
                {
                    id: '',
                    is_me: false,
                    room_last_read: null,
                },
            ],
        },
        message: {
            payload: {
                value: '',
            },
            attachments: [
                {
                    image: {
                        sizes: {
                            '460x460': 'https://avatars.mdst.yandex.net/attachment',
                        },
                    },
                },
            ],
        },
    }),
    attachment_read: getProps({
        chat: {
            type: ROOM_TYPE_OFFER,
        },
        message: {
            payload: {
                value: '',
            },
            attachments: [
                {
                    image: {
                        sizes: {
                            '460x460': 'https://avatars.mdst.yandex.net/attachment',
                        },
                    },
                },
            ],
        },
    }),
    call_incoming: getProps({
        chat: {
            type: ROOM_TYPE_OFFER,
        },
        message: {
            payload: {
                value: 'Звонок в 15:50 мск длительностью 25 сек от *******7684.',
            },
            user: {
                is_me: false,
            },
            properties: {
                type: CALL_INFO,
                call_info: {
                    duration: 25,
                },
            },
        },
    }),
    call_incoming_missed: getProps({
        chat: {
            type: ROOM_TYPE_OFFER,
        },
        message: {
            payload: {
                value: 'Звонок в 15:50 мск длительностью 25 сек от *******7684.',
            },
            user: {
                is_me: false,
            },
            properties: {
                type: CALL_INFO,
                call_info: {
                    duration: 0,
                },
            },
        },
    }),
    call_outcoming: getProps({
        chat: {
            type: ROOM_TYPE_OFFER,
        },
        message: {
            payload: {
                value: 'Звонок в 15:50 мск длительностью 25 сек от *******7684.',
            },
            properties: {
                type: CALL_INFO,
                call_info: {
                    duration: 125,
                },
            },
        },
    }),
    call_outcoming_missed: getProps({
        chat: {
            type: ROOM_TYPE_OFFER,
        },
        message: {
            payload: {
                value: 'Звонок в 15:50 мск длительностью 25 сек от *******7684.',
            },
            properties: {
                type: CALL_INFO,
                call_info: {
                    duration: 0,
                },
            },
        },
    }),
};
