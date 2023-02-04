import {expect} from 'chai';
import {medianFilter} from '../../../src/vector_render_engine/util/median_filter';

describe('util/medianFilter', () => {
    it.skip('should filter out noise', () => {
        expect(medianFilter([1, 2, 10, 4, 5, -1, 6]))
            .to.be.deep.eq([1, 2, 4, 4, 5, 5, 6]);
    });

    it.skip('should preserve sharp edges', () => {
        expect(medianFilter([1, 2, 3, 10, 9, 12]))
            .to.be.deep.eq([1, 2, 3, 9, 10, 12]);
    });
});
