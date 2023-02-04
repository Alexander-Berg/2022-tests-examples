import {expect} from 'chai';
import tileResponse from './fixtures/tile_39624_20548_16_response.json';
import pointsExtracted from './fixtures/tile_39624_20548_16_points_extracted.json';
import tileResponseUnknownClassId from './fixtures/tile_39624_20548_16_response_unknown_classId.json';
import {TileMetadata} from '../../../../../src/vector_render_engine/content_provider/vector_tile_map/tile_content_manager/content_processing_tasks/util/extract_layers';
import {extractIcons} from '../../../../../src/vector_render_engine/content_provider/vector_tile_map/tile_content_manager/content_processing_tasks/util/extract_icons';
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

const stubCustomizer: any = {customizePoint(): void { }};

describe('point extractor', () => {
    it('should return points extracted', () => {
        // chai treats undefined property and value 'undefined' as not equal,
        // JSON.stringify/parse just cuts off such properties, simplifying objects to compare
        const points = JSON.parse(JSON.stringify([
            ...extractIcons(
                TILE,
                <any>tileResponse,
                EMPTY_METADATA,
                () => 0,
                [],
                TileSize.X4,
                MapMode.DEFAULT,
                stubCustomizer
            )
        ]));

        expect(points).to.be.deep.equal(pointsExtracted);
    });

    it('should not fail parsing tile with unknown classId', () => {
        const points = [
            ...extractIcons(
                TILE,
                <any>tileResponseUnknownClassId,
                EMPTY_METADATA,
                () => 0,
                [],
                TileSize.X4,
                MapMode.DEFAULT,
                stubCustomizer
            )
        ];

        expect(points.length).to.be.greaterThan(0);
    });

});
