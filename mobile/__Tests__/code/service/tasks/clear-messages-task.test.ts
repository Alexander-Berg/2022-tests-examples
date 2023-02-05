import { resolve } from '../../../../../../common/xpromise-support'
import { int64 } from '../../../../../../common/ys'
import { NullJSONItem } from '../../../../../common/code/json/json-types'
import { getVoid } from '../../../../../common/code/result/result'
import { FolderType } from '../../../../../mapi/../mapi/code/api/entities/folder/folder'
import { TaskType } from '../../../../code/service/task'
import { ClearMessagesTask } from '../../../../code/service/tasks/clear-messages-task'
import { makeFolders, makeMessages, MockModels } from '../../../__helpers__/models'

describe(ClearMessagesTask, () => {
  it('should return clearMessage type', () => {
    const task = new ClearMessagesTask(1, int64(123), [int64(1), int64(2)], MockModels())
    expect(task.getType()).toBe(TaskType.clearMessage)
  })
  it('should clear the messages if not in folder', (done) => {
    const folders = makeFolders()
    const messages = makeMessages()
    const deleteMessagesByMidsNotInFidSpy = jest
      .spyOn(messages, 'deleteMessagesByMidsNotInFid')
      .mockReturnValue(resolve(getVoid()))
    const fetchFirstFidByTypeSpy = jest.spyOn(folders, 'fetchFirstFidByType').mockReturnValue(resolve(int64(3)))
    const models = MockModels(undefined, { folders, messages })
    const task = new ClearMessagesTask(1, int64(123), [int64(1), int64(2)], models)
    const cleanupSpy = jest.spyOn(task, 'cleanup').mockReturnValue(resolve(getVoid()))
    task.updateDatabase().then((_) => {
      expect(fetchFirstFidByTypeSpy).toBeCalledWith(FolderType.outgoing)
      expect(deleteMessagesByMidsNotInFidSpy).toBeCalledWith([int64(1), int64(2)], int64(3))
      expect(cleanupSpy).toBeCalled()
      deleteMessagesByMidsNotInFidSpy.mockRestore()
      fetchFirstFidByTypeSpy.mockRestore()
      cleanupSpy.mockRestore()
      done()
    })
  })
  it('should return immediatelly if no messages to process', (done) => {
    const folders = makeFolders()
    const fetchFirstFidByTypeSpy = jest.spyOn(folders, 'fetchFirstFidByType').mockReturnValue(resolve(int64(3)))
    const task = new ClearMessagesTask(1, int64(123), [], MockModels(undefined, { folders }))
    task.updateDatabase().then((_) => {
      expect(fetchFirstFidByTypeSpy).not.toBeCalled()
      fetchFirstFidByTypeSpy.mockRestore()
      done()
    })
  })
  it('should not be serializable', () => {
    const task = new ClearMessagesTask(1, int64(123), [int64(1), int64(2)], MockModels())
    expect(() => task.serialize()).toThrowError('The operation is not supported')
  })
  it('should not be deserializable', () => {
    expect(() => ClearMessagesTask.fromJSONItem(new NullJSONItem(), MockModels())).toThrowError(
      'The operation is not supported',
    )
  })
})
