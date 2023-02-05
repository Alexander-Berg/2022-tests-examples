package ru.yandex.yandexmaps.multiplatform.uitesting.api

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

/*
    Lines must be sorted!
 */
public interface PageObjectsProvider {
    public val accountManagerPage: AccountManagerPage
    public val devicePage: DevicePage
    public val directionsCarGuidancePage: DirectionsCarGuidancePage
    public val directionsPedestrianGuidancePage: DirectionsPedestrianGuidancePage
    public val directionsMasstransitGuidancePage: DirectionsMasstransitGuidancePage
    public val directionsRouteVariantsMtDetailsPage: DirectionsRouteVariantsMtDetailsPage
    public val directionsRouteVariantsPage: DirectionsRouteVariantsPage
    public val directionsRouteMtVariantsPage: DirectionsRouteMtVariantsPage
    public val directionsWaypointsSelectionPage: DirectionsWaypointsSelectionPage
    public val directionsWaypointsSuggestsPage: DirectionsWaypointsSuggestsPage
    public val generalSettingsPage: GeneralSettingsPage
    public val mainScreenPage: MainScreenPage
    public val mapLongTapPage: MapLongTapPage
    public val mapLayersPage: MapLayersPage
    public val mapPage: MapPage
    public val menuPage: MenuPage
    public val searchHistoryPage: SearchHistoryPage
    public val searchResultCardPage: SearchResultCardPage
    public val searchResultsPage: SearchResultsPage
    public val searchStartScreenPage: SearchStartScreenPage
    public val searchSuggestsPage: SearchSuggestsPage
    public val settingsPage: SettingsPage
    public val transportCardPage: TransportCardPage
    public val stationCardPage: StationCardPage
    public val selectPointOnMapPage: SelectPointOnMapPage
}
