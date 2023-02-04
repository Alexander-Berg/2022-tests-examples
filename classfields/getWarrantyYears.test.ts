import MockDate from 'mockdate';

import getWarrantyYears from './getWarrantyYears';

describe('тесты для функции getWarrantyYears', () => {
    it('возвращаем список лет в диапазоне +10 лет для всех лет, кроме текущего', () => {
        MockDate.set('2020-10-20');

        const result = getWarrantyYears();

        expect(result).toHaveLength(11);
        expect(result[0]).toBe(2030);
        expect(result[result.length - 1]).toBe(2020);
    });

    it('если текущий месяц декабрь, возвращаем список лет, начиная со следующего года', () => {
        MockDate.set('2020-12-20');

        const result = getWarrantyYears();

        expect(result[result.length - 1]).toBe(2021);
    });
});
