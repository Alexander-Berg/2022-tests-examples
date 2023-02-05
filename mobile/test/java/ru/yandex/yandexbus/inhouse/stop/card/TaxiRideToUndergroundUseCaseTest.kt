package ru.yandex.yandexbus.inhouse.stop.card

import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.search.NearbyStop
import com.yandex.mapkit.search.Stop
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import ru.yandex.yandexbus.inhouse.domain.route.PedestrianRouteDistanceUseCase
import ru.yandex.yandexbus.inhouse.extensions.mapkit.GeoPlaces
import ru.yandex.yandexbus.inhouse.repos.UndergroundStationsRepository
import ru.yandex.yandexbus.inhouse.service.masstransit.NoRoutesFound
import ru.yandex.yandexbus.inhouse.stop.card.taxi.RideSuggest
import ru.yandex.yandexbus.inhouse.stop.card.taxi.TaxiRideToUndergroundUseCase
import ru.yandex.yandexbus.inhouse.whenever
import rx.Single

class TaxiRideToUndergroundUseCaseTest : TaxiRideBaseTest() {
    @Mock
    private lateinit var undergroundStationsRepository: UndergroundStationsRepository

    @Mock
    private lateinit var pedestrianRouteDistanceUseCase: PedestrianRouteDistanceUseCase

    @Mock
    private lateinit var undergroundStopNear: NearbyStop

    @Mock
    private lateinit var undergroundStopAway: NearbyStop

    @Mock
    private lateinit var undergroundStopUnreachable: NearbyStop

    private lateinit var taxiRideToUndergroundUseCase: TaxiRideToUndergroundUseCase

    @Before
    override fun setUp() {
        super.setUp()

        taxiRideToUndergroundUseCase = TaxiRideToUndergroundUseCase(
            taxiManager,
            taxiRouteDistanceUseCase,
            undergroundStationsRepository,
            pedestrianRouteDistanceUseCase
        )

        whenever(pedestrianRouteDistanceUseCase.calculateDistanceInMeters(DEPARTURE, UNDERGROUND_NEAR_POINT))
            .thenReturn(Single.just(WALKING_DISTANCE_TO_UNDERGROUND_NEAR_METERS))

        whenever(pedestrianRouteDistanceUseCase.calculateDistanceInMeters(DEPARTURE, UNDERGROUND_AWAY_POINT))
            .thenReturn(Single.just(WALKING_DISTANCE_TO_UNDERGROUND_AWAY_METERS))

        whenever(pedestrianRouteDistanceUseCase.calculateDistanceInMeters(DEPARTURE, UNDERGROUND_UNREACHABLE_POINT))
            .thenReturn(Single.error(NoRoutesFound()))

        whenever(undergroundStopNear.stop).thenReturn(NearbyStop.Stop("id", UNDERGROUND_NEAR_NAME))
        whenever(undergroundStopNear.point).thenReturn(UNDERGROUND_NEAR_POINT)

        whenever(undergroundStopAway.stop).thenReturn(NearbyStop.Stop("id", UNDERGROUND_AWAY_NAME))
        whenever(undergroundStopAway.point).thenReturn(UNDERGROUND_AWAY_POINT)

        whenever(undergroundStopUnreachable.point).thenReturn(UNDERGROUND_UNREACHABLE_POINT)
    }

    private fun setupUndergroundStops(stops: List<NearbyStop>) {
        whenever(undergroundStationsRepository.findNearestStations(GeoPlaces.Moscow.YANDEX))
            .thenReturn(Single.just(stops))
    }

