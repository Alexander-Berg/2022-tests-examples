import {VectorTile} from '../../../../../src/vector_render_engine/content_provider/vector_tile_map/tile/vector_tile';
import {VectorContentVisibilityManager} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/visibility_manager/vector_content_visibility_manager';

export class TileContentVisibilityManagerStub extends VectorContentVisibilityManager {
    readonly visibleTiles: Set<VectorTile>;

    constructor() {
        super({} as any);
        this.visibleTiles = new Set();
    }

    showObject(tile: VectorTile): void {
        this.visibleTiles.add(tile);
    }

    hideObject(tile: VectorTile): void {
        this.visibleTiles.delete(tile);
    }
}
