package ru.yandex.supercheck.presentation.purchases.list.components.productslist.mapper

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.yandex.supercheck.core.comparator.PrioritizedComparator

class PrioritizedComparatorTest {

    companion object {
        private const val LEFT_IS_LESS = -1
        private const val LEFT_IS_GREATER = 1
        private const val EQUALS = 0

        private const val HIGHEST_PRIORITY_ELEMENT = "HIGHEST_PRIORITY"
        private const val SECOND_HIGHEST_PRIORITY_ELEMENT = "SECOND_HIGHEST_PRIORITY"
    }

    private val comparator =
        PrioritizedComparator(
            HIGHEST_PRIORITY_ELEMENT,
            SECOND_HIGHEST_PRIORITY_ELEMENT
        )

    @Test
    fun compare() {
        assertEquals(LEFT_IS_LESS, comparator.compare(HIGHEST_PRIORITY_ELEMENT, SECOND_HIGHEST_PRIORITY_ELEMENT))

        assertEquals(LEFT_IS_GREATER, comparator.compare(SECOND_HIGHEST_PRIORITY_ELEMENT, HIGHEST_PRIORITY_ELEMENT))

        assertEquals(EQUALS, comparator.compare(SECOND_HIGHEST_PRIORITY_ELEMENT, SECOND_HIGHEST_PRIORITY_ELEMENT))

        assertEquals(EQUALS, comparator.compare("a", "a"))

        assertEquals(LEFT_IS_GREATER, comparator.compare("b", "a"))

        assertEquals(LEFT_IS_GREATER, comparator.compare("a", SECOND_HIGHEST_PRIORITY_ELEMENT))

        assertEquals(
            LEFT_IS_LESS,
            PrioritizedComparator("Нет в продаже", "Замены товаров").compare("Нет в продаже", "Молочная гастрономия")
        )
    }
}