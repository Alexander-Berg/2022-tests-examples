import {expect} from 'chai';
import {bearing} from '../src/geo/bearing';
import {convertPolarToCartesian} from '../src/math/convert-polar-to-cartesian';
import {arePointsEqual} from '../src/math/are-points-equal';
import {centerAndSpanToBounds} from '../src/geo/center-and-span-to-bounds';
import {boundsToCenterAndSpan} from '../src/geo/bounds-to-center-and-span';
import {CenterAndSpan, Bounds, Point} from '../src/types';
import {cycleRestrict} from '../src/math/cycle-restrict';
import {perpendicularDistance} from '../src/math/perpendicular-distance';
import {simplifyPolyline} from '../src/pixels/simplify-polyline';
import {findPointProjectionOnLine} from '../src/math/find-point-projection-on-line';

describe('math', () => {
    describe('bearing', () => {
        it('should calculate correctly', () => {
            const pointsBearing = bearing([37.60322102, 55.73953671], [37.614829, 55.739742]);
            expect(pointsBearing.toFixed(6)).to.be.equal('88.195844');
        });
    });

    describe('convertPolarToCartesian', () => {
        it('should calculate', () => {
            const coords = convertPolarToCartesian([45, 46], 45, -25);
            expect(arePointsEqual(coords, [85.783850, 26.982178])).to.be.true;
        });
    });

    describe('centerAndSpanToBounds', () => {
        it('should calculate correct bounds', () => {
            const bounds = centerAndSpanToBounds({center: [37, 55], span: [0.5, 0.5]});
            expect(arePointsEqual([36.75, 54.749221074893484], bounds[0])).to.be.true;
            expect(arePointsEqual([37.25, 55.249221074893484], bounds[1])).to.be.true;
        });
    });

    describe('boundsToCenterAndSpan', () => {
        it('should calculate correct center and span', () => {
            const result = boundsToCenterAndSpan(
                [[60.6470495, 54.77889741075401], [62.275772499999995, 55.55010941075401]]
            );
            expect(arePointsEqual([61.461411, 55.166368], result.center)).to.be.true;
            expect(arePointsEqual([1.628723, 0.771212], result.span)).to.be.true;
        });
    });

    describe('centerAndSpanToBounds', () => {
        it('should be inverse for boundsToCenterAndSpan', () => {
            const options: CenterAndSpan = {
                center: [37, 55],
                span: [0.5, 0.5]
            };
            const result = boundsToCenterAndSpan(centerAndSpanToBounds(options));
            expect(arePointsEqual(result.center, options.center)).to.be.true;
            expect(arePointsEqual(result.span, options.span)).to.be.true;
        });
    });

    describe('boundsToCenterAndSpan', () => {
        it('should be inverse for centerAndSpanToBounds', () => {
            const bounds: Bounds = [[27.4, 53.8], [27.72, 54]];
            const options = boundsToCenterAndSpan(bounds);
            const result = centerAndSpanToBounds(options);
            expect(arePointsEqual(result[0], bounds[0])).to.be.true;
            expect(arePointsEqual(result[1], bounds[1])).to.be.true;
        });
    });

    describe('cycleRestrict', () => {
        it('should calculate', () => {
            expect(cycleRestrict(100, 0, 214)).to.be.equal(100);
            expect(cycleRestrict(Infinity, 0, 214)).to.be.equal(214);
            expect(cycleRestrict(-Infinity, 0, 214)).to.be.equal(0);
        });
    });

    describe('perpendicularDistance', () => {
        it('should calculate', () => {
            expect(perpendicularDistance([1, 1], [0, 0], [0, 1])).to.be.equal(1);
            expect(perpendicularDistance([1, 1], [0, 1], [1, 0])).to.be.equal(Math.sqrt(2) / 2);
        });
    });

    describe('simplifyPolyline', () => {
        it('should simplify three points to two points', () => {
            const points: Point[] = [[10, 10], [20, 20], [30, 30]];
            expect(simplifyPolyline(points, {eps: 0.5}).length).to.be.equal(2);
        });

        it('shouldn\'t simplify three points to two points', () => {
            const points: Point[] = [[10, 10], [20, 20], [30, 30]];
            expect(simplifyPolyline(points, {eps: 0.1}).length).to.be.equal(3);
        });
    });

    describe('findPointProjectionOnLine', () => {
        const polyline: Point[] = [
            [55.760737, 37.650781],
            [55.761497, 37.599831],
            [55.797317, 37.606902],
            [55.712112, 37.620555]
        ];
        it('should find the same point', () => {
            const foundPoint = findPointProjectionOnLine(polyline, polyline[1], 3, 10);
            expect(
                foundPoint ? [foundPoint[0].toFixed(6), foundPoint[1].toFixed(6)].map(Number) : undefined
            ).to.deep.equal(
                polyline[1]
            );
        });
        it('should find a point nearby', () => {
            const point: Point = [55.711238, 37.621238];
            const foundPoint = findPointProjectionOnLine(polyline, point, 3, 10);
            expect(
                foundPoint ? [foundPoint[0].toFixed(6), foundPoint[1].toFixed(6)].map(Number) : undefined
            ).to.deep.equal(
                [55.711153, 37.620709]
            );
        });
        it('should not find anything', () => {
            const point: Point = [55.999999, 37.999999];
            const foundPoint = findPointProjectionOnLine(polyline, point, 3, 10);
            expect(foundPoint).to.be.equal(undefined);
        });
    });
});
