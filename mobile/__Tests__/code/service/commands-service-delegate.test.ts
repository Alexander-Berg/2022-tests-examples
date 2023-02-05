import { resolve } from '../../../../../common/xpromise-support'
import { int64, int64ToInt32 } from '../../../../../common/ys'
import {
  MockFileSystem,
  MockJSONSerializer,
  mockLogger,
  MockNetwork,
} from '../../../../common/__tests__/__helpers__/mock-patches'
import { XPromise } from '../../../../common/code/promise/xpromise'
import { getVoid } from '../../../../common/code/result/result'
import { createMockInstance } from '../../../../common/__tests__/__helpers__/utils'
import { ConstantIDs } from '../../../../mapi/../mapi/code/api/common/id'
import { AttachmentsManager } from '../../../code/busilogics/attachments/attachments'
import { MessageBodyStore } from '../../../code/busilogics/body/message-body-store'
import { Folders } from '../../../code/busilogics/folders/folders'
import { Labels } from '../../../code/busilogics/labels/labels'
import { Messages, MessagesSettings } from '../../../code/busilogics/messages/messages'
import { SearchModel } from '../../../code/busilogics/search/search-model'
import { Cleanup } from '../../../code/busilogics/sync/cleanup'
import { Threads } from '../../../code/busilogics/threads/threads'
import { CommandsServiceActions } from '../../../code/service/commands-service-actions'
import { CommandsServiceDelegate, CommandsServiceTaskDescriptor } from '../../../code/service/commands-service-delegate'
import { Task, TaskType } from '../../../code/service/task'
import { CustomTypeTestTask } from '../../__helpers__/custom-type-test-task'
import { MockHighPrecisionTimer, MockStorage, MockWithinTransaction } from '../../__helpers__/mock-patches'
import { MockSharedPreferences } from '../../../../common/__tests__/__helpers__/preferences-mock'
import { TestIDSupport } from '../../__helpers__/test-id-support'
import { MockModels } from '../../__helpers__/models'
import { Registry } from '../../../code/registry'

class TestCommandsServiceDelegate extends CommandsServiceDelegate {
  protected submitNetworkPartOfTask(task: Task): XPromise<void> {
    return resolve(getVoid())
  }
}

