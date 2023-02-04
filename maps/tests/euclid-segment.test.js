import Segment from '../src/euclid-segment';
import { arraysAreDeepEqual } from './utils';

describe('euclid-segment', function() {
    let segment;

    describe('getProjectionPoint', function() {
        it('should return a proper result', function() {
            expect(
                arraysAreDeepEqual(
                    Segment.create([0, 0], [1, 0]).getProjectionPoint([0.5, 0.5]),
                    [0.5, 0])
            ).toBe(true);
            expect(
                arraysAreDeepEqual(
                    Segment.create([0, 0], [0, 1]).getProjectionPoint([0.5, 0.5]),
                    [0, 0.5])
            ).toBe(true);
        });

        it('should use \'snapToSegment\' option properly', function() {
            segment = Segment.create([0, 0], [1, 0]);

            expect(
                arraysAreDeepEqual(
                    segment.getProjectionPoint([1.5, 1.5]),
                    [1.5, 0])
            ).toBe(true);
            expect(
                arraysAreDeepEqual(
                    segment.getProjectionPoint([1.5, 1.5], { snapToSegment : true }),
                    [1, 0])
            ).toBe(true);
        });

        it('should use \'vertexSnappingArea\' option properly', function() {
            segment = Segment.create([0, 0], [1, 0]);

            expect(
                arraysAreDeepEqual(
                    segment.getProjectionPoint([0.1, 0.1], { vertexSnappingArea : 0.05 }),
                    [0.1, 0])
            ).toBe(true);
            expect(
                arraysAreDeepEqual(
                    segment.getProjectionPoint([0.1, 0.1], { vertexSnappingArea : 0.1 }),
                    [0, 0])
            ).toBe(true);
        });
    });
});

