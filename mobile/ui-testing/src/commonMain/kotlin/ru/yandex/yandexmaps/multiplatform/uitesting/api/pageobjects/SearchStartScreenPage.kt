package ru.yandex.yandexmaps.multiplatform.uitesting.api.pageobjects

public class SearchCategory(
    public val text: String
)

public interface SearchStartScreenPage {
    public fun waitForSearchShutterVisible(): Boolean
    public fun closeSearch()
    public fun getRestaurantsCategory(): SearchCategory?
    public fun getVisibleCommonSearchCategories(): Set<SearchCategory>
    public fun getAdvertisementSearchCategories(): Set<SearchCategory>
    public fun areMoreCommonSearchCategoriesAvailable(): Boolean
    public fun tapSearchField()
    public fun showMoreCommonSearchCategories()
    public fun tapSearchCategory(category: SearchCategory)
    public fun waitForCategoriesDownload()
    public fun isVoiceSearchButtonVisible(): Boolean
}
