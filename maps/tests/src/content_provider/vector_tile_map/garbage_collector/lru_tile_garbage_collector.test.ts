import {expect} from 'chai';
import {TileStorageBackend} from '../../../../../src/vector_render_engine/content_provider/vector_tile_map/tile_storage/tile_storage_backend';
import {LRUTileGarbageCollector} from '../../../../../src/vector_render_engine/content_provider/vector_tile_map/garbage_collector/lru_tile_garbage_collector';
import {createPairOfCommunicators} from '../util/backend_communicator';
import {TaskQueueStub} from '../../../util/task_queue_stub';
import {VectorTileBackendState} from '../../../../../src/vector_render_engine/content_provider/vector_tile_map/tile/vector_tile_backend';
import {ModelStorageBackendStub} from '../model_storage/model_storage_backend_stub';
import {VectorModelBackend} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/model_storage/vector_model_backend';
import {SyncStorageMessages} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/storage/sync_storage_messages';
import {IdentifiedTileItem} from '../../../../../src/vector_render_engine/content_provider/vector_tile_map/util/identified_tile_item';

describe('LRUTileGarbageCollector', () => {
    const TILE_1_1 = {id: '1', x: 1, y: 1, zoom: 1};
    const TILE_2_1 = {id: '2', x: 2, y: 1, zoom: 1};
    const TILE_3_1 = {id: '3', x: 3, y: 1, zoom: 1};
    const TILE_4_1 = {id: '4', x: 4, y: 1, zoom: 1};

    let storage: TileStorageBackend;
    let modelStorage: ModelStorageBackendStub;
    let taskQueue: TaskQueueStub;
    let gc: LRUTileGarbageCollector;

    beforeEach(() => {
        storage = new TileStorageBackend(
            createPairOfCommunicators<SyncStorageMessages<IdentifiedTileItem, string>>().worker
        );
        modelStorage = new ModelStorageBackendStub();
        taskQueue = new TaskQueueStub();
        gc = new LRUTileGarbageCollector(storage, modelStorage, taskQueue, 3);
    });

    it('should put in tiles with content only', () => {
        const tile1 = storage.getOrCreate(TILE_1_1, VectorTileBackendState.VISIBLE);
        const tile2 = storage.getOrCreate(TILE_2_1, VectorTileBackendState.VISIBLE);
        const tile3 = storage.getOrCreate(TILE_3_1, VectorTileBackendState.VISIBLE);

        tile1.addContent(1 as any);
        tile2.addContent(2 as any);
        tile3.addContent(3 as any);


    });

    it('should it remove CACHED tiles from storage', () => {
        const tile1 = storage.getOrCreate(TILE_1_1, VectorTileBackendState.VISIBLE);
        const tile2 = storage.getOrCreate(TILE_2_1, VectorTileBackendState.VISIBLE);
        const tile3 = storage.getOrCreate(TILE_3_1, VectorTileBackendState.VISIBLE);
        const tile4 = storage.getOrCreate(TILE_4_1, VectorTileBackendState.VISIBLE);

        tile1.addContent(1 as any);
        tile2.addContent(2 as any);
        tile3.addContent(3 as any);
        tile3.addContent(4 as any);

        expect(storage.size).to.be.equal(4);

        tile1.state = VectorTileBackendState.CACHED;
        tile2.state = VectorTileBackendState.CACHED;
        taskQueue.dequeueAll();
        expect(storage.size).to.be.equal(4);

        tile3.state = VectorTileBackendState.CACHED;
        tile4.state = VectorTileBackendState.CACHED;
        taskQueue.dequeueAll();
        expect(storage.size).to.be.equal(3);
    });

    it('should preserve in cache tiles that was most recently VISIBLE', () => {
        const tile1 = storage.getOrCreate(TILE_1_1, VectorTileBackendState.VISIBLE);
        const tile2 = storage.getOrCreate(TILE_2_1, VectorTileBackendState.VISIBLE);
        const tile3 = storage.getOrCreate(TILE_3_1, VectorTileBackendState.VISIBLE);

        tile1.addContent(1 as any);
        tile2.addContent(2 as any);
        tile3.addContent(3 as any);

        tile1.state = VectorTileBackendState.CACHED;
        tile2.state = VectorTileBackendState.CACHED;
        tile3.state = VectorTileBackendState.CACHED;

        tile1.state = VectorTileBackendState.VISIBLE;
        tile1.state = VectorTileBackendState.CACHED;

        const tile4 = storage.getOrCreate(TILE_4_1, VectorTileBackendState.VISIBLE);
        tile4.addContent(4 as any);
        tile4.state = VectorTileBackendState.CACHED;

        taskQueue.dequeueAll();
        expect(storage.size).to.be.equal(3);
        expect(storage.get(tile1.id)).not.to.be.undefined;
        expect(storage.get(tile2.id)).to.be.undefined;
        expect(storage.get(tile3.id)).not.to.be.undefined;
        expect(storage.get(tile4.id)).not.to.be.undefined;
    });

    it('should destroy models', () => {
        const model = new VectorModelBackend('model#1', 1 as any);
        modelStorage.add(model.id, model);
        model.retain();

        expect(modelStorage.size).to.be.equal(1);

        modelStorage.remove(model.id);
        model.release();

        expect(modelStorage.size).to.be.equal(0);
    });
});
