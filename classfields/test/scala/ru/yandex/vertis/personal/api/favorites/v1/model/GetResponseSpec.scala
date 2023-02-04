package ru.yandex.vertis.personal.api.favorites.v1.model

import spray.json.{enrichAny, JsArray, JsObject, JsString}
import ru.yandex.vertis.personal.util.BaseSpec

/**
  * Specs on [[GetResponse]] v1
  *
  * @author dimas
  */
class GetResponseSpec extends BaseSpec {

  "GetResponse" should {
    "convert to json" in {
      val res =
        GetResponse("realty", "1.0", UserEntities("uid", "1", Seq("e1", "e2")))
      val expectedJson = JsObject(
        "service" -> JsString("realty"),
        "version" -> JsString("1.0"),
        "user" -> JsObject(
          "kind" -> JsString("uid"),
          "id" -> JsString("1"),
          "entities" -> JsArray(JsString("e1"), JsString("e2"))
        )
      )
      res.toJson shouldBe expectedJson
    }
  }
}
