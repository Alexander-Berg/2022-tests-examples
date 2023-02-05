import { resolve } from '../../../../common/xpromise-support'
import { Int32, int32ToInt64, int64, Int64, Nullable } from '../../../../common/ys'
import { JSONItem, MapJSONItem } from '../../../common/code/json/json-types'
import { XPromise } from '../../../common/code/promise/xpromise'
import { NetworkStatus } from '../../../mapi/code/api/entities/status/network-status'
import { Task, TaskType, taskTypeToInt32 } from '../../code/service/task'
import { MockModels } from './models'

export class CustomTypeTestTask extends Task {
  private draftID: Int64 = int64(-1)
  public constructor(
    version: Int32,
    uid: Int32,
    private readonly type: TaskType,
    values?: { readonly [name: string]: any },
  ) {
    super(version, int32ToInt64(uid), MockModels())
    if (values) {
      Object.assign(this, values)
    }
  }
  public getType(): TaskType {
    return this.type
  }
  public sendDataToServer(): XPromise<Nullable<NetworkStatus>> {
    return resolve(null)
  }

  public getAdditionalTasks(): readonly Task[] {
    return []
  }

  public serialize(): JSONItem {
    return new MapJSONItem()
      .putInt32('taskType', taskTypeToInt32(this.getType()))
      .putInt32('version', this.version)
      .putInt64('uid', this.uid)
  }

  public preUpdate(): XPromise<void> {
    throw new Error('Not implemented')
  }

  public postUpdate(): XPromise<void> {
    throw new Error('Not implemented')
  }

  public updateDatabase(): XPromise<void> {
    throw new Error('Not implemented')
  }

  public getDraftId(): Int64 {
    return this.draftID
  }
}
