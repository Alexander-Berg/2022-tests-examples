import { resolve } from '../../../../../../common/xpromise-support'
import { int64 } from '../../../../../../common/ys'
import { getVoid } from '../../../../../common/code/result/result'
import { JSONItemFromJSON } from '../../../../../common/__tests__/__helpers__/json-helpers'
import { FolderType } from '../../../../../mapi/../mapi/code/api/entities/folder/folder'
import { MessageMeta } from '../../../../../mapi/../mapi/code/api/entities/message/message-meta'
import { NetworkStatus, NetworkStatusCode } from '../../../../../mapi/../mapi/code/api/entities/status/network-status'
import { createMockInstance } from '../../../../../common/__tests__/__helpers__/utils'
import { Drafts } from '../../../../code/busilogics/draft/drafts'
import { Folders } from '../../../../code/busilogics/folders/folders'
import { Labels } from '../../../../code/busilogics/labels/labels'
import { Messages } from '../../../../code/busilogics/messages/messages'
import { Cleanup } from '../../../../code/busilogics/sync/cleanup'
import { InvalidCommandError, TaskType } from '../../../../code/service/task'
import { DeleteTask } from '../../../../code/service/tasks/delete-task'
import { MockStorage, MockTaskActionsNotifier, MockWithinTransaction } from '../../../__helpers__/mock-patches'
import { idstr, makeSearch, MockModels } from '../../../__helpers__/models'

