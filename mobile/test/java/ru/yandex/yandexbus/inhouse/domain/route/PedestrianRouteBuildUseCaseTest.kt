package ru.yandex.yandexbus.inhouse.domain.route

import com.yandex.mapkit.transport.masstransit.Route
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import ru.yandex.yandexbus.inhouse.BaseTest
import ru.yandex.yandexbus.inhouse.SchedulerProvider
import ru.yandex.yandexbus.inhouse.extensions.mapkit.GeoPlaces
import ru.yandex.yandexbus.inhouse.model.route.PedestrianRouteModel
import ru.yandex.yandexbus.inhouse.model.route.RoutePoint
import ru.yandex.yandexbus.inhouse.service.masstransit.NoRoutesFound
import ru.yandex.yandexbus.inhouse.service.masstransit.RxPedestrianRouter
import ru.yandex.yandexbus.inhouse.whenever
import rx.Single
import rx.schedulers.Schedulers

class PedestrianRouteBuildUseCaseTest : BaseTest() {

    @Mock
    private lateinit var rxPedestrianRouter: RxPedestrianRouter

    @Mock
    private lateinit var routeModelFactory: RouteModelFactory

    @Mock
    private lateinit var route: Route

    @Mock
    private lateinit var routeModel: PedestrianRouteModel

    private lateinit var pedestrianRouteBuildUseCase: PedestrianRouteBuildUseCase

    @Before
    override fun setUp() {
        super.setUp()

        pedestrianRouteBuildUseCase = PedestrianRouteBuildUseCase(
            rxPedestrianRouter,
            routeModelFactory,
            SchedulerProvider(
                main = Schedulers.immediate(),
                computation = Schedulers.immediate()
            )
        )
    }

    @Test
    fun `pedestrian routes request generates RouteModel one per each Route found`() {
        whenever(routeModelFactory.createRoute(route, YANDEX_ROUTE_POINT, GALLERY_ROUTE_POINT)).thenReturn(routeModel)

        whenever(rxPedestrianRouter.requestRoutes(GeoPlaces.Moscow.YANDEX, GeoPlaces.Moscow.GALLERY))
            .thenReturn(Single.just(listOf(route)))

        pedestrianRouteBuildUseCase.requestRoutes(YANDEX_ROUTE_POINT, GALLERY_ROUTE_POINT)
            .test()
            .assertValue(listOf(routeModel))
            .assertCompleted()
    }

    @Test
    fun `pedestrian routes request throws exception if no routes found`() {
        whenever(rxPedestrianRouter.requestRoutes(GeoPlaces.Moscow.YANDEX, GeoPlaces.Minsk.CENTER))
            .thenReturn(Single.error(NoRoutesFound()))

        pedestrianRouteBuildUseCase.requestRoutes(YANDEX_ROUTE_POINT, MINSK_CENTER_ROUTE_POINT)
            .test()
            .assertError(NoRoutesFound::class.java)
    }

    companion object {
        private val YANDEX_ROUTE_POINT = RoutePoint(GeoPlaces.Moscow.YANDEX, address = "")
        private val GALLERY_ROUTE_POINT = RoutePoint(GeoPlaces.Moscow.GALLERY, address = "")
        private val MINSK_CENTER_ROUTE_POINT = RoutePoint(GeoPlaces.Minsk.CENTER, address = "")
    }
}