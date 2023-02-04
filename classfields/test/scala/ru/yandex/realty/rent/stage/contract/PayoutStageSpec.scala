package ru.yandex.realty.rent.stage.contract

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.rent.backend.{PayoutManager, RentPaymentsData}
import ru.yandex.realty.rent.gen.RentModelsGen
import ru.yandex.realty.rent.model.{ContractWithPayments, PayoutTransaction}
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.watching.ProcessingState
import org.joda.time.LocalTime
import ru.yandex.realty.clients.tinkoff.e2c.enums.{CardStatus, TransactionStatus}
import ru.yandex.realty.clients.tinkoff.e2c.request.{GetCardListRequest, GetStateRequest, InitRequest, PaymentRequest}
import ru.yandex.realty.clients.tinkoff.e2c.response.{Card, GetStateResponse, InitResponse, PaymentResponse}
import ru.yandex.realty.clients.tinkoff.e2c.{TinkoffE2CClient, TinkoffE2CManager}
import ru.yandex.realty.rent.dao.{PayoutTransactionDao, UserDao}
import ru.yandex.realty.rent.model.enums.{ContractStatus, PaymentStatus}
import ru.yandex.realty.rent.proto.model.payment.PayoutStatusNamespace.PayoutStatus
import ru.yandex.realty.rent.proto.model.payment.{PayoutErrorNamespace, PayoutStatusNamespace}

