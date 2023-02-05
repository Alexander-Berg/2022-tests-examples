package ru.yandex.yandexbus.inhouse.transport.card

import com.yandex.mapkit.geometry.Polyline
import com.yandex.mapkit.transport.masstransit.Stop
import com.yandex.mapkit.transport.masstransit.Thread
import com.yandex.mapkit.transport.masstransit.ThreadInfo
import com.yandex.mapkit.transport.masstransit.ThreadStop
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import ru.yandex.yandexbus.inhouse.BaseTest
import ru.yandex.yandexbus.inhouse.SchedulerProvider
import ru.yandex.yandexbus.inhouse.exception.MapkitNetworkException
import ru.yandex.yandexbus.inhouse.extensions.mapkit.GeoPlaces
import ru.yandex.yandexbus.inhouse.geometry.toDataClass
import ru.yandex.yandexbus.inhouse.model.Hotspot
import ru.yandex.yandexbus.inhouse.model.Vehicle
import ru.yandex.yandexbus.inhouse.model.VehicleProperty
import ru.yandex.yandexbus.inhouse.model.VehicleType
import ru.yandex.yandexbus.inhouse.repos.FavoriteTransportRepository
import ru.yandex.yandexbus.inhouse.repos.MasstransitLinesRepository
import ru.yandex.yandexbus.inhouse.repos.MasstransitThreadsRepository
import ru.yandex.yandexbus.inhouse.service.location.LocationService
import ru.yandex.yandexbus.inhouse.service.masstransit.MasstransitService
import ru.yandex.yandexbus.inhouse.transport.TransportModel
import ru.yandex.yandexbus.inhouse.transport.card.items.Summary
import ru.yandex.yandexbus.inhouse.utils.network.NetworkInfoProvider
import ru.yandex.yandexbus.inhouse.whenever
import rx.Observable
import rx.Single
import rx.schedulers.TestScheduler
import rx.subjects.BehaviorSubject
import java.lang.Exception
import java.util.*
import java.util.concurrent.TimeUnit

typealias StopItem = ru.yandex.yandexbus.inhouse.transport.card.items.Stop
typealias StopItemType = ru.yandex.yandexbus.inhouse.transport.card.items.Stop.StopType

class TransportCardInteractorTest : BaseTest() {

    private lateinit var transportCardInteractor: TransportCardInteractor

    @Mock
    private lateinit var threadInfo: ThreadInfo

    @Mock
    private lateinit var thread: Thread

    @Mock
    private lateinit var threadPolyline: Polyline

    @Mock
    private lateinit var firstStop: Stop

    @Mock
    private lateinit var lastStop: Stop

    @Mock
    private lateinit var firstThreadStop: ThreadStop

    @Mock
    private lateinit var lastThreadStop: ThreadStop

    @Mock
    private lateinit var masstransitService: MasstransitService

    @Mock
    private lateinit var locationService: LocationService

    @Mock
    private lateinit var favTransportRepository: FavoriteTransportRepository

    @Mock
    private lateinit var networkInfoProvider: NetworkInfoProvider

    private lateinit var masstransitLinesRepository: MasstransitLinesRepository

    private lateinit var masstransitThreadsRepository: MasstransitThreadsRepository

    private lateinit var testScheduler: TestScheduler

    @Before
    override fun setUp() {
        super.setUp()

        whenever(threadInfo.thread).thenReturn(thread)
        whenever(threadInfo.stops).thenReturn(listOf(firstThreadStop, lastThreadStop))
        whenever(thread.essentialStops).thenReturn(listOf(firstStop, lastStop))

        setupThreadStopMock(firstThreadStop, firstStop, stopIndex = 0)
        setupThreadStopMock(lastThreadStop, lastStop, stopIndex = 1)

        whenever(masstransitService.thread(THREAD_ID)).thenReturn(Single.just(threadInfo))
        whenever(masstransitService.vehicle(VEHICLE_ID)).thenReturn(Single.just(vehicle))

        whenever(favTransportRepository.favoriteLineIds).thenReturn(Observable.just(emptySet()))

        whenever(networkInfoProvider.networkChanges).thenReturn(Observable.never())

        masstransitLinesRepository = MasstransitLinesRepository(masstransitService)
        masstransitThreadsRepository = MasstransitThreadsRepository(masstransitService)

        testScheduler = TestScheduler()
        transportCardInteractor = createTransportCardInteractor()
    }

