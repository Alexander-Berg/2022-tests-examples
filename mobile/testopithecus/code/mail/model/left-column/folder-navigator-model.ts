import { Int32, Nullable, Throwing } from '../../../../../../common/ys'
import { formatFolderName } from '../../../utils/mail-utils'
import {
  FilterNavigator,
  FolderName,
  FolderNavigator,
  LabelName,
  LabelNavigator,
} from '../../feature/folder-list-features'
import { MessageContainer, MessageContainerType } from '../../feature/message-list/container-getter-feature'
import { DefaultFolderName } from '../folder-data-model'
import { MailAppModelHandler } from '../mail-model'
import { MessageListDisplayModel } from '../messages-list/message-list-display-model'
import { UndoModel } from '../messages-list/undo-model'
import { MessageListDatabaseFilter } from '../supplementary/message-list-database'

export class FolderNavigatorModel implements FolderNavigator, LabelNavigator, FilterNavigator {
  public constructor(
    public model: MessageListDisplayModel,
    private accHandler: MailAppModelHandler,
    private undoModel: UndoModel,
  ) {}
  private openedFolderList: boolean = false

  public getFoldersList(): Throwing<Map<FolderName, Int32>> {
    const folders = this.accHandler.getCurrentAccount().messagesDB.getFolderList()
    const foldersToUnread = new Map<FolderName, Int32>()
    folders.forEach((folderName) => foldersToUnread.set(folderName, this.getUnreadCounterForFolder(folderName)))
    return foldersToUnread
  }

  public goToFolder(folderDisplayName: string, parentFolders: string[]): Throwing<void> {
    this.goToContainer(formatFolderName(folderDisplayName, parentFolders), MessageContainerType.folder)
  }

  public isInTabsMode(): Throwing<boolean> {
    return this.accHandler.getCurrentAccount().accountSettings.sortingEmailsByCategoryEnabled
  }

  public isOpened(): Throwing<boolean> {
    return this.openedFolderList
  }

  public closeFolderList(): Throwing<void> {
    this.openedFolderList = false
  }

  public openFolderList(): Throwing<void> {
    this.openedFolderList = true
    this.undoModel.resetUndoShowing()
  }

  public ptrFoldersList(): Throwing<void> {}

  public getCurrentContainer(): Throwing<Nullable<string>> {
    if (this.model.getCurrentContainer().type !== MessageContainerType.search) {
      return this.model.getCurrentContainer().name
    }
    return null
  }

  public getLabelList(): Throwing<LabelName[]> {
    return this.accHandler.getCurrentAccount().messagesDB.getLabelList()
  }

  public goToLabel(labelName: string): Throwing<void> {
    this.goToContainer(labelName, MessageContainerType.label)
  }

  public goToFilterImportant(): Throwing<void> {
    this.goToContainer('Important', MessageContainerType.importantFilter)
  }

  public goToFilterUnread(): Throwing<void> {
    this.goToContainer('Unread', MessageContainerType.unreadFilter)
  }

  public goToFilterWithAttachments(): Throwing<void> {
    this.goToContainer('With attachments', MessageContainerType.withAttachmentsFilter)
  }

  private goToContainer(containerName: string, containerType: MessageContainerType): Throwing<void> {
    this.model.setCurrentContainer(new MessageContainer(containerName, containerType))
    this.closeFolderList()
  }

  private getUnreadCounterForFolder(folder: FolderName): Int32 {
    if (
      [
        DefaultFolderName.trash,
        DefaultFolderName.draft,
        DefaultFolderName.spam,
        DefaultFolderName.sent,
        DefaultFolderName.template,
      ].includes(folder)
    ) {
      return 0
    }
    return this.accHandler
      .getCurrentAccount()
      .messagesDB.getMessageIdList(
        new MessageListDatabaseFilter().withFolder(folder).withIsUnreadOnly().withIsInThreadMode(false).withLimit(100),
      ).length
  }
}
