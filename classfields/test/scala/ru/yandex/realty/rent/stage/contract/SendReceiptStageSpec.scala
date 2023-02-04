package ru.yandex.realty.rent.stage.contract

import com.google.protobuf.Timestamp
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalamock.scalatest.MockFactory
import org.scalatestplus.junit.JUnitRunner
import realty.palma.rent_user.{PaymentData, RentUser}
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.application.ng.palma.client.PalmaClient
import ru.yandex.realty.cashbox.proto.receipt.{Receipt, SearchReceiptResponse, Payment => CashboxPayment}
import ru.yandex.realty.clients.cashbox.CashboxClient
import ru.yandex.realty.rent.backend.RentPaymentsData
import ru.yandex.realty.rent.dao.UserDao
import ru.yandex.realty.rent.model.enums.{PassportVerificationStatus, PaymentType}
import ru.yandex.realty.rent.model.{ContractWithPayments, Payment, User}
import ru.yandex.realty.rent.proto.model.payment.ReceiptIncomeTypeNamespace.ReceiptIncomeType
import ru.yandex.realty.rent.proto.model.payment.{HouseServicePaymentData, ReceiptInfo}
import ru.yandex.realty.rent.proto.model.payment.ReceiptRentTypeNamespace.ReceiptRentType
import ru.yandex.realty.rent.proto.model.user.UserData
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.watching.ProcessingState

import scala.concurrent.Future
import scala.util.Random

@RunWith(classOf[JUnitRunner])
class SendReceiptStageSpec extends AsyncSpecBase with MockFactory with RentPaymentsData {

  private val cashBoxClientMock = mock[CashboxClient]
  private val userDaoMock = mock[UserDao]
  private val palmaRentUserClient = mock[PalmaClient[RentUser]]
  implicit val traced: Traced = Traced.empty

  private def invokeStage(
    contract: ContractWithPayments
  ): ProcessingState[ContractWithPayments] = {
    val state = ProcessingState(contract)
    val stage = new SendReceiptStage(cashBoxClientMock, userDaoMock, palmaRentUserClient)
    stage.process(state).futureValue
  }

