import {expect} from 'chai';
import {GraphicsGroupStorageBackend} from 'vector_render_engine/content_provider/graphics/graphics_group/graphics_group_storage_backend';
import {GraphicsGroupBackend} from 'vector_render_engine/content_provider/graphics/graphics_group/graphics_group_backend';
import {PairOfCommunicators, createPairOfCommunicators} from '../../vector_tile_map/util/backend_communicator';
import {SyncStorageMessages} from 'vector_render_engine/content_provider/base_vector_content_provider/storage/sync_storage_messages';
import {GraphicsGroupSerialized} from 'vector_render_engine/content_provider/graphics/graphics_group/graphics_group_serialized';

describe('GraphicsGroupStorageBackend', () => {
    let storage: GraphicsGroupStorageBackend;
    let root: GraphicsGroupBackend;
    let group: GraphicsGroupBackend;
    let communicators: PairOfCommunicators<SyncStorageMessages<GraphicsGroupSerialized, string>>;

    beforeEach(() => {
        communicators = createPairOfCommunicators();
        storage = new GraphicsGroupStorageBackend(communicators.worker);
        root = new GraphicsGroupBackend('root', STYLE, null);
        group = new GraphicsGroupBackend('group', STYLE, root);
    });

    it('should add child to the parent group', () => {
        storage.add('group', group);

        expect(root.children).to.include(group);
    });

    it('should remove child from the parent group', () => {
        storage.add('group', group);

        storage.remove('group');

        expect(root.children).not.to.include(group);
    })
});

const STYLE = {zIndex: 0, opacity: 1};
