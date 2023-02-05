package ru.yandex.yandexbus.inhouse.ui.main.suggests

import org.junit.Before
import org.junit.Test
import org.mockito.InOrder
import org.mockito.Mock
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import ru.yandex.yandexbus.inhouse.BaseTest
import ru.yandex.yandexbus.inhouse.R
import ru.yandex.yandexbus.inhouse.SchedulerProvider
import ru.yandex.yandexbus.inhouse.any
import ru.yandex.yandexbus.inhouse.common.session.TestAppStateNotifier
import ru.yandex.yandexbus.inhouse.datasync.places.Place
import ru.yandex.yandexbus.inhouse.geometry.MapkitPoint
import ru.yandex.yandexbus.inhouse.geometry.Point
import ru.yandex.yandexbus.inhouse.geometry.toDataClass
import ru.yandex.yandexbus.inhouse.model.route.RouteType
import ru.yandex.yandexbus.inhouse.navigation.ScreenChange
import ru.yandex.yandexbus.inhouse.navigation.ScreenChangesNotifier
import ru.yandex.yandexbus.inhouse.place.AddPlaceInteractor
import ru.yandex.yandexbus.inhouse.route.routesetup.RouteTravelInfo
import ru.yandex.yandexbus.inhouse.service.auth.UserManager
import ru.yandex.yandexbus.inhouse.service.location.LocationService
import ru.yandex.yandexbus.inhouse.service.location.UserLocation
import ru.yandex.yandexbus.inhouse.ui.main.suggests.RouteSuggestsContract.ViewState
import ru.yandex.yandexbus.inhouse.ui.main.suggests.RouteSuggestsPresenter.Companion.INITIAL_DATA_TIMEOUT_MS
import ru.yandex.yandexbus.inhouse.utils.ResourceProvider
import ru.yandex.yandexbus.inhouse.utils.Screen
import ru.yandex.yandexbus.inhouse.whenever
import rx.Observable
import rx.schedulers.TestScheduler
import rx.subjects.BehaviorSubject
import java.util.concurrent.TimeUnit

class RouteSuggestsPresenterTest : BaseTest() {

    @Mock
    private lateinit var view: RouteSuggestsContract.View

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    @Mock
    private lateinit var distanceFilter: RoutePointsDistanceFilter

    private lateinit var appStateNotifier: TestAppStateNotifier

    @Mock
    private lateinit var screenChangesNotifier: ScreenChangesNotifier

    @Mock
    private lateinit var locationService: LocationService

    @Mock
    private lateinit var navigator: RouteSuggestsNavigator

    @Mock
    private lateinit var userManager: UserManager

    @Mock
    private lateinit var placeSuggestsInteractor: PlaceSuggestsInteractor

    @Mock
    private lateinit var addPlaceInteractor: AddPlaceInteractor

    @Mock
    private lateinit var searchSuggestsInteractor: SearchSuggestsInteractor

    @Mock
    private lateinit var poiSuggestsInteractor: PoiSuggestsInteractor

    @Mock
    private lateinit var analyticsSender: RouteSuggestsAnalyticsSender

    private lateinit var scheduler: TestScheduler

    private lateinit var presenter: RouteSuggestsPresenter

    @Before
    override fun setUp() {
        super.setUp()

        whenever(resourceProvider.getString(R.string.route_suggest_home))
            .thenReturn(HOME_SUGGEST_ITEM.title)

        whenever(resourceProvider.getString(R.string.route_suggest_work))
            .thenReturn(WORK_SUGGEST_ITEM.title)

        whenever(resourceProvider.getString(R.string.route_suggest_where))
            .thenReturn(WHERE_TO_SUGGEST_ITEM.title)

        whenever(locationService.locations)
            .thenReturn(Observable.just(USER_LOCATION))

        whenever(distanceFilter.isDistantEnough(any(), any()))
            .thenReturn(true)

        whenever(placeSuggestsInteractor.placePoints(any()))
            .thenReturn(Observable.just(emptyList()))

        whenever(searchSuggestsInteractor.searchPoints())
            .thenReturn(Observable.just(emptyList()))

        whenever(poiSuggestsInteractor.poiSuggests())
            .thenReturn(Observable.just(emptyList()))

        whenever(view.clicks)
            .thenReturn(Observable.never())

        whenever(view.suggestsScroll)
            .thenReturn(Observable.never())

        whenever(screenChangesNotifier.screenChanges)
            .thenReturn(Observable.just(ScreenChange(Screen.MAP, ScreenChange.StatusChange.START)))

        appStateNotifier = TestAppStateNotifier()

        scheduler = TestScheduler()

        val schedulerProvider = SchedulerProvider(main = scheduler)

        presenter = RouteSuggestsPresenter(
            resourceProvider,
            schedulerProvider,
            locationService,
            distanceFilter,
            appStateNotifier,
            screenChangesNotifier,
            navigator,
            userManager,
            placeSuggestsInteractor,
            addPlaceInteractor,
            searchSuggestsInteractor,
            poiSuggestsInteractor,
            analyticsSender
        )
    }

