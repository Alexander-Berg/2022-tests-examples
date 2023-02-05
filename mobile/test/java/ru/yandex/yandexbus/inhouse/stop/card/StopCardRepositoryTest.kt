package ru.yandex.yandexbus.inhouse.stop.card

import com.yandex.mapkit.geometry.Point
import org.junit.Test
import org.mockito.Mock
import ru.yandex.maps.toolkit.datasync.binding.datasync.concrete.stop.Stop
import ru.yandex.yandexbus.inhouse.BaseTest
import ru.yandex.yandexbus.inhouse.SchedulerProvider
import ru.yandex.yandexbus.inhouse.model.CityLocationInfo
import ru.yandex.yandexbus.inhouse.model.Hotspot
import ru.yandex.yandexbus.inhouse.model.Vehicle
import ru.yandex.yandexbus.inhouse.service.masstransit.MasstransitService
import ru.yandex.yandexbus.inhouse.service.settings.RegionSettings
import ru.yandex.yandexbus.inhouse.stop.StopModel
import ru.yandex.yandexbus.inhouse.whenever
import rx.Observable
import rx.Single
import rx.observers.AssertableSubscriber
import rx.schedulers.TestScheduler
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class StopTransportRepositoryTest : BaseTest() {

    @Mock
    private lateinit var transportDataSync: StopTransportDataSync
    @Mock
    private lateinit var masstransitService: MasstransitService
    @Mock
    private lateinit var regionSettings: RegionSettings

    private lateinit var repository: StopTransportRepository

    private val testScheduler = TestScheduler()

    override fun setUp() {
        super.setUp()

        whenever(regionSettings.findRegion(stopLocation)).thenReturn(Single.just(stopCity))
        whenever(transportDataSync.syncedStopTransport()).thenReturn(Observable.just(syncedStopTransport))

        repository = StopTransportRepository(
            stopModel,
            transportDataSync,
            masstransitService,
            regionSettings,
            SchedulerProvider(testScheduler, testScheduler, testScheduler)
        )
    }

    @Test
    fun `propagates errors`() {
        whenever(masstransitService.hotspot(stopModel.stopId)).thenReturn(Single.error(Exception()))

        val subscriber = repository.stopTransport().test()

        testScheduler.advanceTimeSeconds(0)
        subscriber.assertError(Exception::class.java)
    }

    @Test
    fun `emits correct data`() {
        whenever(masstransitService.hotspot(stopModel.stopId)).thenReturn(Single.just(hotspot))

        val subscriber = repository.stopTransport().test()

        testScheduler.advanceTimeSeconds(0)
        subscriber.noErrorsNotCompletedAndHasValues(StopTransport)
    }

    @Test
    fun `updates stop info every 60 seconds`() {
        whenever(masstransitService.hotspot(stopModel.stopId)).thenReturn(Single.just(hotspot))

        val subscriber = repository.stopTransport().test()

        testScheduler.advanceTimeSeconds(0)
        subscriber.noErrorsNotCompletedAndHasValues(StopTransport)

        testScheduler.advanceTimeSeconds(60)
        subscriber.noErrorsNotCompletedAndHasValues(StopTransport, StopTransport)
    }

    // TODO(BUS-4914): add more tests for StopTransportRepository
    // check that stopTransport() returns correctly formatted data, e.g. transportBookmarkInfo is sorted and so on

    companion object {
        private val stopLocation = Point(0.0, 0.0)
        private val stopCity = CityLocationInfo.UNKNOWN

        private val stopModel = StopModel("stop_test", "test_stop", stopLocation)

        private val syncedStopTransport = SyncedStopTransport(listOf(stopModel.toStop()))

        private val hotspot = stopModel.toHotspot()

        private val StopTransport = StopTransport(
            hotspot,
            stopModel,
            syncedStopTransport.isStopSynced(stopModel.stopId),
            emptyList(),
            stopCity.id
        )
    }
}

private fun StopModel.toStop(): Stop {
    return Stop.builder()
        .setStopId(stopId)
        .setTags(emptyList())
        .setChildren(emptyList())
        .setLatitude(point?.latitude ?: 0.0)
        .setLongitude(point?.longitude ?: 0.0)
        .build()
}

private fun StopModel.toHotspot(vehicles: List<Vehicle> = emptyList()): Hotspot {
    return Hotspot(stopId).apply {
        this.point = this@toHotspot.point
        this.transport = vehicles
    }
}

private fun <T> AssertableSubscriber<T>.noErrorsNotCompletedAndHasValues(vararg values: T): AssertableSubscriber<T> {
    assertNoErrors()
    assertNotCompleted()
    assertValues(*values)
    return this
}

private fun TestScheduler.advanceTimeSeconds(time: Long) = advanceTimeBy(time, TimeUnit.SECONDS)