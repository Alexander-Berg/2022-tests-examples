package ru.auto.api.testkit

import play.api.libs.json.JsValue

abstract class JsonLookup {
  def shouldBe(expected: JsValue): Unit
}
