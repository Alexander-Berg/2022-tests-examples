import {expect} from 'chai';
import Vector2, * as vec2 from '../../../../../../src/vector_render_engine/math/vector2';
import {splitLineByMaxLength, filterOutEqualNeighboringPoints} from '../../../../../../src/vector_render_engine/content_provider/graphics/content_manager/util/line';

const v = vec2.create;

describe('utils/line', () => {
    describe('splitLineByMaxLength', () => {
        it('should split line', () => {
            const line = generateLineOfLength(8);
            const lines = splitLineByMaxLength(line, 3);
            expect(lines).to.deep.equal([line.slice(0, 3), line.slice(3, 6), line.slice(6, 8)]);
        });

        it('should not make lines of length < 2', () => {
            const line = generateLineOfLength(4);
            const lines = splitLineByMaxLength(line, 3);
            expect(lines).to.deep.equal([line.slice(0, 2), line.slice(2, 4)]);
        });
    });

    describe('filterOutEqualNeighboringPoints', () => {
        it('should filter out equal neighboring points in the middle of the line', () => {
            const line = filterOutEqualNeighboringPoints([v(0, 0), v(1, 1), v(1, 1), v(1, 1), v(2, 2)]);
            expect(line).to.deep.equal([v(0, 0), v(1, 1), v(2, 2)]);
        });

        it('should filter out equal neighboring points at the start of the line', () => {
            const line = filterOutEqualNeighboringPoints([v(0, 0), v(0, 0), v(1, 1)]);
            expect(line).to.deep.equal([v(0, 0), v(1, 1)]);
        });

        it('should filter out equal neighboring points at the end of the line', () => {
            const line = filterOutEqualNeighboringPoints([v(0, 0), v(1, 1), v(1, 1)]);
            expect(line).to.deep.equal([v(0, 0), v(1, 1)]);
        });
    });
});

function generateLineOfLength(n: number): Vector2[] {
    return new Array(n).fill(null).map((_, i) => v(i, i));
}
