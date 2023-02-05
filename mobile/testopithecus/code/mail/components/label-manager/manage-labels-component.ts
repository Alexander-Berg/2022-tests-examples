import { Throwing } from '../../../../../../common/ys'
import { App, MBTAction, MBTComponent } from '../../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { MBTComponentActions } from '../../../../../testopithecus-common/code/mbt/walk/behaviour/user-behaviour'
import { assertBooleanEquals, assertInt32Equals } from '../../../../../testopithecus-common/code/utils/assert'
import { ManageableLabelFeature } from '../../feature/manageable-container-features'

export class ManageLabelsComponent implements MBTComponent {
  public static readonly type: string = 'ManageLabelsComponent'

  public getComponentType(): string {
    return ManageLabelsComponent.type
  }

  public async assertMatches(model: App, application: App): Throwing<Promise<void>> {
    const manageableLabelModel = ManageableLabelFeature.get.castIfSupported(model)
    const manageableLabelApplication = ManageableLabelFeature.get.castIfSupported(application)

    if (manageableLabelModel !== null && manageableLabelApplication !== null) {
      const labelListModel = manageableLabelModel.getLabelList()
      const labelListApplication = manageableLabelApplication.getLabelList()

      assertInt32Equals(labelListModel.length, labelListApplication.length, 'Different number of labels')

      for (const label of labelListModel) {
        assertBooleanEquals(true, labelListApplication.includes(label), `Missing label ${label}`)
      }
    }
  }

  public tostring(): string {
    return this.getComponentType()
  }
}

export class ManageLabelsActions implements MBTComponentActions {
  public getActions(_model: App): MBTAction[] {
    const actions: MBTAction[] = []
    return actions
  }
}
