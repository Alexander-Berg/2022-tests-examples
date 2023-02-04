import * as chai from 'chai'
import {expect} from 'chai';
import {stub, SinonStub} from 'sinon';
import * as sinonChai from 'sinon-chai';
import {GraphicsManager} from '../../../../../src/vector_render_engine/content_provider/graphics/graphics_manager/graphics_manager';
import {PairOfCommunicators, createPairOfCommunicators} from '../../vector_tile_map/util/backend_communicator';
import {GraphicsManagerMessages, GraphicsManagerMessageType, AddGraphicsObject, UpdateGraphicsObject, AddGraphicsGroup} from '../../../../../src/vector_render_engine/content_provider/graphics/graphics_manager/graphics_manager_messages';
import {GraphicsGroupStyle} from 'vector_render_engine/content_provider/graphics/graphics_group/graphics_group_style';
import {GraphicsFeature, GraphicsObjectStyle} from 'vector_render_engine/content_provider/graphics/graphics_object/graphics_object_description';
import Camera from 'vector_render_engine/camera';
import {GraphicsManagerOptions} from 'vector_render_engine/content_provider/graphics/graphics_manager/graphics_manager_options';

chai.use(sinonChai);

describe('GraphicsManager', () => {
    let manager: GraphicsManager;
    let communicators: PairOfCommunicators<GraphicsManagerMessages>;
    let onMessageStub: SinonStub;
    let camera: Camera;

    beforeEach(() => {
        camera = new Camera();
        camera.center.x = 0;
        camera.center.y = 0;
        camera.zoom = 0;
        communicators = createPairOfCommunicators();
        manager = new GraphicsManager(communicators.master, OPTIONS);
        onMessageStub = stub();

        communicators.worker.onMessage.addListener(onMessageStub);
    });

    it('should add group', () => {
        const id = manager.addGroup(GROUP_STYLE);

        const message = onMessageStub.lastCall.args[0];
        expect(onMessageStub).to.have.been.calledOnce;
        expect(message.type).to.equal(GraphicsManagerMessageType.TO_BACKEND_ADD_GROUP);
        expect(message.id).to.equal(id);
        expect(message.style).to.deep.equal(GROUP_STYLE);
    });

    it('should use given id for a group', () => {
        const id = manager.addGroup(GROUP_STYLE, undefined, 'my_id');

        const message = onMessageStub.lastCall.args[0] as AddGraphicsGroup;
        expect(id).to.equal('my_id');
        expect(message.id).to.equal('my_id');
    });

    it('should add object', () => {;
        const id = manager.addObject(FEATURE);

        const message = onMessageStub.lastCall.args[0] as AddGraphicsObject;
        expect(onMessageStub).to.have.been.calledOnce;
        expect(message.type).to.equal(GraphicsManagerMessageType.TO_BACKEND_ADD_OBJECT);
        expect(message.id).to.equal(id);
        expect(message.feature).to.deep.equal(FEATURE);
    });

    it('should use given id for an object', () => {
        const id = manager.addObject(FEATURE, OBJECT_STYLE, undefined, 'my_id');

        const message = onMessageStub.lastCall.args[0] as AddGraphicsObject;
        expect(id).to.equal('my_id');
        expect(message.id).to.equal('my_id');
    });

    it('should update object', () => {
        manager.addObject(FEATURE, undefined, 'object');

        manager.updateObject('object', FEATURE);

        const message = onMessageStub.lastCall.args[0] as UpdateGraphicsObject;
        expect(message.type).to.equal(GraphicsManagerMessageType.TO_BACKEND_UPDATE_OBJECT);
        expect(message.id).to.equal('object');
        expect(message.feature).to.deep.equal(FEATURE);
     });

    it('should remove object', () => {
        const id = manager.addObject(FEATURE);
        onMessageStub.reset();

        manager.removeObject(id);

        expect(onMessageStub).to.have.been.calledOnce;
        expect(onMessageStub.lastCall.args[0].type).to.equal(GraphicsManagerMessageType.TO_BACKEND_REMOVE_OBJECT);
    });

    it('should remove group and children', () => {
        manager.addGroup(GROUP_STYLE, 'group');
        manager.addObject(FEATURE, OBJECT_STYLE, 'group');
        manager.addObject(FEATURE, OBJECT_STYLE, 'group');
        onMessageStub.reset();

        manager.removeGroupAndChildren('group');

        expect(onMessageStub).to.have.been.calledOnce;
        expect(onMessageStub.lastCall.args[0].type).to.equal(
            GraphicsManagerMessageType.TO_BACKEND_REMOVE_GROUP_AND_CHILDREN
        );
    });

    it('should add root group', () => {
        manager = new GraphicsManager(communicators.master, OPTIONS);

        expect(onMessageStub.getCalls()[1].args[0].type).to.equal(GraphicsManagerMessageType.TO_BACKEND_ADD_GROUP);
    });

    it('should setup the backend part', () => {
        manager = new GraphicsManager(communicators.master, OPTIONS);

        expect(onMessageStub.getCalls()[0].args[0].type).to.equal(GraphicsManagerMessageType.TO_BACKEND_SETUP);
    });
});

const FEATURE: GraphicsFeature = {
    type: 'Feature',
    geometry: {
        type: 'LineString',
        coordinates: [{x: 0, y: 0}, {x: 1, y: 0}, {x: 1, y: 1}]
    },
    properties: {
        style: {
            'line-width': 10,
            'line-color': '#0f0'
        }
    }
};

const OBJECT_STYLE: GraphicsObjectStyle = {
    zIndex: 0,
    simplificationRate: 0
};

const GROUP_STYLE: GraphicsGroupStyle = {
    opacity: 0.5,
    zIndex: 10
};

const OPTIONS: GraphicsManagerOptions = {
    minSimplificationZoom: 0,
    maxSimplificationZoom: 10,
    simplificationPreloadedZooms: 0
};
