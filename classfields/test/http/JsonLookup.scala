package ru.yandex.vertis.baker.util.test.http

import play.api.libs.json.JsValue

abstract class JsonLookup {
  def shouldBe(expected: JsValue): Unit
}
