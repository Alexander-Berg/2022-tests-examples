package ru.yandex.yandexmaps.multiplatform.uitesting.api.pageobjects

public interface SearchHistoryPage {
    public fun closeSearch()
    public fun setSearchText(text: String?) // null corresponds to placeholder value
    public fun tapSearchCategory(category: SearchCategory)
}
