package ru.auto.api

import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.util.Protobuf
import org.scalatest.matchers.should.Matchers._
import ru.auto.api.CounterModel.AggregatedCounter
import ru.auto.api.util.JsonMatchers._

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 18.02.17
  */
class CountersTest extends AnyFunSuite {

  test("always serialize zero values") {
    val counters = AggregatedCounter
      .newBuilder()
      .setAll(0)
      .setDaily(0)
      .setPhoneAll(0)
      .setPhoneDaily(0)
      .build()

    Protobuf.toJson(counters) should matchJson("""{
        |  "all": 0,
        |  "daily": 0,
        |  "phone_all": 0,
        |  "phone_daily": 0
        |}""".stripMargin)
  }
}
