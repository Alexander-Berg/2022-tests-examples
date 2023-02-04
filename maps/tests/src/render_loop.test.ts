import {expect} from 'chai';
import * as sinonChai from 'sinon-chai';
import * as chai from 'chai'; chai.use(sinonChai);
import EngineRenderLoop, {RenderLoopImpl} from '../../src/vector_render_engine/render_loop';
import spyNamed from '../util/sinon_spy_named';

class TestEngineRenderLoop extends RenderLoopImpl {
    _scheduleAnimationFrame(): number {
        return setTimeout(this._renderFrame.bind(this));
    }
}

describe('EngineRenderLoop', () => {
    let rendererStub: any;
    let renderLoop: EngineRenderLoop;

    beforeEach(() => {
        rendererStub = {
            render: spyNamed('render')
        };
        renderLoop = new TestEngineRenderLoop();
    });

    afterEach(() => {
        renderLoop.destroy();
    });

    it.skip('should render in the next frame if the loop is not active', (done) => {
        renderLoop.update();
        expect(rendererStub.render).not.to.have.been.called;

        setTimeout(() => {
            expect(rendererStub.render).to.have.been.called;

            done();
        });
    });
});
