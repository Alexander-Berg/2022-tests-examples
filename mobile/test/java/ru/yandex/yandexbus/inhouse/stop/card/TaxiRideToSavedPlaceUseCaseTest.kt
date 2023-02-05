package ru.yandex.yandexbus.inhouse.stop.card

import com.yandex.mapkit.geometry.Point
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import ru.yandex.yandexbus.inhouse.datasync.places.Place
import ru.yandex.yandexbus.inhouse.extensions.mapkit.GeoPlaces
import ru.yandex.yandexbus.inhouse.repos.SavedPlaceRepository
import ru.yandex.yandexbus.inhouse.service.taxi.Ride
import ru.yandex.yandexbus.inhouse.stop.card.taxi.RideSuggest
import ru.yandex.yandexbus.inhouse.stop.card.taxi.TaxiRideToSavedPlaceUseCase
import ru.yandex.yandexbus.inhouse.whenever
import rx.Observable
import java.util.Calendar

class TaxiRideToSavedPlaceUseCaseTest : TaxiRideBaseTest() {

    @Mock
    private lateinit var savedPlaceRepository: SavedPlaceRepository

    private lateinit var taxiRideToSavedPlaceUseCase: TaxiRideToSavedPlaceUseCase

    @Before
    override fun setUp() {
        super.setUp()

        taxiRideToSavedPlaceUseCase = TaxiRideToSavedPlaceUseCase(
            taxiManager,
            taxiRouteDistanceUseCase,
            savedPlaceRepository
        )
    }

    private fun setPlaces(home: Place?, work: Place?) {
        whenever(savedPlaceRepository.home).thenReturn(Observable.just(home))
        whenever(savedPlaceRepository.work).thenReturn(Observable.just(work))
    }

    private fun setupRideTo(place: Place, rideCost: Double, rideLength: Double?): Ride {
        val ride = createRide(DEPARTURE, destination = place.position, costRubles = rideCost)
        setupTaxiRide(ride, rideLength)
        return ride
    }

    @Test
    fun `suggest ride home if work was not set and all conditions are met`() {
        setPlaces(home = HOME, work = null)
        val ride = setupRideTo(HOME, rideCost = 150.0, rideLength = 2399.0)

        taxiRideToSavedPlaceUseCase.makeSuggestion(DEPARTURE, NOON)
            .test()
            .assertValue(RideSuggest.RideToSavedPlace(ride, HOME))
    }

    @Test
    fun `suggest ride to work if home was not set and all conditions are met`() {
        setPlaces(home = null, work = WORK)
        val ride = setupRideTo(WORK, rideCost = 150.0, rideLength = 2399.0)

        taxiRideToSavedPlaceUseCase.makeSuggestion(DEPARTURE, NOON)
            .test()
            .assertValue(RideSuggest.RideToSavedPlace(ride, WORK))
    }

    @Test
    fun `choose go to work during work time and go to home otherwise`() {
        setPlaces(home = HOME, work = WORK)
        val rideHome = setupRideTo(HOME, rideCost = 150.0, rideLength = 2399.0)
        val rideToWork = setupRideTo(WORK, rideCost = 150.0, rideLength = 2399.0)

        taxiRideToSavedPlaceUseCase.makeSuggestion(DEPARTURE, NOON)
            .test()
            .assertValue(RideSuggest.RideToSavedPlace(rideToWork, WORK))

        taxiRideToSavedPlaceUseCase.makeSuggestion(DEPARTURE, MIDNIGHT)
            .test()
            .assertValue(RideSuggest.RideToSavedPlace(rideHome, HOME))
    }

    @Test
    fun `no rides to work during weekend`() {
        setPlaces(home = null, work = WORK)
        setupRideTo(WORK, rideCost = 150.0, rideLength = 2399.0)

        for (dayOfWeek in listOf(Calendar.SATURDAY, Calendar.SUNDAY)) {
            taxiRideToSavedPlaceUseCase.makeSuggestion(DEPARTURE, makeDayOfWeekNoon(dayOfWeek))
                .test()
                .assertValue(null)
        }
    }

