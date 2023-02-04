import {
    normalizeBounds,
    normalizePoint,
    getBoundsByPoints,
    getCenterByPoints,
    expandBounds,
    isInnerBounds,
    isInnerPoint,
} from '../geo';

const P = (latitude: number, longitude: number) => ({ latitude, longitude });
const B = (sw: [number, number], ne: [number, number]) => ({ sw: P(...sw), ne: P(...ne) });

const pointA = { latitude: 56.0148961309862, longitude: 38.025412700251046 };
const pointB = { latitude: 57.04221966044392, longitude: 40.29406283804002 };

describe('Geo helper', function () {
    test('should return normalized point', function () {
        expect(normalizePoint(pointA)).toEqual({
            latitude: 56.014896131,
            longitude: 38.0254127003,
        });
    });

    test('should return normalized bounds', function () {
        expect(normalizeBounds({ ne: pointB, sw: pointA })).toEqual({
            ne: { latitude: 57.0422196604, longitude: 40.294062838 },
            sw: { latitude: 56.014896131, longitude: 38.0254127003 },
        });
    });

    test('should return bounds by points', function () {
        const points = [P(10, 5), P(10, 25), P(20, 10), P(30, 20)];

        expect(getBoundsByPoints(points)).toEqual(B([10, 5], [30, 25]));
    });

    test('should return center by points', function () {
        const points = [P(10, 5), P(15, 10), P(10, 25), P(20, 10), P(30, 20)];

        expect(getCenterByPoints(points)).toEqual(P(15, 10));
    });

    test('should return expanded bounds', function () {
        const bounds = B([55.4132300248, 37.3711396142], [55.4927390341, 37.618331997]);
        const exBoundsA = expandBounds(bounds, 150);
        const exBoundsB = expandBounds(bounds, 20);

        expect(exBoundsA).toEqual(
            B([55.0700900868, 36.3053190627], [55.8360035043, 38.6839034323]),
        );
        expect(exBoundsB).toEqual(
            B([55.4019859011, 37.3360237793], [55.5041076901, 37.6531987157]),
        );
    });

    test('should return inner bounds check', function () {
        const innerBounds = B([6, 6], [8, 8]);
        const outerBoundsA = B([5, 5], [9, 9]);
        const outerBoundsB = B([5, 5], [9, 7]);

        expect(isInnerBounds(innerBounds, outerBoundsA)).toBe(true);
        expect(isInnerBounds(innerBounds, innerBounds)).toBe(true);
        expect(isInnerBounds(innerBounds, outerBoundsB)).toBe(false);
    });

    test('should return inner points check', function () {
        const bounds = B([6, 6], [8, 8]);
        const pointA = P(6, 7);
        const pointB = P(5, 9);

        expect(isInnerPoint(bounds, pointA)).toBe(true);
        expect(isInnerPoint(bounds, pointB)).toBe(false);
    });
});
