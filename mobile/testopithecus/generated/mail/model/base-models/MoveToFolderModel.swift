// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/model/base-models/move-to-folder-model.ts >>>

import Foundation

open class MoveToFolderModel: MoveToFolder {
  private var accHandler: MailAppModelHandler
  private var openMessageModel: OpenMessageModel
  private var contextMenuModel: ContextMenuModel
  private var messageListDisplayModel: MessageListDisplayModel
  private var groupModeModel: GroupModeModel
  public init(_ accHandler: MailAppModelHandler, _ openMessageModel: OpenMessageModel, _ contextMenuModel: ContextMenuModel, _ messageListDisplayModel: MessageListDisplayModel, _ groupModeModel: GroupModeModel) {
    self.accHandler = accHandler
    self.openMessageModel = openMessageModel
    self.contextMenuModel = contextMenuModel
    self.messageListDisplayModel = messageListDisplayModel
    self.groupModeModel = groupModeModel
  }

  @discardableResult
  private func getMids() throws -> YSArray<MessageId>! {
    if self.openMessageModel.isMessageOpened() {
      return YSArray(self.openMessageModel.openedMessage)
    }
    let contextMenuOrder = (try self.contextMenuModel.getOrderOfMessageWithOpenedContextMenu())
    if contextMenuOrder != -1 {
      return YSArray(self.messageListDisplayModel.getMessageId(contextMenuOrder))
    }
    let groupModeSelectedOrders = (try self.groupModeModel.getSelectedMessages())
    if groupModeSelectedOrders.size > 0 {
      return self.messageListDisplayModel.getMidsByOrders(groupModeSelectedOrders)
    }
    return nil
  }

  @discardableResult
  open func tapOnCreateFolder() throws -> Void {
  }

  @discardableResult
  open func getFolderList() throws -> YSArray<FolderName> {
    return self.accHandler.getCurrentAccount().messagesDB.getFolderList()
  }

  @discardableResult
  open func tapOnFolder(_ folderName: FolderName) throws -> Void {
    let currAccount = self.accHandler.getCurrentAccount()
    for mid in requireNonNull((try self.getMids()), "There is no opened/selected messages") {
      if self.openMessageModel.isMessageOpened() {
        currAccount.messagesDB.moveMessageToFolder(mid, folderName)
      } else {
        let midsToMove: YSArray<MessageId> = currAccount.accountSettings.groupBySubjectEnabled ? currAccount.messagesDB.getMessagesInThreadByMid(mid) : YSArray(mid)
        midsToMove.forEach({
          (mid) in
          currAccount.messagesDB.moveMessageToFolder(mid, folderName)
        })
      }
    }
    (try self.contextMenuModel.close())
    (try self.groupModeModel.unselectAllMessages())
  }

}

