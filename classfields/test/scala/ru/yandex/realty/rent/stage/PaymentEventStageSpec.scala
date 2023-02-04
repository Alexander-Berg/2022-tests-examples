package ru.yandex.realty.rent.stage

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.rent.backend.RentPaymentsData
import ru.yandex.realty.rent.dao.FlatDao
import ru.yandex.realty.rent.gen.RentModelsGen
import ru.yandex.realty.rent.model.ContractWithPayments
import ru.yandex.realty.rent.model.enums.{ContractStatus, PaymentStatus}
import ru.yandex.realty.rent.proto.event.{PaymentEvent => PaymentEventV3}
import ru.yandex.realty.rent.proto.api.payment.PaymentEvent
import ru.yandex.realty.rent.stage.contract.PaymentEventStage
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.watching.ProcessingState
import ru.yandex.vertis.broker.client.marshallers.ProtoMarshaller
import ru.yandex.vertis.broker.client.simple.BrokerClient
import ru.yandex.vertis.util.time.DateTimeUtil

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class PaymentEventStageSpec extends AsyncSpecBase with RentModelsGen with RentPaymentsData {
  implicit private val traced: Traced = Traced.empty
  implicit private val marshalledEvent: ProtoMarshaller[PaymentEvent] = ProtoMarshaller.google[PaymentEvent]
  implicit private val marshalledEventV3: ProtoMarshaller[PaymentEventV3] = ProtoMarshaller.google[PaymentEventV3]

  private val DefaultContract =
    createContract(TodayDate, 10, None, None, None, Some(TodayDate), ContractStatus.Active, false)
  private val DefaultPayment = createPayment(DefaultContract, TodayDate, TodayDate)

  "PaymentEventStage" should {
    "reschedule process on min visit time if payment date is in the future" in {
      val (brokerClientMock: BrokerClient, flatDaoMock: FlatDao) = handleShouldNotProcessMocks

      val futureTime = DateTimeUtil.now().plusDays(1)
      val earlierPayment = DefaultPayment.copy(paymentDate = futureTime)
      val laterPayment = DefaultPayment.copy(paymentDate = futureTime.plusDays(1))
      val payments = List(earlierPayment, laterPayment)
      val contractWithPayments = ContractWithPayments(DefaultContract, payments)
      val resultState = invokeStage(brokerClientMock, flatDaoMock, contractWithPayments)

      resultState.entry.payments shouldBe payments
      resultState.entry.contract.visitTime shouldBe Some(futureTime)
    }

    "not process if payment status is not paid_to_owner or paid_by_tenant in invalid moment" in {
      val (brokerClientMock: BrokerClient, flatDaoMock: FlatDao) = handleShouldNotProcessMocks
      val newPayment = DefaultPayment.copy(
        status = PaymentStatus.New,
        data = DefaultPayment.data.toBuilder
          .setPaymentDateHasComeEventSentV3(true)
          .build
      )
      val futurePayment = DefaultPayment.copy(
        status = PaymentStatus.FuturePayment,
        data = DefaultPayment.data.toBuilder
          .setPaymentDateHasComeEventSentV3(true)
          .build
      )
      val unknownPayment = DefaultPayment.copy(
        status = PaymentStatus.Unknown,
        data = DefaultPayment.data.toBuilder
          .setPaymentDateHasComeEventSentV3(true)
          .build
      )
      val payments = List(newPayment, futurePayment, unknownPayment)
      val contractWithPayments = ContractWithPayments(DefaultContract, payments)
      val resultState = invokeStage(brokerClientMock, flatDaoMock, contractWithPayments)

      resultState.entry.payments shouldBe payments
      resultState.entry.contract.visitTime shouldBe None
    }

    "not process if payment status is not paid_to_owner or paid_by_tenant and payment date in the future" in {
      val (brokerClientMock: BrokerClient, flatDaoMock: FlatDao) = handleShouldNotProcessMocks
      val futureTime = DateTimeUtil.now().plusDays(1)
      val newPayment = DefaultPayment.copy(status = PaymentStatus.New, paymentDate = futureTime)
      val futurePayment = DefaultPayment.copy(status = PaymentStatus.FuturePayment, paymentDate = futureTime)
      val payments = List(newPayment, futurePayment)
      val contractWithPayments = ContractWithPayments(DefaultContract, payments)
      val resultState = invokeStage(brokerClientMock, flatDaoMock, contractWithPayments)

      resultState.entry.payments shouldBe payments
      resultState.entry.contract.visitTime shouldBe Some(futureTime)
    }

    "process when payment status is paid_to_owner or paid_by_tenant without guarantee payout" in {
      val (brokerClientMock: BrokerClient, flatDaoMock: FlatDao) = handleShouldProcessMocks

      val paidByTenant = DefaultPayment.copy(status = PaymentStatus.PaidByTenant)
      val paidToOwner = DefaultPayment.copy(
        status = PaymentStatus.PaidToOwner,
        data = DefaultPayment.data.toBuilder
          .setPaymentEventSent(true)
          .setPaymentEventSentV3(true)
          .build
      )
      val payments = List(paidByTenant, paidToOwner)
      val contractWithPayments = ContractWithPayments(DefaultContract, payments)
      val resultState = invokeStage(brokerClientMock, flatDaoMock, contractWithPayments)

      val paymentSentCount =
        resultState.entry.payments.count(p => p.data.getPaymentEventSent && p.data.getPaymentEventSentV3)
      val payoutSentCount =
        resultState.entry.payments.count(p => p.data.getPayoutEventSent && p.data.getPayoutEventSentV3)

      paymentSentCount shouldBe 2
      payoutSentCount shouldBe 1
      resultState.entry.contract.visitTime shouldBe None
    }

    "process when payment status is paid_to_owner with guarantee payout" in {
      val (brokerClientMock: BrokerClient, flatDaoMock: FlatDao) = handleShouldProcessMocks

      val paidToOwner2 = DefaultPayment.copy(
        status = PaymentStatus.PaidToOwner,
        data = DefaultPayment.data.toBuilder.setPaymentDateHasComeEventSentV3(true).build,
        isPaidOutUnderGuarantee = true
      )
      val paidToOwner1 = DefaultPayment.copy(
        status = PaymentStatus.PaidToOwner,
        data = DefaultPayment.data.toBuilder.setPaymentDateHasComeEventSentV3(true).build,
        isPaidOutUnderGuarantee = true
      )
      val paidToOwnerSent = DefaultPayment.copy(
        status = PaymentStatus.PaidToOwner,
        data = DefaultPayment.data.toBuilder
          .setPaymentEventSent(true)
          .setPaymentEventSentV3(true)
          .setPaymentDateHasComeEventSentV3(true)
          .build,
        isPaidOutUnderGuarantee = true
      )
      val payments = List(paidToOwner1, paidToOwner2, paidToOwnerSent)
      val contractWithPayments = ContractWithPayments(DefaultContract, payments)
      val resultState = invokeStage(brokerClientMock, flatDaoMock, contractWithPayments)

      val paymentSentCount =
        resultState.entry.payments.count(p => p.data.getPaymentEventSent && p.data.getPaymentEventSentV3)
      val payoutSentCount =
        resultState.entry.payments.count(p => p.data.getPayoutEventSent && p.data.getPayoutEventSentV3)

      paymentSentCount shouldBe 3
      payoutSentCount shouldBe 0
      resultState.entry.contract.visitTime shouldBe None
    }

    "process when payment status is paid_out_under_guarantee" in {
      val (brokerClientMock: BrokerClient, flatDaoMock: FlatDao) = handleShouldProcessV3Mocks

      val paidOutUnderGuarantee1 = DefaultPayment.copy(
        status = PaymentStatus.PaidOutUnderGuarantee,
        data = DefaultPayment.data.toBuilder.setPaymentDateHasComeEventSentV3(true).build,
        isPaidOutUnderGuarantee = true
      )
      val paidOutUnderGuarantee2 = DefaultPayment.copy(
        status = PaymentStatus.PaidOutUnderGuarantee,
        data = DefaultPayment.data.toBuilder.setPaymentDateHasComeEventSentV3(true).build,
        isPaidOutUnderGuarantee = true
      )
      val paidOutUnderGuaranteeSent = DefaultPayment.copy(
        status = PaymentStatus.PaidOutUnderGuarantee,
        data = DefaultPayment.data.toBuilder
          .setPaymentDateHasComeEventSentV3(true)
          .setPayoutUnderGuaranteeEventSentV3(true)
          .build,
        isPaidOutUnderGuarantee = true
      )
      val payments = List(paidOutUnderGuarantee1, paidOutUnderGuarantee2, paidOutUnderGuaranteeSent)
      val contractWithPayments = ContractWithPayments(DefaultContract, payments)
      val resultState = invokeStage(brokerClientMock, flatDaoMock, contractWithPayments)

      val paymentSentCount =
        resultState.entry.payments.count(p => p.data.getPaymentEventSent && p.data.getPaymentEventSentV3)
      val payoutSentCount =
        resultState.entry.payments.count(p => p.data.getPayoutEventSent && p.data.getPayoutEventSentV3)
      val payoutUnderGuaranteeSentCount =
        resultState.entry.payments.count(p => p.data.getPayoutUnderGuaranteeEventSentV3)

      paymentSentCount shouldBe 0
      payoutSentCount shouldBe 0
      payoutUnderGuaranteeSentCount shouldBe 3
      resultState.entry.contract.visitTime shouldBe None
    }

    "process when penalty increased" in {
      val (brokerClientMock: BrokerClient, flatDaoMock: FlatDao) = handleShouldProcessV3Mocks

      val penaltyIncreased1 = DefaultPayment.copy(
        data = DefaultPayment.data.toBuilder
          .setPaymentEventSent(true)
          .setPaymentEventSentV3(true)
          .setPayoutEventSent(true)
          .setPayoutEventSentV3(true)
          .setPaymentDateHasComeEventSentV3(true)
          .setPayoutUnderGuaranteeEventSentV3(true)
          .setLastPenaltyAmountEventSentV3(1000)
          .setTenantPenaltyAmount(1050)
          .build,
        isPaidOutUnderGuarantee = true
      )
      val penaltyIncreased2 = DefaultPayment.copy(
        data = DefaultPayment.data.toBuilder
          .setPaymentEventSent(true)
          .setPaymentEventSentV3(true)
          .setPayoutEventSent(true)
          .setPayoutEventSentV3(true)
          .setPaymentDateHasComeEventSentV3(true)
          .setPayoutUnderGuaranteeEventSentV3(true)
          .setLastPenaltyAmountEventSentV3(0)
          .setTenantPenaltyAmount(110)
          .build,
        isPaidOutUnderGuarantee = true
      )
      val penaltyNotIncreased = DefaultPayment.copy(
        data = DefaultPayment.data.toBuilder
          .setPaymentEventSent(true)
          .setPaymentEventSentV3(true)
          .setPayoutEventSent(true)
          .setPayoutEventSentV3(true)
          .setPaymentDateHasComeEventSentV3(true)
          .setPayoutUnderGuaranteeEventSentV3(true)
          .setPayoutUnderGuaranteeEventSentV3(true)
          .setLastPenaltyAmountEventSentV3(10)
          .setTenantPenaltyAmount(10)
          .build,
        isPaidOutUnderGuarantee = true
      )
      val payments = List(penaltyIncreased1, penaltyIncreased2, penaltyNotIncreased)
      val contractWithPayments = ContractWithPayments(DefaultContract, payments)
      val resultState = invokeStage(brokerClientMock, flatDaoMock, contractWithPayments)

      val paymentWithPenalty1 =
        resultState.entry.payments.count(p => p.data.getLastPenaltyAmountEventSentV3 == 1050)
      val paymentWithPenalty2 =
        resultState.entry.payments.count(p => p.data.getLastPenaltyAmountEventSentV3 == 110)
      val paymentWithPenalty3 =
        resultState.entry.payments.count(p => p.data.getLastPenaltyAmountEventSentV3 == 10)
      val paymentWithOldPenalty =
        resultState.entry.payments
          .count(p => p.data.getLastPenaltyAmountEventSentV3 == 0 || p.data.getLastPenaltyAmountEventSentV3 == 1000)

      paymentWithPenalty1 shouldBe 1
      paymentWithPenalty2 shouldBe 1
      paymentWithPenalty3 shouldBe 1
      paymentWithOldPenalty shouldBe 0
      resultState.entry.contract.visitTime shouldBe None
    }

    "process when payment date has come" in {
      val (brokerClientMock: BrokerClient, flatDaoMock: FlatDao) = handleShouldProcessV3Mocks

      val paymentDateHasCome1 = DefaultPayment.copy(
        status = PaymentStatus.New,
        paymentDate = TodayDate,
        data = DefaultPayment.data.toBuilder.build
      )
      val paymentDateHasCome2 = DefaultPayment.copy(
        status = PaymentStatus.New,
        paymentDate = TodayDate,
        data = DefaultPayment.data.toBuilder.build
      )
      val paymentDateHasComeSent = DefaultPayment.copy(
        status = PaymentStatus.New,
        paymentDate = TodayDate,
        data = DefaultPayment.data.toBuilder
          .setPaymentDateHasComeEventSentV3(true)
          .build
      )
      val paymentDateHasNotCome = DefaultPayment.copy(
        status = PaymentStatus.New,
        paymentDate = TodayDate.minusDays(1),
        data = DefaultPayment.data.toBuilder.build
      )
      val payments = List(paymentDateHasCome1, paymentDateHasCome2, paymentDateHasComeSent, paymentDateHasNotCome)
      val contractWithPayments = ContractWithPayments(DefaultContract, payments)
      val resultState = invokeStage(brokerClientMock, flatDaoMock, contractWithPayments)

      val paymentDateHasNotComeSentCount =
        resultState.entry.payments.count(p => !p.data.getPaymentDateHasComeEventSentV3)

      val paymentDateHasComeSentCount = resultState.entry.payments.count(p => p.data.getPaymentDateHasComeEventSentV3)

      paymentDateHasNotComeSentCount shouldBe 1
      paymentDateHasComeSentCount shouldBe 3
      resultState.entry.contract.visitTime shouldBe None
    }

    "not process when event has already been sent" in {
      val (brokerClientMock: BrokerClient, flatDaoMock: FlatDao) = handleShouldNotProcessMocks

      (brokerClientMock
        .send[PaymentEvent](_: Option[String], _: PaymentEvent, _: Option[String])(_: ProtoMarshaller[PaymentEvent]))
        .expects(*, *, *, *)
        .never()
        .returning(Future.unit)

      (brokerClientMock
        .send[PaymentEventV3](_: Option[String], _: PaymentEventV3, _: Option[String])(
          _: ProtoMarshaller[PaymentEventV3]
        ))
        .expects(*, *, *, *)
        .never()
        .returning(Future.unit)

      val paidByTenant = DefaultPayment.copy(
        status = PaymentStatus.PaidByTenant,
        data = DefaultPayment.data.toBuilder
          .setPaymentEventSent(true)
          .setPaymentEventSentV3(true)
          .build
      )
      val paidToOwner = DefaultPayment.copy(
        status = PaymentStatus.PaidToOwner,
        data = DefaultPayment.data.toBuilder
          .setPaymentEventSent(true)
          .setPaymentEventSentV3(true)
          .setPayoutEventSent(true)
          .setPayoutEventSentV3(true)
          .build
      )
      val payments = List(paidByTenant, paidToOwner)
      val contractWithPayments = ContractWithPayments(DefaultContract, payments)
      val resultState = invokeStage(brokerClientMock, flatDaoMock, contractWithPayments)

      resultState.entry.contract.visitTime shouldBe None
    }

    "process payment with status paid_to_owner twice when paid_by_tenant event sending was skipped" in {
      val (brokerClientMock: BrokerClient, flatDaoMock: FlatDao) = handleShouldProcessMocks

      val paidToOwner = DefaultPayment.copy(
        status = PaymentStatus.PaidToOwner,
        data = DefaultPayment.data.toBuilder
          .setPaymentEventSent(false)
          .setPaymentEventSentV3(false)
          .build
      )

      val payments = List(paidToOwner)
      val contractWithPayments = ContractWithPayments(DefaultContract, payments)
      val resultState = invokeStage(brokerClientMock, flatDaoMock, contractWithPayments)

      resultState.entry.payments.exists(
        p =>
          p.data.getPaymentEventSent && p.data.getPaymentEventSentV3 &&
            p.data.getPayoutEventSent && p.data.getPayoutEventSentV3
      ) shouldBe true
      resultState.entry.contract.visitTime shouldBe None
    }
  }

  private def invokeStage(
    brokerClient: BrokerClient,
    flatDao: FlatDao,
    contract: ContractWithPayments
  )(implicit traced: Traced): ProcessingState[ContractWithPayments] = {
    val state = ProcessingState(contract)
    val stage = new PaymentEventStage(brokerClient, flatDao)
    stage.process(state).futureValue
  }

  private def handleShouldProcessV3Mocks = {
    val (brokerClientMock: BrokerClient, flatDaoMock: FlatDao) = handleShouldNotProcessMocks

    (brokerClientMock
      .send[PaymentEventV3](_: Option[String], _: PaymentEventV3, _: Option[String])(
        _: ProtoMarshaller[PaymentEventV3]
      ))
      .expects(*, *, *, *)
      .twice()
      .returning(Future.unit)
    (brokerClientMock, flatDaoMock)
  }

  private def handleShouldProcessMocks = {
    val (brokerClientMock: BrokerClient, flatDaoMock: FlatDao) = handleShouldNotProcessMocks

    (brokerClientMock
      .send[PaymentEvent](_: Option[String], _: PaymentEvent, _: Option[String])(_: ProtoMarshaller[PaymentEvent]))
      .expects(*, *, *, *)
      .twice()
      .returning(Future.unit)

    (brokerClientMock
      .send[PaymentEventV3](_: Option[String], _: PaymentEventV3, _: Option[String])(
        _: ProtoMarshaller[PaymentEventV3]
      ))
      .expects(*, *, *, *)
      .twice()
      .returning(Future.unit)
    (brokerClientMock, flatDaoMock)
  }

  private def handleShouldNotProcessMocks = {
    val brokerClientMock = mock[BrokerClient]
    val flatDaoMock = mock[FlatDao]

    (flatDaoMock
      .findById(_: String)(_: Traced))
      .expects(*, *)
      .once()
      .returning(Future.successful(flatGen().next))
    (brokerClientMock, flatDaoMock)
  }
}