  "SendReceiptStage" should {
    "create agency receipt for house service" in {
      val agencyReceiptId = uidGen().next
      handleMocks(
        agencyReceiptId = agencyReceiptId,
        agencyReceiptCount = 1
      )
      val paymentDate = DateTime.now().plusMonths(2)
      val contract = createContract(paymentDate, paymentDate.getDayOfMonth)
      val newPayment = createPayment(contract, paymentDate, paymentDate.plusMonths(1).minusDays(1))
      val paymentData = newPayment.data.toBuilder
        .addTenantTransactions(createTenantPaymentTransactionInfo(paymentDate.minusMonths(1)))
        .setHouseServicePayment(HouseServicePaymentData.newBuilder().setBillAmount(posNum[Long].next).build())
        .build()
      val payment = newPayment.copy(`type` = PaymentType.HouseServices, data = paymentData)
      val contractWithPayments = ContractWithPayments(contract, List(payment))
      val newState = invokeStage(contractWithPayments)
      newState.entry.payments.length shouldBe 1
      val receiptInfoList = newState.entry.payments.head.data.getReceiptsList
      receiptInfoList.size() shouldBe 1
      val agencyReceipt = receiptInfoList.get(0)
      val expectedPayment = payment.copy(
        data = paymentData.toBuilder
          .addReceipts(agencyReceiptInfo(agencyReceiptId, agencyReceipt.getCreateTime, payment))
          .build()
      )
      newState.entry.payments shouldBe Seq(expectedPayment)
    }

    "create agency receipt and commission prepayment receipt" in {
      val agencyReceiptId = uidGen().next
      val commissionPrepaymentReceiptId = uidGen().next
      handleMocks(
        agencyReceiptId = agencyReceiptId,
        commissionPrepaymentReceiptId = commissionPrepaymentReceiptId,
        agencyReceiptCount = 1,
        commissionPrepaymentReceiptCount = 1
      )
      val paymentDate = DateTime.now().plusMonths(2)
      val contract = createContract(paymentDate, paymentDate.getDayOfMonth)
      val newPayment = createPayment(contract, paymentDate, paymentDate.plusMonths(1).minusDays(1))
      val paymentData = newPayment.data.toBuilder
        .addTenantTransactions(createTenantPaymentTransactionInfo(paymentDate.minusMonths(1)))
        .build()
      val payment = newPayment.copy(data = paymentData)
      val contractWithPayments = ContractWithPayments(contract, List(payment))
      val newState = invokeStage(contractWithPayments)
      newState.entry.payments.length shouldBe 1
      val receiptInfoList = newState.entry.payments.head.data.getReceiptsList
      receiptInfoList.size() shouldBe 2
      val agencyReceipt = receiptInfoList.get(0)
      val commissionPrepaymentReceipt = receiptInfoList.get(1)
      val expectedPayment = payment.copy(
        data = paymentData.toBuilder
          .addReceipts(agencyReceiptInfo(agencyReceiptId, agencyReceipt.getCreateTime, payment))
          .addReceipts(
            commissionPrepaymentReceiptInfo(
              commissionPrepaymentReceiptId,
              commissionPrepaymentReceipt.getCreateTime,
              payment
            )
          )
          .build()
      )
      newState.entry.payments shouldBe Seq(expectedPayment)
    }

    "not create receipt without tenant payments" in {
      handleMocks()
      val paymentDate = TodayDate.plusDays(11)
      val contract = createContract(paymentDate, paymentDate.getDayOfMonth)
      val payment = createPayment(contract, paymentDate, paymentDate.plusMonths(1).minusDays(1))
      val contractWithPayments = ContractWithPayments(contract, List(payment))
      val newState = invokeStage(contractWithPayments)
      newState.entry.payments shouldBe Seq(payment)
      newState.entry.visitTime.isEmpty shouldBe true
    }

    "create agency receipt and full commission receipt" in {
      val agencyReceiptId = uidGen().next
      val commissionReceiptId = uidGen().next
      handleMocks(
        agencyReceiptId = agencyReceiptId,
        commissionReceiptId = commissionReceiptId,
        agencyReceiptCount = 1,
        commissionReceiptCount = 1
      )
      val paymentDate = DateTime.now().plusMonths(2)
      val contract = createContract(paymentDate, paymentDate.getDayOfMonth)
      val newPayment = createPayment(contract, paymentDate, paymentDate.plusMonths(1).minusDays(1))
      val paymentData = newPayment.data.toBuilder
        .addTenantTransactions(createTenantPaymentTransactionInfo(paymentDate.plusMonths(2)))
        .build()
      val payment = newPayment.copy(data = paymentData)
      val contractWithPayments = ContractWithPayments(contract, List(payment))
      val newState = invokeStage(contractWithPayments)

      newState.entry.payments.length shouldBe 1
      val receiptInfoList = newState.entry.payments.head.data.getReceiptsList
      receiptInfoList.size() shouldBe 2
      val agencyReceipt = receiptInfoList.get(0)
      val commissionReceipt = receiptInfoList.get(1)
      val expectedPayment = payment.copy(
        data = paymentData.toBuilder
          .addReceipts(agencyReceiptInfo(agencyReceiptId, agencyReceipt.getCreateTime, payment))
          .addReceipts(commissionReceiptInfo(commissionReceiptId, commissionReceipt.getCreateTime, payment))
          .build(),
        `type` = PaymentType.Rent
      )
      newState.entry.payments shouldBe Seq(expectedPayment)
      newState.entry.visitTime.isEmpty shouldBe true
    }

    "create agency receipt, penalty receipt and full commission receipt" in {
      val agencyReceiptId = uidGen().next
      val commissionReceiptId = uidGen().next
      val penaltyId = uidGen().next
      handleMocks(
        agencyReceiptId = agencyReceiptId,
        commissionReceiptId = commissionReceiptId,
        penaltyId = penaltyId,
        agencyReceiptCount = 1,
        commissionReceiptCount = 1,
        penaltyReceiptCount = 1
      )
      val paymentDate = DateTime.now().plusMonths(2)
      val contract = createContract(paymentDate, paymentDate.getDayOfMonth)
      val newPayment = createPayment(contract, paymentDate, paymentDate.plusMonths(1).minusDays(1))
      val paymentData = newPayment.data.toBuilder
        .addTenantTransactions(createTenantPaymentTransactionInfo(paymentDate.plusMonths(2)))
        .setTenantPenaltyAmount(1000)
        .build()
      val payment = newPayment.copy(data = paymentData)
      val contractWithPayments = ContractWithPayments(contract, List(payment))
      val newState = invokeStage(contractWithPayments)

      newState.entry.payments.length shouldBe 1
      val receiptInfoList = newState.entry.payments.head.data.getReceiptsList
      receiptInfoList.size() shouldBe 3
      val agencyReceipt = receiptInfoList.get(0)
      val commissionReceipt = receiptInfoList.get(1)
      val penaltyReceipt = receiptInfoList.get(2)
      val expectedPayment = payment.copy(
        data = paymentData.toBuilder
          .addReceipts(agencyReceiptInfo(agencyReceiptId, agencyReceipt.getCreateTime, payment))
          .addReceipts(commissionReceiptInfo(commissionReceiptId, commissionReceipt.getCreateTime, payment))
          .addReceipts(penaltyReceiptInfo(penaltyId, penaltyReceipt.getCreateTime, payment))
          .build(),
        `type` = PaymentType.Rent
      )
      newState.entry.payments shouldBe Seq(expectedPayment)
      newState.entry.visitTime.isEmpty shouldBe true
    }

    "create agency receipt, commission prepayment receipt and second commission receipt" in {
      val agencyReceiptId = uidGen().next
      val commissionReceiptId = uidGen().next
      val commissionPrepaymentReceiptId = uidGen().next
      handleMocks(agencyReceiptId, commissionReceiptId, commissionPrepaymentReceiptId, "", 1, 1, 1, 0, false)
      val paymentDate = TodayDate.plusDays(11)
      val contract = createContract(paymentDate, paymentDate.getDayOfMonth)
      val newPayment = createPayment(contract, paymentDate, paymentDate.plusMonths(1).minusDays(1))
      val paymentData = newPayment.data.toBuilder
        .addTenantTransactions(createTenantPaymentTransactionInfo(paymentDate.minusMonths(1)))
        .build()
      val payment = newPayment.copy(data = paymentData)
      val contractWithPayments = ContractWithPayments(contract, List(payment))
      val newState = invokeStage(contractWithPayments)

      newState.entry.payments.length shouldBe 1
      val receiptInfoList = newState.entry.payments.head.data.getReceiptsList
      receiptInfoList.size() shouldBe 3
      val agencyReceipt = receiptInfoList.get(0)
      val commissionPrepaymentReceipt = receiptInfoList.get(1)
      val commissionReceipt = receiptInfoList.get(2)
      val expectedPayment = payment.copy(
        data = paymentData.toBuilder
          .addReceipts(agencyReceiptInfo(agencyReceiptId, agencyReceipt.getCreateTime, payment))
          .addReceipts(
            commissionPrepaymentReceiptInfo(
              commissionPrepaymentReceiptId,
              commissionPrepaymentReceipt.getCreateTime,
              payment
            )
          )
          .addReceipts(commissionReceiptInfo(commissionReceiptId, commissionReceipt.getCreateTime, payment))
          .build()
      )
      newState.entry.payments shouldBe Seq(expectedPayment)
      newState.entry.visitTime.isEmpty shouldBe true
    }

    "create payment with rescheduled visit time" in {
      val agencyReceiptId = uidGen().next
      val commissionPrepaymentReceiptId = uidGen().next
      handleMocks(
        agencyReceiptId = agencyReceiptId,
        commissionPrepaymentReceiptId = commissionPrepaymentReceiptId,
        agencyReceiptCount = 1,
        commissionPrepaymentReceiptCount = 1
      )
      val paymentDate = DateTime.now().plusMonths(2)
      var contract = createContract(paymentDate, paymentDate.getDayOfMonth)
      contract = contract.copy(
        data = contract.data.toBuilder.setNowMomentForTesting(DateTimeFormat.write(paymentDate.minusMonths(5))).build()
      )
      val newPayment = createPayment(contract, paymentDate, paymentDate.plusMonths(1).minusDays(1))
      val paymentData = newPayment.data.toBuilder
        .addTenantTransactions(createTenantPaymentTransactionInfo(paymentDate.minusMonths(2)))
        .build()
      val payment = newPayment.copy(data = paymentData)
      val contractWithPayments = ContractWithPayments(contract, List(payment))
      val newState = invokeStage(contractWithPayments)
      newState.entry.payments.length shouldBe 1
      val receiptInfoList = newState.entry.payments.head.data.getReceiptsList
      receiptInfoList.size() shouldBe 2
      val agencyReceipt = receiptInfoList.get(0)
      val commissionPrepaymentReceipt = receiptInfoList.get(1)
      val expectedPayment = payment.copy(
        data = paymentData.toBuilder
          .addReceipts(agencyReceiptInfo(agencyReceiptId, agencyReceipt.getCreateTime, payment))
          .addReceipts(
            commissionPrepaymentReceiptInfo(
              commissionPrepaymentReceiptId,
              commissionPrepaymentReceipt.getCreateTime,
              payment
            )
          )
          .build()
      )
      newState.entry.payments shouldBe Seq(expectedPayment)
      newState.entry.visitTime.get shouldBe payment.startTime.withDayOfMonth(1).plusMinutes(1)
    }

    "multiple creation of agency receipt, commission prepayment receipt and second commission receipt" in {
      val agencyReceiptId = uidGen().next
      val commissionReceiptId = uidGen().next
      val commissionPrepaymentReceiptId = uidGen().next
      handleMocks()
      val paymentDate = TodayDate.plusDays(11)
      val contract = createContract(paymentDate, paymentDate.getDayOfMonth)
      val payment = createPayment(contract, paymentDate, paymentDate.plusMonths(1).minusDays(1))
      val paymentData = payment.data.toBuilder
        .addTenantTransactions(createTenantPaymentTransactionInfo(paymentDate.plusMonths(1)))
        .addReceipts(agencyReceiptInfo(agencyReceiptId, DateTimeFormat.write(DateTime.now()), payment))
        .addReceipts(
          commissionPrepaymentReceiptInfo(
            commissionPrepaymentReceiptId,
            DateTimeFormat.write(DateTime.now()),
            payment
          )
        )
        .addReceipts(commissionReceiptInfo(commissionReceiptId, DateTimeFormat.write(DateTime.now()), payment))
        .build()
      val expectedPayment = payment.copy(data = paymentData)
      val contractWithPayments = ContractWithPayments(contract, List(expectedPayment))
      val newState = invokeStage(contractWithPayments)
      newState.entry.payments shouldBe Seq(expectedPayment)
      newState.entry.visitTime.isEmpty shouldBe true
    }
  }

