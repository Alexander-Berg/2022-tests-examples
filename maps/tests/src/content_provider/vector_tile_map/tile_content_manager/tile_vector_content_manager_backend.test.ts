import * as chai from 'chai';
import {expect} from 'chai';
import {spy} from 'sinon';
import * as sinonChai from 'sinon-chai';
import {createPairOfCommunicators, PairOfCommunicators} from '../util/backend_communicator';
import {
    TileContentManagerMessageType,
    TileVectorContentManagerMessages
} from '../../../../../src/vector_render_engine/content_provider/vector_tile_map/tile_content_manager/tile_vector_content_manager_messages';
import {GpuMemoryPageRegistryMessages} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/gpu_memory/gpu_memory_page_registry_messages';
import {TileVectorContentManagerBackend} from '../../../../../src/vector_render_engine/content_provider/vector_tile_map/tile_content_manager/tile_vector_content_manager_backend';
import {VectorPrimitiveTypes} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/util/vector_primitive_types';
import {GpuMemoryPageRegistryBackend} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/gpu_memory/gpu_memory_page_registry_backend';
import {TileStorageBackend} from '../../../../../src/vector_render_engine/content_provider/vector_tile_map/tile_storage/tile_storage_backend';
import {TaskQueueStub} from '../../../util/task_queue_stub';
import {
    VectorTileBackend,
    VectorTileBackendState
} from '../../../../../src/vector_render_engine/content_provider/vector_tile_map/tile/vector_tile_backend';
import {TileContentRequest} from '../../../../../src/vector_render_engine/content_provider/vector_tile_map/tile_content_manager/content_processing_tasks/tile_content_request';
import {promisifyEventEmitter} from '../../../util/event_emitter';
import {GlyphAtlasRegistryMessages} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/glyph_atlas/glyph_atlas_registry_messages';
import {GlyphAtlasRegistryBackend} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/glyph_atlas/glyph_atlas_registry_backend';
import {FontLoaderStub} from '../glyph_atlas/util/font_loader_stub';
import {IconAtlasRegistryBackend} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/icon_atlas/icon_atlas_registry_backend';
import {IconAtlasRegistryMessages} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/icon_atlas/icon_atlas_registry_messages';
import {LoadManagerStub} from '../util/load_manager_stub';
import {VectorContentBackend} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/util/vector_content_backend';
import {ModelStorageBackendStub} from '../model_storage/model_storage_backend_stub';
import {TileViewportBackend} from '../../../../../src/vector_render_engine/content_provider/vector_tile_map/viewport/tile_viewport_backend';
import {ViewportMessages} from '../../../../../src/vector_render_engine/content_provider/vector_tile_map/viewport/tile_viewport_messages';
import {ObjectIdProvider} from '../../../../../src/vector_render_engine/util/id_provider';
import {SyncStorageMessages} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/storage/sync_storage_messages';
import {IdentifiedTileItem} from '../../../../../src/vector_render_engine/content_provider/vector_tile_map/util/identified_tile_item';
import {Type} from '../../../../../src/vector_render_engine/render/gl/enums';

chai.use(sinonChai);

const STUB_LOCATION = {
    vertexByteOffset: 0,
    vertexByteLength: 0,
    indexByteOffset: 0,
    indexByteLength: 0,
    indexType: Type.UNSIGNED_SHORT,
    bufferId: 0
};

const MIN_COLLIDING_ID = 0;
const MIN_ID = 128;

class TestTileContentRequest extends TileContentRequest {
    emulateReadyContent(primitives: VectorContentBackend): void {
        this._onContentReady.fire({
            content: primitives,
            metrics: {}
        });
    }
}

class TestTileVectorContentManagerBackend extends TileVectorContentManagerBackend {
    latestRequest: TestTileContentRequest;

    _createTileRequest(tile: VectorTileBackend): TileContentRequest {
        const objectIdProvider = this._objectIdManager.createSharedIdProvider(tile.id);
        return this.latestRequest = new TestTileContentRequest(
            tile,
            this._options,
            this._taskQueue,
            this._tileLoader,
            this._viewport,
            this._taskPriority,
            this._opaquePolygonBufferWriter,
            this._transparentPolygonBufferWriter,
            this._extrudedPolygonWriter,
            this._polylineBufferWriter,
            this._pointLabelBufferWriter,
            this._curvedLabelBufferWriter,
            this._iconBufferWriter,
            this._registry,
            this._glyphAtlasRegistry,
            this._iconAtlasRegistry,
            objectIdProvider,
            this._stylesCustomizer
        );
    }
}

