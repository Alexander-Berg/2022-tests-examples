package ru.yandex.realty.pushnoy.model

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import play.api.libs.json.{JsObject, JsValue, Json}
import realty.palma.PushTemplateOuterClass.PushTemplate
import ru.yandex.realty.SpecBase
import ru.yandex.realty.context.v2.palma.PushTemplatesProvider.prepareTemplates
import ru.yandex.realty.pushnoy.model.PalmaPushId.{MISSING_APARTMENT, PalmaId}

import scala.util.{Failure, Success, Try}

@RunWith(classOf[JUnitRunner])
class DevicePushInfoV4RendererSpec extends SpecBase {

  val jsonWithMetrika: JsValue = Json.parse("""
    |{
    | "event":"deeplink",
    | "payload": {
    |   "action":"deeplink",
    |   "params": {
    |     "url":"deeplink",
    |     "push_id":"EGRN_MISSING_APARTMENT_NOTIFICATION",
    |     "body":"text",
    |     "title":"title",
    |     "deeplink":"deeplink"
    |   },
    |   "user_info": {
    |     "action":"deeplink",
    |     "push_id":"EGRN_MISSING_APARTMENT_NOTIFICATION",
    |     "data": {
    |       "deeplink":"deeplink",
    |       "url":"deeplink"
    |     }
    |   }
    | },
    | "repack": {
    |   "apns": {
    |     "aps": {
    |       "alert": {
    |         "title":"title",
    |         "body":"text"
    |       },
    |       "badge":0,
    |       "sound":"default",
    |       "mutable-content":1
    |     },
    |     "repack_payload":["user_info"]
    |   },
    |   "gcm": {
    |     "repack_payload":["action","params"]
    |   }
    | },
    | "event":"deeplink",
    | "app_version": {
    |   "ios":"0.0.0",
    |   "android":"0.0.0",
    |   "compare":"gte"
    | }
    |}""".stripMargin)

  val palmaDevicePushInfoV4: PalmaDevicePushInfoV4 = new PalmaDevicePushInfoV4 {
    override def actionType: PushActionType.Value = PushActionType.Deeplink

    override def getCustomData: JsObject = JsObject.empty

    override def pushType: PalmaId = MISSING_APARTMENT
  }

  val palmaPushMessage: PalmaPushMessage = prepareTemplates(
    Seq(
      PushTemplate
        .newBuilder()
        .setPushId("MISSING_APARTMENT")
        .setIosMinVersion("0.0.0")
        .setAndroidMinVersion("0.0.0")
        .setMetrikaId("EGRN_MISSING_APARTMENT_NOTIFICATION")
        .setPushInfoId("ergn_missing_apartment_notification")
        .setTitle("title")
        .setText("text")
        .setDeeplink("deeplink")
        .build()
    )
  ).head

  PalmaPushRenderer.getClass.getSimpleName should {
    "Render devicePushInfoV4" in {
      PalmaPushRenderer.renderV4(palmaDevicePushInfoV4, palmaPushMessage).asJson shouldEqual jsonWithMetrika
    }
    "Render devicePushInfoV4 without metrikaId" in {
      Try(PalmaPushRenderer.renderV4(palmaDevicePushInfoV4, palmaPushMessage.copy(metrikaId = None)).asJson) match {
        case Success(_) => fail()
        case Failure(_) => succeed
      }
    }
  }
}
