import { resolve } from '../../../../../../common/xpromise-support'
import { int64 } from '../../../../../../common/ys'
import { MapJSONItem } from '../../../../../common/code/json/json-types'
import { JSONItemFromJSON } from '../../../../../common/__tests__/__helpers__/json-helpers'
import { NetworkStatusCode } from '../../../../../mapi/../mapi/code/api/entities/status/network-status'
import { TaskType } from '../../../../code/service/task'
import { SaveSignatureTask } from '../../../../code/service/tasks/save-signature-task'
import { MockModels } from '../../../__helpers__/models'

describe(SaveSignatureTask, () => {
  afterEach(jest.restoreAllMocks)
  it('should be deserializable', async () => {
    const models = MockModels()
    expect(
      await SaveSignatureTask.fromJSONItem(
        JSONItemFromJSON({
          taskType: TaskType.saveSignature,
          version: 1,
          uid: '1111',
          signature: 'Sent from Xmail',
        }),
        models,
      ),
    ).toMatchObject(new (class extends SaveSignatureTask {})(1, int64(1111), 'Sent from Xmail', models))

    expect(
      await SaveSignatureTask.fromJSONItem(
        JSONItemFromJSON([
          {
            taskType: TaskType.saveSignature,
            version: 1,
            uid: '22222',
            signature: 'Sent from Xmail',
          },
        ]),
        models,
      ),
    ).toBeNull()

    expect(
      await SaveSignatureTask.fromJSONItem(
        JSONItemFromJSON({
          signature: 'Sent from Xmail',
        }),
        models,
      ),
    ).toBeNull()

    expect(
      await SaveSignatureTask.fromJSONItem(
        JSONItemFromJSON({
          taskType: TaskType.saveSignature,
          version: 1,
          uid: '3333',
        }),
        models,
      ),
    ).toBeNull()
  })
  it('should be serializable', () => {
    const models = MockModels()
    const task = new SaveSignatureTask(1, int64(98789), 'new signature', models)
    task.getType = () => TaskType.saveSignature
    expect(task.serialize()).toStrictEqual(
      JSONItemFromJSON({
        taskType: TaskType.saveSignature,
        version: 1,
        uid: int64(98789),
        signature: 'new signature',
      }),
    )
  })
  it('should execute SetSettingsNetworkRequest with correct params from sendDataToServer', (done) => {
    const models = MockModels()
    const task = new SaveSignatureTask(1, int64(98789), 'new signature', models)
    jest.spyOn(task.models.network, 'execute').mockReturnValue(resolve(new MapJSONItem().putInt32('status', 1)))
    expect(task.getType()).toBe(TaskType.saveSignature)
    expect(task.uid).toBe(int64(98789))
    task.sendDataToServer().then((val) => {
      expect(task.models.network.execute).toBeCalledTimes(1)
      expect(task.models.network.execute).toBeCalledWith({
        signature: 'new signature',
      })
      done()
    })
  })
  it('should update signature in preferences if network response is ok', (done) => {
    const models = MockModels()
    const task = new SaveSignatureTask(1, int64(98789), 'new signature', models)
    task.getType = () => TaskType.saveSignature
    const updateSharedPrefsSpy = jest.spyOn(task, 'updateSharedPrefs')
    jest
      .spyOn(task.models.network, 'execute')
      .mockReturnValue(resolve(new MapJSONItem().putInt32('status', NetworkStatusCode.ok)))
    task.sendDataToServer().then((val) => {
      expect(updateSharedPrefsSpy).toBeCalledTimes(1)
      expect(updateSharedPrefsSpy).toBeCalledWith('new signature', false)
      done()
    })
  })
  it('should not update signature in preferences if network response is not ok', (done) => {
    const models = MockModels()
    const task = new SaveSignatureTask(1, int64(98789), 'new signature', models)
    const updateSharedPrefsSpy = jest.spyOn(task, 'updateSharedPrefs')
    jest
      .spyOn(task.models.network, 'execute')
      .mockReturnValue(resolve(new MapJSONItem().putInt32('status', NetworkStatusCode.permanentError)))
    task.sendDataToServer().finally(() => {
      expect(updateSharedPrefsSpy).not.toBeCalled()
      done()
    })
  })
})
