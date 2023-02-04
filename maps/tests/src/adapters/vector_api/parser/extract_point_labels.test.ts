import {expect} from 'chai';
import tileResponse from './fixtures/tile_39624_20548_16_response.json';
import pointLabelsExtracted from './fixtures/tile_39624_20548_16_point_labels_extracted.json';
import tileResponseUnknownClassId from './fixtures/tile_39624_20548_16_response_unknown_classId.json';
import {extractPointLabels} from '../../../../../src/vector_render_engine/content_provider/vector_tile_map/tile_content_manager/content_processing_tasks/util/extract_point_labels';
import {TileMetadata} from '../../../../../src/vector_render_engine/content_provider/vector_tile_map/tile_content_manager/content_processing_tasks/util/extract_layers';
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

const stubCustomizer: any = {customizePointLabel(): void { }};

describe('point label extractor', () => {
    it('should return point labels extracted', () => {
        // chai treats undefined property and value 'undefined' as not equal,
        // JSON.stringify/parse just cuts off such properties, simplifying objects to compare
        const pointLabels = JSON.parse(JSON.stringify(
            [...extractPointLabels(
                TILE,
                <any>tileResponse,
                EMPTY_METADATA,
                () => 0,
                TileSize.X4,
                MapMode.DEFAULT,
                stubCustomizer
            )]
        ));

        expect(pointLabels).to.be.deep.equal(pointLabelsExtracted);
    });

    it('should not fail parsing tile with unknown classId', () => {
        const pointLabels = [...extractPointLabels(
            TILE,
            <any>tileResponseUnknownClassId,
            EMPTY_METADATA,
            () => 0,
            TileSize.X4,
            MapMode.DEFAULT,
            stubCustomizer
        )];

        expect(pointLabels.length).to.be.greaterThan(0);
    });

});
