package ru.yandex.market.utils.hyphenation

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class HyphenatorSuccessHyphenationTest(
    private val inputWord: String,
    private val expectedResult: List<String>
) {

    private val hyphenator = Hyphenator.getInstance(HyphenationPattern.RU)

    @Test
    fun `checking rus hyphenation`() {
        assertThat(hyphenator.splitBySyllables(inputWord)).isEqualTo(expectedResult)
    }

    companion object {

        @Parameterized.Parameters(name = "{index}: {0} - it should decompose into {1}")
        @JvmStatic
        fun parameters() = listOf(

            arrayOf("Буря", listOf("Бу–ря")),
            arrayOf("Молоко", listOf("Мо–локо", "Моло–ко")),
            arrayOf("Шуруповёрт", listOf("Шу–руповёрт", "Шуру–повёрт", "Шурупо–вёрт")),
            arrayOf("Говноручка", listOf("Гов–норучка", "Говно–ручка", "Говноруч–ка")),
            arrayOf(
                "Иглоукалыватель",
                listOf(
                    "Иг–лоукалыватель",
                    "Игло–укалыватель",
                    "Иглоука–лыватель",
                    "Иглоукалы–ватель",
                    "Иглоукалыва–тель"
                )
            ),
            arrayOf("ку", listOf("ку")),
            arrayOf("Квинтэссенция", listOf("Квинт–эссенция", "Квинтэс–сенция", "Квинтэссен–ция")),
            arrayOf(
                "Шуруповёрты",
                listOf("Шу–руповёрты", "Шуру–повёрты", "Шурупо–вёрты", "Шуруповёр–ты")
            ),
            arrayOf("связанные", listOf("свя–занные", "связан–ные")),
            arrayOf(
                "Псевдоинтеллектуал",
                listOf(
                    "Псев–доинтеллектуал",
                    "Псевдо–интеллектуал",
                    "Псевдоин–теллектуал",
                    "Псевдоинтел–лектуал",
                    "Псевдоинтеллек–туал",
                    "Псевдоинтеллекту–ал"
                )
            )
        )
    }
}