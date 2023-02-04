const getPrice = require('./getPrice');

it('должен переводить из копеек в рубли', () => {
    const result = getPrice({ billing: { cost: { amount: 12345 } } });

    expect(result).toBe(123.45);
});
