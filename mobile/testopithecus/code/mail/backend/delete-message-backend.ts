import { Int32, Throwing } from '../../../../../common/ys'
import { MailboxClientHandler } from '../../client/mailbox-client'
import { DeleteMessage } from '../feature/base-action-features'
import { MessageListDisplayBackend } from './message-list-display-backend'

export class DeleteMessageBackend implements DeleteMessage {
  public constructor(
    private messageListDisplayBackend: MessageListDisplayBackend,
    private clientsHandler: MailboxClientHandler,
  ) {}

  public deleteMessage(order: Int32): Throwing<void> {
    const fid = this.messageListDisplayBackend.getCurrentFolderId()
    const tid = this.messageListDisplayBackend.getThreadMessage(order).tid!
    this.clientsHandler.getCurrentClient().removeMessageByThreadId(fid, tid)
  }
}
