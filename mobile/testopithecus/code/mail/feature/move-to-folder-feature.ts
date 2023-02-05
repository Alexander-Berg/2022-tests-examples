import { Feature } from '../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { Throwing } from '../../../../../common/ys'
import { FolderName } from '../feature/folder-list-features'

export interface MoveToFolder {
  tapOnFolder(folderName: FolderName): Throwing<void>

  tapOnCreateFolder(): Throwing<void>

  getFolderList(): Throwing<FolderName[]>
}

export class MoveToFolderFeature extends Feature<MoveToFolder> {
  public static get: MoveToFolderFeature = new MoveToFolderFeature()

  public constructor() {
    super('MoveToFolder', 'Перемещение в папку')
  }
}
