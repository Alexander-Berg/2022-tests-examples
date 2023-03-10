// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/feature/message-list/context-menu-feature.ts >>>

import Foundation

public typealias MessageActionName = String

open class ContextMenuFeature: Feature<ContextMenu> {
  public static var `get`: ContextMenuFeature = ContextMenuFeature()
  private init() {
    super.init("ContextMenu", "Меню действий с письмом, открываемое через short swipe или из просмотра письма")
  }

}

public protocol ContextMenu {
  @discardableResult
  func openFromShortSwipe(_ order: Int32) throws -> Void
  @discardableResult
  func openFromMessageView() throws -> Void
  @discardableResult
  func close() throws -> Void
  @discardableResult
  func getAvailableActions() throws -> YSArray<MessageActionName>
  @discardableResult
  func openReplyCompose() throws -> Void
  @discardableResult
  func openReplyAllCompose() throws -> Void
  @discardableResult
  func openForwardCompose() throws -> Void
  @discardableResult
  func deleteMessage() throws -> Void
  @discardableResult
  func markAsSpam() throws -> Void
  @discardableResult
  func markAsNotSpam() throws -> Void
  @discardableResult
  func openApplyLabelsScreen() throws -> Void
  @discardableResult
  func markAsRead() throws -> Void
  @discardableResult
  func markAsUnread() throws -> Void
  @discardableResult
  func markAsImportant() throws -> Void
  @discardableResult
  func markAsUnimportant() throws -> Void
  @discardableResult
  func openMoveToFolderScreen() throws -> Void
  @discardableResult
  func archive() throws -> Void
  @discardableResult
  func showTranslator() throws -> Void
}

