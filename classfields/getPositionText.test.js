const getPositionText = require('./getPositionText').default;

it('Должен вернуть - / 2, если пользователь не участвует в аукционе', () => {
    expect(getPositionText({ current_bid: undefined, competitive_bids: [ { bid: 2200, competitors: 2 } ] })).toBe(`- / 2`);
});

it('Должен вернуть - / 2, если ставка пользователя равна базовой цене', () => {
    expect(getPositionText({ current_bid: 2000, base_price: 2000, competitive_bids: [ { bid: 2200, competitors: 2 } ] })).toBe(`- / 2`);
});

it('Должен вернуть 1 / 1, если пользователь - единственный участник аукциона', () => {
    expect(getPositionText({ current_bid: 2800, competitive_bids: undefined })).toBe(`1 / 1`);
});

it('Должен вернуть 3 / 5, если ставка пользователя находится между ставками других участников аукциона', () => {
    expect(getPositionText({
        current_bid: 2300,
        competitive_bids: [
            { bid: 2500, competitors: 2 },
            { bid: 2200, competitors: 2 },
        ] })).toBe(`3 / 5`);
});

it('Должен вернуть 3-5 / 7, если ставка пользователя совпадает со ставками других участников аукциона', () => {
    expect(getPositionText({
        current_bid: 2300,
        competitive_bids: [
            { bid: 2500, competitors: 2 },
            { bid: 2300, competitors: 2 },
            { bid: 2200, competitors: 2 },
        ] })).toBe(`3-5 / 7`);
});
