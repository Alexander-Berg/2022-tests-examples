package ru.yandex.yandexmaps.multiplatform.uitesting.api.pageobjects

public interface DirectionsWaypointsSelectionPage {
    public fun closeDirections()
    public fun tapFromField()
    public fun tapToField()
    public fun setInputText(text: String)
    public fun tapToSelectOnMap()
    public fun isSelectOnMapShown(): Boolean
}
