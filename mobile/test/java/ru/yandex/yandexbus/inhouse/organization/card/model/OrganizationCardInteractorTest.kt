package ru.yandex.yandexbus.inhouse.organization.card.model

import com.yandex.mapkit.GeoObject
import org.junit.Test
import org.mockito.Mock
import ru.yandex.yandexbus.inhouse.BaseTest
import ru.yandex.yandexbus.inhouse.any
import ru.yandex.yandexbus.inhouse.eq
import ru.yandex.yandexbus.inhouse.geometry.MapkitPoint
import ru.yandex.yandexbus.inhouse.geometry.toDataClass
import ru.yandex.yandexbus.inhouse.organization.card.OrganizationGeoObjectTestFactory
import ru.yandex.yandexbus.inhouse.route.RouteResolveUseCase
import ru.yandex.yandexbus.inhouse.route.routesetup.RouteVariants
import ru.yandex.yandexbus.inhouse.search.BusinessSummary
import ru.yandex.yandexbus.inhouse.search.OrganizationPictures
import ru.yandex.yandexbus.inhouse.search.SearchMetadata
import ru.yandex.yandexbus.inhouse.service.location.LocationService
import ru.yandex.yandexbus.inhouse.service.location.UserLocation
import ru.yandex.yandexbus.inhouse.service.taxi.Cost
import ru.yandex.yandexbus.inhouse.service.taxi.Ride
import ru.yandex.yandexbus.inhouse.service.taxi.TaxiManager
import ru.yandex.yandexbus.inhouse.service.taxi.TaxiOperator
import ru.yandex.yandexbus.inhouse.utils.DistanceProvider
import ru.yandex.yandexbus.inhouse.utils.LocationPermissionAvailabilityProvider
import ru.yandex.yandexbus.inhouse.whenever
import rx.Observable
import rx.Single
import rx.subjects.PublishSubject

class OrganizationCardInteractorTest : BaseTest() {

    private lateinit var geoObject: GeoObject
    @Mock
    private lateinit var organizationInfoResolver: OrganizationInfoResolver
    @Mock
    private lateinit var locationService: LocationService
    @Mock
    private lateinit var taxiManager: TaxiManager
    @Mock
    private lateinit var routeResolveUseCase: RouteResolveUseCase
    @Mock
    private lateinit var distanceProvider: DistanceProvider
    @Mock
    private lateinit var locationPermissionAvailabilityProvider: LocationPermissionAvailabilityProvider

    private lateinit var interactor: OrganizationCardInteractor

    override fun setUp() {
        super.setUp()

        geoObject = OrganizationGeoObjectTestFactory.mockGeoObject(ORGANIZATION_NAME, ORGANIZATION_LOCATION, ORGANIZATION_URI)

        whenever(distanceProvider.distanceInMeters(ORGANIZATION_LOCATION, USER_LOCATION))
            .thenReturn(DISTANCE_TO_ORGANIZATION)
        whenever(distanceProvider.distanceInMeters(USER_LOCATION, ORGANIZATION_LOCATION))
            .thenReturn(DISTANCE_TO_ORGANIZATION)

        whenever(locationService.locations).thenReturn(Observable.just(UserLocation(position = USER_LOCATION)))

        whenever(taxiManager.rideInfo(TaxiOperator.YA_TAXI, USER_LOCATION, ORGANIZATION_LOCATION))
            .thenReturn(Single.just(TAXI_RIDE))

        whenever(organizationInfoResolver.organizationInfo(ORGANIZATION_URI)).thenReturn(Single.just(INFO))

        whenever(locationPermissionAvailabilityProvider.isLocationPermissionGranted).thenReturn(true)

        interactor = OrganizationCardInteractor(
            organizationInfoResolver,
            locationService,
            taxiManager,
            routeResolveUseCase,
            distanceProvider,
            locationPermissionAvailabilityProvider
        )
    }

    @Test
    fun `returns route variants from route resolver`() {
        whenever(routeResolveUseCase.routeVariantsFromCurrentLocation(eq(ORGANIZATION_LOCATION), any()))
            .thenReturn(Single.just(USER_LOCATION to ROUTE_VARIANTS))

        interactor.routes(geoObject)
            .test()
            .assertValue(ROUTE_VARIANTS)
    }

    @Test
    fun `returns error when route resolver fails`() {
        val error = Exception()

        whenever(routeResolveUseCase.routeVariantsFromCurrentLocation(eq(ORGANIZATION_LOCATION), any()))
            .thenReturn(Single.error(error))

        interactor.routes(geoObject)
            .test()
            .assertError(error)
    }

