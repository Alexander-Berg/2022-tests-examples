import { resolve } from '../../../../../../common/xpromise-support'
import { int64, Int64, Nullable } from '../../../../../../common/ys'
import { JSONItem } from '../../../../../common/code/json/json-types'
import { getVoid } from '../../../../../common/code/result/result'
import { JSONItemFromJSON } from '../../../../../common/__tests__/__helpers__/json-helpers'
import { ID } from '../../../../../mapi/../mapi/code/api/common/id'
import { FolderSyncType } from '../../../../../mapi/../mapi/code/api/entities/folder/folder'
import { createMockInstance } from '../../../../../common/__tests__/__helpers__/utils'
import { Folders } from '../../../../code/busilogics/folders/folders'
import { TaskType } from '../../../../code/service/task'
import { MultiMessageTask } from '../../../../code/service/tasks/multi-message-task'
import { MockTaskActionsNotifier } from '../../../__helpers__/mock-patches'
import { idstr, makeMessages, MockModels } from '../../../__helpers__/models'

describe(MultiMessageTask, () => {
  afterEach(jest.restoreAllMocks)
  it('should be deserializable', async () => {
    const models = MockModels()
    expect(
      await MultiMessageTask.fromJSONItem(
        JSONItemFromJSON({
          taskType: TaskType.delete,
          version: 1,
          uid: '2',
          mids: [idstr(3), idstr(4)],
        }),
        models,
      ),
    ).toMatchObject(new (class extends MultiMessageTask {})(1, int64(2), [int64(3), int64(4)], models))

    expect(
      await MultiMessageTask.fromJSONItem(
        JSONItemFromJSON([
          {
            taskType: TaskType.delete,
            version: 1,
            uid: '2',
            mids: [idstr(3), idstr(4)],
          },
        ]),
        models,
      ),
    ).toBeNull()

    expect(
      await MultiMessageTask.fromJSONItem(
        JSONItemFromJSON({
          taskType: TaskType.delete,
          version: 1,
          uid: '2',
        }),
        models,
      ),
    ).toMatchObject(new (class extends MultiMessageTask {})(1, int64(2), [], models))
  })
  it('should deserialize affected folder IDs', () => {
    class Patched extends MultiMessageTask {
      public static deserializeAffectedFids_(jsonItem: JSONItem): Nullable<ReadonlySet<ID>> {
        return this.deserializeAffectedFids(jsonItem)
      }
    }
    expect(
      Patched.deserializeAffectedFids_(
        JSONItemFromJSON({
          taskType: TaskType.delete,
          version: 1,
          uid: '2',
          affectedFids: [idstr(1), idstr(2)],
        }),
      ),
    ).toStrictEqual(new Set([int64(1), int64(2)]))
    expect(
      Patched.deserializeAffectedFids_(
        JSONItemFromJSON({
          taskType: TaskType.delete,
          version: 1,
          uid: '2',
        }),
      ),
    ).toBeNull()
  })
  it('should be serializable', () => {
    const models = MockModels()
    const task = new (class extends MultiMessageTask {})(1, int64(2), [int64(3), int64(4)], models)
    task.getType = () => TaskType.delete
    expect(task.serialize()).toStrictEqual(
      JSONItemFromJSON({
        taskType: TaskType.delete,
        version: 1,
        uid: int64(2),
        mids: [idstr(3), idstr(4)],
      }),
    )
    ;(task as any).affectedFids = new Set<Int64>([int64(10), int64(20)])
    expect(task.serialize()).toStrictEqual(
      JSONItemFromJSON({
        taskType: TaskType.delete,
        version: 1,
        uid: int64(2),
        mids: [idstr(3), idstr(4)],
        affectedFids: [idstr(10), idstr(20)],
      }),
    )
  })
  it('should collect affected folders once', (done) => {
    const messages = makeMessages()
    const expected = [int64(1), int64(2)]
    const getFidsOfMessagesSpy = jest.spyOn(messages, 'getFidsOfMessages').mockReturnValue(resolve(expected))
    const mids = [int64(3), int64(4)]
    const models = MockModels(undefined, { messages })
    const task = new (class extends MultiMessageTask {})(1, int64(2), mids, models)
    task.getAffectedFolders().then((res1) => {
      expect(res1).toStrictEqual(new Set(expected))
      expect(getFidsOfMessagesSpy).toBeCalledWith(mids)
      getFidsOfMessagesSpy.mockClear()
      task.getAffectedFolders().then((res2) => {
        expect(getFidsOfMessagesSpy).not.toBeCalled()
        expect(res2).toStrictEqual(new Set(expected))
        done()
      })
    })
  })
  it('should update database by resetting timestamps', (done) => {
    const messages = makeMessages()
    const resetMessagesTimestampSpy = jest.spyOn(messages, 'resetMessagesTimestamp').mockReturnValue(resolve(getVoid()))
    const mids = [int64(3), int64(4)]
    const models = MockModels(undefined, { messages })
    const task = new (class extends MultiMessageTask {})(1, int64(2), mids, models)
    task.updateDatabase().then((_) => {
      expect(resetMessagesTimestampSpy).toBeCalledWith(mids)
      done()
    })
  })
  it('should update database by resetting timestamps', (done) => {
    const messages = makeMessages()
    const resetMessagesTimestampSpy = jest.spyOn(messages, 'resetMessagesTimestamp').mockReturnValue(resolve(getVoid()))
    const mids = [int64(3), int64(4)]
    const models = MockModels(undefined, { messages })
    const task = new (class extends MultiMessageTask {})(1, int64(2), mids, models)
    task.updateDatabase().then((_) => {
      expect(resetMessagesTimestampSpy).toBeCalledWith(mids)
      done()
    })
  })
  it('should notify notification and widget models on post-update', (done) => {
    const folders = createMockInstance(Folders, {
      getFoldersSyncType: jest.fn().mockReturnValue(
        resolve(
          new Map([
            [int64(1), FolderSyncType.pushSync],
            [int64(2), FolderSyncType.silentSync],
          ]),
        ),
      ),
    })
    const taskActionsNotifier = MockTaskActionsNotifier()
    const models = MockModels({ taskActionsNotifier }, { folders })

    const task = new (class extends MultiMessageTask {})(1, int64(2), [int64(3), int64(4)], models)
    jest.spyOn(task, 'getAffectedFolders').mockReturnValue(resolve(new Set([int64(1), int64(2)])))

    expect.assertions(3)
    task.postUpdate().then((_) => {
      expect(folders.getFoldersSyncType).toBeCalledWith(new Set([int64(1), int64(2)]))
      expect(taskActionsNotifier.notifyUpdateNotifications).toBeCalledWith(int64(2), [int64(1)])
      expect(taskActionsNotifier.notifyWidgetsForFolders).toBeCalledWith(int64(2), [int64(1), int64(2)])
      done()
    })
  })
})
