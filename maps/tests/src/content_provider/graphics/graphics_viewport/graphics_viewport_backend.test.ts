import * as chai from 'chai';
import {expect} from 'chai';
import {SinonStub, stub} from 'sinon';
import * as sinonChai from 'sinon-chai';
import {GraphicsViewportBackend} from 'vector_render_engine/content_provider/graphics/graphics_viewport/graphics_viewport_backend';
import {GraphicsViewportMessages, GraphicsViewportMessageType} from 'vector_render_engine/content_provider/graphics/graphics_viewport/graphics_viewport_messages';
import {createPairOfCommunicators, PairOfCommunicators} from '../../vector_tile_map/util/backend_communicator';

chai.use(sinonChai);

describe('GraphicsViewportBackend', () => {
    let viewport: GraphicsViewportBackend;
    let communicators: PairOfCommunicators<GraphicsViewportMessages>;
    let onUpdateStub: SinonStub;

    beforeEach(() => {
        communicators = createPairOfCommunicators();
        viewport = new GraphicsViewportBackend(communicators.worker);
        onUpdateStub = stub();
        viewport.onUpdate.addListener(onUpdateStub);
    });
    
    it('should notify on zoom update', () => {
        communicators.master.sendMessage({
            type: GraphicsViewportMessageType.TO_BACKEND_SET_ZOOM,
            zoom: 10
        }, true);

        expect(onUpdateStub).to.have.been.calledOnce;
    });
});
