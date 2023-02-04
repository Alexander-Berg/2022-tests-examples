import {expect} from 'chai';
import {splitPolyline} from '../../../src/vector_render_engine/util/polyline';
import {create as p} from '../../../src/vector_render_engine/math/vector2';

describe('polyline utils', () => {
    describe('splitting', () => {
        it('should split a simple one segment polyline', () => {
            expect(splitPolyline(
                [p(0, 0), p(4, 0)]
            )).to.be.deep.equal([
                [p(0, 0), p(2, 0)],
                [p(2, 0), p(4, 0)]
            ]);
        });

        it('should not add repetitive split point', () => {
            expect(splitPolyline(
                [p(0, 0), p(2, 0), p(4, 0)]
            )).to.be.deep.equal([
                [p(0, 0), p(2, 0)],
                [p(2, 0), p(4, 0)]
            ]);
        });

        it('should split a non-trivial polyline with correct proportion', () => {
            expect(splitPolyline(
                [p(0, 0), p(2, 2), p(4, 0)],
                0.25
            )).to.be.deep.equal([
                [p(0, 0), p(1, 1)],
                [p(1, 1), p(2, 2), p(4, 0)]
            ]);
        });
    });
});
