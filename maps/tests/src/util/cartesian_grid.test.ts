import {expect} from 'chai';
import {computeSquaresCoveredByConvexPolygon} from '../../../src/vector_render_engine/util/cartesian_grid';
import Vector2, * as vec2 from '../../../src/vector_render_engine/math/vector2';

const comparator = (a: Vector2, b: Vector2) => a.x - b.x || a.y - b.y;

describe('cartesian grid', () => {
    describe('cartesian grid', () => {
        it('should correctly calculate covered squares', () => {
            expect(computeSquaresCoveredByConvexPolygon([
                vec2.create(-2, 0),
                vec2.create(0, 2),
                vec2.create(2, 0),
                vec2.create(0, -2)
            ]).sort(comparator)).to.be.deep.eq([
                vec2.create(-2, -1),
                vec2.create(-2, 0),
                vec2.create(-1, -2),
                vec2.create(-1, -1),
                vec2.create(-1, 0),
                vec2.create(-1, 1),
                vec2.create(0, -2),
                vec2.create(0, -1),
                vec2.create(0, 0),
                vec2.create(0, 1),
                vec2.create(1, -1),
                vec2.create(1, 0)
            ]);

            expect(computeSquaresCoveredByConvexPolygon([
                vec2.create(0, 0),
                vec2.create(1, 0),
                vec2.create(1, 1),
                vec2.create(0, 1)
            ]).sort(comparator)).to.be.deep.eq([
                vec2.create(0, 0)
            ]);

            expect(computeSquaresCoveredByConvexPolygon([
                vec2.create(-1, 1),
                vec2.create(2, 2),
                vec2.create(1.5, 0),
                vec2.create(0.5, -0.5)
            ]).sort(comparator)).to.be.deep.eq([
                vec2.create(-1, 0),
                vec2.create(-1, 1),
                vec2.create(0, -1),
                vec2.create(0, 0),
                vec2.create(0, 1),
                vec2.create(1, -1),
                vec2.create(1, 0),
                vec2.create(1, 1)
            ]);
        });
    });

});
