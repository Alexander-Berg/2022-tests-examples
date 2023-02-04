package ru.yandex.auto.vin.decoder.utils

import org.joda.time.DateTime
import org.scalatest.Assertion
import org.scalatest.funsuite.AnyFunSuite
import play.api.libs.json._
import ru.yandex.auto.vin.decoder.utils.json.JsonUtils

import java.time.{LocalDate, LocalDateTime, LocalTime}

class JsonUtilsTest extends AnyFunSuite {

  implicit private val ldFormat = JsonUtils.localDateFormat("dd.MM.yyyy", "MM/dd/yyyy")
  implicit private val ldtFormat = JsonUtils.localDateTimeFormat("dd.MM.yyyy HH:mm", "dd.MM.yyyy HH:mm:ss")
  implicit private val jdtFormat = JsonUtils.jodaDateTimeFormat("dd.MM.yyyy", "dd.MM.yyyy HH:mm:ss")

  val localTime = LocalTime.of(0, 0, 0)
  val localDate = LocalDate.of(1989, 9, 21)
  val localDateTime = LocalDateTime.of(localDate, localTime)

  val jodaDateTime = new DateTime(1989, 9, 21, 0, 0, 0)

  def compare[T: Reads](input: String, temporal: T): Assertion = {
    assert(Json.parse("\"" + input + "\"").as[T] == temporal)
  }

  test("LocalDate") {
    compare("21.09.1989", localDate)
    compare("09/21/1989", localDate)
  }

  test("LocalDateTime") {
    compare("21.09.1989 00:00:00", localDateTime)
    compare("21.09.1989 00:00", localDateTime)
  }

  test("joda DateTime") {
    compare("21.09.1989 00:00:00", jodaDateTime)
    compare("21.09.1989", jodaDateTime)
  }
}
