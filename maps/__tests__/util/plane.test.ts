import {vec3} from '../../../webgl_toolkit/util/vector';
import {isPointInsideConvexPolygon, rayFromOriginPlaneIntersection} from '../../util/plane';

describe('plane utils', () => {
    it('rayFromOriginPlaneIntersection', () => {
        expect(rayFromOriginPlaneIntersection(
            vec3.create(0, 0, -1),
            vec3.create(0, 0, 1),
            vec3.create(0, 0, -1)
        )).toBeDeepCloseTo({x: 0, y: 0, z: -1});

        expect(rayFromOriginPlaneIntersection(
            vec3.normalize(vec3.create(1, 1, 0)),
            vec3.normalize(vec3.create(-1, -1, 0)),
            vec3.create(1, 1, 0)
        )).toBeDeepCloseTo({x: 1, y: 1, z: 0});

        expect(rayFromOriginPlaneIntersection(
            vec3.normalize(vec3.create(1, 3, 0)),
            vec3.normalize(vec3.create(-1, -1, 0)),
            vec3.create(1, 1, 0)
        )).toBeDeepCloseTo({x: 0.5, y: 1.5, z: 0});

        expect(rayFromOriginPlaneIntersection(
            vec3.normalize(vec3.create(1, 1, 0)),
            vec3.normalize(vec3.create(-1, -1, -1)),
            vec3.create(1, 1, 0)
        )).toBeDeepCloseTo({x: 1, y: 1, z: 0});

        expect(rayFromOriginPlaneIntersection(
            vec3.normalize(vec3.create(-1, -1, 0)),
            vec3.normalize(vec3.create(-1, -1, -1)),
            vec3.create(1, 1, 0)
        )).toBeUndefined();

        expect(rayFromOriginPlaneIntersection(
            vec3.normalize(vec3.create(0, -1, 0)),
            vec3.normalize(vec3.create(0, 0, 1)),
            vec3.create(0, 0, -1)
        )).toBeUndefined();

        expect(rayFromOriginPlaneIntersection(
            vec3.normalize(vec3.create(0, -1, 0)),
            vec3.normalize(vec3.create(0, 0, -1)),
            vec3.create(0, 0, -1)
        )).toBeUndefined();
    });

    it('isPointInsideConvexPolygon', () => {
        const square = [
            vec3.create(-1, -1, 0),
            vec3.create(-1, 1, 0),
            vec3.create(1, 1, 0),
            vec3.create(1, -1, 0)
        ];
        expect(isPointInsideConvexPolygon(
            vec3.create(0, 0, 0),
            square,
            vec3.create(0, 0, 1)
        )).toBeTruthy();

        expect(isPointInsideConvexPolygon(
            vec3.create(0, 0, 0),
            square,
            vec3.create(0, 0, -1)
        )).toBeTruthy();

        expect(isPointInsideConvexPolygon(
            vec3.create(2, 0, 0),
            square,
            vec3.create(0, 0, 1)
        )).toBeFalsy();
    });

});
