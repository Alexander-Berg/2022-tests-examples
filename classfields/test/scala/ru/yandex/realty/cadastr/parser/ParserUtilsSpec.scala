package ru.yandex.realty.cadastr.parser

import com.google.protobuf.BoolValue
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat => JodaDateTimeFormat}
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ParserUtilsSpec extends ParserUtilsBase {

  "ParserUtils" should {

    "parse share text" in {
      shareTexts.foreach { shareText =>
        val text = shareText._1
        val expected = shareText._2

        assertEquals(expected, parseShareText(text))
      }
    }

    "parse mortgage end date" in {
      val dateFormat = JodaDateTimeFormat.forPattern("dd.MM.yyyy")
      val startTs = parseTimestamp(dateFormat.print(DateTime.now())).get

      getMortgageEndDates(startTs).foreach { pair =>
        val text = pair._1
        val expected = pair._2

        assertEquals(expected, parseMortgageEndDate(startTs, text))
      }
    }
  }
}
