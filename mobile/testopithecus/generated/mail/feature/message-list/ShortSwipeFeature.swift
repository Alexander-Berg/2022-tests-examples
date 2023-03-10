// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/feature/message-list/short-swipe-feature.ts >>>

import Foundation

open class ShortSwipeFeature: Feature<ShortSwipe> {
  public static var `get`: ShortSwipeFeature = ShortSwipeFeature()
  private init() {
    super.init("ShortSwipe", "Архивирование/Удаление через короткий свайп")
  }

}

public protocol ShortSwipe {
  @discardableResult
  func deleteMessageByShortSwipe(_ order: Int32) throws -> Void
  @discardableResult
  func archiveMessageByShortSwipe(_ order: Int32) throws -> Void
  @discardableResult
  func markAsRead(_ order: Int32) throws -> Void
  @discardableResult
  func markAsUnread(_ order: Int32) throws -> Void
}

