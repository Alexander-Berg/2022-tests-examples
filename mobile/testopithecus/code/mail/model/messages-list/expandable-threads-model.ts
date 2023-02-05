import { Int32, Throwing } from '../../../../../../common/ys'
import { ExpandableThreads, ReadOnlyExpandableThreads } from '../../feature/message-list/expandable-threads-feature'
import { MessageView } from '../../feature/mail-view-features'
import { FullMessage, MailAppModelHandler, MessageId } from '../mail-model'
import { MessageListDisplayModel } from './message-list-display-model'

export class ReadOnlyExpandableThreadsModel implements ReadOnlyExpandableThreads {
  public expanded: Set<MessageId> = new Set<MessageId>()

  public constructor(private messageListDisplay: MessageListDisplayModel, private accHandler: MailAppModelHandler) {}

  public isExpanded(threadOrder: Int32): Throwing<boolean> {
    const mid = this.messageListDisplay.getMessageId(threadOrder)
    return this.expanded.has(mid)
  }

  public isRead(threadOrder: Int32, messageOrder: Int32): Throwing<boolean> {
    return this.getThreadMessage(threadOrder, messageOrder).head.read
  }

  // TODO: занулять темы тредных писем в этом методе
  public getMessagesInThread(threadOrder: Int32): Throwing<MessageView[]> {
    return this.accHandler
      .getCurrentAccount()
      .messagesDB.getMessagesInThreadByMid(this.messageListDisplay.getMessageId(threadOrder))
      .map((mid) => this.accHandler.getCurrentAccount().messagesDB.storedMessage(mid).head)
  }

  public getThreadMessage(threadOrder: Int32, messageOrder: Int32): FullMessage {
    const mid = this.getMessagesInThreadByOrder(threadOrder)[messageOrder]
    return this.accHandler.getCurrentAccount().messagesDB.storedMessage(mid)
  }

  private getMessagesInThreadByOrder(threadOrder: Int32): MessageId[] {
    const mid = this.messageListDisplay.getMessageId(threadOrder)
    return this.accHandler.getCurrentAccount().messagesDB.getMessagesInThreadByMid(mid)
  }
}

export class ExpandableThreadsModel implements ExpandableThreads {
  public constructor(
    private readonlyExpandableThreads: ReadOnlyExpandableThreadsModel,
    private messageListDisplay: MessageListDisplayModel,
  ) {}

  public markThreadMessageAsRead(threadOrder: Int32, messageOrder: Int32): Throwing<void> {
    this.readonlyExpandableThreads.getThreadMessage(threadOrder, messageOrder).mutableHead.read = true
  }

  public markThreadMessageAsUnRead(threadOrder: Int32, messageOrder: Int32): Throwing<void> {
    this.readonlyExpandableThreads.getThreadMessage(threadOrder, messageOrder).mutableHead.read = false
  }

  public markThreadMessageAsImportant(threadOrder: Int32, messageOrder: Int32): void {
    this.readonlyExpandableThreads.getThreadMessage(threadOrder, messageOrder).mutableHead.important = true
  }

  public markThreadMessageAsUnimportant(threadOrder: Int32, messageOrder: Int32): void {
    this.readonlyExpandableThreads.getThreadMessage(threadOrder, messageOrder).mutableHead.important = false
  }

  public expandThread(order: Int32): Throwing<void> {
    const mid = this.messageListDisplay.getMessageId(order)
    this.readonlyExpandableThreads.expanded.add(mid)
  }

  public collapseThread(order: Int32): Throwing<void> {
    const mid = this.messageListDisplay.getMessageId(order)
    this.readonlyExpandableThreads.expanded.delete(mid)
  }
}
