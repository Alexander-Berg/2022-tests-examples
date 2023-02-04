package ru.yandex.vertis.personal.api.favorites.v2.model

import spray.json.{enrichAny, JsArray, JsObject, JsString}
import ru.yandex.vertis.personal.util.BaseSpec

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 29.06.16
  */
class GetResponseSpec extends BaseSpec {
  "GetResponse" should {
    "convert to json" in {
      val entity = Entity("a", 0, 2, None)
      val res = GetResponse(
        "autoru",
        "2.0",
        "alabama",
        Map(
          "searches" -> Seq(entity),
          "cards" -> Seq.empty
        )
      )
      val expectedJson = JsObject(
        "service" -> JsString("autoru"),
        "version" -> JsString("2.0"),
        "user" -> JsString("alabama"),
        "entities" -> JsObject(
          "searches" -> JsArray(entity.toJson),
          "cards" -> JsArray.empty
        )
      )
      res.toJson shouldBe expectedJson
    }
  }
}
