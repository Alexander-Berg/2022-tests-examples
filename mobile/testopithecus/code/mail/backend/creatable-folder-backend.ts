import { MailboxClientHandler } from '../../client/mailbox-client'
import { CreatableFolder } from '../feature/manageable-container-features'

export class CreatableFolderBackend implements CreatableFolder {
  public constructor(private clientsHandler: MailboxClientHandler) {}

  public createFolder(folderDisplayName: string): void {
    this.clientsHandler.getCurrentClient().createFolder(folderDisplayName)
  }
}
