import { resolve } from '../../../../../../common/xpromise-support'
import { int64 } from '../../../../../../common/ys'
import { getVoid } from '../../../../../common/code/result/result'
import { JSONItemFromJSON } from '../../../../../common/__tests__/__helpers__/json-helpers'
import { NetworkStatus, NetworkStatusCode } from '../../../../../mapi/../mapi/code/api/entities/status/network-status'
import { createMockInstance } from '../../../../../common/__tests__/__helpers__/utils'
import { Messages } from '../../../../code/busilogics/messages/messages'
import { TaskType } from '../../../../code/service/task'
import { MoveToFolderTask } from '../../../../code/service/tasks/move-to-folder-task'
import { MockTaskActionsNotifier } from '../../../__helpers__/mock-patches'
import { idstr, makeMessages, makeSearch, MockModels } from '../../../__helpers__/models'

describe(MoveToFolderTask, () => {
  it('should be serializable', () => {
    const res = new MoveToFolderTask(1, int64(2), [int64(3), int64(4)], int64(5), int64(6), MockModels())
    expect(res.serialize()).toStrictEqual(
      JSONItemFromJSON({
        taskType: TaskType.moveToFolder,
        version: 1,
        uid: int64(2),
        mids: [idstr(3), idstr(4)],
        targetFid: idstr(5),
        currentFid: idstr(6),
      }),
    )
  })

  it('should be deserializable', async () => {
    const models = MockModels()

    expect(
      await MoveToFolderTask.fromJSONItem(
        JSONItemFromJSON({
          taskType: TaskType.moveToFolder,
          version: 1,
          uid: '2',
          mids: [idstr(3), idstr(4)],
          targetFid: idstr(5),
          currentFid: idstr(6),
        }),
        models,
      ),
    ).toMatchObject(new MoveToFolderTask(1, int64(2), [int64(3), int64(4)], int64(5), int64(6), models))

    expect(
      await MoveToFolderTask.fromJSONItem(
        JSONItemFromJSON([
          {
            taskType: TaskType.moveToFolder,
            version: 1,
            uid: '2',
            mids: [idstr(3), idstr(4)],
            targetFid: idstr(5),
            currentFid: idstr(6),
          },
        ]),
        models,
      ),
    ).toBeNull()
  })

  it('should be re-serializable', async () => {
    const models = MockModels()
    const task = new MoveToFolderTask(1, int64(2), [int64(3), int64(4)], int64(5), int64(6), models)
    const json = task.serialize()
    const reserialized = await MoveToFolderTask.fromJSONItem(json, models)
    expect(task).toMatchObject(reserialized!)
  })

  it('should return type', () => {
    const task = new MoveToFolderTask(1, int64(2), [int64(3), int64(4)], int64(5), int64(6), MockModels())
    expect(task.getType()).toBe(TaskType.moveToFolder)
  })

  it('should return affected folders with target fid once', (done) => {
    const messages = makeMessages()
    const getFidsOfMessagesSpy = jest.spyOn(messages, 'getFidsOfMessages').mockReturnValue(resolve([int64(6)]))
    const mids = [int64(3), int64(4)]
    const models = MockModels(undefined, { messages })
    const task = new MoveToFolderTask(1, int64(2), [int64(3), int64(4)], int64(5), int64(6), models)
    task.getAffectedFolders().then((res1) => {
      expect(res1).toStrictEqual(new Set([int64(5), int64(6)]))
      expect(getFidsOfMessagesSpy).toBeCalledWith(mids)
      getFidsOfMessagesSpy.mockClear()
      task.getAffectedFolders().then((res2) => {
        expect(getFidsOfMessagesSpy).not.toBeCalled()
        expect(res2).toStrictEqual(new Set([int64(5), int64(6)]))
        done()
      })
    })
  })

  it('should send request to server', (done) => {
    const messages = createMockInstance(Messages, {
      sendMoveToFolderAction: jest.fn().mockReturnValue(resolve(new NetworkStatus(NetworkStatusCode.ok))),
    })
    const models = MockModels(undefined, { messages })

    const task = new MoveToFolderTask(1, int64(2), [int64(3), int64(4)], int64(5), int64(6), models)
    expect.assertions(2)
    task.sendDataToServer().then((res) => {
      expect(messages.sendMoveToFolderAction).toBeCalledWith([int64(3), int64(4)], int64(5), int64(6))
      expect(res).toStrictEqual(new NetworkStatus(NetworkStatusCode.ok))
      done()
    })
  })

  it('should update database', (done) => {
    const search = makeSearch()
    const getSearchResultsForFolderInMidsSpy = jest
      .spyOn(search, 'getSearchResultsForFolderInMids')
      .mockReturnValue(resolve([int64(30), int64(40)]))
    const updateMessagesShowForSpy = jest.spyOn(search, 'updateMessagesShowFor').mockReturnValue(resolve(getVoid()))

    const messages = createMockInstance(Messages, {
      moveMessages: jest.fn().mockReturnValue(resolve(getVoid())),
      resetMessagesTimestamp: jest.fn().mockReturnValue(resolve(getVoid())),
    })

    const taskActionsNotifier = MockTaskActionsNotifier()

    const models = MockModels({ taskActionsNotifier }, { search, messages })
    const task = new MoveToFolderTask(1, int64(2), [int64(3), int64(4)], int64(5), int64(6), models)

    expect.assertions(5)
    task.updateDatabase().then((_) => {
      expect(messages.resetMessagesTimestamp).toBeCalledWith([int64(3), int64(4)])
      expect(messages.moveMessages).toBeCalledWith([int64(3), int64(4)], int64(5), int64(6))
      expect(getSearchResultsForFolderInMidsSpy).toBeCalledWith(int64(6), [int64(3), int64(4)])
      expect(updateMessagesShowForSpy).toBeCalledWith('s_f_5', [int64(30), int64(40)])
      expect(taskActionsNotifier.notifyTicketsAboutMoveToFolder).toBeCalledWith(
        int64(2),
        [int64(3), int64(4)],
        int64(6),
        int64(5),
      )
      done()
    })
  })
})
