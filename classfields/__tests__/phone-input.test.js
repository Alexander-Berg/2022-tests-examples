import { getPhoneInputDiff, correctUserPhoneInput } from '../PhoneInputLogic';

describe('PhoneInput', () => {
    describe('getPhoneInputDiff', () => {
        it('not returns diff if prev number is empty', () => {
            expect(getPhoneInputDiff('', '+71234567890')).toEqual({});
        });

        it('not returns diff if user delete symbol from number', () => {
            expect(getPhoneInputDiff('+71234567890', '+7123456789')).toEqual({});
        });

        it('not returns diff if user delete few symbols from number', () => {
            expect(getPhoneInputDiff('+71234567890', '+71234567')).toEqual({});
        });

        it('not returns diff if numbers are equal', () => {
            expect(getPhoneInputDiff('+71234567890', '+71234567890')).toEqual({});
        });

        it('returns correct diff after user inputs one symbol at end', () => {
            expect(getPhoneInputDiff('+7123456789', '+71234567890')).toEqual({
                index: 11,
                value: '0'
            });
        });

        it('returns correct diff after user inputs one symbol in middle', () => {
            expect(getPhoneInputDiff('+7123467890', '+71234567890')).toEqual({
                index: 6,
                value: '5'
            });
        });

        it('returns correct diff after user inputs one symbol in beginning', () => {
            expect(getPhoneInputDiff('+7123456789', '0+7123456789')).toEqual({
                index: 0,
                value: '0'
            });
        });

        it('returns correct diff after user inputs few symbols', () => {
            expect(getPhoneInputDiff('+71237890', '+71234567890')).toEqual({
                index: 5,
                value: '456'
            });
        });
    });

    describe('correctUserPhoneInput', () => {
        it('accepts undefined as parameter', () => {
            expect(correctUserPhoneInput(undefined, undefined)).toBe('');
        });

        it('returns next number as is if there isn\'t diff', () => {
            expect(correctUserPhoneInput('+7123', '+7123')).toBe('+7123');
        });

        it('corrects user input before plus sign', () => {
            expect(correctUserPhoneInput('+7123', '0+7123')).toBe('+71230');
        });

        it('corrects user input between plus and country code digit', () => {
            expect(correctUserPhoneInput('+7123', '+07123')).toBe('+71230');
        });

        it('corrects user input between plus and country code digit, if input value equals to code digit', () => {
            expect(correctUserPhoneInput('+7123', '+77123')).toBe('+71237');
        });

        it('cleans all non digit symbols', () => {
            expect(correctUserPhoneInput('+7123', '++7123')).toBe('+7123');
        });
    });
});
