import { int64 } from '../../../../../common/ys'
import { JSONItemFromJSON } from '../../../../common/__tests__/__helpers__/json-helpers'
import { File } from '../../../../common/code/file-system/file'
import { DelayedTasks } from '../../../code/service/delayed-tasks'
import { TaskType } from '../../../code/service/task'
import { MockModels } from '../../__helpers__/models'
import { TestTask } from '../../__helpers__/test-task'

describe(DelayedTasks, () => {
  const mockModels = MockModels()
  describe(DelayedTasks.prototype.delayTask, () => {
    it('should add Task to internal delayedTaskMultiQueue queue', async () => {
      const delayedTasks = new DelayedTasks(jest.fn())

      const task = await TestTask.fromJSONItem(
        JSONItemFromJSON({
          taskType: TaskType.saveDraft.valueOf(),
          version: 1,
          uid: int64(12345),
        }),
        mockModels,
      )
      delayedTasks.delayTask(task!, new File('task1.task'))
      expect(delayedTasks.getFrozenAccounts()).toContain(int64(12345))
      const tasks = (delayedTasks as any).delayedTaskMultiQueue.get(int64(12345))
      expect(tasks).toHaveLength(1)
      const taskWrapper = tasks![0]
      expect(taskWrapper).not.toBeNull()
      expect(taskWrapper.type).toBe(TaskType.saveDraft)
      expect(taskWrapper.uid).toBe(int64(12345))
      expect(taskWrapper.file).not.toBeNull()
      expect(taskWrapper.file.name).toBe('task1.task')
    })
    it('should use existing key in delayedTaskMultiQueue', async () => {
      const delayedTasks = new DelayedTasks(jest.fn())
      const task = await TestTask.fromJSONItem(
        JSONItemFromJSON({
          taskType: TaskType.saveDraft.valueOf(),
          version: 1,
          uid: int64(12345),
        }),
        mockModels,
      )
      delayedTasks.delayTask(task!, new File('task1.task'))
      const task2 = await TestTask.fromJSONItem(
        JSONItemFromJSON({
          taskType: TaskType.delete.valueOf(),
          version: 1,
          uid: int64(12345),
        }),
        mockModels,
      )
      delayedTasks.delayTask(task2!, new File('task1.task'))
      expect(delayedTasks.getFrozenAccounts()).toContain(int64(12345))
      const tasks = (delayedTasks as any).delayedTaskMultiQueue.get(int64(12345))
      expect(tasks).toHaveLength(2)
    })
  })
  describe(DelayedTasks.prototype.getDelayedTasksForReloginedAccounts, () => {
    it('should return empty array if internal queue is empty', () => {
      const delayedTasks = new DelayedTasks(jest.fn())
      expect(delayedTasks.getDelayedTasksForReloginedAccounts()).toHaveLength(0)
    })
    it('should have the internal queue empty after being called, if relogined account uid is in queue', async () => {
      const delayedTasks = new DelayedTasks(jest.fn())
      const commonUid1 = int64(234)
      const commonUid2 = int64(12345)
      delayedTasks.setAuthorizedUids(new Set([int64(123), int64(678), commonUid1, commonUid2]))
      const task1 = await TestTask.fromJSONItem(
        JSONItemFromJSON({
          taskType: TaskType.saveDraft.valueOf(),
          version: 1,
          uid: commonUid1,
        }),
        mockModels,
      )!
      delayedTasks.delayTask(task1!, new File('task1.task'))
      const task2 = await TestTask.fromJSONItem(
        JSONItemFromJSON({
          taskType: TaskType.delete.valueOf(),
          version: 1,
          uid: commonUid2,
        }),
        mockModels,
      )!
      delayedTasks.delayTask(task2!, new File('task2.task'))
      expect(delayedTasks.getDelayedTasksForReloginedAccounts()).toHaveLength(2)
      expect((delayedTasks as any).delayedTaskMultiQueue.size).toBe(0)
    })
    it('should have the internal queue non-empty after being called, if relogined account is not in the queue', async () => {
      const delayedTasks = new DelayedTasks(jest.fn())
      const task1 = await TestTask.fromJSONItem(
        JSONItemFromJSON({
          taskType: TaskType.saveDraft.valueOf(),
          version: 1,
          uid: int64(7654321),
        }),
        mockModels,
      )
      delayedTasks.delayTask(task1!, new File('task1.task'))
      const task2 = await TestTask.fromJSONItem(
        JSONItemFromJSON({
          taskType: TaskType.delete.valueOf(),
          version: 1,
          uid: int64(8910),
        }),
        mockModels,
      )
      delayedTasks.delayTask(task2!, new File('task2.task'))
      expect(delayedTasks.getDelayedTasksForReloginedAccounts()).toHaveLength(0)
      expect((delayedTasks as any).delayedTaskMultiQueue.size).toBe(2)
    })
  })
  describe(DelayedTasks.prototype.accountIDsChanged, () => {
    it('should call onDelayedAccountsAuthorized if there are uids with delayed tasks', async () => {
      const callback = jest.fn()
      const tasks = new DelayedTasks(callback)
      tasks.accountIDsChanged(new Set([int64(1), int64(2)]))
      expect(callback).not.toBeCalled()

      callback.mockReset()
      tasks.delayTask(
        (await TestTask.fromJSONItem(
          JSONItemFromJSON({
            taskType: TaskType.delete.valueOf(),
            version: 1,
            uid: int64(1),
          }),
          mockModels,
        ))!,
        new File('task1'),
      )
      tasks.delayTask(
        (await TestTask.fromJSONItem(
          JSONItemFromJSON({
            taskType: TaskType.delete.valueOf(),
            version: 1,
            uid: int64(3),
          }),
          mockModels,
        ))!,
        new File('task2'),
      )
      tasks.setAuthorizedUids(new Set([int64(1), int64(3)]))
      tasks.accountIDsChanged(new Set([int64(1), int64(2)]))
      expect(callback).toBeCalledWith(new Set([int64(1)]))
    })
  })
  describe(DelayedTasks.prototype.stop, () => {
    it('should clear up delayed tasks from multiqueue', async () => {
      const tasks = new DelayedTasks(jest.fn())
      tasks.accountIDsChanged(new Set([int64(1), int64(2)]))
      tasks.delayTask(
        (await TestTask.fromJSONItem(
          JSONItemFromJSON({
            taskType: TaskType.delete.valueOf(),
            version: 1,
            uid: int64(1),
          }),
          mockModels,
        ))!,
        new File('task1'),
      )
      tasks.delayTask(
        (await TestTask.fromJSONItem(
          JSONItemFromJSON({
            taskType: TaskType.delete.valueOf(),
            version: 1,
            uid: int64(3),
          }),
          mockModels,
        ))!,
        new File('task2'),
      )
      expect(tasks.getFrozenAccounts().size).toBe(2)
      tasks.stop()
      expect(tasks.getFrozenAccounts().size).toBe(0)
    })
  })
})
