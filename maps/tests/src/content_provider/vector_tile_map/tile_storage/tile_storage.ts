import {Tile} from '../../../../../src/vector_render_engine/content_provider/vector_tile_map/tile/tile';
import {VectorTileStorage} from '../../../../../src/vector_render_engine/content_provider/vector_tile_map/tile_storage/tile_storage';
import {createPairOfCommunicators} from '../util/backend_communicator';
import {SyncStorageMessages} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/storage/sync_storage_messages';
import {IdentifiedTileItem} from '../../../../../src/vector_render_engine/content_provider/vector_tile_map/util/identified_tile_item';
import {VectorTile} from '../../../../../src/vector_render_engine/content_provider/vector_tile_map/tile/vector_tile';

export class TileStorageStub<TileT extends Tile<any>> extends VectorTileStorage {
    constructor() {
        super(createPairOfCommunicators<SyncStorageMessages<IdentifiedTileItem, string>>().master);
    }

    addTile(tile: VectorTile): void {
        this._add(tile.id, tile);
    }
}
