import {align, isPowOf2} from '../../util/pow_of_2';

describe('pow_of_2', () => {
    it('isPowOf2', () => {
        expect(isPowOf2(512)).toBeTruthy();
        expect(isPowOf2(42)).toBeFalsy();
        expect(isPowOf2(-42)).toBeFalsy();
    });

    it('align', () => {
        const n = 0x80000000 * Math.random() | 0;
        expect(align(n, -16) % 16).toEqual(0);
    });
});
