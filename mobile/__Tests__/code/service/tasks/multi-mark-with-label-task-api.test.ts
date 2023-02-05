import { resolve } from '../../../../../../common/xpromise-support'
import { int64 } from '../../../../../../common/ys'
import { JSONItemFromJSON } from '../../../../../common/__tests__/__helpers__/json-helpers'
import { NetworkStatus, NetworkStatusCode } from '../../../../../mapi/../mapi/code/api/entities/status/network-status'
import { createMockInstance } from '../../../../../common/__tests__/__helpers__/utils'
import { Messages } from '../../../../code/busilogics/messages/messages'
import { TaskType } from '../../../../code/service/task'
import { MultiMarkWithLabelTaskApi } from '../../../../code/service/tasks/multi-mark-with-label-task-api'
import { idstr, MockModels } from '../../../__helpers__/models'

describe(MultiMarkWithLabelTaskApi, () => {
  it('should be deserializable', async () => {
    const models = MockModels()
    expect(
      await MultiMarkWithLabelTaskApi.fromJSONItem(
        JSONItemFromJSON({
          taskType: TaskType.multiMarkWithLabel,
          version: 1,
          uid: '2',
          mids: [idstr(3), idstr(4)],
          lids: ['lbl_1', 'lbl_2'],
          mark: true,
        }),
        models,
      ),
    ).toMatchObject(new MultiMarkWithLabelTaskApi(1, int64(2), [int64(3), int64(4)], ['lbl_1', 'lbl_2'], true, models))

    expect(
      await MultiMarkWithLabelTaskApi.fromJSONItem(
        JSONItemFromJSON({
          taskType: TaskType.multiMarkWithLabel,
          version: 1,
          uid: '2',
          mids: [],
          lids: [],
          mark: true,
        }),
        models,
      ),
    ).toMatchObject(new MultiMarkWithLabelTaskApi(1, int64(2), [], [], true, models))

    expect(
      await MultiMarkWithLabelTaskApi.fromJSONItem(
        JSONItemFromJSON({
          taskType: TaskType.multiMarkWithLabel,
          version: 1,
          uid: '2',
          lids: [],
          mark: true,
        }),
        models,
      ),
    ).toBeNull()

    expect(
      await MultiMarkWithLabelTaskApi.fromJSONItem(
        JSONItemFromJSON({
          taskType: TaskType.multiMarkWithLabel,
          version: 1,
          uid: '2',
          mids: [],
          mark: true,
        }),
        models,
      ),
    ).toBeNull()

    expect(
      await MultiMarkWithLabelTaskApi.fromJSONItem(
        JSONItemFromJSON([
          {
            taskType: TaskType.multiMarkWithLabel,
            version: 1,
            uid: '2',
            mids: [idstr(3), idstr(4)],
            lids: ['lbl_1', 'lbl_2'],
            mark: true,
          },
        ]),
        models,
      ),
    ).toBeNull()

    expect(
      await MultiMarkWithLabelTaskApi.fromJSONItem(
        JSONItemFromJSON({
          taskType: TaskType.multiMarkWithLabel,
          version: 1,
          uid: '2',
          mids: [idstr(3), idstr(4)],
          lids: ['lbl_1', 'lbl_2'],
        }),
        models,
      ),
    ).toBeNull()
  })

  it('should be serializable', () => {
    const models = MockModels()
    const task = new MultiMarkWithLabelTaskApi(1, int64(2), [int64(3), int64(4)], ['lbl_1', 'lbl_2'], true, models)
    expect(task.serialize()).toStrictEqual(
      JSONItemFromJSON({
        taskType: TaskType.multiMarkWithLabel,
        version: 1,
        uid: int64(2),
        mids: [idstr(3), idstr(4)],
        lids: ['lbl_1', 'lbl_2'],
        mark: true,
      }),
    )
  })

  it('should be re-serializable', async () => {
    const models = MockModels()
    const task = new MultiMarkWithLabelTaskApi(1, int64(2), [int64(3), int64(4)], ['lbl_1', 'lbl_2'], true, models)
    const json = task.serialize()
    const reserialized = await MultiMarkWithLabelTaskApi.fromJSONItem(json, models)
    expect(task).toMatchObject(reserialized!)
  })

  it('should return type', () => {
    const models = MockModels()
    const task = new MultiMarkWithLabelTaskApi(1, int64(2), [int64(3), int64(4)], ['lbl_1', 'lbl_2'], true, models)
    expect(task.getType()).toBe(TaskType.multiMarkWithLabel)
  })

  it('should send request to server', (done) => {
    const messages = createMockInstance(Messages, {
      sendMarkWithLabelsAction: jest.fn().mockReturnValue(resolve(new NetworkStatus(NetworkStatusCode.ok))),
    })
    const models = MockModels(undefined, { messages })

    const task = new MultiMarkWithLabelTaskApi(1, int64(2), [int64(3), int64(4)], ['lbl_1', 'lbl_2'], true, models)
    expect.assertions(2)
    task.sendDataToServer().then((res) => {
      expect(messages.sendMarkWithLabelsAction).toBeCalledWith([int64(3), int64(4)], ['lbl_1', 'lbl_2'], true)
      expect(res).toStrictEqual(new NetworkStatus(NetworkStatusCode.ok))
      done()
    })
  })
})
