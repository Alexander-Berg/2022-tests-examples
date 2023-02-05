import { Int32, Throwing } from '../../../../../common/ys'
import { LabelType } from '../../../../mapi/code/api/entities/label/label'
import { MailboxClientHandler } from '../../client/mailbox-client'
import { MarkableImportant } from '../feature/base-action-features'
import { MessageListDisplayBackend } from './message-list-display-backend'

export class MarkableImportantBackend implements MarkableImportant {
  public constructor(
    private mailListDisplayBackend: MessageListDisplayBackend,
    private clientsHandler: MailboxClientHandler,
  ) {}

  public markAsImportant(order: Int32): Throwing<void> {
    const tid = this.mailListDisplayBackend.getThreadMessage(order).tid!
    const lid = this.clientsHandler
      .getCurrentClient()
      .getLabelList()
      .filter((label) => label.type === LabelType.important)[0].lid
    this.clientsHandler.getCurrentClient().markThreadWithLabel(tid, lid)
  }

  public markAsUnimportant(order: Int32): Throwing<void> {
    const tid = this.mailListDisplayBackend.getThreadMessage(order).tid!
    const lid = this.clientsHandler
      .getCurrentClient()
      .getLabelList()
      .filter((label) => label.type === LabelType.important)[0].lid
    this.clientsHandler.getCurrentClient().unmarkThreadWithLabel(tid, lid)
  }
}
