import euclid from '../src/euclid';
import Line from '../src/euclid-line';
import { arraysAreDeepEqual } from './utils';

describe('euclid', function() {
    var index, srcVertices, resVertices;

    describe('alignAngles', function() {
        it('should return null if it fails', function() {
            srcVertices = [[1, 1]];
            expect(euclid.alignAngles(srcVertices)).toBeNull();

            srcVertices = [[1, 1], [2, 2], [3, 3]];
            expect(euclid.alignAngles(srcVertices)).toBeNull();
        });

        it('should simplify polygons', function() {
            srcVertices = [[0, 0], [.5,.5], [1, 1], [0, 1], [0, 0]];
            resVertices = [[0, 0], [1, 1], [0, 1], [0, 0]];

            expect(arraysAreDeepEqual(euclid.alignAngles(srcVertices), resVertices)).toBe(true);
        });

        it('should align angles', function() {
            srcVertices = [[0, 0], [15, 0], [17, 17], [0, 0]];
            resVertices = [[0, 0], [16, 0], [16, 16], [0, 0]];

            expect(arraysAreDeepEqual(euclid.alignAngles(srcVertices), resVertices)).toBe(true);
        });
    });

    describe('alignAngle', function() {
        it('should return null in case alignment isn\'t appropriate', function() {
            srcVertices = [[0, 0], [1, 0], [1, 1]];

            index = 0;
            expect(euclid.alignAngle(srcVertices, index)).toBeNull();

            index = 2;
            expect(euclid.alignAngle(srcVertices, index)).toBeNull();
        });

        it('should not modify angle if it\'s already ok', function() {
            srcVertices = [[0, 0], [1, 0], [1, 1]];

            index = 1;
            expect(euclid.alignAngle(srcVertices, index)).toEqual(srcVertices);
        });

        it('should transform angle to 90deg', function() {
            srcVertices = [[0, 0], [1, 0.6], [2, 0]];
            resVertices = [[0, 0], [1, 1], [2, 0]];

            index = 1;
            expect(arraysAreDeepEqual(euclid.alignAngle(srcVertices, index), resVertices)).toBe(true);
        });

        it('should work properly with closed path', function() {
            srcVertices = [[0.1, 0.1], [1, 0], [0, 1], [0.1, 0.1]];
            resVertices = [[0, 0], [1, 0], [0, 1], [0, 0]];

            index = 0;
            expect(arraysAreDeepEqual(euclid.alignAngle(srcVertices, index), resVertices)).toBe(true);

            index = 3;
            expect(arraysAreDeepEqual(euclid.alignAngle(srcVertices, index), resVertices)).toBe(true);
        });
    });

    describe('roundAngle', function() {
        var params = {
            radius : 0.2,
            maxAngle : Math.PI / 6
        };

        it('should return null in case rounding isn\'t appropriate', function() {
            srcVertices = [[0, 0], [1, 0.2], [2, 0]];
            expect(euclid.roundAngle(srcVertices, 0, params)).toBe(null);
            expect(euclid.roundAngle(srcVertices, 2, params)).toBe(null);
        });

        it('should return null if angle is already ok', function() {
            srcVertices = [[0, 0], [1, 0.1], [2, 0]];
            expect(euclid.roundAngle(srcVertices, 1, params)).toBe(null);
        });

        it('should return null if one of segments is too short', function() {
            srcVertices = [[0, 0], [0.1, 0], [0.1, 2]];
            expect(euclid.roundAngle(srcVertices, 1, params)).toBe(null);
        });

        it('should return vertices without sharp angles', function() {
            srcVertices = [[0, 3], [1, 2], [0, 1]];
            resVertices = euclid.roundAngle(srcVertices, 1, params);

            var hasSharpAngle = false;

            for(var i = 1; i < resVertices.length - 1; i++) {
                var angle1 = Line.createFromPoints(resVertices[i - 1], resVertices[i]).getAngle(),
                    angle2 = Line.createFromPoints(resVertices[i], resVertices[i + 1]).getAngle(),
                    delta = Math.abs(angle2 - angle1);

                delta > Math.PI && (delta = 2 * Math.PI - delta);
                hasSharpAngle = hasSharpAngle || delta - params.maxAngle > 1e-6;
            }
            expect(hasSharpAngle).toBe(false);
        });

        it('should drop too close edge points', function() {
            srcVertices = [[0, .22], [0, 0], [.22, 0]];
            resVertices = euclid.roundAngle(srcVertices, 1, params);

            expect(resVertices.length).toBe(4);
        });
    });

    describe('getClosestPointOnGeometry', function() {
        it('should work properly with polyline geometry', function() {
            srcVertices = [[0, 0], [1, 0], [1, 1], [0, 1], [0, 0]];

            expect(arraysAreDeepEqual(euclid.getClosestPointOnGeometry([0.5, 0.4], srcVertices), [0.5, 0])).toBe(true);
            expect(arraysAreDeepEqual(euclid.getClosestPointOnGeometry([2, 0.5], srcVertices), [1, 0.5])).toBe(true);
            expect(arraysAreDeepEqual(euclid.getClosestPointOnGeometry([2, 2], srcVertices), [1, 1])).toBe(true);
        });

        it('should work properly with polygon geometry', function() {
            srcVertices = [
                [[0, 0], [3, 0], [3, 3], [0, 3], [0, 0]],
                [[1, 1], [2, 1], [2, 2], [1, 2], [1, 1]]
            ];

            expect(arraysAreDeepEqual(euclid.getClosestPointOnGeometry([-1, 1.5], srcVertices), [0, 1.5])).toBe(true);
            expect(arraysAreDeepEqual(euclid.getClosestPointOnGeometry([1.5, 1.7], srcVertices), [1.5, 2])).toBe(true);
        });

        it('should work properly with \'guideLine\' option', function() {
            srcVertices = [[[0, 0], [1, 0], [1, 1], [0, 1], [0, 0]]];

            expect(
                arraysAreDeepEqual(
                    euclid.getClosestPointOnGeometry([0.5, 2], srcVertices, { guideLine : [[1, 0], [0, 1]] }),
                    [0, 1])
            ).toBe(true);
        });

        it('should work properly with \'vertexSnappingArea\' option', function() {
            srcVertices = [[[0, 0], [1, 0], [1, 1], [0, 1], [0, 0]]];

            expect(
                arraysAreDeepEqual(
                    euclid.getClosestPointOnGeometry(
                        [0.9, 2],
                        srcVertices,
                        { vertexSnappingArea : 0.1 }),
                    [1, 1])
            ).toBe(true);
        });
    });

    describe('snapToRightAngle', function() {
        it('should return null if geometry is invalid', function() {
            expect(euclid.snapToRightAngle([], 0)).toBeNull();
        });

        it('should return null if criteria aren\'t met', function() {
            var precision = 0.01;

            srcVertices = [[0, 0], [1, 0], [0.9, 0.9], [0, 0]];
            index = 2;

            expect(euclid.snapToRightAngle(srcVertices, index, { precision : precision })).toBeNull();
        });

        it('should work properly with polygon geometry', function() {
            var precision = 0.2;

            srcVertices = [[0, 0], [1, 0], [0.9, 0.9], [0, 1], [0, 0]];
            index = 2;

            expect(
                arraysAreDeepEqual(
                    euclid.snapToRightAngle(srcVertices, index, { precision : precision }),
                    [1, 1])
            ).toBe(true);

            srcVertices = [[0, 0], [1, 0], [1.1, 0.1], [0, 0]];
            index = 2;

            expect(
                arraysAreDeepEqual(
                    euclid.snapToRightAngle(srcVertices, index, { precision : precision }),
                    [1.1, 0])
            ).toBe(true);
        });

        it('should work properly with polyline geometry', function() {
            var precision = 0.2;

            srcVertices = [[0, 0], [1, 0], [0.9, 1]];
            index = 2;

            expect(
                arraysAreDeepEqual(
                    euclid.snapToRightAngle(srcVertices, index, { precision : precision }),
                    [1, 1])
            ).toBe(true);
        });
    });

    describe('getIntersectionPoint', function() {
        it('should return null if lines are collinear', function() {
            var line1 = [[0, 0], [1, 1]],
                line2 = [[0, 0], [1, 1]];

            expect(euclid.getIntersectionPoint(line1, line2)).toBeNull();

            line1 = [[0, 0], [1, 1]];
            line2 = [[1, 0], [2, 1]];

            expect(euclid.getIntersectionPoint(line1, line2)).toBeNull();
        });

        it('should return an intersection point if lines are not collinear', function() {
            var line1 = [[0, 0], [2, 2]],
                line2 = [[0, 2], [2, 0]];

            expect(arraysAreDeepEqual(euclid.getIntersectionPoint(line1, line2), [1, 1])).toBe(true);
        });
    });

    describe('isPointOnSegment', function() {
        it('should return true if it is', function() {
            expect(euclid.isPointOnSegment([0.5, 0.5], [[0, 0], [1, 1]])).toBe(true);
        });

        it('should return false otherwise', function() {
            expect(euclid.isPointOnSegment([1, 0], [[0, 0], [1, 1]])).toBe(false);
        });
    });

    describe('isOrthogonal', function() {
        it('should return true if it is', function() {
            expect(euclid.isOrthogonal([[0, 0], [1, 0]], [[0, 0], [0, 1]])).toBe(true);
        });

        it('should return false otherwise', function() {
            expect(euclid.isOrthogonal([[0, 0], [1, 0]], [[0, 0], [1, 1]])).toBe(false);
        });
    });

    describe('getIntersectionPoints', function() {
        var line = [[0, 2], [5, 2]];
        it('should work properly with polygon geometry', function() {
            srcVertices = [
                [[0, 0], [4, 0], [4, 4], [0, 4], [0, 0]],
                [[1, 1], [2, 1], [2, 3], [1, 3], [1, 1]]
            ];
            expect(
                arraysAreDeepEqual(
                    euclid.getIntersectionPoints(line, srcVertices),
                    [[4, 2], [0, 2], [2, 2], [1, 2]])
            ).toBe(true);
        });

        it('should work properly with polyline geometry', function() {
            srcVertices = [[0, 3], [2, 1], [2, 5], [4, 4], [4, 0]];
            expect(
                arraysAreDeepEqual(
                    euclid.getIntersectionPoints(line, srcVertices),
                    [[1, 2], [2, 2], [4, 2]])
            ).toBe(true);
        });
    });

    describe('getBoundingBox', function() {
        it('should work properly with polygon geometry', function() {
            srcVertices = [
                [[0, 2], [2, 0], [5, 1], [4, 4], [0, 4], [0, 2]]
            ];
            expect(
                arraysAreDeepEqual(
                    euclid.getBoundingBox(srcVertices),
                    [[0, 0], [5, 4]])
            ).toBe(true);
        });

        it('should work properly with polyline geometry', function() {
            srcVertices = [[0, 2], [2, 1], [5, 1], [4, 4], [7, 5]];
            expect(
                arraysAreDeepEqual(
                    euclid.getBoundingBox(srcVertices),
                    [[0, 1], [7, 5]])
            ).toBe(true);
        });
    });

    describe('getPolygonInteriorPoint', function() {
        it('should work properly with inner paths', function() {
            srcVertices = [
                [[0, 0], [4, 0], [4, 3], [0, 3], [0, 0]],
                [[1, 1], [2, 1], [2, 2], [1, 2], [1, 1]]
            ];

            expect(
                arraysAreDeepEqual(
                    euclid.getPolygonInteriorPoint(srcVertices),
                    [3, 1.5])
            ).toBe(true);
        });

        it('should work properly without inner paths', function() {
            srcVertices = [
                [[0, 0], [4, 0], [4, 3], [0, 3], [0, 0]]
            ];

            expect(
                arraysAreDeepEqual(
                    euclid.getPolygonInteriorPoint(srcVertices),
                    [2, 1.5])
            ).toBe(true);
        });

        it('should work in edge case', function() {
            srcVertices = [
                [[0, 0], [0, 2], [2, 2], [2, 1], [1, 1], [1, 0], [0, 0]]
            ];

            expect(
                arraysAreDeepEqual(
                    euclid.getPolygonInteriorPoint(srcVertices),
                    [0.5, 0.98])
            ).toBe(true);
        });
    });

    describe('areCoordsEqual', function() {
        it('should return true if coords are equal', function() {
            expect(euclid.areCoordsEqual(1, 1)).toBe(true);

            expect(euclid.areCoordsEqual(
                [[1, 1], [2, 2]],
                [[1, 1], [2, 2]]
            )).toBe(true);
        });

        it('should return false if coords are not equal', function() {
            expect(euclid.areCoordsEqual(1, 2)).toBe(false);

            expect(euclid.areCoordsEqual(
                [1, 1],
                [[1, 1]]
            )).toBe(false);
        });

        it('should work properly with precision param', function() {
            expect(euclid.areCoordsEqual(
                [1, 1],
                [0.99, 0.99],
                0.1
            )).toBe(true);

            expect(euclid.areCoordsEqual(
                1e-10,
                1e-11,
                0
            )).toBe(false);
        });
    });

    describe('approximateCircle', function() {
        it('should work properly with edgesNumber restriction', function() {
            expect(arraysAreDeepEqual(
                euclid.approximateCircle([60, 60], 50, { edgesNumber : 5 }),
                [
                    [110, 60],
                    [75.45084971874738, 107.55282581475768],
                    [19.549150281252636, 89.38926261462366],
                    [19.54915028125263, 30.61073738537635],
                    [75.45084971874736, 12.447174185242318],
                    [110, 60]
                ])
            ).toBe(true);
        });

        it('should work properly with edgeMaxLength restriction', function() {
            expect(arraysAreDeepEqual(
                euclid.approximateCircle([60, 60], 50, { edgeMaxLength : 50 }),
                [
                    [110, 60],
                    [85, 103.30127018922192],
                    [35, 103.30127018922194],
                    [10, 60],
                    [35, 16.698729810778083],
                    [85, 16.698729810778048],
                    [110, 60]
                ])
            ).toBe(true);
        });

        it('should return null if number of edges is less than 3', function() {
             expect(euclid.approximateCircle([60, 60], 50, { edgesNumber : 2 })).toBeNull();
             expect(euclid.approximateCircle([60, 60], 50, { edgeMaxLength : 100 })).toBeNull();
        });
    });

    describe('getPolygonGeodesicArea', function() {
        it('should calculate area properly', function() {
            expect(euclid.getPolygonGeodesicArea([[[0, 0], [1, 1], [2, 0], [0, 0]]])).toEqual(12391399902);
        });
    });

    describe('convertGeoToMercatorCoords', function() {
        it('should convert coordinates properly', function() {
            expect(
                arraysAreDeepEqual(
                    euclid.convertGeoToMercatorCoords([1, 1]),
                    [111319.49079327357, 110579.96522189587]
                )
            ).toBe(true);
        });
    });

    describe('convertMercatorToGeoCoords', function() {
        it('should convert coordinates properly', function() {
            expect(
                arraysAreDeepEqual(
                    euclid.convertMercatorToGeoCoords([111319.49079327357, 110579.96522189587]),
                    [1, 1]
                )
            ).toBe(true);
        });
    });

    describe('getVectorInclinationAngle', function() {
        it('should calculate angle properly', function() {
            expect(euclid.getVectorInclinationAngle([0, 0], [1, 0])).toBe(0);
            expect(euclid.getVectorInclinationAngle([0, 0], [0, 1])).toBe(Math.PI / 2);
            expect(euclid.getVectorInclinationAngle([0, 0], [-1, 0])).toBe(Math.PI);
            expect(euclid.getVectorInclinationAngle([0, 0], [0, -1])).toBe(3 * Math.PI / 2);
        });
    });
});
