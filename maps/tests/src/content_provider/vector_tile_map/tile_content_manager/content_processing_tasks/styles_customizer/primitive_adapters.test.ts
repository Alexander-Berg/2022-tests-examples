import {expect} from 'chai';
import {
    point,
    polylineInline,
    polylineOutline,
    polylinePattern,
    polygon,
    polygonPattern,
    labelFill,
    labelOutline
} from '../../../../../../../src/vector_render_engine/content_provider/vector_tile_map/tile_content_manager/content_processing_tasks/styles_customizer/primitive_adapters';
import {ParsedIconStyle} from '../../../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/content_manager/content_processing_tasks/primitive/parsed_icon_style';
import {ParsedPolylineStyle} from '../../../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/content_manager/content_processing_tasks/primitive/parsed_polyline_style';
import {LineCap, LineJoin} from '../../../../../../../src/vector_render_engine/primitive/polyline/polyline_style';
import {ParsedPrimitiveStyle} from '../../../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/content_manager/content_processing_tasks/primitive/parsed_primitive_style';
import {ParsedPolygonStyle} from '../../../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/content_manager/content_processing_tasks/primitive/parsed_polygon_style';
import {ParsedLabelStyle} from '../../../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/content_manager/content_processing_tasks/primitive/parsed_label_style';
import {LabelAlignment} from '../../../../../../../src/vector_render_engine/primitive/label/label_style';
import Color from '../../../../../../../src/vector_render_engine/util/color';
import {ColorTransformer} from '../../../../../../../src/vector_render_engine/content_provider/vector_tile_map/tile_content_manager/util/customization/util';

