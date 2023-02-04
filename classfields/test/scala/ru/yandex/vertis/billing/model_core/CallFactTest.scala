package ru.yandex.vertis.billing.model_core

import org.joda.time.DateTime
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.model_core.TeleponyCallFact.CallResults

import scala.concurrent.duration._

class CallFactTest extends AnyWordSpec with Matchers {

  val teleponyCall = TeleponyCallFact(
    timestamp = DateTime.parse("2021-12-22T12:00:00Z"),
    incoming = "123",
    redirect = Some(Phone("123", "234", "456")),
    internal = Phone("234", "123", "456"),
    waitDuration = 5.seconds,
    duration = 22.seconds,
    redirectId = Some("555"),
    objectId = "111",
    recordId = Some("xxx"),
    result = CallResults.StopCaller,
    tag = Some("qqq"),
    callId = Some("telepony-call-id"),
    callbackOrderId = None,
    createTime = Some(DateTime.parse("2021-11-22T12:00:00Z")),
    false
  )

  val teleponyCallback = teleponyCall.copy(redirect = None, callbackOrderId = Some("xxx"))

  val oldTeleponyCall = TeleponyCallFact(
    timestamp = DateTime.parse("2019-12-22T12:00:00Z"),
    incoming = "123",
    redirect = Some(Phone("123", "234", "456")),
    internal = Phone("234", "123", "456"),
    waitDuration = 5.seconds,
    duration = 22.seconds,
    redirectId = Some("555"),
    objectId = "111",
    recordId = Some("xxx"),
    result = CallResults.StopCaller,
    tag = Some("qqq"),
    callId = Some("telepony-call-id"),
    callbackOrderId = None,
    isModeration = false
  )

  // its impossible to create callback older than 22 feb 2020 (Fingerprint.CallFactIdVSBILLING3544DateStart)
  def oldTeleponyCallback = oldTeleponyCall.copy(redirect = None, callbackOrderId = Some("xxx"))

  val metrikaCall = MetrikaCallFact(
    timestamp = DateTime.parse("2021-12-22T12:00:00Z"),
    incoming = "123",
    redirect = Some(Phone("123", "234", "456")),
    internal = Phone("234", "123", "456"),
    waitDuration = 5.seconds,
    duration = 22.seconds,
    track = 982
  )

  "CallFact" should {
    "have stable identity" in {
      teleponyCall.id shouldBe "telepony-call-id"
      teleponyCallback.id shouldBe "telepony-call-id"
      oldTeleponyCall.id shouldBe "d3d13cd61684880e"
      an[IllegalArgumentException] should be thrownBy {
        oldTeleponyCallback.id
      }
      metrikaCall.id shouldBe "3d4b1ca71456cf37"
    }
  }
}
