package ru.yandex.realty.notifier.stage.queue.processor.rent

import akka.http.scaladsl.model.StatusCodes
import org.joda.time.{DateTime, LocalDate}
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.time.{Millis, Minutes, Span}
import ru.yandex.realty.{AsyncSpecBase, SpecBase}
import ru.yandex.realty.api.ProtoResponse.ErrorResponse
import ru.yandex.realty.clients.rent.{RentClient, RentModerationClient}
import ru.yandex.realty.errors.ProtoErrorResponseException
import ru.yandex.realty.model.SortDirection.SortDirection
import ru.yandex.realty.model.util.Page
import ru.yandex.realty.notifier.manager.{PushnoyManager, SmsSenderManager}
import ru.yandex.realty.notifier.model.enums.EventType
import ru.yandex.realty.notifier.model.{NotificationEvent, NotificationTarget, RentUserTarget}
import ru.yandex.realty.notifier.model.enums.EventType.EventType
import ru.yandex.realty.notifier.model.push.{
  FirstRentPaymentPush,
  RentPaymentOverduePush,
  RentPaymentSoonPush,
  RentPaymentTodayPush
}
import ru.yandex.realty.notifier.proto.model.payload.{EventPayload, RentPaymentPayload}
import ru.yandex.realty.notifier.stage.queue.processor.JustSendNotificationProcessor2.{ComeBackAfter, Ignore, Sent}
import ru.yandex.realty.pushnoy.model.{PalmaPushInfo, Targets}
import ru.yandex.realty.rent.proto.api.contract.RentContract
import ru.yandex.realty.rent.proto.api.payment.PaymentStatusNamespace.PaymentStatus
import ru.yandex.realty.rent.proto.api.payment.Payment.ContractBriefInfo
import ru.yandex.realty.rent.proto.api.payment.Payment.TenantRentPaymentPayload
import ru.yandex.realty.rent.proto.api.payment.{GetPaymentResponse, GetPaymentsResponse, Payment}
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.util.protobuf.ProtobufFormats
import ru.yandex.vertis.util.time.DateTimeUtil

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class TenantPaymentNotificationProcessorSpec extends SpecBase with AsyncSpecBase with ProtobufFormats {

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(1, Minutes), interval = Span(50, Millis))

  "TenantPaymentNotificationProcessor" should {
    "ignore when payment does not exist" in new Wiring with Data {
      mockGetPaymentNotFound()

      notificationProcessor()
        .processSingleEvent2(event(), target)
        .futureValue shouldEqual Ignore
    }

    "ignore payment under guarantee" in new Wiring with Data {
      mockGetPayment(paymentResponse(PaymentStatus.PAID_OUT_UNDER_GUARANTEE))

      notificationProcessor()
        .processSingleEvent2(event(), target)
        .futureValue shouldEqual Ignore
    }

    "reschedule when payment in future status" in new Wiring with Data {
      mockGetPayment(paymentResponse(PaymentStatus.FUTURE_PAYMENT))

      notificationProcessor()
        .processSingleEvent2(event(), target)
        .futureValue
        .isInstanceOf[ComeBackAfter] shouldBe true
    }

    "ignore when payment is paid" in {
      Seq(PaymentStatus.PAID_BY_TENANT, PaymentStatus.PAID_TO_OWNER).foreach { paidStatus =>
        new Wiring with Data {
          mockGetPayment(paymentResponse(paidStatus))

          notificationProcessor()
            .processSingleEvent2(event(), target)
            .futureValue shouldEqual Ignore
        }
      }
    }

    "send push when payment is new and event type is first rent payment" in new Wiring with Data {
      mockGetPayment(paymentResponse(PaymentStatus.NEW))
      mockGetPayments(singlePaymentsResponse())
      mockPushnoySuccess(firstPush)

      notificationProcessor(EventType.FirstRentPayment)
        .processSingleEvent2(event(EventType.FirstRentPayment), target)
        .futureValue shouldEqual Sent
    }

    "reschedule when payment is new and expected date is in the future" in new Wiring with Data {
      Seq(
        EventType.RentPaymentSoon -> startDate.minusDays(11),
        EventType.RentPaymentToday -> startDate.minusDays(5),
        EventType.RentPaymentOverduePush -> startDate
      ).foreach {
        case (eventType, nowMoment) =>
          mockGetPayment(paymentResponse(PaymentStatus.NEW, nowMoment))

          notificationProcessor(eventType)
            .processSingleEvent2(event(eventType), target)
            .futureValue
            .isInstanceOf[ComeBackAfter] shouldBe true
      }
    }

    "send push when payment is new and expected date is now" in new Wiring with Data {
      Seq(
        (EventType.RentPaymentSoon, startDate.minusDays(10).plusHours(11), soonPush),
        (EventType.RentPaymentToday, startDate.plusHours(11), todayPush),
        (EventType.RentPaymentOverduePush, startDate.plusDays(1).plusHours(11), overduePush),
        (EventType.RentPaymentOverdue2DaysPush, startDate.plusDays(2).plusHours(11), overduePush),
        (EventType.RentPaymentOverdue9DaysPush, startDate.plusDays(9).plusHours(11), overduePush),
        (EventType.RentPaymentOverdue14DaysPush, startDate.plusDays(14).plusHours(11), overduePush)
      ).foreach {
        case (eventType, nowMoment, push) =>
          mockGetPayment(paymentResponse(PaymentStatus.NEW, nowMoment))
          mockGetPayments(singlePaymentsResponse())
          mockPushnoySuccess(push)

          notificationProcessor(eventType)
            .processSingleEvent2(event(eventType), target)
            .futureValue shouldEqual Sent
      }
    }

    "ignore when payment is new and expected date is in the past" in new Wiring with Data {
      Seq(
        EventType.RentPaymentSoon -> startDate,
        EventType.RentPaymentToday -> startDate.plusDays(1),
        EventType.RentPaymentOverduePush -> startDate.plusDays(2),
        EventType.RentPaymentOverdue2DaysPush -> startDate.plusDays(3),
        EventType.RentPaymentOverdue9DaysPush -> startDate.plusDays(10),
        EventType.RentPaymentOverdue14DaysPush -> startDate.plusDays(15)
      ).foreach {
        case (eventType, nowMoment) =>
          mockGetPayment(paymentResponse(PaymentStatus.NEW, nowMoment))

          notificationProcessor(eventType)
            .processSingleEvent2(event(eventType), target)
            .futureValue shouldEqual Ignore
      }
    }

    "ignore when payment is new, expected date is now, but it's not the first unpaid payment" in new Wiring with Data {
      Seq(
        EventType.RentPaymentSoon -> startDate.minusDays(10).plusHours(11),
        EventType.RentPaymentToday -> startDate.plusHours(11),
        EventType.RentPaymentOverduePush -> startDate.plusDays(1).plusHours(11),
        EventType.RentPaymentOverdue2DaysPush -> startDate.plusDays(2).plusHours(11),
        EventType.RentPaymentOverdue9DaysPush -> startDate.plusDays(9).plusHours(11),
        EventType.RentPaymentOverdue14DaysPush -> startDate.plusDays(14).plusHours(11)
      ).foreach {
        case (eventType, nowMoment) =>
          mockGetPayment(paymentResponse(PaymentStatus.NEW, nowMoment))
          mockGetPayments(doublePaymentsResponse())

          notificationProcessor(eventType)
            .processSingleEvent2(event(eventType), target)
            .futureValue shouldEqual Ignore
      }
    }

    "reschedule when pushnoy request fails" in new Wiring with Data {
      mockGetPayment(paymentResponse(PaymentStatus.NEW))
      mockGetPayments(singlePaymentsResponse())
      mockPushnoyFailure()

      notificationProcessor(EventType.FirstRentPayment)
        .processSingleEvent2(event(EventType.FirstRentPayment), target)
        .futureValue
        .isInstanceOf[ComeBackAfter] shouldBe true
    }
  }

  trait Wiring {
    self: Data =>

    val rentClient: RentModerationClient = mock[RentModerationClient]
    val pushnoyManager: PushnoyManager = mock[PushnoyManager]
    val smsSenderManager: SmsSenderManager = mock[SmsSenderManager]

    def notificationProcessor(eventType: EventType = EventType.RentPaymentToday): TenantPaymentNotificationProcessor =
      new TenantPaymentNotificationProcessor(eventType, rentClient, pushnoyManager, smsSenderManager)

    def mockGetPayment(response: GetPaymentResponse): Unit =
      (rentClient
        .getModerationPayment(_: String, _: String)(_: Traced))
        .expects(flatId, paymentId, *)
        .returning(Future.successful(response))

    def mockGetPaymentNotFound(): Unit =
      (rentClient
        .getModerationPayment(_: String, _: String)(_: Traced))
        .expects(flatId, paymentId, *)
        .returning(Future.failed(ProtoErrorResponseException(StatusCodes.NotFound, ErrorResponse.getDefaultInstance)))

    def mockGetPayments(response: GetPaymentsResponse): Unit =
      (rentClient
        .getModerationPayments(_: String, _: Page, _: Set[String], _: Set[String], _: Option[SortDirection])(_: Traced))
        .expects(flatId, *, *, *, *, *)
        .returning(Future.successful(response))

    def mockPushnoySuccess(expectedPush: PalmaPushInfo): Unit =
      (pushnoyManager
        .sendPush(_: NotificationTarget, _: PalmaPushInfo, _: Targets.Value, _: Option[String])(_: Traced))
        .expects(target, expectedPush, *, *, *)
        .returning(Future.successful(1))

    def mockPushnoyFailure(): Unit =
      (pushnoyManager
        .sendPush(_: NotificationTarget, _: PalmaPushInfo, _: Targets.Value, _: Option[String])(_: Traced))
        .expects(target, *, *, *, *)
        .returning(Future.failed(new RuntimeException))
  }

  trait Data {

    implicit val traced: Traced = Traced.empty

    def dt(y: Int, m: Int, d: Int): DateTime =
      new LocalDate(y, m, d).toDateTimeAtStartOfDay

    val startDate: DateTime = dt(2020, 1, 10)
    val endDate: DateTime = dt(2020, 2, 9)

    val paymentId: String = "payment-id"
    val flatId: String = "flat-id"
    val contractId: String = "contract-id"

    val uid: Long = 1234
    val target: NotificationTarget = RentUserTarget(uid)

    val payload: EventPayload = EventPayload
      .newBuilder()
      .setRentPayment {
        RentPaymentPayload
          .newBuilder()
          .setFlatId(flatId)
      }
      .build()

    def event(eventType: EventType = EventType.RentPaymentToday): NotificationEvent =
      NotificationEvent(
        queueId = 0,
        target = target,
        eventType = eventType,
        eventId = paymentId,
        processTime = DateTimeUtil.now(),
        createTime = DateTimeUtil.now(),
        payload = payload
      )

    private def payment(id: String, status: PaymentStatus): Payment =
      Payment
        .newBuilder()
        .setId(id)
        .setStatus(status)
        .setTenantRentPayment {
          TenantRentPaymentPayload
            .newBuilder()
            .setContractPaymentDate(startDate)
            .setStartTime(startDate)
            .setEndTime(endDate)
        }
        .setContract {
          ContractBriefInfo
            .newBuilder()
            .setContractId(contractId)
        }
        .build()

    def paymentResponse(paymentStatus: PaymentStatus, nowMoment: DateTime = startDate): GetPaymentResponse =
      GetPaymentResponse
        .newBuilder()
        .setPayment(payment(paymentId, paymentStatus))
        .setContract {
          RentContract
            .newBuilder()
            .setContractId(contractId)
            .setNowMomentForTesting(nowMoment)
        }
        .build()

    def singlePaymentsResponse(): GetPaymentsResponse =
      GetPaymentsResponse
        .newBuilder()
        .addPayments(payment(paymentId, PaymentStatus.NEW))
        .build()

    def doublePaymentsResponse(): GetPaymentsResponse =
      GetPaymentsResponse
        .newBuilder()
        .addPayments(payment("another-payment-id", PaymentStatus.NEW))
        .addPayments(payment(paymentId, PaymentStatus.NEW))
        .build()

    val firstPush: PalmaPushInfo = FirstRentPaymentPush(flatId, paymentId, uid)

    val soonPush: PalmaPushInfo = RentPaymentSoonPush(flatId, paymentId, uid, startDate)

    val todayPush: PalmaPushInfo = RentPaymentTodayPush(flatId, paymentId, uid, startDate, endDate)

    val overduePush: PalmaPushInfo = RentPaymentOverduePush(flatId, paymentId, uid, startDate, endDate)
  }
}
