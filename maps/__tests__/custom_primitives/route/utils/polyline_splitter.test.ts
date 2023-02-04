import {findClosestPointOnLine, splitPolyline, shiftPolylinePoint} from '../../../../custom_primitives/primitives/route/utils/polyline_splitter';

describe('route primitve utils', () => {
    it('should findClosestPointOnLine', () => {
        expect(findClosestPointOnLine(
            {x: 1, y: 1, z: 0},
            [
                {x: 2, y: 0, z: 0},
                {x: 2, y: 2, z: 0}
            ]
        )).toBeDeepCloseTo({
            pointOnLine: {x: 2, y: 1, z: 0},
            segmentIndex: 0,
            isSegmentTerminalPoint: false,
            positionAlongLine: 1
        });

        expect(findClosestPointOnLine(
            {x: 1, y: 1, z: 0},
            [
                {x: 3, y: 1, z: 0},
                {x: 2, y: 1, z: 0},
                {x: 2, y: 2, z: 0}
            ]
        )).toBeDeepCloseTo({
            pointOnLine: {x: 2, y: 1, z: 0},
            segmentIndex: 0,
            isSegmentTerminalPoint: true,
            positionAlongLine: 1
        });

        expect(findClosestPointOnLine(
            {x: 1, y: 1, z: 0},
            [
                {x: 3, y: 1, z: 0},
                {x: 3, y: 2, z: 0},
                {x: 0, y: 2, z: 0}
            ]
        )).toBeDeepCloseTo({
            pointOnLine: {x: 1, y: 2, z: 0},
            segmentIndex: 1,
            isSegmentTerminalPoint: false,
            positionAlongLine: 3
        });

        expect(findClosestPointOnLine(
            {x: 1, y: 1, z: 0},
            [
                {x: 12, y: 1, z: 0},
                {x: 2, y: 1, z: 0}
            ]
        )).toBeDeepCloseTo({
            pointOnLine: {x: 2, y: 1, z: 0},
            segmentIndex: 0,
            isSegmentTerminalPoint: true,
            positionAlongLine: 10
        });

        expect(findClosestPointOnLine(
            {x: 1, y: 1, z: 0},
            [
                {x: 2, y: 1, z: 0}
            ]
        )).toBeDeepCloseTo({
            pointOnLine: {x: 2, y: 1, z: 0},
            segmentIndex: 0,
            isSegmentTerminalPoint: true,
            positionAlongLine: 0
        });

        expect(findClosestPointOnLine(
            {x: 1, y: 1, z: 0},
            [
                {x: 0, y: 1, z: 0},
                {x: 2, y: 1, z: 0}
            ]
        )).toBeDeepCloseTo({
            pointOnLine: {x: 1, y: 1, z: 0},
            segmentIndex: 0,
            isSegmentTerminalPoint: false,
            positionAlongLine: 1
        });

        expect(findClosestPointOnLine(
            {x: 1, y: 1, z: 0},
            [
                {x: 2, y: 1, z: 0},
                {x: 1, y: 2, z: 0},
                {x: 1, y: 20, z: 0}
            ]
        )).toBeDeepCloseTo({
            pointOnLine: {x: 1.5, y: 1.5, z: 0},
            segmentIndex: 0,
            isSegmentTerminalPoint: false,
            positionAlongLine: Math.SQRT1_2
        });

        expect(findClosestPointOnLine(
            {x: 1, y: 1, z: 1},
            [
                {x: 10, y: 1, z: 0},
                {x: 2, y: 1, z: 0},
                {x: 2, y: 1, z: 1}
            ]
        )).toBeDeepCloseTo({
            pointOnLine: {x: 2, y: 1, z: 1},
            segmentIndex: 1,
            isSegmentTerminalPoint: true,
            positionAlongLine: 9
        });
    });

    it('should splitPolyline', () => {
        expect(splitPolyline([
            {x: 2, y: 1, z: 0},
            {x: 0, y: 1, z: 0}
        ], {x: 1, y: 1, z: 0}, 0)).toBeDeepCloseTo([
            [{x: 1, y: 1, z: 0}, {x: 0, y: 1, z: 0}],
            [{x: 1, y: 1, z: 0}, {x: 2, y: 1, z: 0}]
        ]);

        expect(splitPolyline([
            {x: 3, y: 1, z: 0},
            {x: 2, y: 1, z: 0},
            {x: 2, y: 2, z: 0}
        ], {x: 2, y: 1, z: 0}, 0)).toBeDeepCloseTo([
            [{x: 2, y: 1, z: 0}, {x: 2, y: 2, z: 0}],
            [{x: 2, y: 1, z: 0}, {x: 3, y: 1, z: 0}]
        ]);

        expect(splitPolyline([
            {x: 3, y: 1, z: 0},
            {x: 2, y: 1, z: 0},
            {x: 1, y: 2, z: 0}
        ], {x: 1.5, y: 1.5, z: 0}, 1)).toBeDeepCloseTo([
            [{x: 1.5, y: 1.5, z: 0}, {x: 1, y: 2, z: 0}],
            [{x: 1.5, y: 1.5, z: 0}, {x: 2, y: 1, z: 0}, {x: 3, y: 1, z: 0}]
        ]);
    });

    it('should shift polyline point', () => {
        const polyline = [
            {x: -2, y: 4, z: 0},
            {x: 2, y: 1, z: 0},
            {x: 5, y: 5, z: 0},
            {x: 9, y: 2, z: 0},
            {x: 6, y: -2, z: 0}
        ];

        // zero shift
        expect(shiftPolylinePoint(polyline, {
            pointOnLine: {x: 5, y: 5, z: 0},
            segmentIndex: 1,
            isSegmentTerminalPoint: true
        }, 0)).toBeDeepCloseTo({
            pointOnLine: {x: 5, y: 5, z: 0},
            segmentIndex: 1,
            isSegmentTerminalPoint: true
        });
        expect(shiftPolylinePoint(polyline, {
            pointOnLine: {x: 5, y: 5, z: 0},
            segmentIndex: 2,
            isSegmentTerminalPoint: true
        }, 0)).toBeDeepCloseTo({
            pointOnLine: {x: 5, y: 5, z: 0},
            segmentIndex: 2,
            isSegmentTerminalPoint: true
        });
        expect(shiftPolylinePoint(polyline, {
            pointOnLine: {x: 3.5, y: 3, z: 0},
            segmentIndex: 1,
            isSegmentTerminalPoint: false
        }, 0)).toBeDeepCloseTo({
            pointOnLine: {x: 3.5, y: 3, z: 0},
            segmentIndex: 1,
            isSegmentTerminalPoint: false
        });

        // terminal to terminal
        expect(shiftPolylinePoint(polyline, {
            pointOnLine: {x: 5, y: 5, z: 0},
            segmentIndex: 1,
            isSegmentTerminalPoint: true
        }, 5)).toBeDeepCloseTo({
            pointOnLine: {x: 9, y: 2, z: 0},
            segmentIndex: 2,
            isSegmentTerminalPoint: true
        });
        expect(shiftPolylinePoint(polyline, {
            pointOnLine: {x: 5, y: 5, z: 0},
            segmentIndex: 1,
            isSegmentTerminalPoint: true
        }, -5)).toBeDeepCloseTo({
            pointOnLine: {x: 2, y: 1, z: 0},
            segmentIndex: 1,
            isSegmentTerminalPoint: true
        });

        // terminal to midpoint
        expect(shiftPolylinePoint(polyline, {
            pointOnLine: {x: 5, y: 5, z: 0},
            segmentIndex: 1,
            isSegmentTerminalPoint: true
        }, 2)).toBeDeepCloseTo({
            pointOnLine: {x: 6.6, y: 3.8, z: 0},
            segmentIndex: 2,
            isSegmentTerminalPoint: false
        });
        expect(shiftPolylinePoint(polyline, {
            pointOnLine: {x: 5, y: 5, z: 0},
            segmentIndex: 1,
            isSegmentTerminalPoint: true
        }, -2)).toBeDeepCloseTo({
            pointOnLine: {x: 3.8, y: 3.4, z: 0},
            segmentIndex: 1,
            isSegmentTerminalPoint: false
        });

        // midpoint to terminal
        expect(shiftPolylinePoint(polyline, {
            pointOnLine: {x: 3.5, y: 3, z: 0},
            segmentIndex: 1,
            isSegmentTerminalPoint: false
        }, 2.5)).toBeDeepCloseTo({
            pointOnLine: {x: 5, y: 5, z: 0},
            segmentIndex: 1,
            isSegmentTerminalPoint: true
        });
        expect(shiftPolylinePoint(polyline, {
            pointOnLine: {x: 3.5, y: 3, z: 0},
            segmentIndex: 1,
            isSegmentTerminalPoint: false
        }, -2.5)).toBeDeepCloseTo({
            pointOnLine: {x: 2, y: 1, z: 0},
            segmentIndex: 1,
            isSegmentTerminalPoint: true
        });

        // midpoint to midpoint
        expect(shiftPolylinePoint(polyline, {
            pointOnLine: {x: 3.5, y: 3, z: 0},
            segmentIndex: 1,
            isSegmentTerminalPoint: false
        }, 5)).toBeDeepCloseTo({
            pointOnLine: {x: 7, y: 3.5, z: 0},
            segmentIndex: 2,
            isSegmentTerminalPoint: false
        });
        expect(shiftPolylinePoint(polyline, {
            pointOnLine: {x: 3.5, y: 3, z: 0},
            segmentIndex: 1,
            isSegmentTerminalPoint: false
        }, -5)).toBeDeepCloseTo({
            pointOnLine: {x: 0, y: 2.5, z: 0},
            segmentIndex: 0,
            isSegmentTerminalPoint: false
        });

        // big shift
        expect(shiftPolylinePoint(polyline, {
            pointOnLine: {x: 2, y: 1, z: 0},
            segmentIndex: 1,
            isSegmentTerminalPoint: false
        }, 15)).toBeDeepCloseTo({
            pointOnLine: {x: 6, y: -2, z: 0},
            segmentIndex: 3,
            isSegmentTerminalPoint: true
        });
        expect(shiftPolylinePoint(polyline, {
            pointOnLine: {x: 2, y: 1, z: 0},
            segmentIndex: 1,
            isSegmentTerminalPoint: false
        }, 12.5)).toBeDeepCloseTo({
            pointOnLine: {x: 7.5, y: 0, z: 0},
            segmentIndex: 3,
            isSegmentTerminalPoint: false
        });
        expect(shiftPolylinePoint(polyline, {
            pointOnLine: {x: 9, y: 2, z: 0},
            segmentIndex: 2,
            isSegmentTerminalPoint: false
        }, -15)).toBeDeepCloseTo({
            pointOnLine: {x: -2, y: 4, z: 0},
            segmentIndex: 0,
            isSegmentTerminalPoint: true
        });
        expect(shiftPolylinePoint(polyline, {
            pointOnLine: {x: 9, y: 2, z: 0},
            segmentIndex: 2,
            isSegmentTerminalPoint: false
        }, -12.5)).toBeDeepCloseTo({
            pointOnLine: {x: 0, y: 2.5, z: 0},
            segmentIndex: 0,
            isSegmentTerminalPoint: false
        });

        // too big shift
        expect(shiftPolylinePoint(polyline, {
            pointOnLine: {x: 2, y: 1, z: 0},
            segmentIndex: 1,
            isSegmentTerminalPoint: false
        }, 25)).toBe(undefined);
        expect(shiftPolylinePoint(polyline, {
            pointOnLine: {x: 9, y: 2, z: 0},
            segmentIndex: 2,
            isSegmentTerminalPoint: false
        }, -25)).toBe(undefined);
    });
});
