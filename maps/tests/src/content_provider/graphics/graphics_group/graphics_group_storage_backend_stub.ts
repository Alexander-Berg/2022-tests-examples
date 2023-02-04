import {GraphicsGroupStorageBackend} from 'vector_render_engine/content_provider/graphics/graphics_group/graphics_group_storage_backend';
import {PairOfCommunicators, createPairOfCommunicators} from '../../vector_tile_map/util/backend_communicator';
import {SyncStorageMessages} from 'vector_render_engine/content_provider/base_vector_content_provider/storage/sync_storage_messages';
import {GraphicsGroupSerialized} from 'vector_render_engine/content_provider/graphics/graphics_group/graphics_group_serialized';

export class GraphicsGroupStorageBackendStub extends GraphicsGroupStorageBackend {
    readonly communicators: PairOfCommunicators<SyncStorageMessages<GraphicsGroupSerialized, string>>;
    
    constructor() {
        const communicators = createPairOfCommunicators() as this['communicators'];
        super(communicators.worker);

        this.communicators = communicators;
    }
}