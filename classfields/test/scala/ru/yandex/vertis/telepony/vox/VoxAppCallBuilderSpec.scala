package ru.yandex.vertis.telepony.vox

import org.joda.time.DateTime
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.model.CallResults._
import ru.yandex.vertis.telepony.model.{RawAppCall, RawCallOrigins, RedirectId}
import ru.yandex.vertis.telepony.vox.AppCallEvent._
import ru.yandex.vertis.telepony.vox.VoxAppCallBuilder.AppCallResults

import scala.concurrent.duration._

class VoxAppCallBuilderSpec extends SpecBase {

  private val time: DateTime = DateTime.now()

  private val outAppCallInit = OutAppCallInit(time)
  private val outAppCallConnected = OutAppCallConnected(time)
  private val outAppCallTimeout = OutAppCallTimeout(time)
  private val outAppCallFailed = OutAppCallFailed(time, 486)
  private val outPhoneCallInit = OutPhoneCallInit(time)
  private val outPhoneCallConnected = OutPhoneCallConnected(time)
  private val outPhoneCallFailed = OutPhoneCallFailed(time, 487)
  private val inCallDisconnected = InCallDisconnected(time)
  private val outAppCallDisconnected = OutAppCallDisconnected(time)
  private val outPhoneCallDisconnected = OutPhoneCallDisconnected(time)
  private val appScenarioTerminated = AppScenarioTerminated(time)
  private val noRedirect = NoRedirectAppCall(time)
  private val startAppScenario = StartAppScenario(time)
  private val blockedAppCall = BlockedAppCall(time)

  // See picture from https://st.yandex-team.ru/TELEPONY-1600#5fb69eccdd6774076443d175
  val callResultsMap = Map(
    Seq()
      -> AppCallResults(Unknown, None),
    Seq(inCallDisconnected)
      -> AppCallResults(StopCaller, None),
    Seq(outAppCallInit, inCallDisconnected, appScenarioTerminated)
      -> AppCallResults(StopCaller, None),
    Seq(outAppCallInit, outAppCallConnected, inCallDisconnected)
      -> AppCallResults(Success, None),
    Seq(outAppCallInit, outAppCallConnected, outAppCallDisconnected, appScenarioTerminated)
      -> AppCallResults(Success, None),
    Seq(outAppCallInit, OutAppCallFailed(time, 486), inCallDisconnected)
      -> AppCallResults(BusyCallee, None),
    Seq(startAppScenario, blockedAppCall, appScenarioTerminated)
      -> AppCallResults(Blocked, None),
    Seq(outAppCallInit, OutAppCallFailed(time, 487), inCallDisconnected, appScenarioTerminated)
      -> AppCallResults(Error, Some(StopCaller)),
    Seq(outAppCallInit, outAppCallTimeout, inCallDisconnected)
      -> AppCallResults(NoAnswer, Some(StopCaller)),
    Seq(outAppCallInit, outAppCallFailed, outPhoneCallInit, inCallDisconnected, appScenarioTerminated)
      -> AppCallResults(BusyCallee, Some(StopCaller)),
    Seq(outAppCallInit, outAppCallTimeout, outPhoneCallInit, inCallDisconnected, appScenarioTerminated)
      -> AppCallResults(NoAnswer, Some(StopCaller)),
    Seq(
      outAppCallInit,
      outAppCallFailed,
      outPhoneCallInit,
      outPhoneCallConnected,
      inCallDisconnected,
      appScenarioTerminated
    )
      -> AppCallResults(BusyCallee, Some(Success)),
    Seq(outAppCallInit, outAppCallTimeout, outPhoneCallInit, outPhoneCallConnected, inCallDisconnected)
      -> AppCallResults(NoAnswer, Some(Success)),
    Seq(
      outAppCallInit,
      outAppCallFailed,
      outPhoneCallInit,
      outPhoneCallConnected,
      outPhoneCallDisconnected,
      appScenarioTerminated
    )
      -> AppCallResults(BusyCallee, Some(Success)),
    Seq(
      noRedirect,
      outPhoneCallInit,
      outPhoneCallConnected,
      outPhoneCallDisconnected,
      appScenarioTerminated
    )
      -> AppCallResults(NoRedirect, Some(Success)),
    Seq(
      outAppCallInit,
      outAppCallTimeout,
      outPhoneCallInit,
      outPhoneCallConnected,
      outPhoneCallDisconnected,
      appScenarioTerminated
    )
      -> AppCallResults(NoAnswer, Some(Success)),
    Seq(outAppCallInit, outAppCallFailed, outPhoneCallInit, outPhoneCallFailed, appScenarioTerminated)
      -> AppCallResults(BusyCallee, Some(Error)),
    Seq(outAppCallInit, outAppCallTimeout, outPhoneCallInit, outPhoneCallFailed, appScenarioTerminated)
      -> AppCallResults(NoAnswer, Some(Error)),
    Seq(
      outAppCallInit,
      outAppCallFailed,
      outPhoneCallInit,
      outPhoneCallFailed,
      outPhoneCallDisconnected,
      appScenarioTerminated
    )
      -> AppCallResults(BusyCallee, Some(Error)),
    Seq(
      outAppCallInit,
      outAppCallTimeout,
      outPhoneCallInit,
      outPhoneCallFailed,
      outPhoneCallDisconnected,
      appScenarioTerminated
    )
      -> AppCallResults(NoAnswer, Some(Error)),
    Seq(
      noRedirect,
      outPhoneCallInit,
      outPhoneCallFailed,
      outPhoneCallDisconnected,
      appScenarioTerminated
    )
      -> AppCallResults(NoRedirect, Some(Error)),
    // для этого случая смотреть https://st.yandex-team.ru/TELEPONY-1882
    Seq(outAppCallInit, outAppCallDisconnected) -> AppCallResults(BusyCallee, None)
  )

