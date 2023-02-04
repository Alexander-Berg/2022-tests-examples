import {SyncStorageMessages} from 'vector_render_engine/content_provider/base_vector_content_provider/storage/sync_storage_messages';
import {GraphicsObjectSerialized} from 'vector_render_engine/content_provider/graphics/graphics_object/graphics_object_serialized';
import {GraphicsObjectStorage} from 'vector_render_engine/content_provider/graphics/graphics_object/graphics_object_storage';
import {createPairOfCommunicators, PairOfCommunicators} from '../../vector_tile_map/util/backend_communicator';
import {GraphicsGroupStorageStub} from '../graphics_group/graphics_group_storage_stub';

export class GraphicsObjectStorageStub extends GraphicsObjectStorage {
    readonly communicators: PairOfCommunicators<SyncStorageMessages<GraphicsObjectSerialized, string>>;
    readonly groupStorage: GraphicsGroupStorageStub;
    
    constructor() {
        const communicators = createPairOfCommunicators() as this['communicators'];
        const groupStorage = new GraphicsGroupStorageStub();
        super(communicators.master, groupStorage);

        this.communicators = communicators;
        this.groupStorage = groupStorage;
    }
}
