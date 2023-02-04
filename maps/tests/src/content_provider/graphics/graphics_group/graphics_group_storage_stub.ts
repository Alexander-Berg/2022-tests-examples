import {GraphicsGroupStorage} from 'vector_render_engine/content_provider/graphics/graphics_group/graphics_group_storage';
import {createPairOfCommunicators, PairOfCommunicators} from '../../vector_tile_map/util/backend_communicator';
import {SyncStorageMessages} from 'vector_render_engine/content_provider/base_vector_content_provider/storage/sync_storage_messages';
import {GraphicsGroupSerialized} from 'vector_render_engine/content_provider/graphics/graphics_group/graphics_group_serialized';

export class GraphicsGroupStorageStub extends GraphicsGroupStorage {
    readonly communicators: PairOfCommunicators<SyncStorageMessages<GraphicsGroupSerialized, string>>;
    
    constructor() {
        const communicators = createPairOfCommunicators() as this['communicators'];
        super(communicators.master);

        this.communicators = communicators;
    }
}
