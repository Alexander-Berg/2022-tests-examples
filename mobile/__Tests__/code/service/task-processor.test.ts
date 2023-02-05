import { reject, resolve } from '../../../../../common/xpromise-support'
import { arrayToSet, Int32, int64, Int64, Nullable, undefinedToNull, YSError } from '../../../../../common/ys'
import { MockFileSystem, MockJSONSerializer, mockLogger } from '../../../../common/__tests__/__helpers__/mock-patches'
import { XPromise } from '../../../../common/code/promise/xpromise'
import { getVoid } from '../../../../common/code/result/result'
import { JSONItemFromJSON } from '../../../../common/__tests__/__helpers__/json-helpers'
import { NetworkStatus, NetworkStatusCode } from '../../../../mapi/../mapi/code/api/entities/status/network-status'
import { Log } from '../../../../common/code/logging/logger'
import { File } from '../../../../common/code/file-system/file'
import { NotDeletedCommandFiles } from '../../../code/busilogics/commands/not-deleted-command-files'
import { Registry } from '../../../code/registry'
import { DelayedTasks } from '../../../code/service/delayed-tasks'
import { ILinkedBlockingDeque, LinkedBlockingDeque } from '../../../code/service/linked-blocking-deque'
import { MessageQueue } from '../../../code/service/message-queue'
import { Task, TaskType } from '../../../code/service/task'
import { TaskProcessor } from '../../../code/service/task-processor'
import { TaskSerializer } from '../../../code/service/task-serializer'
import { ServiceLocatorItems } from '../../../code/utils/service-locator'
import { ISleeper } from '../../../code/utils/sleeper'
import { MockHighPrecisionTimer, MockStorage } from '../../__helpers__/mock-patches'
import { MockModels } from '../../__helpers__/models'
import { rejected } from '../../__helpers__/test-failure'
import { TestTask } from '../../__helpers__/test-task'

type TestLinkedBlockingDeque = ILinkedBlockingDeque & { _items: File[] }

async function provideTestTask(uidValue: Int64, taskType: TaskType): Promise<Nullable<Task>> {
  return await TestTask.fromJSONItem(
    JSONItemFromJSON({
      taskType: taskType.valueOf(),
      version: 1,
      uid: uidValue,
    }),
    MockModels(),
  )
}

function provideMessageQueue(): XPromise<MessageQueue> {
  const serializer = MockJSONSerializer()
  const fs = MockFileSystem({
    listDirectory: jest.fn().mockReturnValue(resolve(['1111.txt', '22222'])),
    delete: jest.fn().mockReturnValue(resolve(getVoid())),
    ensureFolderExists: jest.fn().mockReturnValue(resolve(getVoid())),
  })
  const storage = MockStorage()
  const notDeletedCommandFiles = new NotDeletedCommandFiles(storage)
  notDeletedCommandFiles.fetchAll = jest.fn().mockReturnValue(resolve(['1111.txt', '22222.txt']))
  notDeletedCommandFiles.delete = jest.fn().mockReturnValue(resolve(getVoid()))
  const taskSerializer = new TaskSerializer(
    'commands',
    fs,
    MockHighPrecisionTimer(() => int64(12345)),
    serializer,
    MockModels(),
  )
  return MessageQueue.create('commands', fs, taskSerializer, notDeletedCommandFiles)
}

function provideDelayedTasks(frozenAccounts: Int64[]): DelayedTasks {
  const delayedTasks = new DelayedTasks(jest.fn())
  delayedTasks.setAuthorizedUids(new Set([int64(123), int64(234), int64(678), int64(12345)]))
  delayedTasks.getFrozenAccounts = jest.fn().mockReturnValue(arrayToSet(frozenAccounts))
  return delayedTasks
}

class TaskProcessorImpl extends TaskProcessor {
  public constructor(localCommandsQueue: MessageQueue, delayedTasks: DelayedTasks) {
    super(localCommandsQueue, delayedTasks, sleeper)
  }

