import { Throwing } from '../../../../../../common/ys'
import { ClearFolderInFolderList, FolderName } from '../../feature/folder-list-features'
import { MessageContainer, MessageContainerType } from '../../feature/message-list/container-getter-feature'
import { DefaultFolderName } from '../folder-data-model'
import { MailAppModelHandler, MessageId } from '../mail-model'
import { MessageListDatabaseFilter } from '../supplementary/message-list-database'

export class ClearFolderModel implements ClearFolderInFolderList {
  public constructor(public accountDataHandler: MailAppModelHandler) {}

  public doesClearSpamButtonExist(): Throwing<boolean> {
    return !this.accountDataHandler
      .getCurrentAccount()
      .messagesDB.isContainerEmpty(new MessageContainer(DefaultFolderName.spam, MessageContainerType.folder))
  }

  public doesClearTrashButtonExist(): Throwing<boolean> {
    return !this.accountDataHandler
      .getCurrentAccount()
      .messagesDB.isContainerEmpty(new MessageContainer(DefaultFolderName.trash, MessageContainerType.folder))
  }

  public clearSpam(confirmDeletionIfNeeded: boolean): Throwing<void> {
    if (confirmDeletionIfNeeded) {
      this.clearFolder(DefaultFolderName.spam)
    }
  }

  public clearTrash(confirmDeletionIfNeeded: boolean): Throwing<void> {
    if (confirmDeletionIfNeeded) {
      this.clearFolder(DefaultFolderName.trash)
    }
  }

  private clearFolder(folder: FolderName): Throwing<void> {
    const messagesToDelete: MessageId[] = this.getMessagesInFolder(folder)
    for (const mid of messagesToDelete) {
      this.accountDataHandler.getCurrentAccount().messagesDB.removeMessagePermanently(mid)
    }
  }

  private getMessagesInFolder(folder: FolderName): MessageId[] {
    return this.accountDataHandler
      .getCurrentAccount()
      .messagesDB.getMessageIdList(
        new MessageListDatabaseFilter()
          .withContainer(new MessageContainer(folder, MessageContainerType.folder))
          .withIsInThreadMode(false),
      )
  }
}
