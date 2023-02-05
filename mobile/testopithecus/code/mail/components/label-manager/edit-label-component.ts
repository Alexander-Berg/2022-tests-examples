import { Throwing } from '../../../../../../common/ys'
import { App, MBTAction, MBTComponent } from '../../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { MBTComponentActions } from '../../../../../testopithecus-common/code/mbt/walk/behaviour/user-behaviour'
import { assertInt32Equals, assertStringEquals } from '../../../../../testopithecus-common/code/utils/assert'
import { ManageableLabelFeature } from '../../feature/manageable-container-features'

export class EditLabelComponent implements MBTComponent {
  public static readonly type: string = 'EditLabelComponent'

  public getComponentType(): string {
    return EditLabelComponent.type
  }

  public async assertMatches(model: App, application: App): Throwing<Promise<void>> {
    const manageableLabelModel = ManageableLabelFeature.get.castIfSupported(model)
    const manageableLabelApplication = ManageableLabelFeature.get.castIfSupported(application)

    if (manageableLabelModel !== null && manageableLabelApplication !== null) {
      const currentLabelNameModel = manageableLabelModel.getCurrentEditedLabelName()
      const currentLabelNameApplication = manageableLabelApplication.getCurrentEditedLabelName()
      assertStringEquals(currentLabelNameModel, currentLabelNameApplication, 'Label name is incorrect')

      const currentColorIndexModel = manageableLabelModel.getCurrentEditedLabelColorIndex()
      const currentColorIndexApplication = manageableLabelApplication.getCurrentEditedLabelColorIndex()
      assertInt32Equals(currentColorIndexModel, currentColorIndexApplication, 'Color index is incorrect')
    }
  }

  public tostring(): string {
    return this.getComponentType()
  }
}

export class EditLabelActions implements MBTComponentActions {
  public getActions(_model: App): MBTAction[] {
    const actions: MBTAction[] = []
    return actions
  }
}
