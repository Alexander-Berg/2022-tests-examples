import {isPowOf2, align} from '../../../src/vector_render_engine/util/pow_of_2';
import {expect} from 'chai';

describe('util/pow_of_2', () => {
    describe('isPowOf2', () => {
        it('should return `true` for POT numbers', () => {
            expect(isPowOf2(512)).to.be.true;
        });

        it('should return `false` for NPOT number', () => {
            expect(isPowOf2(42)).to.be.false;
        });

        it('should return `false` for negative numbers', () => {
            expect(isPowOf2(-42)).to.be.false;
        });
    });

    describe('align', () => {
        it('should return multiple of alignment', () => {
            const n = 0x80000000 * Math.random() | 0;
            expect(align(n, -16) % 16).to.be.eq(0);
        });
    });
});
