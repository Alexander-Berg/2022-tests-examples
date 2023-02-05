package ru.yandex.yandexbus.inhouse.route

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mock
import ru.yandex.yandexbus.inhouse.BaseTest
import ru.yandex.yandexbus.inhouse.extensions.mapkit.GeoPlaces.SaintPetersburg.HERMITAGE
import ru.yandex.yandexbus.inhouse.extensions.mapkit.GeoPlaces.SaintPetersburg.KOTLIN
import ru.yandex.yandexbus.inhouse.extensions.mapkit.equalsTo
import ru.yandex.yandexbus.inhouse.model.route.RoutePointResolver
import ru.yandex.yandexbus.inhouse.model.route.TestRoutePoints.SaintPetersburg.HERMITAGE_ROUTE_POINT
import ru.yandex.yandexbus.inhouse.model.route.TestRoutePoints.SaintPetersburg.KOTLIN_ROUTE_POINT
import ru.yandex.yandexbus.inhouse.repos.TimeLimitation
import ru.yandex.yandexbus.inhouse.route.routesetup.RouteBuildUseCase
import ru.yandex.yandexbus.inhouse.route.routesetup.routeVariants
import ru.yandex.yandexbus.inhouse.route.routesetup.testBikeRouteModel
import ru.yandex.yandexbus.inhouse.route.routesetup.testMasstransitRouteModel
import ru.yandex.yandexbus.inhouse.route.routesetup.testPedestrianRouteModel
import ru.yandex.yandexbus.inhouse.route.routesetup.testTaxiRouteModel
import ru.yandex.yandexbus.inhouse.service.location.LocationService
import ru.yandex.yandexbus.inhouse.service.location.UserLocation
import ru.yandex.yandexbus.inhouse.whenever
import rx.Observable
import rx.Single

class RouteResolveUseCaseTest : BaseTest() {
    @Mock
    private lateinit var locationService: LocationService
    @Mock
    private lateinit var routeBuildUseCase: RouteBuildUseCase
    @Mock
    private lateinit var routePointResolver: RoutePointResolver

    private lateinit var routeResolveUseCase: RouteResolveUseCase

    override fun setUp() {
        super.setUp()

        whenever(routePointResolver.resolveAddress(KOTLIN)).thenReturn(Single.just(KOTLIN_ROUTE_POINT))
        whenever(routePointResolver.resolveAddress(HERMITAGE)).thenReturn(Single.just(HERMITAGE_ROUTE_POINT))

        routeResolveUseCase = RouteResolveUseCase(locationService, routeBuildUseCase, routePointResolver)
    }

    @Test
    fun `returns correct routes`() {
        whenever(locationService.locations).thenReturn(Observable.just(UserLocation(position = KOTLIN)))

        val timeLimitation = TimeLimitation.departureNow()

        val routes = routeVariants(testMasstransitRouteModel(), testPedestrianRouteModel(), testTaxiRouteModel(), testBikeRouteModel())
        whenever(routeBuildUseCase.getRouteVariants(KOTLIN_ROUTE_POINT, HERMITAGE_ROUTE_POINT, timeLimitation))
            .thenReturn(Single.just(routes))

        val (location, receivedRoutes) = routeResolveUseCase.routeVariantsFromCurrentLocation(HERMITAGE, timeLimitation)
            .test()
            .assertValueCount(1)
            .onNextEvents.first()

        assertTrue(location.equalsTo(KOTLIN))
        assertEquals(routes, receivedRoutes)
    }

    @Test
    fun `does not return routes when location is unknown`() {
        whenever(locationService.locations).thenReturn(Observable.never())

        routeResolveUseCase.routeVariantsFromCurrentLocation(HERMITAGE, TimeLimitation.departureNow())
            .test()
            .assertValueCount(0)
            .assertNotCompleted()
    }
}
