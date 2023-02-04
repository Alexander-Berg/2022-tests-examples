package ru.yandex.vertis.personal.api.favorites.v2.model

import spray.json.{enrichAny, JsNumber, JsObject, JsString}
import ru.yandex.vertis.personal.util.BaseSpec

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 29.06.16
  */
class CountResponseSpec extends BaseSpec {
  "CountResponse" should {
    "convert to json" in {
      val res = CountResponse(
        "autoru",
        "2.0",
        "abc",
        Map(
          "searches" -> 3,
          "cards" -> Int.MaxValue
        )
      )
      val expectedJson = JsObject(
        "service" -> JsString("autoru"),
        "version" -> JsString("2.0"),
        "user" -> JsString("abc"),
        "entities" -> JsObject(
          "searches" -> JsNumber(3),
          "cards" -> JsNumber(2147483647)
        )
      )
      res.toJson shouldBe expectedJson
    }
  }
}
