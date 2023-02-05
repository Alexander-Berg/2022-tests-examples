import { resolve } from '../../../../../common/xpromise-support'
import { Int32, int64, Int64, Nullable } from '../../../../../common/ys'
import { MockFileSystem, MockJSONSerializer } from '../../../../common/__tests__/__helpers__/mock-patches'
import { XPromise } from '../../../../common/code/promise/xpromise'
import { getVoid } from '../../../../common/code/result/result'
import { File } from '../../../../common/code/file-system/file'
import { NotDeletedCommandFiles } from '../../../code/busilogics/commands/not-deleted-command-files'
import { BufferedMessageQueue } from '../../../code/service/buffered-message-queue'
import { LinkedBlockingDeque } from '../../../code/service/linked-blocking-deque'
import { Task, TaskType } from '../../../code/service/task'
import { TaskSerializer } from '../../../code/service/task-serializer'
import { TaskWrapper } from '../../../code/service/task-wrapper'
import { CustomTypeTestTask } from '../../__helpers__/custom-type-test-task'
import { MockHighPrecisionTimer, MockStorage } from '../../__helpers__/mock-patches'
import { MockModels } from '../../__helpers__/models'
import { rejected } from '../../__helpers__/test-failure'
import {
  registerLinkedBlockingDeque,
  TestLinkedBlockingDeque,
  unregisterLinkedBlockingDeque,
} from '../../__helpers__/test-linked-blocking-queue'
import { TestTask } from '../../__helpers__/test-task'

function makeTestTaskWrapper(uid: Int32, type: TaskType, fileName: string, draftID?: Int32): TaskWrapper {
  return TaskWrapper.create(makeTestTask(uid, type, draftID), new File(fileName))
}
function makeTestTask(uid: Int32, type: TaskType, draftID?: Int32): Task {
  return new CustomTypeTestTask(1, uid, type, draftID === undefined ? undefined : { draftID: int64(draftID) })
}

class PatchedBufferedMessageQueue extends BufferedMessageQueue {
  public addTaskToBuffer_(file: Nullable<File>): XPromise<void> {
    if (this.addTaskToBuffer === undefined) {
      return fail('should have [addTaskToBuffer] method in the base class')
    }
    return this.addTaskToBuffer(file)
  }

  public buffer_(): TaskWrapper[] {
    if (this.buffer === undefined) {
      return fail('should have [buffer] property in the base class')
    }
    return this.buffer
  }
  public dropBuffer_() {
    if (this.buffer === undefined) {
      fail('should have [buffer] property in the base class')
    }
    this.buffer = []
  }

  public metaCache_(): Map<Int64, Set<Int64>> {
    if (this.metaCache === undefined) {
      return fail('should have [metaCache] property in the base class')
    }
    return this.metaCache
  }
  public dropMetaCache_() {
    if (this.metaCache === undefined) {
      fail('should have [metaCache] property in the base class')
    }
    this.metaCache = new Map()
  }

  public filterOutPreviousDraftTasksFromBuffer_(taskToAdd: TaskWrapper): XPromise<void> {
    if (this.filterOutPreviousDraftTasksFromBuffer === undefined) {
      return fail('should have [filterOutPreviousDraftTasksFromBuffer] property in the base class')
    }
    return this.filterOutPreviousDraftTasksFromBuffer(taskToAdd)
  }

  public removeFromCache_(task: TaskWrapper): void {
    if (this.removeFromCache === undefined) {
      return fail('should have [removeFromCache] property in the base class')
    }
    return this.removeFromCache(task)
  }

  public containsStoreSend_(task: TaskWrapper): boolean {
    if (this.containsStoreSend === undefined) {
      return fail('should have [containsStoreSend] property in the base class')
    }
    return this.containsStoreSend(task)
  }

  public storeInCache_(task: TaskWrapper): void {
    if (this.storeInCache === undefined) {
      return fail('should have [storeInCache] property in the base class')
    }
    return this.storeInCache(task)
  }
  // tslint:enable: no-string-literal
}

