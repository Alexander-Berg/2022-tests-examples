import { checkBooleanYesOrNo, checkValueAsArrayOrString } from './utils';

describe('checkBooleanYesOrNo', () => {
    it('возвращает true, если первое значение "true", второе "YES"', () => {
        expect(checkBooleanYesOrNo(true, 'YES')).toBe(true);
    });

    it('возвращает true, если первое значение "false", второе "NO"', () => {
        expect(checkBooleanYesOrNo(true, 'YES')).toBe(true);
    });

    it('возвращает false, если первое значение "true", второе "NO"', () => {
        expect(checkBooleanYesOrNo(true, 'NO')).toBe(false);
    });

    it('возвращает false, если первое значение "false", второе "YES"', () => {
        expect(checkBooleanYesOrNo(false, 'YES')).toBe(false);
    });
});

describe('checkValueAsArrayOrString', () => {
    it('возвращает true, если первое значение "true", второе "YES"', () => {
        expect(checkValueAsArrayOrString('Аренда', 'Аренда')).toBe(true);
    });

    it('возвращает true, если первое значение "false", второе "NO"', () => {
        expect(checkValueAsArrayOrString('Аренда', ['Аренда', 'Недвижимость'])).toBe(true);
    });

    it('возвращает false, если первое значение "true", второе "NO"', () => {
        expect(checkValueAsArrayOrString('Аренда', ['Недвижимость'])).toBe(false);
    });

    it('возвращает false, если первое значение "false", второе "YES"', () => {
        expect(checkValueAsArrayOrString(123, [456])).toBe(false);
    });

    it('возвращает true, если во втором элементе есть такое же значение из первого', () => {
        expect(checkValueAsArrayOrString(['Аренда', 'Недвижимость'], ['Недвижимость'])).toBe(true);
    });

    it('возвращает true,если в первом элементе есть такое же значение из второго', () => {
        expect(checkValueAsArrayOrString(['Недвижимость'], ['Аренда', 'Недвижимость'])).toBe(true);
    });

    it('возвращает false, если во втором элементе нет такого же значения из первого', () => {
        expect(checkValueAsArrayOrString(['Журнал'], ['Аренда', 'Недвижимость'])).toBe(false);
    });

    it('возвращает false, если в первом элементе нет такого же значения из второго', () => {
        expect(checkValueAsArrayOrString(['Аренда', 'Недвижимость'], ['Журнал'])).toBe(false);
    });

    it('возвращает false, если нет второго значения', () => {
        expect(checkValueAsArrayOrString(123)).toBe(false);
    });

    it('возвращает false, если нет значений', () => {
        expect(checkValueAsArrayOrString()).toBe(false);
    });
});
