import {Matrix4, mat4} from '../../util/matrix';

describe('matrix', () => {
    describe('mat4', () => {
        it('copy', () => {
            const m: Matrix4 = [
                7, 7, 1, 0,
                6, 4, 2, 6,
                8, 8, 6, 3,
                4, 4, 3, 6
            ];

            expect(mat4.copy(m)).toBeDeepCloseTo(m);
        });

        it('mul', () => {
            expect(mat4.mul([
                7, 7, 1, 0,
                6, 4, 2, 6,
                8, 8, 6, 3,
                4, 4, 3, 6
            ], [
                8, 5, 9, 0,
                2, 2, 1, 1,
                9, 7, 8, 3,
                3, 8, 0, 1
            ])).toBeDeepCloseTo([
                79, 56, 78, 10,
                92, 100, 74, 16,
                143, 122, 128, 29,
                85, 97, 64, 19
            ]);
        });

        it('apply', () => {
            expect(mat4.apply([
                7, 7, 1, 0,
                6, 4, 2, 6,
                8, 8, 6, 3,
                4, 4, 3, 6
            ], {
                x: 8,
                y: 5,
                z: 9
            })).toBeDeepCloseTo({
                x: 162 / 63,
                y: 152 / 63,
                z: 75 / 63
            });
        });

    });
});
