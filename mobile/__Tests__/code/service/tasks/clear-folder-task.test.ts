import { resolve } from '../../../../../../common/xpromise-support'
import { int64 } from '../../../../../../common/ys'
import { MockNetwork } from '../../../../../common/__tests__/__helpers__/mock-patches'
import { getVoid } from '../../../../../common/code/result/result'
import { JSONItemFromJSON } from '../../../../../common/__tests__/__helpers__/json-helpers'
import { ClearFolderNetworkRequest } from '../../../../../mapi/../mapi/code/api/entities/actions/clear-forlder-network-request'
import { NetworkStatus, NetworkStatusCode } from '../../../../../mapi/../mapi/code/api/entities/status/network-status'
import { createMockInstance } from '../../../../../common/__tests__/__helpers__/utils'
import { Folders } from '../../../../code/busilogics/folders/folders'
import { Messages } from '../../../../code/busilogics/messages/messages'
import { Cleanup } from '../../../../code/busilogics/sync/cleanup'
import { TaskType } from '../../../../code/service/task'
import { ClearFolderTask } from '../../../../code/service/tasks/clear-folder-task'
import { MockStorage } from '../../../__helpers__/mock-patches'
import { MockModels } from '../../../__helpers__/models'

describe(ClearFolderTask, () => {
  afterEach(jest.restoreAllMocks)
  it('should be deserializable', async () => {
    const models = MockModels()
    expect(
      await ClearFolderTask.fromJSONItem(
        JSONItemFromJSON({
          taskType: TaskType.clearFolder,
          version: 1,
          uid: '1111',
          fid: int64(11),
        }),
        models,
      ),
    ).toMatchObject(new (class extends ClearFolderTask {})(1, int64(1111), int64(11), models))
    expect(
      await ClearFolderTask.fromJSONItem(
        JSONItemFromJSON([
          {
            taskType: TaskType.clearFolder,
            version: 1,
            uid: '22222',
            fid: int64(8),
          },
        ]),
        models,
      ),
    ).toBeNull()

    expect(
      await ClearFolderTask.fromJSONItem(
        JSONItemFromJSON({
          fid: int64(43),
        }),
        models,
      ),
    ).toBeNull()

    expect(
      await ClearFolderTask.fromJSONItem(
        JSONItemFromJSON({
          taskType: TaskType.clearFolder,
          version: 1,
          uid: '3333',
        }),
        models,
      ),
    ).toBeNull()
  })
  it('should be serializable', () => {
    const models = MockModels()
    const task = new ClearFolderTask(1, int64(11223344), int64(22), models)
    task.getType = () => TaskType.clearFolder
    expect(task.serialize()).toStrictEqual(
      JSONItemFromJSON({
        taskType: TaskType.clearFolder,
        version: 1,
        uid: int64(11223344),
        fid: int64(22),
      }),
    )
  })
  it('should call ClearFolderNetworkRequest with correct params', (done) => {
    const models = MockModels({
      network: MockNetwork({
        execute: jest.fn().mockReturnValue(resolve(JSONItemFromJSON({ status: 1 }))),
      }),
    })
    const task = new ClearFolderTask(1, int64(2), int64(3), models)
    expect(task.getType()).toBe(TaskType.clearFolder)
    expect(task.uid).toBe(int64(2))
    task.sendDataToServer().then((res) => {
      expect(models.network.execute).toBeCalledWith(new ClearFolderNetworkRequest(int64(3)))
      expect(res).toStrictEqual(new NetworkStatus(NetworkStatusCode.ok))
      done()
    })
  })
  describe(ClearFolderTask.prototype.updateDatabase, () => {
    it('should delete messages in folder', (done) => {
      const storage = MockStorage()
      const models = MockModels({ storage })
      const task = new ClearFolderTask(1, int64(2), int64(3), models)
      const deleteMessagesInFolderSpy = jest.spyOn(task, 'deleteMessagesInFolder').mockReturnValue(resolve(getVoid()))
      task.updateDatabase().then((_) => {
        expect(deleteMessagesInFolderSpy).toBeCalled()
        done()
      })
    })
  })
  describe(ClearFolderTask.prototype.deleteMessagesInFolder, () => {
    // tslint:disable-next-line: max-line-length
    it('should delete messages by fid, for the rest – cleanup folder connections, counters, label and attach orphans', (done) => {
      const mids = [int64(1), int64(2)]
      const messages = createMockInstance(Messages, {
        deleteMessagesByFid: jest.fn().mockReturnValue(resolve(getVoid())),
        getAllMids: jest.fn().mockReturnValue(resolve(mids)),
      })
      const folders = createMockInstance(Folders, {
        cleanUpAllFolderMessagesConnection: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const cleanup = createMockInstance(Cleanup, {
        resetCountersForFolder: jest.fn().mockReturnValue(resolve(getVoid())),
        cleanUpLabelsOrphans: jest.fn().mockReturnValue(resolve(getVoid())),
        cleanupAttachOrphans: jest.fn().mockReturnValue(resolve(getVoid())),
        cleanupInlineAttachOrphans: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const targetFid = int64(3)
      const task = new ClearFolderTask(1, int64(2), targetFid, MockModels(undefined, { messages, folders, cleanup }))
      task.deleteMessagesInFolder().then((_) => {
        expect(messages.deleteMessagesByFid).toBeCalledWith(targetFid)
        expect(messages.getAllMids).toBeCalled()
        expect(folders.cleanUpAllFolderMessagesConnection).toBeCalledWith(targetFid)
        expect(cleanup.resetCountersForFolder).toBeCalledWith(targetFid)
        expect(cleanup.cleanUpLabelsOrphans).toBeCalledWith(mids)
        expect(cleanup.cleanupAttachOrphans).toBeCalledWith(mids)
        expect(cleanup.cleanupInlineAttachOrphans).toBeCalledWith(mids)
        done()
      })
    })
  })
})
