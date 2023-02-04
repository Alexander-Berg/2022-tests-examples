const getPopularItems = require('./getPopularItems');

let items;
beforeEach(() => {
    items = [
        { id: '0', name: '0', count: 0 },
        { id: '1', name: '1', count: 0 },
        { id: '2', name: '2', count: 0, reviews_count: 42 },
        { id: '3', name: '3', count: 1, reviews_count: 15 },
        { id: '4', name: '4', count: 0 },
        { id: '5', name: '5', count: 0, popular: true },
        { id: '6', name: '6', count: 2 },
        { id: '7', name: '7', count: 3, popular: true },
    ];
});

it('Должен оставить айтемы с каунтом', () => {
    expect(getPopularItems(items, 4)).toEqual([ items[3], items[6], items[7] ]);
});

it('Должен оставить айтемы с кастомным каунтом', () => {
    expect(getPopularItems(items, 4, { counterField: 'reviews_count' })).toEqual([ items[2], items[3] ]);
});

it('Должен оставить айтемы популярные и с каунтом', () => {
    expect(getPopularItems(items, 3, { usePopularFlag: true, withEmpty: true }))
        .toEqual([ items[5], items[6], items[7] ]);
});

it('Должен оставить айтемы с каунтом и популярные', () => {
    expect(getPopularItems(items, 3, { usePopularFlagAsSecondary: true, withEmpty: true }))
        .toEqual([ items[3], items[6], items[7] ]);
});

it('Должен оставить айтемы с каунтом и популярные и добавить пустые непопулярные до нужного количества', () => {
    expect(getPopularItems(items, 5, { usePopularFlagAsSecondary: true, withEmpty: true }))
        .toEqual([ items[0], items[3], items[5], items[6], items[7] ]);
});
