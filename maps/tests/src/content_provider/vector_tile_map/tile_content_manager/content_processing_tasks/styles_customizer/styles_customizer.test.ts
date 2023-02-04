import {expect} from 'chai';
import * as sinon from 'sinon';
import {StylesCustomizer} from '../../../../../../../src/vector_render_engine/content_provider/vector_tile_map/tile_content_manager/content_processing_tasks/styles_customizer/styles_customizer';
import {CustomizationConfig} from '../../../../../../../src/vector_render_engine/content_provider/vector_tile_map/tile_content_manager/util/customization/config';
import {PrimitiveTags} from '../../../../../../../src/vector_render_engine/content_provider/vector_tile_map/tile_content_manager/content_processing_tasks/util/feature_metadata';
import {PrimitiveTag} from '../../../../../../../src/vector_render_engine/content_provider/vector_tile_map/tile_content_manager/content_processing_tasks/proto_aliases';
import {ParsedPrimitiveStyle} from '../../../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/content_manager/content_processing_tasks/primitive/parsed_primitive_style';
import {ParsedLabelStyle} from '../../../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/content_manager/content_processing_tasks/primitive/parsed_label_style';
import {ParsedIconStyle} from '../../../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/content_manager/content_processing_tasks/primitive/parsed_icon_style';
import {ParsedPolygonStyle} from '../../../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/content_manager/content_processing_tasks/primitive/parsed_polygon_style';
import {ParsedPolylineStyle} from '../../../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/content_manager/content_processing_tasks/primitive/parsed_polyline_style';
import {LabelAlignment} from '../../../../../../../src/vector_render_engine/primitive/label/label_style';
import {LineJoin, LineCap} from '../../../../../../../src/vector_render_engine/primitive/polyline/polyline_style';
import Color from '../../../../../../../src/vector_render_engine/util/color';

// These are some "integration" tests that roughly check that parts work together.
describe('StylesCustomizer', () => {
    const common: ParsedPrimitiveStyle = {minZoom: 1, maxZoom: 2, zIndex: -1, classId: 10};
    const clr: Color = {r: 0, g: 0, b: 0, a: 1};
    let customizer: StylesCustomizer;
    let logStub: sinon.SinonStub;

    beforeEach(() => {
        customizer = new StylesCustomizer();
        logStub = sinon.stub(console, 'log');
    });

    afterEach(() => {
        logStub.restore();
    });

    it('should apply customization / filter by type', () => {
        const label: ParsedLabelStyle = {
            ...common, distance: 1, align: LabelAlignment.CENTER,
            styles: [{fontId: 'test-font', fontSize: 10, color: clr, outlineColor: clr}]
        };
        const polygon: ParsedPolygonStyle = {
            ...common, extruded: false, height: 10, color: clr
        };
        function check(
            config: CustomizationConfig,
            labelColor: Color,
            labelOutlineColor: Color,
            polygonColor: Color
        ): void {
            customizer.update(config);
            customizer.customizePointLabel([label], [], 15);
            customizer.customizePolygon([polygon], [], 15);

            expect(label.styles[0].color).to.deep.equal(labelColor);
            expect(label.styles[0].outlineColor).to.deep.equal(labelOutlineColor);
            expect(polygon.color).to.deep.equal(polygonColor);
        }

        check([{
            types: 'point',
            stylers: {
                color: 'f00'
            }
        }], {r: 1, g: 0, b: 0, a: 1}, {r: 1, g: 0, b: 0, a: 1}, clr);

        check([{
            types: 'polygon',
            stylers: {
                color: '00ff00'
            }
        }], {r: 1, g: 0, b: 0, a: 1}, {r: 1, g: 0, b: 0, a: 1}, {r: 0, g: 1, b: 0, a: 1});

        check([{
            types: ['point', 'polygon'],
            stylers: {
                color: '00f0'
            }
        }], {r: 0, g: 0, b: 1, a: 0}, {r: 0, g: 0, b: 1, a: 0}, {r: 0, g: 0, b: 1, a: 0});

        check([{
            stylers: {
                color: 'fff'
            }
        }], {r: 1, g: 1, b: 1, a: 1}, {r: 1, g: 1, b: 1, a: 1}, {r: 1, g: 1, b: 1, a: 1});
    });

    it('should apply customization / filter by element', () => {
        const icon: ParsedIconStyle = {
            ...common, imageId: 'test-image', offset: {x: 1, y: 2}, scale: 1
        };
        const polyline: ParsedPolylineStyle = {
            ...common,
            inline: {
                join: LineJoin.BEVEL, startCap: LineCap.BUTT, endCap: LineCap.BUTT, strokeWidth: 10, strokeColor: clr
            },
            outline: {
                join: LineJoin.BEVEL, startCap: LineCap.BUTT, endCap: LineCap.BUTT, strokeWidth: 10, strokeColor: clr
            }
        };
        function check(
            config: CustomizationConfig,
            iconScale: number | undefined,
            polylineInnerScale: number,
            polylineOuterScale: number
        ): void {
            customizer.update(config);
            customizer.customizePoint([icon], [], 14);
            customizer.customizePolyline([polyline], [], 14);

            expect(icon.scaleFactor).to.equal(iconScale);
            expect(polyline.inline!.strokeWidth).to.equal(polylineInnerScale);
            expect(polyline.outline!.strokeWidth).to.equal(polylineOuterScale);
        }

        check([{
            elements: 'geometry.fill',
            stylers: {
                scale: 2
            }
        }], undefined, 20, 10);

        check([{
            elements: ['geometry.outline', 'label.icon'],
            stylers: {
                scale: 3
            }
        }], 3, 20, 30);

        check([{
            stylers: {
                scale: 0.5
            }
        }], 0.5, 10, 15);
    });

    it('should apply customization / filter by tags', () => {
        const polygon: ParsedPolygonStyle = {
            ...common, extruded: false, height: 10, color: clr
        };
        function check(config: CustomizationConfig, tags: PrimitiveTags, clr: Color): void {
            customizer.update(config);
            customizer.customizePolygon([polygon], tags, 16);

            expect(polygon.color).to.deep.equal(clr);
        }

        check([{
            tags: {
                all: ['building', 'country']
            },
            stylers: {
                color: '0f0'
            }
        }], [PrimitiveTag.BUILDING], clr);

        check([{
            tags: {
                all: ['building', 'country']
            },
            stylers: {
                color: '0f0'
            }
        }], [PrimitiveTag.BUILDING, PrimitiveTag.COUNTRY, PrimitiveTag.DISTRICT], {r: 0, g: 1, b: 0, a: 1});

        check([{
            tags: {
                any: ['building', 'country']
            },
            stylers: {
                color: '00f'
            }
        }], [PrimitiveTag.COUNTRY], {r: 0, g: 0, b: 1, a: 1});

        check([{
            tags: {
                none: ['building', 'country']
            },
            stylers: {
                color: 'f00'
            }
        }], [PrimitiveTag.COUNTRY], {r: 0, g: 0, b: 1, a: 1});

        check([{
            tags: {
                none: ['building', 'country']
            },
            stylers: {
                color: 'f00'
            }
        }], [PrimitiveTag.ADDRESS], {r: 1, g: 0, b: 0, a: 1});
    });
});
