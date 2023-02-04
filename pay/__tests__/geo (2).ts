import { getDistance, getPointByDistance, getMidpoint } from '../geo';

const pointA = { latitude: 56.0148961309862, longitude: 38.025412700251046 };
const pointB = { latitude: 57.04221966044392, longitude: 40.29406283804002 };

describe('Geo lib', function () {
    test('should return distance', function () {
        expect(getDistance(pointA, pointB)).toEqual(179.9999999999999);
    });

    test('should return midpoint', function () {
        expect(getMidpoint(pointA, pointB)).toEqual({
            latitude: 56.53372284814389,
            longitude: 39.14435452196181,
        });
    });

    test('should return distance (angle=45, dist=50)', function () {
        expect(getPointByDistance(pointA, 45, 50)).toEqual({
            latitude: 56.331533127879496,
            longitude: 38.59894806821296,
        });
    });

    test('should return distance (angle=225, dist=50)', function () {
        expect(getPointByDistance(pointA, 225, 50)).toEqual({
            latitude: 55.695641533068674,
            longitude: 37.461242462539396,
        });
    });
});
