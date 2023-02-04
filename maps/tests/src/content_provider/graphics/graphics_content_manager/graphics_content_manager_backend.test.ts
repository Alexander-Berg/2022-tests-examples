import * as chai from 'chai';
import {expect} from 'chai';
import {SinonStub, stub} from 'sinon';
import * as sinonChai from 'sinon-chai';
import {GraphicsDetalizationLevelBackend} from 'vector_render_engine/content_provider/graphics/graphics_detaliztion_level/graphics_detalization_level_backend';
import {GraphicsDetalizationLevelState} from 'vector_render_engine/content_provider/graphics/graphics_detaliztion_level/graphics_detalization_level_state';
import {GraphicsGroupBackend} from 'vector_render_engine/content_provider/graphics/graphics_group/graphics_group_backend';
import {GraphicsFeature} from 'vector_render_engine/content_provider/graphics/graphics_object/graphics_object_description';
import {GlyphAtlasRegistryBackend} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/glyph_atlas/glyph_atlas_registry_backend';
import {GlyphAtlasRegistryMessages} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/glyph_atlas/glyph_atlas_registry_messages';
import {GpuMemoryPageRegistryBackend} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/gpu_memory/gpu_memory_page_registry_backend';
import {GpuMemoryPageRegistryMessages} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/gpu_memory/gpu_memory_page_registry_messages';
import {IconAtlasRegistryBackend} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/icon_atlas/icon_atlas_registry_backend';
import {IconAtlasRegistryMessages} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/icon_atlas/icon_atlas_registry_messages';
import {VectorPrimitiveTypes} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/util/vector_primitive_types';
import {GraphicsContentManagerBackend} from '../../../../../src/vector_render_engine/content_provider/graphics/content_manager/graphics_content_manager_backend';
import {AddPrimitives, GraphicsContentManagerMessages, GraphicsContentManagerMessageType, RemovePrimitives} from '../../../../../src/vector_render_engine/content_provider/graphics/content_manager/graphics_content_manager_messages';
import {GraphicsObjectBackend} from '../../../../../src/vector_render_engine/content_provider/graphics/graphics_object/graphics_object_backend';
import {ObjectIdProvider} from '../../../../../src/vector_render_engine/util/id_provider';
import {TaskQueueStub} from '../../../util/task_queue_stub';
import {FontLoaderStub} from '../../vector_tile_map/glyph_atlas/util/font_loader_stub';
import {createPairOfCommunicators, PairOfCommunicators} from '../../vector_tile_map/util/backend_communicator';
import {LoadManagerStub} from '../../vector_tile_map/util/load_manager_stub';
import {GraphicsDetalizationLevelStorageBackendStub} from '../graphics_detalization_level/graphics_detalization_level_storage_backend_stub';
import {GraphicsViewportBackendStub} from '../graphics_viewport/graphics_viewport_backend_stub';

chai.use(sinonChai);