  // scalastyle:off method.length
  private def handleMocks(
    agencyReceiptId: String = "",
    commissionReceiptId: String = "",
    commissionPrepaymentReceiptId: String = "",
    penaltyId: String = "",
    agencyReceiptCount: Int = 0,
    commissionReceiptCount: Int = 0,
    commissionPrepaymentReceiptCount: Int = 0,
    penaltyReceiptCount: Int = 0,
    isFull: Boolean = true
  ) {
    val user = User(
      uid = Random.nextInt(),
      userId = uidGen().next,
      phone = None,
      name = None,
      surname = None,
      patronymic = None,
      fullName = None,
      email = Some(uidGen().next),
      passportVerificationStatus = PassportVerificationStatus.Unknown,
      roommateLinkId = None,
      roommateLinkExpirationTime = None,
      assignedFlats = Map.empty,
      data = UserData.getDefaultInstance,
      createTime = null,
      updateTime = null,
      visitTime = None
    )
    (userDaoMock
      .findByUidOpt(_: Long, _: Boolean)(_: Traced))
      .expects(*, *, *)
      .anyNumberOfTimes()
      .returning(Future.successful(Some(user)))
    (palmaRentUserClient
      .get(_: String)(_: Traced))
      .expects(*, *)
      .anyNumberOfTimes()
      .returning(Future.successful(Some(RentUser(paymentData = Some(PaymentData())))))
    handleCashBoxMocks(
      agencyReceiptId,
      "rent_agent_payment_id",
      agencyReceiptCount,
      CashboxPayment.PaymentType.PAYMENT_TYPE_CARD
    )
    handleCashBoxMocks(
      commissionReceiptId,
      "rent_commission_payment_id",
      commissionReceiptCount,
      if (isFull) CashboxPayment.PaymentType.PAYMENT_TYPE_CARD else CashboxPayment.PaymentType.PAYMENT_TYPE_PREPAYMENT
    )
    handleCashBoxMocks(
      commissionPrepaymentReceiptId,
      "rent_commission_prepayment_id",
      commissionPrepaymentReceiptCount,
      CashboxPayment.PaymentType.PAYMENT_TYPE_CARD
    )
    handleCashBoxMocks(
      penaltyId,
      "rent_penalty_id",
      penaltyReceiptCount,
      CashboxPayment.PaymentType.PAYMENT_TYPE_CARD
    )
  }
  // scalastyle:on method.length

