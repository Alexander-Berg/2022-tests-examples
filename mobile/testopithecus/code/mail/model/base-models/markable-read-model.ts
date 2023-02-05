import { Int32, Throwing } from '../../../../../../common/ys'
import { MarkableRead } from '../../feature/base-action-features'
import { MailAppModelHandler } from '../mail-model'
import { MessageListDisplayModel } from '../messages-list/message-list-display-model'

export class MarkableReadModel implements MarkableRead {
  public constructor(private model: MessageListDisplayModel, private accHandler: MailAppModelHandler) {}

  public markAsRead(order: Int32): Throwing<void> {
    for (const mid of this.model.getThreadByOrder(order)) {
      this.accHandler.getCurrentAccount().messagesDB.storedMessage(mid).mutableHead.read = true
    }
  }

  public markAsUnread(order: Int32): Throwing<void> {
    for (const mid of this.model.getThreadByOrder(order)) {
      this.accHandler.getCurrentAccount().messagesDB.storedMessage(mid).mutableHead.read = false
    }
  }
}
