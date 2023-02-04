import i18n from 'realty-core/view/react/libs/i18n';
import { priceFormat } from '../index';

describe('priceFormat', () => {
    beforeEach(() => {
        i18n.setLang('ru');
    });

    it('returns first argument when called with first empty argument', () => {
        expect(priceFormat(undefined)).toEqual(undefined);
        expect(priceFormat(null)).toEqual(null);
        expect(priceFormat(NaN)).toEqual(NaN);
    });

    it('does not treat numeric 0 as empty argument', () => {
        expect(priceFormat(0)).toEqual('0\u00A0₽');
    });

    it('sets a currency sign before a value if it is not a RUB', () => {
        expect(priceFormat(1000, { currency: 'EUR' })).toEqual('€\u00A01\u00A0000');
        expect(priceFormat(1000, { currency: 'USD' })).toEqual('$\u00A01\u00A0000');
    });

    it('sets a currency sign after a value if it is a RUB', () => {
        expect(priceFormat(1000, { currency: 'RUB' })).toEqual('1\u00A0000\u00A0₽');
    });

    it('формирует цену в миллионах', () => {
        expect(priceFormat(1000000, { currency: 'RUB', isPriceAbbreviate: true })).toEqual('1\u00A0млн\u00A0₽');
        expect(priceFormat(3000000, { currency: 'RUB', isPriceAbbreviate: true })).toEqual('3\u00A0млн\u00A0₽');
    });

    it('формирует цену в миллиардах', () => {
        expect(priceFormat(5000000000, { currency: 'RUB', isPriceAbbreviate: true })).toEqual('5\u00A0млрд\u00A0₽');
    });

    it('формирует цену в миллионах с точностью до 2 знаков', () => {
        expect(priceFormat(12345678, { currency: 'RUB', isPriceAbbreviate: true })).toEqual('12,35\u00A0млн\u00A0₽');
    });

    it('формирует цену в миллионах, отбрасывая нули', () => {
        expect(priceFormat(9100000, { currency: 'RUB', isPriceAbbreviate: true })).toEqual('9,1\u00A0млн\u00A0₽');
    });

    it('формирует цену в миллионах, не форматируя', () => {
        expect(priceFormat(1000000, { currency: 'RUB' })).toEqual('1\u00A0000\u00A0000\u00A0₽');
        expect(priceFormat(3000000, { currency: 'RUB' })).toEqual('3\u00A0000\u00A0000\u00A0₽');
    });

    it('не сокращает цену, если isPriceAbbreviate "false"', () => {
        expect(priceFormat(9100000, { isPriceAbbreviate: false })).toEqual('9\u00A0100\u00A0000\u00A0₽');
    });
});