    @Test
    fun `threadBoundingBox emits exactly one thread bounding box on success`() {
        whenever(threadInfo.boundingBox).thenReturn(GeoPlaces.Minsk.BOUNDS)

        transportCardInteractor.threadBoundingBox().test()
            .assertValue(GeoPlaces.Minsk.BOUNDS)
            .assertCompleted()
    }

    @Test
    fun `threadBoundingBox emits error on failure`() {
        whenever(masstransitService.thread(THREAD_ID)).thenReturn(Single.error(Exception()))

        transportCardInteractor.threadBoundingBox().test()
            .assertError(Exception::class.java)
    }

    @Test
    fun `threadStagePolylines emits exactly one thread geometry on success`() {
        whenever(threadInfo.stages).thenReturn(listOf(threadPolyline))

        transportCardInteractor.threadStagePolylines().test()
            .assertValue(listOf(threadPolyline))
            .assertCompleted()
    }

    @Test
    fun `threadStagePolylines emits error on failure`() {
        whenever(masstransitService.thread(THREAD_ID)).thenReturn(Single.error(Exception()))

        transportCardInteractor.threadStagePolylines().test()
            .assertError(Exception::class.java)
    }

    @Test
    fun `first emitted summary is filled with data from transportModel`() {
        transportCardInteractor.summary().first().test()
            .assertValue(Summary(
                name = LINE_NAME,
                type = VehicleType.TRAMWAY,
                properties = EnumSet.noneOf(VehicleProperty::class.java)
            ))
    }

    @Test
    fun `second emitted summary is filled with data fetched from masstransit info`() {
        val secondEvent = transportCardInteractor.summary().test()
                .assertNoErrors()
                .onNextEvents[1]

        val expectedData = Summary(
                name = LINE_NAME,
                type = VehicleType.TRAMWAY,
                properties = EnumSet.of(VehicleProperty.WEELCHAIR_ACESSIBLE),
                initialStop = STOP_NAMES[0],
                finishStop = STOP_NAMES[1],
                isFavorite = false,
                isFollowYMaps = true
        )
        Assert.assertEquals(expectedData, secondEvent)
    }

    @Test
    fun `summary emits error is masstransit info thread request failed`() {
        whenever(masstransitService.thread(THREAD_ID)).thenReturn(Single.error(Exception()))

        transportCardInteractor.summary().skip(1).test()
            .assertError(Exception::class.java)
    }

    @Test
    fun `summary emits error is masstransit info vehicle request failed`() {
        // Vehicle has already been cached in transportCardInteractor
        // Thus whenever we want alter masstransitService.vehicleId() result,
        // we must use a new TransportCardInteractor instance
        whenever(masstransitService.vehicle(VEHICLE_ID)).thenReturn(Single.error(Exception()))

        createTransportCardInteractor().summary().skip(1).test()
            .assertError(Exception::class.java)
    }

