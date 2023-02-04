import formatTax from './formatTax';

describe('formatTax', () => {
    it('возвращает текст, если была передана стоимость транспортного налога меньше 1000', () => {
        const result = formatTax({ tax: 800 });
        expect(result).toBe('800 ₽');
    });

    it('возвращает текст, если была передана стоимость транспортного налога больше 1000', () => {
        const result = formatTax({ tax: 2400 });
        expect(result).toBe('2.4 тыс.');
    });

    it('возвращает текст, если была передана минимальная стоимость транспортного налога', () => {
        const result = formatTax({ minTax: 2400 });
        expect(result).toBe('от 2.4 тыс.');
    });

    it('возвращает текст, если была передана минимальная стоимость транспортного налога равная нулю', () => {
        const result = formatTax({ minTax: 0 });
        expect(result).toBe('от 0 ₽');
    });

    it('возвращает строку без дробной части у числа, если число кратно тысячи', () => {
        const result = formatTax({ minTax: 1000 });
        expect(result).toBe('от 1 тыс.');
    });
});
