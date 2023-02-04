import {expect} from 'chai';
import {Point} from '../src/types';
import {arePointsEqual} from '../src/math/are-points-equal';
import {geoToPixels} from '../src/pixels/geo-to-pixels';
import {pointToString} from '../src/geo/point-to-string';
import {stringToPoint} from '../src/geo/string-to-point';
import {pixelsToGeo} from '../src/pixels/pixels-to-geo';
import {COMPARISON_TOLERANCE} from '../src/constants';

describe('point', () => {
    describe('pointToString()', () => {
        it('should convert point to string with same coordinate order', () => {
            expect(pointToString([37, 55])).to.be.equal('37.000000,55.000000');
        });

        it('should convert point to string with reverse coordinate order', () => {
            const point: Point = [37, 55];
            expect(pointToString(point, {reverse: true})).to.be.equal('55.000000,37.000000');
            expect(point).to.be.deep.equal([37, 55], 'original point changed');
        });

        it('should convert point to string with specified number of digits after decimal point', () => {
            expect(pointToString([37.123, 55.123], {fractionDigits: 2})).to.be.equal('37.12,55.12');
            expect(pointToString([37, 55], {fractionDigits: 0})).to.be.equal('37,55');
        });

        it('should convert point with altitude to string', () => {
            expect(pointToString([37, 55, 0])).to.be.equal('37.000000,55.000000');
        });
    });

    describe('stringToPoint()', () => {
        it('should return null for empty string', () => {
            expect(stringToPoint('')).to.be.null;
        });

        it('should return null for string with space characters', () => {
            expect(stringToPoint('   ')).to.be.null;
        });

        it('should return null for point with 1 coordinates', () => {
            expect(stringToPoint('1,')).to.be.null;
            expect(stringToPoint('1')).to.be.null;
        });

        it('should return null for point with more than two coordinates', () => {
            expect(stringToPoint('1,2,3')).to.be.null;
            expect(stringToPoint('1 2 3')).to.be.null;
        });

        it('should return null for not string value', () => {
            expect(stringToPoint(undefined)).to.be.null;
            expect(stringToPoint(null)).to.be.null;
            expect(stringToPoint([])).to.be.null;
        });

        it('should stringToPoint string in "lng,lat" format', () => {
            expect(stringToPoint('37,55')).to.be.deep.equal([37, 55]);
            expect(stringToPoint('37,-55')).to.be.deep.equal([37, -55]);
        });

        it('should stringToPoint string in "lng , lat" format', () => {
            expect(stringToPoint('37, 55')).to.be.deep.equal([37, 55]);
            expect(stringToPoint('-37, -55')).to.be.deep.equal([-37, -55]);
            expect(stringToPoint('37,  55')).to.be.deep.equal([37, 55]);
            expect(stringToPoint('37  ,  55')).to.be.deep.equal([37, 55]);
            expect(stringToPoint('37 , 55')).to.be.deep.equal([37, 55]);
            expect(stringToPoint('37 ,55')).to.be.deep.equal([37, 55]);
        });

        it('should stringToPoint string in "lng lat" format', () => {
            expect(stringToPoint('37 55')).to.be.deep.equal([37, 55]);
            expect(stringToPoint('37  55')).to.be.deep.equal([37, 55]);
            expect(stringToPoint(' 37  55 ')).to.be.deep.equal([37, 55]);
        });

        it('should stringToPoint string with reverse coordinate order', () => {
            expect(stringToPoint('55,37', {reverse: true})).to.be.deep.equal([37, 55]);
            expect(stringToPoint('55, 37', {reverse: true})).to.be.deep.equal([37, 55]);
            expect(stringToPoint('55 37', {reverse: true})).to.be.deep.equal([37, 55]);
        });
    });

    describe('arePointsEqual()', () => {
        it('should return `true` for equal points', () => {
            const x = Math.random();
            const y = Math.random();
            expect(arePointsEqual([x, y], [x, y])).to.be.true;
        });

        it('should return `true` if diff between any coordinates lower than default tolerance', () => {
            const x = 10 * Math.random();
            const y = x + 0.1 * COMPARISON_TOLERANCE;

            expect(arePointsEqual([x, 0], [y, 0])).to.be.true;
            expect(arePointsEqual([0, x], [0, y])).to.be.true;
            expect(arePointsEqual([x, x], [y, y])).to.be.true;
        });

        it('should return `false` if diff between any coordinates greater than default tolerance', () => {
            const x = 10 * Math.random();
            const y = x + 10 * COMPARISON_TOLERANCE;

            expect(arePointsEqual([x, 0], [y, 0])).to.be.false;
            expect(arePointsEqual([0, x], [0, y])).to.be.false;
            expect(arePointsEqual([x, x], [y, y])).to.be.false;
        });

        it('should return `true` if diff between any coordinates lower than given tolerance', () => {
            expect(arePointsEqual([1, 0], [2, 0], 2)).to.be.true;
            expect(arePointsEqual([0, 1], [0, 2], 2)).to.be.true;
            expect(arePointsEqual([1, 1], [2, 2], 2)).to.be.true;
        });

        it('should return `false` if diff between any coordinates greater than given tolerance', () => {
            expect(arePointsEqual([1, 0], [4, 0], 2)).to.be.false;
            expect(arePointsEqual([0, 1], [0, 4], 2)).to.be.false;
            expect(arePointsEqual([1, 1], [4, 4], 2)).to.be.false;
        });

        it('should correct handle zero tolerance', () => {
            const x = 10 * Math.random();
            const y = x + 0.1 * COMPARISON_TOLERANCE;

            expect(arePointsEqual([x, 0], [y, 0], 0)).to.be.false;
            expect(arePointsEqual([0, x], [0, y], 0)).to.be.false;
            expect(arePointsEqual([x, x], [y, y], 0)).to.be.false;
        });
    });

    describe('geoToPixels()', () => {
        it('should convert coordinates correctly', () => {
            expect(geoToPixels([37, 55], 1)).to.be.deep.equal([308.6222222222222, 162.39203123642622]);
        });

        it('should convert incorrect coordinates correctly', () => {
            expect(geoToPixels([-360, -90], 1)).to.be.deep.equal([256, 1955.2357893437836]);
        });
    });

    describe('pixelsToGeo()', () => {
        function increaseCoords(coords: Point, times: number): Point {
            return coords.map((coord) => coord * times) as Point;
        }

        it('should convert coordinates correctly', () => {
            const point: Point = [308.62222222222, 162.392031236426];
            expect(arePointsEqual(pixelsToGeo(point, 1), [37, 55])).to.be.true;
        });

        it('should convert coordinates on different zooms proportionally', () => {
            const point: Point = [123, 123];
            const zoom1 = 2;
            const zoom2 = 5;
            const geo1 = pixelsToGeo(increaseCoords(point, Math.pow(2, zoom1)), zoom1);
            const geo2 = pixelsToGeo(increaseCoords(point, Math.pow(2, zoom2)), zoom2);

            expect(arePointsEqual(geo1, geo2)).to.be.true;
        });
    });
});
