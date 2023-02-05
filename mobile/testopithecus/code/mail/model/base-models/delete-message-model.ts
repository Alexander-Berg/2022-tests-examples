import { Int32, Int64, Nullable, Throwing } from '../../../../../../common/ys'
import { currentTimeMs } from '../../../../../testopithecus-common/code/utils/utils'
import { DeleteMessage } from '../../feature/base-action-features'
import { FolderName } from '../../feature/folder-list-features'
import { MailAppModelHandler, MessageId } from '../mail-model'
import { MessageListDisplayModel } from '../messages-list/message-list-display-model'

export class DeleteMessageModel implements DeleteMessage {
  public constructor(private model: MessageListDisplayModel, private accHandler: MailAppModelHandler) {}

  private lastDeleteMessageTime: Nullable<Int64> = null
  private deletedMessageIdToFolder: Map<MessageId, FolderName> = new Map<MessageId, FolderName>()

  public resetLastDeleteMessageTime(): void {
    this.lastDeleteMessageTime = null
  }

  public getLastDeleteMessageTime(): Nullable<Int64> {
    return this.lastDeleteMessageTime
  }

  public getDeletedMessageIdToFolder(): Map<MessageId, FolderName> {
    return this.deletedMessageIdToFolder
  }

  public deleteMessage(order: Int32): Throwing<void> {
    this.deleteMessages(
      new Set<Int32>([order]),
    )
  }

  public deleteOpenedMessage(mid: MessageId): void {
    this.deletedMessageIdToFolder.clear()
    this.deleteMessageByMid(mid)
    this.lastDeleteMessageTime = currentTimeMs()
  }

  private deleteMessageByMid(mid: MessageId): void {
    const folderName = this.accHandler.getCurrentAccount().messagesDB.storedFolder(mid)
    this.deletedMessageIdToFolder.set(mid, folderName)
    this.accHandler.getCurrentAccount().messagesDB.removeMessage(mid)
  }

  public deleteMessages(orders: Set<Int32>): void {
    this.deletedMessageIdToFolder.clear()
    for (const mid of this.model.getMidsByOrders(orders)) {
      this.deleteMessageByMid(mid)
    }
    this.lastDeleteMessageTime = currentTimeMs()
  }
}
