import {expect} from 'chai';
import {SinonSpy, spy} from 'sinon';
import * as chai from 'chai';
import * as sinonChai from 'sinon-chai';
import {BaseContentProcessingAsyncTask, ContentPriority} from '../../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/content_manager/content_processing_tasks/base_async_task';
import {GraphicsStateManagedTaskWrapper} from '../../../../../../src/vector_render_engine/content_provider/graphics/content_manager/content_processing_task/graphics_state_managed_task_wrapper';
import {GraphicsContentBackendPortion} from '../../../../../../src/vector_render_engine/content_provider/graphics/content_manager/util/graphics_content_backend_portion';
import {GraphicsDetalizationLevelBackend} from '../../../../../../src/vector_render_engine/content_provider/graphics/graphics_detaliztion_level/graphics_detalization_level_backend';
import {GraphicsDetalizationLevelState} from '../../../../../../src/vector_render_engine/content_provider/graphics/graphics_detaliztion_level/graphics_detalization_level_state';
import {GraphicsGroupBackend} from '../../../../../../src/vector_render_engine/content_provider/graphics/graphics_group/graphics_group_backend';
import {GraphicsObjectBackend} from '../../../../../../src/vector_render_engine/content_provider/graphics/graphics_object/graphics_object_backend';
import TaskQueue from '../../../../../../src/vector_render_engine/util/task_queue';
import {TaskQueueStub} from '../../../../util/task_queue_stub';
import {GraphicsViewportBackendStub} from '../../graphics_viewport/graphics_viewport_backend_stub';

chai.use(sinonChai);

