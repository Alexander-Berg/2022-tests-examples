package ru.yandex.yandexmaps.multiplatform.uitesting.api.pageobjects

public interface DirectionsCarGuidancePage {
    public fun tapSearch()
    public fun tapCloseSearch()

    public fun searchResultsVisible(): Boolean
    public fun searchResultsSearchText(): String?

    public fun etaVisible(): Boolean
}
