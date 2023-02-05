package ru.yandex.yandexbus.inhouse.stop.card

import com.yandex.mapkit.geometry.Point
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.Mock
import org.mockito.Mockito.verify
import ru.yandex.yandexbus.inhouse.BaseTest
import ru.yandex.yandexbus.inhouse.ads.AdvertiserFactory
import ru.yandex.yandexbus.inhouse.ads.PromoAdInteractor
import ru.yandex.yandexbus.inhouse.any
import ru.yandex.yandexbus.inhouse.exception.MapkitNetworkException
import ru.yandex.yandexbus.inhouse.feature.Feature
import ru.yandex.yandexbus.inhouse.feature.FeatureManager
import ru.yandex.yandexbus.inhouse.model.Hotspot
import ru.yandex.yandexbus.inhouse.model.Vehicle
import ru.yandex.yandexbus.inhouse.service.taxi.Cost
import ru.yandex.yandexbus.inhouse.service.taxi.Ride
import ru.yandex.yandexbus.inhouse.service.taxi.TaxiOperator
import ru.yandex.yandexbus.inhouse.stop.StopModel
import ru.yandex.yandexbus.inhouse.stop.card.taxi.TaxiOnStopCard
import ru.yandex.yandexbus.inhouse.stop.card.taxi.TaxiOnStopCardUseCase
import ru.yandex.yandexbus.inhouse.utils.network.NetworkInfoProvider
import ru.yandex.yandexbus.inhouse.whenever
import rx.Observable
import rx.Single
import rx.subjects.BehaviorSubject

class StopCardInteractorTest : BaseTest() {
    @Mock
    lateinit var repository: StopTransportRepository
    @Mock
    lateinit var stopCardBookmarkVisibilityStorage: StopCardBookmarkVisibilityStorage
    @Mock
    lateinit var featureManager: FeatureManager
    @Mock
    lateinit var advertiserFactory: AdvertiserFactory
    @Mock
    lateinit var promoAdInteractor: PromoAdInteractor
    @Mock
    lateinit var taxiOnTopOrderProvider: TaxiOnTopOrderProvider
    @Mock
    lateinit var taxiOnStopCardUseCase: TaxiOnStopCardUseCase
    @Mock
    lateinit var networkInfoProvider: NetworkInfoProvider

    private lateinit var interactor: StopCardInteractor

    override fun setUp() {
        super.setUp()

        whenever(repository.stopModel).thenReturn(stopModel)
        whenever(repository.stopTransport()).thenReturn(Observable.empty())
        whenever(promoAdInteractor.loadPromoAd()).thenReturn(Single.just(null))
        whenever(networkInfoProvider.networkChanges).thenReturn(Observable.never())

        interactor = StopCardInteractor(
            repository,
            stopCardBookmarkVisibilityStorage,
            featureManager,
            advertiserFactory,
            promoAdInteractor,
            taxiOnTopOrderProvider,
            taxiOnStopCardUseCase,
            networkInfoProvider
        )
    }

    @Test
    fun `redirects call to repository on bookmark change`() {
        val bookmarkInfo = TransportBookmarkInfo(vehicle, isBookmarked = true, isBookmarkedEditable = true)
        interactor.changeBookmark(bookmarkInfo)
        verify(repository).changeBookmark(bookmarkInfo)
    }

    @Test
    fun `redirects call to bookmark visibility storage when bookmark clicked`() {
        interactor.onBookmarkHintClicked()
        verify(stopCardBookmarkVisibilityStorage).onBookmarkHintClicked()
    }

    @Test
    fun `events start with loading`() {
        val firstEvent = interactor.events.test().onNextEvents.first()
        assertEquals(StopCardEvent.Loading(stopModel), firstEvent)
    }

    companion object {
        private val stopModel = StopModel("test_stop", "test_name", Point(0.0, 0.0))
        private val vehicle = Vehicle(id = "", lineId = "", name = "", threadId = null, types = emptyList())
    }
}

@RunWith(Parameterized::class)
class StopCardInteractorDataTest(private val testData: TestData) : BaseTest() {

    @Mock
    lateinit var repository: StopTransportRepository
    @Mock
    lateinit var stopCardBookmarkVisibilityStorage: StopCardBookmarkVisibilityStorage
    @Mock
    lateinit var featureManager: FeatureManager
    @Mock
    lateinit var advertiserFactory: AdvertiserFactory
    @Mock
    lateinit var promoAdInteractor: PromoAdInteractor
    @Mock
    lateinit var taxiOnTopOrderProvider: TaxiOnTopOrderProvider
    @Mock
    lateinit var taxiOnStopCardUseCase: TaxiOnStopCardUseCase
    @Mock
    lateinit var networkInfoProvider: NetworkInfoProvider