    @Test
    fun `first emitted list of stops is empty`() {
        val obs = transportCardInteractor.stops().first().test()
        testScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS)
        obs.assertValue(emptyList())
    }

    @Test
    fun `second emitter list of stops is updated with fetched masstransit info`() {
        val obs = transportCardInteractor.stops().skip(1).first().test()
        testScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS)
        obs.assertValue(listOf(
            StopItem(
                id = STOP_IDS[0],
                type = VehicleType.TRAMWAY,
                stopType = StopItemType.START,
                point = STOP_POSITIONS[0],
                hotspot = Hotspot(STOP_IDS[0]),
                name = STOP_NAMES[0],
                estimated = null,
                isEstimatedRoute = true
            ),
            StopItem(
                id = STOP_IDS[1],
                type = VehicleType.TRAMWAY,
                stopType = StopItemType.FINISH,
                point = STOP_POSITIONS[1],
                hotspot = Hotspot(STOP_IDS[1]),
                name = STOP_NAMES[1],
                estimated = ESTIMATED_TIME,
                estimatedTimeSeconds = ESTIMATED_TIME_UNIX_SECONDS,
                isEstimatedRoute = true,
                isDirection = true
            )
        ))
    }

    @Test
    fun `vehicle goes in direction to stop with the earliest ETA`() {
        // If masstransit line is a loop, it is possible that the vehicle's ETA for the seconds stop
        // is earlier than for the first stop
        val firstThreadStop = Hotspot(STOP_IDS[0]).apply {
            estimated = ESTIMATED_TIME
            estimatedTimeSeconds = ESTIMATED_TIME_UNIX_SECONDS
        }

        val secondThreadStop = Hotspot(STOP_IDS[1]).apply {
            estimated = ESTIMATED_TIME_EARLIER
            estimatedTimeSeconds = ESTIMATED_TIME_EARLIER_UNIX_SECONDS
        }

        val vehicleOnLoopLine = Vehicle(
            id = "",
            lineId = "",
            name = "",
            threadId = null,
            types = listOf(VehicleType.TRAMWAY),
            regularStops = listOf(firstThreadStop, secondThreadStop)
        )

        whenever(masstransitService.vehicle(VEHICLE_ID)).thenReturn(Single.just(vehicleOnLoopLine))

        val isDirectionFlags = createTransportCardInteractor().stops()
            .skip(1)
            .first()
            .map { stops -> stops.map { it.isDirection } }
            .test()

        testScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS)

        isDirectionFlags.assertValue(listOf(false, true))
    }

    @Test
    fun `retries when network is up`() {
        val networkChanges = BehaviorSubject.create<NetworkInfoProvider.Event>()
        whenever(networkInfoProvider.networkChanges).thenReturn(networkChanges)

        whenever(masstransitService.thread(THREAD_ID)).thenReturn(
            Single.fromEmitter { emitter ->
                if (networkChanges.hasValue()
                    && networkChanges.value == NetworkInfoProvider.Event.CONNECTED_OR_CONNECTING) {
                    emitter.onSuccess(threadInfo)
                } else {
                    emitter.onError(MapkitNetworkException())
                }
            }
        )

        val threadBbox = createTransportCardInteractor().threadBoundingBox().test()
        threadBbox.assertNoValues()

        networkChanges.onNext(NetworkInfoProvider.Event.CONNECTED_OR_CONNECTING)
        threadBbox.assertValue(threadInfo.boundingBox)
    }

    private fun createTransportCardInteractor() = TransportCardInteractor(
        transportModel,
        masstransitService,
        masstransitLinesRepository,
        masstransitThreadsRepository,
        locationService,
        favTransportRepository,
        SchedulerProvider(main = testScheduler),
        networkInfoProvider
    )

    private fun setupThreadStopMock(threadStop: ThreadStop, stop: Stop, stopIndex: Int) {
        whenever(stop.id).thenReturn(STOP_IDS[stopIndex])
        whenever(stop.name).thenReturn(STOP_NAMES[stopIndex])
        whenever(threadStop.position).thenReturn(STOP_POSITIONS[stopIndex])
        whenever(threadStop.stop).thenReturn(stop)
    }

    private companion object {
        const val LINE_ID = "test_line_id"
        const val LINE_NAME = "test_line_name"
        const val THREAD_ID = "test_thread_id"
        const val VEHICLE_ID = "test_vehicle_id"

        const val ESTIMATED_TIME = "12:01"
        const val ESTIMATED_TIME_UNIX_SECONDS = 1559390460L  // 1 June 2019, 12:01:00

        const val ESTIMATED_TIME_EARLIER = "11:00"
        const val ESTIMATED_TIME_EARLIER_UNIX_SECONDS = 1559386800L  // 1 June 2019, 11:00:00

        val STOP_IDS = listOf("id_stop_1", "id_stop_2")
        val STOP_NAMES = listOf("First stop", "Last stop")
        val STOP_POSITIONS = listOf(GeoPlaces.Minsk.CENTER, GeoPlaces.Minsk.YANDEX)

        val stopWithEstimation = Hotspot(STOP_IDS[1]).apply {
            estimated = ESTIMATED_TIME
            estimatedTimeSeconds = ESTIMATED_TIME_UNIX_SECONDS
        }

        val transportModel = TransportModel(
            lineId = LINE_ID,
            name = LINE_NAME,
            type = VehicleType.TRAMWAY,
            isFavorite = false,
            isFromSearch = false,
            additionalProperties = EnumSet.noneOf(VehicleProperty::class.java),
            transportId = VEHICLE_ID,
            threadId = THREAD_ID,
            position = GeoPlaces.Minsk.CENTER.toDataClass()
        )

        val vehicle = Vehicle(
            id = "",
            lineId = "",
            name = "",
            threadId = null,
            types = listOf(VehicleType.TRAMWAY),
            regularStops = listOf(stopWithEstimation),
            properties = EnumSet.of(VehicleProperty.WEELCHAIR_ACESSIBLE)
        )
    }
}
