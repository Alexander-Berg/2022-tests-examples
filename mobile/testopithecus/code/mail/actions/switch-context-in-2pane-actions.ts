import { EventusEvent } from '../../../../eventus-common/code/eventus-event'
import {
  App,
  FeatureID,
  MBTAction,
  MBTActionType,
  MBTComponent,
  MBTHistory,
} from '../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { Throwing } from '../../../../../common/ys'
import { SwitchContext2PaneFeature } from '../feature/switch-context-in-2pane-feature'

export class SwitchContextToAction implements MBTAction {
  public static readonly type: MBTActionType = 'SwitchContextToAction'

  public constructor(protected component: MBTComponent) {}

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return SwitchContext2PaneFeature.get.included(modelFeatures)
  }

  public canBePerformed(model: App): Throwing<boolean> {
    return true
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    return this.component
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'SwitchContextToAction'
  }

  public getActionType(): string {
    return SwitchContextToAction.type
  }
}
