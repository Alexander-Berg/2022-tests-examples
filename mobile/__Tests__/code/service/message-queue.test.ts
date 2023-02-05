import { resolve, take } from '../../../../../common/xpromise-support'
import { int64 } from '../../../../../common/ys'
import { MockFileSystem, MockJSONSerializer } from '../../../../common/__tests__/__helpers__/mock-patches'
import { XPromise } from '../../../../common/code/promise/xpromise'
import { getVoid } from '../../../../common/code/result/result'
import { File } from '../../../../common/code/file-system/file'
import { FileSystem } from '../../../../common/code/file-system/file-system'
import { NotDeletedCommandFiles } from '../../../code/busilogics/commands/not-deleted-command-files'
import { LinkedBlockingDeque } from '../../../code/service/linked-blocking-deque'
import { MessageQueue } from '../../../code/service/message-queue'
import { TaskSerializer } from '../../../code/service/task-serializer'
import { TaskWrapper } from '../../../code/service/task-wrapper'
import { MockHighPrecisionTimer, MockStorage } from '../../__helpers__/mock-patches'
import { MockModels } from '../../__helpers__/models'
import { rejected } from '../../__helpers__/test-failure'
import {
  registerLinkedBlockingDeque,
  TestLinkedBlockingDeque,
  unregisterLinkedBlockingDeque,
} from '../../__helpers__/test-linked-blocking-queue'
import { TestTask } from '../../__helpers__/test-task'

function makeMessageQueue(
  files: readonly string[],
  dbEntries: readonly string[],
  taskSerializer?: TaskSerializer,
  notDeletedCommandFiles?: NotDeletedCommandFiles,
  fileSystem?: FileSystem,
): XPromise<MessageQueue> {
  const fs =
    fileSystem ??
    MockFileSystem({
      listDirectory: jest.fn().mockReturnValue(resolve(files)),
      delete: jest.fn().mockReturnValue(resolve(getVoid())),
      ensureFolderExists: jest.fn().mockReturnValue(resolve(getVoid())),
    })
  const notDelComFiles = notDeletedCommandFiles ?? new NotDeletedCommandFiles(MockStorage())
  jest.spyOn(notDelComFiles, 'fetchAll').mockReturnValue(resolve(dbEntries))
  jest.spyOn(notDelComFiles, 'delete').mockReturnValue(resolve(getVoid()))
  // tslint:disable-next-line: max-line-length
  const ts =
    taskSerializer ??
    new TaskSerializer(
      'commands',
      fs,
      MockHighPrecisionTimer(() => int64(12345)),
      MockJSONSerializer(),
      MockModels(),
    )
  return MessageQueue.create('commands', fs, ts, notDelComFiles)
}

