import {expect} from 'chai';
import {GlyphAtlasRegistry} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/glyph_atlas/glyph_atlas_registry';
import {GlyphAtlasRegistryMessages} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/glyph_atlas/glyph_atlas_registry_messages';
import {GpuMemoryPageRegistry} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/gpu_memory/gpu_memory_page_registry';
import {GpuMemoryPageRegistryMessages, GpuMemoryRegistryMessageType} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/gpu_memory/gpu_memory_page_registry_messages';
import {IconAtlasRegistry} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/icon_atlas/icon_atlas_registry';
import {IconAtlasRegistryMessages, IconAtlasRegistryUpdateType} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/icon_atlas/icon_atlas_registry_messages';
import {SyncStorageMessageType} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/storage/sync_storage_messages';
import {VectorPrimitiveTypes} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/util/vector_primitive_types';
import {GraphicsContentManager} from '../../../../../src/vector_render_engine/content_provider/graphics/content_manager/graphics_content_manager';
import {GraphicsContentManagerMessages, GraphicsContentManagerMessageType} from '../../../../../src/vector_render_engine/content_provider/graphics/content_manager/graphics_content_manager_messages';
import {RenderablePolyline} from '../../../../../src/vector_render_engine/primitive/polyline/renderable_polyline';
import RenderContext from '../../../../../src/vector_render_engine/render/context';
import {Type} from '../../../../../src/vector_render_engine/render/gl/enums';
import WebGLRenderingContextStubImpl from '../../../../util/webgl_rendering_context';
import {createPairOfCommunicators, PairOfCommunicators} from '../../vector_tile_map/util/backend_communicator';
import {GraphicsDetalizationLevelStorageStub} from '../graphics_detalization_level/graphics_detalization_level_storage_stub';
import {GraphicsGroupStorageStub} from '../graphics_group/graphics_group_storage_stub';
import {GraphicsObjectStorageStub} from '../graphics_object/graphics_object_storage_stub';

