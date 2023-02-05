import { resolve } from '../../../../../../common/xpromise-support'
import { int64 } from '../../../../../../common/ys'
import { MapJSONItem } from '../../../../../common/code/json/json-types'
import { JSONItemFromJSON } from '../../../../../common/__tests__/__helpers__/json-helpers'
import {
  SetParametersItem,
  SetParametersItems,
  SetParametersRequest,
} from '../../../../../mapi/code/api/entities/actions/set-parameters-request'
import { NetworkStatus, NetworkStatusCode } from '../../../../../mapi/code/api/entities/status/network-status'
import { TaskType } from '../../../../code/service/task'
import { SetParametersTask } from '../../../../code/service/tasks/set-parameters-task'
import { MockModels } from '../../../__helpers__/models'

describe(SetParametersTask, () => {
  afterEach(jest.restoreAllMocks)
  it('should be deserializable', async () => {
    const models = MockModels()
    expect(
      await SetParametersTask.fromJSONItem(
        JSONItemFromJSON({
          taskType: TaskType.setParameters,
          version: 1,
          uid: '2',
          parameters: {
            param1: 'param1_value',
            param2: 'param2_value',
          },
        }),
        models,
      ),
    ).toMatchObject(
      new (class extends SetParametersTask {})(
        1,
        int64(2),
        new SetParametersItems([
          new SetParametersItem('param1', 'param1_value'),
          new SetParametersItem('param2', 'param2_value'),
        ]),
        models,
      ),
    )

    expect(
      await SetParametersTask.fromJSONItem(
        JSONItemFromJSON([
          {
            taskType: TaskType.setParameters,
            version: 1,
            uid: '2',
            parameters: {},
          },
        ]),
        models,
      ),
    ).toBeNull()

    expect(
      await SetParametersTask.fromJSONItem(
        JSONItemFromJSON({
          taskType: TaskType.setParameters,
          parameters: {},
        }),
        models,
      ),
    ).toBeNull()

    expect(
      await SetParametersTask.fromJSONItem(
        JSONItemFromJSON({
          taskType: TaskType.setParameters,
          version: 1,
          uid: '2',
          hello: {},
        }),
        models,
      ),
    ).toBeNull()

    expect(
      await SetParametersTask.fromJSONItem(
        JSONItemFromJSON({
          taskType: TaskType.setParameters,
          version: 1,
          uid: '2',
          parameters: 'hello',
        }),
        models,
      ),
    ).toBeNull()
  })
  it('should be serializable', () => {
    const models = MockModels()
    const task = new SetParametersTask(
      1,
      int64(2),
      new SetParametersItems([new SetParametersItem('key1', 'value1'), new SetParametersItem('key2', 'value2')]),
      models,
    )
    expect(task.serialize()).toStrictEqual(
      JSONItemFromJSON({
        taskType: TaskType.setParameters,
        version: 1,
        uid: int64(2),
        parameters: {
          key1: 'value1',
          key2: 'value2',
        },
      }),
    )
  })
  it('should execute SetParametersRequest with correct params from sendDataToServer', (done) => {
    const models = MockModels()
    const task = new SetParametersTask(
      1,
      int64(2),
      new SetParametersItems([new SetParametersItem('key1', 'value1'), new SetParametersItem('key2', 'value2')]),
      models,
    )
    jest.spyOn(task.models.network, 'execute').mockReturnValue(resolve(new MapJSONItem().putInt32('status', 1)))
    expect(task.getType()).toBe(TaskType.setParameters)
    expect(task.uid).toBe(int64(2))
    task.sendDataToServer().then((status) => {
      expect(task.models.network.execute).toBeCalledWith(new SetParametersRequest(task.parameters))
      expect(status).toStrictEqual(new NetworkStatus(NetworkStatusCode.ok))
      done()
    })
  })
})
