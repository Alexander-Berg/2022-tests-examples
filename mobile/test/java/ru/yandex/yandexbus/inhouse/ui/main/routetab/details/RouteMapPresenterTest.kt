package ru.yandex.yandexbus.inhouse.ui.main.routetab.details

import com.yandex.mapkit.geometry.BoundingBox
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.geometry.Polyline
import com.yandex.mapkit.geometry.PolylinePosition
import com.yandex.mapkit.geometry.Subpolyline
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.verifyZeroInteractions
import ru.yandex.yandexbus.inhouse.BaseTest
import ru.yandex.yandexbus.inhouse.any
import ru.yandex.yandexbus.inhouse.map.events.MapTouchEvent
import ru.yandex.yandexbus.inhouse.model.CityLocationInfo
import ru.yandex.yandexbus.inhouse.model.Hotspot
import ru.yandex.yandexbus.inhouse.model.Vehicle
import ru.yandex.yandexbus.inhouse.model.VehicleType
import ru.yandex.yandexbus.inhouse.model.route.MasstransitRouteModel
import ru.yandex.yandexbus.inhouse.model.route.PedestrianRouteModel
import ru.yandex.yandexbus.inhouse.model.route.RouteModel
import ru.yandex.yandexbus.inhouse.model.route.RouteModel.RouteSection
import ru.yandex.yandexbus.inhouse.model.route.RouteModel.RouteStop
import ru.yandex.yandexbus.inhouse.model.route.RouteModel.Transport
import ru.yandex.yandexbus.inhouse.model.route.RoutePoint
import ru.yandex.yandexbus.inhouse.model.route.TaxiRouteModel
import ru.yandex.yandexbus.inhouse.repos.TimeLimitation
import ru.yandex.yandexbus.inhouse.service.masstransit.MasstransitService
import ru.yandex.yandexbus.inhouse.service.settings.RegionSettings
import ru.yandex.yandexbus.inhouse.service.taxi.Ride
import ru.yandex.yandexbus.inhouse.service.taxi.TaxiOperator
import ru.yandex.yandexbus.inhouse.ui.main.routetab.details.map.RouteMapPresenter
import ru.yandex.yandexbus.inhouse.ui.main.routetab.details.map.RouteMapPresenter.ViewState
import ru.yandex.yandexbus.inhouse.ui.main.routetab.details.map.RouteMapView
import ru.yandex.yandexbus.inhouse.ui.main.routetab.details.map.model.AddressElement
import ru.yandex.yandexbus.inhouse.ui.main.routetab.details.map.model.MasstransitRouteSections
import ru.yandex.yandexbus.inhouse.ui.main.routetab.details.map.model.MasstransitSectionElement
import ru.yandex.yandexbus.inhouse.ui.main.routetab.details.map.model.PedestrianRouteSections
import ru.yandex.yandexbus.inhouse.ui.main.routetab.details.map.model.PedestrianSectionElement
import ru.yandex.yandexbus.inhouse.ui.main.routetab.details.map.model.RouteElementFactory
import ru.yandex.yandexbus.inhouse.ui.main.routetab.details.map.model.RouteElements
import ru.yandex.yandexbus.inhouse.ui.main.routetab.details.map.model.StopElement
import ru.yandex.yandexbus.inhouse.ui.main.routetab.details.map.model.TaxiRouteSections
import ru.yandex.yandexbus.inhouse.ui.main.routetab.details.map.model.TaxiSectionElement
import ru.yandex.yandexbus.inhouse.utils.argumentCaptor
import ru.yandex.yandexbus.inhouse.whenever
import rx.Observable
import rx.Single
import rx.subjects.PublishSubject

class RouteMapPresenterTest : BaseTest() {

    @Mock
    lateinit var argumentsHolder: RouteDetailsArgumentsHolder
    @Mock
    lateinit var routeStopsRepository: FavoriteStopsRepository
    @Mock
    lateinit var routeElementFactory: RouteElementFactory
    @Mock
    lateinit var mapProxy: MapProxyWrapper
    @Mock
    lateinit var jamsUpdater: JamsUpdater
    @Mock
    lateinit var masstransitService: MasstransitService
    @Mock
    lateinit var regionSettings: RegionSettings
    @Mock
    lateinit var navigator: RouteDetailsNavigator

    @Mock
    lateinit var view: RouteMapView

    private val pageChanges = PublishSubject.create<Int>()
    private var elements = emptyList<RouteElements>()

    private lateinit var args: RouteDetailsArgs

    override fun setUp() {
        super.setUp()

        args = buildArgs()
        whenever(argumentsHolder.get()).thenReturn(args)

        whenever(routeStopsRepository.favoriteStopIds()).thenReturn(Observable.just(favouriteStops()))
        whenever(regionSettings.findRegion(any())).thenReturn(Single.just(CityLocationInfo.UNKNOWN))
        whenever(masstransitService.hotspot(STOP_ELEMENT.stop!!.stopId)).thenReturn(Single.just(HOTSPOT))

        whenever(mapProxy.zoomRangeChanges()).thenReturn(Observable.never())
        whenever(mapProxy.mapTouchEvents()).thenReturn(Observable.never())

        whenever(jamsUpdater.jamsUpdates(any())).thenReturn(Observable.never())

        whenever(view.stopClicks()).thenReturn(Observable.never())

        elements = listOf(MASSTRANSIT_ELEMENTS, PEDESTRIAN_ELEMENTS, TAXI_ELEMENTS)

        whenever(routeElementFactory.parseRoute(any(), any(), any(), any(), any())).thenAnswer {
            val route = it.arguments[3] as RouteModel
            elements[args.routeModels.indexOf(route)]
        }
    }

