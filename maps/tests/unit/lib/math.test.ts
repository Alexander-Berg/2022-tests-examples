import {expect} from 'chai';
import {angleDifference} from 'app/lib/math';

describe('angleDifference()', () => {
    it('should compute correct result for small angles', () => {
        expect(angleDifference(30, 40)).to.equal(-10);
        expect(angleDifference(40, 30)).to.equal(10);
    });

    it('should return shortest route between angles', () => {
        expect(angleDifference(350, 10)).to.equal(-20);
        expect(angleDifference(10, 350)).to.equal(20);
    });

    it('should return 180 for 180 difference', () => {
        expect(angleDifference(180, 360)).to.equal(180);
        expect(angleDifference(360, 180)).to.equal(180);
    });

    it('should normalize input angles', () => {
        expect(angleDifference(80, 810)).to.equal(-10);
        expect(angleDifference(810, 80)).to.equal(10);
    });

    it('should return 0 for the same angles', () => {
        expect(angleDifference(15, 15)).to.equal(0);
    });
});
