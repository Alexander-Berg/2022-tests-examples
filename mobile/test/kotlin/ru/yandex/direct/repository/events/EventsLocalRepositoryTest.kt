package ru.yandex.direct.repository.events

import com.nhaarman.mockito_kotlin.mock
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import ru.yandex.direct.domain.events.LightWeightEvent
import ru.yandex.direct.newui.Constants

class EventsLocalRepositoryTest {
    private lateinit var mRepo: EventsLocalRepository

    @Before
    fun runBeforeEachTest() {
        mRepo = EventsLocalRepository(mock(), mock())
    }

    @Test
    fun filterOutdated_shouldKeepEmptyList_withoutChanges() {
        assertThat(mRepo.filterOutdated(emptyList())).isEmpty()
    }

    @Test
    fun filterOutdated_shouldKeepAllEvents_ifEventsAreFresh() {
        val original = createEvents(200)

        val filtered = mRepo.filterOutdated(original)

        assertThat(filtered).usingRecursiveFieldByFieldElementComparator().isEqualTo(original)
    }

    @Test
    fun filterOutdated_shouldKeepAllEvents_ifEventsAreNotFresh_andCountIsLittle() {
        val original = createEvents(5) { it + 2 * Constants.EVENTS_QUERY_MAX_INTERVAL.millis }

        val filtered = mRepo.filterOutdated(original)

        assertThat(filtered).usingRecursiveFieldByFieldElementComparator().isEqualTo(original)
    }

    @Test
    fun filterOutdated_shouldCutOldEvents_ifEventsAreNotFresh_andCountIsBig() {
        val count = 2 * Constants.EVENTS_ARCHIVE_SIZE
        val original = createEvents(count) { it + 2 * Constants.EVENTS_QUERY_MAX_INTERVAL.millis }

        val filtered = mRepo.filterOutdated(original)

        assertThat(filtered.size).isEqualTo(Constants.EVENTS_ARCHIVE_SIZE)
        assertThat(filtered).usingRecursiveFieldByFieldElementComparator()
                .isEqualTo(original.subList(0, Constants.EVENTS_ARCHIVE_SIZE))
    }

    @Test
    fun filterOutdated_shouldCutOldEvents_withEventsOfDifferentFreshness_ifCountIsBig() {
        val freshCount = 200
        val fresh = createEvents(freshCount)
        val outdatedCount = 2 * Constants.EVENTS_ARCHIVE_SIZE
        val outdated = createEvents(outdatedCount) { it + 2 * Constants.EVENTS_QUERY_MAX_INTERVAL.millis }
        val allEvents = fresh + outdated

        val filtered = mRepo.filterOutdated(allEvents)

        assertThat(filtered.size).isEqualTo(freshCount + Constants.EVENTS_ARCHIVE_SIZE)
        assertThat(filtered).usingRecursiveFieldByFieldElementComparator()
                .isEqualTo(allEvents.subList(0, freshCount + Constants.EVENTS_ARCHIVE_SIZE))
    }

    @Test
    fun filterOutdated_shouldKeepOldEvents_withEventsOfDifferentFreshness_ifCountIsLittle() {
        val freshCount = 200
        val fresh = createEvents(freshCount)
        val outdatedCount = 10
        val outdated = createEvents(outdatedCount) { it + 2 * Constants.EVENTS_QUERY_MAX_INTERVAL.millis }
        val allEvents = fresh + outdated

        val filtered = mRepo.filterOutdated(allEvents)

        assertThat(filtered).usingRecursiveFieldByFieldElementComparator().isEqualTo(allEvents)
    }

    private fun createEvents(count: Int, timestamp: (Int) -> Long = { it.toLong() }): List<LightWeightEvent> {
        val currentTimeMillis = System.currentTimeMillis()
        return (1..count).map { i -> LightWeightEvent().apply { setTimeInMillis(currentTimeMillis - timestamp(i)) } }
    }
}