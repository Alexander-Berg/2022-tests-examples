import {TileModelVisibilityManager} from '../../../../../src/vector_render_engine/content_provider/vector_tile_map/visibility_manager/tile_model_visibility_manager';
import {VectorTile} from '../../../../../src/vector_render_engine/content_provider/vector_tile_map/tile/vector_tile';
import {ModelStorageStub} from '../model_storage/model_storage_stub';

export class ModelVisibilityManagerStub extends TileModelVisibilityManager {
    constructor() {
        super({} as any, new ModelStorageStub());
    }

    showTileModels(tile: VectorTile): void {}

    hideTileModels(tile: VectorTile): void {}
}
