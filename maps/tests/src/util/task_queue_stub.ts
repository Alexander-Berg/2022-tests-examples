import TaskQueue, {Task} from '../../../src/vector_render_engine/util/task_queue';

/**
 * Stub TaskQueue for tests, dequeue is done manually by calling dequeueAll() method.
 */
export class TaskQueueStub extends TaskQueue {
    get size(): number {
        return this._queue.size;
    }

    enqueueSync(task: Task): void {
        this._queue.enqueue(task);
    }

    dequeueAll(): void {
        while (this._queue.size) {
            this._queue.dequeue()!.execute();
        }
    }
}
