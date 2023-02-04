import {expect} from 'chai';
import {normalizeColor} from 'src/v2/converters/utils';

describe('normalizeColor()', () => {
    it('should return `undefined` for non-string', () => {
        expect(normalizeColor(1)).to.equal(undefined);
        expect(normalizeColor(undefined)).to.equal(undefined);
        expect(normalizeColor(null)).to.equal(undefined);
    });

    it('should return `undefined` for invalid color format', () => {
        expect(normalizeColor('')).to.equal(undefined);
        expect(normalizeColor('#')).to.equal(undefined);
        expect(normalizeColor('ab')).to.equal(undefined);
        expect(normalizeColor('#ab')).to.equal(undefined);
    });

    it('should normalize hex rgb', () => {
        expect(normalizeColor('abc')).to.equal('aabbccff');
        expect(normalizeColor('#abc')).to.equal('aabbccff');
    });

    it('should normalize rgba', () => {
        expect(normalizeColor('abcd')).to.equal('aabbccdd');
        expect(normalizeColor('#abcd')).to.equal('aabbccdd');
    });

    it('should normalize rrggbb', () => {
        expect(normalizeColor('aabbcc')).to.equal('aabbccff');
        expect(normalizeColor('#aabbcc')).to.equal('aabbccff');
    });

    it('should not change rrggbbaa', () => {
        expect(normalizeColor('aabbccdd')).to.equal('aabbccdd');
        expect(normalizeColor('#aabbccdd')).to.equal('aabbccdd');
    });
});
