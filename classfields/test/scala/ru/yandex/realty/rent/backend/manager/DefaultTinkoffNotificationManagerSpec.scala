package ru.yandex.realty.rent.backend.manager

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.clients.tinkoff.eacq.{PaymentNotification, TinkoffEACQCredentials}
import ru.yandex.realty.rent.dao.{InvalidPaymentNotificationsDao, PaymentDao, PeriodDao, RentContractDao}
import ru.yandex.realty.rent.model.enums.{
  AggregatedMeterReadingsStatus,
  BillStatus,
  ContractStatus,
  PaymentConfirmationStatus,
  PaymentStatus,
  PaymentType,
  PeriodType,
  ReceiptStatus
}
import ru.yandex.realty.rent.model.house.services
import ru.yandex.realty.rent.model.house.services.Period
import ru.yandex.realty.rent.model.{ContractParticipant, ContractPayment, Payment, RentContract}
import ru.yandex.realty.rent.proto.model.contract.ContractData
import ru.yandex.realty.rent.proto.model.house.service.periods.PeriodData
import ru.yandex.realty.rent.proto.model.payment.TransactionStatusNamespace.TransactionStatus
import ru.yandex.realty.rent.proto.model.payment.{PaymentData, TenantPaymentTransactionInfo}
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.{AsyncSpecBase, SpecBase}
import ru.yandex.vertis.util.time.DateTimeUtil

import scala.concurrent.Future

/**
  * @author azakharov
  */
@RunWith(classOf[JUnitRunner])
class DefaultTinkoffNotificationManagerSpec extends SpecBase with AsyncSpecBase {

  implicit val trace = Traced.empty

  private val paymentDao: PaymentDao = mock[PaymentDao]
  private val rentContractDao: RentContractDao = mock[RentContractDao]
  private val periodDao: PeriodDao = mock[PeriodDao]
  private val invalidPaymentNotificationsDao: InvalidPaymentNotificationsDao = mock[InvalidPaymentNotificationsDao]
  private val terminalCredentials = new TinkoffEACQCredentials("Duv0ARpx4kldvbt11", "bx8g20njpd0hp4s70k")
  private val manager: TinkoffNotificationManager =
    new DefaultTinkoffNotificationManager(
      paymentDao,
      rentContractDao,
      periodDao,
      Seq(terminalCredentials),
      invalidPaymentNotificationsDao
    )

  private val paymentId = "5rPdmr5Y"
  private val transactionId = 1234567321L
  private val tenantAmount = 5000000L
  private val payment = Payment(
    id = paymentId,
    contractId = "123",
    `type` = PaymentType.Rent,
    isPaidOutUnderGuarantee = false,
    paymentDate = ISODateTimeFormat.dateParser().parseDateTime("2021-02-20"),
    startTime = ISODateTimeFormat.dateParser().parseDateTime("2021-02-20"),
    endTime = ISODateTimeFormat.dateParser().parseDateTime("2021-03-20"),
    status = PaymentStatus.New,
    data = PaymentData
      .newBuilder()
      .setTenantPaymentAmount(tenantAmount)
      .addTenantTransactions(
        TenantPaymentTransactionInfo
          .newBuilder()
          .setAmount(tenantAmount)
          .setPaymentUrl("https://oplata")
          .setTransactionId(transactionId.toString)
          .setStatus(TransactionStatus.NEW)
      )
      .build(),
    createTime = DateTimeUtil.now(),
    updateTime = DateTimeUtil.now()
  )

  private val notification = PaymentNotification(
    TerminalKey = terminalCredentials.key,
    OrderId = paymentId,
    Success = true,
    Status = "CONFIRMED",
    PaymentId = transactionId,
    Amount = tenantAmount,
    Token = "75529b0c355b09b6a148f6249142067e3fd322b9e7a66b15e692b85c58c18ff9",
    ErrorCode = Some("0"),
    CardId = Some(718044),
    Pan = Some("430000**0777"),
    ExpDate = Some("1122"),
    RebillId = None,
    DATA = None
  )

  val rentContract = RentContract(
    "5623487213491",
    "",
    Some(""),
    "",
    ContractParticipant(None, None, None, None),
    ContractParticipant(None, None, None, None),
    None,
    ContractStatus.Unknown,
    ContractData.getDefaultInstance,
    new DateTime(),
    new DateTime(),
    None,
    1
  )

