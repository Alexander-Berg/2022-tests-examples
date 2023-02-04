package ru.auto.ara.util

import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.RobolectricTest
import ru.auto.ara.util.ui.RootHostMatchFilter
import ru.auto.test.runner.AllureRobolectricRunner
import kotlin.test.assertEquals

@RunWith(AllureRobolectricRunner::class) class RootHostMatchFilterTest: RobolectricTest() {
    private val matchFilter = RootHostMatchFilter("auto.ru")

    @Test
    fun `match filter finds only root hosts`() {
        val words = mapOf(
            "auto.ru/kek/lol" to true,
            "mag.auto.ru" to true,
            "auto.ru-kek.ru" to false
        )
        words.forEach { (word, expectedValue) ->
            assertEquals(
                expected = expectedValue,
                actual = matchFilter.acceptMatch(word, 0, word.length)
            )
        }
    }
}
