const isAvailableTradeIn = require('./isAvailableTradeIn');

it('должен возвращать true, если есть хотя бы пустой user offer', () => {
    const result = isAvailableTradeIn({
        user_car_info: {},
    });

    expect(result).toBe(true);
});

it('должен возвращать false, если нет поля с user offer', () => {
    const result = isAvailableTradeIn({});

    expect(result).toBe(false);
});
