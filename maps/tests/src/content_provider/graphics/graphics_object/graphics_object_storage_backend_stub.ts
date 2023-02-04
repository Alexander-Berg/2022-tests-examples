import {SyncStorageMessages} from 'vector_render_engine/content_provider/base_vector_content_provider/storage/sync_storage_messages';
import {GraphicsObjectSerialized} from 'vector_render_engine/content_provider/graphics/graphics_object/graphics_object_serialized';
import {GraphicsObjectStorageBackend} from 'vector_render_engine/content_provider/graphics/graphics_object/graphics_object_storage_backend';
import {createPairOfCommunicators, PairOfCommunicators} from '../../vector_tile_map/util/backend_communicator';

export class GraphicsObjectStorageBackendStub extends GraphicsObjectStorageBackend {
    readonly communicators: PairOfCommunicators<SyncStorageMessages<GraphicsObjectSerialized, string>>;
    
    constructor() {
        const communicators = createPairOfCommunicators() as this['communicators'];
        super(communicators.worker);

        this.communicators = communicators;
    }
}
