import { resolve } from '../../../../../../common/xpromise-support'
import { int64 } from '../../../../../../common/ys'
import { MockJSONSerializer, MockNetwork } from '../../../../../common/__tests__/__helpers__/mock-patches'
import { getVoid } from '../../../../../common/code/result/result'
import { JSONItemFromJSON } from '../../../../../common/__tests__/__helpers__/json-helpers'
import { ConstantIDs } from '../../../../../mapi/../mapi/code/api/common/id'
import { NetworkStatus, NetworkStatusCode } from '../../../../../mapi/../mapi/code/api/entities/status/network-status'
import { createMockInstance } from '../../../../../common/__tests__/__helpers__/utils'
import { Folders } from '../../../../code/busilogics/folders/folders'
import { Messages } from '../../../../code/busilogics/messages/messages'
import { Cleanup } from '../../../../code/busilogics/sync/cleanup'
import { TaskType, taskTypeToInt32 } from '../../../../code/service/task'
import { PurgeTask } from '../../../../code/service/tasks/purge-task'
import { MockStorage } from '../../../__helpers__/mock-patches'
import { idstr, makeMessagesSettings, MockModels } from '../../../__helpers__/models'
import { TestIDSupport } from '../../../__helpers__/test-id-support'

describe(PurgeTask, () => {
  it('should be serializable', () => {
    const res = new PurgeTask(2, int64(12345), [int64(123), int64(456)], MockModels())
    expect(res.serialize()).toStrictEqual(
      JSONItemFromJSON({
        taskType: taskTypeToInt32(TaskType.purge),
        version: 2,
        uid: int64(12345),
        mids: [idstr(123), idstr(456)],
      }),
    )
  })
  it('should be deserializable', async () => {
    const jsonItem = JSONItemFromJSON({
      taskType: taskTypeToInt32(TaskType.purge),
      version: 2,
      uid: int64(12345),
    })
    const models = MockModels()
    const res = await PurgeTask.fromJSONItem(jsonItem, models)
    expect(res!).toStrictEqual(new PurgeTask(2, int64(12345), [], models))
  })
  it('should return null if cannot be deserialized', async () => {
    const jsonItem = JSONItemFromJSON([
      {
        taskType: taskTypeToInt32(TaskType.purge),
        version: 2,
        uid: int64(12345),
      },
    ])
    expect(await PurgeTask.fromJSONItem(jsonItem, MockModels())).toBeNull()
  })
  it('should send delete_items request to server', (done) => {
    const messages = new Messages(
      MockNetwork(),
      MockStorage(),
      MockJSONSerializer(),
      new TestIDSupport(),
      makeMessagesSettings(),
    )
    const spySendDeleteMessagesInFolderAction = jest
      .spyOn(messages, 'sendDeleteMessagesInFolderAction')
      .mockReturnValue(resolve(new NetworkStatus(NetworkStatusCode.ok)))

    const mids = [int64(3), int64(4)]
    const purgeTask = new PurgeTask(1, int64(2), mids, MockModels(undefined, { messages }))
    purgeTask.sendDataToServer().then((res) => {
      expect(spySendDeleteMessagesInFolderAction).toBeCalledWith(mids, ConstantIDs.noFolderID)
      done()
    })
  })
  it('should perform database update', (done) => {
    const messages = createMockInstance(Messages, {
      resetMessagesTimestamp: jest.fn().mockReturnValue(resolve(getVoid())),
      deleteMessages: jest.fn().mockReturnValue(resolve(getVoid())),
    })
    const folders = createMockInstance(Folders, {
      cleanUpFolderMessagesConnection: jest.fn().mockReturnValue(resolve(getVoid())),
    })
    const cleanup = createMockInstance(Cleanup, {
      removeOrphans: jest.fn().mockReturnValue(resolve(getVoid())),
      rebuildAggregates: jest.fn().mockReturnValue(resolve(getVoid())),
    })
    const models = MockModels(undefined, { messages, folders, cleanup })
    const task = new PurgeTask(1, int64(2), [int64(3), int64(4)], models)

    task.updateDatabase().then((_) => {
      expect(messages.deleteMessages).toBeCalledWith([int64(3), int64(4)], false)
      expect(folders.cleanUpFolderMessagesConnection).toBeCalledWith([int64(3), int64(4)])
      expect(cleanup.removeOrphans).toBeCalledWith([int64(3), int64(4)])
      expect(cleanup.rebuildAggregates).toBeCalledWith([int64(3), int64(4)])
      expect(messages.resetMessagesTimestamp).toBeCalledWith([int64(3), int64(4)])
      done()
    })
  })
})