describe('StylesCustomizer: primitive adapters', () => {
    const common: ParsedPrimitiveStyle = {minZoom: 1, maxZoom: 2, zIndex: -1, classId: 10};

    const colorTransformer: ColorTransformer = {
        transform: ({r, g, b, a}: Color) => ({r: r + 0.1, g: g + 0.2, b: b + 0.3, a: a + 0.4}),
        transformImage: ({size, data}) => ({size, data: data.map(b => b + 10)}),
        colorQuery: '&c1=ff0000',
        key: 1234
    };

    describe('Point', () => {
        let style: ParsedIconStyle;

        beforeEach(() => {
            style = {
                ...common,
                scale: 1, imageId: 'test-image', offset: {x: 4, y: 3}
            };
        });

        it('should hide', () => {
            point.hide(style);

            expect(style.hidden).to.equal(true);
        });

        it('should change scale', () => {
            point.changeScale(style, 0.3);

            expect(style.scaleFactor).to.equal(0.3);
        });

        it('should change color', () => {
            point.changeColor(style, colorTransformer);

            expect(style.transformer!.key).to.equal(1234);
            expect(style.transformer!.colorQuery).to.equal('&c1=ff0000');
            expect(style.transformer!.transformImage({
                size: {width: 4, height: 3},
                data: new Uint8Array([120, 220, 160, 240, 150, 120, 170, 255])
            })).to.deep.equal({
                size: {width: 4, height: 3},
                data: new Uint8Array([130, 230, 170, 250, 160, 130, 180, 265])
            });
        });
    });

    describe('Polyline inline', () => {
        let style: ParsedPolylineStyle;

        beforeEach(() => {
            style = {
                ...common,
                inline: {
                    strokeColor: {r: 1, g: 2, b: 3, a: 4}, strokeWidth: 4,
                    startCap: LineCap.BUTT, join: LineJoin.BEVEL, endCap: LineCap.BUTT
                }
            };
        });

        it('should hide', () => {
            polylineInline.hide(style);

            expect(style.inline!.hidden).to.equal(true);
        });

        it('should change scale', () => {
            polylineInline.changeScale(style, 0.5);

            expect(style.inline!.strokeWidth).to.equal(2);
        });

        it('should change color', () => {
            polylineInline.changeColor(style, colorTransformer);

            expect(style.inline!.strokeColor).to.deep.equal({r: 1.1, g: 2.2, b: 3.3, a: 4.4});
        });
    });

    describe('Polyline outline', () => {
        let style: ParsedPolylineStyle;

        beforeEach(() => {
            style = {
                ...common,
                outline: {
                    strokeColor: {r: 1, g: 2, b: 3, a: 4}, strokeWidth: 4,
                    startCap: LineCap.BUTT, join: LineJoin.BEVEL, endCap: LineCap.BUTT
                }
            };
        });

        it('should hide', () => {
            polylineOutline.hide(style);

            expect(style.outline!.hidden).to.equal(true);
        });

        it('should change scale', () => {
            polylineOutline.changeScale(style, 0.5);

            expect(style.outline!.strokeWidth).to.equal(2);
        });

        it('should change color', () => {
            polylineOutline.changeColor(style, colorTransformer);

            expect(style.outline!.strokeColor).to.deep.equal({r: 1.1, g: 2.2, b: 3.3, a: 4.4});
        });
    });

    describe('Polyline pattern', () => {
        let style: ParsedPolylineStyle;

        beforeEach(() => {
            style = {
                ...common,
                inline: {
                    strokeColor: {r: 1, g: 2, b: 3, a: 4}, strokeWidth: 4,
                    startCap: LineCap.BUTT, join: LineJoin.BEVEL, endCap: LineCap.BUTT,
                    pattern: {
                        scale: 1, height: 3, imageId: 'test-image'
                    }
                }
            };
        });

        it('should hide', () => {
            polylinePattern.hide(style);

            expect(style.inline!.hidden).to.equal(true);
        });

        it('should scale', () => {
            polylinePattern.changeScale(style, 0.25);

            expect(style.inline!.strokeWidth).to.equal(1);
            expect(style.inline!.patternScaleFactor).to.equal(0.25);
        });

        it('should change color', () => {
            polylinePattern.changeColor(style, colorTransformer);

            expect(style.inline!.patternTransformer!.key).to.equal(1234);
            expect(style.inline!.patternTransformer!.colorQuery).to.equal('&c1=ff0000');
            expect(style.inline!.patternTransformer!.transformImage({
                size: {width: 4, height: 3},
                data: new Uint8Array([120, 220, 160, 240, 150, 120, 170, 255])
            })).to.deep.equal({
                size: {width: 4, height: 3},
                data: new Uint8Array([130, 230, 170, 250, 160, 130, 180, 265])
            });
        });
    });

    describe('Polygon inline', () => {
        let style: ParsedPolygonStyle;

        beforeEach(() => {
            style = {
                ...common,
                extruded: false, height: 9, color: {r: 1, g: 2, b: 3, a: 4}
            };
        });

        it('should hide', () => {
            polygon.hide(style);

            expect(style.hidden).to.equal(true);
        });

        it('should change color', () => {
            polygon.changeColor(style, colorTransformer);

            expect(style.color).to.deep.equal({r: 1.1, g: 2.2, b: 3.3, a: 4.4});
        });
    });

    describe('Polygon pattern', () => {
        let style: ParsedPolygonStyle;

        beforeEach(() => {
            style = {
                ...common,
                extruded: false, height: 9, color: {r: 1, g: 2, b: 3, a: 4},
                pattern: {
                    scale: 1, imageId: 'test-image'
                }
            };
        });

        it('should change color', () => {
            polygonPattern.changeColor(style, colorTransformer);

            expect(style.patternTransformer!.key).to.equal(1234);
            expect(style.patternTransformer!.colorQuery).to.equal('&c1=ff0000');
            expect(style.patternTransformer!.transformImage({
                size: {width: 4, height: 3},
                data: new Uint8Array([120, 220, 160, 240, 150, 120, 170, 255])
            })).to.deep.equal({
                size: {width: 4, height: 3},
                data: new Uint8Array([130, 230, 170, 250, 160, 130, 180, 265])
            });
        });
    });

    describe('Label fill', () => {
        let style: ParsedLabelStyle;

        beforeEach(() => {
            style = {
                ...common,
                distance: 10,
                align: LabelAlignment.CENTER,
                styles: [{
                    fontId: 'test-font',
                    fontSize: 20,
                    color: {r: 1, g: 2, b: 3, a: 4},
                    outlineColor: {r: 1, g: 2, b: 3, a: 4}
                }]
            };
        });

        it('should hide', () => {
            labelFill.hide(style);

            expect(style.styles[0].color).to.deep.equal({r: 0, g: 0, b: 0, a: 0});
        });

        it('should change scale', () => {
            labelFill.changeScale(style, 0.5);

            expect(style.styles[0].fontSize).to.equal(10);
        });

        it('should change color', () => {
            labelFill.changeColor(style, colorTransformer);

            expect(style.styles[0].color).to.deep.equal({r: 1.1, g: 2.2, b: 3.3, a: 4.4});
        });
    });

    describe('Label outline', () => {
        let style: ParsedLabelStyle;

        beforeEach(() => {
            style = {
                ...common,
                distance: 10,
                align: LabelAlignment.CENTER,
                styles: [{
                    fontId: 'test-font',
                    fontSize: 20,
                    color: {r: 1, g: 2, b: 3, a: 4},
                    outlineColor: {r: 1, g: 2, b: 3, a: 4}
                }]
            };
        });

        it('should hide', () => {
            labelOutline.hide(style);

            expect(style.styles[0].outlineColor).to.deep.equal({r: 0, g: 0, b: 0, a: 0});
        });

        it('should not change scale', () => {
            labelOutline.changeScale(style, 0.5);

            expect(style.styles[0].fontSize).to.equal(20);
        });

        it('should change color', () => {
            labelOutline.changeColor(style, colorTransformer);

            expect(style.styles[0].outlineColor).to.deep.equal({r: 1.1, g: 2.2, b: 3.3, a: 4.4});
        });
    });
});
