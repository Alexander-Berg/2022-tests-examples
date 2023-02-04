const getBundleDiscountValue = require('./getBundleDiscountValue').default;

describe('getBundleDiscountValue', () => {
    it('правильно высчитывает скидку', () => {
        expect(getBundleDiscountValue(100, 50)).toEqual(50);
    });

    it('возвращает undefined если скидка меньше 20%', () => {
        expect(getBundleDiscountValue(100, 81)).toBeNull();
    });
});
