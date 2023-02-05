package ru.yandex.yandexmaps.multiplatform.uitesting.internal

import ru.yandex.yandexmaps.multiplatform.uitesting.api.PageObjectsProvider
import ru.yandex.yandexmaps.multiplatform.uitesting.api.pageobjects.AccountManagerPage
import ru.yandex.yandexmaps.multiplatform.uitesting.api.pageobjects.DevicePage
import ru.yandex.yandexmaps.multiplatform.uitesting.api.pageobjects.DirectionsCarGuidancePage
import ru.yandex.yandexmaps.multiplatform.uitesting.api.pageobjects.DirectionsMasstransitGuidancePage
import ru.yandex.yandexmaps.multiplatform.uitesting.api.pageobjects.DirectionsPedestrianGuidancePage
import ru.yandex.yandexmaps.multiplatform.uitesting.api.pageobjects.DirectionsRouteMtVariantsPage
import ru.yandex.yandexmaps.multiplatform.uitesting.api.pageobjects.DirectionsRouteVariantsMtDetailsPage
import ru.yandex.yandexmaps.multiplatform.uitesting.api.pageobjects.DirectionsRouteVariantsPage
import ru.yandex.yandexmaps.multiplatform.uitesting.api.pageobjects.DirectionsWaypointsSelectionPage
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
import ru.yandex.yandexmaps.multiplatform.uitesting.api.pageobjects.SettingsPage

internal class PageObjectsAdapter(private val pageObjects: PageObjectsProvider) {

    val accountManager = AccountManager()
    val device = Device()
    val card = Card()
    val directions = Directions()
    val menu = Menu()
    val search = Search()
    val settings = Settings()
    val startScreen = Main()
    val mapLongTapScreen = MapLongTapScreen()
    val mapLayersScreen = MapLayersScreen()

    inner class AccountManager : AccountManagerPage by pageObjects.accountManagerPage

    inner class Card {
        val searchResult = pageObjects.searchResultCardPage
        val transportCard = pageObjects.transportCardPage
        val stationCard = pageObjects.stationCardPage
    }

    inner class Directions {
        val guidance = Guidance()
        val routeVariants = RouteVariants()
        val wayPointsSelection = WayPointsSelection()
        val selectPointOnMap = pageObjects.selectPointOnMapPage

        inner class Guidance {
            val car = Car()
            val mt = Masstransit()
            val pedestrian = Pedestrian()
            val bike = pedestrian

            inner class Car : DirectionsCarGuidancePage by pageObjects.directionsCarGuidancePage

            inner class Pedestrian : DirectionsPedestrianGuidancePage by pageObjects.directionsPedestrianGuidancePage

            inner class Masstransit : DirectionsMasstransitGuidancePage by pageObjects.directionsMasstransitGuidancePage
        }

        inner class RouteVariants : DirectionsRouteVariantsPage by pageObjects.directionsRouteVariantsPage {
            val mt = MtVariants()

            inner class MtVariants : DirectionsRouteMtVariantsPage by pageObjects.directionsRouteMtVariantsPage {

                val details = MtDetails()

                inner class MtDetails : DirectionsRouteVariantsMtDetailsPage by pageObjects.directionsRouteVariantsMtDetailsPage
            }
        }

        inner class WayPointsSelection : DirectionsWaypointsSelectionPage by pageObjects.directionsWaypointsSelectionPage {
            val suggests = pageObjects.directionsWaypointsSuggestsPage
        }
    }

    inner class Main : MainScreenPage by pageObjects.mainScreenPage {
        val map: MapPage = pageObjects.mapPage
    }

    inner class MapLongTapScreen : MapLongTapPage by pageObjects.mapLongTapPage

    inner class MapLayersScreen : MapLayersPage by pageObjects.mapLayersPage

    inner class Menu {
        val page: MenuPage = pageObjects.menuPage
    }

    inner class Search : SearchStartScreenPage by pageObjects.searchStartScreenPage {
        val history: SearchHistoryPage = pageObjects.searchHistoryPage
        val results = Results()
        val suggests: SearchSuggestsPage = pageObjects.searchSuggestsPage

        inner class Results : SearchResultsPage by pageObjects.searchResultsPage {
            val card: SearchResultCardPage = pageObjects.searchResultCardPage
        }
    }

    inner class Settings : SettingsPage by pageObjects.settingsPage {
        val general: GeneralSettingsPage = pageObjects.generalSettingsPage
    }

    inner class Device : DevicePage by pageObjects.devicePage
}
