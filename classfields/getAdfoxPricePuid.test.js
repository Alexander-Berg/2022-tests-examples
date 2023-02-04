const getAdfoxPricePuid = require('./getAdfoxPricePuid');

it.each([
    [ 3999999, '3900000-3999999' ],
    [ 4000000, '4000000-4199999' ],
    [ 4000001, '4000000-4199999' ],
    [ 90000000, '50000000-99999999' ],
    [ 100000000, '' ],
])('должен преобразовать %i в "%s"', (price, expected) => {
    expect(getAdfoxPricePuid(price)).toEqual(expected);
});
