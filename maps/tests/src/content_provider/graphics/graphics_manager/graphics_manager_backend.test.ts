import * as chai from 'chai';
import {expect} from 'chai';
import {stub} from 'sinon';
import * as sinonChai from 'sinon-chai';
import {GraphicsManagerBackend} from '../../../../../src/vector_render_engine/content_provider/graphics/graphics_manager/graphics_manager_backend';
import {GraphicsManagerMessages, GraphicsManagerMessageType} from '../../../../../src/vector_render_engine/content_provider/graphics/graphics_manager/graphics_manager_messages';
import {createPairOfCommunicators, PairOfCommunicators} from '../../vector_tile_map/util/backend_communicator';
import {GraphicsDetalizationLevelStorageBackendStub} from '../graphics_detalization_level/graphics_detalization_level_storage_backend_stub';
import {GraphicsGroupStorageBackendStub} from '../graphics_group/graphics_group_storage_backend_stub';
import {GraphicsObjectStorageBackendStub} from '../graphics_object/graphics_object_storage_backend_stub';
import {GraphicsDetalizationLevelState} from 'vector_render_engine/content_provider/graphics/graphics_detaliztion_level/graphics_detalization_level_state';
import {GraphicsViewportBackendStub} from '../graphics_viewport/graphics_viewport_backend_stub';

chai.use(sinonChai);

