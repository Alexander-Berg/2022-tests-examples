import {assert} from 'chai';

import * as vector2 from '../../../src/vector_render_engine/math/vector2';

describe('math/vector2', () => {
    describe('bboxesOverlap', () => {
        it('should return `true` for overlapping boxes', () => {
            assert(vector2.bboxesOverlap(
                {maxY: 0.5, maxX: 0.5, minY: -1, minX: -1},
                {maxY: 1, maxX: 1, minY: -0.5, minX: -0.5}
            ));
        });

        it('should return `true` for a box in a box', () => {
            assert(vector2.bboxesOverlap(
                {maxY: 1, maxX: 1, minY: -1, minX: -1},
                {maxY: 0.5, maxX: 0.5, minY: -0.5, minX: -0.5}
            ));
            assert(vector2.bboxesOverlap(
                {maxY: 0.5, maxX: 0.5, minY: -0.5, minX: -0.5},
                {maxY: 1, maxX: 1, minY: -1, minX: -1}
            ));
        });

        it('should return `false` for not overlapping boxes', () => {
            assert(!vector2.bboxesOverlap(
                {maxY: 1, maxX: 1, minY: 0, minX: 0},
                {maxY: 0, maxX: 0, minY: -1, minX: -1}
            ));
        });
    });

    describe('pointIsInBBox', () => {
        it('should return `true` for a point inside a bbox', () => {
            assert(vector2.pointIsInBBox(
                vector2.create(0.5, 0.5),
                {maxY: 1, maxX: 1, minY: 0, minX: 0}
            ));
        });

        it('should return `false` for a point outside a bbox', () => {
            assert(!vector2.pointIsInBBox(
                vector2.create(-1, -1),
                {maxY: 1, maxX: 1, minY: 0, minX: 0}
            ));
        });
    });

    describe('dot', () => {
        it('should return 0 for perpendicular vectors', () => {
            assert(vector2.dot(
                vector2.create(1, 1),
                vector2.create(-1, 1)
             ) == 0);
        });

        it('should return >0 for colinear vectors', () => {
            assert(vector2.dot(vector2.create(1, 1), vector2.create(0, 1)) > 0);
        });

        it('should return <0 otherwise', () => {
            assert(vector2.dot(vector2.create(1, 1), vector2.create(-1, 0)) < 0);
        });
    });

    describe('distance', () => {
        it('should return 0 for same points', () => {
            assert(vector2.distance(
                vector2.create(1, 1),
                vector2.create(1, 1)
            ) == 0);
        });
    });

    describe('signedDistanceToLine', () => {
        const line = vector2.getLineFromPoints(
            vector2.create(0, 0),
            vector2.create(1, 1)
        );

        it('should return >0 for a point to the left of a line', () => {
            assert(vector2.getSignedDistanceToLine(vector2.create(0, 1), line) > 0);
        });

        it('should return <0 for a point to the right of a line', () => {
            assert(vector2.getSignedDistanceToLine(vector2.create(1, 0), line) < 0);
        });

        it('should return 0 for a point on the line itself', () => {
            assert(vector2.getSignedDistanceToLine(vector2.create(0.5, 0.5), line) == 0);
        });
    });
});
