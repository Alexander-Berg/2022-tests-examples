package ru.yandex.vertis.billing.howmuch.model.core

import billing.howmuch.model.Source.{Source => ProtoSource}
import common.zio.testkit.failsWith
import ru.yandex.vertis.billing.howmuch.model.core.Source.{
  BillingSync,
  EmptyRequestId,
  EmptySource,
  InvalidStartrekTicketId,
  ServiceRequest,
  StartrekTicket,
  UserRequest
}
import common.zio.ops.tracing.RequestId
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestEnvironment

object SourceSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] = suite("SourceSpec")(
    testM("build StartrekTicket(id) from proto") {
      assertM(Source.fromProto(ProtoSource.StartrekTicket("VSMONEY-2750")))(
        equalTo(StartrekTicket("VSMONEY-2750"))
      ).provideCustomLayer(RequestId.test(None))
    },
    testM("fail on invalid startrek ticket without hyphen") {
      assertM(Source.fromProto(ProtoSource.StartrekTicket("VSMONEY")).run)(
        failsWith[InvalidStartrekTicketId]
      ).provideCustomLayer(RequestId.test(None))
    },
    testM("fail on invalid startrek ticket with non-letter in queue key") {
      assertM(Source.fromProto(ProtoSource.StartrekTicket("VSM5ONEY-2750")).run)(
        failsWith[InvalidStartrekTicketId]
      ).provideCustomLayer(RequestId.test(None))
    },
    testM("fail on invalid startrek ticket with non-digit in ticket number") {
      assertM(Source.fromProto(ProtoSource.StartrekTicket("VSMONEY-27A50")).run)(
        failsWith[InvalidStartrekTicketId]
      ).provideCustomLayer(RequestId.test(None))
    },
    testM("fail on invalid startrek ticket with two hyphens") {
      assertM(Source.fromProto(ProtoSource.StartrekTicket("VSMONEY-2750-2751")).run)(
        failsWith[InvalidStartrekTicketId]
      ).provideCustomLayer(RequestId.test(None))
    },
    testM("build UserRequest(id) from proto") {
      assertM(Source.fromProto(ProtoSource.UserRequest(true)))(
        equalTo(UserRequest("my_test_id"))
      ).provideCustomLayer(RequestId.test(Some("my_test_id")))
    },
    testM("build BillingSync(id) from proto") {
      assertM(Source.fromProto(ProtoSource.BillingSync(true)))(
        equalTo(BillingSync("my_test_id"))
      ).provideCustomLayer(RequestId.test(Some("my_test_id")))
    },
    testM("build ServiceRequest(id) from proto") {
      assertM(Source.fromProto(ProtoSource.ServiceRequest("auction_auto_strategy")))(
        equalTo(ServiceRequest("auction_auto_strategy"))
      ).provideCustomLayer(RequestId.test(Some("my_test_id")))
    },
    testM("fail with EmptyRequestId on UserRequest if request id is empty") {
      assertM(Source.fromProto(ProtoSource.UserRequest(true)).run)(
        failsWith[EmptyRequestId.type]
      ).provideCustomLayer(RequestId.test(None))
    },
    testM("fail with EmptySource on UserRequest(false)") {
      assertM(Source.fromProto(ProtoSource.UserRequest(false)).run)(
        failsWith[EmptySource.type]
      ).provideCustomLayer(RequestId.test(Some("my_test_id")))
    },
    testM("fail with EmptySource on BillingSync(false)") {
      assertM(Source.fromProto(ProtoSource.BillingSync(false)).run)(
        failsWith[EmptySource.type]
      ).provideCustomLayer(RequestId.test(Some("my_test_id")))
    },
    testM("fail with EmptySource on ServiceRequest()") {
      assertM(Source.fromProto(ProtoSource.ServiceRequest("")).run)(
        failsWith[EmptySource.type]
      ).provideCustomLayer(RequestId.test(Some("my_test_id")))
    },
    testM("fail with EmptySource on Empty source") {
      assertM(Source.fromProto(ProtoSource.Empty).run)(
        failsWith[EmptySource.type]
      ).provideCustomLayer(RequestId.test(Some("my_test_id")))
    }
  )
}
