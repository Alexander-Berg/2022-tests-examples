package ru.yandex.realty.pushnoy.model

import org.scalatest.Matchers._
import play.api.libs.json.Json.{toJsFieldJsValueWrapper, JsValueWrapper}
import play.api.libs.json.JsValue

object PushTestUtils {

  def checkV4Push(
    action: PushActionType.Value,
    metrikaPushId: MetrikaPushId,
    url: Option[String],
    customData: Map[String, JsValueWrapper]
  )(push: => PushInfo): Unit = {
    val json = push.asJson
    (json \ "payload" \ "action").as[String] shouldBe action.toString

    (json \ "payload" \ "params" \ "push_id").as[String] shouldBe metrikaPushId.toString
    (json \ "payload" \ "params" \ "title").isDefined shouldBe true
    (json \ "payload" \ "params" \ "body").isDefined shouldBe true
    (json \ "payload" \ "params" \ "url").asOpt[String] shouldBe url
    (json \ "payload" \ "params" \ "deeplink").asOpt[String] shouldBe url

    (json \ "payload" \ "user_info" \ "action").as[String] shouldBe action.toString
    (json \ "payload" \ "user_info" \ "push_id").as[String] shouldBe metrikaPushId.toString
    (json \ "payload" \ "user_info" \ "data" \ "url").asOpt[String] shouldBe url
    (json \ "payload" \ "user_info" \ "data" \ "deeplink").asOpt[String] shouldBe url

    (json \ "repack" \ "apns" \ "aps" \ "alert" \ "title").isDefined shouldBe true

    for ((k, v) <- customData) {
      toJsFieldJsValueWrapper((json \ "payload" \ "params" \ k).as[JsValue]) shouldBe v
      toJsFieldJsValueWrapper((json \ "payload" \ "user_info" \ "data" \ k).as[JsValue]) shouldBe v
    }
  }

}
