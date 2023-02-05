package ru.yandex.market.clean.presentation.formatter

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.yandex.market.feature.timer.ui.ElapsedTimeFormatter
import ru.yandex.market.feature.timer.ui.ElapsedVideoTimeVo

@RunWith(Parameterized::class)
class ElapsedTimeFormatterTest(
    private val elapsedMilliseconds: Long,
    private val expectedOutput: ElapsedVideoTimeVo
) {

    private val formatter = ElapsedTimeFormatter()

    @Test
    fun `Check formatter output matches expectation`() {
        val vo = formatter.format(elapsedMilliseconds)
        assertThat(formatter.format(elapsedMilliseconds)).isEqualTo(expectedOutput)
    }

    companion object {

        @Parameterized.Parameters(name = "{index}: format({0}) == {1}")
        @JvmStatic
        fun data() = listOf(
            arrayOf(
                0,
                ElapsedVideoTimeVo(
                    milliseconds = 0,
                    formattedNormal = "00:00:00",
                    formattedWide = "00\u2009:\u200900\u2009:\u200900",
                    formattedWithoutHoursNormal = "00:00"
                )
            ),
            arrayOf(
                1,
                ElapsedVideoTimeVo(
                    milliseconds = 1,
                    formattedNormal = "00:00:00",
                    formattedWide = "00\u2009:\u200900\u2009:\u200900",
                    formattedWithoutHoursNormal = "00:00"
                )
            ),
            arrayOf(
                1000,
                ElapsedVideoTimeVo(
                    milliseconds = 1000,
                    formattedNormal = "00:00:01",
                    formattedWide = "00\u2009:\u200900\u2009:\u200901",
                    formattedWithoutHoursNormal = "00:01"
                )
            ),
            arrayOf(
                20000,
                ElapsedVideoTimeVo(
                    milliseconds = 20000,
                    formattedNormal = "00:00:20",
                    formattedWide = "00\u2009:\u200900\u2009:\u200920",
                    formattedWithoutHoursNormal = "00:20"
                )
            ),
            arrayOf(
                61000, ElapsedVideoTimeVo(
                    milliseconds = 61000,
                    formattedNormal = "00:01:01",
                    formattedWide = "00\u2009:\u200901\u2009:\u200901",
                    formattedWithoutHoursNormal = "01:01"
                )
            ),
            arrayOf(
                121000,
                ElapsedVideoTimeVo(
                    milliseconds = 121000,
                    formattedNormal = "00:02:01",
                    formattedWide = "00\u2009:\u200902\u2009:\u200901",
                    formattedWithoutHoursNormal = "02:01"
                )
            ),
            arrayOf(
                90000000,
                ElapsedVideoTimeVo(
                    milliseconds = 90000000,
                    formattedNormal = "25:00:00",
                    formattedWide = "25\u2009:\u200900\u2009:\u200900",
                    formattedWithoutHoursNormal = "00:00"
                )
            ),
            arrayOf(
                75600000,
                ElapsedVideoTimeVo(
                    milliseconds = 75600000,
                    formattedNormal = "21:00:00",
                    formattedWide = "21\u2009:\u200900\u2009:\u200900",
                    formattedWithoutHoursNormal = "00:00"
                )
            ),
            arrayOf(
                360000000,
                ElapsedVideoTimeVo(
                    milliseconds = 360000000,
                    formattedNormal = "100:00:00",
                    formattedWide = "100\u2009:\u200900\u2009:\u200900",
                    formattedWithoutHoursNormal = "00:00"
                )
            )
        )
    }
}