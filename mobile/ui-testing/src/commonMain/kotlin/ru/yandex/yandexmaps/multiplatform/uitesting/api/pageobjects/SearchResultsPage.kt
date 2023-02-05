package ru.yandex.yandexmaps.multiplatform.uitesting.api.pageobjects

public interface SearchResultsPage {
    public fun hasSearchResults(): Boolean
    public fun getTopSearchResultTitle(): String
    public fun openTopSearchResult()
    public fun clickOnFirstGeoproduct()
    public fun isAdDetailsSheetShown(): Boolean
    public fun clickOnFirstDirectWithoutScrolling()
    public fun clickFirstSearchResult()
    public fun scrollToSearchResultWithDirectItem()
    public fun isSearchResultWithDirectItemVisible(): Boolean
    public fun closeSearchResults()
    public fun openTopSearchResultWithAd()
}
