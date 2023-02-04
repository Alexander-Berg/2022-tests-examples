import {expect} from 'chai';
import {SyncStorageMessages, SyncStorageMessageType} from 'vector_render_engine/content_provider/base_vector_content_provider/storage/sync_storage_messages';
import {GraphicsDetalizationLevelSerialized} from 'vector_render_engine/content_provider/graphics/graphics_detaliztion_level/graphics_detalization_level_serialized';
import {GraphicsDetalizationLevelStorage} from 'vector_render_engine/content_provider/graphics/graphics_detaliztion_level/graphics_detalization_level_storage';
import {createPairOfCommunicators, PairOfCommunicators} from '../../vector_tile_map/util/backend_communicator';
import {GraphicsObjectStorageStub} from '../graphics_object/graphics_object_storage_stub';
``

describe('GraphicsDetalizationLevelStorage', () => {
    let storage: GraphicsDetalizationLevelStorage;
    let communicators: PairOfCommunicators<SyncStorageMessages<GraphicsDetalizationLevelSerialized, string>>;
    let objectStorage: GraphicsObjectStorageStub;

    beforeEach(() => {
        objectStorage = new GraphicsObjectStorageStub();
        communicators = createPairOfCommunicators();
        storage = new GraphicsDetalizationLevelStorage(communicators.master, objectStorage);
        objectStorage.groupStorage.communicators.worker.sendMessage({
            type: SyncStorageMessageType.FROM_BACKEND_ADD,
            id: 'group',
            item: {id: 'group', style: {zIndex: 0, opacity: 1}, parentId: null}
        }, true);
        objectStorage.communicators.worker.sendMessage({
            type: SyncStorageMessageType.FROM_BACKEND_ADD,
            id: 'object',
            item: {id: 'object', parentId: 'group'}
        }, true);
    });

    it('should add level to the object', () => {
        communicators.worker.sendMessage({
            type: SyncStorageMessageType.FROM_BACKEND_ADD,
            id: 'level',
            item: {id: 'level', zIndex: 0, objectId: 'object', zoom: 0, version: 0}
        }, true);

        const object = objectStorage.get('object')!;
        const level = storage.get('level')!;
        expect(object.detalizationLevels).to.include(level);
    });

    it('should remove level from the object', () => {
        communicators.worker.sendMessage({
            type: SyncStorageMessageType.FROM_BACKEND_ADD,
            id: 'level',
            item: {id: 'level', zIndex: 0, objectId: 'object', zoom: 0, version: 0}
        }, true);
        const level = storage.get('level')!;

        communicators.worker.sendMessage({
            type: SyncStorageMessageType.FROM_BACKEND_REMOVE,
            id: 'level'
        }, true);

        const object = objectStorage.get('object')!;
        expect(object.detalizationLevels).not.to.include(level);
    });
}); 
    
    