package ru.yandex.market.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.yandex.market.utils.InformationUnit.BYTES
import ru.yandex.market.utils.InformationUnit.KILOBYTES
import ru.yandex.market.utils.InformationUnit.MEGABYTES

@RunWith(Parameterized::class)
class InformationUnitTest(
    private val fromUnit: InformationUnit,
    private val toUnit: InformationUnit,
    private val value: Double,
    private val expectedValue: Double
) {

    @Test
    fun `Check converted value match expectation`() {
        val convertedValue = fromUnit.convertTo(value, toUnit)
        assertThat(convertedValue).isEqualTo(expectedValue)
    }

    companion object {

        @Parameterized.Parameters(name = "{index}: {2} {0} is {3} {1}")
        @JvmStatic
        fun data() = listOf<Array<*>>(
            arrayOf(BYTES, BYTES, 1.0, 1.0),
            arrayOf(BYTES, KILOBYTES, 1024.0, 1.0),
            arrayOf(BYTES, MEGABYTES, 1048576.0, 1.0),
            arrayOf(KILOBYTES, BYTES, 1.0, 1024.0),
            arrayOf(KILOBYTES, KILOBYTES, 1.0, 1.0),
            arrayOf(KILOBYTES, MEGABYTES, 1024.0, 1.0),
            arrayOf(MEGABYTES, BYTES, 1.0, 1048576.0),
            arrayOf(MEGABYTES, KILOBYTES, 1.0, 1024.0),
            arrayOf(MEGABYTES, MEGABYTES, 1.0, 1.0),
            arrayOf(BYTES, KILOBYTES, 512.0, 0.5),
            arrayOf(BYTES, MEGABYTES, 524288.0, 0.5),
            arrayOf(KILOBYTES, BYTES, 0.5, 512.0),
            arrayOf(KILOBYTES, MEGABYTES, 512.0, 0.5),
            arrayOf(MEGABYTES, BYTES, 0.5, 524288.0),
            arrayOf(MEGABYTES, KILOBYTES, 0.5, 512.0)
        )
    }
}