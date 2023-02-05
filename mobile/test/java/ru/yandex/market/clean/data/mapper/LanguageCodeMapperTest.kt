package ru.yandex.market.clean.data.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.yandex.market.clean.domain.model.LanguageCode
import ru.yandex.market.utils.LocaleUtils
import java.util.*

@RunWith(Parameterized::class)
class LanguageCodeMapperTest(
    private val input: Locale?,
    private val expectedOutput: LanguageCode
) {

    private val mapper = LanguageCodeMapper()

    @Test
    fun `Properly map known locale to language code`() {
        val mapped = mapper.map(input)

        assertThat(mapped).isEqualTo(expectedOutput)
    }

    companion object {

        @Parameterized.Parameters(name = "{index}: {0} -> {1}")
        @JvmStatic
        fun data(): Iterable<Array<*>> = listOf(
            arrayOf(Locale.ENGLISH, LanguageCode.UNKNOWN),
            arrayOf(LocaleUtils.russian(), LanguageCode.RU),
            arrayOf(null, LanguageCode.UNKNOWN)
        )
    }
}