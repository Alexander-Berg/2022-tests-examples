package ru.yandex.market.utils.hyphenation

import org.junit.Test

class HyphenatorExceptionTest {

    private val hyphenator = Hyphenator.getInstance(HyphenationPattern.RU)

    @Test(expected = IllegalArgumentException::class)
    fun `check on empty args error`() {
        hyphenator.splitBySyllables("")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `check on too mane words error`() {
        hyphenator.splitBySyllables("Закину-ка я несколько слов для определения переносов и ожидаю исключение...")
    }
}