package ru.yandex.yandexbus.inhouse.stop.card.taxi

import com.yandex.mapkit.geometry.Point
import org.junit.Test
import org.mockito.Mock
import ru.yandex.yandexbus.inhouse.BaseTest
import ru.yandex.yandexbus.inhouse.eq
import ru.yandex.yandexbus.inhouse.feature.FeatureManager
import ru.yandex.yandexbus.inhouse.model.CityLocationInfo
import ru.yandex.yandexbus.inhouse.service.settings.RegionSettings
import ru.yandex.yandexbus.inhouse.service.taxi.Cost
import ru.yandex.yandexbus.inhouse.service.taxi.Ride
import ru.yandex.yandexbus.inhouse.service.taxi.TaxiManager
import ru.yandex.yandexbus.inhouse.service.taxi.TaxiOperator
import ru.yandex.yandexbus.inhouse.taxi.experiment.TaxiFeaturesExperiment
import ru.yandex.yandexbus.inhouse.whenever
import rx.Single

class TaxiOnStopCardUseCaseTest : BaseTest() {
    @Mock
    private lateinit var experiment: TaxiFeaturesExperiment
    @Mock
    private lateinit var taxiManager: TaxiManager
    @Mock
    private lateinit var featureManager: FeatureManager
    @Mock
    private lateinit var taxiSuggestsUseCase: TaxiSuggestsUseCase
    @Mock
    private lateinit var regionSettings: RegionSettings

    private lateinit var taxiOnStopUseCase: TaxiOnStopCardUseCase

    override fun setUp() {
        super.setUp()

        whenever(featureManager.taxiTypes).thenReturn(TAXI_TYPES)
        whenever(taxiManager.rides(eq(TAXI_TYPES), eq(LOCATION), eq(null))).thenReturn(Single.just(RIDES))
        whenever(taxiSuggestsUseCase.generateRideSuggests(eq(LOCATION))).thenReturn(Single.just(TAXI_SUGGESTS))

        taxiOnStopUseCase = TaxiOnStopCardUseCase(experiment, taxiManager, featureManager, taxiSuggestsUseCase, regionSettings)
    }

    @Test
    fun `returns no taxi when taxi is disabled by experiment`() {
        whenever(experiment.group).thenReturn(TaxiFeaturesExperiment.Group.NO_FEATURES)

        taxiOnStopUseCase.taxi(LOCATION)
            .test()
            .assertValues(TaxiOnStopCard.NoTaxi)
    }

    @Test
    fun `returns simple taxi when user is not in experiment`() {
        whenever(experiment.group).thenReturn(null)

        taxiOnStopUseCase.taxi(LOCATION)
            .test()
            .assertValues(TaxiOnStopCard.Simple(RIDES))
    }

    @Test
    fun `returns CanBeOnTop when user is in TAXI_ON_TOP experiment`() {
        whenever(experiment.group).thenReturn(TaxiFeaturesExperiment.Group.TAXI_ON_TOP)

        taxiOnStopUseCase.taxi(LOCATION)
            .test()
            .assertValues(TaxiOnStopCard.CanBeOnTop(RIDES))
    }

    @Test
    fun `returns NewDesign when user is in TAXI_NEW_DESIGN experiment`() {
        whenever(experiment.group).thenReturn(TaxiFeaturesExperiment.Group.TAXI_NEW_DESIGN)

        taxiOnStopUseCase.taxi(LOCATION)
            .test()
            .assertValues(TaxiOnStopCard.NewDesign(RIDES))
    }

    @Test
    fun `returns suggests when user is in TAXI_CAROUSEL experiment and location is in Moscow`() {
        whenever(experiment.group).thenReturn(TaxiFeaturesExperiment.Group.TAXI_CAROUSEL)
        whenever(regionSettings.findRegion(LOCATION)).thenReturn(Single.just(MOSCOW_CITY))

        taxiOnStopUseCase.taxi(LOCATION)
            .test()
            .assertValues(
                TaxiOnStopCard.Carousel(RIDES, TaxiRideSuggestListEvent.Loading),
                TaxiOnStopCard.Carousel(RIDES, TaxiRideSuggestListEvent.Data(TAXI_SUGGESTS))
            )
    }

    @Test
    fun `returns Simple taxi when user is in TAXI_CAROUSEL experiment and location is not Moscow`() {
        whenever(experiment.group).thenReturn(TaxiFeaturesExperiment.Group.TAXI_CAROUSEL)
        whenever(regionSettings.findRegion(LOCATION)).thenReturn(Single.just(CityLocationInfo.UNKNOWN))

        taxiOnStopUseCase.taxi(LOCATION)
            .test()
            .assertValues(TaxiOnStopCard.Simple(RIDES))
    }

    private companion object {
        val LOCATION = Point(0.0, 0.0)

        val TAXI_TYPES = listOf(TaxiOperator.YA_TAXI)

        val MOSCOW_CITY = CityLocationInfo.UNKNOWN.copy(id = CityLocationInfo.MOSCOW_ID)

        val RIDES = listOf(
            Ride(
                pickup = LOCATION,
                dropOff = null,
                waitingTimeEstimate = 5,
                costEstimate = Cost(3.0, "BYN", "from 3 byn"),
                taxiOperator = TaxiOperator.YA_TAXI
            )
        )

        val TAXI_SUGGESTS = listOf(
            RideSuggest.RideToUnderground(RIDES.first(), "Niamiha")
        )
    }
}