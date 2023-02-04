const getAbsoluteDiscountValue = require('./getAbsoluteDiscountValue');

describe('getAbsoluteDiscountValue', () => {
    it('правильно высчитывает скидку', () => {
        expect(getAbsoluteDiscountValue(100, 50)).toEqual(50);
    });

    it('округляет до нуля и возвращает undefined, для незначительной скидки (меньше 0,5%)', () => {
        expect(getAbsoluteDiscountValue(100, 99.6)).toBeUndefined();
    });
});
