import {expect} from 'chai';
import {parseGeoLine} from '../../../../../../src/vector_render_engine/content_provider/graphics/content_manager/content_processing_task/parsers/parse_geo_line';
import * as color from '../../../../../../src/vector_render_engine/util/color';
import {areColorsFuzzyEqual} from '../../../../util/color';

const METADATA = new Map([['key', 'value']]);

describe('parseGeoLine', () => {
    it('should parse line', () => {
        const polylines = parseGeoLine({
            type: 'LineString',
            coordinates: [{x: 0, y: 0}, {x: 0, y: 1}]
        }, {
            "line-color": '#3F3',
            "line-opacity": 1,
            "line-width": 5
        }, 0, METADATA);

        expect(polylines.length).equals(1);
        expect(polylines[0].vertices).deep.equals([{x: 0, y: 0}, {x: 0, y: 1}]);
        expect(polylines[0].metadata).to.deep.equal(METADATA);
        const inlineStyles = polylines[0].styles[0].inline!;
        expect(inlineStyles).not.to.be.undefined;
        expect(inlineStyles.strokeWidth).equals(5);
        expect(areColorsFuzzyEqual(
            inlineStyles.strokeColor,
            color.create(0x33 / 0xff, 0xff / 0xff, 0x33 / 0xff, 1)
        ));
    });

    it('should not parse line with less than 2 vertices', () => {
        const polylines = parseGeoLine({
            type: 'LineString',
            coordinates: [{x: 0, y: 0}]
        }, {
            'line-color': '#fff',
            'line-width': 10
        }, 0, undefined);

        expect(polylines.length).to.equal(0);
    });

    it('should not parse line with equal points of a real length < 2', () => {
        const polylines = parseGeoLine({
            type: 'LineString',
            coordinates: [{x: 0, y: 0}, {x: 0, y: 0}, {x: 0, y: 0}]
        }, {
            'line-color': '#fff',
            'line-width': 10
        }, 0, undefined);

        expect(polylines.length).to.equal(0);
    });
});