describe('GraphicsStateManagedTaskWrapper', () => {
    let wrapper: GraphicsStateManagedTaskWrapper;
    let task: TestGraphicsProcessingTask;
    let viewport: GraphicsViewportBackendStub;
    let taskQueue: TaskQueueStub;
    let level: GraphicsDetalizationLevelBackend;
    let object: GraphicsObjectBackend;
    let group: GraphicsGroupBackend;

    beforeEach(() => {
        taskQueue = new TaskQueueStub();
        task = new TestGraphicsProcessingTask(taskQueue, 42);
        viewport = new GraphicsViewportBackendStub();
        group = new GraphicsGroupBackend('group', {opacity: 1, zIndex: 0}, null);
        object = new GraphicsObjectBackend('object', {} as any, {zIndex: 0, simplificationRate: 1}, group);
        level = new GraphicsDetalizationLevelBackend('level', object, 0, GraphicsDetalizationLevelState.VISIBLE);
        wrapper = new GraphicsStateManagedTaskWrapper(level, task, viewport, 0);
     });

    describe('Starting a task', () => {
        let startSpy: SinonSpy;

        beforeEach(() => {
            startSpy = spy(task, 'start');
        });

        it('should start when VISIBLE', () => {
            level.state = GraphicsDetalizationLevelState.VISIBLE;
            wrapper.start();
            expect(startSpy).to.have.been.called;
        });

        it('should start when PRELOADED', () => {
            level.state = GraphicsDetalizationLevelState.PRELOADED;
            wrapper.start();
            expect(startSpy).to.have.been.called;
        });

        it('should start when ALWAYS_VISIBLE', () => {
            level.state = GraphicsDetalizationLevelState.ALWAYS_VISIBLE;
            wrapper.start();
            expect(startSpy).to.have.been.called;
        });

        it('should not start when CACHED', () => {
            level.state = GraphicsDetalizationLevelState.CACHED;
            wrapper.start();
            expect(startSpy).to.not.have.been.called;

        });

        it('should not start when OBSOLETE', () => {
            level.state = GraphicsDetalizationLevelState.OBSOLETE;
            wrapper.start();
            expect(startSpy).to.not.have.been.called;
        });
    });

    describe('Pausing a task', () => {
        let pauseSpy: SinonSpy;

        beforeEach(() => {
            pauseSpy = spy(task, 'pause');
            wrapper.start();
        });

        it('should pause when CACHED', () => {
            level.state = GraphicsDetalizationLevelState.CACHED;
            expect(pauseSpy).to.have.been.called;
        });

        it('should pause when OBSOLETE', () => {
            level.state = GraphicsDetalizationLevelState.OBSOLETE;
            expect(pauseSpy).to.have.been.called;
        });

        it('should not pause when VISIBLE, PRELOADED or ALWAYS_VISIBLE', () => {
            level.state = GraphicsDetalizationLevelState.VISIBLE;
            expect(pauseSpy).to.not.have.been.called;

            level.state = GraphicsDetalizationLevelState.PRELOADED;
            expect(pauseSpy).to.not.have.been.called;

            level.state = GraphicsDetalizationLevelState.ALWAYS_VISIBLE;
            expect(pauseSpy).to.not.have.been.called;
        });
    });


    describe('Resuming a task', () => {
        let resumeSpy: SinonSpy;

        beforeEach(() => {
            resumeSpy = spy(task, 'resume');
            wrapper.start();
            level.state = GraphicsDetalizationLevelState.CACHED;
        });

        it('should resume when VISIBLE', () => {
            level.state = GraphicsDetalizationLevelState.VISIBLE;
            expect(resumeSpy).to.have.been.called;
        });

        it('should resume when PRELOADED', () => {
            level.state = GraphicsDetalizationLevelState.PRELOADED;
            expect(resumeSpy).to.have.been.called;
        });

        it('should resume when ALWAYS_VISIBLE', () => {
            level.state = GraphicsDetalizationLevelState.ALWAYS_VISIBLE;
            expect(resumeSpy).to.have.been.called;
        });

        it('should not resume when CACHED or OBSOLETE', () => {
            level.state = GraphicsDetalizationLevelState.CACHED;
            expect(resumeSpy).to.not.have.been.called;

            level.state = GraphicsDetalizationLevelState.OBSOLETE;
            expect(resumeSpy).to.not.have.been.called;
        });
    });

    describe('Resetting a task priority', () => {
        let resetPrioritySpy: SinonSpy;

        beforeEach(() => {
            wrapper.start();
            // `task.resetPriority()` is called on `start()` so we create a spy after `start()`.
            resetPrioritySpy = spy(task, 'resetPriority');
        });

        it('should reset when state becomes VISIBLE, PRELOADED or ALWAYS_VISIBLE', () => {
            level.state = GraphicsDetalizationLevelState.ALWAYS_VISIBLE;
            level.state = GraphicsDetalizationLevelState.VISIBLE;
            level.state = GraphicsDetalizationLevelState.PRELOADED;
            expect(resetPrioritySpy.callCount).to.equal(3);
        });

        it('should not reset when state becomes CACHED or OBSOLETE', () => {
            level.state = GraphicsDetalizationLevelState.CACHED;
            level.state = GraphicsDetalizationLevelState.OBSOLETE;
            expect(resetPrioritySpy.callCount).to.equal(0);
        });

        describe('Handling viewport update', () => {
            it('should reset when PRELOADED', () => {
                level.state = GraphicsDetalizationLevelState.PRELOADED;
                resetPrioritySpy.reset();

                viewport.setZoom(5);
                expect(resetPrioritySpy).to.have.been.called;
            });

            it('should not reset when VISIBLE', () => {
                viewport.setZoom(5);
                expect(resetPrioritySpy).to.not.have.been.called;
            });

            it('should not reset when ALWAYS_VISIBLE', () => {
                level.state = GraphicsDetalizationLevelState.ALWAYS_VISIBLE;
                resetPrioritySpy.reset();
                viewport.setZoom(5);
                expect(resetPrioritySpy).to.not.have.been.called;
            });

            it('should not reset when CACHED', () => {
                level.state = GraphicsDetalizationLevelState.CACHED;
                resetPrioritySpy.reset();
                viewport.setZoom(5);
                expect(resetPrioritySpy).to.not.have.been.called;
            });

            it('should not reset when OBSOLETE', () => {
                level.state = GraphicsDetalizationLevelState.OBSOLETE;
                resetPrioritySpy.reset();
                viewport.setZoom(5);
                expect(resetPrioritySpy).to.not.have.been.called;
            });
        });
    });

    it('should destroy task', () => {
        const destructor = spy(task, 'destructor');

        wrapper.destructor();

        expect(destructor).to.have.been.called;
    });
});

class TestGraphicsProcessingTask extends BaseContentProcessingAsyncTask<number, GraphicsContentBackendPortion> {
    constructor(taskQueue: TaskQueue, data: number) {
        super(taskQueue, 0, ContentPriority.HIGH_PRIORITY);

        this._data = data;
    }

    protected _startImpl(): void {
        this._scheduleProcessing(this._data);
    }

    protected _process(_data: number): GraphicsContentBackendPortion {
        return {content: {primitives: {}}};
    }

    private readonly _data: number;
}
