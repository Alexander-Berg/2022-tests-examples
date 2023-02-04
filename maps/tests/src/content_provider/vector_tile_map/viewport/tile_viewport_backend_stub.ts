import {TileViewportBackend} from '../../../../../src/vector_render_engine/content_provider/vector_tile_map/viewport/tile_viewport_backend';
import {createPairOfCommunicators} from '../util/backend_communicator';
import {TileStorageBackend} from '../../../../../src/vector_render_engine/content_provider/vector_tile_map/tile_storage/tile_storage_backend';
import {ViewportMessages} from '../../../../../src/vector_render_engine/content_provider/vector_tile_map/viewport/tile_viewport_messages';
import {SyncStorageMessages} from 'vector_render_engine/content_provider/base_vector_content_provider/storage/sync_storage_messages';
import {IdentifiedTileItem} from 'vector_render_engine/content_provider/vector_tile_map/util/identified_tile_item';

export class TileViewportBackendStub extends TileViewportBackend {
    constructor() {
        super(
            createPairOfCommunicators<ViewportMessages>().worker,
            new TileStorageBackend(createPairOfCommunicators<SyncStorageMessages<IdentifiedTileItem, string>>().worker)
        );
    }
}
