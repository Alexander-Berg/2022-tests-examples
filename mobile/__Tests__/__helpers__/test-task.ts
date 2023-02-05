import { resolve } from '../../../../common/xpromise-support'
import { int64, Nullable } from '../../../../common/ys'
import { JSONItem, MapJSONItem } from '../../../common/code/json/json-types'
import { XPromise } from '../../../common/code/promise/xpromise'
import { ID } from '../../../mapi/code/api/common/id'
import { NetworkStatus } from '../../../mapi/code/api/entities/status/network-status'
import { Models } from '../../code/models'
import { Task, TaskType, taskTypeFromInt32 } from '../../code/service/task'

export class TestTask extends Task {
  private type: TaskType = TaskType.delete

  public static fromJSONItem(value: JSONItem, models: Models): XPromise<Nullable<Task>> {
    return Task.fromJSONItem(value, models).then((inner) => {
      if (!inner) {
        return null
      }
      const type = taskTypeFromInt32((value as MapJSONItem).getInt32('taskType')!)!
      const result = new TestTask(inner.version, inner.uid, models)
      result.type = type
      return result
    })
  }
  public sendDataToServer(): XPromise<Nullable<NetworkStatus>> {
    return resolve<Nullable<NetworkStatus>>(null)
  }

  public getType(): TaskType {
    // As we do not have special type for tests, just use existing
    return this.type!
  }

  public getDraftId(): ID {
    return int64(-1)
  }
}
