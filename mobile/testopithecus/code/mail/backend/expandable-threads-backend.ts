import { Int32, Throwing } from '../../../../../common/ys'
import { MessageMeta } from '../../../../mapi/code/api/entities/message/message-meta'
import { MailboxClientHandler } from '../../client/mailbox-client'
import { ExpandableThreads } from '../feature/message-list/expandable-threads-feature'
import { MessageListDisplayBackend } from './message-list-display-backend'

export class ExpandableThreadsBackend implements ExpandableThreads {
  public constructor(
    private messageListDisplayBackend: MessageListDisplayBackend,
    private clientsHandler: MailboxClientHandler,
  ) {}

  public collapseThread(order: Int32): Throwing<void> {
    return
  }

  public expandThread(order: Int32): Throwing<void> {
    return
  }

  public markThreadMessageAsRead(threadOrder: Int32, messageOrder: Int32): Throwing<void> {
    const message = this.getMessageInThread(threadOrder, messageOrder)
    this.clientsHandler.getCurrentClient().markMessageAsRead(message.mid)
  }

  public markThreadMessageAsUnRead(threadOrder: Int32, messageOrder: Int32): Throwing<void> {
    const message = this.getMessageInThread(threadOrder, messageOrder)
    this.clientsHandler.getCurrentClient().markMessageAsUnread(message.mid)
  }

  private getMessageInThread(threadOrder: Int32, messageOrder: Int32): MessageMeta {
    const thread = this.messageListDisplayBackend.getThreadMessage(threadOrder)
    const messages = this.clientsHandler.getCurrentClient().getMessagesInThread(thread.tid!, messageOrder + 1)
    return messages[messageOrder]
  }
}
