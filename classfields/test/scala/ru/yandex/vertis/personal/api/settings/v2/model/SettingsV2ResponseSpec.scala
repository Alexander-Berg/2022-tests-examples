package ru.yandex.vertis.personal.api.settings.v2.model

import ru.yandex.vertis.personal.util.BaseSpec
import spray.json.{enrichAny, JsObject, JsString}

class SettingsV2ResponseSpec extends BaseSpec {

  "SettingsV2Response" should {
    "convert to json" in {
      val example = SettingsV2Response(
        Map("1" -> Map("foo" -> "bar", "baz" -> "bee"))
      )
      val expectedJson = JsObject(
        "items" -> JsObject(
          "1" -> JsObject(
            "foo" -> JsString("bar"),
            "baz" -> JsString("bee")
          )
        )
      )
      example.toJson shouldBe expectedJson
    }
  }

}
