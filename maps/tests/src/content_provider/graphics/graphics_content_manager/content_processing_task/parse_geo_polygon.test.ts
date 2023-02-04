import {expect} from 'chai';
import {parseGeoPolygon, parseGeoPolygonContours} from '../../../../../../src/vector_render_engine/content_provider/graphics/content_manager/content_processing_task/parsers/parse_geo_polygon';
import {Polygon} from '../../../../../../src/vector_render_engine/content_provider/graphics/util/geojson';
import * as color from '../../../../../../src/vector_render_engine/util/color';
import {areColorsFuzzyEqual} from '../../../../util/color';
import {GraphicsFillStyle} from 'vector_render_engine/content_provider/graphics/graphics_object/graphics_object_description';

const METADATA = new Map([['key', 'value']]);
const GEOMETRY: Polygon = {
    type: 'Polygon',
    coordinates: [[
        {x: 0, y: 0},
        {x: 1, y: 0},
        {x: 0, y: 1},
        {x: 1, y: 1}
    ]]
};
const STYLE: GraphicsFillStyle = {
    'fill-color': '#f00',
    'fill-contour-width': 1,
    'fill-contour-color': '#0f0'
};

describe('parsers/parse_geo_polygon', () => {
    describe('parseGeoPolygon', () => {
        it('should parse polygon', () => {
            const polygons = parseGeoPolygon(GEOMETRY, STYLE, 0, METADATA);

            expect(polygons.length).to.equal(1);
            expect(polygons[0].id).to.equal(0);
            expect(polygons[0].metadata).to.deep.equal(METADATA);
            expect(areColorsFuzzyEqual(
                polygons[0].styles[0].color,
                color.create(1, 0, 0, 1)
            )).to.equal(true);
        });

        it('should not parse polygon with less than 3 vertices', () => {
            const polygons = parseGeoPolygon({
                type: 'Polygon',
                coordinates: [[{x: 0, y: 0}, {x: 1, y: 1}]]
            }, STYLE, 0, undefined);

            expect(polygons.length).to.equal(0);
        });
    });

    describe('parseGeoPolygonContours', () => {
        it('should parse polygon contours', () => {
            const contours = parseGeoPolygonContours(GEOMETRY, STYLE, 0, METADATA);

            expect(contours.length).to.equal(1);
            expect(contours[0].id).to.equal(0);
            expect(contours[0].metadata).to.deep.equal(METADATA);
            expect(areColorsFuzzyEqual(
                contours[0].styles[0].inline!.strokeColor,
                color.create(0, 1, 0, 1)
            )).to.equal(true);
        });

        it('should not parse contour with less than 3 vertices', () => {
            const contours = parseGeoPolygonContours({
                type: 'Polygon',
                coordinates: [[{x: 0, y: 0}, {x: 1, y: 1}]]
            }, STYLE, 0, undefined);

            expect(contours.length).to.equal(0);
        })
    });
});
