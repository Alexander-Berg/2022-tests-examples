package ru.yandex.yandexbus.inhouse.service.taxi

import com.yandex.mapkit.geometry.Point
import org.junit.Test
import org.mockito.Mock
import ru.yandex.yandexbus.inhouse.BaseTest
import ru.yandex.yandexbus.inhouse.any
import ru.yandex.yandexbus.inhouse.service.taxi.uber.UberTaxi
import ru.yandex.yandexbus.inhouse.service.taxi.yandex.YandexTaxi
import ru.yandex.yandexbus.inhouse.whenever
import rx.Single

class TaxiManagerTest : BaseTest() {

    @Mock
    private lateinit var uberTaxi: UberTaxi

    @Mock
    private lateinit var yandexTaxi: YandexTaxi

    private lateinit var taxiManager: TaxiManager

    override fun setUp() {
        super.setUp()

        taxiManager = TaxiManager(yandexTaxi, uberTaxi)
    }

    @Test
    fun `returns empty rides list when no operators available`() {
        taxiManager.rides(emptyList(), Point(0.0, 0.0))
            .test()
            .assertNoErrors()
            .assertValue(emptyList())
    }

    @Test
    fun `shortened list for failed taxi ride requests`() {
        val departure = Point(0.0, 0.0)

        val uberRide = createRide(TaxiOperator.UBER, departure, dropOff = null)

        whenever(yandexTaxi.rideInfo(any(), any()))
            .thenReturn(Single.error<Ride>(Exception()))

        whenever(uberTaxi.rideInfo(any(), any()))
            .thenReturn(Single.just(uberRide))

        taxiManager.rides(listOf(TaxiOperator.UBER, TaxiOperator.YA_TAXI), departure)
            .test()
            .assertNoErrors()
            .assertValues(listOf(uberRide))
    }

    private fun createRide(taxiOperator: TaxiOperator, departure: Point, dropOff: Point?): Ride {
        return Ride(
            departure,
            dropOff,
            waitingTimeEstimate = null,
            costEstimate = null,
            taxiOperator = taxiOperator
        )
    }
}
