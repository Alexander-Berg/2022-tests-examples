package ru.yandex.yandexmaps.multiplatform.uitesting.api.pageobjects

public interface GasStationsPromoPage {
    public fun directionsGasStationsBannerIsVisible(): Boolean
    public fun openGasStationsPromoFromDirections()
    public fun showGasStationsInSearch()
    public fun closeGasStationsPromo()
}
