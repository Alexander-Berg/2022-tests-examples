package ru.yandex.yandexmaps.multiplatform.uitesting.api.pageobjects

public interface StationCardPage {
    public fun tapCloseCard()
    public fun tapTransportItem()
    public fun hasTransportItems(): Boolean
    public fun titleText(): String
}
