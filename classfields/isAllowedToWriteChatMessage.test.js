const isAllowedToWriteChatMessage = require('./isAllowedToWriteChatMessage');

it('должен вернуть true, если объявление от дилера и чаты разрешены', () => {
    expect(isAllowedToWriteChatMessage({
        additional_info: {
            is_owner: false,
        },
        seller: {
            chats_enabled: true,
        },
        seller_type: 'COMMERCIAL',
    })).toBe(true);
});

it('должен вернуть false, если объявление от дилера и чаты запрещены', () => {
    expect(isAllowedToWriteChatMessage({
        additional_info: {
            is_owner: false,
        },
        seller: {
            chats_enabled: false,
        },
        seller_type: 'COMMERCIAL',
    })).toBe(false);
});

it('должен вернуть true, если объявление от дилера, чаты разрешены, но это владелец', () => {
    expect(isAllowedToWriteChatMessage({
        additional_info: {
            is_owner: true,
        },
        seller: {
            chats_enabled: true,
        },
        seller_type: 'COMMERCIAL',
    })).toBe(false);
});

it('должен вернуть true, если объявление от частника и чаты разрешены', () => {
    expect(isAllowedToWriteChatMessage({
        additional_info: {
            is_owner: false,
        },
        seller: {
            chats_enabled: true,
        },
        seller_type: 'PRIVATE',
    })).toBe(true);
});

it('должен вернуть false, если объявление от частника и чаты запрещены', () => {
    expect(isAllowedToWriteChatMessage({
        additional_info: {
            is_owner: false,
        },
        seller: {
            chats_enabled: false,
        },
        seller_type: 'PRIVATE',
    })).toBe(false);
});

it('должен вернуть false, если объявление от частника и чаты разрешены, но это владелец', () => {
    expect(isAllowedToWriteChatMessage({
        additional_info: {
            is_owner: true,
        },
        seller: {
            chats_enabled: true,
        },
        seller_type: 'PRIVATE',
    })).toBe(false);
});
