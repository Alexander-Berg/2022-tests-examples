const hasUnconfirmBids = require('./hasUnconfirmBids').default;

it('Должен вернуть true, если ставка поменялась', () => {
    expect(hasUnconfirmBids([ { current_bid: 500 } ], [ { current_bid: 200 } ])).toBe(true);
});

it('Должен вернуть false, если ставка осталась прежней', () => {
    expect(hasUnconfirmBids([ { current_bid: 500 } ], [ { current_bid: 500 } ])).toBe(false);
});

it('Должен вернуть false, если ставку сбросили до дефолтной при помощи кнопки минус', () => {
    expect(hasUnconfirmBids([ { current_bid: 200, base_price: 200 } ], [ { current_bid: undefined } ])).toBe(false);
});

it('Должен вернуть false, если ставку сбросили до дефолтной при помощи крестика', () => {
    expect(hasUnconfirmBids([ { current_bid: undefined } ], [ { base_price: 200, current_bid: 200 } ])).toBe(false);
});
