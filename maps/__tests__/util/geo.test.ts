import {geo2normalizedCartesian, geo2cartesian, EARTH_RADIUS} from '../../util/geo';

const R = EARTH_RADIUS;
const PI = Math.PI;
const SQRT1_2 = Math.SQRT1_2;

describe('geo utils', () => {
    it('geo to normalized cartesian coordinates conversion', () => {
        expect(geo2normalizedCartesian({lon: 0, lat: 0, alt: 0})).toBeDeepCloseTo({x: 0, y: 0, z: 1});
        expect(geo2normalizedCartesian({lon: PI, lat: 0, alt: 0})).toBeDeepCloseTo({x: 0, y: 0, z: -1});
        expect(geo2normalizedCartesian({lon: 0, lat: PI / 2, alt: 0})).toBeDeepCloseTo({x: 0, y: 1, z: 0});
        expect(geo2normalizedCartesian({lon: PI, lat: PI / 2, alt: 0})).toBeDeepCloseTo({x: 0, y: 1, z: 0});
        expect(geo2normalizedCartesian({lon: -PI / 4, lat: 0, alt: 0}))
            .toBeDeepCloseTo({x: -SQRT1_2, y: 0, z: SQRT1_2});
    });

    it('geo to cartesian coordinates conversion', () => {
        expect(geo2cartesian({lon: 0, lat: 0, alt: 0})).toBeDeepCloseTo({x: 0, y: 0, z: R});
        expect(geo2cartesian({lon: 0, lat: 0, alt: 10})).toBeDeepCloseTo({x: 0, y: 0, z: R + 10});
        expect(geo2cartesian({lon: 0, lat: PI / 2, alt: 10})).toBeDeepCloseTo({x: 0, y: R + 10, z: 1}, 10 ** -10);
    });
});