  private def handleCashBoxMocks(
    receiptId: String,
    requisiteName: String,
    createReceiptsRequestCount: Int,
    paymentType: CashboxPayment.PaymentType
  ): Unit = {
    (cashBoxClientMock
      .searchReceipt(_: String, _: String)(_: Traced))
      .expects(*, *, *)
      .anyNumberOfTimes()
      .returning(Future.successful(SearchReceiptResponse.getDefaultInstance))
    if (receiptId.nonEmpty) {
      (cashBoxClientMock
        .createReceipt(_: Receipt)(_: Traced))
        .expects(where {
          case (receipt, _) =>
            receipt.getAdditionalUserRequisite.getName == requisiteName
            !receipt.getPaymentsList.isEmpty && receipt.getPaymentsList.get(0).getPaymentType == paymentType
        })
        .repeat(createReceiptsRequestCount)
        .returning(Future.successful(Receipt.newBuilder().setReceiptId(receiptId).build()))
    }
  }

  def receiptInfo(
    receiptId: String,
    receiptRentType: ReceiptRentType,
    createTime: DateTime,
    amount: Long
  ): ReceiptInfo =
    ReceiptInfo
      .newBuilder()
      .setReceiptId(receiptId)
      .setReceiptRentType(receiptRentType)
      .setReceiptIncomeType(ReceiptIncomeType.INCOME)
      .setAmount(amount)
      .setCreateTime(createTime)
      .build()

