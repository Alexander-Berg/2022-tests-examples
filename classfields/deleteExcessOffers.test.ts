import deleteExcessOffers from './deleteExcessOffers';

it('должен удалить самые старые записи, если перелимит', () => {
    const offers = {
        '111': 10,
        '222': 1,
        '333': 4,
        '555': 100,
        '666': 4,
    };

    expect(deleteExcessOffers(offers, 3)).toEqual({ '111': 10, '333': 4, '555': 100 });
});

it('ничего не поменяется, если нет перелимита', () => {
    expect(deleteExcessOffers({ '111': 1 }, 3)).toEqual({ '111': 1 });
});

it('пустое остается пустым', () => {
    expect(deleteExcessOffers({}, 1)).toEqual({});
});