    @Test
    fun `only favorites and search suggests are shown without location`() {
        whenever(locationService.locations)
            .thenReturn(Observable.just(null))

        whenever(placeSuggestsInteractor.placePoints(any()))
            .thenReturn(Observable.just(listOf(PLACE_POINT_HOME, PLACE_POINT_WORK)))

        whenever(searchSuggestsInteractor.searchPoints())
            .thenReturn(Observable.just(listOf(SEARCH_POINT)))

        presenter.createAttachStart()
        scheduler.advanceTimeBy(0, TimeUnit.MILLISECONDS)

        shownOnly {
            verifyItemsShownOnce(FAVORITES_ITEM, WHERE_TO_SUGGEST_ITEM)
        }
    }

    @Test
    fun `suggests filtered by distance`() {
        whenever(placeSuggestsInteractor.placePoints(any()))
            .thenReturn(Observable.just(listOf(PLACE_POINT_HOME, PLACE_POINT_WORK)))

        whenever(searchSuggestsInteractor.searchPoints())
            .thenReturn(Observable.just(listOf(SEARCH_POINT)))

        whenever(distanceFilter.isDistantEnough(any(), any()))
            .thenReturn(false)

        presenter.createAttachStart()
        scheduler.advanceTimeBy(0, TimeUnit.MILLISECONDS)

        shownOnly {
            verifyItemsShownOnce(FAVORITES_ITEM, WHERE_TO_SUGGEST_ITEM)
        }
    }

    @Test
    fun `everything shown in time`() {
        whenever(placeSuggestsInteractor.placePoints(any()))
            .thenReturn(Observable.just(listOf(PLACE_POINT_HOME, PLACE_POINT_WORK)))

        whenever(searchSuggestsInteractor.searchPoints())
            .thenReturn(Observable.just(listOf(SEARCH_POINT)))

        presenter.createAttachStart()
        scheduler.advanceTimeBy(0, TimeUnit.MILLISECONDS)

        shownOnly {
            verifyItemsShownOnce(
                FAVORITES_ITEM,
                WHERE_TO_SUGGEST_ITEM
            )
            verifyItemsShownOnce(
                FAVORITES_ITEM,
                WHERE_TO_SUGGEST_ITEM,
                HOME_SUGGEST_ITEM,
                WORK_SUGGEST_ITEM,
                SEARCH_SUGGEST_ITEM
            )
        }
    }

    @Test
    fun `no home results to add place suggest`() {
        presenter.createAttachStart()

        scheduler.advanceTimeBy(0, TimeUnit.MILLISECONDS)

        shownOnly {
            verifyItemsShownOnce(FAVORITES_ITEM, WHERE_TO_SUGGEST_ITEM)
            verifyItemsShownOnce(FAVORITES_ITEM, WHERE_TO_SUGGEST_ITEM, ADD_HOME_SUGGEST_ITEM)
        }
    }

    @Test
    fun `awaits all sources until timeout`() {
        val fastPlaces = Observable.just(listOf(PLACE_POINT_HOME))

        val slowSearch = Observable.just(listOf(SEARCH_POINT))
            .delay(INITIAL_DATA_TIMEOUT_MS * 2, TimeUnit.MILLISECONDS, scheduler)

        whenever(placeSuggestsInteractor.placePoints(any())).thenReturn(fastPlaces)
        whenever(searchSuggestsInteractor.searchPoints()).thenReturn(slowSearch)

        presenter.createAttachStart()
        scheduler.advanceTimeBy(INITIAL_DATA_TIMEOUT_MS, TimeUnit.MILLISECONDS)

        shownOnly {
            verifyItemsShownOnce(FAVORITES_ITEM, WHERE_TO_SUGGEST_ITEM)
            verifyItemsShownOnce(FAVORITES_ITEM, WHERE_TO_SUGGEST_ITEM, HOME_SUGGEST_ITEM)
        }
    }