  def agencyReceiptInfo(receiptId: String, createTime: Timestamp, payment: Payment): ReceiptInfo =
    receiptInfo(receiptId, ReceiptRentType.AGENT, DateTimeFormat.read(createTime), payment.data.getOwnerPaymentAmount)

  def commissionReceiptInfo(receiptId: String, createTime: Timestamp, payment: Payment): ReceiptInfo =
    receiptInfo(
      receiptId,
      ReceiptRentType.COMMISSION,
      DateTimeFormat.read(createTime),
      payment.data.getTenantPaymentAmount - payment.data.getOwnerPaymentAmount - payment.data.getTenantPenaltyAmount
    )

  def commissionPrepaymentReceiptInfo(receiptId: String, createTime: Timestamp, payment: Payment): ReceiptInfo =
    receiptInfo(
      receiptId,
      ReceiptRentType.COMMISSION_PREPAYMENT,
      DateTimeFormat.read(createTime),
      payment.data.getTenantPaymentAmount - payment.data.getOwnerPaymentAmount - payment.data.getTenantPenaltyAmount
    )

  def penaltyReceiptInfo(receiptId: String, createTime: Timestamp, payment: Payment): ReceiptInfo =
    receiptInfo(
      receiptId,
      ReceiptRentType.PENALTY,
      DateTimeFormat.read(createTime),
      payment.data.getTenantPenaltyAmount
    )
}
