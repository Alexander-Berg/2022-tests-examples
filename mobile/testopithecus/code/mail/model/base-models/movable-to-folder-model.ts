import { Int32 } from '../../../../../../common/ys'
import { MovableToFolder } from '../../feature/base-action-features'
import { FolderName } from '../../feature/folder-list-features'
import { MailAppModelHandler } from '../mail-model'
import { MessageListDisplayModel } from '../messages-list/message-list-display-model'

export class MovableToFolderModel implements MovableToFolder {
  public constructor(private model: MessageListDisplayModel, private accHandler: MailAppModelHandler) {}

  public moveMessageToFolder(order: Int32, folderName: FolderName): void {
    this.moveMessagesToFolder(
      new Set<Int32>([order]),
      folderName,
    )
  }

  public moveMessagesToFolder(orders: Set<Int32>, folderName: FolderName): void {
    for (const mid of this.model.getMidsByOrders(orders)) {
      this.accHandler.getCurrentAccount().messagesDB.moveMessageToFolder(mid, folderName)
    }
  }
}
