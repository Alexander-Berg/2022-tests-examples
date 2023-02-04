import * as chai from 'chai';
import {expect} from 'chai';
import {SinonStub, stub} from 'sinon';
import * as sinonChai from 'sinon-chai';
import {GraphicsDetalizationLevel} from 'vector_render_engine/content_provider/graphics/graphics_detaliztion_level/graphics_detalization_level';
import {GraphicsGroup} from 'vector_render_engine/content_provider/graphics/graphics_group/graphics_group';
import {GraphicsObject} from '../../../../../src/vector_render_engine/content_provider/graphics/graphics_object/graphics_object';

chai.use(sinonChai);

describe('GraphicsGroup', () => {
    let group: GraphicsGroup;
    let onUpdateStub: SinonStub;

    beforeEach(() => {
        group = new GraphicsGroup('group', STYLE, null);
        onUpdateStub = stub();
        group.onUpdate.addListener(onUpdateStub);
    });

    it('should add child group', () => {
        const groupChild = new GraphicsGroup('group_child', STYLE, group);

        group.addChild(groupChild);

        expect(group.children).to.include(groupChild);
        expect(group.batchedChildren).to.deep.equal([groupChild]);
        expect(onUpdateStub).to.have.been.calledOnce;
    });

    it('should fire onUpdate when child group is updated', () => {
        const child = new GraphicsGroup('child', STYLE, group);
        group.addChild(child);
        onUpdateStub.reset();

        child.addChild(new GraphicsGroup('nested_child', STYLE, group));

        expect(onUpdateStub).to.have.been.calledOnce;
    });

    it('should remove child group', () => {
        const child = new GraphicsGroup('child', STYLE, group);
        group.addChild(child);
        onUpdateStub.reset();

        group.removeChild(child);

        expect(group.children.size).to.equal(0);
        expect(group.batchedChildren.length).to.equal(0);
        expect(onUpdateStub).to.have.been.calledOnce;
    });

    it('should add child object', () => {
        const objectChild = new GraphicsObject('object_child', group);

        group.addChild(objectChild);

        expect(group.children).to.include(objectChild);
        expect(group.batchedChildren).to.deep.equal([]);
        expect(onUpdateStub).to.not.have.been.called;
    });

    it('should update batches and notify when child object\'s active detalization level is updated or changed', () => {
        const object = new GraphicsObject('object', group);
        const level = new GraphicsDetalizationLevel('level', object, 0, 0, 0);
        object.addDetalizationLevel(level);

        group.addChild(object);
        expect(group.batchedChildren.length).to.equal(0);
        expect(onUpdateStub).to.not.have.been.called;

        object.activeDetalizationLevel = level;
        expect(group.batchedChildren.length).to.equal(0);
        expect(onUpdateStub).to.not.have.been.called;

        level.addContent({primitives: {}});
        expect(group.batchedChildren.length).to.equal(1);
        expect(onUpdateStub).to.have.been.called;
    });

    it('should remove child object', () => {
        const child = new GraphicsObject('child', group);
        group.addChild(child);
        const child_2 = new GraphicsObject('child_2', group);
        const level_2 = new GraphicsDetalizationLevel('level_2', child_2, 0, 0, 0);
        level_2.addContent({primitives: {}});
        child_2.activeDetalizationLevel = level_2;
        group.addChild(child_2);
        onUpdateStub.reset();

        group.removeChild(child);
        expect(group.children).not.to.include(child);
        expect(onUpdateStub).to.not.have.been.called;
        onUpdateStub.reset();

        group.removeChild(child_2);
        expect(group.children).not.to.include(child);
        expect(group.batchedChildren.length).to.equal(0);
    });
});

const STYLE = {zIndex: 0, opacity: 1};
