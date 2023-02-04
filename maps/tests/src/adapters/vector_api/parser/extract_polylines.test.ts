import {expect} from 'chai';
import tileResponse from './fixtures/tile_39624_20548_16_response.json';
import tileResponseUnknownClassId from './fixtures/tile_39624_20548_16_response_unknown_classId.json';
import polylinesExtracted from './fixtures/tile_39624_20548_16_polylines_extracted.json';
import {TileMetadata} from '../../../../../src/vector_render_engine/content_provider/vector_tile_map/tile_content_manager/content_processing_tasks/util/extract_layers';
import {extractPolylines} from '../../../../../src/vector_render_engine/content_provider/vector_tile_map/tile_content_manager/content_processing_tasks/util/extract_polylines';
import {RESERVED_ID} from '../../../../../src/vector_render_engine/id_ranges';
import {TileSize} from '../../../../../src/vector_render_engine/content_provider/vector_tile_map/util/tile_size';
import {MapMode} from '../../../../../src/vector_render_engine/content_provider/vector_tile_map/tile_content_manager/tile_vector_content_manager_options';

const ZOOM = 16;
const TILE = {x: 39623, y: 20548, zoom: ZOOM};
const EMPTY_METADATA: TileMetadata = {
    points: new Map(),
    pointLabels: new Map(),
    polylines: new Map(),
    polygons: new Map(),
    curvedLabels: new Map()
};


for (const polyline of polylinesExtracted) {
    (polyline as any).id = RESERVED_ID;
}

const stubCustomizer: any = {customizePolyline(): void { }};

describe('polyline extractor', () => {
    it('should return polylines extracted', () => {
        // chai treats undefined property and value 'undefined' as not equal,
        // JSON.stringify/parse just cuts off such properties, simplifying objects to compare
        const polylines = JSON.parse(JSON.stringify([
            ...extractPolylines(
                TILE,
                <any>tileResponse,
                EMPTY_METADATA,
                () => 0,
                TileSize.X4,
                MapMode.DEFAULT,
                stubCustomizer
            )
        ]));

        expect(polylines).to.be.deep.equal(polylinesExtracted);
    });

    it('should not fail parsing tile with unknown classId', () => {
        const polylines = [
            ...extractPolylines(
                TILE,
                <any>tileResponseUnknownClassId,
                EMPTY_METADATA,
                () => 0,
                TileSize.X4,
                MapMode.DEFAULT,
                stubCustomizer
            )
        ];

        expect(polylines.length).to.be.greaterThan(0);
    });

});