describe(CommandsServiceDelegate, () => {
  describe(CommandsServiceDelegate.prototype.submitTask, () => {
    afterEach(jest.restoreAllMocks)
    beforeAll(() => mockLogger())
    afterAll(() => Registry.drop())
    it('should return error if created task has no UID and not of delete-database type', (done) => {
      const storage = MockStorage()
      const network = MockNetwork()
      const idSupport = new TestIDSupport()
      const folders = new Folders(storage, new MockSharedPreferences(), idSupport, MockHighPrecisionTimer())
      const threads = new Threads(network, storage, idSupport)

      const settings = MessagesSettings.builder()
        .setAttachmentsManager(createMockInstance(AttachmentsManager))
        .setFolders(folders)
        .setThreads(threads)
        .setLabels(new Labels(storage, idSupport))
        .setSearch(new SearchModel(storage, network, idSupport))
        .setCleanup(new Cleanup(storage, threads, folders, idSupport))
        .setBodies(new MessageBodyStore(MockFileSystem(), 'account'))
        .build()

      const messages = new Messages(network, storage, MockJSONSerializer(), idSupport, settings)
      const models = MockModels(undefined, {
        folders,
        threads,
        messages,
        settings,
      })
      const delegate = new TestCommandsServiceDelegate(storage, models)
      const task = new CustomTypeTestTask(1, int64ToInt32(ConstantIDs.noUID), TaskType.delete)
      const createAPITaskSpy = jest.spyOn(CommandsServiceActions, 'createAPITask').mockReturnValue(resolve(task))
      const descriptor = new CommandsServiceTaskDescriptor(ConstantIDs.noUID, true, CommandsServiceActions.delete)
      delegate.submitTask(descriptor).failed((err) => {
        expect(createAPITaskSpy).toBeCalledWith(descriptor, models)
        expect(err.message).toBe('Should add UID to task descriptor')
        done()
      })
    })
    it('should return error if task is null (not supported or not found)', (done) => {
      const delegate = new TestCommandsServiceDelegate(MockStorage(), MockModels())
      const descriptor = new CommandsServiceTaskDescriptor(ConstantIDs.noUID, true, 'non-existant')
      delegate.submitTask(descriptor).failed((err) => {
        expect(err.message).toBe(`Task for descriptor ${descriptor.action} was not created`)
        done()
      })
    })
    it('should run pre-update, should update database, submit to network if required and post-update', (done) => {
      const storage = MockStorage({
        withinTransaction: MockWithinTransaction<any>(),
      })
      const network = MockNetwork()
      const idSupport = new TestIDSupport()
      const folders = new Folders(storage, new MockSharedPreferences(), idSupport, MockHighPrecisionTimer())
      const threads = new Threads(network, storage, idSupport)

      const settings = MessagesSettings.builder()
        .setAttachmentsManager(createMockInstance(AttachmentsManager))
        .setFolders(folders)
        .setThreads(threads)
        .setLabels(new Labels(storage, idSupport))
        .setSearch(new SearchModel(storage, network, idSupport))
        .setCleanup(new Cleanup(storage, threads, folders, idSupport))
        .setBodies(new MessageBodyStore(MockFileSystem(), 'account'))
        .build()

      const messages = new Messages(network, storage, MockJSONSerializer(), idSupport, settings)
      const models = MockModels(undefined, {
        folders,
        threads,
        messages,
        settings,
      })
      const delegate = new TestCommandsServiceDelegate(storage, models)
      const task = new CustomTypeTestTask(1, 1, TaskType.delete)
      const innerTask = new CustomTypeTestTask(2, 2, TaskType.markRead)
      const updateDatabaseInnerTaskSpy = jest.spyOn(innerTask, 'updateDatabase').mockReturnValue(resolve(getVoid()))

      const preUpdateSpy = jest.spyOn(task, 'preUpdate').mockReturnValue(resolve(getVoid()))
      const updateDatabaseSpy = jest.spyOn(task, 'updateDatabase').mockReturnValue(resolve(getVoid()))
      jest.spyOn(task, 'getAdditionalTasks').mockReturnValue([innerTask])
      const postUpdateSpy = jest.spyOn(task, 'postUpdate').mockReturnValue(resolve(getVoid()))

      const submitNetworkPartOfTaskSpy = jest
        .spyOn(delegate as any, 'submitNetworkPartOfTask')
        .mockReturnValue(resolve(getVoid()))

      const createAPITaskSpy = jest.spyOn(CommandsServiceActions, 'createAPITask').mockReturnValue(resolve(task))
      const descriptor = new CommandsServiceTaskDescriptor(int64(1), true, CommandsServiceActions.markAsRead)
      delegate.submitTask(descriptor).then((_) => {
        expect(createAPITaskSpy).toBeCalledWith(descriptor, models)
        expect(storage.withinTransaction).toBeCalledWith(true, expect.any(Function))
        expect(preUpdateSpy).toBeCalled()
        expect(updateDatabaseSpy).toBeCalled()
        expect(updateDatabaseInnerTaskSpy).toBeCalled()
        expect(submitNetworkPartOfTaskSpy).toBeCalledWith(task)
        expect(postUpdateSpy).toBeCalled()
        done()
      })
    })
    it('should run pre-update, should update database, not submit to network if not required and post-update', (done) => {
      const storage = MockStorage({
        withinTransaction: MockWithinTransaction<any>(),
      })
      const network = MockNetwork()
      const idSupport = new TestIDSupport()
      const folders = new Folders(storage, new MockSharedPreferences(), idSupport, MockHighPrecisionTimer())
      const threads = new Threads(network, storage, idSupport)

      const settings = MessagesSettings.builder()
        .setAttachmentsManager(createMockInstance(AttachmentsManager))
        .setFolders(folders)
        .setThreads(threads)
        .setLabels(new Labels(storage, idSupport))
        .setSearch(new SearchModel(storage, network, idSupport))
        .setCleanup(new Cleanup(storage, threads, folders, idSupport))
        .setBodies(new MessageBodyStore(MockFileSystem(), 'account'))
        .build()

      const messages = new Messages(network, storage, MockJSONSerializer(), idSupport, settings)
      const models = MockModels(undefined, {
        folders,
        threads,
        messages,
        settings,
      })
      const delegate = new TestCommandsServiceDelegate(storage, models)
      const task = new CustomTypeTestTask(1, 1, TaskType.delete)
      const innerTask = new CustomTypeTestTask(2, 2, TaskType.markRead)
      const updateDatabaseInnerTaskSpy = jest.spyOn(innerTask, 'updateDatabase').mockReturnValue(resolve(getVoid()))

      const preUpdateSpy = jest.spyOn(task, 'preUpdate').mockReturnValue(resolve(getVoid()))
      const updateDatabaseSpy = jest.spyOn(task, 'updateDatabase').mockReturnValue(resolve(getVoid()))
      jest.spyOn(task, 'getAdditionalTasks').mockReturnValue([innerTask])
      const postUpdateSpy = jest.spyOn(task, 'postUpdate').mockReturnValue(resolve(getVoid()))

      const submitNetworkPartOfTaskSpy = jest
        .spyOn(delegate as any, 'submitNetworkPartOfTask')
        .mockReturnValue(resolve(getVoid()))

      const createAPITaskSpy = jest.spyOn(CommandsServiceActions, 'createAPITask').mockReturnValue(resolve(task))
      const descriptor = new CommandsServiceTaskDescriptor(int64(1), false, CommandsServiceActions.markAsRead)
      delegate.submitTask(descriptor).then((_) => {
        expect(createAPITaskSpy).toBeCalledWith(descriptor, models)
        expect(storage.withinTransaction).toBeCalledWith(true, expect.any(Function))
        expect(preUpdateSpy).toBeCalled()
        expect(updateDatabaseSpy).toBeCalled()
        expect(updateDatabaseInnerTaskSpy).toBeCalled()
        expect(submitNetworkPartOfTaskSpy).not.toBeCalled()
        expect(postUpdateSpy).toBeCalled()
        done()
      })
    })
    it('should not run pre-update, should not update database if uid is not specified, not submit to network if not required and post-update', (done) => {
      const storage = MockStorage()
      const network = MockNetwork()
      const idSupport = new TestIDSupport()
      const folders = new Folders(storage, new MockSharedPreferences(), idSupport, MockHighPrecisionTimer())
      const threads = new Threads(network, storage, idSupport)

      const settings = MessagesSettings.builder()
        .setAttachmentsManager(createMockInstance(AttachmentsManager))
        .setFolders(folders)
        .setThreads(threads)
        .setLabels(new Labels(storage, idSupport))
        .setSearch(new SearchModel(storage, network, idSupport))
        .setCleanup(new Cleanup(storage, threads, folders, idSupport))
        .setBodies(new MessageBodyStore(MockFileSystem(), 'account'))
        .build()

      const messages = new Messages(network, storage, MockJSONSerializer(), idSupport, settings)
      const models = MockModels(undefined, {
        folders,
        threads,
        messages,
        settings,
      })
      const delegate = new TestCommandsServiceDelegate(storage, models)
      const task = new CustomTypeTestTask(1, 123, TaskType.markRead)

      const createAPITaskSpy = jest.spyOn(CommandsServiceActions, 'createAPITask').mockReturnValue(resolve(task))
      const descriptor = new CommandsServiceTaskDescriptor(ConstantIDs.noUID, true, CommandsServiceActions.markAsRead)
      Object.assign(descriptor, { shouldSendToServer: false })
      delegate.submitTask(descriptor).failed((err) => {
        expect(err.message).toBe('Should add UID to task descriptor')
        expect(createAPITaskSpy).toBeCalledWith(descriptor, models)
        done()
      })
    })
  })
})
