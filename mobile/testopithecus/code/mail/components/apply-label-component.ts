import { App, MBTComponent, MBTComponentType } from '../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { assertTrue } from '../../../../testopithecus-common/code/utils/assert'
import { Throwing } from '../../../../../common/ys'
import { ApplyLabelFeature } from '../feature/apply-label-feature'

export class ApplyLabelComponent implements MBTComponent {
  public static readonly type: MBTComponentType = 'AddLabelComponent'

  public async assertMatches(model: App, application: App): Throwing<Promise<void>> {
    const modelApplyLabel = ApplyLabelFeature.get.castIfSupported(model)
    const appApplyLabel = ApplyLabelFeature.get.castIfSupported(application)

    if (modelApplyLabel === null || appApplyLabel === null) {
      return
    }

    const modelLabelList = modelApplyLabel.getLabelList()
    const appLabelList = appApplyLabel.getLabelList()

    for (const label of modelLabelList) {
      assertTrue(appLabelList.includes(label), `There is no label ${label}`)
    }

    const modelSelectedLabels = modelApplyLabel.getSelectedLabels()
    const appSelectedLabels = appApplyLabel.getSelectedLabels()

    for (const label of modelSelectedLabels) {
      assertTrue(appSelectedLabels.includes(label), `Label ${label} is not selected`)
    }
  }

  public tostring(): string {
    return this.getComponentType()
  }

  public getComponentType(): MBTComponentType {
    return ApplyLabelComponent.type
  }
}
