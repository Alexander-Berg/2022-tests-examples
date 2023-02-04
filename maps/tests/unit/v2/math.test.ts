import {expect} from 'chai';
import {cycleRestrict, restrict, isPowOf2} from 'src/v2/math';

describe('math', () => {
    describe('cycleRestrict()', () => {
        it('should restrict a value to the given cycled range', () => {
            expect(cycleRestrict(-250, -180, 180)).to.equal(110);
            expect(cycleRestrict(-300, -180, 180)).to.equal(60);
            expect(cycleRestrict(190, -180, 180)).to.equal(-170);
        });
    });

    describe('restrict()', () => {
        it('should restrict a value to the given range', () => {
            expect(restrict(-250, -180, 180)).to.equal(-180);
            expect(restrict(250, -180, 180)).to.equal(180);
        });
    });

    describe('isPowOf2()', () => {
        it('should return true if number is power of 2', () => {
            expect(isPowOf2(1)).to.be.true;
            expect(isPowOf2(2)).to.be.true;
            expect(isPowOf2(8)).to.be.true;

            expect(isPowOf2(0)).to.be.false;
            expect(isPowOf2(6)).to.be.false;
            expect(isPowOf2(-1)).to.be.false;
        });
    });
});
