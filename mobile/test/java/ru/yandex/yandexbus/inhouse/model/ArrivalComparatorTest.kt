package ru.yandex.yandexbus.inhouse.model

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import ru.yandex.yandexbus.inhouse.utils.datetime.DateTime

class ArrivalComparatorTest {

    private lateinit var estimate: Arrival
    private lateinit var schedule: Arrival
    private lateinit var periodic: Arrival

    private lateinit var earliestSchedule: Arrival
    private lateinit var earlySchedule: Arrival
    private lateinit var laterEstimate: Arrival
    private lateinit var latestEstimate: Arrival
    private lateinit var shortPeriod: Arrival
    private lateinit var longPeriod: Arrival

    @Before
    fun setUp() {
        val earliest = DateTime.now()
        val early = earliest.plusSeconds(1)
        val later = earliest.plusSeconds(2)
        val latest = earliest.plusSeconds(3)

        estimate = EstimatedArrival(listOf(DateTime.now()))
        schedule = ScheduleArrival(listOf(DateTime.now()))
        periodic = PeriodicArrival("each 5 minutes", 5)

        earliestSchedule = ScheduleArrival(listOf(earliest))
        earlySchedule = ScheduleArrival(listOf(early))
        laterEstimate = EstimatedArrival(listOf(later))
        latestEstimate = EstimatedArrival(listOf(latest))
        shortPeriod = PeriodicArrival("each 1 minute", 1)
        longPeriod = PeriodicArrival("each 5 minutes", 5)
    }

    @Test
    fun `order is estimate, schedule, periodic, null`() {
        assertEquals(
            listOf(estimate, schedule, periodic, null),
            listOf(periodic, null, estimate, schedule).sortedWith(ArrivalComparator)
        )
    }

    @Test
    fun `prefer estimate despite longer await time`() {
        assertEquals(
            listOf(latestEstimate, earliestSchedule),
            listOf(earliestSchedule, latestEstimate).sortedWith(ArrivalComparator)
        )
    }

    @Test
    fun `each arrival group is sorted as well`() {
        assertEquals(
            listOf(laterEstimate, latestEstimate, earliestSchedule, earlySchedule, shortPeriod, longPeriod),
            listOf(earliestSchedule, longPeriod, latestEstimate, earlySchedule, shortPeriod, laterEstimate)
                .sortedWith(ArrivalComparator)
        )
    }
}
