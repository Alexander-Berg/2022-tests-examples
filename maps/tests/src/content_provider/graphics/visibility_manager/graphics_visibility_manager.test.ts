import * as chai from 'chai';
import {expect} from 'chai';
import {SinonStub, stub} from 'sinon';
import * as sinonChai from 'sinon-chai';
import {GraphicsGroupStyle} from 'vector_render_engine/content_provider/graphics/graphics_group/graphics_group_style';
import {SyncStorageMessageType} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/storage/sync_storage_messages';
import {GraphicsDetalizationLevel} from '../../../../../src/vector_render_engine/content_provider/graphics/graphics_detaliztion_level/graphics_detalization_level';
import {GraphicsGroup} from '../../../../../src/vector_render_engine/content_provider/graphics/graphics_group/graphics_group';
import {GraphicsObject} from '../../../../../src/vector_render_engine/content_provider/graphics/graphics_object/graphics_object';
import {GraphicsScene} from '../../../../../src/vector_render_engine/content_provider/graphics/visibility_manager/graphics_scene';
import {GraphicsVisibilityManager} from '../../../../../src/vector_render_engine/content_provider/graphics/visibility_manager/graphics_visibility_manager';
import {GraphicsDetalizationLevelStorageStub} from '../graphics_detalization_level/graphics_detalization_level_storage_stub';
import {GraphicsGroupStorageStub} from '../graphics_group/graphics_group_storage_stub';
import {GraphicsObjectStorageStub} from '../graphics_object/graphics_object_storage_stub';
import {GraphicsViewportStub} from '../graphics_viewport/graphics_viewport_stub';

chai.use(sinonChai);

const STYLE: GraphicsGroupStyle = {
    zIndex: 0,
    opacity: 1
};
const CONTENT = {primitives: {0: []}};

