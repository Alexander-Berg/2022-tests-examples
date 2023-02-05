import { Nullable, Throwing } from '../../../../../common/ys'
import { MailboxClientHandler } from '../../client/mailbox-client'
import { FolderName, FolderNavigator } from '../feature/folder-list-features'
import { toBackendFolderName } from '../model/folder-data-model'
import { MessageListDisplayBackend } from './message-list-display-backend'

export class FolderNavigatorBackend implements FolderNavigator {
  public constructor(
    private mailListDisplayBackend: MessageListDisplayBackend,
    private clientsHandler: MailboxClientHandler,
  ) {}

  public ptrFoldersList(): void {}

  public getFoldersList(): Throwing<Map<FolderName, number>> {
    const folderToUnread = new Map<FolderName, number>()
    this.clientsHandler
      .getCurrentClient()
      .getFolderList()
      .forEach((meta) => folderToUnread.set(meta.name!, meta.unreadCounter))
    return folderToUnread
  }

  public goToFolder(folderDisplayName: string, parentFolders: string[]): Throwing<void> {
    const folderBackendName = toBackendFolderName(folderDisplayName, parentFolders)
    const folder = this.clientsHandler
      .getCurrentClient()
      .getFolderList()
      .filter((f) => f.name === folderBackendName)[0]
    this.mailListDisplayBackend.setCurrentFolderId(folder.fid)
  }

  public getCurrentContainer(): Throwing<Nullable<string>> {
    const currentFolderName = this.clientsHandler
      .getCurrentClient()
      .getFolderList()
      .filter((meta) => meta.fid === this.mailListDisplayBackend.getCurrentFolderId())[0].name!
    return currentFolderName
  }

  public closeFolderList(): Throwing<void> {}

  public openFolderList(): Throwing<void> {}

  public isInTabsMode(): Throwing<boolean> {
    const currentSettings = this.clientsHandler.getCurrentClient().getSettings()
    return currentSettings.payload!.userParameters.showFoldersTabs
  }
}
