import * as chai from 'chai';
import {expect} from 'chai';
import {stub, SinonStub} from 'sinon';
import * as sinonChai from 'sinon-chai';
import {GraphicsScene} from '../../../../../src/vector_render_engine/content_provider/graphics/visibility_manager/graphics_scene';
import {GraphicsGroup} from 'vector_render_engine/content_provider/graphics/graphics_group/graphics_group';

chai.use(sinonChai);

describe('GraphicsScene', () => {
    let scene: GraphicsScene;
    let onUpdateStub: SinonStub;

    beforeEach(() => {
        scene = new GraphicsScene();
        onUpdateStub = stub();
        scene.onUpdate.addListener(onUpdateStub);
    });

    it('should set root and fire onUpdate when root changed', () => {
        expect(scene.root).to.be.undefined;

        const root1 = new GraphicsGroup('root1', {zIndex: 0, opacity: 1}, null);
        const root2 = new GraphicsGroup('root2', {zIndex: 0, opacity: 1}, null);

        scene.root = root1;
        expect(scene.root).to.equal(root1);
        expect(onUpdateStub).to.have.been.calledOnce;
        onUpdateStub.reset();

        scene.root = root2;
        expect(scene.root).to.equal(root2);
        expect(onUpdateStub).to.have.been.calledOnce;
    });

    it('should fire onUpdate when root is updated', () => {
        scene.root = new GraphicsGroup('root', {zIndex: 0, opacity: 1}, null);
        onUpdateStub.reset();

        scene.root.addChild(new GraphicsGroup('child', {zIndex: 0, opacity: 1}, scene.root));
        expect(onUpdateStub).to.have.been.calledOnce;
    });
});
