package ru.yandex.realty.util

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.util.LayoutSwitchUtils._

@RunWith(classOf[JUnitRunner])
class LayoutSwitchUtilsTest extends SpecBase {

  "LayoutSwitchUtils" should {

    "switch to russian layout" in {
      switchEnglishLayoutToRussian("offer") shouldEqual "щааук"
      switchEnglishLayoutToRussian("OFFER") shouldEqual "ЩААУК"
      switchEnglishLayoutToRussian("qwerty") shouldEqual "йцукен"
      switchEnglishLayoutToRussian("QWERTY") shouldEqual "ЙЦУКЕН"
    }

    "switch to russian layout with punctuation symbols" in {
      switchEnglishLayoutToRussian("b;c") shouldEqual "ижс"
      switchEnglishLayoutToRussian("B:C") shouldEqual "ИЖС"
      switchEnglishLayoutToRussian(",fpf jnls[f") shouldEqual "база отдыха"
      switchEnglishLayoutToRussian("<FPF JNLS{F") shouldEqual "БАЗА ОТДЫХА"
      switchEnglishLayoutToRussian("djlj`v") shouldEqual "водоём"
      switchEnglishLayoutToRussian("DJLJ~V") shouldEqual "ВОДОЁМ"
      switchEnglishLayoutToRussian("djlj\\v") shouldEqual "водоём"
      switchEnglishLayoutToRussian("DJLJ|V") shouldEqual "ВОДОЁМ"
      switchEnglishLayoutToRussian("[kt,") shouldEqual "хлеб"
      switchEnglishLayoutToRussian("{KT<") shouldEqual "ХЛЕБ"
      switchEnglishLayoutToRussian(",.;'[]") shouldEqual "бюжэхъ"
      switchEnglishLayoutToRussian("<>:\"{}") shouldEqual "БЮЖЭХЪ"
    }

    "switch to english layout" in {
      switchRussianLayoutToEnglish("окна") shouldEqual "jryf"
      switchRussianLayoutToEnglish("ОКНА") shouldEqual "JRYF"
      switchRussianLayoutToEnglish("йцукен") shouldEqual "qwerty"
      switchRussianLayoutToEnglish("ЙЦУКЕН") shouldEqual "QWERTY"
    }
  }
}
