import {GraphicsDetalizationLevelStorageBackend} from 'vector_render_engine/content_provider/graphics/graphics_detaliztion_level/graphics_detalization_level_storage_backend';
import {createPairOfCommunicators, PairOfCommunicators} from '../../vector_tile_map/util/backend_communicator';
import {SyncStorageMessages} from 'vector_render_engine/content_provider/base_vector_content_provider/storage/sync_storage_messages';
import {GraphicsDetalizationLevelSerialized} from 'vector_render_engine/content_provider/graphics/graphics_detaliztion_level/graphics_detalization_level_serialized';

export class GraphicsDetalizationLevelStorageBackendStub extends GraphicsDetalizationLevelStorageBackend {
    readonly communicators: PairOfCommunicators<SyncStorageMessages<GraphicsDetalizationLevelSerialized, string>>;
    
    constructor() {
        const communicators = createPairOfCommunicators() as this['communicators'];
        super(communicators.worker);

        this.communicators = communicators;
    }
}