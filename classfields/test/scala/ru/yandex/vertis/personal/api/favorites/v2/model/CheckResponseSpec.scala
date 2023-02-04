package ru.yandex.vertis.personal.api.favorites.v2.model

import spray.json.{enrichAny, JsBoolean, JsObject}
import ru.yandex.vertis.personal.util.BaseSpec

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 29.06.16
  */
class CheckResponseSpec extends BaseSpec {
  "CheckResponse" should {
    "convert to json" in {
      val res =
        CheckResponse(Map("a" -> true, "b" -> false, "__1fsF:5.15" -> true))
      val expectedJson = JsObject(
        "a" -> JsBoolean(true),
        "b" -> JsBoolean(false),
        "__1fsF:5.15" -> JsBoolean(true)
      )
      res.toJson shouldBe expectedJson
    }
  }
}
