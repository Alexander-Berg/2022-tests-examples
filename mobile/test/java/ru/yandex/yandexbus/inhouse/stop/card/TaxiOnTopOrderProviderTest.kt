package ru.yandex.yandexbus.inhouse.stop.card

import com.yandex.mapkit.geometry.Point
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mock
import ru.yandex.yandexbus.inhouse.BaseTest
import ru.yandex.yandexbus.inhouse.model.Arrival
import ru.yandex.yandexbus.inhouse.model.EstimatedArrival
import ru.yandex.yandexbus.inhouse.model.PeriodicArrival
import ru.yandex.yandexbus.inhouse.model.ScheduleArrival
import ru.yandex.yandexbus.inhouse.model.Vehicle
import ru.yandex.yandexbus.inhouse.service.taxi.Ride
import ru.yandex.yandexbus.inhouse.service.taxi.TaxiOperator
import ru.yandex.yandexbus.inhouse.utils.ServerTimeProvider
import ru.yandex.yandexbus.inhouse.utils.datetime.DateTime
import ru.yandex.yandexbus.inhouse.whenever
import java.util.concurrent.TimeUnit

class TaxiOnTopOrderProviderTest : BaseTest() {

    @Mock
    private lateinit var serverTimeProvider: ServerTimeProvider

    private lateinit var provider: TaxiOnTopOrderProvider

    override fun setUp() {
        super.setUp()

        whenever(serverTimeProvider.timeMillis()).thenReturn(CURRENT_TIME_UTC_MILLIS)

        provider = TaxiOnTopOrderProvider(serverTimeProvider)
    }

    @Test
    fun `taxi is not on top when it will not arrive soon`() {
        assertFalse(
            provider.taxiShouldBeOnTop(
                estimatedTransports(transportArrivalSoon),
                rides(taxiArrivalNotSoon)
            )
        )

        assertFalse(
            provider.taxiShouldBeOnTop(
                estimatedTransports(transportArrivalNotSoon),
                rides(taxiArrivalNotSoon)
            )
        )
    }

    @Test
    fun `taxi is not on top when it will not arrive sooner than 9 min`() {
        assertFalse(
            provider.taxiShouldBeOnTop(
                estimatedTransports(transportArrivalSoon),
                rides(9)
            )
        )

        assertFalse(
            provider.taxiShouldBeOnTop(
                estimatedTransports(transportArrivalNotSoon),
                rides(9)
            )
        )
    }

    @Test
    fun `taxi is not on top when its arrival is unknown`() {
        assertFalse(
            provider.taxiShouldBeOnTop(
                estimatedTransports(transportArrivalNotSoon),
                rides(null)
            )
        )
    }

    @Test
    fun `taxi is not on top when no rides`() {
        assertFalse(
            provider.taxiShouldBeOnTop(
                estimatedTransports(transportArrivalNotSoon),
                emptyList()
            )
        )
    }

    @Test
    fun `taxi is not on top when estimated transport will arrive soon`() {
        assertFalse(
            provider.taxiShouldBeOnTop(
                estimatedTransports(transportArrivalSoon, transportArrivalNotSoon),
                rides(taxiArrivalSoon)
            )
        )

        assertFalse(
            provider.taxiShouldBeOnTop(
                estimatedTransports(transportArrivalSoon, transportArrivalNotSoon),
                rides(taxiArrivalNotSoon)
            )
        )
    }

    @Test
    fun `taxi is not on top when scheduled transport will arrive soon`() {
        assertFalse(
            provider.taxiShouldBeOnTop(
                scheduledTransports(transportArrivalSoon, transportArrivalNotSoon),
                rides(taxiArrivalSoon)
            )
        )

        assertFalse(
            provider.taxiShouldBeOnTop(
                estimatedTransports(transportArrivalSoon, transportArrivalNotSoon),
                rides(taxiArrivalNotSoon)
            )
        )
    }

    @Test
    fun `taxi is not on top when estimated or scheduled transport will arrive soon`() {
        val transports = estimatedTransports(transportArrivalSoon, transportArrivalNotSoon) +
                scheduledTransports(transportArrivalSoon, transportArrivalNotSoon)

        assertFalse(provider.taxiShouldBeOnTop(transports, rides(taxiArrivalSoon)))
        assertFalse(provider.taxiShouldBeOnTop(transports, rides(taxiArrivalNotSoon)))
    }

    @Test
    fun `taxi is not on top when there is no transport with estimated arrival and there is no ride that arrives soon`() {
        val transports: List<Vehicle> = testTransports(
            scheduledArrival(transportArrivalSoon),
            PeriodicArrival("$transportArrivalSoon min", transportArrivalSoon.toInt()),
            null)

        assertFalse(provider.taxiShouldBeOnTop(transports, rides(taxiArrivalNotSoon)))
    }

    @Test
    fun `taxi is on top when it arrives soon and only periodical transport arrives soon`() {
        val transports: List<Vehicle> = testTransports(
            scheduledArrival(transportArrivalNotSoon),
            PeriodicArrival("$transportArrivalSoon min", transportArrivalSoon.toInt()),
            null)

        assertTrue(provider.taxiShouldBeOnTop(transports, rides(taxiArrivalSoon)))
    }

    @Test
    fun `taxi is on top when there is no transport with estimated or scheduled arrival and there is a ride that arrives soon`() {
        val transports: List<Vehicle> = testTransports(
            PeriodicArrival("$transportArrivalSoon min", transportArrivalSoon.toInt()),
            null)

        assertTrue(provider.taxiShouldBeOnTop(transports, rides(taxiArrivalSoon)))
    }

    companion object {

        const val CURRENT_TIME_UTC_MILLIS = 0L

        const val transportArrivalSoon = 5L
        const val transportArrivalNotSoon = 15L

        const val taxiArrivalSoon = 5L
        const val taxiArrivalNotSoon = 15L

        private fun scheduledArrival(estimationsMinutes: Long) = ScheduleArrival(listOf(DateTime(TimeUnit.MINUTES.toMillis(estimationsMinutes))))

        private fun scheduledTransports(vararg estimationsMinutes: Long): List<Vehicle> {
            return estimationsMinutes.map {
                testTransport(ScheduleArrival(listOf(DateTime(TimeUnit.MINUTES.toMillis(it)))))
            }
        }

        private fun estimatedTransports(vararg estimationsMinutes: Long): List<Vehicle> {
            return estimationsMinutes.map {
                testTransport(EstimatedArrival(listOf(DateTime(TimeUnit.MINUTES.toMillis(it)))))
            }
        }

        private fun testTransports(vararg arrivals: Arrival?): List<Vehicle> {
            return arrivals.map {
                testTransport(it)
            }
        }

        private fun testTransport(arrival: Arrival?) = Vehicle(
            id = "",
            lineId = "",
            name = "",
            threadId = null,
            types = emptyList(),
            arrival = arrival
        )

        private fun rides(vararg estimations: Long?): List<Ride> {
            return estimations.map {
                Ride(Point(0.0, 0.0), null, it, null, TaxiOperator.YA_TAXI)
            }
        }
    }
}