describe('GraphicsContentManager', () => {
    let manager: GraphicsContentManager;
    let managerCommunicators: PairOfCommunicators<GraphicsContentManagerMessages>;
    let gpuRegistryCommunicators: PairOfCommunicators<GpuMemoryPageRegistryMessages>;
    let glyphRegistryCommunicators: PairOfCommunicators<GlyphAtlasRegistryMessages>;
    let iconRegistryCommunicators: PairOfCommunicators<IconAtlasRegistryMessages>;
    let stubContext: RenderContext;
    let detalizationStorage: GraphicsDetalizationLevelStorageStub;
    let objectStorage: GraphicsObjectStorageStub;
    let groupStorage: GraphicsGroupStorageStub;
    let gpuRegistry: GpuMemoryPageRegistry;
    let glyphRegistry: GlyphAtlasRegistry;
    let iconRegistry: IconAtlasRegistry;

    beforeEach(() => {
        managerCommunicators = createPairOfCommunicators();
        gpuRegistryCommunicators = createPairOfCommunicators();
        glyphRegistryCommunicators = createPairOfCommunicators();
        iconRegistryCommunicators = createPairOfCommunicators();
        stubContext = new RenderContext(new WebGLRenderingContextStubImpl());

        detalizationStorage = new GraphicsDetalizationLevelStorageStub();
        objectStorage = detalizationStorage.objectStorage;
        groupStorage = objectStorage.groupStorage;
        gpuRegistry = new GpuMemoryPageRegistry(gpuRegistryCommunicators.master, stubContext);
        glyphRegistry = new GlyphAtlasRegistry(
            glyphRegistryCommunicators.master,
            {glyphRangeUrlTemplate: ''},
            stubContext
        );
        iconRegistry = new IconAtlasRegistry(
            iconRegistryCommunicators.master,
            {iconUrlTemplate: ''},
            stubContext
        );
        manager = new GraphicsContentManager(
            managerCommunicators.master,
            detalizationStorage,
            gpuRegistry,
            glyphRegistry,
            iconRegistry,
            {},
            ADDITIONAL_METADATA
        );

        groupStorage.communicators.worker.sendMessage({
            type: SyncStorageMessageType.FROM_BACKEND_ADD,
            id: 'root',
            item: {
                id: 'root',
                parentId: null,
                style: {zIndex: 0, opacity: 1}
            }
        }, true);
        objectStorage.communicators.worker.sendMessage({
            type: SyncStorageMessageType.FROM_BACKEND_ADD,
            id: 'object',
            item: {
                id: 'object',
                parentId: 'root'
            }
        }, true)
    });

    it('should instantiate primitives', () => {
        detalizationStorage.communicators.worker.sendMessage({
            type: SyncStorageMessageType.FROM_BACKEND_ADD,
            id: 'level1',
            item: {id: 'level1', objectId: 'object', version: 0, zIndex: 0, zoom: 0}
        }, true);
        detalizationStorage.communicators.worker.sendMessage({
            type: SyncStorageMessageType.FROM_BACKEND_ADD,
            id: 'level2',
            item: {id: 'level2', objectId: 'object', version: 0, zIndex: 0, zoom: 0}
        }, true);
        detalizationStorage.communicators.worker.sendMessage({
            type: SyncStorageMessageType.FROM_BACKEND_ADD,
            id: 'level3',
            item: {id: 'level3', objectId: 'object', version: 0, zIndex: 0, zoom: 0}
        }, true);
        gpuRegistryCommunicators.worker.sendMessage({
            type: GpuMemoryRegistryMessageType.FROM_BACKEND_ADD_PAGE,
            id: 0,
            attribMapping: {vertexByteSize: 0, attributes: []},
            indexDataByteSize: 100,
            vertexDataByteSize: 100
        }, true);
        iconRegistryCommunicators.worker.sendMessage({
            type: IconAtlasRegistryUpdateType.FROM_BACKEND_ADD_ATLAS,
            atlasId: 0,
            atlasHeight: 32,
            atlasWidth: 32
        }, true);

        managerCommunicators.worker.sendMessage({
            type: GraphicsContentManagerMessageType.FROM_BACKEND_ADD_PRIMITIVES,
            content: {
                primitives: {
                    [VectorPrimitiveTypes.OPAQUE_POLYGON]: [{location: STUB_LOCATION}],
                }
            },
            levelId: 'level1'
        }, true);
        managerCommunicators.worker.sendMessage({
            type: GraphicsContentManagerMessageType.FROM_BACKEND_ADD_PRIMITIVES,
            content: {
                primitives: {
                    [VectorPrimitiveTypes.TRANSPARENT_POLYGON]: [{location: STUB_LOCATION}],
                }
            },
            levelId: 'level2'
        }, true);
        managerCommunicators.worker.sendMessage({
            type: GraphicsContentManagerMessageType.FROM_BACKEND_ADD_PRIMITIVES,
            content: {
                primitives: {
                    [VectorPrimitiveTypes.POLYLINE]: [{location: STUB_LOCATION, atlasId: 0, opacity: 1}]
                }
            },
            levelId: 'level3'
        }, true);

        const level1 = detalizationStorage.get('level1')!;
        const level2 = detalizationStorage.get('level2')!;
        const level3 = detalizationStorage.get('level3')!;
        expect(level1.content.primitives[VectorPrimitiveTypes.OPAQUE_POLYGON]!.length).to.equal(1);
        expect(level2.content.primitives[VectorPrimitiveTypes.TRANSPARENT_POLYGON]!.length).to.equal(1);
        expect(level3.content.primitives[VectorPrimitiveTypes.POLYLINE]!.length).to.equal(1);
    });

    it('should remove primitives', () => {
        detalizationStorage.communicators.worker.sendMessage({
            type: SyncStorageMessageType.FROM_BACKEND_ADD,
            id: 'level',
            item: {id: 'level', objectId: 'object', version: 0, zIndex: 0, zoom: 0}
        }, true);
        const level = detalizationStorage.get('level')!;
        level.addContent({
            primitives: {
                [VectorPrimitiveTypes.POLYLINE]: [new RenderablePolyline({} as any, {} as any, {} as any, 1)]
            }
        });

        managerCommunicators.worker.sendMessage({
            type: GraphicsContentManagerMessageType.FROM_BACKEND_REMOVE_PRIMITIVES,
            levelId: 'level',
            content: [VectorPrimitiveTypes.POLYLINE]
        }, true);

        expect(level.content.primitives[VectorPrimitiveTypes.POLYLINE]!.length).to.equal(0);
    });
});

const ADDITIONAL_METADATA = new Map([['key', 'value']]);

const STUB_LOCATION = {
    vertexByteOffset: 0,
    vertexByteLength: 0,
    indexByteOffset: 0,
    indexByteLength: 0,
    indexType: Type.UNSIGNED_SHORT,
    bufferId: 0
};
