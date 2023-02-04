import {assert} from 'chai';

import Matrix4, * as matrix4 from '../../../src/vector_render_engine/math/matrix4';
import Vector3, * as vector3 from '../../../src/vector_render_engine/math/vector3';
import {deg2rad} from '../../../src/vector_render_engine/util/rad_deg';

const HALF_SQRT3 = 0.5 * Math.sqrt(3);
const SQRT1_2 = Math.SQRT1_2;

describe('math/matrix4', () => {
    const m = matrix4.create();

    beforeEach(() => {
        matrix4.copy(matrix4.IDENTITY, m);
    });

    const DISTANCE_TOLERANCE = 1e-4;

    function checkRotation(
        m: Matrix4,
        vs: Map<Vector3, Vector3>
    ): void {
        for (const [src, dst] of vs) {
            assert(
                vector3.distance(matrix4.apply(m, src), dst) <
                    DISTANCE_TOLERANCE,
                `${JSON.stringify(src)} ${JSON.stringify(matrix4.apply(m, src))} ${JSON.stringify(dst)}`
            );
        }
    }

    describe('rotateX', () => {
        it('should correctly rotate unit vectors by 30 deg', () => {
            checkRotation(
                matrix4.rotateX(m, deg2rad(30), m),
                new Map([
                    [vector3.POSITIVE_Y, vector3.create(0, HALF_SQRT3, 0.5)],
                    [vector3.NEGATIVE_Y, vector3.create(0, -HALF_SQRT3, -0.5)],
                    [vector3.POSITIVE_Z, vector3.create(0, -0.5, HALF_SQRT3)],
                    [vector3.NEGATIVE_Z, vector3.create(0, 0.5, -HALF_SQRT3)]
                ])
            );
        });

        it('should correctly rotate unit vectors by 45 deg', () => {
            checkRotation(
                matrix4.rotateX(m, deg2rad(45), m),
                new Map([
                    [vector3.POSITIVE_Y, vector3.create(0, SQRT1_2, SQRT1_2)],
                    [vector3.NEGATIVE_Y, vector3.create(0, -SQRT1_2, -SQRT1_2)],
                    [vector3.POSITIVE_Z, vector3.create(0, -SQRT1_2, SQRT1_2)],
                    [vector3.NEGATIVE_Z, vector3.create(0, SQRT1_2, -SQRT1_2)]
                ])
            );
        });

        it('should correctly rotate unit vectors by 90 deg', () => {
            checkRotation(
                matrix4.rotateX(m, deg2rad(90), m),
                new Map([
                    [vector3.POSITIVE_Y, vector3.POSITIVE_Z],
                    [vector3.NEGATIVE_Y, vector3.NEGATIVE_Z],
                    [vector3.POSITIVE_Z, vector3.NEGATIVE_Y],
                    [vector3.NEGATIVE_Z, vector3.POSITIVE_Y]
                ])
            );
        });

        it('should correctly rotate unit vectors by 180 deg', () => {
            checkRotation(
                matrix4.rotateX(m, deg2rad(180), m),
                new Map([
                    [vector3.POSITIVE_Y, vector3.NEGATIVE_Y],
                    [vector3.NEGATIVE_Y, vector3.POSITIVE_Y],
                    [vector3.POSITIVE_Z, vector3.NEGATIVE_Z],
                    [vector3.NEGATIVE_Z, vector3.POSITIVE_Z]
                ])
            );
        });

        it('should correctly rotate unit vectors by 270 deg', () => {
            checkRotation(
                matrix4.rotateX(m, deg2rad(270), m),
                new Map([
                    [vector3.POSITIVE_Y, vector3.NEGATIVE_Z],
                    [vector3.NEGATIVE_Y, vector3.POSITIVE_Z],
                    [vector3.POSITIVE_Z, vector3.POSITIVE_Y],
                    [vector3.NEGATIVE_Z, vector3.NEGATIVE_Y]
                ])
            );
        });

        it('should not rotate vectors parallel to X axis', () => {
            const posX = matrix4.apply(
                matrix4.rotateX(m, Math.random(), m),
                vector3.POSITIVE_X
            );

            assert(vector3.areEqual(posX, vector3.POSITIVE_X));

            const negX = matrix4.apply(
                matrix4.rotateX(m, Math.random(), m),
                vector3.NEGATIVE_X
            );

            assert(vector3.areEqual(negX, vector3.NEGATIVE_X));
        });
    });

    describe('rotateY', () => {
        it('should correctly rotate unit vectors by 30 deg', () => {
            checkRotation(
                matrix4.rotateY(m, deg2rad(30), m),
                new Map([
                    [vector3.POSITIVE_X, vector3.create(HALF_SQRT3, 0, 0.5)],
                    [vector3.NEGATIVE_X, vector3.create(-HALF_SQRT3, 0, -0.5)],
                    [vector3.POSITIVE_Z, vector3.create(-0.5, 0, HALF_SQRT3)],
                    [vector3.NEGATIVE_Z, vector3.create(0.5, 0, -HALF_SQRT3)]
                ])
            );
        });

        it('should correctly rotate unit vectors by 45 deg', () => {
            checkRotation(
                matrix4.rotateY(m, deg2rad(45), m),
                new Map([
                    [vector3.POSITIVE_X, vector3.create(SQRT1_2, 0, SQRT1_2)],
                    [vector3.NEGATIVE_X, vector3.create(-SQRT1_2, 0, -SQRT1_2)],
                    [vector3.POSITIVE_Z, vector3.create(-SQRT1_2, 0, SQRT1_2)],
                    [vector3.NEGATIVE_Z, vector3.create(SQRT1_2, 0, -SQRT1_2)]
                ])
            );
        });

        it('should correctly rotate unit vectors by 90 deg', () => {
            checkRotation(
                matrix4.rotateY(m, deg2rad(90), m),
                new Map([
                    [vector3.POSITIVE_X, vector3.POSITIVE_Z],
                    [vector3.NEGATIVE_X, vector3.NEGATIVE_Z],
                    [vector3.POSITIVE_Z, vector3.NEGATIVE_X],
                    [vector3.NEGATIVE_Z, vector3.POSITIVE_X]
                ])
            );
        });

        it('should correctly rotate unit vectors by 180 deg', () => {
            checkRotation(
                matrix4.rotateY(m, deg2rad(180), m),
                new Map([
                    [vector3.POSITIVE_X, vector3.NEGATIVE_X],
                    [vector3.NEGATIVE_X, vector3.POSITIVE_X],
                    [vector3.POSITIVE_Z, vector3.NEGATIVE_Z],
                    [vector3.NEGATIVE_Z, vector3.POSITIVE_Z]
                ])
            );
        });

        it('should correctly rotate unit vectors by 270 deg', () => {
            checkRotation(
                matrix4.rotateY(m, deg2rad(270), m),
                new Map([
                    [vector3.POSITIVE_X, vector3.NEGATIVE_Z],
                    [vector3.NEGATIVE_X, vector3.POSITIVE_Z],
                    [vector3.POSITIVE_Z, vector3.POSITIVE_X],
                    [vector3.NEGATIVE_Z, vector3.NEGATIVE_X]
                ])
            );
        });

        it('should not rotate vectors parallel to Y axis', () => {
            const posY = matrix4.apply(
                matrix4.rotateY(m, Math.random(), m),
                vector3.POSITIVE_Y
            );

            assert(vector3.areEqual(posY, vector3.POSITIVE_Y));

            const negY = matrix4.apply(
                matrix4.rotateY(m, Math.random(), m),
                vector3.NEGATIVE_Y
            );

            assert(vector3.areEqual(negY, vector3.NEGATIVE_Y));
        });
    });

    describe('rotateZ', () => {
        it('should correctly rotate unit vectors by 30 deg', () => {
            checkRotation(
                matrix4.rotateZ(m, deg2rad(30), m),
                new Map([
                    [vector3.POSITIVE_X, vector3.create(HALF_SQRT3, 0.5, 0)],
                    [vector3.NEGATIVE_X, vector3.create(-HALF_SQRT3, -0.5, 0)],
                    [vector3.POSITIVE_Y, vector3.create(-0.5, HALF_SQRT3, 0)],
                    [vector3.NEGATIVE_Y, vector3.create(0.5, -HALF_SQRT3, 0)]
                ])
            );
        });

        it('should correctly rotate unit vectors by 45 deg', () => {
            checkRotation(
                matrix4.rotateZ(m, deg2rad(45), m),
                new Map([
                    [vector3.POSITIVE_X, vector3.create(SQRT1_2, SQRT1_2, 0)],
                    [vector3.NEGATIVE_X, vector3.create(-SQRT1_2, -SQRT1_2, 0)],
                    [vector3.POSITIVE_Y, vector3.create(-SQRT1_2, SQRT1_2, 0)],
                    [vector3.NEGATIVE_Y, vector3.create(SQRT1_2, -SQRT1_2, 0)]
                ])
            );
        });

        it('should correctly rotate unit vectors by 90 deg', () => {
            checkRotation(
                matrix4.rotateZ(m, deg2rad(90), m),
                new Map([
                    [vector3.POSITIVE_X, vector3.POSITIVE_Y],
                    [vector3.NEGATIVE_X, vector3.NEGATIVE_Y],
                    [vector3.POSITIVE_Y, vector3.NEGATIVE_X],
                    [vector3.NEGATIVE_Y, vector3.POSITIVE_X]
                ])
            );
        });

        it('should correctly rotate unit vectors by 180 deg', () => {
            checkRotation(
                matrix4.rotateZ(m, deg2rad(180), m),
                new Map([
                    [vector3.POSITIVE_X, vector3.NEGATIVE_X],
                    [vector3.NEGATIVE_X, vector3.POSITIVE_X],
                    [vector3.POSITIVE_Y, vector3.NEGATIVE_Y],
                    [vector3.NEGATIVE_Y, vector3.POSITIVE_Y]
                ])
            );
        });

        it('should correctly rotate unit vectors by 270 deg', () => {
            checkRotation(
                matrix4.rotateZ(m, deg2rad(270), m),
                new Map([
                    [vector3.POSITIVE_X, vector3.NEGATIVE_Y],
                    [vector3.NEGATIVE_X, vector3.POSITIVE_Y],
                    [vector3.POSITIVE_Y, vector3.POSITIVE_X],
                    [vector3.NEGATIVE_Y, vector3.NEGATIVE_X]
                ])
            );
        });

        it('should not rotate vectors parallel to z axis', () => {
            const posZ = matrix4.apply(
                matrix4.rotateZ(m, Math.random(), m),
                vector3.POSITIVE_Z
            );

            assert(vector3.areEqual(posZ, vector3.POSITIVE_Z));

            const negZ = matrix4.apply(
                matrix4.rotateZ(m, Math.random(), m),
                vector3.NEGATIVE_Z
            );

            assert(vector3.areEqual(negZ, vector3.NEGATIVE_Z));
        });
    });

    describe('transpose', () => {
        it('should transpose matrix', () => {
            const matrix = [
                1, 2, 3, 4,
                5, 6, 7, 8,
                9, 10, 11, 12,
                13, 14, 15, 16
            ];

            const transposed = matrix4.transpose(matrix);

            assert.deepEqual(transposed, [
                1, 5, 9, 13,
                2, 6, 10, 14,
                3, 7, 11, 15,
                4, 8, 12, 16
            ]);
        });

        it('should transpose matrix in-place', () => {
            const matrix = [
                1, 2, 3, 4,
                5, 6, 7, 8,
                9, 10, 11, 12,
                13, 14, 15, 16
            ];

            const transposed = matrix4.transpose(matrix, matrix);

            assert.equal(transposed, matrix);
            assert.deepEqual(matrix, [
                1, 5, 9, 13,
                2, 6, 10, 14,
                3, 7, 11, 15,
                4, 8, 12, 16
            ]);
        });
    });
});
