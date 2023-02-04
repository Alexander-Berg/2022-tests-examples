/* eslint-disable no-undef */
import formatNumber from '../format-number';

function mockWarn() {
    const warn = jest.fn();

    global.console = { warn };

    return warn;
}

describe('formatNumber', () => {
    it('formats positive and negative numbers', () => {
        expect(formatNumber(123)).toBe('123');
        expect(formatNumber(-123)).toBe('\u2212123');
    });

    it('formats into a number with delimiters', () => {
        expect(formatNumber(12345678)).toBe('12\u00A0345\u00A0678');
    });

    it('supports redefining minus through options', () => {
        expect(formatNumber(-1, { minus: '– ' })).toBe('– 1');
    });

    it('supports redefining delimiter through options', () => {
        expect(formatNumber(1234, { delimiter: '^' })).toBe('1^234');
        expect(formatNumber(1, { delimiter: '^' })).toBe('1');
    });

    it('supports redefining delimiter and minus together', () => {
        expect(formatNumber(-1234, { delimiter: '^', minus: 'minus ' })).toBe('minus 1^234');
        expect(formatNumber(1, { delimiter: '^', minus: 'minus ' })).toBe('1');
    });

    // * backward compatibility *

    it('supports passing delimiter as second argument (for backward compatibility)', () => {
        const warn = mockWarn();

        expect(formatNumber(1234, '-')).toBe('1-234');
        expect(formatNumber(12345678, ' ')).toBe('12 345 678');
        expect(warn).toBeCalledWith('delimiter as second argument is deprecated, use options object instead');
        expect(warn).toHaveBeenCalledTimes(2);
    });

    it('converts numbers inside strings (for backward compatibility)', () => {
        const warn = mockWarn();

        expect(formatNumber('test 1234 test')).toBe('test 1\u00A0234 test');
        expect(warn).toBeCalledWith(
            'formatting number inside string is deprecated, ' +
            'use vertis-react/lib/format-text-numbers.js instead.'
        );
        expect(warn).toHaveBeenCalledTimes(1);
    });

    it('replaces first only hyphen to minus', () => {
        expect(formatNumber('-123-345')).toBe('\u2212123-345');
    });
});
/* eslint-enable no-undef */
