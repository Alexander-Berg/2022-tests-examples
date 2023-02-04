package ru.yandex.vertis.telepony.generator

import org.scalacheck.Gen
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.model.mtt.CallStatusEventTypes.CallStatusEventType
import ru.yandex.vertis.telepony.model.mtt.{CallStatusEventTypes, RichRoutingRequest, RoutingRequest, RoutingResponse, StatusRequest}
import ru.yandex.vertis.telepony.service.impl.mtt.Internals.JsonRpcRequest
import ru.yandex.vertis.telepony.settings.MttCallSettings

/**
  * @author neron
  */
object MttGenerator {

  val CallStatusEventTypeGen: Gen[CallStatusEventType] = Gen.oneOf(CallStatusEventTypes.values.toList)

  val RoutingRequestGen: Gen[RoutingRequest] = for {
    proxy <- PhoneGen
    caller <- RefinedSourceGen
    h323 <- H323ConfIdGen
  } yield RoutingRequest(proxy, caller, h323)

  val RichRoutingRequestGen: Gen[RichRoutingRequest] = for {
    sharedNumber <- SharedOperatorNumberGen
    request <- RoutingRequestGen
  } yield RichRoutingRequest(sharedNumber, request)

  val StatusRequestGen: Gen[StatusRequest] = for {
    h323 <- H323ConfIdGen
    caller <- RefinedSourceGen
    proxy <- PhoneGen
    target <- PhoneGen
    eventType <- CallStatusEventTypeGen
    eventCause <- Gen.option(Gen.choose(-100, 100))
    time <- DateTimeGen
  } yield StatusRequest(h323, caller, proxy, target, eventType, eventCause, time)

  def jsonRpcRequestGen[T](request: T): Gen[JsonRpcRequest[T]] =
    for {
      methodName <- ShortStr
      id <- ShortStr
    } yield JsonRpcRequest("2.0", methodName, request, id)

  val RoutingResponseGen: Gen[RoutingResponse] = for {
    target <- PhoneGen
    url <- ShortStr
    callerAudio <- Gen.option(ShortStr)
    calleeAudio <- Gen.option(ShortStr)
    domain <- Gen.option(DomainGen)
  } yield RoutingResponse(target, url, callerAudio, calleeAudio, domain)

  val CallSettingsGen: Gen[MttCallSettings] = for {
    callerAudio <- Gen.option(ShortStr)
    targetAudio <- Gen.option(ShortStr)
  } yield MttCallSettings(callerAudio, targetAudio)

}
