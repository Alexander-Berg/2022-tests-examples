package ru.yandex.market.clean.data.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.yandex.market.domain.reviews.model.UsagePeriod

@RunWith(Parameterized::class)
class UsagePeriodMapperTest(private val input: String?, private val expectedResult: UsagePeriod) {

    private val mapper = UsagePeriodMapper()

    @Test
    fun `Map input string as expected`() {
        val mapped = mapper.map(input)

        assertThat(mapped).isEqualTo(expectedResult)
    }

    companion object {

        @Parameterized.Parameters(name = "{index}: \"{0}\" -> {1}")
        @JvmStatic
        fun data(): Iterable<Array<*>> = listOf(
            arrayOf("UNKNOWN", UsagePeriod.UNKNOWN),
            arrayOf("unknown", UsagePeriod.UNKNOWN),
            arrayOf("FEW_MONTHS", UsagePeriod.FEW_MONTHS),
            arrayOf("few_months", UsagePeriod.FEW_MONTHS),
            arrayOf("FEW_WEEKS", UsagePeriod.FEW_WEEKS),
            arrayOf("few_weeks", UsagePeriod.FEW_WEEKS),
            arrayOf("FEW_YEARS", UsagePeriod.FEW_YEARS),
            arrayOf("few_years", UsagePeriod.FEW_YEARS),
            arrayOf(" few_years ", UsagePeriod.FEW_YEARS),
            arrayOf("abc", UsagePeriod.UNKNOWN),
            arrayOf("", UsagePeriod.UNKNOWN),
            arrayOf(null, UsagePeriod.UNKNOWN)
        )
    }
}