package ru.yandex.market.clean.data.mapper

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.HamcrestCondition
import org.hamcrest.Matchers.equalTo
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.yandex.market.test.matchers.ExceptionalMatchers.hasValueThat
import ru.yandex.market.utils.LocaleUtils
import java.util.*

@RunWith(Parameterized::class)
class LocaleMapperTest(private val locale: Locale) {

    val mapper = LocaleMapper()

    @Test
    fun `Serialize locale to string and then deserialize it back`() {
        val languageTag = mapper.toString(locale)
        val fromString = mapper.fromString(languageTag)

        assertThat(fromString).`is`(
            HamcrestCondition(hasValueThat(equalTo(locale)))
        )
    }

    companion object {

        @Parameterized.Parameters(name = "{index}: {0}")
        @JvmStatic
        fun data(): Iterable<*> = listOf(
            Locale.ENGLISH,
            LocaleUtils.russian(),
            Locale.CANADA,
            Locale.CANADA_FRENCH,
            Locale.CHINA,
            Locale.US
        )
    }
}