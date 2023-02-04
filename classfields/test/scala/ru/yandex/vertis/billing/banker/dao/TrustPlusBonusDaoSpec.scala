package ru.yandex.vertis.billing.banker.dao

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.banker.dao.impl.jdbc.JdbcSpecTemplate
import ru.yandex.vertis.billing.banker.dao.TrustPlusBonusDao.{
  TopupPatch,
  TopupRecord,
  TopupRefundPatch,
  TopupRefundRecord
}
import ru.yandex.vertis.billing.banker.dao.util.{
  CleanableJdbcAccountDao,
  CleanableJdbcAccountTransactionDao,
  CleanableJdbcTrustPlusBonusDao,
  CleanablePaymentSystemDao
}
import ru.yandex.vertis.billing.banker.model.gens.{incomingRqGen, Producer, RequestParams}
import ru.yandex.vertis.billing.banker.model.{Account, AccountTransactionId, AccountTransactions}
import ru.yandex.vertis.billing.trust.model.{PaymentStatus, PurchaseToken, RefundStatus, TrustRefundId}

import scala.util.Random

trait TrustPlusBonusDaoSpec
  extends AnyWordSpec
  with Matchers
  with AsyncSpecBase
  with JdbcSpecTemplate
  with BeforeAndAfterEach {

  protected def trustPlusBonusDao: CleanableJdbcTrustPlusBonusDao
  protected def accountDao: CleanableJdbcAccountDao
  protected def accountTransactionDao: CleanableJdbcAccountTransactionDao
  protected def paymentSystemDao: CleanablePaymentSystemDao

  override def afterEach(): Unit = {
    trustPlusBonusDao.clean().futureValue
    paymentSystemDao.cleanPayments().futureValue
    paymentSystemDao.cleanRequests().futureValue
    accountTransactionDao.clean().futureValue
    accountTransactionDao.cleanLocks().futureValue
    accountDao.clean().futureValue
  }

  "TrustPlusBonusDao topup methods" should {

    "insert and get a record" in {
      val record = createTopupRecord(purchaseToken = "some_purchase_token")
      initAccountAndPaymentRequest(record)

      trustPlusBonusDao.insertTopup(record).futureValue

      val inserted = trustPlusBonusDao.getTopup("some_purchase_token").futureValue.get
      inserted shouldBe record.copy(epoch = inserted.epoch)
    }

    "update epoch on insert" in {
      val record = createTopupRecord(purchaseToken = "some_purchase_token")
      initAccountAndPaymentRequest(record)

      trustPlusBonusDao.insertTopup(record).futureValue

      val inserted = trustPlusBonusDao.getTopup("some_purchase_token").futureValue.get
      inserted.epoch should not be empty
    }

    "update status" in {
      val record = createTopup(
        purchaseToken = "some_purchase_token",
        paymentStatus = PaymentStatus.NotStarted
      )

      trustPlusBonusDao
        .updateTopup("some_purchase_token", TopupPatch.Status(PaymentStatus.Started))
        .futureValue

      val updated = trustPlusBonusDao.getTopup("some_purchase_token").futureValue.get
      updated shouldBe record.copy(paymentStatus = PaymentStatus.Started, epoch = updated.epoch)
    }

    "update epoch on record's update" in {
      val record = createTopup(
        purchaseToken = "some_purchase_token",
        paymentStatus = PaymentStatus.NotStarted
      )

      trustPlusBonusDao
        .updateTopup("some_purchase_token", TopupPatch.Status(PaymentStatus.Started))
        .futureValue

      val updated = trustPlusBonusDao.getTopup("some_purchase_token").futureValue.get
      updated.epoch.get > record.epoch.get shouldBe true
    }

    "get record by parent payment " in {
      val record = createTopup(parentPaymentId = Some("8#some_parent_transaction"))

      trustPlusBonusDao
        .getTopupByParentTransaction("8#some_parent_transaction")
        .futureValue
        .get shouldBe record
    }

    "get records by account " in {
      val record1 = createTopup(accountId = "some_account")
      val record2 = createTopup(accountId = "some_account")
      createTopup(accountId = "other_account")

      trustPlusBonusDao
        .getTopupsByAccount("some_account")
        .futureValue
        .toSet shouldBe Set(record1, record2)
    }

    "get records by status " in {
      val record1 = createTopup(paymentStatus = PaymentStatus.Started)
      val record2 = createTopup(paymentStatus = PaymentStatus.NotStarted)
      createTopup(paymentStatus = PaymentStatus.Cleared)

      trustPlusBonusDao
        .getTopupsByStatuses(Set(PaymentStatus.Started, PaymentStatus.NotStarted))
        .futureValue
        .toSet shouldBe Set(record1, record2)
    }
  }

  "TrustPlusBonusDao refund methods" should {

    "insert and get a refund record" in {
      val record = createRefundRecord(trustRefundId = "some_trust_refund_id")
      createTopup(purchaseToken = record.purchaseToken)
      trustPlusBonusDao.insertRefund(record).futureValue

      val inserted = trustPlusBonusDao.getRefund("some_trust_refund_id").futureValue.get
      inserted shouldBe record.copy(epoch = inserted.epoch)
    }

    "update epoch on insert refund" in {
      val record = createRefundRecord(trustRefundId = "some_trust_refund_id")
      createTopup(purchaseToken = record.purchaseToken)
      trustPlusBonusDao.insertRefund(record).futureValue

      val inserted = trustPlusBonusDao.getRefund("some_trust_refund_id").futureValue.get
      inserted.epoch should not be empty
    }

    "update refund status" in {
      val record = createRefund(
        trustRefundId = "some_trust_refund_id",
        refundStatus = RefundStatus.WaitForNotification
      )

      trustPlusBonusDao
        .updateRefund("some_trust_refund_id", TopupRefundPatch.Status(RefundStatus.Failed))
        .futureValue

      val updated = trustPlusBonusDao.getRefund("some_trust_refund_id").futureValue.get
      updated shouldBe record.copy(refundStatus = RefundStatus.Failed, epoch = updated.epoch)
    }

    "update epoch on refund's update" in {
      val record = createRefund(
        trustRefundId = "some_trust_refund_id",
        refundStatus = RefundStatus.WaitForNotification
      )

      trustPlusBonusDao
        .updateRefund("some_trust_refund_id", TopupRefundPatch.Status(RefundStatus.Failed))
        .futureValue

      val updated = trustPlusBonusDao.getRefund("some_trust_refund_id").futureValue.get
      updated.epoch.get > record.epoch.get shouldBe true
    }

    "get refund by topup" in {
      val record = createRefund(purchaseToken = "some_purchase_token")

      trustPlusBonusDao
        .getRefundsByTopup("some_purchase_token")
        .futureValue shouldBe List(record)
    }
  }

  private def initAccountAndPaymentRequest(record: TopupRecord) = {
    accountDao.upsert(Account(record.accountId, record.accountId)).futureValue
    val accountTransactionId =
      record.parentTransactionId.map(AccountTransactionId.parse(_, AccountTransactions.Incoming))
    accountTransactionDao
      .execute(incomingRqGen(RequestParams(id = accountTransactionId, account = Some(record.accountId))).next)
      .futureValue
  }

  private def createTopup(
      purchaseToken: PurchaseToken = s"purchaseToken${Random.nextLong()}",
      parentPaymentId: Option[String] = Some(s"8#${Random.nextLong()}"),
      accountId: String = s"accountId${Random.nextLong()}",
      paymentStatus: PaymentStatus = PaymentStatus.Cleared) = {
    val record = createTopupRecord(purchaseToken, parentPaymentId, accountId, paymentStatus)
    initAccountAndPaymentRequest(record)
    trustPlusBonusDao.insertTopup(record).futureValue
    trustPlusBonusDao.getTopup(record.purchaseToken).futureValue.get
  }

  private def createRefund(
      trustRefundId: TrustRefundId = s"trustRefundId${Random.nextLong()}",
      purchaseToken: PurchaseToken = s"purchaseToken${Random.nextLong()}",
      refundStatus: RefundStatus = RefundStatus.Success) = {
    val record = createRefundRecord(trustRefundId, purchaseToken, refundStatus)
    createTopup(purchaseToken = record.purchaseToken)
    trustPlusBonusDao.insertRefund(record).futureValue
    trustPlusBonusDao.getRefund(record.trustRefundId).futureValue.get
  }

  private def createTopupRecord(
      purchaseToken: PurchaseToken = s"purchaseToken${Random.nextLong()}",
      parentPaymentId: Option[String] = Some(s"8#${Random.nextLong()}"),
      accountId: String = s"accountId${Random.nextLong()}",
      paymentStatus: PaymentStatus = PaymentStatus.Cleared) =
    TopupRecord(
      purchaseToken = purchaseToken,
      trustPaymentId = "62960107910d391ca253cee4",
      accountId = accountId,
      yandexUid = 12312,
      paymethodId = "yandex_account-w/da4b2475-f88f-59b0-a62b-3a59c578a8d9",
      amount = 1000,
      parentTransactionId = parentPaymentId,
      paymentStatus = paymentStatus,
      epoch = None
    )

  private def createRefundRecord(
      trustRefundId: TrustRefundId = s"trustRefundId${Random.nextLong()}",
      purchaseToken: PurchaseToken = s"purchaseToken${Random.nextLong()}",
      refundStatus: RefundStatus = RefundStatus.Success) =
    TopupRefundRecord(
      trustRefundId = trustRefundId,
      purchaseToken = purchaseToken,
      amount = 1000,
      refundStatus = refundStatus,
      epoch = None
    )
}
