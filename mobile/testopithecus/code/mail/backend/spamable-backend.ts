import { Int32, Throwing } from '../../../../../common/ys'
import { requireNonNull } from '../../../../testopithecus-common/code/utils/utils'
import { MailboxClientHandler } from '../../client/mailbox-client'
import { Spamable } from '../feature/base-action-features'
import { MessageListDisplayBackend } from './message-list-display-backend'

export class SpamableBackend implements Spamable {
  public constructor(
    private messageListDisplayBackend: MessageListDisplayBackend,
    private clientsHandler: MailboxClientHandler,
  ) {}

  public moveToSpam(order: Int32): Throwing<void> {
    const fid = this.messageListDisplayBackend.getCurrentFolderId()

    const tid = this.messageListDisplayBackend.getThreadMessage(order).tid!
    this.clientsHandler.getCurrentClient().moveToSpam(fid, tid)
  }

  public moveFromSpam(order: Int32): Throwing<void> {
    const fid = this.messageListDisplayBackend.getInbox().fid

    const tid = requireNonNull(this.messageListDisplayBackend.getThreadMessage(order).tid, 'message must have tid!')
    this.clientsHandler.getCurrentClient().moveThreadToFolder(fid, tid)
  }
}
