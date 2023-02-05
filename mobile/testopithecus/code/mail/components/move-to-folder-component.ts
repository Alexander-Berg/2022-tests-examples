import { App, MBTComponent, MBTComponentType } from '../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { assertTrue } from '../../../../testopithecus-common/code/utils/assert'
import { Throwing } from '../../../../../common/ys'
import { MoveToFolderFeature } from '../feature/move-to-folder-feature'

export class MoveToFolderComponent implements MBTComponent {
  public static readonly type: MBTComponentType = 'MoveToFolderComponent'

  public async assertMatches(model: App, application: App): Throwing<Promise<void>> {
    const modelMoveToFolder = MoveToFolderFeature.get.castIfSupported(model)
    const appMoveToFolder = MoveToFolderFeature.get.castIfSupported(application)

    if (modelMoveToFolder === null || appMoveToFolder === null) {
      return
    }

    const modelFolderList = modelMoveToFolder.getFolderList()
    const appFolderList = appMoveToFolder.getFolderList()

    for (const folder of modelFolderList) {
      assertTrue(appFolderList.includes(folder), `There is no folder ${folder}`)
    }
  }

  public tostring(): string {
    return this.getComponentType()
  }

  public getComponentType(): MBTComponentType {
    return MoveToFolderComponent.type
  }
}
