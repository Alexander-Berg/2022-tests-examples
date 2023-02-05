import { resolve } from '../../../../../../common/xpromise-support'
import { int64 } from '../../../../../../common/ys'
import { MapJSONItem } from '../../../../../common/code/json/json-types'
import { getVoid } from '../../../../../common/code/result/result'
import { JSONItemFromJSON } from '../../../../../common/__tests__/__helpers__/json-helpers'
import { FolderType } from '../../../../../mapi/../mapi/code/api/entities/folder/folder'
import { createMockInstance } from '../../../../../common/__tests__/__helpers__/utils'
import { Folders } from '../../../../code/busilogics/folders/folders'
import { Messages } from '../../../../code/busilogics/messages/messages'
import { TaskType } from '../../../../code/service/task'
import { MarkSpamTask } from '../../../../code/service/tasks/mark-spam-task'
import { idstr, MockModels } from '../../../__helpers__/models'

describe(MarkSpamTask, () => {
  afterEach(jest.restoreAllMocks)
  it('should be deserializable', async (done) => {
    const models = MockModels(undefined, {
      folders: createMockInstance(Folders, {
        fetchFirstFidByType: jest.fn((type) =>
          resolve(
            ({
              [FolderType.inbox]: int64(1),
              [FolderType.spam]: int64(2),
            } as any)[type],
          ),
        ),
      }),
    })
    expect(
      await MarkSpamTask.fromJSONItem(
        JSONItemFromJSON([
          {
            taskType: TaskType.spam,
            version: 1,
            uid: '222222',
            messageIDs: [int64(123), int64(456)],
            targetFid: int64(12),
            currentFid: int64(34),
            isSpam: true,
          },
        ]),
        models,
      ),
    ).toBeNull()

    expect(
      await MarkSpamTask.fromJSONItem(
        JSONItemFromJSON({
          mids: [int64(123), int64(456)],
        }),
        models,
      ),
    ).toBeNull()

    expect(
      await MarkSpamTask.fromJSONItem(
        JSONItemFromJSON({
          taskType: TaskType.spam,
          version: 1,
          uid: '3333',
        }),
        models,
      ),
    ).toBeNull()

    MarkSpamTask.create(1, int64(2), [int64(3), int64(4)], int64(5), true, models).then(async (t) => {
      expect(
        await MarkSpamTask.fromJSONItem(
          JSONItemFromJSON({
            taskType: TaskType.spam,
            version: 1,
            uid: '2',
            mids: [idstr(3), idstr(4)],
            targetFid: idstr(2),
            currentFid: idstr(5),
            isSpam: true,
          }),
          models,
        ),
      ).toStrictEqual(t)
      done()
    })
  })

  it('should be serializable', (done) => {
    const models = MockModels(undefined, {
      folders: createMockInstance(Folders, {
        fetchFirstFidByType: jest.fn((type) =>
          resolve(
            ({
              [FolderType.inbox]: int64(1),
              [FolderType.spam]: int64(2),
            } as any)[type],
          ),
        ),
      }),
    })
    MarkSpamTask.create(1, int64(2), [int64(3), int64(4)], int64(5), true, models).then((t) => {
      expect(t.serialize()).toStrictEqual(
        JSONItemFromJSON({
          taskType: TaskType.spam,
          version: 1,
          uid: int64(2),
          mids: ['3', '4'],
          targetFid: '2',
          currentFid: '5',
          isSpam: true,
        }),
      )
      done()
    })
  })
  it('should return correct type based on the isSpam flag (spam)', (done) => {
    const models = MockModels(undefined, {
      folders: createMockInstance(Folders, {
        fetchFirstFidByType: jest.fn((type) =>
          resolve(
            ({
              [FolderType.inbox]: int64(1),
              [FolderType.spam]: int64(2),
            } as any)[type],
          ),
        ),
      }),
    })
    const taskSpam = MarkSpamTask.create(1, int64(2), [int64(3), int64(4)], int64(5), true, models)
    taskSpam.then((t) => {
      expect(t.getType()).toBe(TaskType.spam)
      done()
    })
  })
  it('should return correct type based on the isSpam flag (unspam)', (done) => {
    const models = MockModels(undefined, {
      folders: createMockInstance(Folders, {
        fetchFirstFidByType: jest.fn((type) =>
          resolve(
            ({
              [FolderType.inbox]: int64(1),
              [FolderType.spam]: int64(2),
            } as any)[type],
          ),
        ),
      }),
    })
    const taskSpam = MarkSpamTask.create(1, int64(2), [int64(3), int64(4)], int64(5), false, models)
    taskSpam.then((t) => {
      expect(t.getType()).toBe(TaskType.unspam)
      done()
    })
  })
  it('should execute MarkSpamNetworkRequest with correct params from sendDataToServer', (done) => {
    const models = MockModels(undefined, {
      folders: createMockInstance(Folders, {
        fetchFirstFidByType: jest.fn((type) =>
          resolve(
            ({
              [FolderType.inbox]: int64(1),
              [FolderType.spam]: int64(2),
            } as any)[type],
          ),
        ),
      }),
    })
    MarkSpamTask.create(1, int64(2), [int64(3), int64(4)], int64(5), true, models).then((t) => {
      jest.spyOn(t.models.network, 'execute').mockReturnValue(resolve(new MapJSONItem().putInt32('status', 1)))
      expect(t.getType()).toBe(TaskType.spam)
      expect(t.uid).toBe(int64(2))
      t.sendDataToServer().then((_) => {
        expect(t.models.network.execute).toBeCalledTimes(1)
        expect(t.models.network.execute).toBeCalledWith({
          currentFolderFid: int64(5),
          mids: [int64(3), int64(4)],
          isSpam: true,
          tids: [],
        })
        done()
      })
    })
  })
  it('should run proper database query on mark spam', (done) => {
    const models = MockModels(
      {
        taskActionsNotifier: {
          notifyTicketsAboutMoveToFolder: jest.fn(),
          notifyFTSMailAboutSendMailTaskSuccess: jest.fn(),
          notifyScheduleCheckAttachesInLastSend: jest.fn(),
          notifyUpdateNotifications: jest.fn(),
          notifyWidgetsForFolders: jest.fn(),
        },
      },
      {
        messages: createMockInstance(Messages, {
          resetMessagesTimestamp: jest.fn().mockReturnValue(resolve(getVoid())),
        }),
        folders: createMockInstance(Folders, {
          fetchFirstFidByType: jest.fn((type) =>
            resolve(
              ({
                [FolderType.inbox]: int64(1),
                [FolderType.spam]: int64(2),
              } as any)[type],
            ),
          ),
        }),
      },
    )
    const mids = [int64(2), int64(3)]
    const task = MarkSpamTask.create(1, int64(1), [int64(2), int64(3)], int64(4), true, models)
    task.then((t) => {
      const moveToFolderSpy = jest.spyOn(t, 'moveToFolder').mockReturnValue(resolve(getVoid()))
      const updateSearchShowForSpy = jest.spyOn(t, 'updateSearchShowFor').mockReturnValue(resolve(getVoid()))
      t.updateDatabase().then((_) => {
        expect(models.messages().resetMessagesTimestamp).toBeCalledWith(mids)
        expect(moveToFolderSpy).toBeCalledWith(int64(2))
        expect(updateSearchShowForSpy).toBeCalledWith(int64(2), int64(4))
        expect(models.taskActionsNotifier.notifyTicketsAboutMoveToFolder).toBeCalledWith(
          int64(1),
          mids,
          int64(4),
          int64(2),
        )
        moveToFolderSpy.mockRestore()
        updateSearchShowForSpy.mockRestore()
        done()
      })
    })
  })
  it('should run proper database query on mark unspam', (done) => {
    const models = MockModels(
      {
        taskActionsNotifier: {
          notifyTicketsAboutMoveToFolder: jest.fn(),
          notifyFTSMailAboutSendMailTaskSuccess: jest.fn(),
          notifyScheduleCheckAttachesInLastSend: jest.fn(),
          notifyUpdateNotifications: jest.fn(),
          notifyWidgetsForFolders: jest.fn(),
        },
      },
      {
        messages: createMockInstance(Messages, {
          resetMessagesTimestamp: jest.fn().mockReturnValue(resolve(getVoid())),
        }),
        folders: createMockInstance(Folders, {
          fetchFirstFidByType: jest.fn((type) =>
            resolve(
              ({
                [FolderType.inbox]: int64(1),
                [FolderType.spam]: int64(2),
              } as any)[type],
            ),
          ),
        }),
      },
    )
    const mids = [int64(2), int64(3)]
    const task = MarkSpamTask.create(1, int64(1), [int64(2), int64(3)], int64(1), false, models)
    task.then((t) => {
      const moveToFolderSpy = jest.spyOn(t, 'moveToFolder').mockReturnValue(resolve(getVoid()))
      const updateSearchShowForSpy = jest.spyOn(t, 'updateSearchShowFor').mockReturnValue(resolve(getVoid()))
      t.updateDatabase().then((_) => {
        expect(models.messages().resetMessagesTimestamp).toBeCalledWith(mids)
        expect(moveToFolderSpy).toBeCalledWith(int64(1))
        expect(updateSearchShowForSpy).toBeCalledWith(int64(1), int64(2))
        expect(models.taskActionsNotifier.notifyTicketsAboutMoveToFolder).toBeCalledWith(
          int64(1),
          mids,
          int64(2),
          int64(1),
        )
        moveToFolderSpy.mockRestore()
        updateSearchShowForSpy.mockRestore()
        done()
      })
    })
  })
})
