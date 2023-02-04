import {expect} from 'chai';
import * as sinon from 'sinon';
import {parseElements, Elements} from '../../../../../../../src/vector_render_engine/content_provider/vector_tile_map/tile_content_manager/content_processing_tasks/styles_customizer/elements';
import {parseTypes, Types} from '../../../../../../../src/vector_render_engine/content_provider/vector_tile_map/tile_content_manager/content_processing_tasks/styles_customizer/types';
import {parseStylers} from '../../../../../../../src/vector_render_engine/content_provider/vector_tile_map/tile_content_manager/content_processing_tasks/styles_customizer/stylers';

describe('StylesCustomizer: parsing', () => {
    let warnStub: sinon.SinonStub;
    beforeEach(() => {
        warnStub = sinon.stub(console, 'warn');
    });
    afterEach(() => {
        warnStub.restore();
    });

    describe('parseTypes', () => {
        it('should handle unknown values', () => {
            expect(parseTypes('test.unknown')).to.equal(Types.None);
            expect(parseTypes(['point', 'test.unknown'])).to.equal(Types.None);
            expect(warnStub.callCount).to.equal(2);
        });

        it('should handle no value case', () => {
            expect(parseTypes(undefined)).to.equal(Types.All);
            expect(warnStub.callCount).to.equal(0);
        });

        it('should parse known values', () => {
            expect(parseTypes('point')).to.equal(Types.Point);
            expect(parseTypes([])).to.equal(Types.None);
            expect(parseTypes(['polygon', 'polyline'])).to.equal(Types.Polygon | Types.Polyline);
        });
    });

    describe('parseElements', () => {
        it('should handle unknown values', () => {
            expect(parseElements('test.unknown')).to.equal(Elements.None);
            expect(parseElements(['geometry.fill', 'test.unknown'])).to.equal(Elements.None);
            expect(warnStub.callCount).to.equal(2);
        });

        it('should handle no value case', () => {
            expect(parseElements(undefined)).to.equal(Elements.All);
            expect(warnStub.callCount).to.equal(0);
        });

        it('should parse known values', () => {
            expect(parseElements('geometry.fill')).to.equal(Elements.GeometryFill);
            expect(parseElements([])).to.equal(Elements.None);
            expect(parseElements(['geometry.fill.pattern', 'label.icon']))
                .to.equal(Elements.GeometryFillPattern | Elements.LabelIcon);
        });
    });

    describe('parseStylers', () => {
        it('should handle no value case', () => {
            expect(parseStylers(undefined)).to.deep.equal([]);
            expect(warnStub.callCount).to.equal(0);
        });

        it('should parse known values', () => {
            expect(parseStylers([]).length).to.equal(0);
            expect(parseStylers({}).length).to.equal(1);
            expect(parseStylers([{}, {}]).length).to.equal(2);
        });

        it('should parse zoom matchers', () => {
            const stylers = parseStylers([
                {},
                {zoom: 3},
                {zoom: [4]},
                {zoom: [5, 8]}
            ]);

            expect(stylers[0].matchZoom(1000)).to.equal(true);

            expect(stylers[1].matchZoom(3)).to.equal(true);
            expect(stylers[1].matchZoom(4)).to.equal(false);

            expect(stylers[2].matchZoom(4)).to.equal(true);
            expect(stylers[2].matchZoom(3)).to.equal(false);

            expect(stylers[3].matchZoom(5)).to.equal(true);
            expect(stylers[3].matchZoom(7)).to.equal(true);
            expect(stylers[3].matchZoom(8)).to.equal(true);
            expect(stylers[3].matchZoom(4)).to.equal(false);
            expect(stylers[3].matchZoom(9)).to.equal(false);
        });

        it('should parse styler properties', () => {
            const stylers = parseStylers([
                {visibility: 'off', scale: 0.3},
                {scale: 1.2, color: 'f0f', opacity: 0.2},
                {hue: '0f0', saturation: 0.2, lightness: 0.3, opacity: 0.7},
                {color: '#f00', "secondary-color": "#0f0", "tertiary-color": "#00f"}
            ]);

            expect(stylers[0].properties).to.deep.equal({isHidden: true, scale: 0.3});
            expect(stylers[1].properties).to.deep.equal({scale: 1.2, color: {r: 1, g: 0, b: 1, a: 1}, opacity: 0.2});

            expect(stylers[2].properties.opacity).to.equal(0.7);
            expect(stylers[2].properties.hue).to.equal(2);
            expect(stylers[2].properties.colorTransform).to.be.not.undefined;

            expect(stylers[3].properties.color).to.deep.equal({r: 1, g: 0, b: 0, a: 1});
            expect(stylers[3].properties.secondaryColor).to.deep.equal({r: 0, g: 1, b: 0, a: 1});
            expect(stylers[3].properties.tertiaryColor).to.deep.equal({r: 0, g: 0, b: 1, a: 1});
        });
    });
});
