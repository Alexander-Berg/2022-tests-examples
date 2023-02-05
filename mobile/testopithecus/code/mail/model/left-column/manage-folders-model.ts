import { Nullable, Throwing } from '../../../../../../common/ys'
import { requireNonNull } from '../../../../../testopithecus-common/code/utils/utils'
import { formatFolderName } from '../../../utils/mail-utils'
import { FolderName } from '../../feature/folder-list-features'
import { ContainerDeletionMethod, ManageableFolder } from '../../feature/manageable-container-features'
import { MailAppModelHandler } from '../mail-model'

export class ManageFoldersModel implements ManageableFolder {
  public constructor(private accHandler: MailAppModelHandler) {}

  private nameOfCreatedFolder: Nullable<FolderName> = null

  private oldNameOfEditedFolder: Nullable<FolderName> = null
  private oldParentFoldersOfEditedFolder: FolderName[] = []

  private newNameOfEditedFolder: Nullable<FolderName> = null
  private parentFolders: FolderName[] = []

  public closeFolderManager(): Throwing<void> {}

  public openFolderManager(): Throwing<void> {}

  public deleteFolder(
    folderDisplayName: FolderName,
    parentFolders: FolderName[],
    deletionMethod: ContainerDeletionMethod,
  ): Throwing<void> {
    this.accHandler.getCurrentAccount().messagesDB.removeFolder(formatFolderName(folderDisplayName, parentFolders))
    const folders = this.accHandler.getCurrentAccount().messagesDB.getUserFolders()
    for (const folder of folders) {
      if (folder.split(`${folderDisplayName}|`).length > 1) {
        this.accHandler.getCurrentAccount().messagesDB.removeFolder(folder)
      }
    }
  }

  public getFolderListForManageFolderScreen(): Throwing<FolderName[]> {
    return this.accHandler.getCurrentAccount().messagesDB.getUserFolders()
  }

  public closeCreateFolderScreen(): Throwing<void> {
    this.dropAll()
  }

  public closeEditFolderScreen(): Throwing<void> {
    this.dropAll()
  }

  public closeFolderLocationScreen(): Throwing<void> {
    this.parentFolders = []
  }

  public enterNameForNewFolder(folderName: FolderName): Throwing<void> {
    this.nameOfCreatedFolder = folderName
  }

  public enterNameForEditedFolder(folderName: FolderName): Throwing<void> {
    this.newNameOfEditedFolder = folderName
  }

  public getFolderListForFolderLocationScreen(): Throwing<FolderName[]> {
    const userFolders = this.accHandler.getCurrentAccount().messagesDB.getUserFolders()
    userFolders.push(this.accHandler.getCurrentAccount().client.oauthAccount.account.login as FolderName)
    return userFolders
  }

  public openCreateFolderScreen(): Throwing<void> {}

  public openEditFolderScreen(folderName: FolderName, parentFolders: FolderName[]): Throwing<void> {
    this.oldNameOfEditedFolder = folderName
    this.oldParentFoldersOfEditedFolder = parentFolders
  }

  public openFolderLocationScreen(): Throwing<void> {}

  public selectParentFolder(parentFolders: FolderName[]): Throwing<void> {
    this.parentFolders = parentFolders
  }

  public submitNewFolder(): Throwing<void> {
    this.accHandler
      .getCurrentAccount()
      .messagesDB.createFolder(
        formatFolderName(requireNonNull(this.nameOfCreatedFolder, 'Folder name is not set'), this.parentFolders),
      )
    this.dropAll()
  }

  public submitEditedFolder(): Throwing<void> {
    this.accHandler
      .getCurrentAccount()
      .messagesDB.renameFolder(
        formatFolderName(
          requireNonNull(this.oldNameOfEditedFolder, 'Old folder name is not set'),
          this.oldParentFoldersOfEditedFolder,
        ),
        formatFolderName(requireNonNull(this.newNameOfEditedFolder, 'New folder name is not set'), this.parentFolders),
      )
    this.dropAll()
  }

  public getCurrentParentFolderForEditedFolder(): Throwing<string> {
    if (this.parentFolders.length === 0 && this.oldParentFoldersOfEditedFolder.length === 0) {
      return this.accHandler.getCurrentAccount().client.oauthAccount.account.login
    } else if (this.parentFolders.length === 0) {
      return this.oldParentFoldersOfEditedFolder[this.oldParentFoldersOfEditedFolder.length - 1]
    } else {
      return this.parentFolders[this.parentFolders.length - 1]
    }
  }

  public getCurrentParentFolderForNewFolder(): Throwing<string> {
    if (this.parentFolders.length === 0) {
      return this.accHandler.getCurrentAccount().client.oauthAccount.account.login
    } else {
      return this.parentFolders[this.parentFolders.length - 1]
    }
  }

  public getCurrentEditedFolderName(): Throwing<FolderName> {
    if (this.newNameOfEditedFolder === null) {
      return this.oldNameOfEditedFolder!
    }
    return this.newNameOfEditedFolder!
  }

  public getCurrentNewFolderName(): Throwing<FolderName> {
    if (this.nameOfCreatedFolder === null) {
      return ''
    }
    return this.nameOfCreatedFolder!
  }

  private dropAll(): void {
    this.oldNameOfEditedFolder = null
    this.oldParentFoldersOfEditedFolder = []
    this.newNameOfEditedFolder = null
    this.parentFolders = []
  }
}
