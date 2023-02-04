package ru.yandex.realty.archive

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.Assertion
import org.scalatest.{FlatSpec, Matchers}
import org.scalatestplus.junit.JUnitRunner
import play.api.libs.json.Json
import ru.yandex.realty.model.archive.JodaUtils.{dateTimeFormatter, dateTimeJsonFormat, parseDateTimeAsJson}

/**
  * Created by Viacheslav Kukushkin <vykukushkin@yandex-team.ru> on 2019-02-15
  */

@RunWith(classOf[JUnitRunner])
class DateTimeParserSpec extends FlatSpec with Matchers {
  //short format is initially stored to C* and hbase
  //long format is created after scala changes data
  val strShort = "2015-04-19T00:00:00+03:00"
  val strLong = "2015-04-19T00:00:00.000+03:00"

  def jsonConstant(str: String): String = {
    "\"" ++ str ++ "\""
  }

  it should "read both formats by play and save them to long one" in {
    def jsonSavedToLongFormat(str: String): Assertion = {
      val dt = Json.parse(jsonConstant(str)).as[DateTime]
      Json.toJson(dt).toString() shouldEqual jsonConstant(strLong)
    }

    jsonSavedToLongFormat(strShort)
    jsonSavedToLongFormat(strLong)
  }

  it should "parse dates manually in the same format as play does" in {
    def parseManually(str: String): Assertion = {
      val dtManually = parseDateTimeAsJson(str)
      val dtJsoned = Json.parse(jsonConstant(str)).as[DateTime]
      dtManually shouldEqual dtJsoned
    }

    parseManually(strShort)
    parseManually(strLong)
  }

  it should "serialize dates manually in the same format as play does" in {
    def printManually(str: String): Assertion = {
      val dt = parseDateTimeAsJson(str)
      val outManually = dateTimeFormatter.print(dt)
      val outJsoned = Json.toJson(dt).toString()
      jsonConstant(outManually) shouldEqual outJsoned
    }

    printManually(strShort)
    printManually(strLong)
  }
}