describe(DeleteTask, () => {
  it('should be serializable', () => {
    const res = new DeleteTask(1, int64(2), [int64(3), int64(4)], int64(5), int64(6), true, MockModels())
    expect(res.serialize()).toStrictEqual(
      JSONItemFromJSON({
        taskType: TaskType.delete,
        version: 1,
        uid: int64(2),
        mids: [idstr(3), idstr(4)],
        targetFid: idstr(5),
        currentFid: idstr(6),
        from_push: true,
      }),
    )
  })

  it('should be deserializable', async () => {
    const models = MockModels()

    expect(
      await DeleteTask.fromJSONItem(
        JSONItemFromJSON({
          taskType: TaskType.delete,
          version: 1,
          uid: '2',
          mids: [idstr(3), idstr(4)],
          targetFid: idstr(5),
          currentFid: idstr(6),
          from_push: true,
        }),
        models,
      ),
    ).toMatchObject(new DeleteTask(1, int64(2), [int64(3), int64(4)], int64(5), int64(6), true, models))

    expect(
      await DeleteTask.fromJSONItem(
        JSONItemFromJSON([
          {
            taskType: TaskType.delete,
            version: 1,
            uid: '2',
            mids: [idstr(3), idstr(4)],
            targetFid: idstr(5),
            currentFid: idstr(6),
            from_push: true,
          },
        ]),
        models,
      ),
    ).toBeNull()

    expect(
      await DeleteTask.fromJSONItem(
        JSONItemFromJSON({
          taskType: TaskType.delete,
          version: 1,
          uid: '2',
          mids: [idstr(3), idstr(4)],
          targetFid: idstr(5),
          currentFid: idstr(6),
        }),
        models,
      ),
    ).toBeNull()
  })

  it('should be re-serializable', async () => {
    const models = MockModels()
    const task = new DeleteTask(1, int64(2), [int64(3), int64(4)], int64(5), int64(6), true, models)
    const json = task.serialize()
    const reserialized = await DeleteTask.fromJSONItem(json, models)
    expect(task).toMatchObject(reserialized!)
  })

  it('should return type', () => {
    const task = new DeleteTask(1, int64(2), [int64(3), int64(4)], int64(5), int64(6), true, MockModels())
    expect(task.getType()).toBe(TaskType.delete)
  })

  it('should send request to server', (done) => {
    const messages = createMockInstance(Messages, {
      sendDeleteMessagesAction: jest.fn().mockReturnValue(resolve(new NetworkStatus(NetworkStatusCode.ok))),
    })
    const models = MockModels(undefined, { messages })

    const task = new DeleteTask(1, int64(2), [int64(3), int64(4)], int64(5), int64(6), true, models)
    expect.assertions(2)
    task.sendDataToServer().then((res) => {
      expect(messages.sendDeleteMessagesAction).toBeCalledWith([int64(3), int64(4)], int64(6))
      expect(res).toStrictEqual(new NetworkStatus(NetworkStatusCode.ok))
      done()
    })
  })

  describe(DeleteTask.create, () => {
    it('should reject on empty message IDs', (done) => {
      const models = MockModels()
      DeleteTask.create(1, int64(2), [], true, models).failed((error) => {
        expect(error).toStrictEqual(
          new InvalidCommandError(
            'Failed to create DeleteTask. MessageIDs should have at least one message to get folderID.',
          ),
        )
        done()
      })
    })

    it('should reject on empty "fetchFirstFidByType"', (done) => {
      const folders: Folders = createMockInstance(Folders, {
        fetchFirstFidByType: jest.fn().mockReturnValue(resolve(null)),
      })
      const messages: Messages = createMockInstance(Messages, {
        messageMetaByMid: jest.fn().mockReturnValue(resolve(createMockInstance(MessageMeta, { fid: int64(1) }))),
      })
      const models = MockModels(undefined, { folders, messages })
      DeleteTask.create(1, int64(2), [int64(3), int64(4)], true, models).failed((error) => {
        expect(folders.fetchFirstFidByType).toBeCalledWith(FolderType.trash)
        expect(error).toStrictEqual(
          new InvalidCommandError(
            'Failed to create DeleteTask. Can\'t find fid by folder type. Folder with type = "7" does not exist.',
          ),
        )
        done()
      })
    })

    it('should reject on empty "messageMetaByMid"', (done) => {
      const folders: Folders = createMockInstance(Folders, {
        fetchFirstFidByType: jest.fn().mockReturnValue(resolve(int64(3))),
      })
      const messages: Messages = createMockInstance(Messages, {
        messageMetaByMid: jest.fn().mockReturnValue(resolve(null)),
      })
      const models = MockModels(undefined, { folders, messages })
      DeleteTask.create(1, int64(2), [int64(3), int64(4)], true, models).failed((error) => {
        expect(messages.messageMetaByMid).toBeCalledWith(int64(3))
        expect(error).toStrictEqual(
          new InvalidCommandError(
            'Failed to create DeleteTask. Can\'t find fid by local mid. Message with mid = "3" does not exist.',
          ),
        )
        done()
      })
    })

    it('should be creatable', (done) => {
      const folders: Folders = createMockInstance(Folders, {
        fetchFirstFidByType: jest.fn().mockReturnValue(resolve(int64(3))),
      })
      const messages: Messages = createMockInstance(Messages, {
        messageMetaByMid: jest.fn().mockReturnValue(resolve(createMockInstance(MessageMeta, { fid: int64(1) }))),
      })
      const models = MockModels(undefined, { folders, messages })
      DeleteTask.create(1, int64(2), [int64(3), int64(4)], true, models).then((task) => {
        expect(task).toBeInstanceOf(DeleteTask)
        expect(task as DeleteTask).toStrictEqual(
          new DeleteTask(1, int64(2), [int64(3), int64(4)], int64(3), int64(1), true, models),
        )
        expect(folders.fetchFirstFidByType).toBeCalledWith(FolderType.trash)
        expect(messages.messageMetaByMid).toBeCalledWith(int64(3))
        done()
      })
    })
  })

  it("should skip full database update if it's from push", (done) => {
    const search = makeSearch()
    const getSearchResultsForFolderInMidsSpy = jest
      .spyOn(search, 'getSearchResultsForFolderInMids')
      .mockReturnValue(resolve([int64(30), int64(40)]))
    const updateMessagesShowForSpy = jest.spyOn(search, 'updateMessagesShowFor').mockReturnValue(resolve(getVoid()))

    const messages = createMockInstance(Messages, {
      moveMessages: jest.fn().mockReturnValue(resolve(getVoid())),
      resetMessagesTimestamp: jest.fn().mockReturnValue(resolve(getVoid())),
    })

    const drafts = createMockInstance(Drafts, {
      deleteDraftsByMids: jest.fn().mockReturnValue(resolve(getVoid())),
    })

    const taskActionsNotifier = MockTaskActionsNotifier()

    const models = MockModels({ taskActionsNotifier }, { search, messages, drafts })
    const task = new DeleteTask(1, int64(2), [int64(3), int64(4)], int64(5), int64(6), true, models)

    task.updateDatabase().then((_) => {
      expect(messages.resetMessagesTimestamp).toBeCalledWith([int64(3), int64(4)])
      expect(messages.moveMessages).toBeCalledWith([int64(3), int64(4)], int64(5), int64(6))
      expect(drafts.deleteDraftsByMids).toBeCalledWith([int64(3), int64(4)])
      expect(getSearchResultsForFolderInMidsSpy).toBeCalledWith(int64(6), [int64(3), int64(4)])
      expect(updateMessagesShowForSpy).toBeCalledWith('s_f_5', [int64(30), int64(40)])
      expect(taskActionsNotifier.notifyTicketsAboutMoveToFolder).toBeCalledWith(
        int64(2),
        [int64(3), int64(4)],
        int64(6),
        int64(5),
      )
      done()
    })
  })

  it("should perform full database update if it's not from push", (done) => {
    const search = makeSearch()
    const getSearchResultsForFolderInMidsSpy = jest
      .spyOn(search, 'getSearchResultsForFolderInMids')
      .mockReturnValue(resolve([int64(30), int64(40)]))
    const updateMessagesShowForSpy = jest.spyOn(search, 'updateMessagesShowFor').mockReturnValue(resolve(getVoid()))

    const messages = createMockInstance(Messages, {
      moveMessages: jest.fn().mockReturnValue(resolve(getVoid())),
      resetMessagesTimestamp: jest.fn().mockReturnValue(resolve(getVoid())),
      deleteMessagesByFidAndMids: jest.fn().mockReturnValue(resolve(getVoid())),
    })

    const folders = createMockInstance(Folders, {
      cleanUpFolderMessagesConnection: jest.fn().mockReturnValue(resolve(getVoid())),
    })

    const labels = createMockInstance(Labels, {
      deleteLabelsByFidAndMids: jest.fn().mockReturnValue(resolve(getVoid())),
    })

    const cleanup = createMockInstance(Cleanup, {
      removeOrphans: jest.fn().mockReturnValue(resolve(getVoid())),
      rebuildAggregates: jest.fn().mockReturnValue(resolve(getVoid())),
    })

    const drafts = createMockInstance(Drafts, {
      deleteDraftsByMids: jest.fn().mockReturnValue(resolve(getVoid())),
    })

    const storage = MockStorage({
      withinTransaction: MockWithinTransaction<any>(),
    })

    const taskActionsNotifier = MockTaskActionsNotifier()

    const models = MockModels({ storage, taskActionsNotifier }, { search, messages, folders, labels, cleanup, drafts })
    const task = new DeleteTask(1, int64(2), [int64(3), int64(4)], int64(5), int64(6), false, models)

    task.updateDatabase().then((_) => {
      expect(messages.deleteMessagesByFidAndMids).toBeCalledWith(int64(5), [int64(3), int64(4)])
      expect(folders.cleanUpFolderMessagesConnection).toBeCalledWith([int64(3), int64(4)], int64(5))
      expect(labels.deleteLabelsByFidAndMids).toBeCalledWith(int64(5), [int64(3), int64(4)])
      expect(cleanup.removeOrphans).toBeCalledWith([int64(3), int64(4)])
      expect(cleanup.rebuildAggregates).toBeCalledWith([int64(3), int64(4)])
      expect(drafts.deleteDraftsByMids).toBeCalledWith([int64(3), int64(4)])

      expect(messages.resetMessagesTimestamp).toBeCalledWith([int64(3), int64(4)])
      expect(messages.moveMessages).toBeCalledWith([int64(3), int64(4)], int64(5), int64(6))
      expect(getSearchResultsForFolderInMidsSpy).toBeCalledWith(int64(6), [int64(3), int64(4)])
      expect(updateMessagesShowForSpy).toBeCalledWith('s_f_5', [int64(30), int64(40)])

      expect(taskActionsNotifier.notifyTicketsAboutMoveToFolder).toBeCalledWith(
        int64(2),
        [int64(3), int64(4)],
        int64(6),
        int64(5),
      )

      done()
    })
  })
})
