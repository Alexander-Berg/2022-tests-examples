import { LabelData } from '../../../../mapi/code/api/entities/label/label'
import { Throwing } from '../../../../../common/ys'
import { BackendActions } from '../feature/backend-actions-feature'
import { FolderName } from '../feature/folder-list-features'
import { LabelModel } from './base-models/label-model'
import { CreatableFolderModel } from './left-column/creatable-folder-model'
import { MailAppModelHandler } from './mail-model'

export class BackendActionsModel implements BackendActions {
  public constructor(
    private accountDataHandler: MailAppModelHandler,
    private creatableFolder: CreatableFolderModel,
    private creatableLabel: LabelModel,
  ) {}

  public addFolder(folder: FolderName): Throwing<void> {
    this.accountDataHandler.getCurrentAccount().client.createFolder(folder)
    this.creatableFolder.createFolder(folder)
  }

  public addLabel(label: LabelData): Throwing<void> {
    this.accountDataHandler.getCurrentAccount().client.createLabel(label)
    this.creatableLabel.createLabel(label.name)
  }
}
