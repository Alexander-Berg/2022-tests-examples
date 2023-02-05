import { int64 } from '../../../../../common/ys'
import { JSONItemKind, MapJSONItem } from '../../../../common/code/json/json-types'
import { JSONItemFromJSON } from '../../../../common/__tests__/__helpers__/json-helpers'
import { Task, TaskType, taskTypeFromInt32, taskTypeToInt32 } from '../../../code/service/task'
import { MockModels } from '../../__helpers__/models'
import { TestTask } from '../../__helpers__/test-task'

describe(Task, () => {
  const models = MockModels()
  it('should deserialize from JSONItem', async () => {
    const task = await TestTask.fromJSONItem(
      JSONItemFromJSON({
        taskType: TaskType.delete.valueOf(),
        version: 1,
        uid: int64(12345),
      }),
      models,
    )
    expect(task).not.toBeNull()
    expect(task!.version).toBe(1)
    expect(task!.uid).toBe(int64(12345))
  })
  it('should return null if unable to deserialize from JSONItem (not MapJSONItem)', async () => {
    const task = await Task.fromJSONItem(
      JSONItemFromJSON([
        {
          taskType: TaskType.delete.valueOf(),
          version: 1,
          uid: int64(12345),
        },
      ]),
      models,
    )
    expect(task).toBeNull()
  })
  it('should serialize into JSONItem', () => {
    const task = new TestTask(1, int64(12345), models)
    const value = task.serialize()
    expect(value.kind).toBe(JSONItemKind.map)
    const map = value as MapJSONItem
    expect(map.getInt32('taskType')).toBe(task.getType())
    expect(map.getInt32('version')).toBe(task.version)
    expect(map.getInt64('uid')).toBe(task.uid)
  })
  it('should return additional tasks (empty array if none)', () => {
    const task = new TestTask(1, int64(12345), models)
    expect(task.getAdditionalTasks()).toHaveLength(0)
  })
  it('should do nothing on preUpdate', (done) => {
    const task = new TestTask(1, int64(12345), models)
    task.preUpdate().finally(done)
  })
  it('should do nothing on postUpdate', (done) => {
    const task = new TestTask(1, int64(12345), models)
    task.postUpdate().finally(done)
  })
  it('should do nothing on updateDatabase', (done) => {
    const task = new TestTask(1, int64(12345), models)
    task.updateDatabase().finally(done)
  })
  it('should throw on sendDataToServer', () => {
    const task = new (class extends Task {})(1, int64(12345), models)
    expect(() => task.sendDataToServer()).toThrowError('Not implemented')
  })
  it('should throw on getType', () => {
    const task = new (class extends Task {})(1, int64(12345), models)
    expect(() => task.getType()).toThrowError('Not implemented')
  })
})

describe(taskTypeFromInt32, () => {
  it('should create TaskType from Int32', () => {
    expect(taskTypeFromInt32(0)).toBe(TaskType.markRead)
    expect(taskTypeFromInt32(1)).toBe(TaskType.markUnread)
    expect(taskTypeFromInt32(2)).toBe(TaskType.delete)
    expect(taskTypeFromInt32(3)).toBe(TaskType.markWithLabel)
    expect(taskTypeFromInt32(4)).toBe(TaskType.moveToFolder)
    expect(taskTypeFromInt32(5)).toBe(TaskType.spam)
    expect(taskTypeFromInt32(6)).toBe(TaskType.unspam)
    expect(taskTypeFromInt32(7)).toBe(TaskType.archive)
    expect(taskTypeFromInt32(8)).toBe(TaskType.saveSignature)
    expect(taskTypeFromInt32(9)).toBe(TaskType.clearFolder)
    expect(taskTypeFromInt32(10)).toBe(TaskType.clearMessage)
    expect(taskTypeFromInt32(11)).toBe(TaskType.multiMarkWithLabelOffline)
    expect(taskTypeFromInt32(12)).toBe(TaskType.multiMarkWithLabel)
    expect(taskTypeFromInt32(13)).toBe(TaskType.deleteDraftEntry)
    expect(taskTypeFromInt32(14)).toBe(TaskType.sendMessage)
    expect(taskTypeFromInt32(15)).toBe(TaskType.saveDraft)
    expect(taskTypeFromInt32(16)).toBe(TaskType.uploadAttach)
    expect(taskTypeFromInt32(17)).toBe(TaskType.purge)
    expect(taskTypeFromInt32(18)).toBe(TaskType.setParameters)
  })
})

describe(taskTypeToInt32, () => {
  it('should create Int32 from TaskType', () => {
    expect(taskTypeToInt32(TaskType.markRead)).toBe(0)
    expect(taskTypeToInt32(TaskType.markUnread)).toBe(1)
    expect(taskTypeToInt32(TaskType.delete)).toBe(2)
    expect(taskTypeToInt32(TaskType.markWithLabel)).toBe(3)
    expect(taskTypeToInt32(TaskType.moveToFolder)).toBe(4)
    expect(taskTypeToInt32(TaskType.spam)).toBe(5)
    expect(taskTypeToInt32(TaskType.unspam)).toBe(6)
    expect(taskTypeToInt32(TaskType.archive)).toBe(7)
    expect(taskTypeToInt32(TaskType.saveSignature)).toBe(8)
    expect(taskTypeToInt32(TaskType.clearFolder)).toBe(9)
    expect(taskTypeToInt32(TaskType.clearMessage)).toBe(10)
    expect(taskTypeToInt32(TaskType.multiMarkWithLabelOffline)).toBe(11)
    expect(taskTypeToInt32(TaskType.multiMarkWithLabel)).toBe(12)
    expect(taskTypeToInt32(TaskType.deleteDraftEntry)).toBe(13)
    expect(taskTypeToInt32(TaskType.sendMessage)).toBe(14)
    expect(taskTypeToInt32(TaskType.saveDraft)).toBe(15)
    expect(taskTypeToInt32(TaskType.uploadAttach)).toBe(16)
    expect(taskTypeToInt32(TaskType.purge)).toBe(17)
    expect(taskTypeToInt32(TaskType.setParameters)).toBe(18)
  })
})
