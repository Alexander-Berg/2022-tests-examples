package ru.yandex.direct.domain.events

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.assertj.core.data.Offset
import org.junit.Test
import ru.yandex.direct.domain.daterange.Duration
import ru.yandex.direct.domain.enums.EventStatus
import ru.yandex.direct.domain.enums.EventType
import ru.yandex.direct.domain.enums.ModerationResult

class LightWeightEventTest {
    private val mEpsilon = 1e-5

    @Test
    fun isAbleToMerge_shouldReturnTrue_onlyForModerationPlaceAndCtrEvents() {
        val softly = SoftAssertions()
        val hasAbilityToMerge = mapOf(
                EventType.BANNER_MODERATED to true,
                EventType.WARN_PLACE to true,
                EventType.UNKNOWN to false,
                EventType.MONEY_IN to false,
                EventType.MONEY_IN_ACCOUNT to false,
                EventType.MONEY_OUT to false,
                EventType.MONEY_OUT_ACCOUNT to false,
                EventType.MONEY_WARNING to false,
                EventType.MONEY_WARNING_ACCOUNT to false,
                EventType.CAMPAIGN_FINISHED to false,
                EventType.PAUSED_BY_DAY_BUDGET to false
        )
        for ((type, isAbleToMerge) in hasAbilityToMerge.entries) {
            softly.assertThat(event().apply { eventType = type }.isAbleToMerge).isEqualTo(isAbleToMerge)
        }
        softly.assertAll()
    }

    @Test
    fun merge_shouldPreserveFreshEventStatus() {
        val staleEvent = event().apply { eventStatus = EventStatus.STALE }
        val freshEvent = event().apply { eventStatus = EventStatus.FRESH }
        val mergedEvent = staleEvent.merge(freshEvent)
        assertThat(mergedEvent.eventStatus).isEqualTo(EventStatus.FRESH)
    }

    @Test
    fun merge_shouldRemoveDuplicateIds() {
        val firstEvent = event().apply {
            bannerID = mutableListOf(1, 2)
            phraseID = mutableListOf(1, 2)
            cacheId = mutableListOf(1, 2)
        }
        val secondEvent = event().apply {
            bannerID = listOf(2, 3)
            phraseID = listOf(2, 3)
            cacheId = listOf(2, 3)
        }
        val mergedEvent = firstEvent.merge(secondEvent)
        val softly = SoftAssertions()
        softly.assertThat(mergedEvent.bannerID).containsExactlyInAnyOrder(1, 2, 3)
        softly.assertThat(mergedEvent.phraseID).containsExactlyInAnyOrder(1, 2, 3)
        softly.assertThat(mergedEvent.cacheId).containsExactlyInAnyOrder(1, 2, 3)
        softly.assertAll()
    }

    @Test
    fun merge_shouldAddMoneyPayed() {
        val firstEvent = event().apply { payed = 1.0 }
        val secondEvent = event().apply { payed = 1.0 }
        val mergedEvent = firstEvent.merge(secondEvent)
        assertThat(mergedEvent.payed).isEqualTo(2.0, Offset.offset(mEpsilon))
    }

    @Test
    fun events_shouldBeDifferent_ifTimeDeltaIsLargerThanTwoHours() {
        val firstEvent = event().apply { setTimeInMillis(0) }
        val secondEvent = event().apply { setTimeInMillis(Duration.hours(3).millis) }
        assertThat(firstEvent.isSimilarTo(secondEvent)).isFalse()
    }

    @Test
    fun events_shouldBeDifferent_ifHaveDifferentCampaignIds() {
        val firstEvent = event().apply { campaignID = 0 }
        val secondEvent = event().apply { campaignID = 1 }
        assertThat(firstEvent.isSimilarTo(secondEvent)).isFalse()
    }

    @Test
    fun events_shouldBeDifferent_ifHaveDifferentAccountIds() {
        val firstEvent = event().apply { accountID = 0 }
        val secondEvent = event().apply { accountID = 1 }
        assertThat(firstEvent.isSimilarTo(secondEvent)).isFalse()
    }

    @Test
    fun events_shouldBeDifferent_ifHaveDifferentModerationResult() {
        val firstEvent = event().apply { moderationResult = ModerationResult.Declined }
        val secondEvent = event().apply { moderationResult = ModerationResult.Accepted }
        assertThat(firstEvent.isSimilarTo(secondEvent)).isFalse()
    }

    @Test
    fun events_shouldBeDifferent_ifHaveDifferentEventType() {
        val firstEvent = event().apply { eventType = EventType.BANNER_MODERATED }
        val secondEvent = event().apply { eventType = EventType.MONEY_IN }
        assertThat(firstEvent.isSimilarTo(secondEvent)).isFalse()
    }

    @Test
    fun events_shouldBeDifferent_ifHaveDifferentStatuses() {
        val firstEvent = event().apply { eventStatus = EventStatus.FRESH }
        val secondEvent = event().apply { eventStatus = EventStatus.STALE }
        assertThat(firstEvent.isSimilarTo(secondEvent)).isFalse()
    }

    @Test
    fun events_shouldBeSimilar_ifHaveDifferentTextDescription() {
        val firstEvent = event().apply { textDescription = "Account is out of funds" }
        val secondEvent = event().apply { textDescription = "Account is empty" }
        assertThat(firstEvent.isSimilarTo(secondEvent)).isTrue()
    }

    @Test
    fun events_shouldBeSimilar_ifHaveDifferentMoneyPayed() {
        val firstEvent = event().apply { payed = 100.0 }
        val secondEvent = event().apply { payed = 200.0 }
        assertThat(firstEvent.isSimilarTo(secondEvent)).isTrue()
    }

    @Test
    fun events_shouldBeSimilar_ifTimeDeltaIsLessThanTwoHours() {
        val firstEvent = event().apply { timestamp = "2018-08-06T13:30:55Z" }
        val secondEvent = event().apply { timestamp = "2018-08-06T13:30:56Z" }
        assertThat(firstEvent.isSimilarTo(secondEvent)).isTrue()
    }

    @Test
    fun events_shouldBeSimilar_otherwise() {
        assertThat(event().isSimilarTo(event())).isTrue()
    }

    private fun event() = LightWeightEvent().apply {
        payed = 0.0
        timestamp = "2018-07-30T10:38:02Z"
    }
}