    @Test
    fun `rides to work are allowed on working days`() {
        setPlaces(home = null, work = WORK)
        val ride = setupRideTo(WORK, rideCost = 150.0, rideLength = 2399.0)

        val workdays = listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY)
        for (dayOfWeek in workdays) {
            taxiRideToSavedPlaceUseCase.makeSuggestion(DEPARTURE, makeDayOfWeekNoon(dayOfWeek))
                .test()
                .assertValue(RideSuggest.RideToSavedPlace(ride, WORK))
        }
    }

    @Test
    fun `rides to work are not allowed during the night time`() {
        setPlaces(home = null, work = WORK)
        setupRideTo(WORK, rideCost = 150.0, rideLength = 2399.0)

        taxiRideToSavedPlaceUseCase.makeSuggestion(DEPARTURE, MIDNIGHT)
            .test()
            .assertValue(null)
    }

    @Test
    fun `no suggestion if neither home nor work were set`() {
        setPlaces(home = null, work = null)

        taxiRideToSavedPlaceUseCase.makeSuggestion(DEPARTURE, NOON)
            .test()
            .assertValue(null)
    }

    @Test
    fun `suggest ride home if work is not reachable by taxi and all conditions are met`() {
        setPlaces(home = HOME, work = WORK)
        val rideHome = setupRideTo(HOME, rideCost = 150.0, rideLength = 2399.0)
        setupRideTo(WORK, rideCost = 150.0, rideLength = null)

        taxiRideToSavedPlaceUseCase.makeSuggestion(DEPARTURE, NOON)
            .test()
            .assertValue(RideSuggest.RideToSavedPlace(rideHome, HOME))
    }

    @Test
    fun `suggest ride to work if home is not reachable by taxi and all conditions are met`() {
        setPlaces(home = HOME, work = WORK)
        setupRideTo(HOME, rideCost = 150.0, rideLength = null)
        val rideToWork = setupRideTo(WORK, rideCost = 150.0, rideLength = 2399.0)

        taxiRideToSavedPlaceUseCase.makeSuggestion(DEPARTURE, NOON)
            .test()
            .assertValue(RideSuggest.RideToSavedPlace(rideToWork, WORK))
    }

    @Test
    fun `no suggestions if taxi ride is too short`() {
        setPlaces(home = HOME, work = null)
        setupRideTo(HOME, rideCost = 1.0, rideLength = 1299.0)

        taxiRideToSavedPlaceUseCase.makeSuggestion(DEPARTURE, NOON)
            .test()
            .assertValue(null)
    }

    @Test
    fun `no suggestions if taxi ride is too long`() {
        setPlaces(home = null, work = WORK)
        setupRideTo(WORK, rideCost = 1.0, rideLength = 23801.0)

        taxiRideToSavedPlaceUseCase.makeSuggestion(DEPARTURE, NOON)
            .test()
            .assertValue(null)
    }

    @Test
    fun `suggest ride if cost and ride length are acceptable`() {
        val acceptableCostToLength = listOf(
            170.0 to 2399.0,
            200.0 to 3699.0,
            230.0 to 5499.0,
            270.0 to 7699.0,
            330.0 to 10799.0,
            390.0 to 15499.0,
            400.0 to 23799.0
        )

        setPlaces(home = HOME, work = null)
        for ((rideCost, rideLength) in acceptableCostToLength) {
            val ride = setupRideTo(HOME, rideCost, rideLength)

            taxiRideToSavedPlaceUseCase.makeSuggestion(DEPARTURE, NOON)
                .test()
                .assertValue(RideSuggest.RideToSavedPlace(ride, HOME))
        }
    }

    @Test
    fun `no suggestions if ride is too expensive`() {
        val unacceptableCostTOLenght = listOf(
            171.0 to 2399.0,
            201.0 to 3699.0,
            231.0 to 5499.0,
            271.0 to 7699.0,
            331.0 to 10799.0,
            391.0 to 15499.0,
            401.0 to 23799.0
        )

        setPlaces(home = HOME, work = null)
        for ((rideCost, rideLength) in unacceptableCostTOLenght) {
            setupRideTo(HOME, rideCost, rideLength)

            taxiRideToSavedPlaceUseCase.makeSuggestion(DEPARTURE, NOON)
                .test()
                .assertValue(null)
        }
    }

    companion object {
        private val DEPARTURE = GeoPlaces.Moscow.GALLERY

        private val HOME = Place(
            id = "home",
            type = Place.Type.HOME,
            position = Point(55.743408, 37.562695),
            addressLine = null,
            shortAddressLine = null,
            building = false
        )

        private val WORK = Place(
            id = "work",
            type = Place.Type.WORK,
            position = GeoPlaces.Moscow.YANDEX,
            addressLine = null,
            shortAddressLine = null,
            building = false
        )
    }
}
