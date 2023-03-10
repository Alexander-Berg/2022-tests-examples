// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/model/search/search-model.ts >>>

import Foundation

open class SearchModel: Search {
  private var isSearchOpened: Bool
  private var lastContainer: MessageContainer
  private var messageList: MessageListDisplayModel
  private let openMessageModel: OpenMessageModel
  public init(_ messageList: MessageListDisplayModel, _ openMessageModel: OpenMessageModel) {
    self.messageList = messageList
    self.openMessageModel = openMessageModel
    self.isSearchOpened = false
    self.lastContainer = self.messageList.getCurrentContainer()
  }

  @discardableResult
  open func searchAllMessages() throws -> Void {
    (try self.searchByQuery("yandex"))
  }

  @discardableResult
  open func searchByQuery(_ query: String) throws -> Void {
    self.messageList.setCurrentContainer(MessageContainer(query, MessageContainerType.search))
  }

  @discardableResult
  open func closeSearch() throws -> Void {
    self.isSearchOpened = false
    self.messageList.setCurrentContainer(self.lastContainer)
    (try self.openMessageModel.closeMessage())
  }

  @discardableResult
  open func clearTextField() throws -> Void {
    self.messageList.setCurrentContainer(self.lastContainer)
  }

  @discardableResult
  open func isInSearch() throws -> Bool {
    return self.isSearchOpened
  }

  @discardableResult
  open func isSearchedForMessages() throws -> Bool {
    return self.messageList.getCurrentContainer().type == MessageContainerType.search
  }

  @discardableResult
  open func openSearch() throws -> Void {
    self.lastContainer = self.messageList.getCurrentContainer()
    self.isSearchOpened = true
  }

}

