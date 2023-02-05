package ru.yandex.market.checkout.data.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.yandex.market.domain.models.region.CountryCode

@RunWith(Parameterized::class)
class CountryCodeMapperTest(
    private val input: Long,
    private val expectedOutput: CountryCode
) {

    private val mapper = CountryCodeMapper()

    @Test
    fun `Properly map geo id to known country code`() {
        val mapped = mapper.mapByGeoId(input)

        assertThat(mapped).isEqualTo(expectedOutput)
    }

    companion object {

        @Parameterized.Parameters(name = "{index}: {0} -> {1}")
        @JvmStatic
        fun data(): Iterable<Array<*>> = listOf(
            arrayOf(225, CountryCode.RU),
            arrayOf(0, CountryCode.UNKNOWN)
        )
    }
}