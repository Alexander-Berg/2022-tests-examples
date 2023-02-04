import {expect} from 'chai';
import * as sinon from 'sinon';
import {applyStyler, combineStylers} from '../../../../../../../src/vector_render_engine/content_provider/vector_tile_map/tile_content_manager/content_processing_tasks/styles_customizer/stylers';
import {PrimitiveAdapter} from '../../../../../../../src/vector_render_engine/content_provider/vector_tile_map/tile_content_manager/content_processing_tasks/styles_customizer/primitive_adapters';
import {ParsedPrimitiveStyle} from '../../../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/content_manager/content_processing_tasks/primitive/parsed_primitive_style';

describe('StylesCustomizer: stylers', () => {
    describe('applyStyler', () => {
        const stubStyle: ParsedPrimitiveStyle = {minZoom: 1, maxZoom: 2, zIndex: -1, classId: 1};
        let adapter: {[Key in keyof PrimitiveAdapter<ParsedPrimitiveStyle>]: sinon.SinonStub}

        beforeEach(() => {
            adapter = {
                hide: sinon.stub(),
                changeScale: sinon.stub(),
                changeColor: sinon.stub()
            };
        });

        it('should does nothing if there are no properties', () => {
            applyStyler(adapter, stubStyle, {});

            expect(adapter.hide.called).to.equal(false);
            expect(adapter.changeScale.called).to.equal(false);
            expect(adapter.changeColor.called).to.equal(false);
        });

        it('should hide', () => {
            applyStyler(adapter, stubStyle, {isHidden: true});

            expect(adapter.hide.lastCall.args).to.deep.equal([stubStyle]);
        });

        it('should change scale', () => {
            applyStyler(adapter, stubStyle, {scale: 0.3});

            expect(adapter.changeScale.lastCall.args).to.deep.equal([stubStyle, 0.3]);
        });

        it('should change color', () => {
            applyStyler(adapter, stubStyle, {color: {r: 1, g: 0, b: 1, a: 1}});

            expect(adapter.changeColor.lastCall.args.length).to.equal(2);
        });

        it('should omit other changes if hide is applied', () => {
            applyStyler(adapter, stubStyle, {isHidden: true, scale: 0.1, color: {r: 0, g: 0, b: 0, a: 1}});

            expect(adapter.hide.lastCall.args).to.deep.equal([stubStyle]);
            expect(adapter.changeScale.called).to.equal(false);
            expect(adapter.changeColor.called).to.equal(false);
        });

        it('should change scale and color', () => {
            applyStyler(adapter, stubStyle, {scale: 0.4, color: {r: 1, g: 0, b: 1, a: 1}});

            expect(adapter.changeScale.lastCall.args).to.deep.equal([stubStyle, 0.4]);
            expect(adapter.changeColor.lastCall.args.length).to.equal(2);
        });
    });

    describe('combineStylers', () => {
        it('should handle edge cases', () => {
            expect(combineStylers([])).to.deep.equal({});
            expect(combineStylers([{}])).to.deep.equal({});
            expect(combineStylers([{}, {}, {}])).to.deep.equal({});
        });

        it('should merge several objects', () => {
            const transform1 = [
                0, 1, 0, 2,
                0, 2, 1, 1,
                1, 2, 0, 0,
                0, 0, 0, 1
            ];
            const transform2 = [
                1, 0, 2, 1,
                0, 0, 1, 1,
                0, 2, 2, 1,
                0, 1, 1, 0
            ];

            expect(combineStylers([
                {colorTransform: transform1, scale: 0.1}
            ])).to.deep.equal({colorTransform: transform1, scale: 0.1});
            expect(combineStylers([
                {isHidden: true, opacity: 0.3},
                {opacity: 0.2},
                {colorTransform: transform2}
            ])).to.deep.equal({isHidden: true, opacity: 0.2, colorTransform: transform2});
            expect(combineStylers([
                {scale: 0.3, color: {r: 1, g: 0, b: 0, a: 1}, hue: 3},
                {color: {r: 0, g: 1, b: 0, a: 1}, opacity: 0.1},
                {scale: 0.6, opacity: 0.5}
            ])).to.deep.equal({scale: 0.6, color: {r: 0, g: 1, b: 0, a: 1}, opacity: 0.5, hue: 3});
        });
    });
});
