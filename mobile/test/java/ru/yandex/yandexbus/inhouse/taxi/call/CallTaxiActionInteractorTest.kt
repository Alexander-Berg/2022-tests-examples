package ru.yandex.yandexbus.inhouse.taxi.call

import com.yandex.mapkit.geometry.Point
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verifyZeroInteractions
import ru.yandex.yandexbus.inhouse.BaseTest
import ru.yandex.yandexbus.inhouse.datasync.places.Place
import ru.yandex.yandexbus.inhouse.repos.SavedPlaceRepository
import ru.yandex.yandexbus.inhouse.service.location.LocationService
import ru.yandex.yandexbus.inhouse.service.location.UserLocation
import ru.yandex.yandexbus.inhouse.service.taxi.Cost
import ru.yandex.yandexbus.inhouse.service.taxi.Ride
import ru.yandex.yandexbus.inhouse.service.taxi.TaxiManager
import ru.yandex.yandexbus.inhouse.service.taxi.TaxiOperator
import ru.yandex.yandexbus.inhouse.utils.DistanceProvider
import ru.yandex.yandexbus.inhouse.whenever
import rx.Observable
import rx.Single

class CallTaxiActionInteractorTest : BaseTest() {

    @Mock
    private lateinit var savedPlaceRepository: SavedPlaceRepository
    @Mock
    private lateinit var locationService: LocationService
    @Mock
    private lateinit var taxiManager: TaxiManager
    @Mock
    private lateinit var distanceProvider: DistanceProvider

    private lateinit var callTaxiActionInteractor: CallTaxiActionInteractor

    @Before
    fun before() {
        whenever(taxiManager.rideInfo(TaxiOperator.YA_TAXI, LOCATION, null))
            .thenReturn(Single.just(Ride(LOCATION, null, null, MIN_COST, TaxiOperator.YA_TAXI)))

        whenever(taxiManager.rideInfo(TaxiOperator.YA_TAXI, LOCATION, HOME_LOCATION))
            .thenReturn(Single.just(Ride(LOCATION, HOME_LOCATION, null, MIN_COST, TaxiOperator.YA_TAXI)))

        callTaxiActionInteractor = CallTaxiActionInteractor(savedPlaceRepository, locationService, taxiManager, distanceProvider)
    }

    @Test
    fun `no rides when location is unknown`() {
        whenever(savedPlaceRepository.home).thenReturn(Observable.just(null))
        whenever(locationService.locations).thenReturn(Observable.never<UserLocation>().startWith((null as UserLocation?)))

        callTaxiActionInteractor.rideToHome()
            .test()
            .assertNoValues()
            .assertNoErrors()
            .assertNotCompleted()

        verifyZeroInteractions(taxiManager)
    }

    @Test
    fun `default ride when home address is unknown`() {
        whenever(savedPlaceRepository.home).thenReturn(Observable.just(null))
        whenever(locationService.locations).thenReturn(Observable.just(USER_LOCATION))

        val (actionType, ride) = callTaxiActionInteractor.rideToHome()
            .test()
            .assertValueCount(1)
            .onNextEvents.first()

        assertEquals(TaxiDestinationType.HOME, actionType)
        assertEquals(Ride(LOCATION, null, null, MIN_COST, TaxiOperator.YA_TAXI), ride)
    }

    @Test
    fun `distance to home is more or eq than 500 m and less or eq than 100 km`() {
        whenever(distanceProvider.distanceInMeters(LOCATION, HOME_LOCATION)).thenReturn(500.0)
        whenever(savedPlaceRepository.home).thenReturn(Observable.just(HOME_PLACE))
        whenever(locationService.locations).thenReturn(Observable.just(USER_LOCATION))

        val (actionType, ride) = callTaxiActionInteractor.rideToHome()
            .test()
            .assertValueCount(1)
            .onNextEvents.first()

        assertEquals(TaxiDestinationType.HOME, actionType)
        assertEquals(Ride(LOCATION, HOME_LOCATION, null, MIN_COST, TaxiOperator.YA_TAXI), ride)
    }

    @Test
    fun `distance to home is less than 500 m`() {
        whenever(distanceProvider.distanceInMeters(LOCATION, HOME_LOCATION)).thenReturn(100.0)
        whenever(savedPlaceRepository.home).thenReturn(Observable.just(HOME_PLACE))
        whenever(locationService.locations).thenReturn(Observable.just(USER_LOCATION))

        val (actionType, ride) = callTaxiActionInteractor.rideToHome()
            .test()
            .assertValueCount(1)
            .onNextEvents.first()

        assertEquals(TaxiDestinationType.ANYWHERE, actionType)
        assertEquals(Ride(LOCATION, null, null, MIN_COST, TaxiOperator.YA_TAXI), ride)
    }

    @Test
    fun `distance to home is more than 100 km`() {
        whenever(distanceProvider.distanceInMeters(LOCATION, HOME_LOCATION)).thenReturn(101_000.0)
        whenever(savedPlaceRepository.home).thenReturn(Observable.just(HOME_PLACE))
        whenever(locationService.locations).thenReturn(Observable.just(USER_LOCATION))

        val (actionType, ride) = callTaxiActionInteractor.rideToHome()
            .test()
            .assertValueCount(1)
            .onNextEvents.first()

        assertEquals(TaxiDestinationType.ANYWHERE, actionType)
        assertEquals(Ride(LOCATION, null, null, MIN_COST, TaxiOperator.YA_TAXI), ride)
    }

    companion object {
        private val LOCATION = Point(0.0, 0.0)
        private val USER_LOCATION = UserLocation(null, LOCATION, null)

        private val HOME_LOCATION = Point(1.0, 1.0)
        private val HOME_PLACE = Place("id_home", Place.Type.HOME, HOME_LOCATION, null, null, false)

        private val MIN_COST = Cost(3.0, "BYN", "3 byn")
    }

}
