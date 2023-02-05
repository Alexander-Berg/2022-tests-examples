import { Throwing } from '../../../../../../common/ys'
import { App, MBTAction, MBTComponent } from '../../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { MBTComponentActions } from '../../../../../testopithecus-common/code/mbt/walk/behaviour/user-behaviour'
import { assertBooleanEquals, assertInt32Equals } from '../../../../../testopithecus-common/code/utils/assert'
import { ManageableFolderFeature } from '../../feature/manageable-container-features'

export class SelectParentFolderComponent implements MBTComponent {
  public static readonly type: string = 'SelectParentFolderComponent'

  public getComponentType(): string {
    return SelectParentFolderComponent.type
  }

  public async assertMatches(model: App, application: App): Throwing<Promise<void>> {
    const manageableFolderModel = ManageableFolderFeature.get.castIfSupported(model)
    const manageableFolderApplication = ManageableFolderFeature.get.castIfSupported(application)

    if (manageableFolderModel !== null && manageableFolderApplication !== null) {
      const folderListModel = manageableFolderModel
        .getFolderListForFolderLocationScreen()
        .map((folder) => folder.split('|').reverse()[0])
      const folderListApplication = manageableFolderApplication
        .getFolderListForFolderLocationScreen()
        .map((folder) => folder.split('|').reverse()[0])

      assertInt32Equals(folderListModel.length, folderListApplication.length, 'Different number of folders')

      for (const folder of folderListModel) {
        assertBooleanEquals(true, folderListApplication.includes(folder), `Missing folder ${folder}`)
      }
    }
  }

  public tostring(): string {
    return this.getComponentType()
  }
}

export class SelectParentFolderActions implements MBTComponentActions {
  public getActions(_model: App): MBTAction[] {
    const actions: MBTAction[] = []
    return actions
  }
}
