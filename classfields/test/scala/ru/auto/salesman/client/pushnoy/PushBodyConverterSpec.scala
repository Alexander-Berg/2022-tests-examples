package ru.auto.salesman.client.pushnoy

import org.scalacheck.Gen
import ru.auto.salesman.client.pushnoy.PushBodyConverter._
import ru.auto.salesman.test.BaseSpec
import spray.json.{JsNumber, JsString}

class PushBodyConverterSpec extends BaseSpec with PushnoyModelGenerator {

  "PushBodyConverter" should {
    "convert input parameters to body" in {
      forAll(pushTemplateV1Gen, AppVersionGenerator) { (template, appVersion) =>
        val pushBody = convertToPushBody(template, Some(appVersion))

        val apns = pushBody.repack.apns
        val aps = pushBody.repack.apns.aps
        val gcm = pushBody.repack.gcm
        val userInfo = pushBody.repack.apns.aps.userInfo
        val alert = pushBody.repack.apns.aps.alert
        val payload = pushBody.payload

        pushBody.appVersion.get shouldBe appVersion
        pushBody.event shouldBe template.event
        payload.url shouldBe template.deepLink
        payload.action shouldBe "deeplink"
        payload.body shouldBe template.body
        payload.pushName shouldBe template.pushName
        payload.title shouldBe template.title
        alert.body shouldBe template.body
        alert.title shouldBe template.title
        userInfo.url shouldBe template.deepLink
        userInfo.action shouldBe "deeplink"
        userInfo.pushName shouldBe template.pushName
        aps.sound shouldBe "default"
        val payloadContent =
          List("url", "action", "body", "push_name", "title")
        gcm.repackPayload shouldBe payloadContent
        apns.repackPayload shouldBe List[String]()
      }
    }

    "always include badge: 1 for iOS in push body" in {
      forAll(pushTemplateV1Gen, Gen.option(AppVersionGenerator)) {
        (template, appVersion) =>
          val pushBody = convertToPushBody(template, appVersion)
          pushBody.repack.apns.aps.badge shouldBe 1
      }
    }

    "always include content-available: 1 for iOS in push body" in {
      forAll(pushTemplateV1Gen, Gen.option(AppVersionGenerator)) {
        (template, appVersion) =>
          val pushBody = convertToPushBody(template, appVersion)
          pushBody.repack.apns.aps.alert.contentAvailable shouldBe 1
      }
    }

    "format Alert.body case class field as body json field" in {
      forAll(alertGen()) { alert =>
        alert.jsonFields should contain("body" -> JsString(alert.body.asString))
      }
    }

    "format Alert.title case class field as title json field" in {
      forAll(alertGen()) { alert =>
        alert.jsonFields should contain(
          "title" -> JsString(alert.title.asString)
        )
      }
    }

    "format Alert.contentAvailable case class field as content-available json field" in {
      forAll(alertGen()) { alert =>
        alert.jsonFields should contain(
          "content-available" -> JsNumber(alert.contentAvailable)
        )
      }
    }
  }
}
