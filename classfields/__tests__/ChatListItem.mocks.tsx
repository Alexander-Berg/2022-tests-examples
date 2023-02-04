import { ModelChatMessageContentType, ModelChatType } from 'view/models';

// eslint-disable-next-line @typescript-eslint/no-var-requires
const merge = require('lodash/merge');

const { TEXT_PLAIN } = ModelChatMessageContentType;
const { ROOM_TYPE_OFFER, ROOM_TYPE_TECH_SUPPORT, ROOM_TYPE_SIMPLE } = ModelChatType;
const YELLOW_PIXEL =
    // eslint-disable-next-line max-len
    'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQYV2P4vFH9PwAHMQLLbxpUjgAAAABJRU5ErkJggg==';

const initialProps = {
    chat: {
        type: ROOM_TYPE_TECH_SUPPORT,
        id: '777',
        source: undefined,
        created: '',
        users: [
            {
                id: '1',
                is_me: true,
            },
            {
                id: '2',
                is_me: false,
                room_last_read: '02-03-2020 13:00',
                profile: {
                    alias: 'Я.Недвижимость',
                },
            },
        ],
        me: '1',
        owner: '',
        messages: [
            {
                id: '',
                room_id: '',
                author: '',
                user: {
                    id: '2',
                    is_me: false,
                },
                created: '02-03-2020 12:00',
                payload: {
                    content_type: TEXT_PLAIN,
                    value:
                        // eslint-disable-next-line max-len
                        'Нужно бежать со всех ног, чтобы только оставаться на месте, а чтобы куда-то попасть, надо бежать как минимум вдвое быстрее!',
                },
            },
        ],
        subject: {
            price: {
                price: 750000,
            },
            status: 'ACTIVE',
            mark: {
                name: 'BMW',
            },
            model: {
                name: 'X5',
            },
            super_gen: {
                name: '1 серия',
            },
            year: '2004',
            images: {
                '120x90': 'https://avatars.mdst.yandex.net/car_offer',
                small: 'https://avatars.mdst.yandex.net/tech_support',
            },
        },
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
    selected: false,
};

const getProps = (overrides = {}) => merge({}, initialProps, overrides);

export default {
    tech_support: getProps({
        chat: {
            messages: [
                {
                    payload: {
                        value: '<br>Нужно бежать со&nbsp;всех ног, чтобы только оставаться на месте</br>',
                    },
                },
            ],
        },
    }),
    selected: getProps({
        selected: true,
    }),
    short_message: getProps({
        chat: {
            messages: [
                {
                    payload: {
                        value: 'Нужно бежать со всех ног.',
                    },
                },
            ],
        },
    }),
    yesterday_message: getProps({
        chat: {
            messages: [
                {
                    created: '02-02-2020 12:00',
                },
            ],
        },
    }),
    day_before_yesterday_message: getProps({
        chat: {
            messages: [
                {
                    created: '02-01-2020 12:00',
                },
            ],
        },
    }),
    last_year_message: getProps({
        chat: {
            messages: [
                {
                    created: '02-03-2019 12:00',
                },
            ],
        },
    }),
    muted_last_year_message: getProps({
        chat: {
            messages: [
                {
                    created: '02-03-2019 12:00',
                },
            ],
            muted: true,
        },
    }),
    unread: getProps({
        chat: {
            has_unread: true,
        },
    }),
    muted: getProps({
        chat: {
            muted: true,
        },
    }),
    attachment: getProps({
        chat: {
            messages: [
                {
                    payload: {
                        value: '',
                    },
                    attachments: [{}],
                },
            ],
        },
    }),
    seller: getProps({
        chat: {
            type: ROOM_TYPE_OFFER,
            users: [
                {
                    id: '1',
                    is_me: true,
                    profile: null,
                },
                {
                    id: '2',
                    is_me: false,
                    profile: null,
                },
            ],
            owner: '2',
            me: '1',
        },
    }),
    buyer: getProps({
        chat: {
            type: ROOM_TYPE_OFFER,
            users: [
                {
                    id: '1',
                    is_me: true,
                    profile: null,
                },
                {
                    id: '2',
                    is_me: false,
                    profile: null,
                },
            ],
            owner: '1',
            me: '1',
        },
    }),
    sold: getProps({
        chat: {
            type: ROOM_TYPE_OFFER,
            users: [
                {
                    id: '1',
                    is_me: true,
                    profile: null,
                },
                {
                    id: '2',
                    is_me: false,
                    profile: null,
                },
            ],
            owner: '2',
            me: '1',
            subject: {
                status: 'SOLD',
            },
        },
    }),
    blocked: getProps({
        chat: {
            type: ROOM_TYPE_OFFER,
            users: [
                {
                    id: '1',
                    is_me: true,
                    profile: null,
                },
                {
                    id: '2',
                    is_me: false,
                    profile: null,
                },
            ],
            owner: '2',
            me: '1',
            blocked_by_me: true,
        },
    }),
    without_avatar: getProps({
        chat: {
            type: ROOM_TYPE_OFFER,
            users: [
                {
                    id: '1',
                    is_me: true,
                    profile: null,
                },
                {
                    id: '2',
                    is_me: false,
                    profile: null,
                },
            ],
            owner: '2',
            me: '1',
            subject: {
                images: {
                    '120x90': null,
                    small: null,
                },
            },
        },
    }),
    developer: getProps({
        chat: {
            type: ROOM_TYPE_OFFER,
            users: [
                {
                    id: '1',
                    is_me: true,
                    profile: null,
                },
                {
                    id: '2',
                    is_me: false,
                    profile: {
                        alias: 'ПИК',
                        userpic: {
                            sizes: {
                                '24x24': YELLOW_PIXEL,
                            },
                        },
                    },
                },
            ],
            owner: '2',
            me: '1',
            subject: {
                images: {
                    '120x90': null,
                    small: null,
                },
                newbuilding: {
                    id: 285654,
                    full_name: 'ЖК «Светлый»',
                },
            },
        },
    }),
    with_avatar: getProps({
        chat: {
            type: ROOM_TYPE_OFFER,
            users: [
                {
                    id: '1',
                    is_me: true,
                    profile: null,
                },
                {
                    id: '2',
                    is_me: false,
                    profile: {
                        alias: 'john doe',
                        userpic: {
                            sizes: {
                                '24x24': YELLOW_PIXEL,
                            },
                        },
                    },
                },
            ],
            owner: '2',
            me: '1',
        },
    }),
    with_nickname: getProps({
        chat: {
            type: ROOM_TYPE_OFFER,
            users: [
                {
                    id: '1',
                    is_me: true,
                    profile: null,
                },
                {
                    id: '2',
                    is_me: false,
                    profile: {
                        alias: 'john doe',
                    },
                },
            ],
            owner: '2',
            me: '1',
        },
    }),
    notification_center: getProps({
        chat: {
            type: ROOM_TYPE_SIMPLE,
            subject: {
                images: {
                    // красный пиксель
                    '120x90':
                        // eslint-disable-next-line max-len
                        'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQYV2O4ba7+HwAFYwI5dFG+fAAAAABJRU5ErkJggg==',
                    small:
                        // eslint-disable-next-line max-len
                        'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQYV2O4ba7+HwAFYwI5dFG+fAAAAABJRU5ErkJggg==',
                },
            },
            messages: [
                {
                    payload: {
                        value:
                            // eslint-disable-next-line max-len
                            '<html><body><br>Нужно бежать со&nbsp;всех ног, чтобы только оставаться на месте</br></body></html>',
                    },
                },
            ],
            id: 'testChat-28199386-777',
            users: [
                {
                    id: '1',
                    is_me: true,
                },
                {
                    id: '2',
                    is_me: false,
                    room_last_read: '02-03-2020 13:00',
                    profile: {
                        alias: 'Центр уведомлений',
                    },
                },
            ],
            me: '1',
        },
    }),
};
