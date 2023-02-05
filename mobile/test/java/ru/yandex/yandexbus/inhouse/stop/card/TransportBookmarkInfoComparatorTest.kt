package ru.yandex.yandexbus.inhouse.stop.card

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import ru.yandex.yandexbus.inhouse.BaseTest
import ru.yandex.yandexbus.inhouse.model.Arrival
import ru.yandex.yandexbus.inhouse.model.EstimatedArrival
import ru.yandex.yandexbus.inhouse.model.PeriodicArrival
import ru.yandex.yandexbus.inhouse.model.ScheduleArrival
import ru.yandex.yandexbus.inhouse.model.Vehicle
import ru.yandex.yandexbus.inhouse.utils.datetime.DateTime

class TransportBookmarkInfoComparatorTest : BaseTest() {

    private lateinit var favoriteEstimated: TransportBookmarkInfo
    private lateinit var favoriteScheduled: TransportBookmarkInfo
    private lateinit var favoritePeriodic: TransportBookmarkInfo
    private lateinit var commonEstimated: TransportBookmarkInfo
    private lateinit var commonScheduled: TransportBookmarkInfo
    private lateinit var commonPeriodic: TransportBookmarkInfo

    @Before
    override fun setUp() {
        val now = DateTime.now()

        favoriteEstimated = transportBookmarkInfo(
            EstimatedArrival(listOf(now.plusSeconds(1))),
            isFavorite = true
        )
        favoriteScheduled = transportBookmarkInfo(
            ScheduleArrival(listOf(now)),
            isFavorite = true
        )
        favoritePeriodic = transportBookmarkInfo(
            PeriodicArrival("every 3 minutes", 3),
            isFavorite = true
        )

        commonEstimated = transportBookmarkInfo(
            EstimatedArrival(listOf(now.minusSeconds(1))),
            isFavorite = false
        )
        commonScheduled = transportBookmarkInfo(
            ScheduleArrival(listOf(now.plusSeconds(1))),
            isFavorite = false
        )
        commonPeriodic = transportBookmarkInfo(
            PeriodicArrival("every 1 minute", 1),
            isFavorite = false
        )
    }


    @Test
    fun `order is bookmarked then other by arrival`() {
        assertEquals(
            listOf(
                favoriteEstimated,
                favoriteScheduled,
                favoritePeriodic,
                commonEstimated,
                commonScheduled,
                commonPeriodic
            ),
            listOf(
                commonPeriodic,
                favoriteEstimated,
                commonEstimated,
                commonScheduled,
                favoritePeriodic,
                favoriteScheduled
            )
                .sortedWith(TransportBookmarkInfoComparator)
        )
    }

    companion object {

        private fun transportBookmarkInfo(arrival: Arrival, isFavorite: Boolean): TransportBookmarkInfo {
            val vehicle = Vehicle(
                id = "",
                lineId = "",
                name = "",
                threadId = null,
                types = emptyList(),
                arrival = arrival
            )
            return TransportBookmarkInfo(vehicle, isFavorite)
        }
    }
}
