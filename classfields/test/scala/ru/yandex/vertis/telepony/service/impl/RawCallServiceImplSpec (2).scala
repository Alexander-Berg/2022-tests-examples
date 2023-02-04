package ru.yandex.vertis.telepony.service.impl

import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.model.{CallResults, Phone}
import ru.yandex.vertis.telepony.model.RawCall.Origins
import ru.yandex.vertis.telepony.service.RawCallService.Actions.Ignored
import ru.yandex.vertis.telepony.service.RawCallService.{Actions, Matched, Result}
import ru.yandex.vertis.telepony.service.{ActualCallService, BlockedCallService, RedirectServiceV2, UnmatchedCallService}
import ru.yandex.vertis.telepony.settings.CallbackSettings
import ru.yandex.vertis.telepony.settings.CallbackSettings.AfterHoursSettings
import ru.yandex.vertis.telepony.util.{AutomatedContext, RequestContext, Threads}

import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * @author neron
  */
class RawCallServiceImplSpec extends SpecBase with MockitoSupport {

  import Threads.lightWeightTasksEc
  implicit val rc: RequestContext = AutomatedContext(id = "RawCallServiceSpec")

  trait TestEnv {
    val callService = mock[ActualCallService]
    val blockedCallService = mock[BlockedCallService]
    val unmatchedCallService = mock[UnmatchedCallService]
    val redirectService = mock[RedirectServiceV2]
    val callbackSettings = mock[CallbackSettings]
    val afterHoursSettings = mock[AfterHoursSettings]

    val proxyPhone = Phone("+79998887766")
    val noProxyPhone = Phone("+79998887788")

    val service = new RawCallServiceImpl(
      callService,
      blockedCallService,
      unmatchedCallService,
      redirectService,
      callbackSettings
    )
  }

