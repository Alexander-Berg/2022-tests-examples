package ru.yandex.yandexbus.inhouse.promocode

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.yandex.yandexbus.inhouse.promocode.repo.PromoCode

class PromoCodeComparatorTest {

    @Test
    fun `promocodes priority order`() {
        val unprioritizedVisited = createPromoCodeState("id1", State.VISITED, null)

        val unprioritizedNew = createPromoCodeState("id2", State.NEW, null)

        val lowPriorityVisited = createPromoCodeState("id3", State.VISITED, 0)
        val lowPriorityNew = createPromoCodeState("id4", State.NEW, 0)

        val highPriorityVisited = createPromoCodeState("id5", State.VISITED, 1)
        val highPriorityNew = createPromoCodeState("id6", State.NEW, 1)

        val states = listOf(
            unprioritizedNew, unprioritizedVisited,
            lowPriorityVisited, lowPriorityNew,
            highPriorityNew, highPriorityVisited
        )

        assertEquals(
            listOf(
                highPriorityNew, lowPriorityNew, unprioritizedNew,
                highPriorityVisited, lowPriorityVisited, unprioritizedVisited
            ),
            states.sortedWith(PromoCodeComparator.descending())
        )
    }

    private fun createPromoCodeState(id: String, state: State, priority: Int?): PromoCodeInfo {
        return PromoCodeInfo(createPromoCode(id, priority), state)
    }

    private fun createPromoCode(id: String, priority: Int?): PromoCode {
        return PromoCodeTestData.emptyPromoCode.copy(
            id = id,
            priority = priority
        )
    }
}
