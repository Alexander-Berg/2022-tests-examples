import { resolve } from '../../../../../../common/xpromise-support'
import { int64 } from '../../../../../../common/ys'
import { getVoid } from '../../../../../common/code/result/result'
import { JSONItemFromJSON } from '../../../../../common/__tests__/__helpers__/json-helpers'
import { NetworkStatus, NetworkStatusCode } from '../../../../../mapi/../mapi/code/api/entities/status/network-status'
import { createMockInstance } from '../../../../../common/__tests__/__helpers__/utils'
import { Labels } from '../../../../code/busilogics/labels/labels'
import { Messages } from '../../../../code/busilogics/messages/messages'
import { SearchModel } from '../../../../code/busilogics/search/search-model'
import { TaskType } from '../../../../code/service/task'
import { MarkWithLabelTask } from '../../../../code/service/tasks/mark-with-label-task'
import { MockTaskActionsNotifier } from '../../../__helpers__/mock-patches'
import { idstr, MockModels } from '../../../__helpers__/models'

describe(MarkWithLabelTask, () => {
  it('should be deserializable', async () => {
    const models = MockModels()
    expect(
      await MarkWithLabelTask.fromJSONItem(
        JSONItemFromJSON({
          taskType: TaskType.markWithLabel,
          version: 1,
          uid: '2',
          mids: [idstr(3), idstr(4)],
          mark: true,
          lid: 'label',
        }),
        models,
      ),
    ).toMatchObject(new MarkWithLabelTask(1, int64(2), [int64(3), int64(4)], true, 'label', models))

    expect(
      await MarkWithLabelTask.fromJSONItem(
        JSONItemFromJSON([
          {
            taskType: TaskType.markWithLabel,
            version: 1,
            uid: '2',
            mids: [idstr(3), idstr(4)],
            mark: true,
            lid: 'label',
          },
        ]),
        models,
      ),
    ).toBeNull()

    expect(
      await MarkWithLabelTask.fromJSONItem(
        JSONItemFromJSON({
          taskType: TaskType.markWithLabel,
          version: 1,
          uid: '2',
          mids: [idstr(3), idstr(4)],
          lid: 'label',
        }),
        models,
      ),
    ).toBeNull()

    expect(
      await MarkWithLabelTask.fromJSONItem(
        JSONItemFromJSON({
          taskType: TaskType.markWithLabel,
          version: 1,
          uid: '2',
          mark: true,
          lid: 'label',
        }),
        models,
      ),
    ).toBeNull()

    expect(
      await MarkWithLabelTask.fromJSONItem(
        JSONItemFromJSON({
          taskType: TaskType.markWithLabel,
          version: 1,
          uid: '2',
          mids: [idstr(3), idstr(4)],
          mark: true,
        }),
        models,
      ),
    ).toBeNull()
  })

  it('should be serializable', () => {
    const models = MockModels()
    const task = new MarkWithLabelTask(1, int64(2), [int64(3), int64(4)], true, 'label', models)
    expect(task.serialize()).toStrictEqual(
      JSONItemFromJSON({
        taskType: TaskType.markWithLabel,
        version: 1,
        uid: int64(2),
        mids: [idstr(3), idstr(4)],
        mark: true,
        lid: 'label',
      }),
    )
  })

  it('should be re-serializable', async () => {
    const models = MockModels()
    const task = new MarkWithLabelTask(1, int64(2), [int64(3), int64(4)], true, 'label', models)
    const json = task.serialize()
    const reserialized = await MarkWithLabelTask.fromJSONItem(json, models)
    expect(task).toMatchObject(reserialized!)
  })

  it('should return type', () => {
    const models = MockModels()
    const task = new MarkWithLabelTask(1, int64(2), [int64(3), int64(4)], true, 'label', models)
    expect(task.getType()).toBe(TaskType.markWithLabel)
  })

  it('should send request to server', (done) => {
    const messages = createMockInstance(Messages, {
      sendMarkWithLabelsAction: jest.fn().mockReturnValue(resolve(new NetworkStatus(NetworkStatusCode.ok))),
    })
    const models = MockModels(undefined, { messages })

    const task = new MarkWithLabelTask(1, int64(2), [int64(3), int64(4)], true, 'label', models)
    expect.assertions(2)
    task.sendDataToServer().then((res) => {
      expect(messages.sendMarkWithLabelsAction).toBeCalledWith([int64(3), int64(4)], ['label'], true)
      expect(res).toStrictEqual(new NetworkStatus(NetworkStatusCode.ok))
      done()
    })
  })

  it('should update database if mark with label', (done) => {
    const labels = createMockInstance(Labels, {
      markWithLabel: jest.fn().mockReturnValue(resolve(getVoid())),
    })
    const models = MockModels(undefined, { labels })

    const task = new MarkWithLabelTask(1, int64(2), [int64(3), int64(4)], true, 'label', models)
    expect.assertions(2)
    task.updateDatabase().then((res) => {
      expect(labels.markWithLabel).toBeCalledWith([int64(3), int64(4)], ['label'])
      expect(labels.unmarkWithLabels).not.toBeCalled()
      done()
    })
  })

  it('should update database if unmark with label', (done) => {
    const search = createMockInstance(SearchModel, {
      dropLabelsShowFor: jest.fn().mockReturnValue(resolve(getVoid())),
    })
    const labels = createMockInstance(Labels, {
      unmarkWithLabels: jest.fn().mockReturnValue(resolve(getVoid())),
    })
    const models = MockModels(undefined, { labels, search })

    const task = new MarkWithLabelTask(1, int64(2), [int64(3), int64(4)], false, 'label', models)
    expect.assertions(3)
    task.updateDatabase().then((res) => {
      expect(labels.unmarkWithLabels).toBeCalledWith([int64(3), int64(4)], ['label'])
      expect(search.dropLabelsShowFor).toBeCalledWith([int64(3), int64(4)], ['label'])
      expect(labels.markWithLabel).not.toBeCalled()
      done()
    })
  })

  it('should notify notification on post-update', (done) => {
    const messages = createMockInstance(Messages, {
      getFidsOfMessages: jest.fn().mockReturnValue(resolve([int64(103), int64(104)])),
    })
    const taskActionsNotifier = MockTaskActionsNotifier()
    const models = MockModels({ taskActionsNotifier }, { messages })

    const task = new MarkWithLabelTask(1, int64(2), [int64(3), int64(4)], true, 'label', models)

    expect.assertions(2)
    task.postUpdate().then((_) => {
      expect(messages.getFidsOfMessages).toBeCalledWith([int64(3), int64(4)])
      expect(taskActionsNotifier.notifyWidgetsForFolders).toBeCalledWith(int64(2), [int64(103), int64(104)])
      done()
    })
  })
})
