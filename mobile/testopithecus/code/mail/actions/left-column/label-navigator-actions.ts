import { Throwing } from '../../../../../../common/ys'
import { EventusEvent } from '../../../../../eventus-common/code/eventus-event'
import {
  App,
  FeatureID,
  MBTAction,
  MBTActionType,
  MBTComponent,
  MBTHistory,
} from '../../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { MaillistComponent } from '../../components/maillist-component'
import { LabelName, LabelNavigatorFeature } from '../../feature/folder-list-features'

export class GoToLabelAction implements MBTAction {
  public static readonly type: MBTActionType = 'GoToLabel'

  public constructor(private label: LabelName) {}

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return LabelNavigatorFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): Throwing<boolean> {
    const labelNavigatorModel = LabelNavigatorFeature.get.forceCast(model)
    const labels = labelNavigatorModel.getLabelList()
    return labels.filter((label) => label === this.label).length > 0
  }

  public async perform(model: App, application: App, _history: MBTHistory): Throwing<Promise<MBTComponent>> {
    LabelNavigatorFeature.get.forceCast(model).goToLabel(this.label)
    LabelNavigatorFeature.get.forceCast(application).goToLabel(this.label)
    return new MaillistComponent()
  }

  public events(): EventusEvent[] {
    return []
  }

  public getActionType(): string {
    return GoToLabelAction.type
  }

  public tostring(): string {
    return `GoToLabel(${this.label})`
  }
}
