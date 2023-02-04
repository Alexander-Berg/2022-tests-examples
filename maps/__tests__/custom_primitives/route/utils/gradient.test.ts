import {color} from '../../../../../webgl_toolkit/util/color';
import {computeGradientPoints, getColor, pickTextureSize} from '../../../../custom_primitives/primitives/route/utils/gradient';

describe('gradient utils', () => {
    it('should getColor', () => {
        expect(getColor(
            0.5,
            [
                {color: color.create(1, 0, 0, 1), offset: 0},
                {color: color.create(0, 1, 0, 1), offset: 1}
            ],
            1
        )).toBeDeepCloseTo(
            {r: 0.5, g: 0.5, b: 0, a: 1}
        );

        expect(getColor(
            15,
            [
                {color: color.create(0, 0, 0, 0), offset: 0},
                {color: color.create(1, 1, 1, 1), offset: 1}
            ],
            20
        )).toBeDeepCloseTo(
            {r: 0.75, g: 0.75, b: 0.75, a: 0.75}
        );

        expect(getColor(
            -1,
            [
                {color: color.create(0, 0, 0, 0), offset: 0},
                {color: color.create(1, 1, 1, 1), offset: 1}
            ],
            20
        )).toBeDeepCloseTo(
            {r: 0, g: 0, b: 0, a: 0}
        );

        expect(getColor(
            100,
            [
                {color: color.create(0, 0, 0, 0), offset: 0},
                {color: color.create(1, 1, 1, 1), offset: 1}
            ],
            20
        )).toBeDeepCloseTo(
            {r: 1, g: 1, b: 1, a: 1}
        );

        expect(getColor(
            15,
            [
                {color: color.create(1, 0, 0, 0), offset: 0},
                {color: color.create(0, 1, 0, 0), offset: 0.5},
                {color: color.create(0, 0, 1, 0), offset: 1}
            ],
            20
        )).toBeDeepCloseTo(
            {r: 0, g: 0.5, b: 0.5, a: 0}
        );

        expect(getColor(
            10,
            [
                {color: color.create(1, 0, 0, 0), offset: 0},
                {color: color.create(0, 1, 0, 0), offset: 0.5},
                {color: color.create(0, 0, 1, 0), offset: 1}
            ],
            20
        )).toBeDeepCloseTo(
            {r: 0, g: 1, b: 0, a: 0}
        );
    });

    it('should computeGradientPoints', () => {
        expect(computeGradientPoints(
            0,
            1,
            [
                {color: {r: 1, g: 0, b: 0, a: 0}, offset: 0}
            ],
            1,
            1
        )).toBeDeepCloseTo([
            {color: {r: 1, g: 0, b: 0, a: 0}, offset: 0},
            {color: {r: 1, g: 0, b: 0, a: 0}, offset: 1}
        ]);

        expect(computeGradientPoints(
            0,
            1,
            [
                {color: {r: 1, g: 0, b: 0, a: 0}, offset: 0},
                {color: {r: 0, g: 1, b: 0, a: 0}, offset: 1}
            ],
            1,
            1
        )).toBeDeepCloseTo([
            {color: {r: 1, g: 0, b: 0, a: 0}, offset: 0},
            {color: {r: 0, g: 1, b: 0, a: 0}, offset: 1}
        ]);

        expect(computeGradientPoints(
            -5,
            5,
            [
                {color: {r: 1, g: 0, b: 0, a: 0}, offset: 0},
                {color: {r: 0, g: 1, b: 0, a: 0}, offset: 1}
            ],
            10,
            1
        )).toBeDeepCloseTo([
            {color: {r: 1, g: 0, b: 0, a: 0}, offset: 0},
            {color: {r: 1, g: 0, b: 0, a: 0}, offset: 0.5},
            {color: {r: 0.5, g: 0.5, b: 0, a: 0}, offset: 1}
        ]);

        expect(computeGradientPoints(
            5,
            15,
            [
                {color: {r: 1, g: 0, b: 0, a: 0}, offset: 0},
                {color: {r: 0, g: 1, b: 0, a: 0}, offset: 0.5},
                {color: {r: 0, g: 0, b: 1, a: 0}, offset: 1}
            ],
            20,
            1
        )).toBeDeepCloseTo([
            {color: {r: 0.5, g: 0.5, b: 0, a: 0}, offset: 0},
            {color: {r: 0, g: 1, b: 0, a: 0}, offset: 0.5},
            {color: {r: 0, g: 0.5, b: 0.5, a: 0}, offset: 1}
        ]);
    });

    it('should pick texture size', () => {
        const clr = color.OPAQUE_BLACK;

        // not enough data
        expect(pickTextureSize([
        ])).toEqual(1);
        expect(pickTextureSize([
            {color: clr, offset: 0}
        ])).toEqual(1);

        // edge case
        expect(pickTextureSize([
            {color: clr, offset: 0},
            {color: clr, offset: 1}
        ])).toEqual(2);

        // "good" set
        expect(pickTextureSize([
            {color: clr, offset: 0},
            {color: clr, offset: 1 / 2},
            {color: clr, offset: 1}
        ])).toEqual(3);
        expect(pickTextureSize([
            {color: clr, offset: 0},
            {color: clr, offset: 1 / 3},
            {color: clr, offset: 2 / 3},
            {color: clr, offset: 1}
        ])).toEqual(4);
        expect(pickTextureSize([
            {color: clr, offset: 0},
            {color: clr, offset: 1 / 4},
            {color: clr, offset: 2 / 4},
            {color: clr, offset: 3 / 4},
            {color: clr, offset: 1}
        ])).toEqual(5);
        expect(pickTextureSize([
            {color: clr, offset: 1 / 4},
            {color: clr, offset: 1}
        ])).toEqual(5);
        expect(pickTextureSize([
            {color: clr, offset: 0},
            {color: clr, offset: 3 / 4}
        ])).toEqual(5);
        expect(pickTextureSize([
            {color: clr, offset: 1 / 4},
            {color: clr, offset: 1 / 2}
        ])).toEqual(5);
        expect(pickTextureSize([
            {color: clr, offset: 1 / 2},
            {color: clr, offset: 3 / 4}
        ])).toEqual(5);

        // not so "good" set
        expect(pickTextureSize([
            {color: clr, offset: 0.2},
            {color: clr, offset: 0.5}
        ])).toEqual(11);
        expect(pickTextureSize([
            {color: clr, offset: 0.4},
            {color: clr, offset: 0.7}
        ])).toEqual(11);

        // arbitrary set
        expect(pickTextureSize([
            {color: clr, offset: 0.1},
            {color: clr, offset: 0.5},
            {color: clr, offset: 0.54}
        ])).toEqual(51);
        expect(pickTextureSize([
            {color: clr, offset: 0},
            {color: clr, offset: 0.1},
            {color: clr, offset: 0.4},
            {color: clr, offset: 0.8},
            {color: clr, offset: 1}
        ])).toEqual(11);
        expect(pickTextureSize([
            {color: clr, offset: 0.1},
            {color: clr, offset: 0.11},
            {color: clr, offset: 0.111},
            {color: clr, offset: 0.1111},
            {color: clr, offset: 0.8}
        ])).toEqual(256);
    });
});