    @Test
    fun `place without travel time shown as well`() {
        val places = Observable.concat(
            Observable.just(listOf(PLACE_POINT_HOME.copy(routeTravelInfo = null))),
            Observable.just(listOf(PLACE_POINT_HOME))
                .delay(INITIAL_DATA_TIMEOUT_MS * 2, TimeUnit.MILLISECONDS, scheduler)
        )
        whenever(placeSuggestsInteractor.placePoints(any())).thenReturn(places)

        presenter.createAttachStart()
        scheduler.advanceTimeBy(INITIAL_DATA_TIMEOUT_MS * 2, TimeUnit.MILLISECONDS)

        shownOnly {
            verifyItemsShownOnce(FAVORITES_ITEM, WHERE_TO_SUGGEST_ITEM)
            verifyItemsShownOnce(FAVORITES_ITEM, WHERE_TO_SUGGEST_ITEM, HOME_SUGGEST_ITEM.copy(routeTravelInfo = null))
            verifyItemsShownOnce(FAVORITES_ITEM, WHERE_TO_SUGGEST_ITEM, HOME_SUGGEST_ITEM)
        }
    }

    @Test
    fun `no 'add home' suggest on timeout`() {
        val places = Observable.just(listOf(PLACE_POINT_HOME))
            .delay(INITIAL_DATA_TIMEOUT_MS * 2, TimeUnit.MILLISECONDS, scheduler)

        whenever(placeSuggestsInteractor.placePoints(any())).thenReturn(places)

        presenter.createAttachStart()
        scheduler.advanceTimeBy(INITIAL_DATA_TIMEOUT_MS, TimeUnit.MILLISECONDS)

        shownOnly {
            verifyItemsShownOnce(FAVORITES_ITEM, WHERE_TO_SUGGEST_ITEM)
        }
    }

    @Test
    fun `new search suggests receival after timeout`() {
        val searchPoints = BehaviorSubject.create(listOf(SEARCH_POINT))

        whenever(searchSuggestsInteractor.searchPoints()).thenReturn(searchPoints)

        presenter.createAttachStart()
        scheduler.advanceTimeBy(INITIAL_DATA_TIMEOUT_MS * 2, TimeUnit.MILLISECONDS)

        val newSearchPoint = SEARCH_POINT.copy(queryText = "new search result")

        searchPoints.onNext(listOf(newSearchPoint))

        shownOnly {
            verifyItemsShownOnce(
                FAVORITES_ITEM,
                WHERE_TO_SUGGEST_ITEM
            )
            verifyItemsShownOnce(
                FAVORITES_ITEM,
                WHERE_TO_SUGGEST_ITEM,
                SEARCH_SUGGEST_ITEM,
                ADD_HOME_SUGGEST_ITEM
            )
            verifyItemsShownOnce(
                FAVORITES_ITEM,
                WHERE_TO_SUGGEST_ITEM,
                SEARCH_SUGGEST_ITEM.copy(title = newSearchPoint.queryText),
                ADD_HOME_SUGGEST_ITEM
            )
        }
    }

    @Test
    fun `search point equal to place omitted`() {
        whenever(placeSuggestsInteractor.placePoints(any()))
            .thenReturn(Observable.just(listOf(PLACE_POINT_HOME)))

        whenever(searchSuggestsInteractor.searchPoints())
            .thenReturn(Observable.just(listOf(SEARCH_POINT.copy(point = PLACE_POINT_HOME.point))))

        presenter.createAttachStart()
        scheduler.advanceTimeBy(0, TimeUnit.MILLISECONDS)

        shownOnly {
            verifyItemsShownOnce(FAVORITES_ITEM, WHERE_TO_SUGGEST_ITEM)
            verifyItemsShownOnce(FAVORITES_ITEM, WHERE_TO_SUGGEST_ITEM, HOME_SUGGEST_ITEM)
        }
    }

    @Test
    fun `poi is not shown if not enough space remains`() {
        val newSearchText = "new search"

        whenever(placeSuggestsInteractor.placePoints(any()))
            .thenReturn(Observable.just(listOf(PLACE_POINT_HOME, PLACE_POINT_WORK)))

        whenever(searchSuggestsInteractor.searchPoints())
            .thenReturn(Observable.just(listOf(SEARCH_POINT, SEARCH_POINT.copy(queryText = newSearchText))))

        whenever(poiSuggestsInteractor.poiSuggests())
            .thenReturn(Observable.just(listOf(POI_SUGGEST)))

        presenter.createAttachStart()
        scheduler.advanceTimeBy(0, TimeUnit.MILLISECONDS)

        shownOnly {
            verifyItemsShownOnce(
                FAVORITES_ITEM,
                WHERE_TO_SUGGEST_ITEM
            )
            verifyItemsShownOnce(
                FAVORITES_ITEM,
                WHERE_TO_SUGGEST_ITEM,
                HOME_SUGGEST_ITEM,
                WORK_SUGGEST_ITEM,
                SEARCH_SUGGEST_ITEM,
                SEARCH_SUGGEST_ITEM.copy(title = newSearchText)
            )
        }
    }

