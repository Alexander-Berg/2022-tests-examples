package ru.yandex.yandexbus.inhouse.domain.route

import com.yandex.mapkit.geometry.Point
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import ru.yandex.yandexbus.inhouse.BaseTest
import ru.yandex.yandexbus.inhouse.feature.FeatureManager
import ru.yandex.yandexbus.inhouse.model.route.RoutePoint
import ru.yandex.yandexbus.inhouse.repos.TimeLimitation
import ru.yandex.yandexbus.inhouse.service.masstransit.RxDrivingRouter
import ru.yandex.yandexbus.inhouse.service.taxi.TaxiManager
import ru.yandex.yandexbus.inhouse.taxi.experiment.TaxiFeaturesExperiment
import ru.yandex.yandexbus.inhouse.whenever

class TaxiRouteBuildUseCaseTest : BaseTest() {

    @Mock
    private lateinit var featureManager: FeatureManager
    @Mock
    private lateinit var drivingRouter: RxDrivingRouter
    @Mock
    private lateinit var taxiManager: TaxiManager
    @Mock
    private lateinit var taxiFeaturesExperiment: TaxiFeaturesExperiment

    private lateinit var taxiUseCase: TaxiRouteBuildUseCase

    @Before
    override fun setUp() {
        super.setUp()

        whenever(taxiFeaturesExperiment.group).thenReturn(null)

        taxiUseCase = TaxiRouteBuildUseCase(
            drivingRouter,
            featureManager,
            taxiManager,
            RouteModelFactory(),
            taxiFeaturesExperiment
        )
    }

    @Test
    fun `returns empty routes list when there is no taxi services`() {
        whenever(featureManager.taxiTypes).thenReturn(emptyList())

        taxiUseCase.requestRoutes(FROM, TO, TimeLimitation.departureNow())
            .test()
            .assertValue(emptyList())
            .assertCompleted()
    }

    @Test
    fun `returns empty routes list when taxi is disabled by experiment`() {
        whenever(taxiFeaturesExperiment.group).thenReturn(TaxiFeaturesExperiment.Group.NO_FEATURES)

        taxiUseCase.requestRoutes(FROM, TO, TimeLimitation.departureNow())
            .test()
            .assertValue(emptyList())
            .assertCompleted()
    }

    // TODO add more tests

    companion object {
        private val FROM = RoutePoint(Point(55.734010, 37.589450), "улица Тимура Фрунзе, 11с2-5")
        private val TO = RoutePoint(Point(55.751999, 37.617734), "Кремль")
    }
}
