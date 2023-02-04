import {expect} from 'chai';
import {spy} from 'sinon';
import * as sinonChai from 'sinon-chai';
import * as chai from 'chai';
import {createPairOfCommunicators} from '../util/backend_communicator';
import {
    GpuMemoryPageRegistryMessages,
    GpuMemoryRegistryMessageType
} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/gpu_memory/gpu_memory_page_registry_messages';
import WebGLRenderingContextStubImpl from '../../../../util/webgl_rendering_context';
import {GpuMemoryPageRegistry} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/gpu_memory/gpu_memory_page_registry';
import RenderContext from '../../../../../src/vector_render_engine/render/context';
import {allocateAttributes} from '../../../../../src/vector_render_engine/render/attrib_mapping';

chai.use(sinonChai);

describe('GpuMemoryPageRegistry', () => {
    it('should add/remove pages by messages from backend', () => {
        const PAGE_ID = 1;
        const stubCommunicators = createPairOfCommunicators<GpuMemoryPageRegistryMessages>();
        const stubContext = new RenderContext(new WebGLRenderingContextStubImpl());
        const registry = new GpuMemoryPageRegistry(stubCommunicators.master, stubContext);

        stubCommunicators.worker.sendMessage({
            type: GpuMemoryRegistryMessageType.FROM_BACKEND_ADD_PAGE,
            attribMapping: allocateAttributes([]),
            id: PAGE_ID,
            vertexDataByteSize: 100,
            indexDataByteSize: 200
        }, true);

        expect(registry.getPage(PAGE_ID)).not.to.be.undefined;

        stubCommunicators.worker.sendMessage({
            type: GpuMemoryRegistryMessageType.FROM_BACKEND_REMOVE_PAGE,
            id: PAGE_ID
        }, true);

        expect(registry.getPage(PAGE_ID)).to.be.undefined;
    });

    it('should update page', () => {
        const PAGE_ID = 1;
        const VERTEX_UPDATE_DATA = new Uint8Array([11, 22, 33, 44, 55, 66]);
        const VERTEX_UPDATE_OFFSET = 10;
        const INDEX_UPDATE_DATA = new Uint8Array([1, 2, 3, 4, 5, 6]);
        const INDEX_UPDATE_OFFSET = 20;
        const stubCommunicators = createPairOfCommunicators<GpuMemoryPageRegistryMessages>();
        const stubContext = new RenderContext(new WebGLRenderingContextStubImpl());
        const registry = new GpuMemoryPageRegistry(stubCommunicators.master, stubContext);

        stubCommunicators.worker.sendMessage({
            type: GpuMemoryRegistryMessageType.FROM_BACKEND_ADD_PAGE,
            attribMapping: allocateAttributes([]),
            id: PAGE_ID,
            vertexDataByteSize: 100,
            indexDataByteSize: 200
        }, true);

        const page = registry.getPage(PAGE_ID)!;
        const updateVertexContent = spy(page, 'updateVertexContent');
        const updateIndexContent = spy(page, 'updateIndexContent');

        stubCommunicators.worker.sendMessage({
            type: GpuMemoryRegistryMessageType.FROM_BACKEND_UPDATE_PAGE,
            id: PAGE_ID,
            vertexData: VERTEX_UPDATE_DATA.buffer,
            vertexByteOffset: VERTEX_UPDATE_OFFSET,
            indexData: INDEX_UPDATE_DATA.buffer,
            indexByteOffset: INDEX_UPDATE_OFFSET
        }, true);

        expect(updateVertexContent).to.have.been.calledWith(VERTEX_UPDATE_DATA.buffer, VERTEX_UPDATE_OFFSET);
        expect(updateIndexContent).to.have.been.calledWith(INDEX_UPDATE_DATA.buffer, INDEX_UPDATE_OFFSET);
    })

});
