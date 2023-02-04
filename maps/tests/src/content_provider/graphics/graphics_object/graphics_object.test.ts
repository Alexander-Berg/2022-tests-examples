import * as chai from 'chai';
import {expect} from 'chai';
import * as sinonChai from 'sinon-chai';
import {stub, SinonStub} from 'sinon';
import {GraphicsObject} from '../../../../../src/vector_render_engine/content_provider/graphics/graphics_object/graphics_object';
import {GraphicsGroup} from 'vector_render_engine/content_provider/graphics/graphics_group/graphics_group';
import {GraphicsDetalizationLevel} from 'vector_render_engine/content_provider/graphics/graphics_detaliztion_level/graphics_detalization_level';

chai.use(sinonChai);

describe('GraphicsObject', () => {
    let object: GraphicsObject;
    let root: GraphicsGroup;
    let level: GraphicsDetalizationLevel;
    let onActiveDetalizationChangedStub: SinonStub;

    beforeEach(() => {
        root = new GraphicsGroup('root', {zIndex: 0, opacity: 1}, null);
        object = new GraphicsObject('object', root);
        level = new GraphicsDetalizationLevel('level', object, 0, 0, 0);
        onActiveDetalizationChangedStub = stub();
        object.onActiveDetalizationChanged.addListener(onActiveDetalizationChangedStub);
    });

    it('should add detalization level', () => {
        object.addDetalizationLevel(level);

        expect(object.detalizationLevels).to.include(level);
    });

    it('should remove detalization level', () => {
        object.addDetalizationLevel(level);
        
        object.removeDetalizationLevel(level);

        expect(object.detalizationLevels).not.to.include(level);
    });

    it('should notify when active detalization level is changed', () => {
        object.activeDetalizationLevel = level;

        expect(onActiveDetalizationChangedStub).to.have.been.calledWith(object, undefined, level);
        expect(object.activeDetalizationLevel).to.equal(level);
    });

    it('should not notify when active detalization level is not changed', () => {
        object.activeDetalizationLevel = level;
        onActiveDetalizationChangedStub.reset();

        object.activeDetalizationLevel = level;

        expect(onActiveDetalizationChangedStub).to.not.have.been.called;
    });

    it('should update latest active detalization version', () => {
        const level_0 = level;
        const level_1 = new GraphicsDetalizationLevel('level_1', object, 0, 0, 1);

        object.activeDetalizationLevel = level_1;
        expect(object.latestActiveDetalizationVersion).to.equal(1);

        object.activeDetalizationLevel = level_0;
        expect(object.latestActiveDetalizationVersion).to.equal(1);

        object.activeDetalizationLevel = undefined;
        expect(object.latestActiveDetalizationVersion).to.equal(1);
    });
});
