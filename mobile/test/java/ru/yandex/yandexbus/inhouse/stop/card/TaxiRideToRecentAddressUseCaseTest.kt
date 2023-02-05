package ru.yandex.yandexbus.inhouse.stop.card

import com.yandex.mapkit.geometry.Point
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import ru.yandex.maps.toolkit.datasync.binding.datasync.concrete.history.route.RouteHistoryItem
import ru.yandex.yandexbus.inhouse.any
import ru.yandex.yandexbus.inhouse.domain.history.RouteAddressHistoryRepository
import ru.yandex.yandexbus.inhouse.eq
import ru.yandex.yandexbus.inhouse.extensions.mapkit.GeoPlaces
import ru.yandex.yandexbus.inhouse.service.taxi.Ride
import ru.yandex.yandexbus.inhouse.service.taxi.TaxiOperator
import ru.yandex.yandexbus.inhouse.stop.card.taxi.RideSuggest
import ru.yandex.yandexbus.inhouse.stop.card.taxi.TaxiRideToRecentAddressUseCase
import ru.yandex.yandexbus.inhouse.whenever
import rx.Single

class TaxiRideToRecentAddressUseCaseTest : TaxiRideBaseTest() {

    @Mock
    private lateinit var routeAddressHistoryRepo: RouteAddressHistoryRepository

    @Mock
    private lateinit var routeHistoryItem1: RouteHistoryItem

    @Mock
    private lateinit var routeHistoryItem2: RouteHistoryItem

    private lateinit var taxiRideToRecentAddressUseCase: TaxiRideToRecentAddressUseCase

    @Before
    override fun setUp() {
        super.setUp()

        whenever(routeHistoryItem1.latitude).thenReturn(RECENT_ADDRESS_POINT_1.latitude)
        whenever(routeHistoryItem1.longitude).thenReturn(RECENT_ADDRESS_POINT_1.longitude)

        whenever(routeHistoryItem2.latitude).thenReturn(RECENT_ADDRESS_POINT_2.latitude)
        whenever(routeHistoryItem2.longitude).thenReturn(RECENT_ADDRESS_POINT_2.longitude)

        val recentAddresses = listOf(routeHistoryItem1, routeHistoryItem2)
        whenever(routeAddressHistoryRepo.addressHistory()).thenReturn(
            Single.just(recentAddresses).toObservable()
        )

        taxiRideToRecentAddressUseCase = TaxiRideToRecentAddressUseCase(
            taxiManager,
            taxiRouteDistanceUseCase,
            routeAddressHistoryRepo
        )
    }

    private fun setupTaxiLengthRide(ride: Ride, rideLength: Double) {
        // RouteHistoryItem is datasync class, and it does not mapkit Point class
        // Instead, it operates with lat and lon individually as Doubles
        // This makes mocking a bit harder, therefore we use any() mock here
        whenever(taxiManager.rideInfo(eq(TaxiOperator.YA_TAXI), eq(DEPARTURE), any())).thenReturn(Single.just(ride))
        whenever(taxiRouteDistanceUseCase.calculateDistanceInMeters(any(), any()))
            .thenReturn(Single.just(rideLength))
    }

    @Test
    fun `generates ride suggest if all conditions are met`() {
        val acceptableCostToLength = listOf(
            170.0 to 2399.0,
            200.0 to 3699.0,
            230.0 to 5499.0,
            270.0 to 7699.0,
            330.0 to 10799.0,
            390.0 to 15499.0,
            400.0 to 23799.0
        )

        for ((rideCost, rideLength) in acceptableCostToLength) {
            val ride = createRide(DEPARTURE, RECENT_ADDRESS_POINT_1, rideCost)
            setupTaxiLengthRide(ride, rideLength)

            taxiRideToRecentAddressUseCase.makeSuggestion(
                DEPARTURE,
                bannedDestinations = listOf(GeoPlaces.Moscow.GALLERY)
            )
                .test()
                .assertValue(RideSuggest.RideToRecentAddress(ride, routeHistoryItem1))
        }
    }

    @Test
    fun `no suggestions if ride is too expensive`() {
        val unacceptableCostTOLenght = listOf(
            161.0 to 1299.0,
            171.0 to 2399.0,
            201.0 to 3699.0,
            231.0 to 5499.0,
            271.0 to 7699.0,
            331.0 to 10799.0,
            391.0 to 15499.0,
            401.0 to 23799.0
        )

        for ((rideCost, rideLength) in unacceptableCostTOLenght) {
            val ride = createRide(DEPARTURE, RECENT_ADDRESS_POINT_1, rideCost)
            setupTaxiLengthRide(ride, rideLength)

            taxiRideToRecentAddressUseCase.makeSuggestion(
                DEPARTURE,
                bannedDestinations = listOf(GeoPlaces.Moscow.GALLERY)
            )
                .test()
                .assertValue(null)
        }
    }

    @Test
    fun `do not suggest ride for banned destination`() {
        val ride = createRide(DEPARTURE, RECENT_ADDRESS_POINT_2, costRubles = 1.0)
        setupTaxiLengthRide(ride, 2500.0)

        taxiRideToRecentAddressUseCase.makeSuggestion(
            DEPARTURE,
            bannedDestinations = listOf(RECENT_ADDRESS_POINT_1)
        )
            .test()
            .assertValue(RideSuggest.RideToRecentAddress(ride, routeHistoryItem2))
    }

    @Test
    fun `no suggestions if all destinations are banned`() {
        taxiRideToRecentAddressUseCase.makeSuggestion(
            DEPARTURE,
            bannedDestinations = listOf(RECENT_ADDRESS_POINT_1, RECENT_ADDRESS_POINT_2)
        )
            .test()
            .assertValue(null)
    }

    @Test
    fun `no suggestions if landmark is too close`() {
        val ride = createRide(DEPARTURE, RECENT_ADDRESS_POINT_1, costRubles = 1.0)
        setupTaxiLengthRide(ride, rideLength = 1000.0)

        taxiRideToRecentAddressUseCase.makeSuggestion(
            DEPARTURE,
            bannedDestinations = listOf(RECENT_ADDRESS_POINT_1)
        )
            .test()
            .assertValue(null)
    }

    companion object {
        private val DEPARTURE = GeoPlaces.Moscow.YANDEX

        private val RECENT_ADDRESS_POINT_1 = Point(55.755100, 37.573173)
        private val RECENT_ADDRESS_POINT_2 = Point(55.753339, 37.599936)
    }
}