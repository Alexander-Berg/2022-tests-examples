package ru.yandex.vertis.telepony.vox

import org.joda.time.DateTime
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.model.{Phone, RedirectId, RefinedSource}

import scala.concurrent.duration._

class AppCustomDataJsonConversionSpec extends SpecBase {

  "AppCustomDataJsonConversion" should {

    "parse AppCustomData from json string" in {
      val currentDateTime = new DateTime()
      val currentMillis = currentDateTime.getMillis
      val expectedPhone = "+79111448065"
      val jsonString =
        s"""
          |{
          |  "events": [
          |     { "eventType": 1, "time": "$currentMillis" },
          |     { "eventType": 2, "time": "$currentMillis" },
          |     { "eventType": 3, "time": "$currentMillis" },
          |     { "eventType": 4, "time": "$currentMillis" },
          |     { "eventType": 4, "time": "$currentMillis", "code": 666 },
          |     { "eventType": 5, "time": "$currentMillis"},
          |     { "eventType": 6, "time": "$currentMillis"},
          |     { "eventType": 7, "time": "$currentMillis"},
          |     { "eventType": 8, "time": "$currentMillis"},
          |     { "eventType": 8, "time": "$currentMillis", "code": 111 },
          |     { "eventType": 9, "time": "$currentMillis" },
          |     { "eventType": 10, "time": "$currentMillis" },
          |     { "eventType": 11, "time": "$currentMillis" },
          |     { "eventType": 12, "time": "$currentMillis" },
          |     { "eventType": 13, "time": "$currentMillis" },
          |     { "eventType": 14, "time": "$currentMillis" },
          |     { "eventType": 99, "time": "$currentMillis" },
          |     { "eventType": 0, "time": "$currentMillis" },
          |     { "eventType": -1, "time": "$currentMillis" }
          |   ],
          | "sourcePhone": "$expectedPhone",
          | "callerId": "$expectedPhone",
          | "targetPhone": "$expectedPhone",
          | "sourceUsername": "sourcename",
          | "targetUsername": "targetname",
          | "talkDuration": "1000000",
          | "redirectId": "redirectId",
          | "recordUrl": "recordUrl"
          |}
          |""".stripMargin
      val customData = AppCustomDataJsonConversion.from(jsonString)
      customData.isSuccess shouldBe true
      customData.get shouldBe AppCustomData(
        events = Seq(
          AppCallEvent.StartAppScenario(currentDateTime),
          AppCallEvent.OutAppCallInit(currentDateTime),
          AppCallEvent.OutAppCallConnected(currentDateTime),
          AppCallEvent.UnknownAppCallEvent(currentDateTime),
          AppCallEvent.OutAppCallFailed(currentDateTime, 666),
          AppCallEvent.OutAppCallTimeout(currentDateTime),
          AppCallEvent.OutPhoneCallInit(currentDateTime),
          AppCallEvent.OutPhoneCallConnected(currentDateTime),
          AppCallEvent.UnknownAppCallEvent(currentDateTime),
          AppCallEvent.OutPhoneCallFailed(currentDateTime, 111),
          AppCallEvent.InCallDisconnected(currentDateTime),
          AppCallEvent.OutAppCallDisconnected(currentDateTime),
          AppCallEvent.OutPhoneCallDisconnected(currentDateTime),
          AppCallEvent.AppScenarioTerminated(currentDateTime),
          AppCallEvent.NoRedirectAppCall(currentDateTime),
          AppCallEvent.BlockedAppCall(currentDateTime),
          AppCallEvent.UnknownAppCallEvent(currentDateTime),
          AppCallEvent.UnknownAppCallEvent(currentDateTime),
          AppCallEvent.UnknownAppCallEvent(currentDateTime)
        ),
        sourcePhone = Some(RefinedSource(expectedPhone)),
        callerId = Some(Phone(expectedPhone)),
        targetPhone = Some(Phone(expectedPhone)),
        sourceVoxUsername = "sourcename",
        targetVoxUsername = Some("targetname"),
        talkDuration = 1000000.milliseconds,
        redirectId = RedirectId("redirectId"),
        recordUrl = Some("recordUrl"),
        uuid = None,
        payloadJson = None
      )
    }

    "print AppCustomData as json string" in {
      val currentDateTime = new DateTime()
      val currentMillis = currentDateTime.getMillis
      val expectedPhone = "+79111448065"
      val customData = AppCustomData(
        events = Seq(
          AppCallEvent.StartAppScenario(currentDateTime),
          AppCallEvent.OutAppCallInit(currentDateTime),
          AppCallEvent.OutAppCallConnected(currentDateTime),
          AppCallEvent.OutAppCallFailed(currentDateTime, 666),
          AppCallEvent.OutAppCallTimeout(currentDateTime),
          AppCallEvent.OutPhoneCallInit(currentDateTime),
          AppCallEvent.OutPhoneCallConnected(currentDateTime),
          AppCallEvent.OutPhoneCallFailed(currentDateTime, 111),
          AppCallEvent.InCallDisconnected(currentDateTime),
          AppCallEvent.OutAppCallDisconnected(currentDateTime),
          AppCallEvent.OutPhoneCallDisconnected(currentDateTime),
          AppCallEvent.AppScenarioTerminated(currentDateTime),
          AppCallEvent.NoRedirectAppCall(currentDateTime),
          AppCallEvent.BlockedAppCall(currentDateTime),
          AppCallEvent.UnknownAppCallEvent(currentDateTime)
        ),
        sourcePhone = Some(RefinedSource(expectedPhone)),
        callerId = Some(Phone(expectedPhone)),
        targetPhone = Some(Phone(expectedPhone)),
        sourceVoxUsername = "sourcename",
        targetVoxUsername = Some("targetname"),
        talkDuration = 1000000.milliseconds,
        redirectId = RedirectId("redirectId"),
        recordUrl = Some("recordUrl"),
        uuid = None,
        payloadJson = None
      )
      val expectedString =
        s"""
           |{
           |  "events": [
           |     { "eventType": 1, "time": $currentMillis },
           |     { "eventType": 2, "time": $currentMillis },
           |     { "eventType": 3, "time": $currentMillis },
           |     { "eventType": 4, "time": $currentMillis, "code": 666 },
           |     { "eventType": 5, "time": $currentMillis},
           |     { "eventType": 6, "time": $currentMillis},
           |     { "eventType": 7, "time": $currentMillis},
           |     { "eventType": 8, "time": $currentMillis, "code": 111 },
           |     { "eventType": 9, "time": $currentMillis },
           |     { "eventType": 10, "time": $currentMillis },
           |     { "eventType": 11, "time": $currentMillis },
           |     { "eventType": 12, "time": $currentMillis },
           |     { "eventType": 13, "time": $currentMillis },
           |     { "eventType": 14, "time": $currentMillis },
           |     { "eventType": -1, "time": $currentMillis }
           |   ],
           | "sourcePhone": "+79111448065",
           | "callerId": "+79111448065",
           | "targetPhone": "+79111448065",
           | "sourceUsername": "sourcename",
           | "targetUsername": "targetname",
           | "talkDuration": 1000000,
           | "redirectId": "redirectId",
           | "recordUrl": "recordUrl"
           |}
           |""".stripMargin.replaceAll("\\s", "")
      val jsonString = AppCustomDataJsonConversion.to(customData)
      jsonString shouldBe expectedString
    }

    "parse not full filled AppCustomData from json string" in {
      val jsonString =
        s"""
           |{
           | "events": [],
           | "sourceUsername": "sourcename",
           | "talkDuration": "1000000",
           | "redirectId": "redirectId"
           |}
           |""".stripMargin
      val customData = AppCustomDataJsonConversion.from(jsonString)
      customData.isSuccess shouldBe true
      customData.get shouldBe AppCustomData(
        events = Seq.empty,
        sourcePhone = None,
        callerId = None,
        targetPhone = None,
        sourceVoxUsername = "sourcename",
        targetVoxUsername = None,
        talkDuration = 1000000.milliseconds,
        redirectId = RedirectId("redirectId"),
        recordUrl = None,
        uuid = None,
        payloadJson = None
      )
    }

    "parse video info AppCustomData from json string" in {
      val jsonString =
        s"""
           |{
           | "events": [],
           | "sourceUsername": "sourcename",
           | "talkDuration": "1000000",
           | "redirectId": "redirectId",
           | "uuid": "someId123",
           | "payloadJson": "{description: 'Some JSON-like String'}"
           |}
           |""".stripMargin
      val customData = AppCustomDataJsonConversion.from(jsonString)
      customData.isSuccess shouldBe true
      customData.get shouldBe AppCustomData(
        events = Seq.empty,
        sourcePhone = None,
        callerId = None,
        targetPhone = None,
        sourceVoxUsername = "sourcename",
        targetVoxUsername = None,
        talkDuration = 1000000.milliseconds,
        redirectId = RedirectId("redirectId"),
        recordUrl = None,
        uuid = Some("someId123"),
        payloadJson = Some("{description: 'Some JSON-like String'}")
      )
    }

    "do isomorphic transformations (view -> event -> view)" in {
      val currentMillis = new DateTime().getMillis
      val expectedView = AppCustomDataJsonConversion.AppCallEventView(8, currentMillis, Some(666))
      val event = AppCustomDataJsonConversion.buildAppCallEvent(expectedView)
      val eventView = AppCustomDataJsonConversion.buildAppCallEventView(event)
      eventView shouldBe expectedView
    }

    "do isomorphic transformations (event -> view -> event)" in {
      val expectedEvent = AppCallEvent.OutPhoneCallFailed(new DateTime(), -100)
      val eventView = AppCustomDataJsonConversion.buildAppCallEventView(expectedEvent)
      val event = AppCustomDataJsonConversion.buildAppCallEvent(eventView)
      event shouldBe expectedEvent
    }
  }
}
