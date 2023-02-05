package ru.yandex.yandexbus.inhouse.stop.card

import org.junit.Before
import org.junit.Test
import ru.yandex.yandexbus.inhouse.extensions.mapkit.GeoPlaces
import ru.yandex.yandexbus.inhouse.service.masstransit.NoRoutesFound
import ru.yandex.yandexbus.inhouse.stop.card.taxi.RideSuggest
import ru.yandex.yandexbus.inhouse.stop.card.taxi.TaxiRideToLandmarkUseCase

class TaxiRideToLandmarkUseCaseTest : TaxiRideBaseTest() {

    private lateinit var taxiRideToLandmarkUseCase: TaxiRideToLandmarkUseCase

    @Before
    override fun setUp() {
        super.setUp()

        taxiRideToLandmarkUseCase = TaxiRideToLandmarkUseCase(taxiManager, taxiRouteDistanceUseCase)
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
            val ride = createRide(DEPARTURE, LANDMARK.location, rideCost)
            setupTaxiRide(ride, rideLength)

            taxiRideToLandmarkUseCase.makeSuggestion(DEPARTURE, LANDMARK)
                .test()
                .assertValue(RideSuggest.RideToLandmark(ride, LANDMARK))
        }
    }

    @Test
    fun `no suggestions if landmark is too close`() {
        val ride = createRide(DEPARTURE, LANDMARK.location, costRubles = 1.0)
        setupTaxiRide(ride, rideLength = 1000.0)

        taxiRideToLandmarkUseCase.makeSuggestion(DEPARTURE, LANDMARK)
            .test()
            .assertValue(null)
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
            val ride = createRide(DEPARTURE, LANDMARK.location, rideCost)
            setupTaxiRide(ride, rideLength)

            taxiRideToLandmarkUseCase.makeSuggestion(DEPARTURE, LANDMARK)
                .test()
                .assertValue(null)
        }
    }

    @Test
    fun `error returned if landmark is not reachable by taxi`() {
        val ride = createRide(DEPARTURE, LANDMARK.location, costRubles = 1.0)
        setupTaxiRide(ride, rideLength = null)

        taxiRideToLandmarkUseCase.makeSuggestion(DEPARTURE, LANDMARK)
            .test()
            .assertError(NoRoutesFound::class.java)
    }

    companion object {
        private val DEPARTURE = GeoPlaces.Moscow.YANDEX

        private val LANDMARK = RideSuggest.RideToLandmark.Landmark(
            location = GeoPlaces.Moscow.GALLERY,
            nameResId = 0
        )
    }
}