describe(BufferedMessageQueue, () => {
  beforeAll(registerLinkedBlockingDeque)
  afterEach(unregisterLinkedBlockingDeque)
  describe('constructor', () => {
    it('should initialize its base class', () => {
      const fs = MockFileSystem()
      const taskSerializer = new TaskSerializer(
        'commands',
        fs,
        MockHighPrecisionTimer(),
        MockJSONSerializer(),
        MockModels(),
      )
      const queue = new LinkedBlockingDeque<File>()
      const notDeletedCommandFiles = new NotDeletedCommandFiles(MockStorage())
      const messageQueue = new PatchedBufferedMessageQueue(taskSerializer, fs, queue, notDeletedCommandFiles)
      // tslint:disable: no-string-literal
      expect((messageQueue as any).taskSerializer).toBe(taskSerializer)
      expect((messageQueue as any).fs).toBe(fs)
      expect((messageQueue as any).queue).toBe(queue)
      expect((messageQueue as any).notDeletedCommandFiles).toBe(notDeletedCommandFiles)
      // tslint:enable: no-string-literal
    })
  })
  describe(BufferedMessageQueue.create, () => {
    // tslint:disable-next-line: max-line-length
    it('should fetch files from dir and from Storage, remove blacklisted from the filesystem and from the Storage', (done) => {
      const serializer = MockJSONSerializer()
      const fs = MockFileSystem({
        listDirectory: jest.fn().mockReturnValue(resolve(['file2', 'file3', 'file1', 'file2'])),
        delete: jest.fn().mockReturnValue(resolve(getVoid())),
        ensureFolderExists: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const storage = MockStorage()
      const notDeletedCommandFiles = new NotDeletedCommandFiles(storage)
      const fetchAllSpy = jest
        .spyOn(notDeletedCommandFiles, 'fetchAll')
        .mockReturnValue(resolve(['file2', 'file3', 'file4']))
      const deleteSpy = jest.spyOn(notDeletedCommandFiles, 'delete').mockReturnValue(resolve(getVoid()))
      const taskSerializer = new TaskSerializer(
        'commands',
        fs,
        MockHighPrecisionTimer(() => int64(12345)),
        serializer,
        MockModels(),
      )
      BufferedMessageQueue.create('commands', fs, taskSerializer, notDeletedCommandFiles).then((res) => {
        expect(res).toBeInstanceOf(BufferedMessageQueue)
        expect(res).not.toBeNull()
        expect(((res as any).queue.deque as TestLinkedBlockingDeque)._items).toStrictEqual([new File('file1')])
        fetchAllSpy.mockRestore()
        deleteSpy.mockRestore()
        done()
      })
    })
  })
  describe('isEmpty', () => {
    it('should return true if both buffer and queue is empty, false if either is non-empty', () => {
      const fs = MockFileSystem()
      const taskSerializer = new TaskSerializer(
        'commands',
        fs,
        MockHighPrecisionTimer(),
        MockJSONSerializer(),
        MockModels(),
      )
      const queue = new LinkedBlockingDeque<File>()
      const notDeletedCommandFiles = new NotDeletedCommandFiles(MockStorage())
      const messageQueue = new PatchedBufferedMessageQueue(taskSerializer, fs, queue, notDeletedCommandFiles)
      expect(messageQueue.isEmpty()).toBe(true)
      queue.offer(new File('queue1'))
      expect(messageQueue.isEmpty()).toBe(false)
      messageQueue.buffer_().push(TaskWrapper.create(new TestTask(1, int64(1), MockModels()), new File('buffer1')))
      expect(messageQueue.isEmpty()).toBe(false)
      queue.poll()
      expect(messageQueue.isEmpty()).toBe(false)
      messageQueue.buffer_().shift()
      expect(messageQueue.isEmpty()).toBe(true)
    })
  })
  describe('clear', () => {
    it('should clear both buffer and queue', () => {
      const fs = MockFileSystem()
      const taskSerializer = new TaskSerializer(
        'commands',
        fs,
        MockHighPrecisionTimer(),
        MockJSONSerializer(),
        MockModels(),
      )
      const queue = new LinkedBlockingDeque<File>()
      const notDeletedCommandFiles = new NotDeletedCommandFiles(MockStorage())
      const messageQueue = new PatchedBufferedMessageQueue(taskSerializer, fs, queue, notDeletedCommandFiles)
      expect(messageQueue.isEmpty()).toBe(true)
      queue.offer(new File('file1'))
      expect(messageQueue.isEmpty()).toBe(false)
      messageQueue.clear()
      expect(messageQueue.isEmpty()).toBe(true)

      queue.offer(new File('file1'))
      messageQueue.buffer_().push(makeTestTaskWrapper(1, TaskType.saveDraft, 'buffer1'))
      expect(messageQueue.isEmpty()).toBe(false)
      messageQueue.clear()
      expect(messageQueue.isEmpty()).toBe(true)

      messageQueue.buffer_().push(makeTestTaskWrapper(2, TaskType.saveDraft, 'buffer2'))
      expect(messageQueue.isEmpty()).toBe(false)
      messageQueue.clear()
      expect(messageQueue.isEmpty()).toBe(true)
    })
  })
  describe('poll', () => {
    it('should poll queue directly if the buffer is empty', () => {
      const fs = MockFileSystem()
      const taskSerializer = new TaskSerializer(
        'commands',
        fs,
        MockHighPrecisionTimer(),
        MockJSONSerializer(),
        MockModels(),
      )
      const queue = new LinkedBlockingDeque<File>()
      queue.offer(new File('file1'))

      const notDeletedCommandFiles = new NotDeletedCommandFiles(MockStorage())
      const messageQueue = new BufferedMessageQueue(taskSerializer, fs, queue, notDeletedCommandFiles)
      expect(messageQueue.poll()).toStrictEqual(new File('file1'))
    })
    it('should poll the buffer leaving cache intact if no such element there', () => {
      const fs = MockFileSystem()
      const taskSerializer = new TaskSerializer(
        'commands',
        fs,
        MockHighPrecisionTimer(),
        MockJSONSerializer(),
        MockModels(),
      )
      const queue = new LinkedBlockingDeque<File>()
      const notDeletedCommandFiles = new NotDeletedCommandFiles(MockStorage())
      const messageQueue = new PatchedBufferedMessageQueue(taskSerializer, fs, queue, notDeletedCommandFiles)
      messageQueue
        .buffer_()
        .push(
          makeTestTaskWrapper(123, TaskType.saveDraft, 'buffer-file1', 1),
          makeTestTaskWrapper(1, TaskType.archive, 'buffer-file3'),
        )
      messageQueue.metaCache_().set(int64(321), new Set([int64(3)]))
      expect(messageQueue.poll()).toStrictEqual(new File('buffer-file1'))
      expect(messageQueue.buffer_()).toStrictEqual([makeTestTaskWrapper(1, TaskType.archive, 'buffer-file3')])
      expect(messageQueue.metaCache_()).toStrictEqual(new Map([[int64(321), new Set([int64(3)])]]))
    })
    it('should poll the buffer if it is non-empty', () => {
      const fs = MockFileSystem()
      const taskSerializer = new TaskSerializer(
        'commands',
        fs,
        MockHighPrecisionTimer(),
        MockJSONSerializer(),
        MockModels(),
      )
      const queue = new LinkedBlockingDeque<File>()
      queue.offer(new File('queue-file'))

      const notDeletedCommandFiles = new NotDeletedCommandFiles(MockStorage())
      const messageQueue = new PatchedBufferedMessageQueue(taskSerializer, fs, queue, notDeletedCommandFiles)
      messageQueue
        .buffer_()
        .push(
          makeTestTaskWrapper(123, TaskType.saveDraft, 'buffer-file1', 1),
          makeTestTaskWrapper(1, TaskType.archive, 'buffer-file3'),
          makeTestTaskWrapper(321, TaskType.saveDraft, 'buffer-file4', 3),
          makeTestTaskWrapper(123, TaskType.saveDraft, 'buffer-file2', 2),
        )
      messageQueue
        .metaCache_()
        .set(int64(123), new Set([int64(1), int64(2)]))
        .set(int64(321), new Set([int64(3)]))
      expect(messageQueue.metaCache_().size).toBe(2)
      expect(messageQueue.metaCache_().get(int64(123))!.size).toBe(2)
      expect(messageQueue.metaCache_().get(int64(321))!.size).toBe(1)

      expect(messageQueue.poll()).toStrictEqual(new File('buffer-file1'))
      expect(messageQueue.metaCache_().get(int64(123))!.size).toBe(1)
      expect(messageQueue.metaCache_().get(int64(321))!.size).toBe(1)
      expect(messageQueue.poll()).toStrictEqual(new File('buffer-file3'))
      expect(messageQueue.metaCache_().get(int64(123))!.size).toBe(1)
      expect(messageQueue.metaCache_().get(int64(321))!.size).toBe(1)
      expect(messageQueue.poll()).toStrictEqual(new File('buffer-file4'))
      expect(messageQueue.metaCache_().get(int64(123))!.size).toBe(1)
      expect(messageQueue.metaCache_().get(int64(321))!.size).toBe(0)
      expect(messageQueue.poll()).toStrictEqual(new File('buffer-file2'))
      expect(messageQueue.metaCache_().get(int64(123))!.size).toBe(0)
      expect(messageQueue.metaCache_().get(int64(321))!.size).toBe(0)

      expect(messageQueue.isEmpty()).toBe(false)
      expect(messageQueue.poll()).toStrictEqual(new File('queue-file'))
      expect(messageQueue.isEmpty()).toBe(true)
    })
  })
  describe('observeHead', () => {
    it('should refill from Main Queue and load head task if buffer is non-empty', (done) => {
      const fs = MockFileSystem()
      const taskSerializer = new TaskSerializer(
        'commands',
        fs,
        MockHighPrecisionTimer(),
        MockJSONSerializer(),
        MockModels(),
      )
      const tasks = [makeTestTask(1, TaskType.delete), makeTestTask(1, TaskType.saveDraft, 1)]
      const retriableReadTaskFromFileSpy = jest
        .spyOn(taskSerializer, 'retriableReadTaskFromFile')
        .mockReturnValue(resolve(tasks[0]))
      const queue = new LinkedBlockingDeque<File>()
      const notDeletedCommandFiles = new NotDeletedCommandFiles(MockStorage())
      const messageQueue = new PatchedBufferedMessageQueue(taskSerializer, fs, queue, notDeletedCommandFiles)
      messageQueue.buffer_().push(...tasks.map((t, i) => TaskWrapper.create(t, new File(`file${i + 1}`))))
      const refillFromMainQueueSpy = jest.spyOn(messageQueue, 'refillFromMainQueue').mockReturnValue(resolve(getVoid()))
      messageQueue.observeHead().then((task) => {
        expect(refillFromMainQueueSpy).toBeCalled()
        expect(task).toStrictEqual(tasks[0])
        expect(retriableReadTaskFromFileSpy).toBeCalledWith('file1')
        done()
      })
    })
    it('should refill from Main Queue and return null if buffer is empty', (done) => {
      const fs = MockFileSystem()
      const taskSerializer = new TaskSerializer(
        'commands',
        fs,
        MockHighPrecisionTimer(),
        MockJSONSerializer(),
        MockModels(),
      )
      const retriableReadTaskFromFileSpy = jest.spyOn(taskSerializer, 'retriableReadTaskFromFile')
      const queue = new LinkedBlockingDeque<File>()
      const notDeletedCommandFiles = new NotDeletedCommandFiles(MockStorage())
      const messageQueue = new PatchedBufferedMessageQueue(taskSerializer, fs, queue, notDeletedCommandFiles)
      const refillFromMainQueueSpy = jest.spyOn(messageQueue, 'refillFromMainQueue').mockReturnValue(resolve(getVoid()))
      messageQueue.observeHead().then((task) => {
        expect(refillFromMainQueueSpy).toBeCalled()
        expect(task).toBeNull()
        expect(retriableReadTaskFromFileSpy).not.toBeCalled()
        done()
      })
    })
  })
  describe('refillFromMainQueue', () => {
    it('should return immediatelly if buffer length exceeds allowed size', (done) => {
      const fs = MockFileSystem()
      const taskSerializer = new TaskSerializer(
        'commands',
        fs,
        MockHighPrecisionTimer(),
        MockJSONSerializer(),
        MockModels(),
      )
      const queue = new LinkedBlockingDeque<File>()
      const peekSpy = jest.spyOn(queue, 'peek')

      const notDeletedCommandFiles = new NotDeletedCommandFiles(MockStorage())
      const messageQueue = new PatchedBufferedMessageQueue(taskSerializer, fs, queue, notDeletedCommandFiles)
      messageQueue.dropBuffer_()
      // tslint:disable-next-line: no-string-literal
      for (let i = 0; i < ((BufferedMessageQueue as any).maxBufferSize as number) + 1; i++) {
        messageQueue.buffer_().push(makeTestTaskWrapper(1, TaskType.delete, 'buffer-file1'))
      }
      messageQueue.refillFromMainQueue().then((_) => {
        expect(peekSpy).not.toBeCalled()
        done()
      })
    })
    it('should return immediatelly if the queue is empty', (done) => {
      const fs = MockFileSystem()
      const taskSerializer = new TaskSerializer(
        'commands',
        fs,
        MockHighPrecisionTimer(),
        MockJSONSerializer(),
        MockModels(),
      )
      const queue = new LinkedBlockingDeque<File>()
      const peekSpy = jest.spyOn(queue, 'peek')

      const notDeletedCommandFiles = new NotDeletedCommandFiles(MockStorage())
      const messageQueue = new PatchedBufferedMessageQueue(taskSerializer, fs, queue, notDeletedCommandFiles)
      messageQueue.dropBuffer_()
      messageQueue.refillFromMainQueue().then((_) => {
        expect(peekSpy).not.toBeCalled()
        done()
      })
    })
    it('should add task to buffer, removing it from the queue (on success) and continue until buffer is full or queue is empty', (done) => {
      const fs = MockFileSystem()
      const taskSerializer = new TaskSerializer(
        'commands',
        fs,
        MockHighPrecisionTimer(),
        MockJSONSerializer(),
        MockModels(),
      )
      const queue = new LinkedBlockingDeque<File>()
      queue.offer(new File('file1'))
      queue.offer(new File('file2'))

      const notDeletedCommandFiles = new NotDeletedCommandFiles(MockStorage())
      const messageQueue = new PatchedBufferedMessageQueue(taskSerializer, fs, queue, notDeletedCommandFiles)
      const addTaskToBufferSpy = jest.spyOn(messageQueue as any, 'addTaskToBuffer').mockReturnValue(resolve(getVoid()))
      const pollSpy = jest.spyOn(queue, 'poll')
      messageQueue.refillFromMainQueue().then((_) => {
        expect(addTaskToBufferSpy).toBeCalledTimes(2)
        expect(addTaskToBufferSpy).toBeCalledWith(new File('file1'))
        expect(addTaskToBufferSpy).toBeCalledWith(new File('file2'))
        expect(queue.isEmpty()).toBe(true)
        expect(pollSpy).toBeCalledTimes(2)
        addTaskToBufferSpy.mockRestore()
        pollSpy.mockRestore()
        done()
      })
    })
    it("should add task to buffer, removing it from the queue and delete it's file (on failure) and continue until buffer is full or queue is empty", (done) => {
      const fs = MockFileSystem()
      const taskSerializer = new TaskSerializer(
        'commands',
        fs,
        MockHighPrecisionTimer(),
        MockJSONSerializer(),
        MockModels(),
      )
      const queue = new LinkedBlockingDeque<File>()
      queue.offer(new File('file1'))
      queue.offer(new File('file2'))
      queue.offer(new File('file3'))

      const notDeletedCommandFiles = new NotDeletedCommandFiles(MockStorage())
      const messageQueue = new PatchedBufferedMessageQueue(taskSerializer, fs, queue, notDeletedCommandFiles)
      const addTaskToBufferSpy = jest
        .spyOn(messageQueue as any, 'addTaskToBuffer')
        .mockReturnValueOnce(resolve(getVoid()))
        .mockReturnValueOnce(rejected('FAILED'))
        .mockReturnValueOnce(resolve(getVoid()))
      const deleteTaskFileSpy = jest.spyOn(messageQueue, 'deleteTaskFile').mockReturnValue(resolve(getVoid()))
      const pollSpy = jest.spyOn(queue, 'poll')
      messageQueue.refillFromMainQueue().then((_) => {
        expect(addTaskToBufferSpy).toBeCalledTimes(3)
        expect(addTaskToBufferSpy).toBeCalledWith(new File('file1'))
        expect(addTaskToBufferSpy).toBeCalledWith(new File('file2'))
        expect(addTaskToBufferSpy).toBeCalledWith(new File('file3'))
        expect(deleteTaskFileSpy).toBeCalledWith(new File('file2'))
        expect(queue.isEmpty()).toBe(true)
        expect(pollSpy).toBeCalledTimes(3)
        addTaskToBufferSpy.mockRestore()
        deleteTaskFileSpy.mockRestore()
        pollSpy.mockRestore()
        done()
      })
    })
  })
  describe('refillFromDelayed', () => {
    it('should move non-send and absent wrappers from argument into the buffer, deleting duplicates', (done) => {
      const fs = MockFileSystem()
      const taskSerializer = new TaskSerializer(
        'commands',
        fs,
        MockHighPrecisionTimer(),
        MockJSONSerializer(),
        MockModels(),
      )
      const queue = new LinkedBlockingDeque<File>()
      const notDeletedCommandFiles = new NotDeletedCommandFiles(MockStorage())
      const messageQueue = new PatchedBufferedMessageQueue(taskSerializer, fs, queue, notDeletedCommandFiles)
      const wrappers = [
        makeTestTaskWrapper(1, TaskType.delete, 'file1'),
        makeTestTaskWrapper(2, TaskType.delete, 'file2'),
        makeTestTaskWrapper(3, TaskType.saveDraft, 'file3', 1),
        makeTestTaskWrapper(3, TaskType.saveDraft, 'file4', 2),
        makeTestTaskWrapper(3, TaskType.saveDraft, 'file5', 2),
        makeTestTaskWrapper(4, TaskType.saveDraft, 'file6', 3),
        makeTestTaskWrapper(5, TaskType.sendMessage, 'file7', 4),
        makeTestTaskWrapper(5, TaskType.sendMessage, 'file8', 4),
        makeTestTaskWrapper(5, TaskType.sendMessage, 'file9', 5),
        makeTestTaskWrapper(6, TaskType.sendMessage, 'file10', 6),
      ]
      const wrappersCopy = [...wrappers]

      const deleteTaskFileSpy = jest.spyOn(messageQueue, 'deleteTaskFile').mockReturnValue(resolve(getVoid()))
      const pollSpy = jest.spyOn(queue, 'poll')
      messageQueue.refillFromDelayed(wrappers).then((_) => {
        expect(deleteTaskFileSpy).toBeCalledTimes(2)
        expect(deleteTaskFileSpy).toBeCalledWith(wrappersCopy[3].file)
        expect(deleteTaskFileSpy).toBeCalledWith(wrappersCopy[6].file)
        expect(queue.isEmpty()).toBe(true)
        expect(messageQueue.metaCache_()).toStrictEqual(
          new Map([
            [int64(3), new Set([int64(1), int64(2)])],
            [int64(4), new Set([int64(3)])],
            [int64(5), new Set([int64(4), int64(5)])],
            [int64(6), new Set([int64(6)])],
          ]),
        )
        expect(messageQueue.buffer_()).toStrictEqual([
          wrappersCopy[0],
          wrappersCopy[1],
          wrappersCopy[2],
          wrappersCopy[4],
          wrappersCopy[5],
          wrappersCopy[7],
          wrappersCopy[8],
          wrappersCopy[9],
        ])
        deleteTaskFileSpy.mockRestore()
        pollSpy.mockRestore()
        done()
      })
    })
  })
  // Yes, guilty as charged. My bad, but the refactoring to extra classes is rather expensive.
  describe('private methods', () => {
    afterEach(jest.restoreAllMocks)
    describe('addTaskToBuffer', () => {
      it('should return immediatelly if the argument is null', (done) => {
        const fs = MockFileSystem()
        const taskSerializer = new TaskSerializer(
          'commands',
          fs,
          MockHighPrecisionTimer(),
          MockJSONSerializer(),
          MockModels(),
        )
        const queue = new LinkedBlockingDeque<File>()
        const notDeletedCommandFiles = new NotDeletedCommandFiles(MockStorage())
        const messageQueue = new PatchedBufferedMessageQueue(taskSerializer, fs, queue, notDeletedCommandFiles)
        messageQueue.addTaskToBuffer_(null).then((_) => {
          expect(fs.exists).not.toBeCalled()
          done()
        })
      })
      it('should return immediatelly if the passed file does not exist', (done) => {
        const fs = MockFileSystem({
          exists: jest.fn().mockReturnValue(resolve(false)),
        })
        const taskSerializer = new TaskSerializer(
          'commands',
          fs,
          MockHighPrecisionTimer(),
          MockJSONSerializer(),
          MockModels(),
        )
        const retriableReadTaskFromFileSpy = jest.spyOn(taskSerializer, 'retriableReadTaskFromFile')
        const queue = new LinkedBlockingDeque<File>()
        const notDeletedCommandFiles = new NotDeletedCommandFiles(MockStorage())
        const messageQueue = new PatchedBufferedMessageQueue(taskSerializer, fs, queue, notDeletedCommandFiles)
        messageQueue.addTaskToBuffer_(new File('file1')).then((_) => {
          expect(fs.exists).toBeCalledWith('file1')
          expect(retriableReadTaskFromFileSpy).not.toBeCalled()
          done()
        })
      })
      it("should add file to buffer if it's not of Store/Send type", (done) => {
        const fs = MockFileSystem({
          exists: jest.fn().mockReturnValue(resolve(true)),
        })
        const taskSerializer = new TaskSerializer(
          'commands',
          fs,
          MockHighPrecisionTimer(),
          MockJSONSerializer(),
          MockModels(),
        )
        const task = makeTestTask(1, TaskType.delete)
        const retriableReadTaskFromFileSpy = jest
          .spyOn(taskSerializer, 'retriableReadTaskFromFile')
          .mockReturnValue(resolve(task))
        const queue = new LinkedBlockingDeque<File>()
        const notDeletedCommandFiles = new NotDeletedCommandFiles(MockStorage())
        const messageQueue = new PatchedBufferedMessageQueue(taskSerializer, fs, queue, notDeletedCommandFiles)
        const filterOutPreviousDraftTasksFromBufferSpy = jest.spyOn(
          messageQueue as any,
          'filterOutPreviousDraftTasksFromBuffer',
        )
        messageQueue.addTaskToBuffer_(new File('file1')).then((_) => {
          expect(fs.exists).toBeCalledWith('file1')
          expect(retriableReadTaskFromFileSpy).toBeCalledWith('file1')
          expect(filterOutPreviousDraftTasksFromBufferSpy).not.toBeCalled()
          expect(messageQueue.isEmpty()).toBe(false)
          expect(messageQueue.poll()!).toStrictEqual(new File('file1'))
          done()
        })
      })
      it("should add file to buffer (removing copies) and to cache if it's of Store/Send type", (done) => {
        const fs = MockFileSystem({
          exists: jest.fn().mockReturnValue(resolve(true)),
        })
        const taskSerializer = new TaskSerializer(
          'commands',
          fs,
          MockHighPrecisionTimer(),
          MockJSONSerializer(),
          MockModels(),
        )
        const task = makeTestTask(1, TaskType.saveDraft, 1)
        const retriableReadTaskFromFileSpy = jest
          .spyOn(taskSerializer, 'retriableReadTaskFromFile')
          .mockReturnValue(resolve(task))
        const queue = new LinkedBlockingDeque<File>()
        const notDeletedCommandFiles = new NotDeletedCommandFiles(MockStorage())
        const messageQueue = new PatchedBufferedMessageQueue(taskSerializer, fs, queue, notDeletedCommandFiles)
        const filterOutPreviousDraftTasksFromBufferSpy = jest
          .spyOn(messageQueue as any, 'filterOutPreviousDraftTasksFromBuffer')
          .mockReturnValue(resolve(getVoid()))
        messageQueue.addTaskToBuffer_(new File('file1')).then((_) => {
          expect(fs.exists).toBeCalledWith('file1')
          expect(retriableReadTaskFromFileSpy).toBeCalledWith('file1')
          expect(filterOutPreviousDraftTasksFromBufferSpy).toBeCalledTimes(1)
          const wrapper = TaskWrapper.create(task, new File('file1'))
          expect(filterOutPreviousDraftTasksFromBufferSpy).toBeCalledWith(wrapper)
          expect(messageQueue.isEmpty()).toBe(false)
          // Check metaCache before polling: poll changes metaCache
          expect(messageQueue.metaCache_()).toStrictEqual(new Map([[int64(1), new Set([int64(1)])]]))
          expect(messageQueue.poll()!).toStrictEqual(wrapper.file)
          done()
        })
      })
    })
    describe('filterOutPreviousDraftTasksFromBuffer', () => {
      afterEach(jest.restoreAllMocks)
      it('should reject if a task to add is not of Store/Send type', (done) => {
        const fs = MockFileSystem()
        const taskSerializer = new TaskSerializer(
          'commands',
          fs,
          MockHighPrecisionTimer(),
          MockJSONSerializer(),
          MockModels(),
        )
        const task = makeTestTaskWrapper(1, TaskType.delete, 'file1')
        const queue = new LinkedBlockingDeque<File>()
        const notDeletedCommandFiles = new NotDeletedCommandFiles(MockStorage())
        const messageQueue = new PatchedBufferedMessageQueue(taskSerializer, fs, queue, notDeletedCommandFiles)
        messageQueue.filterOutPreviousDraftTasksFromBuffer_(task).failed((err) => {
          expect(err.message).toBe('Error trying to filter out non store/send tasks')
          done()
        })
      })
      it('should return immediatelly if the task is not present in cache', (done) => {
        const fs = MockFileSystem({
          delete: jest.fn(),
        })
        const taskSerializer = new TaskSerializer(
          'commands',
          fs,
          MockHighPrecisionTimer(),
          MockJSONSerializer(),
          MockModels(),
        )
        const task = makeTestTaskWrapper(1, TaskType.saveDraft, 'file1')
        const queue = new LinkedBlockingDeque<File>()
        const notDeletedCommandFiles = new NotDeletedCommandFiles(MockStorage())
        const messageQueue = new PatchedBufferedMessageQueue(taskSerializer, fs, queue, notDeletedCommandFiles)
        const deleteTaskFileSpy = jest.spyOn(messageQueue, 'deleteTaskFile')
        messageQueue.metaCache_().set(int64(1), new Set([int64(1)]))
        messageQueue.filterOutPreviousDraftTasksFromBuffer_(task).then((_) => {
          expect(deleteTaskFileSpy).not.toBeCalled()
          done()
        })
      })
      it('should delete buffered tasks with the same uid and draftID', (done) => {
        const fs = MockFileSystem()
        const taskSerializer = new TaskSerializer(
          'commands',
          fs,
          MockHighPrecisionTimer(),
          MockJSONSerializer(),
          MockModels(),
        )
        const bufferedTasks = [
          makeTestTaskWrapper(1, TaskType.saveDraft, 'file1', 1),
          makeTestTaskWrapper(1, TaskType.saveDraft, 'file2', 1),
          makeTestTaskWrapper(1, TaskType.saveDraft, 'file3', 2),
          makeTestTaskWrapper(2, TaskType.saveDraft, 'file4', 1),
          makeTestTaskWrapper(1, TaskType.delete, 'file5', 1),
        ]
        const queue = new LinkedBlockingDeque<File>()
        const notDeletedCommandFiles = new NotDeletedCommandFiles(MockStorage())
        const messageQueue = new PatchedBufferedMessageQueue(taskSerializer, fs, queue, notDeletedCommandFiles)
        messageQueue.buffer_().push(...bufferedTasks)
        const task = makeTestTaskWrapper(1, TaskType.saveDraft, 'file6', 1)
        const deleteTaskFileSpy = jest
          .spyOn(messageQueue, 'deleteTaskFile')
          .mockReturnValueOnce(resolve(getVoid()))
          .mockReturnValueOnce(rejected('FAILED'))
          .mockReturnValue(resolve(getVoid()))
        messageQueue
          .metaCache_()
          .set(int64(1), new Set([int64(1), int64(2)]))
          .set(int64(2), new Set([int64(1)]))
        messageQueue.filterOutPreviousDraftTasksFromBuffer_(task).then((_) => {
          expect(deleteTaskFileSpy).toBeCalledTimes(2)
          expect(deleteTaskFileSpy).toBeCalledWith(new File('file1'))
          expect(deleteTaskFileSpy).toBeCalledWith(new File('file2'))
          expect(messageQueue.buffer_()).toStrictEqual([bufferedTasks[2], bufferedTasks[3], bufferedTasks[4]])
          expect(messageQueue.metaCache_()).toStrictEqual(
            new Map([
              [int64(1), new Set([int64(2)])],
              [int64(2), new Set([int64(1)])],
            ]),
          )
          done()
        })
      })
    })
    describe('removeFromCache', () => {
      it('should remove task by UID and DraftID from metaCache', () => {
        const fs = MockFileSystem()
        const taskSerializer = new TaskSerializer(
          'commands',
          fs,
          MockHighPrecisionTimer(),
          MockJSONSerializer(),
          MockModels(),
        )
        const queue = new LinkedBlockingDeque<File>()
        const notDeletedCommandFiles = new NotDeletedCommandFiles(MockStorage())
        const messageQueue = new PatchedBufferedMessageQueue(taskSerializer, fs, queue, notDeletedCommandFiles)
        messageQueue
          .metaCache_()
          .set(int64(1), new Set([int64(1), int64(2)]))
          .set(int64(2), new Set([int64(1)]))
        messageQueue.removeFromCache_(makeTestTaskWrapper(3, TaskType.sendMessage, 'file1', 1))
        expect(messageQueue.metaCache_().size).toBe(2)
        expect(messageQueue.metaCache_().get(int64(1))).toStrictEqual(new Set([int64(1), int64(2)]))
        expect(messageQueue.metaCache_().get(int64(2))).toStrictEqual(new Set([int64(1)]))

        messageQueue.removeFromCache_(makeTestTaskWrapper(2, TaskType.sendMessage, 'file1', 2))
        expect(messageQueue.metaCache_().size).toBe(2)
        expect(messageQueue.metaCache_().get(int64(1))).toStrictEqual(new Set([int64(1), int64(2)]))
        expect(messageQueue.metaCache_().get(int64(2))).toStrictEqual(new Set([int64(1)]))

        messageQueue.removeFromCache_(makeTestTaskWrapper(1, TaskType.sendMessage, 'file1', 1))
        expect(messageQueue.metaCache_().size).toBe(2)
        expect(messageQueue.metaCache_().get(int64(1))).toStrictEqual(new Set([int64(2)]))
        expect(messageQueue.metaCache_().get(int64(2))).toStrictEqual(new Set([int64(1)]))
      })
    })
    describe('containsStoreSend', () => {
      it('should check if metaCache contains a task by UID and DraftID', () => {
        const fs = MockFileSystem()
        const taskSerializer = new TaskSerializer(
          'commands',
          fs,
          MockHighPrecisionTimer(),
          MockJSONSerializer(),
          MockModels(),
        )
        const queue = new LinkedBlockingDeque<File>()
        const notDeletedCommandFiles = new NotDeletedCommandFiles(MockStorage())
        const messageQueue = new PatchedBufferedMessageQueue(taskSerializer, fs, queue, notDeletedCommandFiles)
        messageQueue.metaCache_().set(int64(1), new Set([int64(1)]))
        expect(messageQueue.containsStoreSend_(makeTestTaskWrapper(2, TaskType.sendMessage, 'file1', 1))).toBe(false)
        expect(messageQueue.containsStoreSend_(makeTestTaskWrapper(1, TaskType.sendMessage, 'file1', 2))).toBe(false)
        expect(messageQueue.containsStoreSend_(makeTestTaskWrapper(1, TaskType.sendMessage, 'file1', 1))).toBe(true)
      })
    })
    describe('storeInCache', () => {
      it('should store DraftID by UID', () => {
        const fs = MockFileSystem()
        const taskSerializer = new TaskSerializer(
          'commands',
          fs,
          MockHighPrecisionTimer(),
          MockJSONSerializer(),
          MockModels(),
        )
        const queue = new LinkedBlockingDeque<File>()
        const notDeletedCommandFiles = new NotDeletedCommandFiles(MockStorage())
        const messageQueue = new PatchedBufferedMessageQueue(taskSerializer, fs, queue, notDeletedCommandFiles)
        expect(messageQueue.metaCache_().size).toBe(0)
        messageQueue.storeInCache_(makeTestTaskWrapper(1, TaskType.sendMessage, 'file1', 1))
        expect(messageQueue.metaCache_()).toStrictEqual(new Map([[int64(1), new Set([int64(1)])]]))
        messageQueue.storeInCache_(makeTestTaskWrapper(1, TaskType.sendMessage, 'file1', 2))
        expect(messageQueue.metaCache_()).toStrictEqual(new Map([[int64(1), new Set([int64(1), int64(2)])]]))
        messageQueue.storeInCache_(makeTestTaskWrapper(2, TaskType.sendMessage, 'file1', 1))
        expect(messageQueue.metaCache_()).toStrictEqual(
          new Map([
            [int64(1), new Set([int64(1), int64(2)])],
            [int64(2), new Set([int64(1)])],
          ]),
        )
      })
    })
  })
})
