package ru.yandex.vertis.personal.api.settings.v1.model

import spray.json.{enrichAny, JsObject, JsString}
import ru.yandex.vertis.personal.util.BaseSpec

/**
  * Spec on settings [[GetResponse]] v1.
  *
  * @author dimas
  */
class GetResponseSpec extends BaseSpec {

  "GetResponse" should {
    "convert to json" in {
      val res = GetResponse(
        "realty",
        "1.0",
        UserSettings("uid", "1", Map("foo" -> "bar", "baz" -> "bee"))
      )
      val expectedJson = JsObject(
        "service" -> JsString("realty"),
        "version" -> JsString("1.0"),
        "user" -> JsObject(
          "kind" -> JsString("uid"),
          "id" -> JsString("1"),
          "settings" -> JsObject(
            "foo" -> JsString("bar"),
            "baz" -> JsString("bee")
          )
        )
      )
      res.toJson shouldBe expectedJson
    }
  }

}