    override fun setUp() {
        super.setUp()

        whenever(repository.stopModel).thenReturn(testData.stopModel)
        whenever(repository.stopTransport()).thenReturn(Observable.just(testData.stopTransport))
        whenever(stopCardBookmarkVisibilityStorage.visibility).thenReturn(Observable.just(testData.bookmarkHintAvailable))

        whenever(featureManager.isFeatureEnabled(Feature.FEEDBACK)).thenReturn(testData.feedbackEnabled)
        whenever(featureManager.isFeatureEnabled(Feature.METRO_N_TRAINS)).thenReturn(testData.openInAppFeatureEnabled)
        whenever(promoAdInteractor.loadPromoAd()).thenReturn(Single.just(testData.promoAd))

        val vehicles = testData.stopTransport.transportBookmarkInfo.map { it.vehicle }
        whenever(taxiOnTopOrderProvider.taxiShouldBeOnTop(vehicles, testData.taxiOnStopCard.rides))
            .thenReturn(testData.taxiOnTop)

        whenever(taxiOnStopCardUseCase.taxi(any())).thenReturn(Observable.just(testData.taxiOnStopCard))
        whenever(networkInfoProvider.networkChanges).thenReturn(Observable.never())
    }

    @Test
    fun test() {
        val secondEvent = createInteractor().events.test()
            .assertNoErrors()
            .onNextEvents[1]

        val expectedData = StopCardEvent.Data(
            stopModel = testData.stopModel,
            stopTransport = testData.stopTransport,
            advertiserFactory = advertiserFactory,
            bookmarkHintAvailable = testData.bookmarkHintAvailable,
            feedbackEnabled = testData.feedbackEnabled,
            openInAppFeatureEnabled = testData.openInAppFeatureEnabled,
            taxiOnStopCard = testData.taxiOnStopCard,
            promoAd = testData.promoAd,
            isFollowYMaps = true
        )
        assertEquals(expectedData, secondEvent)
    }

    @Test
    fun `retries when network is up`() {
        val networkChanges = BehaviorSubject.create<NetworkInfoProvider.Event>()
        whenever(networkInfoProvider.networkChanges).thenReturn(networkChanges)

        whenever(repository.stopTransport()).thenReturn(
            Single.fromEmitter<StopTransport> { emitter ->
                if (networkChanges.hasValue()
                    && networkChanges.value == NetworkInfoProvider.Event.CONNECTED_OR_CONNECTING) {
                    emitter.onSuccess(testData.stopTransport)
                } else {
                    emitter.onError(MapkitNetworkException())
                }
            }.toObservable()
        )

        val stopTransport = createInteractor().stopTransport.test()
        stopTransport.assertNoValues()

        networkChanges.onNext(NetworkInfoProvider.Event.CONNECTED_OR_CONNECTING)
        stopTransport.assertValue(testData.stopTransport)
    }

    private fun createInteractor() = StopCardInteractor(
        repository,
        stopCardBookmarkVisibilityStorage,
        featureManager,
        advertiserFactory,
        promoAdInteractor,
        taxiOnTopOrderProvider,
        taxiOnStopCardUseCase,
        networkInfoProvider
    )

    data class TestData(
        val stopModel: StopModel,
        val stopTransport: StopTransport,
        val bookmarkHintAvailable: Boolean = false,
        val feedbackEnabled: Boolean = false,
        val openInAppFeatureEnabled: Boolean = false,
        val taxiOnStopCard: TaxiOnStopCard = TaxiOnStopCard.NoTaxi,
        val promoAd: PromoAdInteractor.PromoAd?
    ) {
        val taxiOnTop: Boolean = taxiOnStopCard is TaxiOnStopCard.CanBeOnTop
    }

    companion object {

        private val stopLocation = Point(0.0, 0.0)

        private val stopModel = StopModel("test_stop", "test_name", stopLocation)

        private val hotspot = Hotspot(stopModel.stopId).apply {
            point = stopLocation
        }

        private val stopTransport =
            StopTransport(hotspot, stopModel, stopBookmarked = true, transportBookmarkInfo = emptyList(), regionId = 0)

        @JvmStatic
        @Parameterized.Parameters
        fun testData(): Collection<TestData> = listOf(
            TestData(
                stopModel = stopModel,
                stopTransport = stopTransport,
                bookmarkHintAvailable = false,
                feedbackEnabled = false,
                openInAppFeatureEnabled = false,
                taxiOnStopCard = TaxiOnStopCard.NoTaxi,
                promoAd = null
            ),

            TestData(
                stopModel = stopModel,
                stopTransport = stopTransport,
                bookmarkHintAvailable = true,
                feedbackEnabled = true,
                openInAppFeatureEnabled = true,
                taxiOnStopCard = TaxiOnStopCard.Simple(
                    listOf(
                        Ride(
                            pickup = stopLocation,
                            dropOff = null,
                            waitingTimeEstimate = 5,
                            costEstimate = Cost(3.0, "BYN", "from 3 byn"),
                            taxiOperator = TaxiOperator.YA_TAXI
                        )
                    )
                ),
                promoAd = null
            )
        )
    }
}