    @Test
    fun `go back when args are not valid`() {
        whenever(argumentsHolder.get()).thenReturn(RouteDetailsArgs.invalid())

        val presenter = createPresenter()

        presenter.onCreate()
        verify(navigator).goBack()
        verifyNoMoreInteractions(routeStopsRepository, routeElementFactory, mapProxy, jamsUpdater, masstransitService, regionSettings, navigator, view)
    }

    @Test
    fun `calls prepare and release on view`() {
        val presenter = createPresenter()

        presenter.onCreate()

        presenter.onAttach(view)
        verify(view).prepare()

        presenter.onDetach()
        verify(view).release()
    }

    @Test
    fun `show proper route when page changed`() {

        val presenter = createPresenter()

        presenter.onCreate()
        presenter.onAttach(view)
        presenter.onViewStart()

        assertStateShown(1, PEDESTRIAN_ELEMENTS)

        pageChanges.onNext(args.routeModels.indexOf(MASSTRANSIT_ROUTE))
        assertStateShown(2, MASSTRANSIT_ELEMENTS)
    }

    private fun assertStateShown(invocationNumber: Int, expectedRouteElements: RouteElements) {
        val stateCaptor = argumentCaptor<ViewState>()
        verify(view, times(invocationNumber)).showState(stateCaptor.capture())
        assertEquals(ViewState(expectedRouteElements), stateCaptor.allValues[invocationNumber - 1])
    }

    @Test
    fun `show state after view start only if it changed`() {

        val presenter = createPresenter()

        presenter.onCreate()
        presenter.onAttach(view)
        presenter.onViewStart()

        assertStateShown(1, PEDESTRIAN_ELEMENTS)

        presenter.onViewStop()
        presenter.onViewStart()
        verify(view, times(1)).showState(any())
    }

    @Test
    fun `show changed state only when view started`() {

        val presenter = createPresenter()

        presenter.onCreate()
        presenter.onAttach(view)
        presenter.onViewStart()

        assertStateShown(1, PEDESTRIAN_ELEMENTS)

        presenter.onViewStop()
        verify(view, times(1)).showState(any())

        pageChanges.onNext(args.routeModels.indexOf(MASSTRANSIT_ROUTE))
        verify(view, times(1)).showState(any())

        presenter.onViewStart()
        assertStateShown(2, MASSTRANSIT_ELEMENTS)
    }

    // jams updates require network, which is redundant while app is in background
    @Test
    fun `update jams only when view is started`() {

        val jamsEvents = PublishSubject.create<String>()
        whenever(jamsUpdater.jamsUpdates(any())).thenReturn(jamsEvents)

        val presenter = createPresenter()

        presenter.onCreate()
        presenter.onAttach(view)

        pageChanges.onNext(args.routeModels.indexOf(MASSTRANSIT_ROUTE)) //set initial route

        verifyZeroInteractions(jamsUpdater)

        presenter.onViewStart()
        assertStateShown(1, MASSTRANSIT_ELEMENTS)

        jamsEvents.onNext("0")
        assertStateShown(2, MASSTRANSIT_ELEMENTS)

        presenter.onViewStop()
        assertFalse(jamsEvents.hasObservers())

        presenter.onViewStart()
        jamsEvents.onNext("1")
        assertStateShown(3, MASSTRANSIT_ELEMENTS)
    }

    @Test
    fun `no new state if tap on map when stop bubble is not showing`() {

        val tapSubject = PublishSubject.create<MapTouchEvent>()
        whenever(mapProxy.mapTouchEvents()).thenReturn(tapSubject)

        val presenter = createPresenter()

        presenter.onCreate()
        presenter.onAttach(view)
        presenter.onViewStart()
        assertStateShown(1, PEDESTRIAN_ELEMENTS)

        tapSubject.onNext(MapTouchEvent(STOP1, MapTouchEvent.Type.TAP))
        assertStateShown(1, PEDESTRIAN_ELEMENTS)
    }


    //TODO: add tests for:
    // * zoom change event
    // * setting transport line\type filters
    // * changing favourite stops
    // * proper viewState.city response
    // * metrics

    private fun favouriteStops() = setOf<String>()

    private fun buildArgs(defaultSelectedRoute: Int = 1) = RouteDetailsArgs(
        listOf(MASSTRANSIT_ROUTE, PEDESTRIAN_ROUTE, TAXI_ROUTE),
        defaultSelectedRoute,
        emptySet(),
        emptySet(),
        emptySet(),
        emptyList(),
        TimeLimitation.departureNow()
    )

