import { Throwing } from '../../../../../../common/ys'
import { App, MBTAction, MBTComponent } from '../../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { MBTComponentActions } from '../../../../../testopithecus-common/code/mbt/walk/behaviour/user-behaviour'
import { assertStringEquals } from '../../../../../testopithecus-common/code/utils/assert'
import { ManageableFolderFeature } from '../../feature/manageable-container-features'

export class EditFolderComponent implements MBTComponent {
  public static readonly type: string = 'EditFolderComponent'

  public getComponentType(): string {
    return EditFolderComponent.type
  }

  public async assertMatches(model: App, application: App): Throwing<Promise<void>> {
    const manageableFolderModel = ManageableFolderFeature.get.castIfSupported(model)
    const manageableFolderApplication = ManageableFolderFeature.get.castIfSupported(application)

    if (manageableFolderModel !== null && manageableFolderApplication !== null) {
      const currentFolderNameModel = manageableFolderModel.getCurrentEditedFolderName()
      const currentFolderNameApplication = manageableFolderApplication.getCurrentEditedFolderName()
      assertStringEquals(currentFolderNameModel, currentFolderNameApplication, 'Folder name is incorrect')

      const currentParentFolderModel = manageableFolderModel.getCurrentParentFolderForEditedFolder()
      const currentParentFolderApplication = manageableFolderApplication.getCurrentParentFolderForEditedFolder()
      assertStringEquals(currentParentFolderModel, currentParentFolderApplication, 'Parent folder name is incorrect')
    }
  }

  public tostring(): string {
    return this.getComponentType()
  }
}

export class EditFolderActions implements MBTComponentActions {
  public getActions(_model: App): MBTAction[] {
    const actions: MBTAction[] = []
    return actions
  }
}
