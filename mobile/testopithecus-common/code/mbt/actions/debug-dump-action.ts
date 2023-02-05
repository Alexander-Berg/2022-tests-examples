import { Throwing } from '../../../../../common/ys'
import { Log } from '../../../../common/code/logging/logger'
import { EventusEvent } from '../../../../eventus-common/code/eventus-event'
import { App, FeatureID, MBTAction, MBTActionType, MBTComponent, MBTHistory } from '../mbt-abstractions'

export class DebugDumpAction implements MBTAction {
  public static readonly type: MBTActionType = 'DebugDump'

  public supported(_modelFeatures: FeatureID[], _applicationFeatures: FeatureID[]): boolean {
    return true
  }

  public canBePerformed(_model: App): boolean {
    return true
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    const expected = await model.dump(model)
    const actual = await application.dump(model)
    Log.info(`DEBUG DUMP\nMODEL\n${expected}APPLICATION\n${actual}`)
    return history.currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'DEBUG DUMP'
  }

  public getActionType(): MBTActionType {
    return DebugDumpAction.type
  }
}
