import * as chai from 'chai';
import {expect} from 'chai';
import * as sinonChai from 'sinon-chai';
import {createPairOfCommunicators} from '../util/backend_communicator';
import {
    GpuMemoryPageRegistryMessages,
    GpuMemoryRegistryMessageType
} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/gpu_memory/gpu_memory_page_registry_messages';
import {
    GpuMemoryPageRegistryBackend
} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/gpu_memory/gpu_memory_page_registry_backend';
import {allocateAttributes, Attribute} from '../../../../../src/vector_render_engine/render/attrib_mapping';
import {promisifyEventEmitter} from '../../../util/event_emitter';
import {Type} from '../../../../../src/vector_render_engine/render/gl/enums';

chai.use(sinonChai);

describe('GpuMemoryPageRegistryBackend', () => {
    it('should create pages', () => {
        const ATTRIB_MAPPING = allocateAttributes([]);
        const stubCommunicators = createPairOfCommunicators<GpuMemoryPageRegistryMessages>();
        const registry = new GpuMemoryPageRegistryBackend(stubCommunicators.worker);

        const page1 = registry.getFreeOrCreatePage(ATTRIB_MAPPING);

        expect(page1.attribMapping).to.be.equal(ATTRIB_MAPPING);
        expect(registry.getPage(page1.id)).not.to.be.undefined;
    });

    it('should reuse released pages', () => {
        const ATTRIB_MAPPING = allocateAttributes([]);
        const stubCommunicators = createPairOfCommunicators<GpuMemoryPageRegistryMessages>();
        const registry = new GpuMemoryPageRegistryBackend(stubCommunicators.worker);

        const page1 = registry.getFreeOrCreatePage(ATTRIB_MAPPING);
        page1.retain();
        page1.retain();

        const page2 = registry.getFreeOrCreatePage(ATTRIB_MAPPING);

        expect(page2).not.to.be.equal(page1);

        page1.release();
        page1.release();

        const page3 = registry.getFreeOrCreatePage(ATTRIB_MAPPING);

        expect(page3).to.be.equal(page1);
    });

    it('should submit updates', async () => {
        const ATTRIB_MAPPING = allocateAttributes([[
            Attribute.POSITION,
            {
                size: 2,
                type: Type.UNSIGNED_SHORT,
                normalized: false
            }
        ], [
            Attribute.ID,
            {
                size: 4,
                type: Type.UNSIGNED_BYTE,
                normalized: false
            }
        ]]);
        const stubCommunicators = createPairOfCommunicators<GpuMemoryPageRegistryMessages>();
        const registry = new GpuMemoryPageRegistryBackend(stubCommunicators.worker);

        const addPage = promisifyEventEmitter(stubCommunicators.master.onMessage).then(([message]) => {
            if (message.type === GpuMemoryRegistryMessageType.FROM_BACKEND_ADD_PAGE) {
                expect(message.attribMapping).to.be.deep.equal(ATTRIB_MAPPING);
                expect(message.vertexDataByteSize).to.be.greaterThan(0);
                expect(message.indexDataByteSize).to.be.greaterThan(0);
            } else {
                throw new Error('Not FROM_BACKEND_ADD_PAGE event');
            }
        });

        const page1 = registry.getFreeOrCreatePage(ATTRIB_MAPPING);

        await addPage;

        const updatePage = promisifyEventEmitter(stubCommunicators.master.onMessage).then(([message]) => {
            if (message.type === GpuMemoryRegistryMessageType.FROM_BACKEND_UPDATE_PAGE) {
                expect(new Uint32Array(message.vertexData)).to.be.deep.equal(new Uint32Array([1, 2]));
                expect(new Uint16Array(message.indexData)).to.be.deep.equal(new Uint16Array([0, 1, 2]));
            } else {
                throw new Error('Not FROM_BACKEND_UPDATE_PAGE event');
            }
        });

        page1.data.vertexBuffer.pushUint32(1);
        page1.data.vertexBuffer.pushUint32(2);
        page1.data.indexBuffer.push(0);
        page1.data.indexBuffer.push(1);
        page1.data.indexBuffer.push(2);
        page1.data.endMesh();

        registry.flushBackendUpdates();

        await updatePage;
    });

});
