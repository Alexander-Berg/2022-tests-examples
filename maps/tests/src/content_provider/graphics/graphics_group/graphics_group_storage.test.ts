import {expect} from 'chai';
import {GraphicsGroupStorage} from 'vector_render_engine/content_provider/graphics/graphics_group/graphics_group_storage';
import {PairOfCommunicators, createPairOfCommunicators} from '../../vector_tile_map/util/backend_communicator';
import {SyncStorageMessages, SyncStorageMessageType} from 'vector_render_engine/content_provider/base_vector_content_provider/storage/sync_storage_messages';
import {GraphicsGroupSerialized} from 'vector_render_engine/content_provider/graphics/graphics_group/graphics_group_serialized';

describe('GraphicsGroupStorage', () => {
    let storage: GraphicsGroupStorage;
    let communicators: PairOfCommunicators<SyncStorageMessages<GraphicsGroupSerialized, string>>;

    beforeEach(() => {
        communicators = createPairOfCommunicators();
        storage = new GraphicsGroupStorage(communicators.master);

        communicators.worker.sendMessage({
            type: SyncStorageMessageType.FROM_BACKEND_ADD,
            id: 'root',
            item: {id: 'root', style: STYLE, parentId: null}
        }, true);
    });

    it('should add child to the parent group', () => {
        communicators.worker.sendMessage({
            type: SyncStorageMessageType.FROM_BACKEND_ADD,
            id: 'group',
            item: {id: 'group', style: STYLE, parentId: 'root'}
        }, true);

        const root = storage.get('root')!;
        const group = storage.get('group')!;
        expect(root.children).to.include(group);
    });

    it('should remove child from the parent group', () => {
        communicators.worker.sendMessage({
            type: SyncStorageMessageType.FROM_BACKEND_ADD,
            id: 'group',
            item: {id: 'group', style: STYLE, parentId: 'root'}
        }, true);
        const group = storage.get('group')!;
        
        communicators.worker.sendMessage({
            type: SyncStorageMessageType.FROM_BACKEND_REMOVE,
            id: 'group'
        }, true);

        const root = storage.get('root')!;
        expect(root.children).not.to.include(group);
    });
});

const STYLE = {zIndex: 0, opacity: 1};
