import {expect} from 'chai';
import {stub} from 'sinon';
import * as chai from 'chai';
import * as sinonChai from 'sinon-chai';
import {GraphicsDetalizationGarbageCollector} from '../../../../../src/vector_render_engine/content_provider/graphics/garbage_collector/graphics_detalization_garbage_collector';
import {GraphicsDetalizationLevelBackend} from '../../../../../src/vector_render_engine/content_provider/graphics/graphics_detaliztion_level/graphics_detalization_level_backend';
import {GraphicsDetalizationLevelState} from '../../../../../src/vector_render_engine/content_provider/graphics/graphics_detaliztion_level/graphics_detalization_level_state';
import {GraphicsGroupBackend} from '../../../../../src/vector_render_engine/content_provider/graphics/graphics_group/graphics_group_backend';
import {GraphicsObjectBackend} from '../../../../../src/vector_render_engine/content_provider/graphics/graphics_object/graphics_object_backend';
import {GraphicsDetalizationLevelStorageBackendStub} from '../graphics_detalization_level/graphics_detalization_level_storage_backend_stub';

chai.use(sinonChai);

describe('GraphicsDetalizationGarbageCollector', () => {
    let gc: GraphicsDetalizationGarbageCollector;
    let detalizationStorage: GraphicsDetalizationLevelStorageBackendStub;
    let object: GraphicsObjectBackend;
    let group: GraphicsGroupBackend;

    beforeEach(() => {
        detalizationStorage = new GraphicsDetalizationLevelStorageBackendStub();
        gc = new GraphicsDetalizationGarbageCollector(detalizationStorage);
        group = new GraphicsGroupBackend('group', {zIndex: 0, opacity: 1}, null);
        object = new GraphicsObjectBackend('object', {} as any, {simplificationRate: 0, zIndex: 0}, group);
    });

    it('should destroy obsolete levels only when actual level has content', () => {
        const level = new GraphicsDetalizationLevelBackend('l', object, 0, GraphicsDetalizationLevelState.VISIBLE);
        detalizationStorage.add(level.id, level);
        const levelDestructor = stub(level, 'destructor');

        level.state = GraphicsDetalizationLevelState.OBSOLETE;
        expect(levelDestructor).to.not.have.been.called;

        const level2 = new GraphicsDetalizationLevelBackend('l2', object, 0, GraphicsDetalizationLevelState.PRELOADED);
        detalizationStorage.add(level2.id, level2);
        expect(levelDestructor).to.not.have.been.called;

        level2.addContent({});
        expect(levelDestructor).to.have.been.calledOnce;
        expect(detalizationStorage.has(level.id)).to.equal(false);
        expect(detalizationStorage.size).to.equal(1);
    });
});
