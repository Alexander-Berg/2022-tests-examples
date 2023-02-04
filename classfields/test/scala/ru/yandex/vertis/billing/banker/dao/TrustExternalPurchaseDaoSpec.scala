package ru.yandex.vertis.billing.banker.dao

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.banker.dao.impl.jdbc.JdbcSpecTemplate
import ru.yandex.vertis.billing.banker.dao.PaymentSystemDao.{PaymentRequestRecord, RefundPaymentRequestRecord}
import ru.yandex.vertis.billing.banker.dao.TrustExternalPurchaseDao._
import ru.yandex.vertis.billing.banker.dao.TrustExternalPurchaseDaoSpec._
import ru.yandex.vertis.billing.banker.dao.util.{
  CleanableJdbcAccountDao,
  CleanableJdbcTrustExternalPurchaseDao,
  CleanablePaymentSystemDao
}
import ru.yandex.vertis.billing.banker.model.PaymentRequest.EmptyForm
import ru.yandex.vertis.billing.banker.model.gens._
import ru.yandex.vertis.billing.banker.model.{Account, Epoch, PaymentRequestId, RefundPaymentRequestId}
import ru.yandex.vertis.billing.trust.exceptions.TrustException.BasketMarkupError
import ru.yandex.vertis.billing.trust.model.PaymentStatus.{
  `3dsStarted`,
  Authorized,
  Cleared,
  NotAuthorized,
  NotStarted,
  Started
}
import ru.yandex.vertis.billing.trust.model.{PaymentStatus, RefundStatus}

import java.time.Instant
import scala.concurrent.Future

