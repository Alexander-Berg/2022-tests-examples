import {expect} from 'chai';
import {PairOfCommunicators, createPairOfCommunicators} from '../../vector_tile_map/util/backend_communicator';
import {SyncStorageMessages, SyncStorageMessageType} from 'vector_render_engine/content_provider/base_vector_content_provider/storage/sync_storage_messages';
import {GraphicsObjectSerialized} from 'vector_render_engine/content_provider/graphics/graphics_object/graphics_object_serialized';
import {GraphicsObjectStorage} from 'vector_render_engine/content_provider/graphics/graphics_object/graphics_object_storage';
import {GraphicsGroupStorageStub} from '../graphics_group/graphics_group_storage_stub';

describe('GraphicsObjectStorage', () => {
    let storage: GraphicsObjectStorage;
    let communicators: PairOfCommunicators<SyncStorageMessages<GraphicsObjectSerialized, string>>;
    let groupStorage: GraphicsGroupStorageStub;
    
    beforeEach(() => {
        communicators = createPairOfCommunicators();
        groupStorage = new GraphicsGroupStorageStub();
        storage = new GraphicsObjectStorage(communicators.master, groupStorage);

        groupStorage.communicators.worker.sendMessage({
            type: SyncStorageMessageType.FROM_BACKEND_ADD,
            id: 'group',
            item: {id: 'group', style: {zIndex: 0, opacity: 0}, parentId: null}
        }, true);
    });
    
    it('should add child to the parent group', () => {
        communicators.worker.sendMessage({
            type: SyncStorageMessageType.FROM_BACKEND_ADD,
            id: 'object',
            item: {id: 'object', parentId: 'group'}
        }, true);

        const group = groupStorage.get('group')!;
        const object = storage.get('object')!;
        expect(group.children).to.include(object);
    });

    it('should remove child from the parent group', () => {
        communicators.worker.sendMessage({
            type: SyncStorageMessageType.FROM_BACKEND_ADD,
            id: 'object',
            item: {id: 'object', parentId: 'group'}
        }, true);
        const object = storage.get('object')!;

        communicators.worker.sendMessage({
            type: SyncStorageMessageType.FROM_BACKEND_REMOVE,
            id: 'object'
        }, true);

        const group = groupStorage.get('group')!;
        expect(group.children).not.to.include(object);
    });
});