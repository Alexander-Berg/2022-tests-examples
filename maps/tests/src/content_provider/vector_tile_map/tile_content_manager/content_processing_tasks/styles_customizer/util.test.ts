import {expect} from 'chai';
import {
    getColorHue,
    setColorHue,
    makeColorTransformer
} from '../../../../../../../src/vector_render_engine/content_provider/vector_tile_map/tile_content_manager/util/customization/util';

describe('StylesCustomizer: util', () => {
    describe('getColorHue', () => {
        it('should return color hue', () => {
            expect(getColorHue({r: 0, g: 0, b: 0, a: 1})).to.equal(undefined);
            expect(getColorHue({r: 0.3, g: 0.3, b: 0.3, a: 1})).to.equal(undefined);

            expect(getColorHue({r: 1, g: 0, b: 0, a: 0})).to.equal(0);
            expect(getColorHue({r: 0, g: 1, b: 0, a: 0})).to.equal(2);
            expect(getColorHue({r: 0, g: 0, b: 1, a: 0})).to.equal(4);

            expect(getColorHue({r: 0.2, g: 0.1, b: 0.6, a: 1})).closeTo(4.2, 1E-8);
        })
    });

    describe('setColorHue', () => {
        it('should change color hue', () => {
            expect(setColorHue({r: 0.2, g: 0.3, b: 0.4, a: 1}, 0)).to.deep.equal({r: 0.4, g: 0.2, b: 0.2, a: 1});
            expect(setColorHue({r: 0.2, g: 0.3, b: 0.4, a: 1}, 2)).to.deep.equal({r: 0.2, g: 0.4, b: 0.2, a: 1});
            expect(setColorHue({r: 0.2, g: 0.3, b: 0.4, a: 1}, 4)).to.deep.equal({r: 0.2, g: 0.2, b: 0.4, a: 1});

            expect(setColorHue({r: 0.2, g: 0.3, b: 0.4, a: 1}, 3)).to.deep.equal({r: 0.2, g: 0.4, b: 0.4, a: 1});
        });
    });

    describe('makeColorTransformer', () => {
        it('should not make transformer when nothing is defined', () => {
            expect(
                makeColorTransformer(undefined, undefined, undefined, undefined, undefined, undefined)
            ).to.equal(undefined);
        });

        it('should make transformer that sets color', () => {
            const transformer = makeColorTransformer(
                {r: 0.3, g: 0.1, b: 0.5, a: 0.9},
                undefined,
                undefined,
                undefined,
                undefined,
                undefined
            );

            expect(transformer!.transform({r: 1, g: 2, b: 3, a: 4})).to.deep.equal({r: 0.3, g: 0.1, b: 0.5, a: 0.9});
            expect(transformer!.key).to.equal(-435707093);
        });

        it('should make transformer that changes opacity', () => {
            const transformer = makeColorTransformer(undefined, undefined, undefined, 0.5, undefined, undefined);

            expect(transformer!.transform({r: 1, g: 2, b: 3, a: 4})).to.deep.equal({r: 1, g: 2, b: 3, a: 2});
            expect(transformer!.key).to.equal(-1640531400);
        });

        it('should make transformer that changes hue', () => {
            const transformer = makeColorTransformer(undefined, 3, undefined, undefined, undefined, undefined);

            expect(transformer!.transform({r: 1, g: 2, b: 3, a: 4})).to.deep.equal({r: 1, g: 3, b: 3, a: 4});
            expect(transformer!.key).to.equal(-1640531400);
        });

        it('should make transformer that applies matrix', () => {
            const transformer = makeColorTransformer(undefined, undefined, [
                0.5, 0.1, 0.2, 0.0,
                0.0, 0.3, 0.1, 0.0,
                0.0, 0.2, 0.2, 0.7,
                0.9, 0.6, 0.0, 0.9
            ], undefined, undefined, undefined);

            const ret = transformer!.transform({r: 0.1, g: 0.2, b: 0.3, a: 0.4});
            expect(ret.r).closeTo(0.13, 1E-8);
            expect(ret.g).closeTo(0.09, 1E-8);
            expect(ret.b).closeTo(0.80, 1E-8);
            expect(ret.a).closeTo(0.40, 1E-8);
            expect(transformer!.key).to.equal(-1930722967);
        });

        it('should prefer color over hue and matrix', () => {
            const transformer =
                makeColorTransformer({r: 0.1, g: 0, b: 0, a: 1}, 3, [1], undefined, undefined, undefined);

            expect(transformer!.transform({r: 0.3, g: 0.2, b: 0.1, a: 0})).to.deep.equal({r: 0.1, g: 0, b: 0, a: 1});
        });

        it('should apply opacity over other changes', () => {
            const transformer =
                makeColorTransformer({r: 0.1, g: 0.2, b: 0.3, a: 0.5}, undefined, undefined, 0.2, undefined, undefined);

            expect(transformer!.transform({r: 1, g: 1, b: 1, a: 1})).to.deep.equal({r: 0.1, g: 0.2, b: 0.3, a: 0.1});
        });

        it('should transform image pixels', () => {
            const transformer = makeColorTransformer(
                {r: 1, g: 0, b: 0, a: 1},
                undefined,
                undefined,
                0.5,
                undefined,
                undefined
            );

            expect(transformer!.transformImage({
                size: {width: 4, height: 3},
                data: new Uint8Array([120, 220, 160, 240, 150, 120, 170, 255])
            })).to.deep.equal({
                size: {width: 4, height: 3},
                data: new Uint8Array([120, 220, 160, 120, 150, 120, 170, 127])
            })
            expect(transformer!.key).to.equal(-662422020);
            expect(transformer!.colorQuery).to.equal('&c1=ff0000');
        });
    });
});