    private fun createPresenter() = RouteMapPresenter(
        argumentsHolder,
        routeStopsRepository,
        routeElementFactory,
        mapProxy,
        jamsUpdater,
        regionSettings,
        navigator
    ).also {
        it.pageChangeObservable = pageChanges
    }

    class TPolyline(private val pointsList: List<Point>) : Polyline() {
        override fun getPoints() = pointsList.toMutableList()
    }

    class TSubpolyline : Subpolyline() {
        override fun getBegin(): PolylinePosition = PolylinePosition(0, 0.0)
        override fun getEnd(): PolylinePosition = PolylinePosition(0, 0.0)
    }

    private companion object {
        val DEPARTURE = Point(0.0, 0.0)
        val DESTINATION = Point(0.04, 0.04)
        val STOP1 = Point(0.01, 0.02)
        val STOP2 = Point(0.02, 0.02)
        val STOP3 = Point(0.03, 0.03)

        const val RIDE_DISTANCE = 1000.0

        val TO_STOP1 = TPolyline(listOf(DEPARTURE, STOP1))
        val FROM_STOP3 = TPolyline(listOf(STOP3, DESTINATION))
        val BUS = TPolyline(listOf(STOP1, STOP2, STOP3))
        val STRAIGHT = TPolyline(listOf(DEPARTURE, DESTINATION))

        val SUBPOLYLINE = TSubpolyline()

        val TRANSPORT = Transport("line", "a1", 0, VehicleType.BUS, false, true, "", emptyList())

        val MASSTRANSIT_ROUTE = MasstransitRouteModel(
            "uri:masstransit",
            BoundingBox(),
            RoutePoint(DEPARTURE, ""),
            RoutePoint(DESTINATION, ""),
            listOf(RouteSection(false, null, 0.0, null, null, null, BUS, SUBPOLYLINE, listOf(TRANSPORT), emptyList())),
            transfersCount = 0,
            travelTime = 0.0,
            travelTimeText = "",
            arrivalEstimation = null,
            departureEstimation = null,
            acceptTypes = emptyList(),
            avoidTypes = emptyList(),
            originalRoute = null
        )

        val MASSTRANSIT_ELEMENTS = createRouteElements(
            pedestrianSections = listOf(PedestrianSectionElement(TO_STOP1, 0, 3), PedestrianSectionElement(FROM_STOP3, 2, 3)),
            masstransitSections = listOf(MasstransitSectionElement(BUS, SUBPOLYLINE, 1, 3)),
            stopsOrTransfers = listOf(
                StopElement(CityLocationInfo.UNKNOWN, STOP1, false, false, false, null, RouteStop("stop1", "stop1", STOP1)),
                StopElement(CityLocationInfo.UNKNOWN, STOP2, false, false, false, null, RouteStop("stop2", "stop2", STOP2)),
                StopElement(CityLocationInfo.UNKNOWN, STOP3, false, false, false, null, RouteStop("stop3", "stop3", STOP3))
            )
        )

        val PEDESTRIAN_ROUTE = PedestrianRouteModel(
            "uri:pedestrian",
            BoundingBox(),
            RoutePoint(DEPARTURE, ""),
            RoutePoint(DESTINATION, ""),
            emptyList(),
            0,
            0.0,
            "",
            null,
            null,
            "1km"
        )

        val PEDESTRIAN_ELEMENTS = createRouteElements(pedestrianSections = listOf(PedestrianSectionElement(STRAIGHT, 0, 1)))

        val TAXI_ROUTE = TaxiRouteModel(
            null,
            BoundingBox(),
            RoutePoint(DEPARTURE, ""),
            RoutePoint(DESTINATION, ""),
            emptyList(),
            0,
            0.0,
            "",
            null,
            null,
            Ride(DEPARTURE, null, null, null, TaxiOperator.YA_TAXI),
            RIDE_DISTANCE,
            null
        )

        val TAXI_ELEMENTS = createRouteElements(
            taxiSections = listOf(TaxiSectionElement(STRAIGHT, SUBPOLYLINE, 0, 1))
        )

        val STOP_ELEMENT = MASSTRANSIT_ELEMENTS.stopsOrTransfers.first()

        val VEHICLE = Vehicle(id = "", lineId = "", name = TRANSPORT.name, threadId = null, types = emptyList())

        val HOTSPOT = Hotspot("stop_id").apply {
            name = "name"
            transport = listOf(VEHICLE)
        }

        fun createRouteElements(
            stopsOrTransfers: List<StopElement> = emptyList(),
            addresses: List<AddressElement> = emptyList(),
            masstransitSections: List<MasstransitSectionElement> = emptyList(),
            taxiSections: List<TaxiSectionElement> = emptyList(),
            pedestrianSections: List<PedestrianSectionElement> = emptyList()
        ): RouteElements {
            val routeId = ""
            val jamsId = ""
            return RouteElements(
                routeId,
                stopsOrTransfers,
                addresses,
                MasstransitRouteSections(jamsId, masstransitSections, route = null),
                TaxiRouteSections(jamsId, taxiSections, route = null),
                PedestrianRouteSections(jamsId, pedestrianSections)
            )
        }
    }
}
