package ru.yandex.yandexmaps.probator.model

import ru.yandex.yandexmaps.multiplatform.uitesting.api.Application
import ru.yandex.yandexmaps.multiplatform.uitesting.api.MetricsEvent
import ru.yandex.yandexmaps.multiplatform.uitesting.api.interactors.MockLocation
import ru.yandex.yandexmaps.multiplatform.uitesting.api.interactors.MockLocationInteractor
import ru.yandex.yandexmaps.multiplatform.uitesting.api.pageobjects.AccountManagerPage
import ru.yandex.yandexmaps.multiplatform.uitesting.api.pageobjects.DevicePage
import ru.yandex.yandexmaps.multiplatform.uitesting.api.pageobjects.DirectionsCarGuidancePage
import ru.yandex.yandexmaps.multiplatform.uitesting.api.pageobjects.DirectionsMasstransitGuidancePage
import ru.yandex.yandexmaps.multiplatform.uitesting.api.pageobjects.DirectionsPedestrianGuidancePage
import ru.yandex.yandexmaps.multiplatform.uitesting.api.pageobjects.DirectionsRouteMtVariantsPage
import ru.yandex.yandexmaps.multiplatform.uitesting.api.pageobjects.DirectionsRouteVariantsMtDetailsPage
import ru.yandex.yandexmaps.multiplatform.uitesting.api.pageobjects.DirectionsRouteVariantsPage
import ru.yandex.yandexmaps.multiplatform.uitesting.api.pageobjects.DirectionsWaypointsSelectionPage
import ru.yandex.yandexmaps.multiplatform.uitesting.api.pageobjects.DirectionsWaypointsSuggestsPage
import ru.yandex.yandexmaps.multiplatform.uitesting.api.pageobjects.GeneralSettingsPage
import ru.yandex.yandexmaps.multiplatform.uitesting.api.pageobjects.MainScreenPage
import ru.yandex.yandexmaps.multiplatform.uitesting.api.pageobjects.MapLayersPage
import ru.yandex.yandexmaps.multiplatform.uitesting.api.pageobjects.MapLongTapPage
import ru.yandex.yandexmaps.multiplatform.uitesting.api.pageobjects.MapPage
import ru.yandex.yandexmaps.multiplatform.uitesting.api.pageobjects.MenuPage
import ru.yandex.yandexmaps.multiplatform.uitesting.api.pageobjects.SearchHistoryPage
import ru.yandex.yandexmaps.multiplatform.uitesting.api.pageobjects.SearchResultCardPage
import ru.yandex.yandexmaps.multiplatform.uitesting.api.pageobjects.SearchResultsPage
import ru.yandex.yandexmaps.multiplatform.uitesting.api.pageobjects.SearchStartScreenPage
import ru.yandex.yandexmaps.multiplatform.uitesting.api.pageobjects.SearchSuggestsPage
import ru.yandex.yandexmaps.multiplatform.uitesting.api.pageobjects.SelectPointOnMapPage
import ru.yandex.yandexmaps.multiplatform.uitesting.api.pageobjects.SettingsPage
import ru.yandex.yandexmaps.multiplatform.uitesting.api.pageobjects.StationCardPage
import ru.yandex.yandexmaps.multiplatform.uitesting.api.pageobjects.TransportCardPage
import javax.inject.Inject

class TestingApplication @Inject constructor(
    override val accountManagerPage: AccountManagerPage,
    override val devicePage: DevicePage,
    override val directionsCarGuidancePage: DirectionsCarGuidancePage,
    override val directionsPedestrianGuidancePage: DirectionsPedestrianGuidancePage,
    override val directionsMasstransitGuidancePage: DirectionsMasstransitGuidancePage,
    override val directionsRouteVariantsMtDetailsPage: DirectionsRouteVariantsMtDetailsPage,
    override val directionsRouteVariantsPage: DirectionsRouteVariantsPage,
    override val directionsRouteMtVariantsPage: DirectionsRouteMtVariantsPage,
    override val directionsWaypointsSelectionPage: DirectionsWaypointsSelectionPage,
    override val directionsWaypointsSuggestsPage: DirectionsWaypointsSuggestsPage,
    override val generalSettingsPage: GeneralSettingsPage,
    override val mainScreenPage: MainScreenPage,
    override val mapPage: MapPage,
    override val mapLongTapPage: MapLongTapPage,
    override val mapLayersPage: MapLayersPage,
    override val menuPage: MenuPage,
    override val searchHistoryPage: SearchHistoryPage,
    override val searchResultCardPage: SearchResultCardPage,
    override val searchResultsPage: SearchResultsPage,
    override val searchStartScreenPage: SearchStartScreenPage,
    override val searchSuggestsPage: SearchSuggestsPage,
    override val settingsPage: SettingsPage,
    override val transportCardPage: TransportCardPage,
    override val stationCardPage: StationCardPage,
    override val selectPointOnMapPage: SelectPointOnMapPage,
    val mockLocationInteractor: MockLocationInteractor,
) : Application, AutoCloseable {

    init {
        MetricsTracker.beginTest()
    }

    override fun close() {
        MetricsTracker.endTest()
    }

    override fun getMetricsEvents(): List<MetricsEvent> = MetricsTracker.getMetricsEvents()

    override fun mockLocation(location: MockLocation) {
        mockLocationInteractor.setMockLocation(location)
    }
}
