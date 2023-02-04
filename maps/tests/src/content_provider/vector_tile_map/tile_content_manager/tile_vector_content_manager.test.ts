import {expect} from 'chai';
import {createPairOfCommunicators, PairOfCommunicators} from '../util/backend_communicator';
import {TileContentManagerMessageType, TileVectorContentManagerMessages} from '../../../../../src/vector_render_engine/content_provider/vector_tile_map/tile_content_manager/tile_vector_content_manager_messages';
import {VectorTileStorage} from '../../../../../src/vector_render_engine/content_provider/vector_tile_map/tile_storage/tile_storage';
import {SyncStorageMessages, SyncStorageMessageType} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/storage/sync_storage_messages';
import {TileVectorContentManager} from '../../../../../src/vector_render_engine/content_provider/vector_tile_map/tile_content_manager/tile_vector_content_manager';
import {GpuMemoryPageRegistryMessages, GpuMemoryRegistryMessageType} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/gpu_memory/gpu_memory_page_registry_messages';
import {GpuMemoryPageRegistry} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/gpu_memory/gpu_memory_page_registry';
import RenderContext from '../../../../../src/vector_render_engine/render/context';
import WebGLRenderingContextStubImpl from '../../../../util/webgl_rendering_context';
import {DEFAULT_OPTIONS as TILE_CONTENT_DEFAULT_OPTIONS} from '../../../../../src/vector_render_engine/content_provider/vector_tile_map/tile_content_manager/tile_vector_content_manager_backend';
import {VectorPrimitiveTypes} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/util/vector_primitive_types';
import {GlyphAtlasRegistry} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/glyph_atlas/glyph_atlas_registry';
import {GlyphAtlasRegistryMessages, GlyphAtlasRegistryUpdateType} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/glyph_atlas/glyph_atlas_registry_messages';
import {DEFAULT_OPTIONS as GLYPH_ATLAS_REGISTRY_DEFAULT_OPTIONS} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/glyph_atlas/glyph_atlas_registry_backend';
import {IconAtlasRegistry} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/icon_atlas/icon_atlas_registry';
import {IconAtlasRegistryMessages, IconAtlasRegistryUpdateType} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/icon_atlas/icon_atlas_registry_messages';
import {DEFAULT_OPTIONS as ICON_ATLAS_REGISTRY_DEFAULT_OPTIONS} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/icon_atlas/icon_atlas_registry_backend';
import {ModelStorageStub} from '../model_storage/model_storage_stub';
import {IdentifiedTileItem} from '../../../../../src/vector_render_engine/content_provider/vector_tile_map/util/identified_tile_item';
import {ReadonlyStorage} from '../../../../../src/vector_render_engine/util/storage';
import {VectorModel} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/model_storage/vector_model';
import {Type} from '../../../../../src/vector_render_engine/render/gl/enums';

const STUB_LOCATION = {
    vertexByteOffset: 0,
    vertexByteLength: 0,
    indexByteOffset: 0,
    indexByteLength: 0,
    indexType: Type.UNSIGNED_SHORT,
    bufferId: 0
};

describe('TileVectorContentManager', () => {
    let stubContext: RenderContext;
    let storageCommunicators: PairOfCommunicators<SyncStorageMessages<IdentifiedTileItem, string>>;
    let contentManagerCommunicators: PairOfCommunicators<TileVectorContentManagerMessages>;
    let memoryRegistryCommunicators: PairOfCommunicators<GpuMemoryPageRegistryMessages>;
    let glyphAtlasRegistryCommunicators: PairOfCommunicators<GlyphAtlasRegistryMessages>;
    let iconAtlasRegistryCommunicators: PairOfCommunicators<IconAtlasRegistryMessages>;
    let storage: VectorTileStorage;
    let modelStorage: ReadonlyStorage<VectorModel, string>;
    let memoryRegistry: GpuMemoryPageRegistry;
    let glyphAtlasRegistry: GlyphAtlasRegistry;
    let iconAtlasRegistry: IconAtlasRegistry;
    let contentManager: TileVectorContentManager;

    beforeEach(() => {
        stubContext = new RenderContext(new WebGLRenderingContextStubImpl());
        storageCommunicators = createPairOfCommunicators();
        contentManagerCommunicators = createPairOfCommunicators();
        memoryRegistryCommunicators = createPairOfCommunicators();
        glyphAtlasRegistryCommunicators = createPairOfCommunicators();
        iconAtlasRegistryCommunicators = createPairOfCommunicators();
        storage = new VectorTileStorage(storageCommunicators.master);
        modelStorage = new ModelStorageStub();
        memoryRegistry = new GpuMemoryPageRegistry(memoryRegistryCommunicators.master, stubContext);
        glyphAtlasRegistry = new GlyphAtlasRegistry(
            glyphAtlasRegistryCommunicators.master,
            GLYPH_ATLAS_REGISTRY_DEFAULT_OPTIONS,
            stubContext
        );
        iconAtlasRegistry = new IconAtlasRegistry(
            iconAtlasRegistryCommunicators.master,
            ICON_ATLAS_REGISTRY_DEFAULT_OPTIONS,
            stubContext
        );
        contentManager = new TileVectorContentManager(
            contentManagerCommunicators.master,
            storage,
            modelStorage,
            memoryRegistry,
            glyphAtlasRegistry,
            iconAtlasRegistry,
            TILE_CONTENT_DEFAULT_OPTIONS
        );
    });

    it('should instantiate primitives',  () => {
        storageCommunicators.worker.sendMessage({
            type: SyncStorageMessageType.FROM_BACKEND_ADD,
            id: '1',
            item: {id: '1', x: 1, y: 1, zoom: 1}
        }, true);

        memoryRegistryCommunicators.worker.sendMessage({
            type: GpuMemoryRegistryMessageType.FROM_BACKEND_ADD_PAGE,
            id: 0,
            attribMapping: {vertexByteSize: 0, attributes: []},
            indexDataByteSize: 100,
            vertexDataByteSize: 100
        }, true);
        iconAtlasRegistryCommunicators.worker.sendMessage({
            type: IconAtlasRegistryUpdateType.FROM_BACKEND_ADD_ATLAS,
            atlasId: 0,
            atlasHeight: 32,
            atlasWidth: 32
        }, true);

        const tile1 = storage.get('1')!;

        contentManagerCommunicators.worker.sendMessage({
            type: TileContentManagerMessageType.FROM_BACKEND_ADD_PRIMITIVES,
            tileId: '1',
            content: {
                primitives: {
                    [VectorPrimitiveTypes.OPAQUE_POLYGON]: [{location: STUB_LOCATION}, {location: STUB_LOCATION}],
                    [VectorPrimitiveTypes.POLYLINE]: [{location: STUB_LOCATION, atlasId: 0, opacity: 1}]
                }
            }
        }, true);

        expect(tile1.content.primitives[VectorPrimitiveTypes.OPAQUE_POLYGON]!.length).to.be.equal(2);
        expect(tile1.content.primitives[VectorPrimitiveTypes.TRANSPARENT_POLYGON]).to.be.undefined;
        expect(tile1.content.primitives[VectorPrimitiveTypes.POLYLINE]!.length).to.be.equal(1);
    });

});