describe(MessageQueue, () => {
  beforeAll(registerLinkedBlockingDeque)
  afterEach(unregisterLinkedBlockingDeque)

  describe(MessageQueue.create, () => {
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
      MessageQueue.create('commands', fs, taskSerializer, notDeletedCommandFiles).then((res) => {
        expect(res).toBeInstanceOf(MessageQueue)
        expect(res).not.toBeNull()
        expect(((res as any).queue.deque as TestLinkedBlockingDeque)._items).toStrictEqual([new File('file1')])
        fetchAllSpy.mockRestore()
        deleteSpy.mockRestore()
        done()
      })
    })
  })
  describe('clear', () => {
    it('should clear the queue', async () => {
      const queue = await take(makeMessageQueue(['file1', 'file2', 'file3'], ['file3', 'file4']))
      expect(queue.isEmpty()).toBe(false)
      queue.clear()
      expect(queue.isEmpty()).toBe(true)
    })
  })
  describe('isEmpty', () => {
    it('should return true if the queue is empty', async () => {
      const queue = await take(makeMessageQueue(['file1', 'file2', 'file3'], ['file1', 'file2', 'file3']))
      expect(queue.isEmpty()).toBe(true)
    })
    it('should return false if the queue is not empty', async () => {
      const queue = await take(makeMessageQueue(['file1', 'file2', 'file3'], ['file1', 'file2']))
      expect(queue.isEmpty()).toBe(false)
    })
  })
  describe('offer', () => {
    it("should serialize the task and add to queue's head", async () => {
      const taskSerializer = new TaskSerializer(
        'commands',
        MockFileSystem(),
        MockHighPrecisionTimer(),
        MockJSONSerializer(),
        MockModels(),
      )
      const serializeSpy = jest.spyOn(taskSerializer, 'serialize').mockReturnValue(resolve(new File('file2')))
      const queue = await take(makeMessageQueue(['file1'], [], taskSerializer))
      await take(queue.offer(new TestTask(1, int64(1234), MockModels())))
      expect(((queue as any).queue.deque as TestLinkedBlockingDeque)._items).toStrictEqual([
        new File('file1'),
        new File('file2'),
      ])
      serializeSpy.mockRestore()
    })
  })
  describe('deleteTaskFile', () => {
    it('should do nothing if the argument is null', async () => {
      const deleteMock = jest.fn().mockReturnValue(resolve(getVoid()))
      const taskSerializer = new TaskSerializer(
        'commands',
        MockFileSystem({
          delete: deleteMock,
        }),
        MockHighPrecisionTimer(),
        MockJSONSerializer(),
        MockModels(),
      )
      const queue = await take(makeMessageQueue(['file1'], [], taskSerializer))
      await take(queue.deleteTaskFile(null))
      expect(deleteMock).not.toBeCalled()
      deleteMock.mockRestore()
    })
    it('should delete the file and, if error, put it the filename in the table', async () => {
      const deleteMock = jest.fn().mockReturnValue(rejected('FAILED'))
      const fs = MockFileSystem({
        listDirectory: jest.fn().mockReturnValue(resolve(['file1'])),
        delete: deleteMock,
        ensureFolderExists: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const taskSerializer = new TaskSerializer(
        'commands',
        fs,
        MockHighPrecisionTimer(),
        MockJSONSerializer(),
        MockModels(),
      )
      const notDeletedCommandFiles = new NotDeletedCommandFiles(MockStorage())
      const storeSpy = jest.spyOn(notDeletedCommandFiles, 'store').mockReturnValue(resolve(getVoid()))
      const queue = await take(makeMessageQueue(['file1'], [], taskSerializer, notDeletedCommandFiles, fs))
      await take(queue.deleteTaskFile(new File('file1')))
      expect(deleteMock).toBeCalledWith('file1', true)
      expect(storeSpy).toBeCalledWith('file1')
      deleteMock.mockRestore()
      storeSpy.mockRestore()
    })
    it('should delete the file and, if no error, do not put it the filename in the table', async () => {
      const deleteMock = jest.fn().mockReturnValue(resolve(getVoid()))
      const fs = MockFileSystem({
        listDirectory: jest.fn().mockReturnValue(resolve(['file1'])),
        delete: deleteMock,
        ensureFolderExists: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const taskSerializer = new TaskSerializer(
        'commands',
        fs,
        MockHighPrecisionTimer(),
        MockJSONSerializer(),
        MockModels(),
      )
      const notDeletedCommandFiles = new NotDeletedCommandFiles(MockStorage())
      const storeSpy = jest.spyOn(notDeletedCommandFiles, 'store').mockReturnValue(resolve(getVoid()))
      const queue = await take(makeMessageQueue(['file1'], [], taskSerializer, notDeletedCommandFiles, fs))
      await take(queue.deleteTaskFile(new File('file1')))
      expect(fs.delete).toBeCalledWith('file1', true)
      expect(storeSpy).not.toBeCalled()
      deleteMock.mockRestore()
      storeSpy.mockRestore()
    })
  })
  describe('removeHead', () => {
    it('should do nothing if the argument is null', (done) => {
      const fs = MockFileSystem({
        delete: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const notDeletedCommandFiles = new NotDeletedCommandFiles(MockStorage())
      const taskSerializer = new TaskSerializer(
        'commands',
        fs,
        MockHighPrecisionTimer(),
        MockJSONSerializer(),
        MockModels(),
      )
      const deque = new LinkedBlockingDeque<File>()
      const queue = new MessageQueue(taskSerializer, fs, deque, notDeletedCommandFiles)
      queue.removeHead().then((_) => {
        expect(fs.delete).not.toBeCalled()
        done()
      })
    })
    it('should delete the file and, if error, put it the filename in the table', (done) => {
      const fs = MockFileSystem({
        delete: jest.fn().mockReturnValue(rejected('FAILED')),
      })
      const notDeletedCommandFiles = new NotDeletedCommandFiles(MockStorage())
      const storeSpy = jest.spyOn(notDeletedCommandFiles, 'store').mockReturnValue(resolve(getVoid()))
      const taskSerializer = new TaskSerializer(
        'commands',
        fs,
        MockHighPrecisionTimer(),
        MockJSONSerializer(),
        MockModels(),
      )
      const deque = new LinkedBlockingDeque<File>()
      deque.offerFirst(new File('file1'))
      const queue = new MessageQueue(taskSerializer, fs, deque, notDeletedCommandFiles)
      queue.removeHead().then((_) => {
        expect(fs.delete).toBeCalledWith('file1', true)
        expect(storeSpy).toBeCalledWith('file1')
        storeSpy.mockRestore()
        done()
      })
    })
    it('should delete the file and, if no error, do not put it the filename in the table', (done) => {
      const fs = MockFileSystem({
        delete: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const notDeletedCommandFiles = new NotDeletedCommandFiles(MockStorage())
      const storeSpy = jest.spyOn(notDeletedCommandFiles, 'store').mockReturnValue(resolve(getVoid()))
      const taskSerializer = new TaskSerializer(
        'commands',
        fs,
        MockHighPrecisionTimer(),
        MockJSONSerializer(),
        MockModels(),
      )
      const deque = new LinkedBlockingDeque<File>()
      deque.offerFirst(new File('file1'))
      const queue = new MessageQueue(taskSerializer, fs, deque, notDeletedCommandFiles)
      queue.removeHead().then((_) => {
        expect(fs.delete).toBeCalledWith('file1', true)
        expect(storeSpy).not.toBeCalled()
        storeSpy.mockRestore()
        done()
      })
    })
  })
  describe('refillFromDelayed', () => {
    it('should prepend the queue with arguments', (done) => {
      const fs = MockFileSystem({
        delete: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const notDeletedCommandFiles = new NotDeletedCommandFiles(MockStorage())
      const taskSerializer = new TaskSerializer(
        'commands',
        fs,
        MockHighPrecisionTimer(),
        MockJSONSerializer(),
        MockModels(),
      )
      const deque = new LinkedBlockingDeque<File>()
      deque.offerFirst(new File('file4'))
      deque.offerFirst(new File('file3'))
      const queue = new MessageQueue(taskSerializer, fs, deque, notDeletedCommandFiles)
      queue
        .refillFromDelayed([
          TaskWrapper.create(new TestTask(1, int64(1), MockModels()), new File('file1')),
          TaskWrapper.create(new TestTask(2, int64(2), MockModels()), new File('file2')),
        ])
        .then((_) => {
          expect(deque.poll()).toStrictEqual(new File('file1'))
          expect(deque.poll()).toStrictEqual(new File('file2'))
          expect(deque.poll()).toStrictEqual(new File('file3'))
          expect(deque.poll()).toStrictEqual(new File('file4'))
          done()
        })
    })
    it('should delete the file and, if error, put it the filename in the table', (done) => {
      const fs = MockFileSystem({
        delete: jest.fn().mockReturnValue(rejected('FAILED')),
      })
      const notDeletedCommandFiles = new NotDeletedCommandFiles(MockStorage())
      const storeSpy = jest.spyOn(notDeletedCommandFiles, 'store').mockReturnValue(resolve(getVoid()))
      const taskSerializer = new TaskSerializer(
        'commands',
        fs,
        MockHighPrecisionTimer(),
        MockJSONSerializer(),
        MockModels(),
      )
      const deque = new LinkedBlockingDeque<File>()
      deque.offerFirst(new File('file1'))
      const queue = new MessageQueue(taskSerializer, fs, deque, notDeletedCommandFiles)
      queue.removeHead().then((_) => {
        expect(fs.delete).toBeCalledWith('file1', true)
        expect(storeSpy).toBeCalledWith('file1')
        storeSpy.mockRestore()
        done()
      })
    })
    it('should delete the file and, if no error, do not put it the filename in the table', (done) => {
      const fs = MockFileSystem({
        delete: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const notDeletedCommandFiles = new NotDeletedCommandFiles(MockStorage())
      const storeSpy = jest.spyOn(notDeletedCommandFiles, 'store').mockReturnValue(resolve(getVoid()))
      const taskSerializer = new TaskSerializer(
        'commands',
        fs,
        MockHighPrecisionTimer(),
        MockJSONSerializer(),
        MockModels(),
      )
      const deque = new LinkedBlockingDeque<File>()
      deque.offerFirst(new File('file1'))
      const queue = new MessageQueue(taskSerializer, fs, deque, notDeletedCommandFiles)
      queue.removeHead().then((_) => {
        expect(fs.delete).toBeCalledWith('file1', true)
        expect(storeSpy).not.toBeCalled()
        storeSpy.mockRestore()
        done()
      })
    })
  })
  describe('poll', () => {
    it('should return the head of deque, if not empty', () => {
      const fs = MockFileSystem()
      const notDeletedCommandFiles = new NotDeletedCommandFiles(MockStorage())
      const taskSerializer = new TaskSerializer(
        'commands',
        fs,
        MockHighPrecisionTimer(),
        MockJSONSerializer(),
        MockModels(),
      )
      const deque = new LinkedBlockingDeque<File>()
      deque.offerFirst(new File('file1'))
      deque.offerFirst(new File('file2'))
      const queue = new MessageQueue(taskSerializer, fs, deque, notDeletedCommandFiles)
      expect(queue.poll()).toStrictEqual(new File('file2'))
      expect(queue.poll()).toStrictEqual(new File('file1'))
      expect(queue.poll()).toBeNull()
    })
  })
  describe('observeHead', () => {
    it('should return null if the queue is empty', (done) => {
      const fs = MockFileSystem()
      const notDeletedCommandFiles = new NotDeletedCommandFiles(MockStorage())
      const taskSerializer = new TaskSerializer(
        'commands',
        fs,
        MockHighPrecisionTimer(),
        MockJSONSerializer(),
        MockModels(),
      )
      const deque = new LinkedBlockingDeque<File>()
      const queue = new MessageQueue(taskSerializer, fs, deque, notDeletedCommandFiles)
      queue.observeHead().then((res) => {
        expect(res).toBeNull()
        done()
      })
    })
    it('should return immediatelly if the queue is not empty and the file on top does not exist', (done) => {
      const fs = MockFileSystem({
        exists: jest.fn().mockReturnValue(resolve(false)),
      })
      const notDeletedCommandFiles = new NotDeletedCommandFiles(MockStorage())
      const taskSerializer = new TaskSerializer(
        'commands',
        fs,
        MockHighPrecisionTimer(),
        MockJSONSerializer(),
        MockModels(),
      )
      const deque = new LinkedBlockingDeque<File>()
      deque.offerFirst(new File('file1'))
      const queue = new MessageQueue(taskSerializer, fs, deque, notDeletedCommandFiles)
      queue.observeHead().then((res) => {
        expect(res).toBeNull()
        done()
      })
    })
    it('should try to load the file if the queue is not empty and the file on top exists', (done) => {
      const models = MockModels()
      const fs = MockFileSystem({
        exists: jest.fn().mockReturnValue(resolve(true)),
      })
      const notDeletedCommandFiles = new NotDeletedCommandFiles(MockStorage())
      const taskSerializer = new TaskSerializer('commands', fs, MockHighPrecisionTimer(), MockJSONSerializer(), models)
      const retriableReadTaskFromFileSpy = jest
        .spyOn(taskSerializer, 'retriableReadTaskFromFile')
        .mockReturnValue(resolve(new TestTask(2, int64(12345), models)))
      const deque = new LinkedBlockingDeque<File>()
      deque.offerFirst(new File('file1'))
      const queue = new MessageQueue(taskSerializer, fs, deque, notDeletedCommandFiles)
      queue.observeHead().then((res) => {
        expect(res).toStrictEqual(new TestTask(2, int64(12345), models))
        expect(retriableReadTaskFromFileSpy).toBeCalledWith('file1')
        retriableReadTaskFromFileSpy.mockRestore()
        done()
      })
    })
    it('should fail loading from the top of the queue fails', (done) => {
      const fs = MockFileSystem({
        exists: jest.fn().mockReturnValue(resolve(true)),
      })
      const notDeletedCommandFiles = new NotDeletedCommandFiles(MockStorage())
      const taskSerializer = new TaskSerializer(
        'commands',
        fs,
        MockHighPrecisionTimer(),
        MockJSONSerializer(),
        MockModels(),
      )
      const retriableReadTaskFromFileSpy = jest
        .spyOn(taskSerializer, 'retriableReadTaskFromFile')
        .mockReturnValue(rejected('FAILED'))
      const deque = new LinkedBlockingDeque<File>()
      deque.offerFirst(new File('file1'))
      const queue = new MessageQueue(taskSerializer, fs, deque, notDeletedCommandFiles)
      queue.observeHead().failed((err) => {
        expect(err.message).toBe('FAILED')
        expect(retriableReadTaskFromFileSpy).toBeCalledWith('file1')
        retriableReadTaskFromFileSpy.mockRestore()
        done()
      })
    })
  })
})