describe('GraphicsVisibilityManager', () => {
    let manager: GraphicsVisibilityManager;
    let groupStorage: GraphicsGroupStorageStub;
    let objectStorage: GraphicsObjectStorageStub;
    let detalizationStorage: GraphicsDetalizationLevelStorageStub;
    let scene: GraphicsScene;
    let viewport: GraphicsViewportStub;
    let onChangeStub: SinonStub;

    const addGroupToStorage = (id: string, parentId: string | null): GraphicsGroup => {
        groupStorage.communicators.worker.sendMessage({
            type: SyncStorageMessageType.FROM_BACKEND_ADD,
            id,
            item: {id, style: STYLE, parentId}
        }, true);
        return groupStorage.get(id)!;
    };
    const addObjectToStorage = (id: string, parentId: string = 'root'): GraphicsObject => {
        objectStorage.communicators.worker.sendMessage({
            type: SyncStorageMessageType.FROM_BACKEND_ADD,
            id,
            item: {id, parentId}
        }, true);
        return objectStorage.get(id)!;
    };
    const addLevelToStorage = (
        id: string,
        objectId: string,
        zoom: number = 0,
        version: number = 0
    ): GraphicsDetalizationLevel => {
        detalizationStorage.communicators.worker.sendMessage({
            type: SyncStorageMessageType.FROM_BACKEND_ADD,
            id,
            item: {id, zIndex: 0, zoom, objectId, version}
        }, true);
        return detalizationStorage.get(id)!;
    }

    beforeEach(() => {
        detalizationStorage = new GraphicsDetalizationLevelStorageStub();
        objectStorage = detalizationStorage.objectStorage;
        groupStorage = objectStorage.groupStorage;
        scene = new GraphicsScene();
        viewport = new GraphicsViewportStub();
        manager = new GraphicsVisibilityManager(scene, viewport, groupStorage, objectStorage, detalizationStorage);
        onChangeStub = stub();

        manager.onStateChanged.addListener(onChangeStub);
    });

    it('should set scene root', () => {
        addGroupToStorage('root', null);

        expect(scene.root).not.to.equal(undefined);
        expect(scene.root!.id).to.equal('root');
    });

    it('should update scene root', () => {
        addGroupToStorage('root', null);

        addGroupToStorage('root2', null);

        expect(scene.root!.id).to.equal('root2')
    });

    it('should remove scene root', () => {
        addGroupToStorage('root', null);

        groupStorage.communicators.worker.sendMessage({
            type: SyncStorageMessageType.FROM_BACKEND_REMOVE,
            id: 'root'
        }, true);

        expect(scene.root).to.equal(undefined);
    });

    it('should pause', () => {
        addGroupToStorage('root', null);

        manager.pause();

        expect(scene.root).to.equal(undefined);
    });

    it('should resume', () => {
        addGroupToStorage('root', null);
        const root = scene.root;
        manager.pause();
        manager.resume();

        expect(scene.root).to.equal(root);
    });

    it('should fire onStateChanged when total or visible number of objects changed', () => {
        addGroupToStorage('root', null);
        expect(onChangeStub).to.not.have.been.called;

        addObjectToStorage('object', 'root');
        expect(onChangeStub).to.have.been.calledOnce;
        expect(onChangeStub.lastCall.args[0]).to.deep.equal({objectsVisible: 0, objectsTotal: 1});
        expect(manager.state).to.deep.equal({objectsVisible: 0, objectsTotal: 1});
        onChangeStub.reset();

        addLevelToStorage('level', 'object');
        detalizationStorage.get('level')!.addContent(CONTENT);
        expect(onChangeStub).to.have.been.calledOnce;
        expect(onChangeStub.lastCall.args[0]).to.deep.equal({objectsVisible: 1, objectsTotal: 1});
        expect(manager.state).to.deep.equal({objectsVisible: 1, objectsTotal: 1});
        onChangeStub.reset();

        objectStorage.communicators.worker.sendMessage({
            type: SyncStorageMessageType.FROM_BACKEND_REMOVE,
            id: 'object'
        }, true);
        expect(onChangeStub).to.have.been.calledOnce;
        expect(onChangeStub.lastCall.args[0]).to.deep.equal({objectsVisible: 0, objectsTotal: 0});
    });

    describe('level visualization logic', () => {
        let root: GraphicsGroup;
        let object: GraphicsObject;
        let level_0: GraphicsDetalizationLevel;

        beforeEach(() => {
            root = addGroupToStorage('root', null);
            object = addObjectToStorage('object', 'root');
            level_0 = addLevelToStorage('level_0', 'object', 0, 0);
            viewport.setZoom(0);
        });

        it('should visualize the only level with content', () => {
            level_0.addContent(CONTENT);

            expect(object.activeDetalizationLevel).to.equal(level_0);
        });

        it('should recalculate levels on viewport update', () => {
            const level_5 = addLevelToStorage('level_5', 'object', 5);
            level_0.addContent(CONTENT);
            level_5.addContent(CONTENT);

            viewport.setZoom(10);
            expect(object.activeDetalizationLevel?.id).to.equal('level_5');
        })

        it('should visualize a level with the nearest zoom', () => {
            const level_6 = addLevelToStorage('level_6', 'object', 6);

            level_0.addContent(CONTENT);
            viewport.setZoom(5);
            expect(object.activeDetalizationLevel?.id).to.equal('level_0');

            level_6.addContent(CONTENT);
            expect(object.activeDetalizationLevel?.id).to.equal('level_6');

            viewport.setZoom(2);
            expect(object.activeDetalizationLevel?.id).to.equal('level_0');
        });

        it('should visualize another level when the active level is removed', () => {
            const level_10 = addLevelToStorage('level_10', 'object', 10);
            level_0.addContent(CONTENT);
            level_10.addContent(CONTENT);

            expect(object.activeDetalizationLevel?.id).to.equal('level_0');

            detalizationStorage.communicators.worker.sendMessage({
                type: SyncStorageMessageType.FROM_BACKEND_REMOVE,
                id: 'level_0'
            }, true)
            expect(object.activeDetalizationLevel?.id).to.equal('level_10');
        });

        it('should visualize a level with the new version', () => {
            const level_5 = addLevelToStorage('level_5', 'object', 5, 1);
            level_0.addContent(CONTENT);

            level_5.addContent(CONTENT);
            expect(object.activeDetalizationLevel?.id).to.equal('level_5');
        });
    });
});
