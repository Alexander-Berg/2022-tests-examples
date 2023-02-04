import mockdate from 'mockdate';

import deleteNotSessionOffers from './deleteNotSessionOffers';

const MOCK_DATE_NOW = 1633963449063;
const THIRTY_MINUTES = 30 * 60 * 1000;

beforeEach(() => {
    mockdate.set(MOCK_DATE_NOW);
});

it('должен удалить просроченные записи и оставить непросроченные', () => {
    const offers = {
        '111': MOCK_DATE_NOW - THIRTY_MINUTES - 1,
        '222': MOCK_DATE_NOW - THIRTY_MINUTES + 1,
        '333': MOCK_DATE_NOW - THIRTY_MINUTES,
    };
    expect(deleteNotSessionOffers(offers)).toEqual({ '222': MOCK_DATE_NOW - THIRTY_MINUTES + 1, '333': MOCK_DATE_NOW - THIRTY_MINUTES });
});

it('пустое остается пустым', () => {
    expect(deleteNotSessionOffers({})).toEqual({});
});