describe('TileVectorContentManagerBackend', () => {
    let storageCommunicators: PairOfCommunicators<SyncStorageMessages<IdentifiedTileItem, string>>;
    let contentManagerCommunicators: PairOfCommunicators<TileVectorContentManagerMessages>;
    let memoryRegistryCommunicators: PairOfCommunicators<GpuMemoryPageRegistryMessages>;
    let glyphAtlasRegistryCommunicators: PairOfCommunicators<GlyphAtlasRegistryMessages>;
    let iconAtlasRegistryCommunicators: PairOfCommunicators<IconAtlasRegistryMessages>;
    let viewportCommunicators: PairOfCommunicators<ViewportMessages>;
    let storage: TileStorageBackend;
    let modelStorage: ModelStorageBackendStub;
    let memoryRegistry: GpuMemoryPageRegistryBackend;
    let glyphAtlasRegistry: GlyphAtlasRegistryBackend;
    let iconAtlasRegistry: IconAtlasRegistryBackend;
    let viewport: TileViewportBackend;
    let contentManager: TestTileVectorContentManagerBackend;
    let taskQueue: TaskQueueStub;
    let loadManager: LoadManagerStub;
    let objectIdProvider: ObjectIdProvider;
    let fontLoader: FontLoaderStub;

    beforeEach(() => {
        storageCommunicators = createPairOfCommunicators();
        contentManagerCommunicators = createPairOfCommunicators();
        memoryRegistryCommunicators = createPairOfCommunicators();
        glyphAtlasRegistryCommunicators = createPairOfCommunicators();
        iconAtlasRegistryCommunicators = createPairOfCommunicators();
        viewportCommunicators = createPairOfCommunicators();
        storage = new TileStorageBackend(storageCommunicators.worker);
        modelStorage = new ModelStorageBackendStub();
        memoryRegistry = new GpuMemoryPageRegistryBackend(memoryRegistryCommunicators.worker);
        viewport = new TileViewportBackend(viewportCommunicators.worker, storage);
        taskQueue = new TaskQueueStub();
        loadManager = new LoadManagerStub();
        fontLoader = new FontLoaderStub(loadManager);
        let i = MIN_COLLIDING_ID;
        let j = MIN_ID;
        objectIdProvider = {collidingIdGenerator: () => i++, idGenerator: () => j++};
        glyphAtlasRegistry = new GlyphAtlasRegistryBackend(glyphAtlasRegistryCommunicators.worker, fontLoader);
        iconAtlasRegistry = new IconAtlasRegistryBackend(iconAtlasRegistryCommunicators.worker, loadManager);
        contentManager = new TestTileVectorContentManagerBackend(
            contentManagerCommunicators.worker,
            storage,
            modelStorage,
            memoryRegistry,
            glyphAtlasRegistry,
            iconAtlasRegistry,
            viewport,
            objectIdProvider,
            taskQueue,
            loadManager,
            0
        );
    });

    it('create request by appearing a new tile in storage', () => {
        const createRequest = spy(contentManager, '_createTileRequest');
        const tile1 = storage.getOrCreate({id: '1', x: 1, y: 1, zoom: 1}, VectorTileBackendState.VISIBLE);

        expect(createRequest).to.have.been.calledWith(tile1);
    });

    it('should flush updates of memory registry and submit primitives to the main thread', async () => {
        storage.getOrCreate({id: '1', x: 1, y: 1, zoom: 1}, VectorTileBackendState.VISIBLE);
        const memoryPage = memoryRegistry.getFreeOrCreatePage({vertexByteSize: 0, attributes: []});

        const flushUpdate = spy(memoryRegistry, 'flushBackendUpdates');

        const addPrimitive = promisifyEventEmitter(contentManagerCommunicators.master.onMessage).then(([message]) => {
            if (message.type === TileContentManagerMessageType.FROM_BACKEND_ADD_PRIMITIVES) {
                expect(message.tileId).to.be.deep.equal('1');
                expect(message.content.primitives[VectorPrimitiveTypes.OPAQUE_POLYGON]!.length).to.be.deep.equal(2);
            } else {
                throw new Error('not FROM_BACKEND_ADD_PRIMITIVES message caught');
            }
        });

        contentManager.latestRequest.emulateReadyContent({
            primitives: {
                [VectorPrimitiveTypes.OPAQUE_POLYGON]: [
                    {location: {...STUB_LOCATION, bufferId: memoryPage.id}},
                    {location: {...STUB_LOCATION, bufferId: memoryPage.id}}
                ]
            }
        });

        expect(flushUpdate).to.have.been.called;
        await addPrimitive;
    });
});
