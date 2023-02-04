package ru.yandex.vertis.telepony.generator

import org.scalacheck.Gen
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.model.beeline.CallStates.CallState
import ru.yandex.vertis.telepony.model.beeline.{CallStates, HostId, RoutingRequest, StatusRequest, SysId}
import ru.yandex.vertis.telepony.settings.BeelineCallSettings

/**
  * @author neron
  */
object BeelineGenerator {

  val CallStateGen: Gen[CallState] = Gen.oneOf(CallStates.values.toSeq)

  val CallSettingsGen: Gen[BeelineCallSettings] = for {
    recording <- BooleanGen
    callerAudio <- ShortStr
    targetAudio <- ShortStr
    silenceAudio <- ShortStr
  } yield BeelineCallSettings(recording, callerAudio, targetAudio, silenceAudio)

  val RoutingRequestGen: Gen[RoutingRequest] = for {
    scriptId <- ShortStr
    hostId <- ShortStr.map(HostId)
    sysId <- ShortStr.map(SysId)
    caller <- SourceGen.map(Some.apply)
    proxy <- PhoneGen
  } yield RoutingRequest(scriptId, hostId, sysId, caller, proxy)

  val StatusRequestGen: Gen[StatusRequest] = for {
    externalId <- ShortStr
    callerState <- CallStateGen
    calleeState <- CallStateGen
    reason <- Gen.choose(-100, 100)
    talkDuration <- CallTalkDurationGen
    additionalParameter <- ShortStr
    time <- DateTimeGen
  } yield StatusRequest(externalId, callerState, calleeState, reason, talkDuration, additionalParameter, time)

}