  public onQueueEmpty(): void {
    // do nothing
  }
  public onError(task: Task, err: YSError): void {
    // do nothing
  }
  public delayCurrentCommand(task: Task): void {
    super.delayCurrentCommand(task)
  }
  public currentRetriesCount(): Int32 {
    return super.currentRetriesCount()
  }
  public incrementRetries(): void {
    super.incrementRetries()
  }
  public resetRetries(): void {
    super.resetRetries()
  }
  public deleteCurrentCommand(): XPromise<void> {
    return super.deleteCurrentCommand()
  }
  public executeFirstScheduledTaskInner(): XPromise<Nullable<Task>> {
    return super.executeFirstScheduledTaskInner()
  }
}

const sleeper: ISleeper = {
  sleep: jest.fn().mockReturnValue(resolve(getVoid())),
}

describe(TaskProcessor, () => {
  beforeAll(() => {
    const deque: TestLinkedBlockingDeque = {
      _items: [],
      contains: jest.fn().mockImplementation((item, isEqual) => Boolean(deque._items.find((i) => isEqual(i, item)))),
      isEmpty: jest.fn().mockImplementation(() => deque._items.length === 0),
      offer: jest.fn().mockImplementation((item) => {
        deque._items.push(item)
        return true
      }),
      peek: jest.fn().mockImplementation(() => undefinedToNull(deque._items[0])),
      poll: jest.fn().mockImplementation(() => undefinedToNull(deque._items.shift())),
      clear: jest.fn().mockImplementation(() => (deque._items = [])),
      offerFirst: jest.fn().mockImplementation((item) => deque._items.unshift(item)),
      toArray: jest.fn().mockImplementation(() => deque._items),
    }
    Registry.registerServiceLocatorItems(
      new Map<ServiceLocatorItems, () => any>([
        [ServiceLocatorItems.blockingDeque, () => deque],
        [ServiceLocatorItems.sleeper, () => sleeper],
      ]),
    )
    mockLogger()
  })
  afterEach(() => {
    ;(Registry.getServiceLocator().locate(ServiceLocatorItems.blockingDeque) as LinkedBlockingDeque<File>).clear()
    ;(sleeper.sleep as jest.Mock).mockClear()
  })
  afterAll(() => Registry.drop())
  it('should call onError from executeFirstScheduledTaskIfPresent if task results in error', (done) => {
    provideMessageQueue().then((res) => {
      const errorPromise = new YSError('error from mock!')
      res.observeHead = jest.fn().mockReturnValue(reject<Nullable<Task>>(errorPromise))
      const taskProcessor = new TaskProcessorImpl(res, provideDelayedTasks([]))
      const onError = jest.spyOn(taskProcessor, 'onError')
      taskProcessor.executeFirstScheduledTaskIfPresent().failed((_) => {
        expect(onError).toBeCalledTimes(1)
        expect(onError).toBeCalledWith(null, errorPromise)
        done()
      })
    })
  })
  it('should return correct timeout interval from getRetryInterval', () => {
    expect(TaskProcessor.getRetryInterval(0)).toBe(0)
    expect(TaskProcessor.getRetryInterval(1)).toBe(1000)
    expect(TaskProcessor.getRetryInterval(2)).toBe(10000)
    expect(TaskProcessor.getRetryInterval(3)).toBe(60000)
    expect(TaskProcessor.getRetryInterval(4)).toBe(600000)
    expect(() => TaskProcessor.getRetryInterval(10)).toThrowError('Invalid attemptNumber of retry 10')
  })
  it('should not call delayTask from delayCurrentCommand if localCommandsQueue.poll returns null', (done) => {
    provideMessageQueue().then(async (res) => {
      res.poll = jest.fn().mockReturnValue(null)
      const delayedTasks = provideDelayedTasks([])
      delayedTasks.delayTask = jest.fn()
      const taskProcessor = new TaskProcessorImpl(res, delayedTasks)
      const task = await provideTestTask(int64(12345), TaskType.saveDraft)
      taskProcessor.delayCurrentCommand(task!)
      expect(delayedTasks.delayTask).not.toBeCalled()
      done()
    })
  })
  it('should call delayTask from delayCurrentCommand if localCommandsQueue.poll returns File', (done) => {
    provideMessageQueue().then(async (res) => {
      const fileWithCommand = new File('command1.txt')
      res.poll = jest.fn().mockReturnValue(fileWithCommand)
      const delayedTasks = provideDelayedTasks([])
      delayedTasks.delayTask = jest.fn()
      const taskProcessor = new TaskProcessorImpl(res, delayedTasks)
      const task = await provideTestTask(int64(12345), TaskType.clearFolder)
      taskProcessor.delayCurrentCommand(task!)
      expect(delayedTasks.delayTask).toBeCalledWith(task, fileWithCommand)
      done()
    })
  })
  it('should call onQueueEmpty from delayCurrentCommand if localCommandsQueue is empty', (done) => {
    provideMessageQueue().then(async (res) => {
      const fileWithCommand = new File('command1.txt')
      res.poll = jest.fn().mockReturnValue(fileWithCommand)
      res.isEmpty = jest.fn().mockReturnValue(true)
      const delayedTasks = provideDelayedTasks([])
      delayedTasks.delayTask = jest.fn()
      const taskProcessor = new TaskProcessorImpl(res, delayedTasks)
      const task = await provideTestTask(int64(12345), TaskType.markRead)
      taskProcessor.onQueueEmpty = jest.fn()
      taskProcessor.delayCurrentCommand(task!)
      expect(delayedTasks.delayTask).toBeCalledTimes(1)
      expect(delayedTasks.delayTask).toBeCalledWith(task, fileWithCommand)
      expect(taskProcessor.onQueueEmpty).toBeCalledTimes(1)
      done()
    })
  })
  it('should call sendDataToServer from executeFirstScheduledTaskInner if task is not for frozen uid', (done) => {
    provideMessageQueue().then(async (res) => {
      const task = await provideTestTask(int64(123), TaskType.markRead)
      task!.sendDataToServer = jest.fn().mockReturnValue(resolve(getVoid()))
      res.observeHead = jest.fn().mockReturnValue(resolve(task))
      const delayedTasks = provideDelayedTasks([int64(456), int64(789)])
      const taskProcessor = new TaskProcessorImpl(res, delayedTasks)
      taskProcessor.executeFirstScheduledTaskIfPresent().then((_) => {
        expect(task!.sendDataToServer).toBeCalledTimes(1)
        done()
      })
    })
  })
  it('should reset current retries counter with resetRetries', (done) => {
    provideMessageQueue().then((res) => {
      const delayedTasks = provideDelayedTasks([int64(12345), int64(67890)])
      const taskProcessor = new TaskProcessorImpl(res, delayedTasks)
      taskProcessor.incrementRetries()
      const currentRetriesCount = taskProcessor.currentRetriesCount()
      taskProcessor.resetRetries()
      expect(taskProcessor.currentRetriesCount()).toBe(0)
      expect(currentRetriesCount).not.toBe(0)
      done()
    })
  })
  it('should remove task from head in deleteCurrentCommand', (done) => {
    provideMessageQueue().then((res) => {
      const delayedTasks = provideDelayedTasks([int64(123), int64(456)])
      const taskProcessor = new TaskProcessorImpl(res, delayedTasks)
      res.removeHead = jest.fn().mockReturnValue(resolve(getVoid()))
      taskProcessor.deleteCurrentCommand().then((_) => {
        expect(res.removeHead).toBeCalledTimes(1)
        done()
      })
    })
  })
  it('should call onQueueEmpty from deleteCurrentCommand if queue is empty after head removal', (done) => {
    provideMessageQueue().then((res) => {
      const delayedTasks = provideDelayedTasks([int64(12345), int64(67890)])
      res.isEmpty = jest.fn().mockReturnValue(true)
      const taskProcessor = new TaskProcessorImpl(res, delayedTasks)
      taskProcessor.onQueueEmpty = jest.fn()
      taskProcessor.deleteCurrentCommand().then((_) => {
        expect(taskProcessor.onQueueEmpty).toBeCalledTimes(1)
        done()
      })
    })
  })
  it('should not call onQueueEmpty from deleteCurrentCommand if queue is not empty after head removal', (done) => {
    provideMessageQueue().then((res) => {
      const delayedTasks = provideDelayedTasks([int64(12345), int64(67890)])
      res.isEmpty = jest.fn().mockReturnValue(false)
      const taskProcessor = new TaskProcessorImpl(res, delayedTasks)
      taskProcessor.onQueueEmpty = jest.fn()
      taskProcessor.deleteCurrentCommand().then((_) => {
        expect(taskProcessor.onQueueEmpty).not.toBeCalled()
        done()
      })
    })
  })
  it('should reset retries counter from deleteCurrentCommand', (done) => {
    provideMessageQueue().then((res) => {
      const delayedTasks = provideDelayedTasks([int64(12345), int64(67890)])
      res.isEmpty = jest.fn().mockReturnValue(true)
      const taskProcessor = new TaskProcessorImpl(res, delayedTasks)
      taskProcessor.resetRetries = jest.fn()
      taskProcessor.deleteCurrentCommand().then((_) => {
        expect(taskProcessor.resetRetries).toBeCalledTimes(1)
        done()
      })
    })
  })
  it('should call observeHead in executeFirstScheduledTaskIfPresent', (done) => {
    provideMessageQueue().then(async (res) => {
      const task = await provideTestTask(int64(11111), TaskType.sendMessage)
      task!.sendDataToServer = jest.fn().mockReturnValue(resolve(new NetworkStatus(NetworkStatusCode.ok)))
      const delayedTasks = provideDelayedTasks([int64(123), int64(456)])
      res.observeHead = jest.fn().mockReturnValue(resolve(task))
      const taskProcessor = new TaskProcessorImpl(res, delayedTasks)
      taskProcessor.executeFirstScheduledTaskIfPresent().then((_) => {
        expect(res.observeHead).toBeCalledTimes(1)
        done()
      })
    })
  })
  it('should not call sendDataToServer and should delay task if task is for frozen uid', (done) => {
    provideMessageQueue().then(async (res) => {
      const task = await provideTestTask(int64(123), TaskType.clearFolder)
      task!.sendDataToServer = jest.fn().mockReturnValue(resolve(new NetworkStatus(NetworkStatusCode.ok)))
      const delayedTasks = provideDelayedTasks([int64(123), int64(456)])
      res.observeHead = jest.fn().mockReturnValue(resolve(task))
      const taskProcessor = new TaskProcessorImpl(res, delayedTasks)
      taskProcessor.delayCurrentCommand = jest.fn()
      taskProcessor.executeFirstScheduledTaskIfPresent().then((_) => {
        expect(task!.sendDataToServer).not.toBeCalled()
        expect(taskProcessor.delayCurrentCommand).toBeCalledWith(task)
        done()
      })
    })
  })
  it('should delete current command from executeFirstScheduledTaskInner if sendDataToServer throws not Transport level error', (done) => {
    provideMessageQueue().then(async (res) => {
      const task = await provideTestTask(int64(123), TaskType.markRead)
      task!.sendDataToServer = jest.fn().mockReturnValue(rejected('sendDataToServer failed!'))
      const delayedTasks = provideDelayedTasks([int64(456), int64(789)])
      res.observeHead = jest.fn().mockReturnValue(resolve(task))
      const taskProcessor = new TaskProcessorImpl(res, delayedTasks)
      const deleteCurrentCommandSpy = jest
        .spyOn(taskProcessor, 'deleteCurrentCommand')
        .mockReturnValue(resolve(getVoid()))
      const onErrorSpy = jest.spyOn(taskProcessor, 'onError')
      taskProcessor.executeFirstScheduledTaskInner().then((t) => {
        expect(t).toBe(task)
        expect(deleteCurrentCommandSpy).toBeCalled()
        expect(onErrorSpy).toBeCalled()
        done()
      })
    })
  })
  it('should not delete current command from executeFirstScheduledTaskInner if sendDataToServer throws Transport level error', (done) => {
    provideMessageQueue().then(async (res) => {
      const task = await provideTestTask(int64(123), TaskType.markRead)
      task!.sendDataToServer = jest.fn().mockReturnValue(rejected('Error communicating with the server'))
      const delayedTasks = provideDelayedTasks([int64(456), int64(789)])
      res.observeHead = jest.fn().mockReturnValue(resolve(task))
      const taskProcessor = new TaskProcessorImpl(res, delayedTasks)
      const deleteCurrentCommandSpy = jest
        .spyOn(taskProcessor, 'deleteCurrentCommand')
        .mockReturnValue(resolve(getVoid()))
      const onErrorSpy = jest.spyOn(taskProcessor, 'onError')
      taskProcessor.executeFirstScheduledTaskInner().then((t) => {
        expect(t).toBe(task)
        expect(deleteCurrentCommandSpy).not.toBeCalled()
        expect(onErrorSpy).not.toBeCalled()
        done()
      })
    })
  })
  it('should call onError from executeFirstScheduledTaskInner if sendDataToServer throws non-Transport level error, and deletion fails', (done) => {
    provideMessageQueue().then(async (res) => {
      const task = await provideTestTask(int64(123), TaskType.markRead)
      task!.sendDataToServer = jest.fn().mockReturnValue(rejected('ERROR'))
      const delayedTasks = provideDelayedTasks([int64(456), int64(789)])
      res.observeHead = jest.fn().mockReturnValue(resolve(task))
      const taskProcessor = new TaskProcessorImpl(res, delayedTasks)
      const deleteCurrentCommandSpy = jest
        .spyOn(taskProcessor, 'deleteCurrentCommand')
        .mockReturnValue(reject(new YSError('FAILED')))
      const onErrorSpy = jest.spyOn(taskProcessor, 'onError')
      taskProcessor.executeFirstScheduledTaskInner().failed((err) => {
        expect(deleteCurrentCommandSpy).toBeCalled()
        expect(err.message).toBe('FAILED')
        expect(onErrorSpy).toBeCalled()
        done()
      })
    })
  })
  it('should delete current command from executeFirstScheduledTaskInner if sendDataToServer returns null', (done) => {
    provideMessageQueue().then(async (res) => {
      const task = await provideTestTask(int64(123), TaskType.markRead)
      task!.sendDataToServer = jest.fn().mockReturnValue(resolve(null))
      const delayedTasks = provideDelayedTasks([int64(456), int64(789)])
      res.observeHead = jest.fn().mockReturnValue(resolve(task))
      const taskProcessor = new TaskProcessorImpl(res, delayedTasks)
      const deleteCurrentCommandSpy = jest
        .spyOn(taskProcessor, 'deleteCurrentCommand')
        .mockReturnValue(resolve(getVoid()))
      taskProcessor.executeFirstScheduledTaskInner().then((t) => {
        expect(deleteCurrentCommandSpy).toBeCalled()
        expect(t).toBe(task)
        done()
      })
    })
  })
  it('should call deleteCurrentCommand after successfully completed sendDataToServer', (done) => {
    provideMessageQueue().then(async (res) => {
      const task = await provideTestTask(int64(123), TaskType.markRead)
      task!.sendDataToServer = jest.fn().mockReturnValue(resolve(new NetworkStatus(NetworkStatusCode.ok)))
      res.observeHead = jest.fn().mockReturnValue(resolve(task))
      const delayedTasks = provideDelayedTasks([int64(456), int64(789)])
      const taskProcessor = new TaskProcessorImpl(res, delayedTasks)
      const deleteCurrentCommandSpy = jest
        .spyOn(taskProcessor, 'deleteCurrentCommand')
        .mockReturnValue(resolve(getVoid()))
      taskProcessor.executeFirstScheduledTaskInner().then((t) => {
        expect(deleteCurrentCommandSpy).toBeCalledTimes(1)
        expect(t).toBe(task)
        done()
      })
    })
  })
  it('should call deleteCurrentCommand after sendDataToServer failed with permanent error', (done) => {
    provideMessageQueue().then(async (res) => {
      const task = await provideTestTask(int64(123), TaskType.markRead)
      task!.sendDataToServer = jest.fn().mockReturnValue(resolve(new NetworkStatus(NetworkStatusCode.permanentError)))
      const delayedTasks = provideDelayedTasks([int64(456), int64(789)])
      res.observeHead = jest.fn().mockReturnValue(resolve(task))
      const taskProcessor = new TaskProcessorImpl(res, delayedTasks)
      const deleteCurrentCommandSpy = jest
        .spyOn(taskProcessor, 'deleteCurrentCommand')
        .mockReturnValue(resolve(getVoid()))
      taskProcessor.executeFirstScheduledTaskInner().then((t) => {
        expect(Log.getDefaultLogger()!.error).toBeCalledWith(
          expect.stringContaining('Permanent network error occured: '),
        )
        expect(deleteCurrentCommandSpy).toBeCalled()
        expect(t).toBe(task)
        done()
      })
    })
  })
  it('should call deleteCurrentCommand after sendDataToServer failed with authenticationError error', (done) => {
    provideMessageQueue().then(async (res) => {
      const task = await provideTestTask(int64(123), TaskType.markRead)
      task!.sendDataToServer = jest
        .fn()
        .mockReturnValue(resolve(new NetworkStatus(NetworkStatusCode.authenticationError)))
      const delayedTasks = provideDelayedTasks([int64(456), int64(789)])
      res.observeHead = jest.fn().mockReturnValue(resolve(task))
      const taskProcessor = new TaskProcessorImpl(res, delayedTasks)
      const deleteCurrentCommandSpy = jest
        .spyOn(taskProcessor, 'deleteCurrentCommand')
        .mockReturnValue(resolve(getVoid()))
      taskProcessor.executeFirstScheduledTaskInner().then((t) => {
        expect(Log.getDefaultLogger()!.error).toBeCalledWith(expect.stringContaining('Authentication error: '))
        expect(deleteCurrentCommandSpy).toBeCalled()
        expect(t).toBe(task)
        done()
      })
    })
  })
  it('should sleep for a period, increment retries (if inbound) and return task if sendDataToServer returned temporary error', (done) => {
    provideMessageQueue().then(async (res) => {
      const task = await provideTestTask(int64(123), TaskType.markRead)
      task!.sendDataToServer = jest.fn().mockReturnValue(resolve(new NetworkStatus(NetworkStatusCode.temporaryError)))
      const delayedTasks = provideDelayedTasks([int64(456), int64(789)])
      res.observeHead = jest.fn().mockReturnValue(resolve(task))
      const taskProcessor = new TaskProcessorImpl(res, delayedTasks)
      taskProcessor.incrementRetries()
      taskProcessor.incrementRetries()
      const retries = taskProcessor.currentRetriesCount()
      const deleteCurrentCommandSpy = jest
        .spyOn(taskProcessor, 'deleteCurrentCommand')
        .mockReturnValue(resolve(getVoid()))
      taskProcessor.executeFirstScheduledTaskInner().then((t) => {
        expect(deleteCurrentCommandSpy).not.toBeCalled()
        expect(sleeper.sleep).toBeCalledWith(TaskProcessor.getRetryInterval(retries))
        expect(taskProcessor.currentRetriesCount()).toBe(retries + 1)
        expect(t).toBe(task)
        done()
      })
    })
  })
  it('should delete current command if retries counter is out of bounds and sendDataToServer returned temporary error', (done) => {
    provideMessageQueue().then(async (res) => {
      const task = await provideTestTask(int64(123), TaskType.markRead)
      task!.sendDataToServer = jest.fn().mockReturnValue(resolve(new NetworkStatus(NetworkStatusCode.temporaryError)))
      const delayedTasks = provideDelayedTasks([int64(456), int64(789)])
      res.observeHead = jest.fn().mockReturnValue(resolve(task))
      const taskProcessor = new TaskProcessorImpl(res, delayedTasks)
      for (let i = 0; i <= TaskProcessor.getMaxRetries(); ++i) {
        taskProcessor.incrementRetries()
      }
      const retries = taskProcessor.currentRetriesCount()
      const deleteCurrentCommandSpy = jest
        .spyOn(taskProcessor, 'deleteCurrentCommand')
        .mockReturnValue(resolve(getVoid()))
      taskProcessor.executeFirstScheduledTaskInner().then((t) => {
        expect(deleteCurrentCommandSpy).toBeCalled()
        expect(Log.getDefaultLogger()!.warn).toBeCalledWith(
          expect.stringContaining('Exceeded number of retries for a task'),
        )
        expect(sleeper.sleep).not.toBeCalled()
        expect(taskProcessor.currentRetriesCount()).toBe(retries)
        expect(t).toBe(task)
        done()
      })
    })
  })
  it('should return immediatelly if the queue is empty', (done) => {
    provideMessageQueue().then((res) => {
      const delayedTasks = provideDelayedTasks([int64(456), int64(789)])
      res.observeHead = jest.fn().mockReturnValue(resolve(null))
      const taskProcessor = new TaskProcessorImpl(res, delayedTasks)
      taskProcessor.executeFirstScheduledTaskInner().then((t) => {
        expect(t).toBeNull()
        done()
      })
    })
  })
})