  private val periodId: String = "8222197328723"
  private val houseServicePaymentId = "8Yu584ng38G"
  private val period: Period = services.Period(
    periodId = periodId,
    contractId = rentContract.contractId,
    periodType = PeriodType.Regular,
    period = new DateTime("2021-10-01"),
    meterReadingsStatus = AggregatedMeterReadingsStatus.NotSent,
    billStatus = BillStatus.ShouldBePaid,
    receiptStatus = ReceiptStatus.NotSent,
    paymentConfirmationStatus = PaymentConfirmationStatus.NotSent,
    paymentId = Some(houseServicePaymentId),
    data = PeriodData.getDefaultInstance,
    createTime = DateTimeUtil.now(),
    updateTime = DateTimeUtil.now()
  )
  private val houseServicePayment = Payment(
    id = houseServicePaymentId,
    contractId = rentContract.contractId,
    `type` = PaymentType.HouseServices,
    paymentDate = ISODateTimeFormat.dateParser().parseDateTime("2021-11-01"),
    startTime = ISODateTimeFormat.dateParser().parseDateTime("2021-10-01"),
    endTime = ISODateTimeFormat.dateParser().parseDateTime("2021-10-30"),
    status = PaymentStatus.New,
    isPaidOutUnderGuarantee = false,
    data = PaymentData
      .newBuilder()
      .setTenantPaymentAmount(tenantAmount)
      .addTenantTransactions(
        TenantPaymentTransactionInfo
          .newBuilder()
          .setAmount(tenantAmount)
          .setPaymentUrl("https://oplata")
          .setTransactionId(transactionId.toString)
          .setStatus(TransactionStatus.NEW)
      )
      .build(),
    createTime = DateTimeUtil.now(),
    updateTime = DateTimeUtil.now()
  )

  private val houseServicePaymentNotification = PaymentNotification(
    TerminalKey = terminalCredentials.key,
    OrderId = houseServicePaymentId,
    Success = true,
    Status = "CONFIRMED",
    PaymentId = transactionId,
    Amount = tenantAmount,
    Token = "5a9ed82401833c4b63300f6495cf0a137b823758633b88d091a0f49a29ebd90b",
    ErrorCode = Some("0"),
    CardId = Some(718044),
    Pan = Some("430000**0777"),
    ExpDate = Some("1122"),
    RebillId = None,
    DATA = None
  )

  "DefaultTinkoffNotificationManager" should {
    "process valid notification and update state" in {
      (paymentDao
        .get(_: String)(_: Traced))
        .expects(paymentId, *)
        .returning(Future.successful(Some(payment)))

      (rentContractDao
        .updateContractWithPayment(_: String, _: String)(_: ContractPayment => ContractPayment)(_: Traced))
        .expects(*, *, *, *)
        .returning(Future.successful(ContractPayment(rentContract, payment)))

      val res = manager.processNotification(notification).futureValue
      res shouldBe "OK"
    }

    "process valid house service notification and update state" in {
      (paymentDao
        .get(_: String)(_: Traced))
        .expects(houseServicePaymentId, *)
        .returning(Future.successful(Some(houseServicePayment)))

      (rentContractDao
        .updateContractWithPayment(_: String, _: String)(_: ContractPayment => ContractPayment)(_: Traced))
        .expects(*, *, *, *)
        .returning(Future.successful(ContractPayment(rentContract, houseServicePayment)))

      (periodDao
        .findByPaymentId(_: String)(_: Traced))
        .expects(houseServicePaymentId, *)
        .returning(Future.successful(period))

      (periodDao
        .update(_: String)(_: Period => Period)(_: Traced))
        .expects(*, *, *)
        .returning(Future.successful(period))

      val res = manager.processNotification(houseServicePaymentNotification).futureValue
      res shouldBe "OK"
    }

    "log notification with invalid terminalKey" in {
      val badNotification = notification.copy(TerminalKey = "BAD_TERMINAL_KEY")
      (invalidPaymentNotificationsDao
        .insert(_: PaymentNotification, _: String)(_: Traced))
        .expects(badNotification, "unknown terminal", *)
        .returning(Future.successful(1L))
      interceptCause[PaymentNotificationErrorException] {
        manager.processNotification(badNotification).futureValue
      }
    }

    "log notification with invalid token" in {
      val badNotification = notification.copy(Token = "BAD_TOKEN")
      (invalidPaymentNotificationsDao
        .insert(_: PaymentNotification, _: String)(_: Traced))
        .expects(badNotification, "invalid token", *)
        .returning(Future.successful(1L))
      interceptCause[PaymentNotificationErrorException] {
        manager.processNotification(badNotification).futureValue
      }
    }

    "ignore notification with invalid amount" in {
      (paymentDao
        .get(_: String)(_: Traced))
        .expects(paymentId, *)
        .returning(Future.successful(Some(payment)))
      val badNotification = {
        val n = notification.copy(Amount = 1234567)
        n.copy(Token = terminalCredentials.makeTokenFor(n))
      }
      (invalidPaymentNotificationsDao
        .insert(_: PaymentNotification, _: String)(_: Traced))
        .expects(badNotification, "payment amount mismatch", *)
        .returning(Future.successful(1L))
      interceptCause[PaymentNotificationErrorException] {
        manager.processNotification(badNotification).futureValue
      }
    }

    "ignore notification with unknown transaction id" in {
      val paymentWithoutTransactions =
        payment.copy(data = payment.data.toBuilder.clearTenantTransactions().build())
      (paymentDao
        .get(_: String)(_: Traced))
        .expects(paymentId, *)
        .returning(Future.successful(Some(paymentWithoutTransactions)))
      (invalidPaymentNotificationsDao
        .insert(_: PaymentNotification, _: String)(_: Traced))
        .expects(notification, "transaction not found", *)
        .returning(Future.successful(1L))
      interceptCause[PaymentNotificationErrorException] {
        manager.processNotification(notification).futureValue
      }
    }
  }
}
