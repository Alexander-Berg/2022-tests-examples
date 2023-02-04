package ru.yandex.vertis.personal.api.settings.v1.model

import ru.yandex.vertis.personal.util.BaseSpec
import spray.json.{enrichAny, JsObject, JsString}

class UserSettingsSpec extends BaseSpec {

  "GetResponse" should {
    "convert to json" in {
      val res = UserSettings("uid", "1", Map("foo" -> "bar", "baz" -> "bee"))
      val expectedJson = JsObject(
        "kind" -> JsString("uid"),
        "id" -> JsString("1"),
        "settings" -> JsObject(
          "foo" -> JsString("bar"),
          "baz" -> JsString("bee")
        )
      )
      res.toJson shouldBe expectedJson
    }
  }

}
