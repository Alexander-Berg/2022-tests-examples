package ru.yandex.auto.vin.decoder.partners.jlr

import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.{JsObject, Json}
import ru.yandex.auto.vin.decoder.partners.jlr.JlrResponses.AllRepairsExResponse

import scala.io.Source

class JlrResponsesTest extends AnyWordSpecLike {

  def getVinResponses: collection.Map[String, String] = {
    Json.parse(getClass.getResourceAsStream("/jlr_responses.json")).as[JsObject].value.map { case (vin, content) =>
      vin -> content.toString()
    }
  }

  def testContext(testCode: collection.Map[String, String] => Unit): Unit = {
    testCode(getVinResponses)
  }

  "JlrResponses" should {
    "deserialize" in testContext { data =>
      data.foreach { case (_, rawResponse) =>
        Json.parse(rawResponse).as[AllRepairsExResponse].toMessage
      }
    }
  }
}
