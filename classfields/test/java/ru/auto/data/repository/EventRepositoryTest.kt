package ru.auto.data.repository

import io.kotest.core.spec.style.BehaviorSpec
import io.qameta.allure.kotlin.junit4.AllureRunner
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.auto.data.model.frontlog.FrontlogEvent
import ru.auto.data.model.frontlog.FrontlogEventType
import ru.auto.data.model.frontlog.SelfType
import ru.auto.data.network.exception.ApiException
import ru.auto.data.network.scala.ScalaApi
import ru.auto.data.network.scala.response.BaseResponse
import ru.auto.data.storage.frontlog.IFrontlogStorage
import ru.auto.data.util.Optional
import ru.auto.experiments.IExperimentsRepository
import rx.Completable
import rx.Observable
import rx.Single
import rx.schedulers.TestScheduler
import rx.subjects.PublishSubject
import java.net.SocketTimeoutException
import java.util.*
import java.util.concurrent.TimeUnit

/**
 *
 * @author jagger on 28.12.18.
 */
@RunWith(AllureRunner::class) class EventRepositoryTest : BehaviorSpec() {
    private val api: ScalaApi = mock()
    private val storage: IFrontlogStorage = mock()
    private val legacyNetworkInfoRepository: ILegacyNetworkInfoRepository = mock()
    private val stubEvent = FrontlogEvent(
        type = FrontlogEventType.CARD_VIEW,
        id = "id",
        createdAt = Date(),
        selfType = SelfType.TYPE_SINGLE,
        category = null,
        cardId = null,
        index = null,
        containerId = null,
        groupSize = null,
        contextPage = null,
        contextBlock = null,
        phoneNumber = null,
        section = null,
        offersCount = null,
        regionIds = listOf(),
        searchQueryId = null,
        query = null,
        appVersion = null,
        originalRequestId = null,
        currentScreen = null,
        previousScreen = null
    )
    private val bufferSize = EventRepository.BUFFER_SIZE
    private val bufferTimespanSeconds = EventRepository.BUFFER_TIMESPAN_SECONDS
    private val maxEventsPerRequest = EventRepository.MAX_EVENTS_PER_REQUEST
    private val experimentsRepository: IExperimentsRepository = mock()
    private val gmsAdvertisingRepository: IGmsAdvertisingRepository = mock()
    private val hmsAdvertisingRepository: IHmsAdvertisingRepository = mock()

    private lateinit var repo: IEventRepository
    private lateinit var scheduler: TestScheduler

    @Before
    fun setUp() {
        whenever(legacyNetworkInfoRepository.observeNetworkStatusConnected()).thenReturn(Observable.never())
        whenever(api.sendEvents(any(), any())).thenReturn(Single.just(BaseResponse()))
        whenever(storage.write(any())).thenReturn(Completable.complete())
        whenever(gmsAdvertisingRepository.gaid).thenReturn(Single.just(Optional.empty()))
        whenever(hmsAdvertisingRepository.oaid).thenReturn(Single.just(Optional.empty()))
        scheduler = TestScheduler()
        repo = EventRepository(
            api,
            storage,
            legacyNetworkInfoRepository,
            scheduler,
            bufferSize,
            experimentsRepository,
            gmsAdvertisingRepository,
            hmsAdvertisingRepository
        )
    }

    @Test
    fun `no requests should be sent if events count is less then buffer size`() {
        repo.startSyncing().subscribe()
        (0..4).forEach { repo.sendFrontlogEvent(stubEvent) }
        scheduler.advanceTimeBy(1L, TimeUnit.SECONDS)
        verify(api, times(0)).sendEvents(any(), any())
    }

    @Test
    fun `one request should be sent if events count is equal to the buffer size`() {
        repo.startSyncing().subscribe()
        (0 until bufferSize).forEach { repo.sendFrontlogEvent(stubEvent) }
        scheduler.advanceTimeBy(1L, TimeUnit.MILLISECONDS)
        verify(api, times(1)).sendEvents(argThat { events.size == bufferSize }, any())
    }

    @Test
    fun `one request should be sent if buffer timespan elapse`() {
        repo.startSyncing().subscribe()
        val eventsOccur = 5
        (0 until 5).forEach { repo.sendFrontlogEvent(stubEvent) }
        scheduler.advanceTimeBy(bufferTimespanSeconds, TimeUnit.SECONDS)
        verify(api, times(1)).sendEvents(argThat { events.size == eventsOccur }, any())
    }

    @Test
    fun `events should be written to database if non 4xx error occur`() {
        whenever(api.sendEvents(any(), any())).thenReturn(Single.error(ApiException("500", httpCode = 500)))
        repo.startSyncing().subscribe()
        (0 until bufferSize).forEach { repo.sendFrontlogEvent(stubEvent) }
        scheduler.advanceTimeBy(1L, TimeUnit.MILLISECONDS)
        verify(api, times(1)).sendEvents(any(), any())
        verify(storage, times(1)).write(argThat { size == bufferSize })
    }

    @Test
    fun `events should not be written to database if 4xx error occur`() {
        whenever(api.sendEvents(any(), any())).thenReturn(Single.error(ApiException("420", httpCode = 420)))
        repo.startSyncing().subscribe()
        (0 until bufferSize).forEach { repo.sendFrontlogEvent(stubEvent) }
        scheduler.advanceTimeBy(1L, TimeUnit.MILLISECONDS)
        verify(api, times(1)).sendEvents(any(), any())
        verify(storage, times(0)).write(any())
    }

    @Test
    fun `cached events should be sent when network occur`() {
        val events = (0 until maxEventsPerRequest / 2).map { stubEvent }
        whenever(storage.read()).thenReturn(Single.just(events))
        whenever(storage.remove(any())).thenReturn(Completable.complete())
        whenever(legacyNetworkInfoRepository.observeNetworkStatusConnected()).thenReturn(Observable.just(Unit))
        repo.startSyncing().subscribe()
        verify(api, times(1)).sendEvents(any(), any())
        verify(storage, times(1)).remove(any())
    }

    @Test
    fun `large amount of cached events should be separated to different requests`() {
        val events = (0 until maxEventsPerRequest * 2).map { stubEvent }
        whenever(storage.read()).thenReturn(Single.just(events))
        whenever(storage.remove(any())).thenReturn(Completable.complete())
        whenever(legacyNetworkInfoRepository.observeNetworkStatusConnected()).thenReturn(Observable.just(Unit))
        repo.startSyncing().subscribe()
        verify(api, times(2)).sendEvents(any(), any())
        verify(storage, times(2)).remove(any())
    }

    @Test
    fun `if error occured when sending cached events then we should retry on next network event occur`() {
        val events = (0 until maxEventsPerRequest / 2).map { stubEvent }
        val networkEvent = PublishSubject.create<Any>()
        whenever(legacyNetworkInfoRepository.observeNetworkStatusConnected()).thenReturn(networkEvent)
        whenever(storage.read()).thenReturn(Single.just(events))
        whenever(storage.remove(any())).thenReturn(Completable.complete())

        // First call will fail with socket timeout
        whenever(api.sendEvents(any(), any())).thenReturn(Single.error(SocketTimeoutException()))
        repo.startSyncing().subscribe()
        networkEvent.onNext(Unit)
        verify(api, times(1)).sendEvents(any(), any())
        verify(storage, times(0)).remove(any())

        // Retry on network appear
        whenever(api.sendEvents(any(), any())).thenReturn(Single.just(BaseResponse()))
        networkEvent.onNext(Unit)
        verify(api, times(2)).sendEvents(any(), any())
        verify(storage, times(1)).remove(any())
    }
}
