import {expect} from 'chai';
import {
    Point,
    Cycled,
    Bounds
} from '../src/types';
import {COMPARISON_TOLERANCE} from '../src/constants';
import {getGeoBounds} from '../src/geo/get-geo-bounds';
import {getPixelsBounds} from '../src/pixels/get-pixels-bounds';
import {getPixelIntersectingBounds} from '../src/pixels/get-pixel-intersecting-bounds';
import {geoBoundsToPixelsBounds} from '../src/pixels/geo-bounds-to-pixels-bounds';
import {pixelsBoundsToGeoBounds} from '../src/pixels/pixels-bounds-to-geo-bounds';
import {removeMarginFromBounds} from '../src/geo/remove-margin-from-bounds';

const DEFAULT_ZOOM = 32;
describe('bounds', () => {
    describe('getGeoBounds', () => {
        it('should calculate correctly', () => {
            const coords: Point[] = [
                [55.760737, 37.650781],
                [55.761497, 37.599831],
                [55.797317, 37.606902],
                [55.712112, 37.620555],
                [55.837141, 37.624076],
                [55.762047, 37.557258],
                [55.771018, 37.628568],
                [55.756037, 37.621453],
                [55.710398, 37.590219]
            ];
            const bounds = getGeoBounds(coords);

            const fixedBounds = bounds.map((bound) => [bound[0].toFixed(6), bound[1].toFixed(6)]);
            expect(fixedBounds).to.deep.equal([['55.710398', '37.650781'], ['55.837141', '37.557258']]);
        });
    });

    describe('getPixelsBounds', () => {
        it('should calculate correctly', () => {
            const coords: Point[] = [
                [720060199179.2761, 426179643202.4318],
                [720062520370.4904, 426375290193.9614],
                [720171921777.4541, 426348145773.1416],
                [719911688754.5522, 426295726759.03986],
                [720293552197.0779, 426282206738.20166],
                [720064200179.9218, 426538666078.51917],
                [720091599398.8461, 426264957310.59546],
                [720045844444.1357, 426292278658.2951],
                [719906453857.5244, 426412184929.73303]
            ];

            const isCycled: Cycled = [true, false];
            const worldSize = Math.pow(2, DEFAULT_ZOOM + 8);
            const bounds = getPixelsBounds(
                coords,
                worldSize,
                isCycled
            );

            expect(bounds).to.deep.equal([
                [719906453857.5244, 426179643202.4318],
                [720293552197.0779, 426538666078.51917]
            ]);
        });
    });

    describe('getPixelIntersectingBounds', () => {
        it('should calculate correctly for intersecting bounds', () => {
            const bounds1: Bounds = [
                [10, 30],
                [20, 40]
            ];

            const bounds2: Bounds = [
                [15, 35],
                [25, 45]
            ];

            const intersectingBounds = getPixelIntersectingBounds(bounds1, bounds2);

            expect(intersectingBounds).to.deep.equal([
                [15, 35],
                [20, 40]
            ]);
        });

        it('should return null for non-intersecting bounds', () => {
            const bounds1: Bounds = [
                [10, 10],
                [20, 20]
            ];

            const bounds2: Bounds = [
                [30, 30],
                [40, 40]
            ];

            const intersectingBounds = getPixelIntersectingBounds(bounds1, bounds2);

            expect(intersectingBounds).to.equal(null);
        });
    });

    describe('pixelsBoundsToGeoBounds', () => {
        it('should be opposite of geoBoundsToPixelsBounds', () => {
            const bounds: Bounds = [
                [30.3254165, 59.90946294],
                [30.3336275, 59.90533894]
            ];
            const result = pixelsBoundsToGeoBounds(geoBoundsToPixelsBounds(bounds, DEFAULT_ZOOM), DEFAULT_ZOOM);

            expect(result[0][0]).to.be.closeTo(bounds[0][0], COMPARISON_TOLERANCE);
            expect(result[0][1]).to.be.closeTo(bounds[0][1], COMPARISON_TOLERANCE);
            expect(result[1][0]).to.be.closeTo(bounds[1][0], COMPARISON_TOLERANCE);
            expect(result[1][1]).to.be.closeTo(bounds[1][1], COMPARISON_TOLERANCE);
        });

        it('should preserve topology (because [p1, p2] is not the same as [p2, p1])', () => {
            const bounds: Bounds = [
                [13.683785499999999, 75.99052769991785],
                [-174.6729755, 35.290400699917846]
            ];
            const result = pixelsBoundsToGeoBounds(geoBoundsToPixelsBounds(bounds, DEFAULT_ZOOM), DEFAULT_ZOOM);

            expect(result[0][0]).to.be.closeTo(bounds[0][0], COMPARISON_TOLERANCE);
            expect(result[0][1]).to.be.closeTo(bounds[0][1], COMPARISON_TOLERANCE);
            expect(result[1][0]).to.be.closeTo(bounds[1][0], COMPARISON_TOLERANCE);
            expect(result[1][1]).to.be.closeTo(bounds[1][1], COMPARISON_TOLERANCE);
        });
    });

    describe('removeMarginFromBounds', () => {
        it('should normalize bounds', () => {
            const bounds: Bounds = [
                [13.683785499999999, 75.99052769991785],
                [-174.6729755, 35.290400699917846]
            ];
            const result = removeMarginFromBounds(bounds, DEFAULT_ZOOM, [70, 50, 50, 50]);

            expect(result).to.deep.equal([
                [-174.6729754836291, 35.29040068652103],
                [13.683785483629094, 75.99052770546635]
            ]);
        });
    });
});
