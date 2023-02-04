import {expect} from 'chai';
import {stub, SinonStub} from 'sinon';
import * as sinonChai from 'sinon-chai';
import * as chai from 'chai';
import {GraphicsDetalizationLevelBackend} from 'vector_render_engine/content_provider/graphics/graphics_detaliztion_level/graphics_detalization_level_backend';
import {GraphicsObjectBackend} from 'vector_render_engine/content_provider/graphics/graphics_object/graphics_object_backend';
import {GraphicsGroupBackend} from 'vector_render_engine/content_provider/graphics/graphics_group/graphics_group_backend';
import {GraphicsDetalizationLevelState} from 'vector_render_engine/content_provider/graphics/graphics_detaliztion_level/graphics_detalization_level_state';

chai.use(sinonChai);

describe('GraphicsDetalizationLevelBackend', () => {
    let level: GraphicsDetalizationLevelBackend;
    let object: GraphicsObjectBackend;
    let group: GraphicsGroupBackend;
    let onContentAddedStub: SinonStub;
    let onStateChangedStub: SinonStub;

    beforeEach(() => {
        group = new GraphicsGroupBackend('group', {zIndex: 0, opacity: 1}, null);
        object = new GraphicsObjectBackend('object', {} as any, {zIndex: 0, simplificationRate: 2}, group);
        level = new GraphicsDetalizationLevelBackend('lod', object, 0, GraphicsDetalizationLevelState.VISIBLE);
        onContentAddedStub = stub();
        onStateChangedStub = stub();
        level.onContentAdded.addListener(onContentAddedStub);
        level.onStateChanged.addListener(onStateChangedStub);
    });

    it('should inherit parent properties', () => {
        object.update({} as any, {simplificationRate: 0, zIndex: 1});

        level = new GraphicsDetalizationLevelBackend('lod', object, 0, GraphicsDetalizationLevelState.VISIBLE);

        expect(level.version).to.equal(object.version);
        expect(level.zIndex).to.equal(object.style.zIndex);
    });
    
    it('should notify when content is added', () => {
        level.addContent({});

        expect(onContentAddedStub).to.have.been.calledOnce;
    });

    it('should notify when state is changed', () => {
        level.state = GraphicsDetalizationLevelState.PRELOADED;

        expect(onStateChangedStub).to.have.been.calledWith(
            level, 
            GraphicsDetalizationLevelState.VISIBLE, 
            GraphicsDetalizationLevelState.PRELOADED
        );
    });

    it('should not notify when state is not changed', () => {
        level.state = GraphicsDetalizationLevelState.VISIBLE;

        expect(onStateChangedStub).to.not.have.been.called;
    });
});