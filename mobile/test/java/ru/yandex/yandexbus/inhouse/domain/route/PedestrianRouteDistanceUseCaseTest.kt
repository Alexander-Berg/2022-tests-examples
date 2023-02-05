package ru.yandex.yandexbus.inhouse.domain.route

import com.yandex.mapkit.LocalizedValue
import com.yandex.mapkit.transport.masstransit.Summary
import com.yandex.mapkit.transport.masstransit.Weight
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import ru.yandex.yandexbus.inhouse.BaseTest
import ru.yandex.yandexbus.inhouse.extensions.mapkit.GeoPlaces
import ru.yandex.yandexbus.inhouse.service.masstransit.NoRoutesFound
import ru.yandex.yandexbus.inhouse.service.masstransit.RxPedestrianRouter
import ru.yandex.yandexbus.inhouse.whenever
import rx.Single

class PedestrianRouteDistanceUseCaseTest: BaseTest() {

    @Mock
    private lateinit var rxPedestrianRouter: RxPedestrianRouter

    private lateinit var pedestrianRouteDistanceUseCase: PedestrianRouteDistanceUseCase

    @Before
    override fun setUp() {
        super.setUp()
        pedestrianRouteDistanceUseCase = PedestrianRouteDistanceUseCase(rxPedestrianRouter)
    }

    @Test
    fun `route distance is evaluated using the shortest route`() {
        val routes = listOf(
            createSummary(routeDistance = 10.0),
            createSummary(routeDistance = 5.0),
            createSummary(routeDistance = 15.0)
        )

        whenever(rxPedestrianRouter.requestRoutesSummary(GeoPlaces.Moscow.YANDEX, GeoPlaces.Moscow.GALLERY))
            .thenReturn(Single.just(routes))

        pedestrianRouteDistanceUseCase.calculateDistanceInMeters(GeoPlaces.Moscow.YANDEX, GeoPlaces.Moscow.GALLERY)
            .test()
            .assertValue(5.0)
            .assertCompleted()
    }

    @Test
    fun `exception is thrown if no routes found`() {
        whenever(rxPedestrianRouter.requestRoutesSummary(GeoPlaces.Moscow.YANDEX, GeoPlaces.Minsk.CENTER))
            .thenReturn(Single.error(NoRoutesFound()))

        pedestrianRouteDistanceUseCase.calculateDistanceInMeters(GeoPlaces.Moscow.YANDEX, GeoPlaces.Minsk.CENTER)
            .test()
            .assertError(NoRoutesFound::class.java)
    }

    companion object {
        private fun createSummary(routeDistance: Double): Summary {
            val weightTime = LocalizedValue(1.0, "")
            val weightDistance = LocalizedValue(routeDistance, "")
            val transferCount = 0
            val weight = Weight(weightTime, weightDistance, transferCount)

            val summary = Mockito.mock(Summary::class.java)
            whenever(summary.weight).thenReturn(weight)

            return summary
        }
    }
}
