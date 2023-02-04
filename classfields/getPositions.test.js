const getPositions = require('./getPositions').default;

it('Должен вернуть массив ставок, если пользователь не участвует в аукционе', () => {
    expect(getPositions([
        { bid: 2300, competitors: 2 },
        { bid: 2400, competitors: 2 },
    ], undefined, 2000)).toEqual([
        { bid: 2400, text: '1-2', isCurrent: false },
        { bid: 2300, text: '3-4', isCurrent: false },
    ]);
});

it('Должен вернуть массив ставок, если ставка пользователя равна базовой цене', () => {
    expect(getPositions([
        { bid: 2300, competitors: 2 },
        { bid: 2400, competitors: 2 },
    ], 2000, 2000)).toEqual([
        { bid: 2400, text: '1-2', isCurrent: false },
        { bid: 2300, text: '3-4', isCurrent: false },
    ]);
});

it('Должен вернуть массив ставок, если пользователь участвует в аукционе и его ставка сопадает со ставками других участников', () => {
    expect(getPositions([
        { bid: 2300, competitors: 2 },
        { bid: 2400, competitors: 2 },
    ], 2400, 2000)).toEqual([
        { bid: 2400, text: '1-3', isCurrent: true },
        { bid: 2300, text: '4-5', isCurrent: false },
    ]);
});

it('Должен вернуть массив ставок, если ставка пользователя отличается от ставок других участников', () => {
    expect(getPositions([
        { bid: 2300, competitors: 2 },
        { bid: 2400, competitors: 2 },
    ], 2700, 2000)).toEqual([
        { bid: 2700, text: '1', isCurrent: true },
        { bid: 2400, text: '2-3', isCurrent: false },
        { bid: 2300, text: '4-5', isCurrent: false },
    ]);
});
