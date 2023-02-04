import {SyncStorageMessages} from 'vector_render_engine/content_provider/base_vector_content_provider/storage/sync_storage_messages';
import {GraphicsDetalizationLevelSerialized} from 'vector_render_engine/content_provider/graphics/graphics_detaliztion_level/graphics_detalization_level_serialized';
import {GraphicsDetalizationLevelStorage} from 'vector_render_engine/content_provider/graphics/graphics_detaliztion_level/graphics_detalization_level_storage';
import {createPairOfCommunicators, PairOfCommunicators} from '../../vector_tile_map/util/backend_communicator';
import {GraphicsObjectStorageStub} from '../graphics_object/graphics_object_storage_stub';

export class GraphicsDetalizationLevelStorageStub extends GraphicsDetalizationLevelStorage {
    readonly communicators: PairOfCommunicators<SyncStorageMessages<GraphicsDetalizationLevelSerialized, string>>;
    readonly objectStorage: GraphicsObjectStorageStub;
    
    constructor() {
        const communicators = createPairOfCommunicators() as this['communicators'];
        const objectStorage = new GraphicsObjectStorageStub();
        super(communicators.master, objectStorage);

        this.communicators = communicators;
        this.objectStorage = objectStorage;
    }
}