  "VoxAppCallBuilder" should {
    "get call result by events sequence" in {
      callResultsMap.foreach {
        case (events, callResults) =>
          val targetPhone = PhoneGen.next
          VoxAppCallBuilder.getCallResults(events.toSet, Some(targetPhone)) shouldBe callResults
      }
    }
    "when targetPhone is not defined result should be NoRedirect if it isn't Blocked" in {
      callResultsMap.foreach {
        case (events, _) =>
          if (events.toSet[AppCallEvent].collectFirst { case e: BlockedAppCall => e }.isEmpty) {
            VoxAppCallBuilder.getCallResults(events.toSet, None) shouldBe AppCallResults(NoRedirect, None)
          } else {
            VoxAppCallBuilder.getCallResults(events.toSet, None) shouldBe AppCallResults(Blocked, None)
          }

      }
    }

    "build raw app call from custom_data" in {
      val externalId: String = ShortStr.next
      val startTime: DateTime = DateTimeGen.next
      val duration: FiniteDuration = CallDurationGen.next
      val recordUrl: Option[String] = Some(ShortStr.next)

      val sourceVoxUsername = ShortStr.next
      val sourcePhone = Some(RefinedSourceGen.next)
      val talkDuration = CallDurationGen.next
      val targetVoxUsername = ShortStr.next
      val callerId = Some(PhoneGen.next)
      val targetPhone = PhoneGen.next
      val redirectId = RedirectId(ShortStr.next)
      val time = DateTime.now().getMillis

      val uuid = Some(ShortStr.next)
      val payloadJson = Some("{description: 'Some JSON-like String'}")
      val rawCustomData: String =
        s"""
          {
            "sourceUsername": "$sourceVoxUsername",
            "sourcePhone": "${sourcePhone.get.callerId.value}",
            "talkDuration": ${talkDuration.toMillis},
            "targetUsername": "$targetVoxUsername",
            "callerId": "${callerId.get.value}",
            "targetPhone": "${targetPhone.value}",
            "redirectId": "${redirectId.value}",
            "uuid": "${uuid.value}",
            "payloadJson": "${payloadJson.value}",
            "events": [
              { "eventType": 1, "time": $time },
              { "eventType": 2, "time": $time },
              { "eventType": 4, "time": $time, "code": 403 },
              { "eventType": 6, "time": $time },
              { "eventType": 8, "time": $time, "code": 404 }
            ]
          }
          """

      val actualRawAppCall =
        VoxAppCallBuilder.buildRawAppCall(externalId, startTime, duration, recordUrl, rawCustomData).get

      val expectedRawAppCall = RawAppCall(
        externalId = externalId,
        sourceVoxUsername = sourceVoxUsername,
        sourcePhone = sourcePhone,
        startTime = startTime,
        duration = duration,
        talkDuration = talkDuration,
        recordUrl = recordUrl,
        origin = RawCallOrigins.Offline,
        targetVoxUsername = Some(targetVoxUsername),
        callerId = callerId,
        targetPhone = Some(targetPhone),
        appCallResult = UnavailableCallee,
        phoneCallResult = Some(InvalidCallee),
        redirectId = redirectId,
        uuid = uuid,
        payloadJson = payloadJson
      )

      actualRawAppCall shouldEqual expectedRawAppCall
    }

    "build raw app call from custom_data without targetPhone" in {
      val externalId: String = ShortStr.next
      val startTime: DateTime = DateTimeGen.next
      val duration: FiniteDuration = CallDurationGen.next
      val recordUrl: Option[String] = Some(ShortStr.next)

      val sourceVoxUsername = ShortStr.next
      val sourcePhone = Some(RefinedSourceGen.next)
      val talkDuration = CallDurationGen.next
      val targetVoxUsername = ShortStr.next
      val callerId = Some(PhoneGen.next)
      val redirectId = RedirectId(ShortStr.next)
      val time = DateTime.now().getMillis
      val rawCustomData: String =
        s"""
          {
            "sourceUsername": "$sourceVoxUsername",
            "sourcePhone": "${sourcePhone.get.callerId.value}",
            "talkDuration": ${talkDuration.toMillis},
            "targetUsername": "$targetVoxUsername",
            "callerId": "${callerId.get.value}",
            "redirectId": "${redirectId.value}",
            "events": [
              { "eventType": 1, "time": $time },
              { "eventType": 2, "time": $time },
              { "eventType": 4, "time": $time, "code": 403 },
              { "eventType": 6, "time": $time },
              { "eventType": 8, "time": $time, "code": 404 }
            ]
          }
          """

      val actualRawAppCall =
        VoxAppCallBuilder.buildRawAppCall(externalId, startTime, duration, recordUrl, rawCustomData).get

      val expectedRawAppCall = RawAppCall(
        externalId = externalId,
        sourceVoxUsername = sourceVoxUsername,
        sourcePhone = sourcePhone,
        startTime = startTime,
        duration = duration,
        talkDuration = talkDuration,
        recordUrl = recordUrl,
        origin = RawCallOrigins.Offline,
        targetVoxUsername = Some(targetVoxUsername),
        callerId = callerId,
        targetPhone = None,
        appCallResult = NoRedirect,
        phoneCallResult = None,
        redirectId = redirectId,
        uuid = None,
        payloadJson = None
      )

      actualRawAppCall shouldEqual expectedRawAppCall
    }

    "collect raw app call from online events" in {
      val externalId: String = ShortStr.next
      val startTime: DateTime = outAppCallInit.time
      val duration: FiniteDuration = CallDurationGen.next
      val recordUrl: Option[String] = Some(ShortStr.next)

      val sourceVoxUsername = ShortStr.next
      val sourcePhone = Some(RefinedSourceGen.next)
      val talkDuration = CallDurationGen.next
      val targetVoxUsername = Some(ShortStr.next)
      val callerId = Some(PhoneGen.next)
      val targetPhone = PhoneGen.next
      val redirectId = RedirectId(ShortStr.next)

      val uuid = Some(ShortStr.next)
      val payloadJson = Some(ShortStr.next)

      val modifiedAppScenarioTerminated =
        appScenarioTerminated.copy(time = startTime.plusMillis(duration.toMillis.toInt))

      val events: Map[AppCallEvent, Seq[AppCallEvent]] = Map(
        outAppCallInit -> Seq(outAppCallInit),
        outAppCallTimeout -> Seq(outAppCallInit, outAppCallTimeout),
        outPhoneCallInit -> Seq(outAppCallInit, outAppCallTimeout, outPhoneCallInit),
        outPhoneCallConnected -> Seq(outAppCallInit, outAppCallTimeout, outPhoneCallInit, outPhoneCallConnected),
        inCallDisconnected -> Seq(
          outAppCallInit,
          outAppCallTimeout,
          outPhoneCallInit,
          outPhoneCallConnected,
          inCallDisconnected
        ),
        modifiedAppScenarioTerminated -> Seq(
          outAppCallInit,
          outAppCallTimeout,
          outPhoneCallInit,
          outPhoneCallConnected,
          inCallDisconnected,
          modifiedAppScenarioTerminated
        )
      )

      def baseData(events: Seq[AppCallEvent]): AppCustomData = AppCustomData(
        events = events,
        sourcePhone = sourcePhone,
        callerId = None,
        targetPhone = Some(targetPhone),
        sourceVoxUsername = sourceVoxUsername,
        targetVoxUsername = targetVoxUsername,
        talkDuration = 0.seconds,
        redirectId = redirectId,
        recordUrl = None,
        uuid = uuid,
        payloadJson = payloadJson
      )

      val appEvents = Seq(
        VoxAppCall(outAppCallInit, Some(baseData(events(outAppCallInit))), externalId, redirectId),
        VoxAppCall(outAppCallTimeout, Some(baseData(events(outAppCallTimeout))), externalId, redirectId),
        VoxAppCall(
          outPhoneCallInit,
          Some(baseData(events(outPhoneCallInit)).copy(callerId = callerId)),
          externalId,
          redirectId
        ),
        VoxAppCall(outPhoneCallConnected, Some(baseData(events(outPhoneCallConnected))), externalId, redirectId),
        VoxAppCall(inCallDisconnected, Some(baseData(events(inCallDisconnected)).copy()), externalId, redirectId),
        VoxAppCall(
          modifiedAppScenarioTerminated,
          Some(
            baseData(events(modifiedAppScenarioTerminated)).copy(recordUrl = recordUrl, talkDuration = talkDuration)
          ),
          externalId,
          redirectId
        )
      )

      val collectedCallRes = VoxAppCallBuilder.collectRawAppCallFromOnlineEvents(appEvents)
      collectedCallRes.isSuccess shouldBe true
      val collectedCall = collectedCallRes.get
      val expectedCall = RawAppCall(
        externalId = externalId,
        sourceVoxUsername = sourceVoxUsername,
        sourcePhone = sourcePhone,
        startTime = startTime,
        duration = duration,
        talkDuration = talkDuration,
        recordUrl = recordUrl,
        origin = RawCallOrigins.Online,
        targetVoxUsername = targetVoxUsername,
        callerId = callerId,
        targetPhone = Some(targetPhone),
        appCallResult = NoAnswer,
        phoneCallResult = Some(Success),
        redirectId = redirectId,
        uuid = uuid,
        payloadJson = payloadJson
      )
      collectedCall shouldBe expectedCall
    }
  }
}