import java.util.UUID
import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class PayoutStageSpec extends AsyncSpecBase with RentModelsGen with RentPaymentsData {

  val tinkoffE2CClient: TinkoffE2CClient = mock[TinkoffE2CClient]
  val userDao: UserDao = mock[UserDao]
  val payoutTransactionDao: PayoutTransactionDao = mock[PayoutTransactionDao]
  val tinkoffE2CManager: TinkoffE2CManager = new TinkoffE2CManager(tinkoffE2CClient)

  val payoutManager: PayoutManager =
    new PayoutManager(tinkoffE2CClient, tinkoffE2CManager, userDao, payoutTransactionDao)

  private def invokeStage(contract: ContractWithPayments): ProcessingState[ContractWithPayments] = {
    val state = ProcessingState(contract)
    val stage = new PayoutStage(payoutManager)
    stage.process(state)(Traced.empty).futureValue
  }

  "PayoutStage" should {
    "use the guaranteed payment" in {
      val now = TodayDate.withTime(new LocalTime(19, 0))
      val paymentDate = TodayDate
      val previousPaymentDate = TodayDate.minusMonths(1)
      val contract =
        createContract(paymentDate, paymentDate.getDayOfMonth, None, None, None, Some(now), ContractStatus.Active, true)
      val previousPayment = createPayment(
        contract,
        previousPaymentDate,
        previousPaymentDate.plusMonths(1).minusDays(1)
      ).copy(status = PaymentStatus.PaidToOwner)
      val payment = createPayment(contract, paymentDate, paymentDate.plusMonths(1).minusDays(1))
        .copy(status = PaymentStatus.New)
      val contractWithPayments = ContractWithPayments(contract, List(previousPayment, payment))
      val transactionId = UUID.randomUUID().toString
      val payoutTransaction = PayoutTransaction(payment.id, transactionId, createTime = now)
      (payoutTransactionDao
        .findLastByPaymentId(_: String)(_: Traced))
        .expects(where { (id: String, _: Traced) =>
          id == payment.id
        })
        .once()
        .returning(Future.successful(Some(payoutTransaction)))
      val response = GetStateResponse(Some(TransactionStatus.COMPLETED), Success = true, "", None, None)
      (tinkoffE2CClient
        .getState(_: GetStateRequest)(_: Traced))
        .expects(where { (request: GetStateRequest, _: Traced) =>
          request.PaymentId == transactionId && request.IP.isEmpty
        })
        .returning(Future.successful(response))
      val newState = invokeStage(contractWithPayments)
      newState.entry.payments.size shouldBe 2
      val resultPayment = newState.entry.payments.maxBy(_.paymentDate.getMillis)
      resultPayment.status shouldBe PaymentStatus.PaidOutUnderGuarantee
      resultPayment.isPaidOutUnderGuarantee shouldBe true
    }

    "use regular payment for the first month" in {
      val now = TodayDate.withTime(new LocalTime(19, 0))
      val paymentDate = TodayDate
      val contract = createContract(paymentDate, paymentDate.getDayOfMonth, nowMoment = Some(now))
      val payment = createPayment(contract, paymentDate, paymentDate.plusMonths(1).minusDays(1))
        .copy(status = PaymentStatus.New)
      val contractWithPayments = ContractWithPayments(contract, List(payment))
      val newState = invokeStage(contractWithPayments)
      val resultPayment = newState.entry.payments.maxBy(_.paymentDate.getMillis)
      resultPayment.status shouldBe PaymentStatus.New
      resultPayment.isPaidOutUnderGuarantee shouldBe false
    }

    "not paid twice to the owner" in {
      val now = TodayDate.withTime(new LocalTime(19, 0))
      val paymentDate = TodayDate
      val contract = createContract(paymentDate, paymentDate.getDayOfMonth, nowMoment = Some(now))
      val payment = createPayment(contract, paymentDate, paymentDate.plusMonths(1).minusDays(1))
        .copy(status = PaymentStatus.PaidByTenant, isPaidOutUnderGuarantee = true)
      val contractWithPayments = ContractWithPayments(contract, List(payment))
      val newState = invokeStage(contractWithPayments)
      val resultPayment = newState.entry.payments.maxBy(_.paymentDate.getMillis)
      resultPayment.status shouldBe PaymentStatus.PaidToOwner
      resultPayment.isPaidOutUnderGuarantee shouldBe true
    }

    "try payout under guarantee and fail because of the absent card" in {
      val now = TodayDate.withTime(new LocalTime(19, 0))
      val paymentDate = TodayDate
      val previousPaymentDate = TodayDate.minusMonths(1)
      val contract =
        createContract(paymentDate, paymentDate.getDayOfMonth, None, None, None, Some(now), ContractStatus.Active, true)
      val owner = userGen().next.copy(uid = contract.getOwnerUid)
      val previousPayment = createPayment(
        contract,
        previousPaymentDate,
        previousPaymentDate.plusMonths(1).minusDays(1)
      ).copy(status = PaymentStatus.PaidToOwner)
      val payment = createPayment(contract, paymentDate, paymentDate.plusMonths(1).minusDays(1))
        .copy(status = PaymentStatus.New)
      val contractWithPayments = ContractWithPayments(contract, List(previousPayment, payment))
      (payoutTransactionDao
        .findLastByPaymentId(_: String)(_: Traced))
        .expects(*, *)
        .atLeastOnce()
        .returning(Future.successful(None))
      (userDao
        .findByUid(_: Long)(_: Traced))
        .expects(where { (uid: Long, _: Traced) =>
          uid == contract.getOwnerUid
        })
        .atLeastOnce()
        .returning(Future.successful(owner))
      (tinkoffE2CClient
        .getCardList(_: GetCardListRequest)(_: Traced))
        .expects(*, *)
        .returning(Future.successful(Nil))
      val state = invokeStage(contractWithPayments)
      state.entry.payments.size shouldBe 2
      val resultPayment = state.entry.payments.maxBy(_.paymentDate.getMillis)
      resultPayment.data.getPayoutStatus shouldBe PayoutStatusNamespace.PayoutStatus.UNRECOVERABLE_ERROR
      resultPayment.data.getPayoutError shouldBe PayoutErrorNamespace.PayoutError.BOUND_OWNER_CARD_IS_ABSENT
    }

    "try payout under guarantee and reject payment" in {
      val now = TodayDate.withTime(new LocalTime(19, 0))
      val paymentDate = TodayDate
      val previousPaymentDate = TodayDate.minusMonths(1)
      val contract =
        createContract(paymentDate, paymentDate.getDayOfMonth, None, None, None, Some(now), ContractStatus.Active, true)
      val owner = userGen().next.copy(uid = contract.getOwnerUid)
      val previousPayment = createPayment(
        contract,
        previousPaymentDate,
        previousPaymentDate.plusMonths(1).minusDays(1)
      ).copy(status = PaymentStatus.PaidToOwner)
      val payment = createPayment(contract, paymentDate, paymentDate.plusMonths(1).minusDays(1))
        .copy(status = PaymentStatus.New)
      val contractWithPayments = ContractWithPayments(contract, List(previousPayment, payment))
      (payoutTransactionDao
        .findLastByPaymentId(_: String)(_: Traced))
        .expects(where { (id: String, _: Traced) =>
          id == payment.id
        })
        .once()
        .returning(Future.successful(None))
      (userDao
        .findByUid(_: Long)(_: Traced))
        .expects(where { (uid: Long, _: Traced) =>
          uid == contract.getOwnerUid
        })
        .once()
        .returning(Future.successful(owner))
      (tinkoffE2CClient
        .getCardList(_: GetCardListRequest)(_: Traced))
        .expects(*, *)
        .once()
        .returning(Future.successful(List(Card("", "", "", CardStatus.Active, 7, None))))
      (tinkoffE2CClient
        .init(_: InitRequest)(_: Traced))
        .expects(*, *)
        .once()
        .returning(
          Future.successful(InitResponse(Some(TransactionStatus.CHECKED), Some(payment.id), true, "", None, None))
        )
      (payoutTransactionDao
        .create(_: Iterable[PayoutTransaction])(_: Traced))
        .expects(*, *)
        .once()
        .returning(Future.unit)
      (tinkoffE2CClient
        .payment(_: PaymentRequest)(_: Traced))
        .expects(*, *)
        .returning(Future.successful(PaymentResponse(Some(TransactionStatus.REJECTED), true, "", None, None)))
      val state = invokeStage(contractWithPayments)
      state.entry.payments.size shouldBe 2
      val resultPayment = state.entry.payments.maxBy(_.paymentDate.getMillis)
      resultPayment.data.getPayoutStatus shouldBe PayoutStatusNamespace.PayoutStatus.RECOVERABLE_ERROR
      resultPayment.status shouldBe PaymentStatus.New
      resultPayment.isPaidOutUnderGuarantee shouldBe false
    }

    "try payout to owner after tenant payment and reject guaranteed payment" in {
      val now = TodayDate.withTime(new LocalTime(19, 0))
      val paymentDate = TodayDate
      val previousPaymentDate = TodayDate.minusMonths(1)
      val contract =
        createContract(paymentDate, paymentDate.getDayOfMonth, None, None, None, Some(now), ContractStatus.Active, true)
      val owner = userGen().next.copy(uid = contract.getOwnerUid)
      val previousPayment = createPayment(
        contract,
        previousPaymentDate,
        previousPaymentDate.plusMonths(1).minusDays(1)
      ).copy(status = PaymentStatus.PaidToOwner)
      val payment = createPayment(contract, paymentDate, paymentDate.plusMonths(1).minusDays(1))
      val updatedPayment = payment.copy(
        status = PaymentStatus.PaidByTenant,
        data = payment.data.toBuilder.setPayoutStatus(PayoutStatus.RECOVERABLE_ERROR).build()
      )
      val contractWithPayments = ContractWithPayments(contract, List(previousPayment, updatedPayment))
      (payoutTransactionDao
        .findLastByPaymentId(_: String)(_: Traced))
        .expects(where { (id: String, _: Traced) =>
          id == payment.id
        })
        .once()
        .returning(Future.successful(None))
      (userDao
        .findByUid(_: Long)(_: Traced))
        .expects(where { (uid: Long, _: Traced) =>
          uid == contract.getOwnerUid
        })
        .once()
        .returning(Future.successful(owner))
      (tinkoffE2CClient
        .getCardList(_: GetCardListRequest)(_: Traced))
        .expects(*, *)
        .once()
        .returning(Future.successful(List(Card("", "", "", CardStatus.Active, 7, None))))
      (tinkoffE2CClient
        .init(_: InitRequest)(_: Traced))
        .expects(*, *)
        .once()
        .returning(
          Future.successful(InitResponse(Some(TransactionStatus.CHECKED), Some(payment.id), true, "", None, None))
        )
      (payoutTransactionDao
        .create(_: Iterable[PayoutTransaction])(_: Traced))
        .expects(*, *)
        .once()
        .returning(Future.unit)
      (tinkoffE2CClient
        .payment(_: PaymentRequest)(_: Traced))
        .expects(*, *)
        .returning(Future.successful(PaymentResponse(Some(TransactionStatus.COMPLETED), true, "", None, None)))
      val state = invokeStage(contractWithPayments)
      state.entry.payments.size shouldBe 2
      val resultPayment = state.entry.payments.maxBy(_.paymentDate.getMillis)
      resultPayment.data.getPayoutStatus shouldBe PayoutStatusNamespace.PayoutStatus.PAID_OUT
      resultPayment.status shouldBe PaymentStatus.PaidToOwner
      resultPayment.isPaidOutUnderGuarantee shouldBe false
    }

    "try to make owner payout after tenant payment while processing guaranteed payout" in {
      val now = TodayDate.withTime(new LocalTime(19, 0))
      val paymentDate = TodayDate
      val previousPaymentDate = TodayDate.minusMonths(1)
      val contract =
        createContract(
          paymentDate,
          paymentDate.getDayOfMonth,
          None,
          None,
          nowMoment = Some(now),
          status = ContractStatus.Active,
          usePayoutUnderGuarantee = true
        )
      val previousPayment = createPayment(
        contract,
        previousPaymentDate,
        previousPaymentDate.plusMonths(1).minusDays(1)
      ).copy(status = PaymentStatus.PaidToOwner)
      val payment = createPayment(contract, paymentDate, paymentDate.plusMonths(1).minusDays(1))
        .copy(status = PaymentStatus.PaidByTenant)
      val contractWithPayments = ContractWithPayments(contract, List(previousPayment, payment))
      val owner = userGen().next.copy(uid = contract.getOwnerUid)
      val transactionId = UUID.randomUUID().toString
      (payoutTransactionDao
        .findLastByPaymentId(_: String)(_: Traced))
        .expects(where { (id: String, _: Traced) =>
          id == payment.id
        })
        .once()
        .returning(
          Future.successful(Some(PayoutTransaction(payment.id, transactionId, createTime = now)))
        )
      (tinkoffE2CClient
        .getState(_: GetStateRequest)(_: Traced))
        .expects(where { (r: GetStateRequest, _: Traced) =>
          r.PaymentId == transactionId
        })
        .once()
        .returning(
          Future.successful(GetStateResponse(Some(TransactionStatus.PROCESSING), Success = true, "", None, None))
        )
      (userDao
        .findByUid(_: Long)(_: Traced))
        .expects(where { (uid: Long, _: Traced) =>
          uid == contract.getOwnerUid
        })
        .anyNumberOfTimes()
        .returning(Future.successful(owner))
      (tinkoffE2CClient
        .getCardList(_: GetCardListRequest)(_: Traced))
        .expects(*, *)
        .anyNumberOfTimes()
        .returning(Future.successful(List(Card("", "", "", CardStatus.Active, 7, None))))
      (tinkoffE2CClient
        .init(_: InitRequest)(_: Traced))
        .expects(*, *)
        .anyNumberOfTimes()
        .returning(
          Future.successful(InitResponse(Some(TransactionStatus.CHECKED), Some(payment.id), true, "", None, None))
        )
      (payoutTransactionDao
        .create(_: Iterable[PayoutTransaction])(_: Traced))
        .expects(*, *)
        .anyNumberOfTimes()
        .returning(Future.unit)
      (tinkoffE2CClient
        .payment(_: PaymentRequest)(_: Traced))
        .expects(*, *)
        .anyNumberOfTimes()
        .returning(Future.successful(PaymentResponse(Some(TransactionStatus.COMPLETED), true, "", None, None)))

      val newState = invokeStage(contractWithPayments)
      val paidToOwnerPayment = newState.entry.payments.maxBy(_.paymentDate.getMillis)
      paidToOwnerPayment.status shouldBe PaymentStatus.PaidByTenant
      paidToOwnerPayment.isPaidOutUnderGuarantee shouldBe false
      newState.entry.contract.visitTime.isDefined shouldBe true
    }
  }
}