trait TrustExternalPurchaseDaoSpec
  extends AnyWordSpec
  with Matchers
  with AsyncSpecBase
  with JdbcSpecTemplate
  with BeforeAndAfterEach
  with BeforeAndAfterAll {

  protected def accountDao: CleanableJdbcAccountDao
  protected def paymentSystemDao: CleanablePaymentSystemDao
  protected def purchaseDao: CleanableJdbcTrustExternalPurchaseDao

  override def beforeEach(): Unit = {
    createAccount(AccountId).toTry.get
    createPaymentRequest(AccountId, PrId).toTry.get
  }

  override def afterEach(): Unit = {
    purchaseDao.clean().toTry.get
    paymentSystemDao.cleanPayments().toTry.get
    paymentSystemDao.cleanRequests().toTry.get
    accountDao.clean().toTry.get
  }

  "TrustExternalPurchaseDao" should {

    "update record payment status to started" in {
      val record = PurchaseRecord(PurchaseToken, PrId, TrustPaymentId, NotStarted, YandexPassportUid, PaymentMode)
      purchaseDao.insert(record).futureValue

      purchaseDao.updateStatus(PurchaseToken, StatusPatch.Started(NotStarted, epochNow())).futureValue

      val updatedRecord = purchaseDao.getByToken(PurchaseToken).futureValue.get
      updatedRecord.paymentStatus shouldBe Started
      updatedRecord.startTs should not be empty
    }

    "update record payment status to 3ds started" in {
      val record = PurchaseRecord(PurchaseToken, PrId, TrustPaymentId, NotStarted, YandexPassportUid, PaymentMode)
      purchaseDao.insert(record).futureValue

      purchaseDao.updateStatus(PurchaseToken, StatusPatch.Started3Ds(NotStarted, epochNow())).futureValue

      val updatedRecord = purchaseDao.getByToken(PurchaseToken).futureValue.get
      updatedRecord.paymentStatus shouldBe `3dsStarted`
      updatedRecord.startTs should not be empty
    }

    "update record payment status to authorized" in {
      val record = PurchaseRecord(PurchaseToken, PrId, TrustPaymentId, Started, YandexPassportUid, PaymentMode)
      purchaseDao.insert(record).futureValue

      purchaseDao.updateStatus(PurchaseToken, StatusPatch.Authorized(Started, epochNow(), ReceiptUrl)).futureValue

      val updatedRecord = purchaseDao.getByToken(PurchaseToken).futureValue.get
      updatedRecord.paymentStatus shouldBe Authorized
      updatedRecord.receiptUrl shouldBe Some(ReceiptUrl)
      updatedRecord.paymentTs should not be empty
    }

    "update record payment status to not authorized" in {
      val record = PurchaseRecord(PurchaseToken, PrId, TrustPaymentId, Started, YandexPassportUid, PaymentMode)
      purchaseDao.insert(record).futureValue

      purchaseDao.updateStatus(PurchaseToken, StatusPatch.NotAuthorized(Started, epochNow(), AuthErrorCode)).futureValue

      val updatedRecord = purchaseDao.getByToken(PurchaseToken).futureValue.get
      updatedRecord.paymentStatus shouldBe NotAuthorized
      updatedRecord.authErrorCode shouldBe Some(AuthErrorCode)
      updatedRecord.cancelTs should not be empty
    }

    "update record payment status to cleared" in {
      val record = PurchaseRecord(PurchaseToken, PrId, TrustPaymentId, Authorized, YandexPassportUid, PaymentMode)
      purchaseDao.insert(record).futureValue

      purchaseDao.updateStatus(PurchaseToken, StatusPatch.Cleared(Authorized, epochNow())).futureValue

      val updatedRecord = purchaseDao.getByToken(PurchaseToken).futureValue.get
      updatedRecord.paymentStatus shouldBe Cleared
      updatedRecord.clearTs should not be empty
    }

    "update record payment status to refunded" in {
      val record = PurchaseRecord(PurchaseToken, PrId, TrustPaymentId, Cleared, YandexPassportUid, PaymentMode)
      purchaseDao.insert(record).futureValue

      purchaseDao.updateStatus(PurchaseToken, StatusPatch.Refunded(Cleared, epochNow())).futureValue

      val updatedRecord = purchaseDao.getByToken(PurchaseToken).futureValue.get
      updatedRecord.paymentStatus shouldBe PaymentStatus.Refunded
      updatedRecord.refundTs should not be empty
    }

    "update plus bonus when action completes" in {
      val withdraw = Some(200L)
      val topup = Some(100L)
      val record = PurchaseRecord(
        purchaseToken = PurchaseToken,
        prId = PrId,
        trustPaymentId = TrustPaymentId,
        paymentStatus = Started,
        yandexUid = YandexPassportUid,
        paymentMode = PaymentMode,
        plusWithdrawAmount = withdraw
      )
      purchaseDao.insert(record).futureValue
      val action = Future.unit

      purchaseDao.updatePlusBonus(record.purchaseToken, None, topup)(action).futureValue

      val updatedRecord = purchaseDao.getByToken(PurchaseToken).futureValue
      updatedRecord.flatMap(_.plusWithdrawAmount) shouldBe None
      updatedRecord.flatMap(_.plusTopupAmount) shouldBe topup
    }

    "not update plus bonus when action fails" in {
      val withdraw = Some(200L)
      val topup = Some(100L)
      val record = PurchaseRecord(
        purchaseToken = PurchaseToken,
        prId = PrId,
        trustPaymentId = TrustPaymentId,
        paymentStatus = Started,
        yandexUid = YandexPassportUid,
        paymentMode = PaymentMode,
        plusWithdrawAmount = withdraw
      )
      purchaseDao.insert(record).futureValue
      val action = Future.failed(BasketMarkupError("unknown_error", Some("PaymentOrderNotFound")))

      val exception = purchaseDao.updatePlusBonus(record.purchaseToken, None, topup)(action).failed.futureValue
      exception shouldBe an[BasketMarkupError]

      val updatedRecord = purchaseDao.getByToken(PurchaseToken).futureValue
      updatedRecord.flatMap(_.plusWithdrawAmount) shouldBe withdraw
      updatedRecord.flatMap(_.plusTopupAmount) shouldBe None
    }

    "fail to update record payment status twice" in {
      val record = PurchaseRecord(PurchaseToken, PrId, TrustPaymentId, Started, YandexPassportUid, PaymentMode)
      purchaseDao.insert(record).futureValue

      val result = for {
        _ <- purchaseDao.updateStatus(PurchaseToken, StatusPatch.Authorized(Started, epochNow(), ReceiptUrl))
        _ <- purchaseDao.updateStatus(PurchaseToken, StatusPatch.Authorized(Started, epochNow(), ReceiptUrl))
      } yield ()

      result.failed.futureValue shouldBe an[UpdateAlreadyAppliedException]
      val updatedRecord = purchaseDao.getByToken(PurchaseToken).futureValue
      updatedRecord.map(_.paymentStatus) shouldBe Some(Authorized)
    }

    "fail to update record payment status when another patch is applied" in {
      val record = PurchaseRecord(PurchaseToken, PrId, TrustPaymentId, Started, YandexPassportUid, PaymentMode)
      purchaseDao.insert(record).futureValue

      val result = for {
        _ <- purchaseDao.updateStatus(PurchaseToken, StatusPatch.Authorized(Started, epochNow(), ReceiptUrl))
        _ <- purchaseDao.updateStatus(PurchaseToken, StatusPatch.Cleared(Started, epochNow()))
      } yield ()

      result.failed.futureValue shouldBe an[UpdateConflictException]
    }

    "get record by purchase token" in {
      val record = PurchaseRecord(PurchaseToken, PrId, TrustPaymentId, Started, YandexPassportUid, PaymentMode)
      purchaseDao.insert(record).futureValue

      val result = purchaseDao.getByToken(PurchaseToken).futureValue

      result should not be empty
      result.get.purchaseToken shouldBe PurchaseToken
    }

    "get record by payment request id" in {
      val record = PurchaseRecord(PurchaseToken, PrId, TrustPaymentId, Started, YandexPassportUid, PaymentMode)
      purchaseDao.insert(record).futureValue

      val result = purchaseDao.getById(PrId).futureValue

      result should not be empty
      result.get.prId shouldBe PrId
    }

    "get records by external transaction id" in {
      val records = List(
        PurchaseRecord("token_1", "pr_1", "tp_1", NotAuthorized, 1L, PaymentMode, Some(ExternalTransactionId)),
        PurchaseRecord("token_2", "pr_2", "tp_2", NotAuthorized, 1L, PaymentMode, None),
        PurchaseRecord("token_3", "pr_3", "tp_3", Authorized, 1L, PaymentMode, Some(ExternalTransactionId))
      )
      records.foreach { record =>
        createPaymentRequest(AccountId, record.prId).futureValue
        purchaseDao.insert(record).futureValue
      }

      val result = purchaseDao.getByExternalId(ExternalTransactionId).futureValue

      result.map(_.purchaseToken) should contain theSameElementsAs List("token_1", "token_3")
    }

    "filter by payment status and [since, until) interval" in {
      val records = List(
        PurchaseRecord("t_1", "pr_id_1", "tp_1", Authorized, YandexPassportUid, PaymentMode, epoch = Some(100501)),
        PurchaseRecord("t_2", "pr_id_2", "tp_2", NotAuthorized, YandexPassportUid, PaymentMode, epoch = Some(100502)),
        PurchaseRecord("t_3", "pr_id_3", "tp_3", Authorized, YandexPassportUid, PaymentMode, epoch = Some(100503)),
        PurchaseRecord("t_4", "pr_id_4", "tp_4", NotAuthorized, YandexPassportUid, PaymentMode, epoch = Some(100504)),
        PurchaseRecord("t_5", "pr_id_5", "tp_5", Authorized, YandexPassportUid, PaymentMode, epoch = Some(100505)),
        PurchaseRecord("t_6", "pr_id_6", "tp_6", NotAuthorized, YandexPassportUid, PaymentMode, epoch = Some(100506)),
        PurchaseRecord("t_7", "pr_id_7", "tp_7", Authorized, YandexPassportUid, PaymentMode, epoch = Some(100507)),
        PurchaseRecord("t_8", "pr_id_8", "tp_8", NotAuthorized, YandexPassportUid, PaymentMode, epoch = Some(100508))
      )
      records.foreach { record =>
        createPaymentRequest(AccountId, record.prId).futureValue
        purchaseDao.insert(record).futureValue
      }

      val result = purchaseDao
        .findAllByStatuses(
          statuses = Set(Authorized),
          since = 100501,
          until = 100505
        )
        .futureValue

      result shouldBe List(
        PurchaseRecord("t_1", "pr_id_1", "tp_1", Authorized, YandexPassportUid, PaymentMode, epoch = Some(100501)),
        PurchaseRecord("t_3", "pr_id_3", "tp_3", Authorized, YandexPassportUid, PaymentMode, epoch = Some(100503))
      )
    }

    "filter by few payment statuses" in {
      val records = List(
        PurchaseRecord("t_1", "pr_id_1", "tp_1", Authorized, YandexPassportUid, PaymentMode, epoch = Some(100501)),
        PurchaseRecord("t_2", "pr_id_2", "tp_2", NotAuthorized, YandexPassportUid, PaymentMode, epoch = Some(100502)),
        PurchaseRecord("t_3", "pr_id_3", "tp_3", Started, YandexPassportUid, PaymentMode, epoch = Some(100503))
      )
      records.foreach { record =>
        createPaymentRequest(AccountId, record.prId).futureValue
        purchaseDao.insert(record).futureValue
      }

      val result = purchaseDao.findAllByStatuses(statuses = Set(Authorized, NotAuthorized)).futureValue

      result shouldBe List(
        PurchaseRecord("t_1", "pr_id_1", "tp_1", Authorized, YandexPassportUid, PaymentMode, epoch = Some(100501)),
        PurchaseRecord("t_2", "pr_id_2", "tp_2", NotAuthorized, YandexPassportUid, PaymentMode, epoch = Some(100502))
      )
    }

    "handle all possible payment status values" in {
      PaymentStatus.values.zipWithIndex.foreach { case (status, i) =>
        val record = PurchaseRecord(s"token_$i", s"pr_ir_$i", s"tp_id_$i", status, YandexPassportUid, PaymentMode)

        val result = for {
          _ <- createPaymentRequest(AccountId, record.prId)
          _ <- purchaseDao.insert(record)
          result <- purchaseDao.getById(record.prId)
        } yield result

        result.futureValue.map(_.paymentStatus) shouldBe Some(status)
      }
    }

    "insert a new refund record" in {
      purchaseDao
        .insert(
          PurchaseRecord(PurchaseToken, PrId, TrustPaymentId, PaymentStatus.Cleared, YandexPassportUid, PaymentMode)
        )
        .futureValue
      createRefundPaymentRequest(AccountId, PrId, RefundPrId).futureValue
      val record = PurchaseRefundRecord(
        trustRefundId = TrustRefundId,
        purchaseToken = PurchaseToken,
        refundPrId = RefundPrId,
        refundStatus = RefundStatus.WaitForNotification,
        fiscalReceiptUrl = None,
        epoch = Some(Instant.now.toEpochMilli)
      )

      purchaseDao.upsertRefund(record).futureValue

      val insertedRecord = purchaseDao.getRefundById(TrustRefundId).futureValue
      insertedRecord shouldBe Some(record)
    }

    "update an existing refund record" in {
      purchaseDao
        .insert(
          PurchaseRecord(PurchaseToken, PrId, TrustPaymentId, PaymentStatus.Cleared, YandexPassportUid, PaymentMode)
        )
        .futureValue
      createRefundPaymentRequest(AccountId, PrId, RefundPrId).futureValue
      val record =
        PurchaseRefundRecord(
          trustRefundId = TrustRefundId,
          purchaseToken = PurchaseToken,
          refundPrId = RefundPrId,
          refundStatus = RefundStatus.WaitForNotification,
          fiscalReceiptUrl = None,
          epoch = None
        )
      purchaseDao.upsertRefund(record).futureValue
      val update = record.copy(refundStatus = RefundStatus.Success, fiscalReceiptUrl = Some(FiscalReceiptUrl))

      purchaseDao.upsertRefund(update).futureValue

      val updatedRecord = purchaseDao.getRefundById(TrustRefundId).futureValue
      updatedRecord.map(_.refundStatus) shouldBe Some(RefundStatus.Success)
      updatedRecord.flatMap(_.fiscalReceiptUrl) shouldBe Some(FiscalReceiptUrl)
      updatedRecord.flatMap(_.epoch) should not be empty
    }

    "get by refund payment request id" in {
      purchaseDao
        .insert(
          PurchaseRecord(PurchaseToken, PrId, TrustPaymentId, PaymentStatus.Cleared, YandexPassportUid, PaymentMode)
        )
        .futureValue
      createRefundPaymentRequest(AccountId, PrId, RefundPrId).futureValue
      val record =
        PurchaseRefundRecord(
          trustRefundId = TrustRefundId,
          purchaseToken = PurchaseToken,
          refundPrId = RefundPrId,
          refundStatus = RefundStatus.WaitForNotification,
          fiscalReceiptUrl = None,
          epoch = Some(Instant.now.toEpochMilli)
        )
      purchaseDao.upsertRefund(record).futureValue

      val result = purchaseDao.getRefundByPrId(RefundPrId).futureValue

      result shouldBe Some(record)
    }
  }

  private def createAccount(accountId: String) = accountDao.upsert(Account(accountId, accountId))

  private def createPaymentRequest(accountId: String, prId: PaymentRequestId) = {
    val source = paymentRequestSourceGen(PaymentRequestSourceParams(account = Some(accountId))).next
    paymentSystemDao.insertRequest(
      PaymentRequestRecord(
        id = prId,
        method = "card",
        source = source,
        form = EmptyForm(prId)
      )
    )
  }

  private def createRefundPaymentRequest(
      accountId: String,
      prId: PaymentRequestId,
      refundPrId: RefundPaymentRequestId) = {
    val source = refundRequestSourceGen(RefundRequestSourceParams(refundFor = Some(prId))).next
    paymentSystemDao.insertRequest(
      RefundPaymentRequestRecord(
        id = refundPrId,
        method = "card",
        account = accountId,
        source = source
      )
    )
  }

  private def epochNow(): Epoch = Instant.now().toEpochMilli
}

object TrustExternalPurchaseDaoSpec {
  val YandexPassportUid = 22296196L
  val PaymentMode = "web_paymentt"
  val AccountId = "user:123"
  val PurchaseToken = "9d6eb0d01365b4714439768b728518eb"
  val TrustPaymentId = "62502568b955d7531210d072"
  val ExternalTransactionId = "user:71137095-f36d2c57ed71e42faf0f97401611a389-1648473346946"
  val ReceiptUrl = "https://receipt_url"
  val AuthErrorCode = "transaction_not_permitted"
  val PrId = "5fe73dd6-9f3d-4b9e-aef5-663f44a7d328"
  val RefundPrId = "615761e6-1a17-468c-b370-e7407016c937"
  val TrustRefundId = "6218966ab955d77d3e53020a"
  val FiscalReceiptUrl = s"https://trust-test.yandex.ru/checks/$PurchaseToken/receipts/$TrustRefundId"
}
