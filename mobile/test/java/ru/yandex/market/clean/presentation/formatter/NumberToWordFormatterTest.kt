package ru.yandex.market.clean.presentation.formatter

import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.yandex.market.clean.presentation.feature.cashback.growingcashback.agitate.NumberToWordFormatter

@RunWith(Parameterized::class)
class NumberToWordFormatterTest(
    private val input: Int,
    private val expectedOutput: String,
) {

    private val formatter = NumberToWordFormatter()

    @Test
    fun format() {
        val formatted = formatter.format(input).consumeError { e -> e.toString() }
        Assertions.assertThat(formatted).isEqualTo(expectedOutput)
    }

    companion object {
        @Parameterized.Parameters(name = "{index}: {0} -> {1}")
        @JvmStatic
        fun data(): Iterable<Array<*>> = listOf(
            //0
            arrayOf(
                3,
                "три",
            ),
            //1
            arrayOf(
                7,
                "семь",
            ),
            //2
            arrayOf(
                13,
                "тринадцать",
            ),
            //3
            arrayOf(
                17,
                "семнадцать",
            ),
            //4
            arrayOf(
                25,
                "двадцать пять",
            ),
            //5
            arrayOf(
                42,
                "сорок два",
            ),
            //6
            arrayOf(
                137,
                "сто тридцать семь",
            ),
            //7
            arrayOf(
                293,
                "двести девяносто три",
            ),
            //8
            arrayOf(
                1300,
                "одна тысяча триста",
            ),
            //9
            arrayOf(
                2587,
                "две тысячи пятьсот восемьдесят семь",
            ),
            //10
            arrayOf(
                2583,
                "две тысячи пятьсот восемьдесят три",
            ),
            //11
            arrayOf(
                15723,
                "пятнадцать тысяч семьсот двадцать три",
            ),
            //12
            arrayOf(
                837624,
                "восемьсот тридцать семь тысяч шестьсот двадцать четыре",
            ),
            //13
            arrayOf(
                831629,
                "восемьсот тридцать одна тысяча шестьсот двадцать девять",
            ),
        )
    }
}