describe('GraphicsManagerBackend', () => {
    let manager: GraphicsManagerBackend;
    let communicators: PairOfCommunicators<GraphicsManagerMessages>;
    let viewport: GraphicsViewportBackendStub;
    let objectStorage: GraphicsObjectStorageBackendStub;
    let groupStorage: GraphicsGroupStorageBackendStub;
    let detalizationStorage: GraphicsDetalizationLevelStorageBackendStub;

    beforeEach(() => {
        communicators = createPairOfCommunicators();
        detalizationStorage = new GraphicsDetalizationLevelStorageBackendStub();
        objectStorage = new GraphicsObjectStorageBackendStub();
        groupStorage = new GraphicsGroupStorageBackendStub();
        viewport = new GraphicsViewportBackendStub();
        manager = new GraphicsManagerBackend(
            communicators.worker,
            viewport,
            groupStorage,
            objectStorage,
            detalizationStorage
        );

        communicators.master.sendMessage({
            type: GraphicsManagerMessageType.TO_BACKEND_SETUP,
            options: {
                minSimplificationZoom: 0,
                maxSimplificationZoom: 10,
                simplificationPreloadedZooms: 2
            }
        }, true);
        communicators.master.sendMessage({
            type: GraphicsManagerMessageType.TO_BACKEND_ADD_GROUP,
            id: 'root',
            parentId: null,
            style: GROUP_STYLE
        }, true);
        viewport.setZoom(9);
    });

    it('should add group', () => {
        communicators.master.sendMessage({
            type: GraphicsManagerMessageType.TO_BACKEND_ADD_GROUP,
            id: 'group',
            parentId: 'root',
            style: GROUP_STYLE
        }, true);

        const root = groupStorage.get('root')!;
        const group = groupStorage.get('group')!;
        expect(groupStorage.size).to.equal(2);
        expect(group).not.to.equal(undefined);
        expect(root).not.to.equal(undefined);
        expect(group.parent).to.equal(root);
        expect(root.children).to.include(group);
    });

    it('should add object', () => {
        communicators.master.sendMessage({
            type: GraphicsManagerMessageType.TO_BACKEND_ADD_OBJECT,
            id: 'object',
            parentId: 'root',
            feature: {} as any,
            style: OBJECT_STYLE
        }, true);

        const root = groupStorage.get('root')!;
        const object = objectStorage.get('object')!;
        expect(objectStorage.size).to.equal(1);
        expect(object).not.to.equal(undefined);
        expect(object.parent).to.equal(root);
        expect(root.children).to.include(object);

        const levels = [...object.detalizationLevels];
        expect(levels.length).to.equal(4);
        expect(levels.every((level) => detalizationStorage.has(level.id)));
        expect(levels.map((level) => level.zoom)).to.deep.equal([7, 8, 9, 10]);
        expect(levels.map((level) => level.state)).to.deep.equal([
            GraphicsDetalizationLevelState.PRELOADED,
            GraphicsDetalizationLevelState.PRELOADED,
            GraphicsDetalizationLevelState.VISIBLE,
            GraphicsDetalizationLevelState.PRELOADED
        ]);
    });

    it('should remove object', () => {
        communicators.master.sendMessage({
            type: GraphicsManagerMessageType.TO_BACKEND_ADD_OBJECT,
            id: 'object',
            parentId: 'root',
            feature: {} as any,
            style: OBJECT_STYLE
         }, true);

        communicators.master.sendMessage({
            type: GraphicsManagerMessageType.TO_BACKEND_REMOVE_OBJECT,
            id: 'object'
        }, true);

        const object = objectStorage.has('object');
        expect(object).to.equal(false);
        expect(groupStorage.get('root')!.children).not.to.include(object);
        expect(detalizationStorage.size).to.equal(0);
    });

    it('should remove group and its children', () => {
        communicators.master.sendMessage({
            type: GraphicsManagerMessageType.TO_BACKEND_ADD_GROUP,
            id: 'group',
            parentId: 'root',
            style: GROUP_STYLE
        }, true);
        communicators.master.sendMessage({
            type: GraphicsManagerMessageType.TO_BACKEND_ADD_GROUP,
            id: 'child_group',
            parentId: 'group',
            style: GROUP_STYLE
        }, true);
        communicators.master.sendMessage({
            type: GraphicsManagerMessageType.TO_BACKEND_ADD_OBJECT,
            id: 'object',
            parentId: 'group',
            feature: {} as any,
            style: OBJECT_STYLE
        }, true);

        communicators.master.sendMessage({
            type: GraphicsManagerMessageType.TO_BACKEND_REMOVE_GROUP_AND_CHILDREN,
            id: 'group'
        }, true);

        expect(groupStorage.size).to.equal(1);
        expect(groupStorage.has('group')).to.equal(false);
        expect(groupStorage.has('child_group')).to.equal(false);
        expect(objectStorage.has('object')).to.equal(false);
        expect(groupStorage.get('root')!.children.size).to.equal(0);
        expect(detalizationStorage.size).to.equal(0);
    });

    it('should update object', () => {
        communicators.master.sendMessage({
            type: GraphicsManagerMessageType.TO_BACKEND_ADD_OBJECT,
            id: 'object',
            parentId: 'root',
            feature: {} as any,
            style: OBJECT_STYLE
        }, true);
        const object = objectStorage.get('object')!;

        const newFeature = {type: 'Feature'} as any;
        const newStyle = {zIndex: 10, simplificationRate: 0};
        communicators.master.sendMessage({
            type: GraphicsManagerMessageType.TO_BACKEND_UPDATE_OBJECT,
            id: 'object',
            feature: newFeature,
            style: newStyle
        }, true);

        expect(object.feature).to.deep.equal(newFeature);
        expect(object.style).to.deep.equal(newStyle);

        const levels = [...object.detalizationLevels];
        expect(levels.length).to.equal(5); // 4 prev + 1 actual, new simplification rate is 0 => no need for preload
        expect(levels.map((level) => level.zoom)).to.deep.equal([7, 8, 9, 10, -1]);
        expect(levels.map((level) => level.version)).to.deep.equal([0, 0, 0, 0, 1]);
        expect(levels.map((level) => level.state)).to.deep.equal([
            GraphicsDetalizationLevelState.OBSOLETE,
            GraphicsDetalizationLevelState.OBSOLETE,
            GraphicsDetalizationLevelState.OBSOLETE,
            GraphicsDetalizationLevelState.OBSOLETE,
            GraphicsDetalizationLevelState.ALWAYS_VISIBLE
        ]);
    });

    it('should update levels of detalization when zoom is updated', () => {
        communicators.master.sendMessage({
            type: GraphicsManagerMessageType.TO_BACKEND_ADD_OBJECT,
            id: 'object',
            parentId: 'root',
            feature: {} as any,
            style: OBJECT_STYLE
        }, true);

        viewport.setZoom(5);

        const object = objectStorage.get('object')!;
        const levels = [...object.detalizationLevels];
        expect(levels.length).to.equal(8);
        expect(levels.map((level) => level.state)).to.deep.equal([
            GraphicsDetalizationLevelState.PRELOADED,
            GraphicsDetalizationLevelState.CACHED,
            GraphicsDetalizationLevelState.CACHED,
            GraphicsDetalizationLevelState.CACHED,
            GraphicsDetalizationLevelState.PRELOADED,
            GraphicsDetalizationLevelState.PRELOADED,
            GraphicsDetalizationLevelState.VISIBLE,
            GraphicsDetalizationLevelState.PRELOADED
        ]);
    });

    it('should not mark the object\'s only level as PRELOADED or CACHED', () => {
        communicators.master.sendMessage({
            type: GraphicsManagerMessageType.TO_BACKEND_ADD_OBJECT,
            id: 'object',
            parentId: 'root',
            feature: {} as any,
            style: {zIndex: 0, simplificationRate: 0}
        }, true);

        const object = objectStorage.get('object')!;
        const level = [...object.detalizationLevels][0];
        expect(level.state).to.equal(GraphicsDetalizationLevelState.ALWAYS_VISIBLE);

        viewport.setZoom(8);
        expect(level.state).to.equal(GraphicsDetalizationLevelState.ALWAYS_VISIBLE);

        viewport.setZoom(0);
        expect(level.state).to.equal(GraphicsDetalizationLevelState.ALWAYS_VISIBLE);

        viewport.setZoom(30);
        expect(level.state).to.equal(GraphicsDetalizationLevelState.ALWAYS_VISIBLE);
    });
});

const GROUP_STYLE = {opacity: 1, zIndex: 0};
const OBJECT_STYLE = {zIndex: 0, simplificationRate: 1};
