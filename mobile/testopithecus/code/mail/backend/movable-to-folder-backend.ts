import { Int32 } from '../../../../../common/ys'
import { MailboxClientHandler } from '../../client/mailbox-client'
import { MovableToFolder } from '../feature/base-action-features'
import { MessageListDisplayBackend } from './message-list-display-backend'

export class MovableToFolderBackend implements MovableToFolder {
  public constructor(
    private mailListDisplayBackend: MessageListDisplayBackend,
    private clientsHandler: MailboxClientHandler,
  ) {}

  public moveMessageToFolder(order: Int32, folderName: string): void {
    const tid = this.mailListDisplayBackend.getThreadMessage(order).tid!
    const fid = this.clientsHandler
      .getCurrentClient()
      .getFolderList()
      .filter((f) => f.name === folderName)[0].fid
    this.clientsHandler.getCurrentClient().moveThreadToFolder(tid, fid)
  }
}
