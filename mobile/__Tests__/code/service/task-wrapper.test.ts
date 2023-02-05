import { resolve } from '../../../../../common/xpromise-support'
import { Int64, int64, Nullable } from '../../../../../common/ys'
import { XPromise } from '../../../../common/code/promise/xpromise'
import { ConstantIDs, ID } from '../../../../mapi/../mapi/code/api/common/id'
import { NetworkStatus } from '../../../../mapi/../mapi/code/api/entities/status/network-status'
import { MailSendRequest } from '../../../../mapi/code/api/entities/draft/mail-send-request'
import { File } from '../../../../common/code/file-system/file'
import { Task, TaskType, taskTypeToInt32 } from '../../../code/service/task'
import { TaskWrapper } from '../../../code/service/task-wrapper'
import { MockModels } from '../../__helpers__/models'
import { TestTask } from '../../__helpers__/test-task'

describe(TaskWrapper, () => {
  describe(TaskWrapper.create, () => {
    it('should create from Task and File (not store/send task)', () => {
      const testTask = new TestTask(1, int64(123), MockModels())
      const file = new File('f1')
      const wrapper = TaskWrapper.create(testTask, file)
      expect(wrapper.uid).toBe(testTask.uid)
      expect(wrapper.type).toBe(taskTypeToInt32(testTask.getType()))
      expect(wrapper.draftID).toBe(ConstantIDs.noMessageID)
      expect(wrapper.file).toStrictEqual(file)
    })
    it('should create from Task and File (store/send task)', () => {
      const testTask = new (class extends Task {
        public readonly draftID: Int64 = int64(321)

        public getType(): TaskType {
          return TaskType.saveDraft
        }
        public sendDataToServer(): XPromise<Nullable<NetworkStatus>> {
          return resolve(null)
        }
        public sendToServerWithMailSendRequest(mailSendRequest: MailSendRequest): XPromise<Nullable<NetworkStatus>> {
          return resolve(null)
        }
        public getDraftId(): ID {
          return this.draftID
        }
      })(1, int64(123), MockModels())
      const file = new File('f1')
      const wrapper = TaskWrapper.create(testTask, file)
      expect(wrapper.uid).toBe(testTask.uid)
      expect(wrapper.type).toBe(testTask.getType())
      expect(wrapper.draftID).toBe(int64(321))
      expect(wrapper.file).toStrictEqual(file)
    })
  })
  describe('isStoreSendTask', () => {
    it('should return true of store type of task', () => {
      const testTask = new TestTask(1, int64(123), MockModels())
      testTask.getType = () => TaskType.saveDraft
      const wrapper = TaskWrapper.create(testTask, new File('f1'))
      expect(wrapper.isStoreSendTask()).toBe(true)
    })
    it('should return true of send type of task', () => {
      const testTask = new TestTask(1, int64(123), MockModels())
      testTask.getType = () => TaskType.sendMessage
      const wrapper = TaskWrapper.create(testTask, new File('f1'))
      expect(wrapper.isStoreSendTask()).toBe(true)
    })
  })
})
