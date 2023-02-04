package ru.yandex.vertis.billing.model_core

import org.joda.time.DateTime
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.model_core.TeleponyCallFact.CallResults

class CallFactHeaderTest extends AnyWordSpec with Matchers {

  val teleponyCallHeader = TeleponyCallFactHeader(
    timestamp = DateTime.parse("2021-12-22T12:00:00Z"),
    incoming = "123",
    redirect = Some(Phone("123", "234", "456")),
    internal = Phone("234", "123", "456"),
    redirectId = Some("555"),
    objectId = "111",
    recordId = Some("xxx"),
    result = CallResults.StopCaller,
    tag = Some("qqq"),
    callId = Some("telepony-call-id"),
    callbackOrderId = Some("ppp")
  )

  val oldTeleponyCallHeader = TeleponyCallFactHeader(
    timestamp = DateTime.parse("2019-12-22T12:00:00Z"),
    incoming = "123",
    redirect = Some(Phone("123", "234", "456")),
    internal = Phone("234", "123", "456"),
    redirectId = Some("555"),
    objectId = "111",
    recordId = Some("xxx"),
    result = CallResults.StopCaller,
    tag = Some("qqq"),
    callId = Some("telepony-call-id"),
    callbackOrderId = Some("ppp")
  )

  val metrikaCallHeader = MetrikaCallFactHeader(
    timestamp = DateTime.parse("2021-12-22T12:00:00Z"),
    incoming = "123",
    redirect = Some(Phone("123", "234", "456")),
    internal = Phone("234", "123", "456"),
    track = 982
  )

  "CallFactHeader" should {
    "have stable identity" in {
      teleponyCallHeader.identity shouldBe "telepony-call-id"
      oldTeleponyCallHeader.identity shouldBe "d3d13cd61684880e"
      metrikaCallHeader.identity shouldBe "3d4b1ca71456cf37"
    }
  }
}
