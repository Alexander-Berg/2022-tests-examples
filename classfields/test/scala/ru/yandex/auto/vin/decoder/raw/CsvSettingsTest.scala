package ru.yandex.auto.vin.decoder.raw

import com.univocity.parsers.csv.{CsvParser, CsvParserSettings}
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.auto.vin.decoder.raw.autoru.AutoruCsvUtils
import ru.yandex.auto.vin.decoder.raw.autoteka.AutotekaCsvUtils

import scala.util.Try

class CsvSettingsTest extends AnyFunSuite {

  test("Quotes parsed correctly") {

    // Рекомендации по формату csv: https://datatracker.ietf.org/doc/html/rfc4180 (2.5, 2.6, 2.7)
    // Каждое value может быть заключено или не заключено в кавычки
    // - Если value не заключено в кавычки, то кавычки внутри value недопустимы
    // - Если value заключено в кавычки, кавычки внутри value должны экранироваться удваиванием: "ЗАО ""Торгмаш"" ИНН 1"

    assert(checkQuotesParsing(AutotekaCsvUtils.csvParseSettings()))
    assert(checkQuotesParsing(AutoruCsvUtils.csvParseSettings()))
  }

  private def checkParsing(
      settings: CsvParserSettings,
      input: String,
      check: Array[String] => Boolean): Boolean = {
    Try(check(new CsvParser(settings).parseLine(input))).getOrElse(false)
  }

  private def checkQuotesParsing(settings: CsvParserSettings): Boolean = {
    List(
      ValuesQuoted.simple,
      ValuesQuoted.innerQuotesEscaped,
      ValuesQuoted.innerQuotesUnescaped,
      ValuesUnquoted.simple,
      ValuesUnquoted.singleValueQuoted,
      ValuesUnquoted.innerQuotes,
      ValuesUnquoted.quoteAtStartUnescaped,
      ValuesUnquoted.quoteAtStartEscaped,
      ValuesUnquoted.delimiterInsideQuotes
    ).forall(checkParsing(settings, _, _.length == 3))
  }

  private object ValuesQuoted {

    // (a, b, c)
    val simple =
      """
        |"h1";"h2";"h3"
        |"a";"b";"c"
        |""".stripMargin

    // (regulation, Стандартное ТО. ТО "Замена свечей зажигания". ТО по регламенту Oilservice. Подготовительно-заключительные работы., 0)
    val innerQuotesEscaped =
      """
        |"h1";"h2";"h3"
        |"regulation";" Стандартное ТО. ТО ""Замена свечей зажигания"". ТО по регламенту Oilservice. Подготовительно-заключительные работы.";"0"
        |""".stripMargin

    // value закавычены, но внутренние кавычки не экранированы (не удвоены)
    // (regulation, " Стандартное ТО. ТО "Замена свечей зажигания". ТО по регламенту Oilservice. Подготовительно-заключительные работы.", 0)
    val innerQuotesUnescaped =
      """
      |"h1";"h2";"h3"
      |"regulation";" Стандартное ТО. ТО "Замена свечей зажигания". ТО по регламенту Oilservice. Подготовительно-заключительные работы.";"0"
      |""".stripMargin
  }

  private object ValuesUnquoted {

    // (a, b, c)
    val simple =
      """
        |h1;h2;h3
        |a;b;c
        |""".stripMargin

    // (a, b, c)
    val singleValueQuoted =
      """
        |h1;h2;h3
        |a;"b";c
        |""".stripMargin

    // (a, b"c"d, e)
    val innerQuotes =
      """
        |h1;h2;h3
        |a;b"c"d;e
        |""".stripMargin

    // Когда value начинается с кавычки, парсер считает что это закавыченное value, и ждёт
    // что закрывающая кавычка будет прямо перед следующим разделителем value.
    // Если парсер встречает закрывающую кавычку раньше, он разрешает это
    // в соответствии с опцией setUnescapedQuoteHandling:
    // STOP_AT_DELIMITER: (a, "c"def, g)
    // STOP_AT_CLOSING_QUOTE: (a, c"der;g) -- всё до следующего "; считается за value и легко может перевалить за макс. длину
    val quoteAtStartUnescaped =
      """
      |h1;h2;h3
      |a;"c"def;g
      |""".stripMargin

    // Если нужно начать value с кавычки, то надо 1. завернуть весь value в кавычки, 2. удвоить все внутренние кавычки
    // (a, "c"def, g)
    val quoteAtStartEscaped = List(
      "h1;h2;h3",
      "a;\"\"\"c\"\"def\";g"
    ).mkString("\n")

    // Не соответствует рекомендациям (кавычки внутри незакавыченного value)
    // STOP_AT_DELIMITER: (a, "b;"cde, f)
    // STOP_AT_CLOSING_QUOTE: null
    val delimiterInsideQuotes =
      """
      |"h1;h2;h3",
      |a;"b;"cde;f
      |""".stripMargin
  }
}
