import {expect} from 'chai';
import 'tests/chai-extensions';
import * as projection from 'src/v2/wgs84-mercator';

describe('wgs84Mercator', () => {
    it('should return the same geo coordinates when project/unproject', () => {
        const lngLat = [37.62, 55.75];

        expect(projection.fromPixels(projection.toPixels(lngLat, 23), 23))
            .to.roughlyEqualPoint(lngLat);
        expect(projection.fromPixels(projection.toPixels(lngLat, 10), 10))
            .to.roughlyEqualPoint(lngLat);
    });

    describe('toPixels()', () => {
        it('should not handle zero zoom as missing parameter', () => {
            const lngLat = [37.62, 55.75];

            expect(projection.toPixels(lngLat, 0))
                .to.not.roughlyEqualPoint(projection.toPixels(lngLat));
        });
    });

    describe('fromPixels()', () => {
        it('should not handle zero zoom as missing parameter', () => {
            const pixels = [640, 480];

            expect(projection.fromPixels(pixels, 0))
                .to.not.roughlyEqualPoint(projection.fromPixels(pixels));
        });
    });
});
