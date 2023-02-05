package ru.yandex.market.clean.presentation.formatter

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.HamcrestCondition
import org.hamcrest.Matchers.equalTo
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.common.android.ResourcesManager

class SizeFormatterTest {

    private val resourcesDataStore = mock<ResourcesManager>()
    private val formatter = SizeFormatter(resourcesDataStore)

    @Test
    fun `check correct formatting of letter sizes`() {
        internationalPairs.forEach { (input, output) ->
            assertThat(
                formatter.formatInternational(input)
            ).`is`(HamcrestCondition(equalTo(output)))
        }
    }

    @Test
    fun `check correct formatting of numeric sizes`() {
        whenever(resourcesDataStore.getFormattedString(any(), eq(1))).thenReturn("+1")
        whenever(resourcesDataStore.getFormattedString(any(), eq(2))).thenReturn("+2")
        whenever(resourcesDataStore.getFormattedString(any(), eq(3))).thenReturn("+3")

        numericPairs.forEach { (input, output) ->
            assertThat(
                formatter.formatNumeric(input)
            ).`is`(HamcrestCondition(equalTo(output)))
        }
    }

    companion object {
        private val internationalPairs = listOf(
            Pair(
                first = listOf("XXS", "XS", "S", "M", "L", "XL", "2XL", "3XL",),
                second = "XXS-3XL"
            ),
            Pair(
                first = listOf("XXS", "M", "L", "XL"),
                second = "XXS, M-XL"
            ),
            Pair(
                first = listOf("XXS", "XS", "S", "L", "M", "XL", "2XL", "3XL",),
                second = "XXS-S, L, M, XL-3XL"
            ),
            Pair(
                first = listOf("XXS", "XS", "S", "L", "XL", "2XL", "3XL",),
                second = "XXS-S, L-3XL"
            ),
        )

        private val numericPairs = listOf(
            Pair(
                first = emptyList(),
                second = ""
            ),
            Pair(
                first = listOf("26"),
                second = "26"
            ),
            Pair(
                first = listOf("26", "28"),
                second = "26, 28"
            ),
            Pair(
                first = listOf("26", "28", "30"),
                second = "26-30"
            ),
            Pair(
                first = listOf("26", "28", "30", "32"),
                second = "26-32"
            ),
            Pair(
                first = listOf("26", "28", "30", "32", "36"),
                second = "26-32, 36"
            ),
            Pair(
                first = listOf("26", "28", "30", "32", "36", "38"),
                second = "26-32, 36, 38"
            ),
            Pair(
                first = listOf("26", "28", "30", "32", "36", "38", "40"),
                second = "26-32, 36-40"
            ),
            Pair(
                first = listOf("26", "28", "30", "32", "36", "38", "40", "44", "46"),
                second = "26-32, 36-40, 44, 46"
            ),
            Pair(
                first = listOf("26", "28", "30", "32", "36", "38", "40", "44", "46", "50", "56"),
                second = "26-32, 36-40, 44, 46 +2"
            ),
        )
    }
}