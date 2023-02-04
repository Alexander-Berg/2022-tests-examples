const prepareState = require('./priceInfo');

it('должен вернуть цены, если они есть', () => {
    expect(
        prepareState({
            price_info: {
                eur_price: 1,
                rur_price: 2,
                usd_price: 3,
            },
        }),
    ).toEqual({
        EUR: 1,
        RUR: 2,
        USD: 3,
    });
});

it('не должен вернуть цены, если их нет', () => {
    expect(
        prepareState({}),
    ).toEqual({});
});
