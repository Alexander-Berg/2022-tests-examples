package ru.yandex.yandexmaps.multiplatform.uitesting.api.pageobjects

public interface SearchResultCardPage {
    public fun closeCard()
    public fun placecardTitle(): String
    public fun transportCardTitle(): String
    public fun tapRoute()
    public fun isDirectVisible(): Boolean
    public fun tapAdBlock()
    public fun hasGeoProductAd(): Boolean
    public fun isAdDetailsSheetShown(): Boolean
}
