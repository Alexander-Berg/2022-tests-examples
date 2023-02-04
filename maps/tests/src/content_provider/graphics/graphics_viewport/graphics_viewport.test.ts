import {expect} from 'chai';
import {SinonStub, stub} from 'sinon';
import * as chai from 'chai';
import * as sinonChai from 'sinon-chai';
import Camera from 'vector_render_engine/camera';
import {GraphicsViewport} from 'vector_render_engine/content_provider/graphics/graphics_viewport/graphics_viewport';
import {GraphicsViewportMessages} from 'vector_render_engine/content_provider/graphics/graphics_viewport/graphics_viewport_messages';
import {createPairOfCommunicators, PairOfCommunicators} from '../../vector_tile_map/util/backend_communicator';

chai.use(sinonChai);

describe('GraphicsViewport', () => {
    let viewport: GraphicsViewport;
    let communicators: PairOfCommunicators<GraphicsViewportMessages>;
    let camera: Camera;
    let onMessageStub: SinonStub;

    beforeEach(() => {
        camera = new Camera();
        communicators = createPairOfCommunicators();
        viewport = new GraphicsViewport(communicators.master, camera);
        onMessageStub = stub();
        communicators.worker.onMessage.addListener(onMessageStub);
    });

    it('should init zoom', () => {
        camera.zoom = 5;
        camera.flushUpdates();

        viewport = new GraphicsViewport(communicators.master, camera);

        expect(onMessageStub.lastCall.args[0].zoom).to.equal(5);
    });

    it('should update zoom', () => {
        camera.zoom = 10;
        camera.flushUpdates();

        expect(onMessageStub.lastCall.args[0].zoom).to.equal(10);
    });

    it('should round zoom', () => {
        camera.zoom = 10.3;
        camera.flushUpdates();
        expect(onMessageStub.lastCall.args[0].zoom).to.equal(10);
        onMessageStub.reset();

        camera.zoom = 9.7;
        camera.flushUpdates();
        expect(onMessageStub).to.not.have.been.called;
    });
});
