import { requireNonNull } from '../../../../../testopithecus-common/code/utils/utils'
import { Nullable, Throwing } from '../../../../../../common/ys'
import { FolderName } from '../../feature/folder-list-features'
import { MoveToFolder } from '../../feature/move-to-folder-feature'
import { MailAppModelHandler, MessageId } from '../mail-model'
import { ContextMenuModel } from '../messages-list/context-menu-model'
import { GroupModeModel } from '../messages-list/group-mode-model'
import { MessageListDisplayModel } from '../messages-list/message-list-display-model'
import { OpenMessageModel } from '../opened-message/open-message-model'

export class MoveToFolderModel implements MoveToFolder {
  public constructor(
    private accHandler: MailAppModelHandler,
    private openMessageModel: OpenMessageModel,
    private contextMenuModel: ContextMenuModel,
    private messageListDisplayModel: MessageListDisplayModel,
    private groupModeModel: GroupModeModel,
  ) {}

  private getMids(): Throwing<Nullable<MessageId[]>> {
    if (this.openMessageModel.isMessageOpened()) {
      return [this.openMessageModel.openedMessage]
    }

    const contextMenuOrder = this.contextMenuModel.getOrderOfMessageWithOpenedContextMenu()
    if (contextMenuOrder !== -1) {
      return [this.messageListDisplayModel.getMessageId(contextMenuOrder)]
    }

    const groupModeSelectedOrders = this.groupModeModel.getSelectedMessages()
    if (groupModeSelectedOrders.size > 0) {
      return this.messageListDisplayModel.getMidsByOrders(groupModeSelectedOrders)
    }
    return null
  }

  public tapOnCreateFolder(): Throwing<void> {
    // do nothing
  }

  public getFolderList(): Throwing<FolderName[]> {
    return this.accHandler.getCurrentAccount().messagesDB.getFolderList()
  }

  public tapOnFolder(folderName: FolderName): Throwing<void> {
    const currAccount = this.accHandler.getCurrentAccount()
    for (const mid of requireNonNull(this.getMids(), 'There is no opened/selected messages')) {
      if (this.openMessageModel.isMessageOpened()) {
        currAccount.messagesDB.moveMessageToFolder(mid, folderName)
      } else {
        const midsToMove: MessageId[] = currAccount.accountSettings.groupBySubjectEnabled
          ? currAccount.messagesDB.getMessagesInThreadByMid(mid)
          : [mid]
        midsToMove.forEach((mid) => currAccount.messagesDB.moveMessageToFolder(mid, folderName))
      }
    }
    this.contextMenuModel.close()
    this.groupModeModel.unselectAllMessages()
  }
}
