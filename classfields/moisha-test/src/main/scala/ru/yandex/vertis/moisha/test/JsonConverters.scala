package ru.yandex.vertis.moisha.test

import spray.json.{JsValue, RootJsonWriter}

trait JsonConverters {

  implicit class RichHasRootJsonWriter[A: RootJsonWriter](private val a: A) {

    def jsonKeys: Set[String] = jsonFields.toMap.keySet

    def jsonFields: Seq[(String, JsValue)] = {
      implicitly[RootJsonWriter[A]]
        .write(a)
        .asJsObject
        .fields
        // Call toSeq to workaround https://github.com/scalatest/scalatest/issues/1224
        .toSeq
    }
  }
}
