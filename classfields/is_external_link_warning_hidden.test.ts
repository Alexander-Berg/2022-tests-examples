import { ModelChatMessageContentType } from '../models';

import is_external_link_warning_hidden from './is_external_link_warning_hidden';

const test_message = {
    id: '111',
    room_id: '333',
    author: '',
    user: {
        id: '',
        is_me: true,
    },
    created: '01-01-2020 12:00',
    payload: { content_type: ModelChatMessageContentType.TEXT_PLAIN, value: '' },
    properties: {},
};

it('вернет true если есть запись об этом сообщении', () => {
    const storage = [ { key: '333_111', is_hidden: true, ts: 1640995200000 } ];

    expect(is_external_link_warning_hidden(storage, test_message)).toBe(true);
});

it('вернет false если нет записи об этом сообщении', () => {
    const storage = [ { key: '333_222', is_hidden: true, ts: 1640995200000 } ];

    expect(is_external_link_warning_hidden(storage, test_message)).toBe(false);
});

it('вернет false если в ls пустой массив', () => {
    expect(is_external_link_warning_hidden([], test_message)).toBe(false);
});
