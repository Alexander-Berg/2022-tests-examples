import { Int32, Int64, Nullable, Throwing } from '../../../../../../common/ys'
import { currentTimeMs } from '../../../../../testopithecus-common/code/utils/utils'
import { Spamable } from '../../feature/base-action-features'
import { FolderName } from '../../feature/folder-list-features'
import { DefaultFolderName } from '../folder-data-model'
import { MailAppModelHandler, MessageId } from '../mail-model'
import { MessageListDisplayModel } from '../messages-list/message-list-display-model'
import { MessageListDatabaseFilter } from '../supplementary/message-list-database'

export class SpamableModel implements Spamable {
  public constructor(private model: MessageListDisplayModel, private accHandler: MailAppModelHandler) {}

  private lastSpamActionTime: Nullable<Int64> = null
  private spammedMessageIdToFolder: Map<MessageId, FolderName> = new Map<MessageId, FolderName>()
  private midToReadStatus: Map<MessageId, boolean> = new Map<MessageId, boolean>()

  public resetLastSpamMessageTime(): void {
    this.lastSpamActionTime = null
  }

  public getLastSpamMessageTime(): Nullable<Int64> {
    return this.lastSpamActionTime
  }

  public getSpammedMessageIdToFolder(): Map<MessageId, FolderName> {
    return this.spammedMessageIdToFolder
  }

  public getMidToReadStatus(): Map<MessageId, boolean> {
    return this.midToReadStatus
  }

  public moveToSpam(order: Int32): Throwing<void> {
    this.moveToSpamMessages(
      new Set<Int32>([order]),
    )
  }

  public moveToSpamMessages(orders: Set<Int32>): void {
    const mids: MessageId[] = []
    this.spammedMessageIdToFolder.clear()
    this.midToReadStatus.clear()
    for (const order of orders.values()) {
      this.model.getThreadByOrder(order).forEach((mid) => mids.push(mid))
    }

    const notSpamMessages = this.accHandler
      .getCurrentAccount()
      .messagesDB.getMessages()
      .filter((mid) => !mids.includes(mid))

    for (const mid of mids) {
      const folderName = this.accHandler.getCurrentAccount().messagesDB.storedFolder(mid)
      this.spammedMessageIdToFolder.set(mid, folderName)
      this.midToReadStatus.set(mid, this.accHandler.getCurrentAccount().messagesDB.storedMessage(mid).mutableHead.read)
      this.accHandler.getCurrentAccount().messagesDB.storedMessage(mid).mutableHead.read = true // Разная логика в iOS приложении и на Web-е
      this.accHandler.getCurrentAccount().messagesDB.storedMessage(mid).mutableHead.threadCounter = null // В папке Spam отсутствует тредный режим
      this.accHandler.getCurrentAccount().messagesDB.moveMessageToFolder(mid, DefaultFolderName.spam)

      for (const itemMid of notSpamMessages) {
        const threadCounter = this.accHandler.getCurrentAccount().messagesDB.storedMessage(itemMid).mutableHead
          .threadCounter
        if (
          this.accHandler.getCurrentAccount().messagesDB.storedMessage(itemMid).mutableHead.subject ===
            this.accHandler.getCurrentAccount().messagesDB.storedMessage(mid).mutableHead.subject &&
          threadCounter !== null
        ) {
          this.accHandler.getCurrentAccount().messagesDB.storedMessage(itemMid).mutableHead.threadCounter =
            threadCounter - 1
        }
      }
    }
    this.lastSpamActionTime = currentTimeMs()
  }

  public moveFromSpam(order: Int32): Throwing<void> {
    this.moveFromSpamMessages(
      new Set<Int32>([order]),
    )
  }

  public addThreadCounter(notSpamMessages: MessageId[], mid: MessageId): void {
    let threadCounter: Int32 = 1
    for (const itemMid of notSpamMessages) {
      if (
        this.accHandler.getCurrentAccount().messagesDB.storedMessage(itemMid).mutableHead.subject ===
        this.accHandler.getCurrentAccount().messagesDB.storedMessage(mid).mutableHead.subject
      ) {
        const counter = this.accHandler.getCurrentAccount().messagesDB.storedMessage(itemMid).mutableHead.threadCounter
        this.accHandler.getCurrentAccount().messagesDB.storedMessage(itemMid).mutableHead.threadCounter =
          counter === null ? 2 : counter + 1
        threadCounter = threadCounter + 1
      }
    }
    this.accHandler.getCurrentAccount().messagesDB.storedMessage(mid).mutableHead.threadCounter =
      threadCounter !== 1 ? threadCounter : null
  }

  public moveFromSpamMessages(orders: Set<Int32>, folder: FolderName = DefaultFolderName.inbox): void {
    const notSpamMessages = this.accHandler
      .getCurrentAccount()
      .messagesDB.getMessageIdList(new MessageListDatabaseFilter().withExcludedFolders([DefaultFolderName.spam]))

    for (const mid of this.model.getMidsByOrders(orders)) {
      this.accHandler.getCurrentAccount().messagesDB.moveMessageToFolder(mid, folder)
      this.addThreadCounter(notSpamMessages, mid)
    }
    this.resetLastSpamMessageTime()
  }
}
