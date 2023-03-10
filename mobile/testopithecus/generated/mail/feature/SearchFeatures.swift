// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/feature/search-features.ts >>>

import Foundation

open class ZeroSuggestFeature: Feature<ZeroSuggest> {
  public static var `get`: ZeroSuggestFeature = ZeroSuggestFeature()
  private init() {
    super.init("ZeroSuggest", "ZeroSuggest - история успешных запросов. " + "Экран появляется при открытии поиска (после выполнения экшена openSearch()). " + "Запросы сохраняются в саджест только при открытии в поисковой выдаче письма или аттача на просмотр.")
  }

}

public protocol ZeroSuggest {
  @discardableResult
  func isShown() throws -> Bool
  @discardableResult
  func getZeroSuggest() throws -> YSArray<String>
  @discardableResult
  func searchByZeroSuggest(_ suggest: String) throws -> Void
}

open class SearchFeature: Feature<Search> {
  public static var `get`: SearchFeature = SearchFeature()
  private init() {
    super.init("Search", "Фича поиска писем из поисковой строки. Для мобильных InstantSearch." + " Весь поиск осуществляется после выполнения экшена openSearch()")
  }

}

public protocol Search {
  @discardableResult
  func searchAllMessages() throws -> Void
  @discardableResult
  func closeSearch() throws -> Void
  @discardableResult
  func clearTextField() throws -> Void
  @discardableResult
  func isInSearch() throws -> Bool
  @discardableResult
  func isSearchedForMessages() throws -> Bool
  @discardableResult
  func openSearch() throws -> Void
  @discardableResult
  func searchByQuery(_ query: String) throws -> Void
}

open class AdvancedSearchFeature: Feature<AdvancedSearch> {
  public static var `get`: AdvancedSearchFeature = AdvancedSearchFeature()
  private init() {
    super.init("AdvancedSearch", "Фича расширенного поиска. Для мобильных InstantSearch." + " Весь поиск осуществляется после выполнения экшена openSearch() и поиска по обычному запросу " + "(до этого момента расширенный поиск не видно)")
  }

}

public protocol AdvancedSearch {
  @discardableResult
  func addLabelToSearch(_ labelName: LabelName) throws -> Void
  @discardableResult
  func addFolderToSearch(_ folderName: FolderName) throws -> Void
  @discardableResult
  func searchOnlyImportant() throws -> Void
}

