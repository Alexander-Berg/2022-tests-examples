import {mat4, Matrix4} from '../../util/matrix';
import {lookAt, rotateX, rotateY, rotateZ, scale} from '../../util/matrix_transformation';
import {vec3} from '../../util/vector';

describe('matrix', () => {
    describe('mat4', () => {
        it('rotateX', () => {
            const m: Matrix4 = rotateX(mat4.createDiagonalMatrix(1), Math.PI / 2);
            expect(mat4.apply(m, {x: 0, y: 1, z: 0})).toBeDeepCloseTo({x: 0, y: 0, z: 1});
        });
        it('rotateY', () => {
            const m: Matrix4 = rotateY(mat4.createDiagonalMatrix(1), Math.PI / 2);
            expect(mat4.apply(m, {x: 1, y: 0, z: 0})).toBeDeepCloseTo({x: 0, y: 0, z: -1});
        });
        it('rotateZ', () => {
            const m: Matrix4 = rotateZ(mat4.createDiagonalMatrix(1), Math.PI / 2);
            expect(mat4.apply(m, {x: 1, y: 0, z: 0})).toBeDeepCloseTo({x: 0, y: 1, z: 0});
        });
        it('scale', () => {
            const m: Matrix4 = scale(mat4.createDiagonalMatrix(1), {x: 3, y: 2, z: 2});
            expect(mat4.apply(m, {x: 1, y: 2, z: 3})).toBeDeepCloseTo({x: 3, y: 4, z: 6});
        });
        it('lookAt', () => {
            const m = lookAt(mat4.createDiagonalMatrix(1), vec3.ORIGIN, vec3.POSITIVE_X, vec3.POSITIVE_Z);
            expect(mat4.apply(m, {x: 1, y: 0, z: 0})).toBeDeepCloseTo({x: 0, y: 0, z: -1});
        });
    });
});
