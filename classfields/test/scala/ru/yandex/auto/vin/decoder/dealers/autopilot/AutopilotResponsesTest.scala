package ru.yandex.auto.vin.decoder.dealers.autopilot

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.Json
import ru.yandex.auto.vin.decoder.dealers.autopilot.AutopilotResponses.AutopilotEntry

import scala.io.Source

class AutopilotResponsesTest extends AnyFunSuite with Matchers {

  test("deserialization") {
    val jsValue = Json.parse(getClass.getResourceAsStream("/autopilotTest2.json"))
    assert(jsValue.validate[Seq[AutopilotEntry]].isSuccess)
  }
}
