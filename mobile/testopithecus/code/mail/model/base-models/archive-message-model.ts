import { Int32, Int64, Nullable } from '../../../../../../common/ys'
import { currentTimeMs } from '../../../../../testopithecus-common/code/utils/utils'
import { ArchiveMessage } from '../../feature/base-action-features'
import { FolderName } from '../../feature/folder-list-features'
import { DefaultFolderName } from '../folder-data-model'
import { MailAppModelHandler, MessageId } from '../mail-model'
import { MessageListDisplayModel } from '../messages-list/message-list-display-model'

export class ArchiveMessageModel implements ArchiveMessage {
  public constructor(private model: MessageListDisplayModel, private accHandler: MailAppModelHandler) {}

  private lastArchiveMessageTime: Nullable<Int64> = null
  private archivedMessageIdToFolder: Map<MessageId, FolderName> = new Map<MessageId, FolderName>()

  public resetLastArchiveMessageTime(): void {
    this.lastArchiveMessageTime = null
  }

  public getLastArchiveMessageTime(): Nullable<Int64> {
    return this.lastArchiveMessageTime
  }

  public getArchivedMessageIdToFolder(): Map<MessageId, FolderName> {
    return this.archivedMessageIdToFolder
  }

  public archiveMessage(order: Int32): void {
    this.archiveMessages(
      new Set<Int32>([order]),
    )
  }

  public archiveMessages(orders: Set<Int32>): void {
    if (!this.accHandler.getCurrentAccount().messagesDB.getFolderList().includes(DefaultFolderName.archive)) {
      this.accHandler.getCurrentAccount().messagesDB.createFolder(DefaultFolderName.archive)
    }
    this.archivedMessageIdToFolder.clear()
    for (const mid of this.model.getMidsByOrders(orders)) {
      const folderName = this.accHandler.getCurrentAccount().messagesDB.storedFolder(mid)
      this.archivedMessageIdToFolder.set(mid, folderName)
      this.accHandler.getCurrentAccount().messagesDB.moveMessageToFolder(mid, DefaultFolderName.archive)
    }
    this.lastArchiveMessageTime = currentTimeMs()
  }
}
