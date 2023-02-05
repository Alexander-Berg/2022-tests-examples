import { FolderName } from '../../feature/folder-list-features'
import { CreatableFolder } from '../../feature/manageable-container-features'
import { MailAppModelHandler } from '../mail-model'

export class CreatableFolderModel implements CreatableFolder {
  public constructor(private accHandler: MailAppModelHandler) {}

  public createFolder(folderDisplayName: FolderName): void {
    this.accHandler.getCurrentAccount().messagesDB.createFolder(folderDisplayName)
  }
}
