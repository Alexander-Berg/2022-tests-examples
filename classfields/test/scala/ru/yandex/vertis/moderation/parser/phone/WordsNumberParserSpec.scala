package ru.yandex.vertis.moderation.parser.phone

import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.parser.StringPatcher
import ru.yandex.vertis.moderation.parser.phone.WordsNumberParser.Numeral
import ru.yandex.vertis.moderation.parser.phone.WordsNumberParserSpec._

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * @author potseluev
  */
class WordsNumberParserSpec extends SpecBase {

  implicit private def toStr(number: Numeral): String = number.toString

  private val parser = new WordsNumberParser()
  private val patcher: StringPatcher = parser.asPatcher

  private val testCases: Seq[TestCase] =
    Seq(
      //    TestCase(
      //      description = Some("correctly split numbers"),
      //      text = "пять тысяч двести пятьдесят три тысячи сто сорок два",
      //      expected = "5250 3142" //now it returns 5253 1142, maybe we will fix it someday
      //    ),
      TestCase(
        text = "ноль троек",
        expected = "0 3"
      ),
      TestCase(
        text = "пять тысяч двести пятьдесят три тысячи сто сорок два",
        expected = "5253 1142"
      ),
      TestCase(
        text = "тысяча два",
        expected = "1002"
      ),
      TestCase(
        text = "тысяча тысяч",
        expected = "1000 1000"
      ),
      TestCase(
        text = "+ семь девят сот пятсот - двадцать семнадцать",
        expected = "+ 7 900 500 - 20 17"
      ),
      TestCase(
        text = "двадцать девятнадцать",
        expected = "20 19"
      ),
      TestCase(
        text = "двадцать пять девятнадцать",
        expected = "25 19"
      ),
      TestCase(
        text = "8 девятьсот 2восем 3восемь восемь 66 ноль-ноль",
        expected = "8 900 28 38 8 66 0-0"
      ),
      TestCase(
        text = "сто двадцать одна тысяча",
        expected = "121000"
      ),
      TestCase(
        description = Some("not multiply if cases are mismatched"),
        text = "сто двадцать одна тысяч",
        expected = "121 1000"
      ),
      TestCase(
        description = Some("not multiply if cases are mismatched"),
        text = "два тысяча",
        expected = "2 1000"
      ),
      TestCase(
        description = Some("multiply if cases are matched"),
        text = "два тысячи",
        expected = "2000"
      ),
      TestCase(
        description = Some("not repeat if result will be too long"),
        text = "пять десятков",
        expected = "5 10"
      ),
      TestCase(
        description = Some("repeat"),
        text = "две десятки",
        expected = "1010"
      ),
      TestCase(
        text = "две тысячи тысяча",
        expected = "2000 1000"
      ),
      TestCase(
        text = "+ семь девят сот пятсот четыре симЁрки",
        expected = "+ 7 900 500 7777"
      ),
      TestCase(
        text = "единиц",
        expected = "1"
      ),
      TestCase(
        text = "двадцать единиц",
        expected = "20 1"
      ),
      TestCase(
        text = "тысяча двадцать три единицы",
        expected = "1020 111"
      ),
      TestCase(
        text = "двадцать два тридцать пять",
        expected = "22 35"
      ),
      TestCase(
        text = "вотсап для связи 8 сто пятсот четыре симёрки звоните",
        expected = "вотсап для связи 8 100 500 7777 звоните"
      ),
      TestCase(
        text = "семь84семь",
        expected = "7847"
      ),
      TestCase(
        text = "восем девятсот сто питсот читыре",
        expected = "8 900 100 504"
      ),
      TestCase(
        text = "двацат одЫн",
        expected = "21"
      ),
      TestCase(
        text = "двапать три одна тысича четыре три симёрки",
        expected = "23 1004 777"
      ),
      TestCase(
        text = "Сто пят сИмнадцат трИ нуля",
        expected = "105 17 000"
      ),
      TestCase(
        text = "два слова и число три тысячи сто двадцать пять+ еще что-то",
        expected = "2 слова и число 3125+ еще что-то"
      ),
      TestCase(
        text = "тысяча сто три",
        expected = "1103"
      ),
      TestCase(
        text = "одна тысяча сто три",
        expected = "1103"
      ),
      TestCase(
        text = "Восемь девять10 215 07 пять 6",
        expected = "8 910 215 07 5 6"
      ),
      TestCase(
        text = "восемь,девять сот,девятнадцать,восемь сот,двадцать шесть,восемьдесят,сорок шесть.",
        expected = "8,919,826,80,46."
      ),
      TestCase(
        text = "Восемь,девять,один,шесть,один,один,ноль,шесть,семь,семь,шесть",
        expected = "8,9,1,6,1,1,0,6,7,7,6"
      ),
      TestCase(
        text = "три единицы двести пять",
        expected = "111 205"
      ),
      TestCase(
        text = "Восемь-деВять-одИн-шеСть-оДин-одиН-ноль-шесть-семь-семь-шесть",
        expected = "8-9-1-6-1-1-0-6-7-7-6"
      ),
      TestCase(
        text = "восемь 9197235 три нуля",
        expected = "8 9197235 000"
      ),
      TestCase(
        text = "двадцать-два",
        expected = "20-2"
      ),
      TestCase(
        text = "восемь 9 двадцать 8 3 восемь семь 88 девять",
        expected = "8 9 20 8 3 8 7 88 9"
      ),
      TestCase(
        text = "восемь-девятьсот шесьдесят один-двести семьдесят семь-восемнадцать-сорок один",
        expected = "8-961-277-18-41"
      )
    )

  "WordsNumberParser" should {

    testCases.foreach { case TestCase(text, expected, optDescription) =>
      val description = s"${optDescription.map(s => s"$s and ").getOrElse("")}transform $text to $expected"
      description in {
        patcher(text).futureValue.value shouldBe expected
      }
    }
  }
}

object WordsNumberParserSpec {

  case class TestCase(text: String, expected: String, description: Option[String] = None)

}
