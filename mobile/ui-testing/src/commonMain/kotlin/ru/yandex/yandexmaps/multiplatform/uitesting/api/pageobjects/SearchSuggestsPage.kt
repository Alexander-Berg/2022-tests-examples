package ru.yandex.yandexmaps.multiplatform.uitesting.api.pageobjects

public interface SearchSuggestsPage {
    public fun hasSuggests(): Boolean
    public fun hasSuggestWithText(text: String): Boolean
    public fun openTopSuggest()
    public fun openSuggestWithText(text: String)
    public fun getSearchText(): String
    public fun setSearchText(text: String?) // null corresponds to placeholder value
    public fun tapSearchButton()
    public fun closeSuggests()
}
