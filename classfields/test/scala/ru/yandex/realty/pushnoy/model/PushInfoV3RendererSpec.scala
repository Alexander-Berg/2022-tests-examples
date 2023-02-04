package ru.yandex.realty.pushnoy.model

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import play.api.libs.json.{JsValue, Json}
import realty.palma.PushTemplateOuterClass.PushTemplate
import ru.yandex.realty.SpecBase
import ru.yandex.realty.context.v2.palma.PushTemplatesProvider.prepareTemplates
import ru.yandex.realty.pushnoy.model.PalmaPushId.{MISSING_APARTMENT, PalmaId}

@RunWith(classOf[JUnitRunner])
class PushInfoV3RendererSpec extends SpecBase {

  val jsonWithMetrika: JsValue = Json.parse("""
      |{
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
      |     }
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

  val jsonWithoutMetrika: JsValue = Json.parse("""
      |{
      | "payload": {
      |   "action":"deeplink",
      |   "params": {
      |     "url":"deeplink",
      |     "body":"text",
      |     "title":"title",
      |     "deeplink":"deeplink"
      |   },
      |   "user_info": {
      |     "action":"deeplink",
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
      |     }
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

  val palmaPushInfoV3: PalmaPushInfoV3 = new PalmaPushInfoV3 {
    override def target: Targets.Value = Targets.Devices

    override def actionType: Option[PushActionType.Value] = Some(PushActionType.Deeplink)

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
    "Render palmaPushInfoV3 with metrikaId" in {
      PalmaPushRenderer.renderV3(palmaPushInfoV3, palmaPushMessage).asJson shouldEqual jsonWithMetrika
    }

    "Render palmaPushInfoV3 without metrikaId" in {
      PalmaPushRenderer
        .renderV3(palmaPushInfoV3, palmaPushMessage.copy(metrikaId = None))
        .asJson shouldEqual jsonWithoutMetrika
    }
  }
}
