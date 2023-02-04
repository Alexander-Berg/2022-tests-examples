package ru.yandex.auto.vin.decoder.dealers.spbauto

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.Json
import ru.yandex.auto.vin.decoder.dealers.spbcar.SpbcarResponses.SpbcarEntry

import scala.io.Source

class SpbcarResponsesTest extends AnyFunSuite with Matchers {

  test("deserialization") {
    val jsValue = Json.parse(getClass.getResourceAsStream("/spbAutoSample1.json"))
    val jsValue2 = Json.parse(getClass.getResourceAsStream("/spbAutoSample2.json"))
    assert(jsValue.validate[SpbcarEntry].isSuccess)
    assert(jsValue2.validate[SpbcarEntry].isSuccess)
  }
}
