import * as chai from 'chai';
import {expect} from 'chai';
import {spy} from 'sinon';
import * as sinonChai from 'sinon-chai';
import {VectorTileBackend, VectorTileBackendState} from '../../../../../../src/vector_render_engine/content_provider/vector_tile_map/tile/vector_tile_backend';
import TaskQueue from '../../../../../../src/vector_render_engine/util/task_queue';
import {TaskQueueStub} from '../../../../util/task_queue_stub';
import {promisifyEventEmitter} from '../../../../util/event_emitter';
import {TileViewportBackend} from '../../../../../../src/vector_render_engine/content_provider/vector_tile_map/viewport/tile_viewport_backend';
import {EventTrigger} from '../../../../../../src/vector_render_engine/util/event_emitter';
import {BaseContentProcessingAsyncTask, ContentPriority} from '../../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/content_manager/content_processing_tasks/base_async_task';
import {TileStateManagedTaskWrapper} from '../../../../../../src/vector_render_engine/content_provider/vector_tile_map/tile_content_manager/content_processing_tasks/tile_state_managed_task_wrapper';
import {LoadManagerStub} from '../../util/load_manager_stub';
import {BaseContentLoadingAsyncTask} from '../../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/content_manager/content_processing_tasks/base_loading_async_task';
import {LoadManager} from '../../../../../../src/vector_render_engine/util/load_manager';

chai.use(sinonChai);

class TestTileProcessingTask extends BaseContentProcessingAsyncTask<number, number> {
    constructor(taskQueue: TaskQueue, data: number) {
        super(taskQueue, 0, ContentPriority.HIGH_PRIORITY);

        this._data = data;
    }

    protected _startImpl(): void {
        this._scheduleProcessing(this._data);
    }

    protected _process(data: number): number {
        return data * 2;
    }

    private readonly _data: number;
}


class TestTileLoadingTask extends BaseContentLoadingAsyncTask<number, number> {
    constructor(taskQueue: TaskQueue, loadManager: LoadManager, data: number) {
        super(taskQueue, 0, ContentPriority.HIGH_PRIORITY);

        this._data = data;
        this._loadManager = loadManager;
    }

    protected _loadImpl(): Promise<number> {
        this._activeLoading = this._loadManager.load('fake url', 0);
        return this._activeLoading.then(() => this._data);
    }

    protected _loadCancel(): void {
        this._loadManager.cancel(this._activeLoading);
    }

    protected _process(data: number): number {
        return data * 2;
    }

    private readonly _data: number;
    private readonly _loadManager: LoadManager;
    private _activeLoading: Promise<ArrayBuffer>;
}

describe('TileStateManagedTaskWrapper', () => {
    let onViewportChanged: EventTrigger<any>;
    let viewport: TileViewportBackend;

    beforeEach(() => {
        onViewportChanged = new EventTrigger();
        const viewportStub: Partial<TileViewportBackend> = {
            onViewportChanged,
            cameraPosition: {x: 0, y: 0, zoom: 0}
        };
        viewport = viewportStub as TileViewportBackend;
    });

    it('should start only if the tile is not cached', () => {
        const tile = new VectorTileBackend({id: '1_1_1', x: 1, y: 1, zoom: 1}, VectorTileBackendState.CACHED);
        const taskQueue = new TaskQueueStub();
        const task = new TestTileProcessingTask(taskQueue, 1);
        const wrapper = new TileStateManagedTaskWrapper(tile, task, viewport, 0);
        const start = spy(task, 'start');

        wrapper.start();
        expect(start).not.to.be.called;

        tile.state = VectorTileBackendState.PRELOADED;
        expect(start).to.be.called;
    });

    it('should stop loading if tile becomes invisible (or preloaded)', () => {
        const tile = new VectorTileBackend({id: '1_1_1', x: 1, y: 1, zoom: 1}, VectorTileBackendState.VISIBLE);
        const taskQueue = new TaskQueueStub();
        const loadManager = new LoadManagerStub();
        const task = new TestTileLoadingTask(taskQueue, loadManager, 1);
        const wrapper = new TileStateManagedTaskWrapper(tile, task, viewport, 0);

        wrapper.start();
        expect(loadManager.size).to.be.equal(1);

        tile.state = VectorTileBackendState.CACHED;
        expect(loadManager.size).to.be.equal(0);

        tile.state = VectorTileBackendState.VISIBLE;
        expect(loadManager.size).to.be.equal(1);
    });

    it('should dequeue task from queue by "pause" and return it back by "resume"', () => {
        const tile = new VectorTileBackend({id: '1_1_1', x: 1, y: 1, zoom: 1}, VectorTileBackendState.VISIBLE);
        const taskQueue = new TaskQueueStub();
        const task = new TestTileProcessingTask(taskQueue, 1);
        const wrapper = new TileStateManagedTaskWrapper(tile, task, viewport, 0);

        wrapper.start();
        expect(taskQueue.size).to.be.equal(1);

        // pausing processing
        tile.state = VectorTileBackendState.CACHED;
        expect(taskQueue.size).to.be.equal(0);

        // resume processing
        tile.state = VectorTileBackendState.VISIBLE;
        expect(taskQueue.size).to.be.equal(1);
    });

    it('should emit content', async () => {
        const tile = new VectorTileBackend({id: '1_1_1', x: 1, y: 1, zoom: 1}, VectorTileBackendState.VISIBLE);
        const taskQueue = new TaskQueueStub();
        const task = new TestTileProcessingTask(taskQueue, 10);
        const wrapper = new TileStateManagedTaskWrapper(tile, task, viewport, 0);

        wrapper.start();

        const content = promisifyEventEmitter(task.onContentReady).then(([content]) => {
            expect(content).to.be.equal(20);
        });

        taskQueue.dequeueAll();

        await content;
    });

    it('should not forget task in the queue after "resume"', async () => {
        const tile = new VectorTileBackend({id: '1_1_1', x: 1, y: 1, zoom: 1}, VectorTileBackendState.VISIBLE);
        const taskQueue = new TaskQueueStub();
        const task = new TestTileProcessingTask(taskQueue, 1);
        const wrapper = new TileStateManagedTaskWrapper(tile, task, viewport, 0);

        wrapper.start();
        expect(taskQueue.size).to.be.equal(1);

        // pause processing
        tile.state = VectorTileBackendState.CACHED;
        expect(taskQueue.size).to.be.equal(0);

        // resume processing
        tile.state = VectorTileBackendState.VISIBLE;
        expect(taskQueue.size).to.be.equal(1);

        // update priority
        onViewportChanged.fire();

        // pause, should dequeue task
        tile.state = VectorTileBackendState.CACHED;
        expect(taskQueue.size).to.be.equal(0);

        // update priority, resume
        tile.state = VectorTileBackendState.PRELOADED;
        expect(taskQueue.size).to.be.equal(1);

        // pause, should dequeue task
        tile.state = VectorTileBackendState.CACHED;
        expect(taskQueue.size).to.be.equal(0);
    });
});
