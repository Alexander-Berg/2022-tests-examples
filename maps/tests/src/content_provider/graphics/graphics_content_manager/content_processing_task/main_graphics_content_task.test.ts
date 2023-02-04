import * as chai from 'chai';
import {expect} from 'chai';
import {SinonStub, stub} from 'sinon';
import * as sinonChai from 'sinon-chai';
import {BaseContentProcessingAsyncTask, ContentPriority} from '../../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/content_manager/content_processing_tasks/base_async_task';
import {VectorPrimitiveTypes} from '../../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/util/vector_primitive_types';
import {DEFAULT_OPTIONS} from '../../../../../../src/vector_render_engine/content_provider/graphics/content_manager/graphics_content_manager_backend';
import {GraphicsContentBackendPortion} from '../../../../../../src/vector_render_engine/content_provider/graphics/content_manager/util/graphics_content_backend_portion';
import {GraphicsObjectBackend} from '../../../../../../src/vector_render_engine/content_provider/graphics/graphics_object/graphics_object_backend';
import {ObjectIdProvider} from '../../../../../../src/vector_render_engine/util/id_provider';
import {TaskQueueStub} from '../../../../util/task_queue_stub';
import {GraphicsFeature} from 'vector_render_engine/content_provider/graphics/graphics_object/graphics_object_description';
import {MainGraphicsContentTask} from 'vector_render_engine/content_provider/graphics/content_manager/content_processing_task/main_graphics_content_task';
import {GraphicsDetalizationLevelBackend} from 'vector_render_engine/content_provider/graphics/graphics_detaliztion_level/graphics_detalization_level_backend';
import {GraphicsGroupBackend} from 'vector_render_engine/content_provider/graphics/graphics_group/graphics_group_backend';
import {GraphicsDetalizationLevelState} from 'vector_render_engine/content_provider/graphics/graphics_detaliztion_level/graphics_detalization_level_state';

chai.use(sinonChai);

const CONTENT = {content: {primitives: {}}};
const POLYGONS = {content: {primitives: {[VectorPrimitiveTypes.OPAQUE_POLYGON]: [{location: {} as any}]}}};
const POLYLINES = {content: {primitives: {
    [VectorPrimitiveTypes.POLYLINE]: [{location: {} as any, atlasId: 0, opacity: 1}]}}
};
const ICONS = {content: {primitives: {[VectorPrimitiveTypes.ICON]: [{location: {} as any, atlasId: 0, zIndex: 0}]}}};

const FEATURE: GraphicsFeature = {
    type: 'Feature',
    geometry: {
        type: 'LineString',
        coordinates: [{x: 0, y: 0}, {x: 0, y: 1}]
    },
    properties: {
        style: {
            "line-color": '#3F3',
            "line-width": 5
        },
    }
};

class TaskStub<T = any> extends BaseContentProcessingAsyncTask<any, T> {
    constructor(taskQueue: TaskQueueStub) {
        super(taskQueue, 0, ContentPriority.HIGH_PRIORITY);
    }

    emitContent(content: T): void {
        this._onContentReady.fire(content);
    }

    protected _startImpl(): void {
        this._scheduleProcessing({});
    }

    protected _process(): void {}
}

class TestGraphicsContentTask extends MainGraphicsContentTask {
    constructor(level: GraphicsDetalizationLevelBackend, taskQueue: TaskQueueStub) {
        const mock = {} as any;
        const ids: ObjectIdProvider = {idGenerator: () => 0, collidingIdGenerator: () => 1};
        const options = DEFAULT_OPTIONS;
        super(level, options, taskQueue, 0, mock, mock, mock, mock, mock, mock, mock, mock, mock, mock, ids);

        this._writePolygons = stub().returns(POLYGONS);
        this._writePolylines = stub().returns(CONTENT);
        this._writeIcons = stub().returns(CONTENT);
        this._writeLabels = stub().returns(CONTENT);
    }

    startSubTask(task: BaseContentProcessingAsyncTask<any, GraphicsContentBackendPortion>): void {
        this._startSubTask(task);
    }
}

describe('MainGraphicsContentTask', () => {
    let task: TestGraphicsContentTask;
    let object: GraphicsObjectBackend;
    let level: GraphicsDetalizationLevelBackend;
    let taskQueue: TaskQueueStub;
    let onContentReadyStub: SinonStub;
    let root: GraphicsGroupBackend;

    beforeEach(() => {
        root = new GraphicsGroupBackend('root', {zIndex: 0, opacity: 1}, null);
        object = new GraphicsObjectBackend('object', FEATURE, {zIndex: 0, simplificationRate: 0}, root);
        level = new GraphicsDetalizationLevelBackend('level', object, 0, GraphicsDetalizationLevelState.VISIBLE);
        taskQueue = new TaskQueueStub();
        task = new TestGraphicsContentTask(level, taskQueue);
        onContentReadyStub = stub();

        task.onContentReady.addListener(onContentReadyStub);
    });

    it('should emit content as ready if there are no subtasks', () => {
        task.start();

        taskQueue.dequeueAll();

        const content = onContentReadyStub.lastCall.args[0] as GraphicsContentBackendPortion;
        expect(onContentReadyStub).to.have.been.calledOnce;
        expect(content.content.primitives[VectorPrimitiveTypes.OPAQUE_POLYGON]!.length).to.equal(1);
    });

    it('should accumulate content and wait for all subtasks to finish before emit', () => {
        const subtask_1 = new TaskStub<GraphicsContentBackendPortion>(taskQueue);
        const subtask_2 = new TaskStub<GraphicsContentBackendPortion>(taskQueue);
        task.start();
        task.startSubTask(subtask_1);
        task.startSubTask(subtask_2);

        subtask_1.emitContent(POLYLINES);
        expect(onContentReadyStub).to.not.have.been.called;

        taskQueue.dequeueAll();
        expect(onContentReadyStub).to.not.have.been.called;

        subtask_2.emitContent(ICONS);
        expect(onContentReadyStub).to.have.been.calledOnce;

        const content = onContentReadyStub.lastCall.args[0] as GraphicsContentBackendPortion;
        expect(content.content.primitives[VectorPrimitiveTypes.OPAQUE_POLYGON]!.length).to.equal(1);
        expect(content.content.primitives[VectorPrimitiveTypes.POLYLINE]!.length).to.equal(1);
        expect(content.content.primitives[VectorPrimitiveTypes.ICON]!.length).to.equal(1);
    });

    it('should resume', () => {
        const subtask = new TaskStub<GraphicsContentBackendPortion>(taskQueue);
        task.start();
        task.startSubTask(subtask);

        task.pause();
        expect(taskQueue.size).to.equal(0);

        task.resume();
        expect(taskQueue.size).to.equal(2);

        taskQueue.dequeueAll();
        expect(onContentReadyStub).to.not.have.been.called;

        subtask.emitContent(ICONS);
        expect(onContentReadyStub).to.have.been.calledOnce;
    });
});
