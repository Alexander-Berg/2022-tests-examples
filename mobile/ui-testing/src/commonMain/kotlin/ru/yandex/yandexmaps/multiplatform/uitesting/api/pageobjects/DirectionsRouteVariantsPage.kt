package ru.yandex.yandexmaps.multiplatform.uitesting.api.pageobjects

public interface DirectionsRouteVariantsPage {

    public fun tapLetsGoButton()

    public fun tapOnAllTab()
    public fun tapOnCarTab()
    public fun tapOnMtTab()
    public fun tapOnPedestrianTab()
    public fun tapOnTaxiTab()
    public fun tapOnBikeTab()
    public fun tapOnScooterTab()

    public fun tapOnEachVariant()
    public fun tapOnFirstVariant()

    public fun hasLoginButtonInTaxiSnippet(): Boolean
    public fun tapOnEachTaxiVariant()
    public fun tapOnFirstTaxiVariant()

    public fun isDirectionsScreenShown(): Boolean
}
