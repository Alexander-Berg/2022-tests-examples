import { int64, Throwing } from '../../../../../common/ys'
import { EventusEvent } from '../../../../eventus-common/code/eventus-event'
import { UserLock } from '../../users/user-pool'
import { App, FeatureID, MBTAction, MBTActionType, MBTComponent, MBTHistory } from '../mbt-abstractions'

export class PingAccountLockAction implements MBTAction {
  public static readonly type: MBTActionType = 'PingAccountLock'

  public constructor(private accountLock: UserLock) {}

  public supported(_modelFeatures: FeatureID[], _applicationFeatures: FeatureID[]): boolean {
    return true
  }

  public canBePerformed(_model: App): boolean {
    return true
  }

  public async perform(_model: App, _application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    this.accountLock.ping(int64(30 * 1000))
    return history.currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'PingAccountLock'
  }

  public getActionType(): MBTActionType {
    return PingAccountLockAction.type
  }
}
