import { getValue } from '../getValue';

describe('Badge', () => {
    it('getValue', () => {
        expect(getValue('')).toBe('');
        expect(getValue(null)).toBe('');
        expect(getValue('А')).toBe('а');
        expect(getValue('а')).toBe('а');
        expect(getValue('3D-Тур')).toBe('3D-Тур');
        expect(getValue('a4')).toBe('a4');
        expect(getValue('А4')).toBe('А4');
        expect(getValue('НДС')).toBe('НДС');
        expect(getValue('Долгосрочная аренда')).toBe('долгосрочная аренда');
        expect(getValue('В проекте')).toBe('в проекте');
    });
});