    @Test
    fun `generates ride suggest if all conditions are met`() {
        val acceptableCostToLength = listOf(
            160.0 to 1299.0,
            170.0 to 2399.0,
            200.0 to 3699.0,
            230.0 to 5499.0,
            270.0 to 7699.0,
            330.0 to 10799.0,
            390.0 to 15499.0,
            400.0 to 23799.0
        )

        setupUndergroundStops(listOf(undergroundStopAway))

        for ((rideCost, rideLength) in acceptableCostToLength) {
            val ride = createRide(DEPARTURE, destination = UNDERGROUND_AWAY_POINT, costRubles = rideCost)
            setupTaxiRide(ride, rideLength)

            taxiRideToUndergroundUseCase.makeSuggestion(DEPARTURE, NOON)
                .test()
                .assertValue(RideSuggest.RideToUnderground(ride, UNDERGROUND_AWAY_NAME))
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

        setupUndergroundStops(listOf(undergroundStopAway))

        for ((rideCost, rideLength) in unacceptableCostTOLenght) {
            val ride = createRide(DEPARTURE, destination = UNDERGROUND_AWAY_POINT, costRubles = rideCost)
            setupTaxiRide(ride, rideLength)

            taxiRideToUndergroundUseCase.makeSuggestion(DEPARTURE, NOON)
                .test()
                .assertValue(null)
        }
    }


    @Test
    fun `no suggestions at night time`() {
        setupUndergroundStops(listOf(undergroundStopAway))
        val ride = createRide(DEPARTURE, destination = UNDERGROUND_AWAY_POINT, costRubles = 159.0)
        setupTaxiRide(ride, rideLength = 1299.0)

        taxiRideToUndergroundUseCase.makeSuggestion(DEPARTURE, MIDNIGHT)
            .test()
            .assertValue(null)
    }

    @Test
    fun `no suggestions if there are underground stop which is too close to a departure point`() {
        setupUndergroundStops(listOf(undergroundStopNear, undergroundStopAway))
        val rideNearStop = createRide(DEPARTURE, destination = UNDERGROUND_NEAR_POINT, costRubles = 100.0)
        val rideAwayStop = createRide(DEPARTURE, destination = UNDERGROUND_AWAY_POINT, costRubles = 159.0)
        setupTaxiRide(rideNearStop, rideLength = 700.0)
        setupTaxiRide(rideAwayStop, rideLength = 1299.0)

        taxiRideToUndergroundUseCase.makeSuggestion(DEPARTURE, NOON)
            .test()
            .assertValue(null)
    }

    @Test
    fun `no suggestions if taxi ride is super short`() {
        setupUndergroundStops(listOf(undergroundStopAway))
        val ride = createRide(DEPARTURE, destination = UNDERGROUND_AWAY_POINT, costRubles = 1.0)
        setupTaxiRide(ride, rideLength = 299.0)

        taxiRideToUndergroundUseCase.makeSuggestion(DEPARTURE, NOON)
            .test()
            .assertValue(null)
    }

    @Test
    fun `no suggestions if taxi ride is extremely long`() {
        setupUndergroundStops(listOf(undergroundStopAway))
        val ride = createRide(DEPARTURE, destination = UNDERGROUND_AWAY_POINT, costRubles = 1.0)
        setupTaxiRide(ride, rideLength = 23801.0)

        taxiRideToUndergroundUseCase.makeSuggestion(DEPARTURE, NOON)
            .test()
            .assertValue(null)
    }

    @Test
    fun `no suggestions if underground stop is not reachable by taxi`() {
        setupUndergroundStops(listOf(undergroundStopAway))
        val ride = createRide(DEPARTURE, destination = UNDERGROUND_AWAY_POINT, costRubles = 159.0)
        setupTaxiRide(ride, rideLength = null)

        taxiRideToUndergroundUseCase.makeSuggestion(DEPARTURE, NOON)
            .test()
            .assertValue(null)
    }

    @Test
    fun `no suggestions if underground stop is not reachable by foot`() {
        setupUndergroundStops(listOf(undergroundStopUnreachable))
        val ride = createRide(DEPARTURE, destination = UNDERGROUND_UNREACHABLE_POINT, costRubles = 159.0)
        setupTaxiRide(ride, rideLength = 1299.0)

        taxiRideToUndergroundUseCase.makeSuggestion(DEPARTURE, NOON)
            .test()
            .assertValue(null)
    }

    @Test
    fun `no suggestions if no underground stops found`() {
        setupUndergroundStops(emptyList())

        taxiRideToUndergroundUseCase.makeSuggestion(DEPARTURE, NOON)
            .test()
            .assertValue(null)
    }

    companion object {
        private val DEPARTURE = GeoPlaces.Moscow.YANDEX

        private val UNDERGROUND_NEAR_POINT = Point(55.735380, 37.592283)
        private val UNDERGROUND_AWAY_POINT = Point(55.744220, 37.567866)
        private val UNDERGROUND_UNREACHABLE_POINT = Point(55.758478, 37.658479)

        private const val UNDERGROUND_NEAR_NAME = "Near"
        private const val UNDERGROUND_AWAY_NAME = "Away"

        private const val WALKING_DISTANCE_TO_UNDERGROUND_NEAR_METERS = 500.0
        private const val WALKING_DISTANCE_TO_UNDERGROUND_AWAY_METERS = 1200.0
    }
}
