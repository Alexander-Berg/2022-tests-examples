import {expect} from 'chai';
import {SyncStorageMessages} from 'vector_render_engine/content_provider/base_vector_content_provider/storage/sync_storage_messages';
import {GraphicsGroupBackend} from 'vector_render_engine/content_provider/graphics/graphics_group/graphics_group_backend';
import {GraphicsObjectBackend} from 'vector_render_engine/content_provider/graphics/graphics_object/graphics_object_backend';
import {GraphicsObjectSerialized} from 'vector_render_engine/content_provider/graphics/graphics_object/graphics_object_serialized';
import {GraphicsObjectStorageBackend} from 'vector_render_engine/content_provider/graphics/graphics_object/graphics_object_storage_backend';
import {createPairOfCommunicators, PairOfCommunicators} from '../../vector_tile_map/util/backend_communicator';

describe('GrahpicsObjectStorageBackend', () => {
    let storage: GraphicsObjectStorageBackend;
    let object: GraphicsObjectBackend;
    let group: GraphicsGroupBackend;
    let communicators: PairOfCommunicators<SyncStorageMessages<GraphicsObjectSerialized, string>>;
    
    beforeEach(() => {
        communicators = createPairOfCommunicators();
        storage = new GraphicsObjectStorageBackend(communicators.worker);
        group = new GraphicsGroupBackend('group', {zIndex: 0, opacity: 1}, null);
        object = new GraphicsObjectBackend('object', {} as any, {zIndex: 0, simplificationRate: 0}, group);
    });
    
    it('should add child to the parent group', () => {
        storage.add('object', object);
        
        expect(group.children).to.include(object);
    });

    it('should remove child from the parent group', () => {
        storage.add('object', object);
        
        storage.remove('object');

        expect(group.children).not.to.include(object);
    });
});
