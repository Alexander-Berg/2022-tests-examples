import { LabelData } from '../../../../mapi/code/api/entities/label/label'
import { Throwing } from '../../../../../common/ys'
import { Feature } from '../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { FolderName } from './folder-list-features'

export class BackendActionsFeature extends Feature<BackendActions> {
  public static readonly get: BackendActionsFeature = new BackendActionsFeature()

  private constructor() {
    super('BackendActions', 'Действия с бекендом, на клиенте реализовывать не надо')
  }
}

export interface BackendActions {
  addFolder(folder: FolderName): Throwing<void>

  addLabel(label: LabelData): Throwing<void>
}