    @Test
    fun `poi is shown if enough space remains`() {
        whenever(poiSuggestsInteractor.poiSuggests())
            .thenReturn(Observable.just(listOf(POI_SUGGEST)))

        presenter.createAttachStart()
        scheduler.advanceTimeBy(0, TimeUnit.MILLISECONDS)

        shownOnly {
            verifyItemsShownOnce(
                FAVORITES_ITEM,
                WHERE_TO_SUGGEST_ITEM
            )
            verifyItemsShownOnce(
                FAVORITES_ITEM,
                WHERE_TO_SUGGEST_ITEM,
                ADD_HOME_SUGGEST_ITEM,
                POI_SUGGEST_ITEM
            )
        }
    }

    private fun InOrder.verifyItemsShownOnce(vararg items: SuggestItem) {
        verify(view).show(ViewState(items.toList()))
    }

    private inline fun shownOnly(block: InOrder.() -> Unit) {
        inOrder(view).block()
        verify(view, atLeastOnce()).clicks
        verify(view, atLeastOnce()).suggestsScroll
        verifyNoMoreInteractions(view)
    }

    private fun RouteSuggestsPresenter.createAttachStart() {
        onCreate()
        onAttach(view)

        appStateNotifier.onAppGoesForeground()
        onViewStart()
    }

    private companion object {

        val USER_LOCATION = UserLocation(
            position = MapkitPoint(
                500.0,
                500.0
            )
        )

        val FAVORITES_ITEM = FavoritesSuggestItem

        val WHERE_TO_SUGGEST_ITEM = RouteSuggestItem.WhereToGo("where?")

        val PLACE_POINT_HOME = PlaceSuggestsInteractor.PlacePoint(
            Point(0.0, 0.0),
            Place.Type.HOME,
            RouteTravelInfo(
                USER_LOCATION.position.toDataClass(),
                Point(0.0, 0.0),
                100.0,
                "takes some time",
                RouteType.MASSTRANSIT
            )
        )

        val HOME_SUGGEST_ITEM = RouteSuggestItem.Common(
            R.drawable.saved_place_home_card_icon_24,
            "home",
            USER_LOCATION.position.toDataClass(),
            PLACE_POINT_HOME.point,
            PLACE_POINT_HOME.routeTravelInfo,
            RouteSuggestItem.SuggestType.HOME
        )


        val PLACE_POINT_WORK = PlaceSuggestsInteractor.PlacePoint(
            Point(1.0, 0.0),
            Place.Type.WORK,
            RouteTravelInfo(
                USER_LOCATION.position.toDataClass(),
                Point(1.0, 0.0),
                100.0,
                "takes some time",
                RouteType.MASSTRANSIT
            )
        )

        val WORK_SUGGEST_ITEM = RouteSuggestItem.Common(
            R.drawable.saved_place_work_card_icon_24,
            "work",
            USER_LOCATION.position.toDataClass(),
            PLACE_POINT_WORK.point,
            PLACE_POINT_WORK.routeTravelInfo,
            RouteSuggestItem.SuggestType.WORK
        )


        val SEARCH_POINT = SearchSuggestsInteractor.SearchPoint(
            Point(2.0, 0.0),
            "search result"
        )

        val SEARCH_SUGGEST_ITEM = RouteSuggestItem.Common(
            icon = null,
            title = SEARCH_POINT.queryText,
            departure = USER_LOCATION.position.toDataClass(),
            destination = SEARCH_POINT.point,
            type = RouteSuggestItem.SuggestType.SEARCH_HISTORY
        )


        val ADD_HOME_SUGGEST_ITEM = RouteSuggestItem.AddPlace(
            R.drawable.saved_place_home_card_icon_24,
            HOME_SUGGEST_ITEM.title,
            RouteSuggestItem.SuggestType.HOME_ADD
        )


        val POI_SUGGEST = PoiSuggest("Red Square", Point(55.753595, 37.621031))

        val POI_SUGGEST_ITEM = RouteSuggestItem.Common(
            icon = null,
            title = POI_SUGGEST.name,
            departure = USER_LOCATION.position.toDataClass(),
            destination = POI_SUGGEST.location,
            type = RouteSuggestItem.SuggestType.POI
        )
    }
}
