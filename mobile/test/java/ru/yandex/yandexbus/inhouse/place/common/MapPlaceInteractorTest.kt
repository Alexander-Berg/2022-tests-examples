package ru.yandex.yandexbus.inhouse.place.common

import org.junit.Test
import org.mockito.Mock
import ru.yandex.yandexbus.inhouse.BaseTest
import ru.yandex.yandexbus.inhouse.SchedulerProvider
import ru.yandex.yandexbus.inhouse.any
import ru.yandex.yandexbus.inhouse.eq
import ru.yandex.yandexbus.inhouse.geometry.MapkitPoint
import ru.yandex.yandexbus.inhouse.model.route.RoutePoint
import ru.yandex.yandexbus.inhouse.route.RouteResolveUseCase
import ru.yandex.yandexbus.inhouse.route.routesetup.RouteVariants
import ru.yandex.yandexbus.inhouse.service.location.LocationService
import ru.yandex.yandexbus.inhouse.service.location.UserLocation
import ru.yandex.yandexbus.inhouse.utils.DistanceProvider
import ru.yandex.yandexbus.inhouse.utils.LocationPermissionAvailabilityProvider
import ru.yandex.yandexbus.inhouse.whenever
import rx.Observable
import rx.Single
import rx.schedulers.TestScheduler
import rx.subjects.PublishSubject
import java.util.concurrent.TimeUnit

class MapPlaceInteractorTest : BaseTest() {

    @Mock
    private lateinit var locationService: LocationService
    @Mock
    private lateinit var routeResolveUseCase: RouteResolveUseCase
    @Mock
    private lateinit var locationPermissionAvailabilityProvider: LocationPermissionAvailabilityProvider
    @Mock
    private lateinit var distanceProvider: DistanceProvider

    private lateinit var scheduler: TestScheduler

    private lateinit var interactor: MapPlaceInteractor

    override fun setUp() {
        super.setUp()

        scheduler = TestScheduler()

        whenever(distanceProvider.distanceInMeters(eq(LOCATION), eq(PLACE_POSITION))).thenReturn(DISTANCE_TO_PLACE)

        interactor = MapPlaceInteractor(
            locationService,
            routeResolveUseCase,
            locationPermissionAvailabilityProvider,
            SchedulerProvider(scheduler, scheduler, scheduler),
            distanceProvider
        )
    }

    @Test
    fun `returns correct distance to place when distance is known`() {
        val userLocation = UserLocation(position = LOCATION)
        whenever(locationService.location).thenReturn(userLocation)
        whenever(locationService.locations).thenReturn(Observable.just(userLocation))

        interactor.distanceToPlace(PLACE_POSITION)
            .test()
            .assertValue(DISTANCE_TO_PLACE)
    }

    @Test
    fun `returns initial distance after delay if location is unknown and then real distance`() {
        val locations = PublishSubject.create<UserLocation>()
        whenever(locationService.locations).thenReturn(locations)

        val distances = interactor.distanceToPlace(PLACE_POSITION).test()

        distances.assertNoValues()

        scheduler.advanceTimeBy(LOCATION_REQUEST_DELAY_MILLIS, TimeUnit.MILLISECONDS)
        distances.assertValue(null)

        locations.onNext(UserLocation(position = LOCATION))
        distances.assertValues(null, DISTANCE_TO_PLACE)
    }

    @Test
    fun `returns error for routes request when location permission is not granted`() {
        whenever(locationPermissionAvailabilityProvider.isLocationPermissionGranted).thenReturn(false)

        interactor.routesToPlace(PLACE_POSITION)
            .test()
            .assertError(Exception::class.java)
    }

    @Test
    fun `returns correct routes when location permission is granted`() {
        val routes = Triple(
            RoutePoint(LOCATION, "My address"),
            RoutePoint(PLACE_POSITION, "Place address"),
            RouteVariants.EMPTY
        )

        whenever(locationPermissionAvailabilityProvider.isLocationPermissionGranted).thenReturn(true)
        whenever(routeResolveUseCase.routeVariantsFromCurrentLocationWithAddresses(eq(PLACE_POSITION), any()))
            .thenReturn(Single.just(routes))

        interactor.routesToPlace(PLACE_POSITION)
            .test()
            .assertValue(routes)
    }

    private companion object {
        const val LOCATION_REQUEST_DELAY_MILLIS = 300L

        val LOCATION = MapkitPoint(0.0, 0.0)
        val PLACE_POSITION = MapkitPoint(1.0, 1.0)

        const val DISTANCE_TO_PLACE = 5.0
    }
}