describe('GraphicsContentManagerBackend', () => {
    let manager: GraphicsContentManagerBackend;
    let viewport: GraphicsViewportBackendStub;
    let storage: GraphicsDetalizationLevelStorageBackendStub;
    let gpuRegistry: GpuMemoryPageRegistryBackend;
    let glyphRegistry: GlyphAtlasRegistryBackend;
    let iconRegistry: IconAtlasRegistryBackend;
    let idProvider: ObjectIdProvider;
    let taskQueue: TaskQueueStub;
    let loadManager: LoadManagerStub;
    let fontLoader: FontLoaderStub;

    let managerCommunicators: PairOfCommunicators<GraphicsContentManagerMessages>;
    let gpuCommunicators: PairOfCommunicators<GpuMemoryPageRegistryMessages>;
    let glyphCommunicators: PairOfCommunicators<GlyphAtlasRegistryMessages>;
    let iconCommunicators: PairOfCommunicators<IconAtlasRegistryMessages>;

    let onMessageStub: SinonStub;
    let root: GraphicsGroupBackend;
    let object: GraphicsObjectBackend;

    beforeEach(() => {
        managerCommunicators = createPairOfCommunicators();
        gpuCommunicators = createPairOfCommunicators();
        glyphCommunicators = createPairOfCommunicators();
        iconCommunicators = createPairOfCommunicators();

        idProvider = {idGenerator: () => 0, collidingIdGenerator: () => 1};
        taskQueue = new TaskQueueStub();
        loadManager = new LoadManagerStub();
        fontLoader = new FontLoaderStub(loadManager);
        viewport = new GraphicsViewportBackendStub();
        storage = new GraphicsDetalizationLevelStorageBackendStub();
        gpuRegistry = new GpuMemoryPageRegistryBackend(gpuCommunicators.worker);
        glyphRegistry = new GlyphAtlasRegistryBackend(glyphCommunicators.worker, fontLoader);
        iconRegistry = new IconAtlasRegistryBackend(iconCommunicators.worker, loadManager);
        manager = new GraphicsContentManagerBackend(
            managerCommunicators.worker,
            storage,
            viewport,
            gpuRegistry,
            glyphRegistry,
            iconRegistry,
            idProvider,
            taskQueue,
            loadManager,
            0
        );

        onMessageStub = stub();
        managerCommunicators.master.onMessage.addListener(onMessageStub);

        root = new GraphicsGroupBackend('root', {opacity: 1, zIndex: 0}, null);
        object = new GraphicsObjectBackend('object', FEATURE, {zIndex: 0, simplificationRate: 0}, root);
    });

    it('should start content task when detalization level is added to the storage', () => {
        storage.add('l', new GraphicsDetalizationLevelBackend('l', object, 0, GraphicsDetalizationLevelState.VISIBLE));

        expect(taskQueue.size).to.equal(1);
    });

    it('should flush updates of memory registry and submit primitives to the main thread', async () => {
        storage.add('l', new GraphicsDetalizationLevelBackend('l', object, 0, GraphicsDetalizationLevelState.VISIBLE));

        taskQueue.dequeueAll();

        const message = onMessageStub.lastCall.args[0] as AddPrimitives;
        expect(message.type).to.equal(GraphicsContentManagerMessageType.FROM_BACKEND_ADD_PRIMITIVES);
        expect(message.levelId).to.equal('l');
        expect(message.content.primitives[VectorPrimitiveTypes.POLYLINE]!.length).to.equal(1);
    });

    it('should stop a running task when level is removed', () => {
        storage.add('l', new GraphicsDetalizationLevelBackend('l', object, 0, GraphicsDetalizationLevelState.VISIBLE));

        storage.remove('l');

        expect(taskQueue.size).to.equal(0);
    });

    it('should submit content to main thread when content is ready', () => {
        storage.add('l', new GraphicsDetalizationLevelBackend('l', object, 0, GraphicsDetalizationLevelState.VISIBLE));

        taskQueue.dequeueAll();

        const message = onMessageStub.lastCall.args[0] as AddPrimitives;
        expect(onMessageStub).to.have.been.calledOnce;
        expect(message.type).to.equal(GraphicsContentManagerMessageType.FROM_BACKEND_ADD_PRIMITIVES);
        expect(message.levelId).to.equal('l');
        expect(message.content.primitives[VectorPrimitiveTypes.OPAQUE_POLYGON]!.length).to.equal(0);
        expect(message.content.primitives[VectorPrimitiveTypes.POLYLINE]!.length).to.equal(1);
    });
});

const FEATURE: GraphicsFeature = {
    type: 'Feature',
    geometry: {
        type: 'LineString',
        coordinates: [{x: 0, y: 0}, {x: 1, y: 1}]
    },
    properties: {
        style: {
            'line-width': 10,
            'line-color': '#fff'
        }
    }
};
