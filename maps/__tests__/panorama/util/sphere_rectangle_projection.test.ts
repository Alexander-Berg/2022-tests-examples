import {extent2} from '../../../../webgl_toolkit/util/extent';
import {rect2sphere, sphere2rect} from '../../../panorama/util/sphere_rectangle_projection';

describe('panorama utils', () => {
    const SIZE = extent2.create(20, 10);

    it('sphere2rect', () => {
        expect(sphere2rect(SIZE, {x: 0, y: 0})).toBeDeepCloseTo({x: 0, y: 5});
        expect(sphere2rect(SIZE, {x: 0, y: Math.PI / 2})).toBeDeepCloseTo({x: 0, y: 0});
        expect(sphere2rect(SIZE, {x: Math.PI, y: 0})).toBeDeepCloseTo({x: 10, y: 5});
        expect(sphere2rect(SIZE, {x: -Math.PI, y: 0})).toBeDeepCloseTo({x: -10, y: 5});
        expect(sphere2rect(SIZE, {x: Math.PI / 2, y: 0})).toBeDeepCloseTo({x: 5, y: 5});
    });

    it('rect2sphere', () => {
        expect(rect2sphere(SIZE, {x: 0, y: 5})).toBeDeepCloseTo({x: 0, y: 0});
        expect(rect2sphere(SIZE, {x: 0, y: 0})).toBeDeepCloseTo({x: 0, y: Math.PI / 2});
        expect(rect2sphere(SIZE, {x: 10, y: 5})).toBeDeepCloseTo({x: Math.PI, y: 0});
        expect(rect2sphere(SIZE, {x: -10, y: 5})).toBeDeepCloseTo({x: -Math.PI, y: 0});
        expect(rect2sphere(SIZE, {x: 5, y: 5})).toBeDeepCloseTo({x: Math.PI / 2, y: 0});
    });

});
