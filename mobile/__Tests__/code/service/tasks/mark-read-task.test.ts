import { resolve } from '../../../../../../common/xpromise-support'
import { int64 } from '../../../../../../common/ys'
import { MockNetwork } from '../../../../../common/__tests__/__helpers__/mock-patches'
import { getVoid } from '../../../../../common/code/result/result'
import { JSONItemFromJSON } from '../../../../../common/__tests__/__helpers__/json-helpers'
import { MarkReadNetworkRequest } from '../../../../../mapi/../mapi/code/api/entities/actions/mark-read-network-request'
import { NetworkStatus, NetworkStatusCode } from '../../../../../mapi/../mapi/code/api/entities/status/network-status'
import { idToString } from '../../../../../mapi/code/api/common/id'
import { TaskType, taskTypeToInt32 } from '../../../../code/service/task'
import { MarkReadTask } from '../../../../code/service/tasks/mark-read-task'
import {
  idstr,
  makeCleanup,
  makeMessages,
  makeMessagesSettings,
  makeSearch,
  MockModels,
} from '../../../__helpers__/models'

describe(MarkReadTask, () => {
  afterEach(jest.restoreAllMocks)
  it('should have markRead type', () => {
    const messages = makeMessages()
    const taskRead = new MarkReadTask(1, int64(2), [int64(3), int64(4)], true, MockModels(undefined, { messages }))
    expect(taskRead.getType()).toStrictEqual(TaskType.markRead)
    const taskUnread = new MarkReadTask(1, int64(2), [int64(3), int64(4)], false, MockModels(undefined, { messages }))
    expect(taskUnread.getType()).toStrictEqual(TaskType.markUnread)
  })
  it('should send mark_read request to server if mark read', (done) => {
    const network = MockNetwork({
      execute: jest.fn().mockReturnValue(
        resolve(
          JSONItemFromJSON({
            status: 1,
          }),
        ),
      ),
    })
    const messages = makeMessages({ network })
    const mids = [int64(3), int64(4)]
    const taskRead = new MarkReadTask(1, int64(2), mids, true, MockModels(undefined, { messages }))
    taskRead.sendDataToServer().then((res) => {
      expect(network.execute).toBeCalledWith(new MarkReadNetworkRequest(mids, [], true))
      expect(res).toStrictEqual(new NetworkStatus(NetworkStatusCode.ok))
      done()
    })
  })
  it('should send mark_unread request to server if mark unread', (done) => {
    const network = MockNetwork({
      execute: jest.fn().mockReturnValue(
        resolve(
          JSONItemFromJSON({
            status: 1,
          }),
        ),
      ),
    })
    const messages = makeMessages({ network })
    const mids = [int64(3), int64(4)]
    const taskRead = new MarkReadTask(1, int64(2), mids, false, MockModels(undefined, { messages }))
    taskRead.sendDataToServer().then((res) => {
      expect(network.execute).toBeCalledWith(new MarkReadNetworkRequest(mids, [], false))
      expect(res).toStrictEqual(new NetworkStatus(NetworkStatusCode.ok))
      done()
    })
  })
  it('should update the database to with read/unread flag', (done) => {
    const cleanup = makeCleanup()
    const rebuildCountersSpy = jest.spyOn(cleanup, 'rebuildCounters').mockReturnValue(resolve(getVoid()))
    const messages = makeMessages({
      settings: makeMessagesSettings({ params: { cleanup } }),
    })

    const resetMessagesTimestampSpy = jest.spyOn(messages, 'resetMessagesTimestamp').mockReturnValue(resolve(getVoid()))
    const updateMessagesReadSpy = jest.spyOn(messages, 'updateMessagesRead').mockReturnValue(resolve(getVoid()))

    const search = makeSearch()
    const dropUnreadShowForSpy = jest.spyOn(search, 'dropUnreadShowFor').mockReturnValue(resolve(getVoid()))

    const mids = [int64(3), int64(4)]
    const taskRead = new MarkReadTask(1, int64(2), mids, true, MockModels(undefined, { messages, cleanup, search }))
    taskRead.updateDatabase().then((_) => {
      expect(resetMessagesTimestampSpy).toBeCalledWith(mids)
      expect(updateMessagesReadSpy).toBeCalledWith(mids, false)
      expect(rebuildCountersSpy).toBeCalledWith(mids)
      expect(dropUnreadShowForSpy).toBeCalledWith(mids)
      done()
    })
  })
  it('should serialize itself', () => {
    const messages = makeMessages()
    const mids = [int64(3), int64(4)]
    const taskRead = new MarkReadTask(1, int64(2), mids, true, MockModels(undefined, { messages }))
    expect(taskRead.serialize()).toStrictEqual(
      JSONItemFromJSON({
        taskType: taskTypeToInt32(TaskType.markRead),
        version: 1,
        uid: int64(2),
        mids: mids.map((id) => idToString(id)!),
        markRead: true,
      }),
    )
    ;(taskRead as any).affectedFids = new Set([int64(10), int64(20)])
    expect(taskRead.serialize()).toStrictEqual(
      JSONItemFromJSON({
        taskType: taskTypeToInt32(TaskType.markRead),
        version: 1,
        uid: int64(2),
        mids: mids.map((id) => idToString(id)!),
        markRead: true,
        affectedFids: [idstr(10), idstr(20)],
      }),
    )
  })
  it('should deserialize itself with affected folders IDs', async (done) => {
    const messages = makeMessages()
    const getFidsOfMessagesSpy = jest
      .spyOn(messages, 'getFidsOfMessages')
      .mockReturnValue(resolve([int64(10), int64(20)]))
    const models = MockModels(undefined, { messages })
    const task = (await MarkReadTask.fromJSONItem(
      JSONItemFromJSON({
        taskType: taskTypeToInt32(TaskType.markRead),
        version: 1,
        uid: int64(2),
        mids: [idstr(101), idstr(102)],
        markRead: true,
        affectedFids: [idstr(10), idstr(20)],
      }),
      models,
    )!) as MarkReadTask
    const expected = new MarkReadTask(1, int64(2), [int64(101), int64(102)], true, models)
    ;(expected as any).affectedFids = new Set([int64(10), int64(20)])
    expect(task).toStrictEqual(expected)
    task.getAffectedFolders().then((affectedFids) => {
      expect(affectedFids).toStrictEqual(new Set([int64(10), int64(20)]))
      expect(getFidsOfMessagesSpy).not.toBeCalled()
      done()
    })
  })
  it('should deserialize itself without affected folders IDs', async (done) => {
    const messages = makeMessages()
    const getFidsOfMessagesSpy = jest
      .spyOn(messages, 'getFidsOfMessages')
      .mockReturnValue(resolve([int64(10), int64(20)]))
    const models = MockModels(undefined, { messages })
    const task = (await MarkReadTask.fromJSONItem(
      JSONItemFromJSON({
        taskType: taskTypeToInt32(TaskType.markRead),
        version: 1,
        uid: int64(2),
        mids: [idstr(101), idstr(102)],
        markRead: true,
      }),
      models,
    )!) as MarkReadTask
    const expected = new MarkReadTask(1, int64(2), [int64(101), int64(102)], true, models)
    ;(expected as any).affectedFids = null
    expect(task).toStrictEqual(expected)
    task.getAffectedFolders().then((affectedFids) => {
      expect(affectedFids).toStrictEqual(new Set([int64(10), int64(20)]))
      expect(getFidsOfMessagesSpy).toBeCalledWith([int64(101), int64(102)])
      done()
    })
  })
  it('should return null when deserializing if JSON is malformed', async () => {
    expect(
      await MarkReadTask.fromJSONItem(
        JSONItemFromJSON([
          {
            taskType: taskTypeToInt32(TaskType.markRead),
            version: 1,
            uid: int64(2),
            mids: [idstr(101), idstr(102)],
            markRead: true,
          },
        ]),
        MockModels(),
      ),
    ).toBeNull()
  })
})
