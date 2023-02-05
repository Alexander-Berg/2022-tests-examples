package ru.yandex.yandexmaps.multiplatform.uitesting.internal.testcases

import ru.yandex.yandexmaps.multiplatform.mapkit.extensions.toNativePoint
import ru.yandex.yandexmaps.multiplatform.mapkit.map.CameraPositionFactory
import ru.yandex.yandexmaps.multiplatform.uitesting.api.interactors.toMockLocation
import ru.yandex.yandexmaps.multiplatform.uitesting.api.pageobjects.OrientationMode
import ru.yandex.yandexmaps.multiplatform.uitesting.api.pageobjects.SearchCategory
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseDsl
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.testcases.helpers.KnownLocations

internal fun TestCaseDsl.openCarGuidance() {
    pages.startScreen.tapDirectionsTabButton()
    pages.directions.routeVariants.tapOnCarTab()
    pages.directions.wayPointsSelection.tapToField()
    pages.directions.wayPointsSelection.setInputText("Садовническая улица, 82с2")
    pages.directions.wayPointsSelection.suggests.openSuggestWithText("Садовническая улица, 82с2")
    pages.directions.routeVariants.tapLetsGoButton()
}

internal fun TestCaseDsl.setLocationAndSpanAtYandex() {
    interactor.mockLocation(KnownLocations.YANDEX_CENTRAL_OFFICE.toMockLocation())
    pages.startScreen.map.setCameraPosition(
        CameraPositionFactory.createCameraPosition(KnownLocations.YANDEX_CENTRAL_OFFICE.toNativePoint(), 16.0f, 0.0f, 0.0f)
    )
}

internal fun TestCaseDsl.findUsingSearchTab(searchText: String) {
    pages.startScreen.tapSearchTabButton()
    pages.search.tapSearchField()
    pages.search.history.setSearchText(searchText)
    pages.search.suggests.tapSearchButton()
}

internal fun TestCaseDsl.setSpanAtMoscow() {
    pages.startScreen.map.setCameraPosition(
        CameraPositionFactory.createCameraPosition(KnownLocations.MOSCOW_CENTER.toNativePoint(), 12.0f, 0.0f, 0.0f)
    )
}

internal fun TestCaseDsl.setLocationAtYandexAndSpanAtMoscow() {
    interactor.mockLocation(KnownLocations.YANDEX_CENTRAL_OFFICE.toMockLocation())
    pages.startScreen.map.setCameraPosition(
        CameraPositionFactory.createCameraPosition(KnownLocations.MOSCOW_CENTER.toNativePoint(), 12.0f, 0.0f, 0.0f)
    )
}

internal fun TestCaseDsl.openToponymCard() {
    val searchText = "3-я Фрунзенская улица, 15"
    pages.startScreen.tapSearchTabButton()
    pages.search.tapSearchField()
    pages.search.history.setSearchText(searchText)
    pages.search.suggests.tapSearchButton()

    assert("Открыта карточка топонима") {
        assertEqual(pages.search.results.card.placecardTitle(), searchText, "Wrong value for search result card title!")
    }
}

internal fun TestCaseDsl.setSpanAtStops() {
    pages.startScreen.map.setCameraPosition(
        CameraPositionFactory.createCameraPosition(KnownLocations.DAY_NIGHT_BUS_STOP.toNativePoint(), 19.0f, 0.0f, 0.0f)
    )
}

internal fun TestCaseDsl.setSpanAtStopsFar() {
    pages.startScreen.map.setCameraPosition(
        CameraPositionFactory.createCameraPosition(KnownLocations.DAY_NIGHT_BUS_STOP.toNativePoint(), 14.0f, 0.0f, 0.0f)
    )
}

internal fun TestCaseDsl.setLandscapeOrientation() {
    pages.device.setOrientation(OrientationMode.LANDSCAPE)
}

internal fun TestCaseDsl.setPortraitOrientation() {
    pages.device.setOrientation(OrientationMode.PORTRAIT)
}

internal fun TestCaseDsl.login() {
    val login = "yndx-kripp-le-n31lm7"
    val pass = "gen2688"

    pages.menu.page.openMenu()
    pages.settings.openLogin()
    pages.accountManager.logIn(login, pass)

    assert("Вход выполнен") {
        assertEqual(pages.settings.getLoginName(), login, "Wrong login name not equal!")
    }

    pages.settings.closeSettings()
}

internal fun TestCaseDsl.searchByAdCategoryUntilResultsAppeared(): SearchCategory? {
    if (!pages.search.waitForSearchShutterVisible()) {
        pages.startScreen.tapSearchTabButton()
        assert("""Открыт экран поиска""") {
            assertEqual(pages.search.waitForSearchShutterVisible(), true, "Search shutter not found!")
        }
    }
    return pages.search.getAdvertisementSearchCategories().firstOrNull { adCategory ->
        pages.search.tapSearchCategory(adCategory)
        if (pages.search.results.hasSearchResults()) {
            true
        } else {
            pages.search.results.closeSearchResults()
            pages.startScreen.tapSearchTabButton()
            assert("""Открыт экран поиска""") {
                assertEqual(pages.search.waitForSearchShutterVisible(), true, "Search shutter not found!")
            }
            false
        }
    }
}
