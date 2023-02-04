import { transformFirstSymbolToLowerCase } from '../index';

describe('transformFirstSymbolToLowerCase', () => {
    it('Первая буква строки преобразовывается к нижнему регистру', () => {
        expect(transformFirstSymbolToLowerCase('Строка')).toBe('строка');
    });

    it('Если в строке несколько заглавных букв, к нижнему регистру приводится только первая', () => {
        expect(transformFirstSymbolToLowerCase('СТРОКА')).toBe('сТРОКА');
    });

    it('При вызове с пустой строкой возвращается пустая строка', () => {
        expect(transformFirstSymbolToLowerCase('')).toBe('');
    });

    it('Если строка состоит из одного символа преобразуется только он', () => {
        expect(transformFirstSymbolToLowerCase('А')).toBe('а');
    });

    it('Если первый символ - прописная буква, преобразования не происходит', () => {
        expect(transformFirstSymbolToLowerCase('строка')).toBe('строка');
    });
});
