import { textToRuPhone } from '../phone';

describe('textToRuPhone', () => {
    it('ignores invalid characters', () => {
        expect(textToRuPhone('!@#$%^&*()_zxcфыв'))
            .toEqual('+7');

        expect(textToRuPhone('+7---999----111 22 33'))
            .toEqual('+79991112233');
    });

    it('converts phones starting with 8', () => {
        expect(textToRuPhone('89991112233'))
            .toEqual('+79991112233');
    });

    it('converts phones without plus sign in the beggining', () => {
        expect(textToRuPhone('79991112233'))
            .toEqual('+79991112233');
    });

    it('converts phones without country code', () => {
        expect(textToRuPhone('9991112233'))
            .toEqual('+79991112233');
    });
});
