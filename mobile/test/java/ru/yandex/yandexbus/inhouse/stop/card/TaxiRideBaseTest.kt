package ru.yandex.yandexbus.inhouse.stop.card

import com.yandex.mapkit.geometry.Point
import org.mockito.Mock
import ru.yandex.yandexbus.inhouse.BaseTest
import ru.yandex.yandexbus.inhouse.domain.route.TaxiRouteDistanceUseCase
import ru.yandex.yandexbus.inhouse.service.masstransit.NoRoutesFound
import ru.yandex.yandexbus.inhouse.service.taxi.Cost
import ru.yandex.yandexbus.inhouse.service.taxi.Ride
import ru.yandex.yandexbus.inhouse.service.taxi.TaxiManager
import ru.yandex.yandexbus.inhouse.service.taxi.TaxiOperator
import ru.yandex.yandexbus.inhouse.whenever
import rx.Single
import java.lang.IllegalArgumentException
import java.util.Calendar
import java.util.TimeZone

abstract class TaxiRideBaseTest : BaseTest() {
    @Mock
    protected lateinit var taxiManager: TaxiManager

    @Mock
    protected lateinit var taxiRouteDistanceUseCase: TaxiRouteDistanceUseCase

    protected fun setupTaxiRide(ride: Ride, rideLength: Double?) {
        whenever(taxiManager.rideInfo(TaxiOperator.YA_TAXI, ride.pickup, ride.dropOff))
            .thenReturn(Single.just(ride))

        if (rideLength != null) {
            whenever(taxiRouteDistanceUseCase.calculateDistanceInMeters(ride.pickup, ride.dropOff!!))
                .thenReturn(Single.just(rideLength))
        } else {
            whenever(taxiRouteDistanceUseCase.calculateDistanceInMeters(ride.pickup, ride.dropOff!!))
                .thenReturn(Single.error(NoRoutesFound()))
        }
    }

    companion object {
        @JvmStatic
        protected val utcTimezone: TimeZone = TimeZone.getTimeZone("UTC")

        @JvmStatic
        protected val NOON: Calendar = makeDayOfWeekNoon(Calendar.FRIDAY)

        @JvmStatic
        protected val MIDNIGHT: Calendar = Calendar.getInstance(utcTimezone).apply {
            set(2019, Calendar.FEBRUARY, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

        @JvmStatic
        protected fun makeDayOfWeekNoon(dayOfWeek: Int): Calendar {
            val dayOfMonth = when (dayOfWeek) {
                Calendar.FRIDAY -> 1  // February 1st 2019 was Friday
                Calendar.SATURDAY -> 2
                Calendar.SUNDAY -> 3
                Calendar.MONDAY -> 4
                Calendar.TUESDAY -> 5
                Calendar.WEDNESDAY -> 6
                Calendar.THURSDAY -> 7
                else -> throw IllegalArgumentException()
            }
            return Calendar.getInstance(utcTimezone).apply {
                set(2019, Calendar.FEBRUARY, dayOfMonth, 12, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
        }

        @JvmStatic
        protected fun createRide(departure: Point, destination: Point, costRubles: Double?): Ride {
            val cost = if (costRubles != null) {
                Cost(costRubles, null, "")
            } else {
                null
            }

            return Ride(
                pickup = departure,
                dropOff = destination,
                waitingTimeEstimate = null,
                costEstimate = cost,
                taxiOperator = TaxiOperator.YA_TAXI
            )

        }
    }
}
