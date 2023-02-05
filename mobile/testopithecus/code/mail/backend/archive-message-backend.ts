import { Int32 } from '../../../../../common/ys'
import { MailboxClientHandler } from '../../client/mailbox-client'
import { ArchiveMessage } from '../feature/base-action-features'
import { MessageListDisplayBackend } from './message-list-display-backend'

export class ArchiveMessageBackend implements ArchiveMessage {
  public constructor(
    private messageListDisplayBackend: MessageListDisplayBackend,
    private clientsHandler: MailboxClientHandler,
  ) {}

  public archiveMessage(order: Int32): void {
    const tid = this.messageListDisplayBackend.getThreadMessage(order).tid!
    this.clientsHandler.getCurrentClient().archive('Archive', tid)
  }
}
