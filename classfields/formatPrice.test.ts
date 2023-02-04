import formatPrice from './formatPrice';

describe('formatPrice', () => {
    it('вернет объект с числом и аббревиатурой для тысяч', () => {
        const actual = formatPrice(10500);

        expect(actual).toEqual({
            formattedValue: '11',
            abbreviation: 'тыс ₽',
        });
    });

    it('вернет объект с числом и аббревиатурой для миллионов', () => {
        const actual = formatPrice(1000000);

        expect(actual).toEqual({
            formattedValue: '1',
            abbreviation: 'млн ₽',
        });
    });
});
