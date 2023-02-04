const getDiscount = require('./getDiscount');

describe('function "getDiscount"', () => {
    const testCases = [
        { originalPrice: 99, price: 66, discount: 33 },
        { originalPrice: 0, price: 66, discount: 0 },
        { originalPrice: 77, price: 99, discount: 0 },
        { originalPrice: 999, price: 1, discount: 99 },
        { price: 99, discount: 0 },
    ];

    testCases.forEach(({ discount, originalPrice, price }) => {
        it(`правильно рассчитывает скидку для пары цен: ${ price } и ${ originalPrice }`, () => {
            const result = getDiscount(originalPrice, price);
            expect(result).toBe(discount);
        });
    });
});
