import {vec2, vec3} from '../../util/vector';

describe('vector', () => {
    describe('vec2', () => {
        it('copy', () => {
            expect(vec2.copy({x: 1, y: 2})).toEqual({x: 1, y: 2});
        });
        it('add', () => {
            expect(vec2.add({x: 2, y: 2}, {x: 1, y: 2})).toEqual({x: 3, y: 4});
        });
        it('sub', () => {
            expect(vec2.sub({x: 2, y: 2}, {x: 1, y: 2})).toEqual({x: 1, y: 0});
        });
        it('divn', () => {
            expect(vec2.divn({x: 2, y: 4}, 2)).toEqual({x: 1, y: 2});
        });
        it('dot', () => {
            expect(vec2.dot({x: 1, y: 0}, {x: 0, y: 1})).toEqual(0);
            expect(vec2.dot({x: 1, y: 1}, {x: -1, y: 1})).toEqual(0);
            expect(vec2.dot({x: 1, y: 1}, {x: 0, y: 1})).toEqual(1);
        });
        it('rotate', () => {
            expect(vec2.rotate({x: 1, y: 0}, Math.PI / 2)).toBeDeepCloseTo({x: 0, y: 1});
            expect(vec2.rotate({x: 1, y: 1}, Math.PI)).toBeDeepCloseTo({x: -1, y: -1});
            expect(vec2.rotate({x: 1, y: 0}, Math.PI / 4)).toBeDeepCloseTo({x: Math.SQRT1_2, y: Math.SQRT1_2});
        });
        it('length', () => {
            expect(vec2.length({x: 3, y: 4})).toEqual(5);
        });
        it('normalize', () => {
            expect(vec2.normalize({x: 2, y: 0})).toEqual({x: 1, y: 0});
        });
    });

    describe('vec3', () => {
        it('copy', () => {
            expect(vec3.copy({x: 1, y: 2, z: 3})).toEqual({x: 1, y: 2, z: 3});
        });
        it('add', () => {
            expect(vec3.add({x: 2, y: 2, z: 2}, {x: 1, y: 2, z: 3})).toEqual({x: 3, y: 4, z: 5});
        });
        it('sub', () => {
            expect(vec3.sub({x: 2, y: 2, z: 2}, {x: 1, y: 2, z: 3})).toEqual({x: 1, y: 0, z: -1});
        });
        it('muln', () => {
            expect(vec3.muln({x: 1, y: 2, z: 3}, 2)).toEqual({x: 2, y: 4, z: 6});
        });
        it('divn', () => {
            expect(vec3.divn({x: 2, y: 4, z: 6}, 2)).toEqual({x: 1, y: 2, z: 3});
        });
        it('length', () => {
            expect(vec3.length({x: 3, y: 0, z: 4})).toEqual(5);
        });
        it('normalize', () => {
            expect(vec3.normalize({x: 2, y: 0, z: 0})).toEqual({x: 1, y: 0, z: 0});
        });
        it('cross', () => {
            expect(vec3.cross({x: 1, y: 0, z: 0}, {x: 0, y: 1, z: 0})).toEqual({x: 0, y: 0, z: 1});
        });
        it('rotateX', () => {
            expect(vec3.rotateX({x: 0, y: 1, z: 0}, Math.PI / 2)).toBeDeepCloseTo({x: 0, y: 0, z: 1});
        });
        it('rotateY', () => {
            expect(vec3.rotateY({x: 1, y: 0, z: 0}, Math.PI / 2)).toBeDeepCloseTo({x: 0, y: 0, z: -1});
        });
        it('rotateZ', () => {
            expect(vec3.rotateZ({x: 1, y: 0, z: 0}, Math.PI / 2)).toBeDeepCloseTo({x: 0, y: 1, z: 0});
        });
        it('mix', () => {
            expect(vec3.mix({x: 100, y: 10, z: 1}, 0.5, {x: 1, y: 1, z: 1}, 0.5)).toEqual({x: 50.5, y: 5.5, z: 1});
            expect(vec3.mix({x: 1, y: 0, z: 0}, 0.5, {x: 2, y: 1, z: 0}, 0.1)).toEqual({x: 0.7, y: 0.1, z: 0});
            expect(vec3.mix({x: 100, y: 10, z: 1}, 0, {x: 1, y: 1, z: 1}, 0.3)).toEqual({x: 0.3, y: 0.3, z: 0.3});
        });
    });
});