  "RawCallService" should {

    "ignore call if it exist in blocked call history" in new TestEnv {
      val bannedCall = BannedCallGen.next
      when(redirectService.getOnTime(?, ?)(?)).thenReturn(Future.successful(Some(bannedCall.redirect)))
      when(blockedCallService.getCallOpt(?)(?)).thenReturn(Future.successful(Some(bannedCall)))
      when(callService.getCallOpt(?)(?)).thenReturn(Future.successful(None))
      when(callbackSettings.afterHoursCallbackProxy).thenReturn(proxyPhone)
      when(callbackSettings.afterHoursCallbackSettings).thenReturn(afterHoursSettings)

      val rawCall = rawCallGen(callResult = Some(CallResults.Success)).next
        .copy(
          startTime = bannedCall.time
        )

      val result = service.process(rawCall).futureValue

      result should ===(Result(Matched(bannedCall), Ignored))
    }

    "ignore blocked call if it exist in call history" in new TestEnv {
      val existing = CallV2Gen.next
      when(redirectService.getOnTime(?, ?)(?)).thenReturn(Future.successful(Some(existing.redirect)))
      when(blockedCallService.getCallOpt(?)(?)).thenReturn(Future.successful(None))
      when(callService.getCallOpt(?)(?)).thenReturn(Future.successful(Some(existing)))
      when(callbackSettings.afterHoursCallbackProxy).thenReturn(proxyPhone)
      when(callbackSettings.afterHoursCallbackSettings).thenReturn(afterHoursSettings)

      val rawCall = RawCallGen.next
        .copy(
          callResult = CallResults.Blocked,
          startTime = existing.time
        )

      val result = service.process(rawCall).futureValue

      result should ===(Result(Matched(existing), Ignored))
    }

    "update call with unknown call-result" in new TestEnv {
      val existing = CallV2Gen.next.copy(callResult = CallResults.Unknown)
      val updated = CallV2Gen.next
      val rawCall = rawCallGen(callResult = Some(CallResults.Success)).next
        .copy(
          startTime = existing.time
        )
      when(redirectService.getOnTime(?, ?)(?)).thenReturn(Future.successful(Some(existing.redirect)))
      when(blockedCallService.getCallOpt(?)(?)).thenReturn(Future.successful(None))
      when(callService.getCallOpt(?)(?)).thenReturn(Future.successful(Some(existing)))
      when(callService.update(?, ?)(?)).thenReturn(Future.successful(updated))
      when(callbackSettings.afterHoursCallbackProxy).thenReturn(proxyPhone)
      when(callbackSettings.afterHoursCallbackSettings).thenReturn(afterHoursSettings)
      val result = service.process(rawCall).futureValue
      result should ===(Result(Matched(updated), Actions.Updated))
    }

    "update call if new call is online call" in new TestEnv {
      val existing = CallV2Gen.next
      val updated = CallV2Gen.next
      val rawCall = rawCallGen(callResult = Some(CallResults.Success)).next
        .copy(
          startTime = existing.time,
          origin = Origins.Online
        )
      when(redirectService.getOnTime(?, ?)(?)).thenReturn(Future.successful(Some(existing.redirect)))
      when(blockedCallService.getCallOpt(?)(?)).thenReturn(Future.successful(None))
      when(callService.getCallOpt(?)(?)).thenReturn(Future.successful(Some(existing)))
      when(callService.update(?, ?)(?)).thenReturn(Future.successful(updated))
      when(callbackSettings.afterHoursCallbackProxy).thenReturn(proxyPhone)
      when(callbackSettings.afterHoursCallbackSettings).thenReturn(afterHoursSettings)
      val result = service.process(rawCall).futureValue
      result should ===(Result(Matched(updated), Actions.Updated))
    }

    "not update call if processed call has unknown call result" in new TestEnv {
      val existing = CallV2Gen.next
      val updated = CallV2Gen.next
      val rawCall = RawCallGen.next.copy(
        callResult = CallResults.Unknown,
        startTime = existing.time,
        talkDuration = existing.talkDuration.plus(1.second),
        duration = existing.duration.plus(1.second)
      )
      when(redirectService.getOnTime(?, ?)(?)).thenReturn(Future.successful(Some(existing.redirect)))
      when(blockedCallService.getCallOpt(?)(?)).thenReturn(Future.successful(None))
      when(callService.getCallOpt(?)(?)).thenReturn(Future.successful(Some(existing)))
      when(callService.update(?, ?)(?)).thenReturn(Future.successful(updated))
      when(callbackSettings.afterHoursCallbackProxy).thenReturn(proxyPhone)
      when(callbackSettings.afterHoursCallbackSettings).thenReturn(afterHoursSettings)
      val result = service.process(rawCall).futureValue
      result should ===(Result(Matched(existing), Actions.Ignored))
    }

    "convert success call status if after hours callback" in new TestEnv {
      val rawCall = RawCallGen.next.copy(
        callResult = CallResults.Success,
        target = Some(proxyPhone)
      )
      when(callbackSettings.afterHoursCallbackProxy).thenReturn(proxyPhone)
      when(callbackSettings.afterHoursCallbackSettings).thenReturn(afterHoursSettings)

      val result = service.fixResultIfAfterHoursCallback(rawCall).futureValue
      result.callResult shouldBe CallResults.AfterHoursCallback
    }

    "not convert non-success call status if after hours callback" in new TestEnv {
      val rawCall = RawCallGen.next.copy(
        callResult = CallResults.BusyCallee,
        target = Some(proxyPhone)
      )
      when(callbackSettings.afterHoursCallbackProxy).thenReturn(proxyPhone)
      when(callbackSettings.afterHoursCallbackSettings).thenReturn(afterHoursSettings)

      val result = service.fixResultIfAfterHoursCallback(rawCall).futureValue
      result.callResult shouldBe CallResults.BusyCallee
      result shouldBe rawCall
    }

    "not convert success call status if not after hours callback" in new TestEnv {
      val rawCall = RawCallGen.next
        .copy(
          target = Some(noProxyPhone),
          callResult = CallResults.Success
        )
      when(callbackSettings.afterHoursCallbackProxy).thenReturn(proxyPhone)
      when(callbackSettings.afterHoursCallbackSettings).thenReturn(afterHoursSettings)

      val result = service.fixResultIfAfterHoursCallback(rawCall).futureValue
      result.callResult shouldBe CallResults.Success
      result shouldBe rawCall
    }
  }

}