    @Test
    fun `returns correct data`() {
        val data = OrganizationCardData(INFO, DISTANCE_TO_ORGANIZATION, TAXI_RIDE)
        interactor.data(geoObject)
            .test()
            .assertValue(data)
    }

    @Test
    fun `returns error when info resolver fails`() {
        val error = Exception()
        whenever(organizationInfoResolver.organizationInfo(ORGANIZATION_URI)).thenReturn(Single.error(error))
        interactor.data(geoObject)
            .test()
            .assertError(error)
    }

    @Test
    fun `returns no distance and no ride when user location is unknown`() {
        whenever(locationService.locations).thenReturn(Observable.just(null))

        val data = OrganizationCardData(INFO, distanceToOrganizationMeters = null, taxiRide = null)
        interactor.data(geoObject)
            .test()
            .assertValue(data)
    }

    @Test
    fun `returns no ride when taxi request fails`() {
        whenever(taxiManager.rideInfo(eq(TaxiOperator.YA_TAXI), eq(USER_LOCATION), eq(ORGANIZATION_LOCATION)))
            .thenReturn(Single.error(Exception()))

        val data = OrganizationCardData(INFO, DISTANCE_TO_ORGANIZATION, taxiRide = null)
        interactor.data(geoObject)
            .test()
            .assertValue(data)
    }

    @Test
    fun `emits new data until location is known`() {
        val locations = PublishSubject.create<UserLocation>()
        whenever(locationService.locations).thenReturn(locations)

        val data = OrganizationCardData(INFO, null, null)
        val subscriber = interactor.data(geoObject).test()

        locations.onNext(null)
        subscriber
            .assertValue(data)
            .assertNotCompleted()

        // No new emissions since previous location is the same
        locations.onNext(null)
        subscriber
            .assertValue(data)
            .assertNotCompleted()

        // This time observable emits 2 new items and completes
        locations.onNext(UserLocation(position = USER_LOCATION))
        subscriber
            .assertValues(
                data,
                data.copy(distanceToOrganizationMeters = DISTANCE_TO_ORGANIZATION),
                data.copy(distanceToOrganizationMeters = DISTANCE_TO_ORGANIZATION, taxiRide = TAXI_RIDE)
            )
            .assertCompleted()
    }

    @Test
    fun `returns correct data when location permission is not granted`() {
        whenever(locationPermissionAvailabilityProvider.isLocationPermissionGranted).thenReturn(false)
        whenever(locationService.locations).thenReturn(Observable.never())

        interactor.data(geoObject)
            .test()
            .assertValue(OrganizationCardData(INFO, null, null))
    }

    @Test
    fun `routes request fails when location permission is not granted`() {
        whenever(locationPermissionAvailabilityProvider.isLocationPermissionGranted).thenReturn(false)
        whenever(locationService.locations).thenReturn(Observable.never())

        interactor.routes(geoObject)
            .test()
            .assertError(Exception::class.java)
    }

    private companion object {

        const val ORGANIZATION_NAME = "Yandex"

        val ORGANIZATION_LOCATION = MapkitPoint(0.0, 0.0)
        val USER_LOCATION = MapkitPoint(1.0, 1.0)

        const val ORGANIZATION_URI = "ymapsbm1://org/test"

        val SUMMARY = BusinessSummary(
            uri = ORGANIZATION_URI,
            title = ORGANIZATION_NAME,
            location = ORGANIZATION_LOCATION.toDataClass(),
            categories = emptyList(),
            verifiedOwner = true,
            organizationRating = null,
            businessId = null
        )

        val INFO = OrganizationInfo(
            shortAddress = "Niamiha 12",
            address = "Belarus, Minsk, Niamiha 12",
            businessSummary = SUMMARY,
            geoProduct = null,
            workingStatus = null,
            operatingStatus = null,
            organizationPictures = OrganizationPictures(emptyList(), null),
            nearbyUndergroundStops = emptyList(),
            phones = emptyList(),
            links = emptyList(),
            dataProviders = emptyList(),
            searchMetadata = SearchMetadata("logId", "reqId")
        )

        val ROUTE_VARIANTS = RouteVariants.EMPTY

        val TAXI_RIDE = Ride(
            USER_LOCATION,
            ORGANIZATION_LOCATION,
            3,
            Cost(5.0, "BYN", "5 BYN"),
            TaxiOperator.YA_TAXI
        )

        const val DISTANCE_TO_ORGANIZATION = 5.0
    }
}
