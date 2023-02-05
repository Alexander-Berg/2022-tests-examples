package ru.yandex.yandexbus.inhouse.domain.route

import com.yandex.mapkit.transport.masstransit.Route
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import ru.yandex.yandexbus.inhouse.BaseTest
import ru.yandex.yandexbus.inhouse.SchedulerProvider
import ru.yandex.yandexbus.inhouse.extensions.mapkit.GeoPlaces
import ru.yandex.yandexbus.inhouse.model.route.MasstransitRouteModel
import ru.yandex.yandexbus.inhouse.model.route.RoutePoint
import ru.yandex.yandexbus.inhouse.repos.TimeLimitation
import ru.yandex.yandexbus.inhouse.service.masstransit.NoRoutesFound
import ru.yandex.yandexbus.inhouse.service.masstransit.RxMasstransitRouter
import ru.yandex.yandexbus.inhouse.whenever
import rx.Single
import rx.schedulers.Schedulers

class MasstransitRouteBuildUseCaseTest : BaseTest() {

    @Mock
    private lateinit var rxMasstransitRouter: RxMasstransitRouter

    @Mock
    private lateinit var routeModelFactory: RouteModelFactory

    @Mock
    private lateinit var route: Route

    @Mock
    private lateinit var routeModel: MasstransitRouteModel

    private lateinit var masstransitRouteBuildUseCase: MasstransitRouteBuildUseCase

    @Before
    override fun setUp() {
        super.setUp()

        masstransitRouteBuildUseCase = MasstransitRouteBuildUseCase(
            rxMasstransitRouter,
            routeModelFactory,
            SchedulerProvider(
                main = Schedulers.immediate(),
                computation = Schedulers.immediate()
            )
        )
    }

    @Test
    fun `masstransit routes request generates RouteModel one per each Route found`() {
        whenever(routeModelFactory.createRoute(route, YANDEX_ROUTE_POINT, GALLERY_ROUTE_POINT)).thenReturn(routeModel)

        whenever(rxMasstransitRouter.requestRoutes(
            from = GeoPlaces.Moscow.YANDEX,
            to = GeoPlaces.Moscow.GALLERY,
            avoidTypes = emptyList(),
            acceptTypes = emptyList(),
            timeLimitation = DEPARTURE_AT_JAN_1_2019)
        )
            .thenReturn(Single.just(listOf(route)))

        masstransitRouteBuildUseCase.requestRoutes(
            departure = YANDEX_ROUTE_POINT,
            destination = GALLERY_ROUTE_POINT,
            avoidTypes = emptyList(),
            acceptTypes = emptyList(),
            timeLimitation = DEPARTURE_AT_JAN_1_2019
        )
            .test()
            .assertValue(listOf(routeModel))
            .assertCompleted()
    }

    @Test
    fun `masstransit routes request throws exception if no routes found`() {
        whenever(rxMasstransitRouter.requestRoutes(
            from = GeoPlaces.Moscow.YANDEX,
            to = GeoPlaces.Minsk.CENTER,
            avoidTypes = emptyList(),
            acceptTypes = emptyList(),
            timeLimitation = DEPARTURE_AT_JAN_1_2019)
        )
            .thenReturn(Single.error(NoRoutesFound()))

        masstransitRouteBuildUseCase.requestRoutes(
            departure = YANDEX_ROUTE_POINT,
            destination = MINSK_CENTER_ROUTE_POINT,
            avoidTypes = emptyList(),
            acceptTypes = emptyList(),
            timeLimitation = DEPARTURE_AT_JAN_1_2019
        )
            .test()
            .assertError(NoRoutesFound::class.java)
    }

    companion object {
        private val YANDEX_ROUTE_POINT = RoutePoint(GeoPlaces.Moscow.YANDEX, address = "")
        private val GALLERY_ROUTE_POINT = RoutePoint(GeoPlaces.Moscow.GALLERY, address = "")
        private val MINSK_CENTER_ROUTE_POINT = RoutePoint(GeoPlaces.Minsk.CENTER, address = "")

        private val DEPARTURE_AT_JAN_1_2019 = TimeLimitation(
            timeMillis = 1546300800000L,
            timeType = TimeLimitation.TimeType.DEPARTURE,
            isUseCurrentTime = false
        )
    }
}
