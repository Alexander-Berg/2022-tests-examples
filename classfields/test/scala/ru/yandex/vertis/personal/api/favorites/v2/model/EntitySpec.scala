package ru.yandex.vertis.personal.api.favorites.v2.model

import spray.json.{enrichAny, JsNumber, JsObject, JsString}
import ru.yandex.vertis.personal.util.BaseSpec

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 29.06.16
  */
class EntitySpec extends BaseSpec {
  "Entity" should {
    "convert to json (with payload)" in {
      val entity = Entity(
        "abc",
        1233334L,
        1444155L,
        Some("some_payload")
      )
      val expectedJson = JsObject(
        "entity_id" -> JsString("abc"),
        "create_timestamp" -> JsNumber(1233334L),
        "update_timestamp" -> JsNumber(1444155L),
        "payload" -> JsString("some_payload")
      )
      entity.toJson shouldBe expectedJson
    }

    "convert to json without payload" in {
      val entity = Entity(
        "abc",
        1233334L,
        1444155L,
        Option.empty
      )
      val expectedJson = JsObject(
        "entity_id" -> JsString("abc"),
        "create_timestamp" -> JsNumber(1233334L),
        "update_timestamp" -> JsNumber(1444155L)
      )
      entity.toJson shouldBe expectedJson
    }
  }
}
