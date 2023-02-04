import {expect} from 'chai';
import {GraphicsDetalizationLevelStorageBackend} from 'vector_render_engine/content_provider/graphics/graphics_detaliztion_level/graphics_detalization_level_storage_backend';
import {PairOfCommunicators, createPairOfCommunicators} from '../../vector_tile_map/util/backend_communicator';
import {SyncStorageMessages} from 'vector_render_engine/content_provider/base_vector_content_provider/storage/sync_storage_messages';
import {GraphicsDetalizationLevelSerialized} from 'vector_render_engine/content_provider/graphics/graphics_detaliztion_level/graphics_detalization_level_serialized';
import {GraphicsObjectBackend} from 'vector_render_engine/content_provider/graphics/graphics_object/graphics_object_backend';
import {GraphicsGroupBackend} from 'vector_render_engine/content_provider/graphics/graphics_group/graphics_group_backend';
import {GraphicsDetalizationLevelBackend} from 'vector_render_engine/content_provider/graphics/graphics_detaliztion_level/graphics_detalization_level_backend';
import {GraphicsDetalizationLevelState} from 'vector_render_engine/content_provider/graphics/graphics_detaliztion_level/graphics_detalization_level_state';

describe('GraphicsDetalizationLevelStorageBackend', () => {
    let storage: GraphicsDetalizationLevelStorageBackend;
    let communicators: PairOfCommunicators<SyncStorageMessages<GraphicsDetalizationLevelSerialized, string>>;
    let object: GraphicsObjectBackend;
    let group: GraphicsGroupBackend;

    beforeEach(() => {
        communicators = createPairOfCommunicators();
        storage = new GraphicsDetalizationLevelStorageBackend(communicators.worker);
        group = new GraphicsGroupBackend('group', {zIndex: 0, opacity: 1}, null);
        object = new GraphicsObjectBackend('object', {} as any, {zIndex: 0, simplificationRate: 0}, group);
    });

    it('should add level to the object', () => {
        const level = new GraphicsDetalizationLevelBackend('level', object, 0, GraphicsDetalizationLevelState.VISIBLE);

        storage.add('level', level);

        expect(object.detalizationLevels).to.include(level);
    });

    it('should remove level from the object', () => {
        const level = new GraphicsDetalizationLevelBackend('level', object, 0, GraphicsDetalizationLevelState.VISIBLE);
        storage.add('level', level);

        storage.remove('level');

        expect(object.detalizationLevels).not.to.include(level);
    });
});
