import mockdate from 'mockdate';

import deleteExpiredOffers from './deleteExpiredOffers';

beforeEach(() => {
    mockdate.set(1633963449063);
});

it('должен удалить просроченные записи и оставить непросроченные', () => {
    const offers = {
        '111': 1633963449063 - 1 * 24 * 60 * 60 * 1000 - 1,
        '222': 1633963449063 - 1 * 24 * 60 * 60 * 1000 + 1,
        '333': 1633963449063 - 1 * 24 * 60 * 60 * 1000,

    };
    expect(deleteExpiredOffers(offers, 1)).toEqual({ '222': 1633963449063 - 1 * 24 * 60 * 60 * 1000 + 1 });
});

it('пустое остается пустым', () => {
    expect(deleteExpiredOffers({}, 1)).toEqual({});
});
