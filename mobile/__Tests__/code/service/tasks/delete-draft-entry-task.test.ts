import { resolve } from '../../../../../../common/xpromise-support'
import { int64 } from '../../../../../../common/ys'
import { MockNetwork } from '../../../../../common/__tests__/__helpers__/mock-patches'
import { getVoid } from '../../../../../common/code/result/result'
import { JSONItemFromJSON } from '../../../../../common/__tests__/__helpers__/json-helpers'
import { NetworkStatus, NetworkStatusCode } from '../../../../../mapi/../mapi/code/api/entities/status/network-status'
import { createMockInstance } from '../../../../../common/__tests__/__helpers__/utils'
import { Drafts } from '../../../../code/busilogics/draft/drafts'
import { TaskType } from '../../../../code/service/task'
import { DeleteDraftEntryTask } from '../../../../code/service/tasks/delete-draft-entry-task'
import { MockStorage } from '../../../__helpers__/mock-patches'
import { idstr, MockModels } from '../../../__helpers__/models'

describe(DeleteDraftEntryTask, () => {
  it('should be serializable', () => {
    const res = new DeleteDraftEntryTask(1, int64(2), int64(3), int64(4), MockModels())
    expect(res.serialize()).toStrictEqual(
      JSONItemFromJSON({
        taskType: TaskType.deleteDraftEntry,
        version: 1,
        uid: int64(2),
        draftID: idstr(3),
        revision: int64(4),
      }),
    )
  })

  it('should be deserializable', async () => {
    const models = MockModels()

    expect(
      await DeleteDraftEntryTask.fromJSONItem(
        JSONItemFromJSON({
          taskType: TaskType.deleteDraftEntry,
          version: 1,
          uid: '2',
          draftID: '3',
          revision: '4',
        }),
        models,
      ),
    ).toMatchObject(new DeleteDraftEntryTask(1, int64(2), int64(3), int64(4), models))

    expect(
      await DeleteDraftEntryTask.fromJSONItem(
        JSONItemFromJSON([
          {
            taskType: TaskType.deleteDraftEntry,
            draftID: '3',
            revision: '4',
          },
        ]),
        models,
      ),
    ).toBeNull()
  })
  it('should return type', () => {
    const task = new DeleteDraftEntryTask(1, int64(2), int64(3), int64(4), MockModels())
    expect(task.getType()).toBe(TaskType.deleteDraftEntry)
  })

  it('should not send request to server', (done) => {
    const network = MockNetwork()
    const models = MockModels({ network })
    const task = new DeleteDraftEntryTask(1, int64(2), int64(3), int64(4), models)
    task.sendDataToServer().then((res) => {
      expect(network.execute).not.toBeCalled()
      expect(res).toStrictEqual(new NetworkStatus(NetworkStatusCode.ok))
      done()
    })
  })
  it('should do nothing on database part', (done) => {
    const storage = MockStorage()
    const models = MockModels({ storage })
    const task = new DeleteDraftEntryTask(1, int64(2), int64(3), int64(4), models)
    task.updateDatabase().then((_) => {
      expect(storage.runQuery).not.toBeCalled()
      expect(storage.prepareStatement).not.toBeCalled()
      expect(storage.runStatement).not.toBeCalled()
      done()
    })
  })
  it('should perform database update on network part', (done) => {
    const drafts = createMockInstance(Drafts, {
      deleteDraftEntryIfActual: jest.fn().mockReturnValue(resolve(getVoid())),
    })
    const models = MockModels(undefined, { drafts })
    const task = new DeleteDraftEntryTask(1, int64(2), int64(3), int64(4), models)
    task.sendDataToServer().then((_) => {
      expect(drafts.deleteDraftEntryIfActual).toBeCalledWith(int64(3), int64(4))
      done()
    })
  })
})
