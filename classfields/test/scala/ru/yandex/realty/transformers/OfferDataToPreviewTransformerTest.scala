package ru.yandex.realty.transformers

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.{JsObject, JsValue, Json}
import ru.yandex.realty.util.Resource

@RunWith(classOf[JUnitRunner])
class OfferDataToPreviewTransformerTest extends WordSpec with Matchers {

  private val pathInput = "/transformers/offer.json"
  private val pathExpected = "/transformers/expectedOfferData.json"

  "OfferDataToPreviewTransformerTest" should {
    val transformer = new OfferDataToPreviewTransformer()
    "transform" in {
      val offer = getJson(pathInput)
      val res = transformer.transform(offer.asInstanceOf[JsObject])
      val expected = getJson(pathExpected)
      res shouldBe expected
    }

  }

  def getJson(path: String): JsValue = {
    val jsonStr = Resource.fromClassPathToString(path)
    Json.parse(jsonStr)
  }

}
