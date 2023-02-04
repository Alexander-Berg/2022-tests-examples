package ru.yandex.realty.antirobot

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import play.api.libs.json.{JsArray, JsObject, Json}
import ru.yandex.realty.SpecBase

@RunWith(classOf[JUnitRunner])
class AntirobotHelperSpec extends SpecBase {

  "AntirobotHelper" should {
    "fakeOffer without price should be fine " in {
      val json = Json.parse("""
          |{
          |  "offerId": "12321",
          |  "price": {
          |   "value": 1,
          |   "price": {
          |     "value": 2
          |   }
          |  },
          |  "history": {}
          |}
          """.stripMargin).as[JsObject]

      ((AntirobotHelper.fakeOffer(json) \ "history").as[JsObject] \ "prices").as[JsArray].value shouldBe empty
    }
  }
}
