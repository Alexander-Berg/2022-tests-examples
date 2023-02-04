package ru.yandex.vertis.telepony.journal

import org.joda.time.DateTime
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.journal.serializer.VoxAppCallSerDe
import ru.yandex.vertis.telepony.model.{Phone, RedirectId, RefinedSource}
import ru.yandex.vertis.telepony.model.proto.{AppCallEvent => protoAppCallEvent, VoxAppCallEvent}
import ru.yandex.vertis.telepony.util.ProtoUtils
import ru.yandex.vertis.telepony.vox.{AppCallEvent, AppCustomData, VoxAppCall}

import scala.concurrent.duration._

class VoxAppCallSerDeSpec extends SpecBase with ProtoUtils {

  private val protoBuilder = VoxAppCallEvent.newBuilder()
  protoBuilder.setSessionId("session")

  private val currentDateTime = new DateTime()
  private val currentMillis = currentDateTime.getMillis

  private val expectedPhone = "+79111448065"
  private val redirectId = "redirectId"

  private val rawCustomData =
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
       | "sourcePhone": "$expectedPhone",
       | "callerId": "$expectedPhone",
       | "targetPhone": "$expectedPhone",
       | "sourceUsername": "sourcename",
       | "targetUsername": "targetname",
       | "talkDuration": 1000000,
       | "redirectId": "$redirectId",
       | "recordUrl": "recordUrl"
       |}
       |""".stripMargin.replaceAll("\\s", "")
  protoBuilder.setRawCustomData(rawCustomData)
  protoBuilder.setRedirectId(redirectId)

  private val currentProtoEventBuilder = protoBuilder.getCurrentEventBuilder
  currentProtoEventBuilder.setEventType(protoAppCallEvent.EventType.OUT_PHONE_CALL_FAILED_EVENT)
  currentProtoEventBuilder.setTime(toTimestamp(currentDateTime))
  currentProtoEventBuilder.getCodeBuilder.setValue(555)

  private val proto = protoBuilder.build()

  private val voxAppCall = VoxAppCall(
    currentEvent = AppCallEvent.OutPhoneCallFailed(currentDateTime, 555),
    appCustomData = Some(
      AppCustomData(
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
    ),
    sessionId = "session",
    redirectId = RedirectId("redirectId")
  )

  "VoxAppCallSerDe" should {
    "parse proto view" in {
      val event = VoxAppCallSerDe.fromProto(proto)
      event shouldBe voxAppCall
    }

    "build proto view" in {
      val protoResult = VoxAppCallSerDe.toProto(voxAppCall)
      protoResult shouldBe proto
    }

    "do isomorphic transformations" in {
      val bytes = VoxAppCallSerDe.serialize(voxAppCall)
      val result = VoxAppCallSerDe.deserialize(bytes)
      result.isSuccess shouldBe true
      result.get shouldBe voxAppCall
    }
  }
}
