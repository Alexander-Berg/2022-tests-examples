package ru.yandex.vertis.billing.banker.dao

import com.google.common.base.Charsets
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.banker.dao.impl.jdbc.JdbcSpecTemplate
import ru.yandex.vertis.billing.banker.dao.ReceiptDao.ForId
import ru.yandex.vertis.billing.banker.model.gens.{
  receiptGen,
  AccountGen,
  Producer,
  ReceiptFromPaymentRequest,
  ReceiptFromTransaction,
  ReceiptSendFailDescriptionGen,
  ReceiptType
}
import ru.yandex.vertis.billing.banker.model.{Receipt, ReceiptSentStatuses, ReceiptStatuses}
import ru.yandex.vertis.billing.banker.service.ReceiptDeliveryService.SentReceiptResponse
import ru.yandex.vertis.billing.banker.util.CollectionUtils.RichTraversableLike
import ru.yandex.vertis.billing.banker.util.DateTimeUtils

import scala.io.Source
import scala.util.Failure

/**
  * @author ruslansd
  */
trait ReceiptDaoSpec extends AnyWordSpec with Matchers with JdbcSpecTemplate {

  def dao: ReceiptDao
  def accountDao: AccountDao

  private val account = AccountGen.next
  accountDao.upsert(account).futureValue

  private def checkStoreDuplicates(receiptType: ReceiptType) = {
    val receipt = receiptGen(receiptType).next.copy(accountId = account.id)
    dao.insert(receipt).futureValue
    dao.insert(receipt).toTry should matchPattern { case Failure(_) =>
    }
  }

  private def checkUpsert(receiptType: ReceiptType) = {
    val original = receiptGen(receiptType).next.copy(accountId = account.id)
    dao.insert(original).futureValue
    val changedContent = original.content.reverse
    val changed = original.copy(content = changedContent)
    dao.upsert(changed).futureValue
    val actual = dao.get(ForId(original.id)).futureValue.exactlyOne
    checkReceipts(actual.copy(createTime = None), changed)
  }

  "ReceiptDao" should {

    "return nothing on empty set" in {
      dao.get(ReceiptDao.ForId("12121")).futureValue.toList should matchPattern { case Nil =>
      }
    }

    "store receipts and provide them" in {
      val receipts = receiptGen()
        .next(10)
        .map(_.copy(accountId = account.id))
        .toList
      receipts.foreach { r =>
        dao.insert(r).futureValue
      }

      receipts.foreach { r =>
        val receipt = dao.get(ReceiptDao.ForId(r.id)).futureValue.exactlyOne
        checkReceipts(receipt, r)
      }
    }

    "not store duplicates with payment request receipts" in {
      checkStoreDuplicates(ReceiptFromPaymentRequest)
    }

    "not store duplicates with transaction receipts" in {
      checkStoreDuplicates(ReceiptFromTransaction)
    }

    "store receipt with different statuses" in {
      val active = receiptGen().next
        .copy(accountId = account.id, status = ReceiptStatuses.Active)
      val refund = active
        .copy(status = ReceiptStatuses.Refund, id = active.id + "active")
      dao.insert(active).futureValue
      dao.insert(refund).futureValue
    }

    "change status of receipt delivery" in {
      val receipt = receiptGen().next
        .copy(
          accountId = account.id,
          sent = None,
          sentStatus = ReceiptSentStatuses.Ready,
          failDescription = None
        )
      dao.insert(receipt).futureValue

      val ready = dao.get(ReceiptDao.ForId(receipt.id)).futureValue.exactlyOne
      ready.id shouldBe receipt.id
      ready.sentStatus shouldBe ReceiptSentStatuses.Ready
      ready.failDescription shouldBe None
      ready.isSent shouldBe false

      dao.sent(receipt.id, SentReceiptResponse(ReceiptSentStatuses.OK)).futureValue
      val success = dao.get(ReceiptDao.ForId(receipt.id)).futureValue.exactlyOne
      success.id shouldBe receipt.id
      success.sentStatus shouldBe ReceiptSentStatuses.OK
      success.failDescription shouldBe None
      success.isSent shouldBe true

      dao.sent(receipt.id, SentReceiptResponse(ReceiptSentStatuses.Fail)).futureValue
      val fail = dao.get(ReceiptDao.ForId(receipt.id)).futureValue.exactlyOne
      fail.id shouldBe receipt.id
      fail.sentStatus shouldBe ReceiptSentStatuses.Fail
      fail.failDescription shouldBe None
      fail.isSent shouldBe false

      val failDesc = Some(ReceiptSendFailDescriptionGen.next)
      dao.sent(receipt.id, SentReceiptResponse(ReceiptSentStatuses.Fail, failDesc)).futureValue
      val failWithDesc = dao.get(ReceiptDao.ForId(receipt.id)).futureValue.exactlyOne
      failWithDesc.id shouldBe receipt.id
      failWithDesc.sentStatus shouldBe ReceiptSentStatuses.Fail
      failWithDesc.failDescription shouldBe failDesc
      failWithDesc.isSent shouldBe false
    }

    "process old sent status correctly" in {
      val readyReceipt = receiptGen().next
        .copy(
          accountId = account.id,
          sent = Some(false),
          sentStatus = ReceiptSentStatuses.Ready,
          failDescription = None
        )
      dao.insert(readyReceipt).futureValue

      val ready = dao.get(ReceiptDao.ForId(readyReceipt.id)).futureValue.exactlyOne
      ready.id shouldBe readyReceipt.id
      ready.sentStatus shouldBe ReceiptSentStatuses.Ready
      ready.failDescription shouldBe None
      ready.isSent shouldBe false

      val successReceipt = receiptGen().next
        .copy(
          accountId = account.id,
          sent = Some(true),
          sentStatus = ReceiptSentStatuses.Ready,
          failDescription = None
        )
      dao.insert(successReceipt).futureValue

      val success = dao.get(ReceiptDao.ForId(successReceipt.id)).futureValue.exactlyOne
      success.id shouldBe successReceipt.id
      success.sentStatus shouldBe ReceiptSentStatuses.Ready
      success.failDescription shouldBe None
      success.isSent shouldBe true
    }

    "correctly set short url" in {
      val readyReceipt = receiptGen().next
        .copy(
          accountId = account.id,
          sent = Some(false),
          sentStatus = ReceiptSentStatuses.Ready,
          failDescription = None
        )
      dao.insert(readyReceipt).futureValue

      val ready = dao.get(ReceiptDao.ForId(readyReceipt.id)).futureValue.exactlyOne
      ready.id shouldBe readyReceipt.id
      ready.sentStatus shouldBe ReceiptSentStatuses.Ready
      ready.failDescription shouldBe None
      ready.isSent shouldBe false

      val shortUrl = "i am short url. trust me"

      dao.setShortUrl(readyReceipt.id, shortUrl).futureValue

      val success = dao.get(ReceiptDao.ForId(readyReceipt.id)).futureValue.exactlyOne
      success.id shouldBe readyReceipt.id
      success.shortUrl shouldBe Some(shortUrl)
      success.sentStatus shouldBe ReceiptSentStatuses.Ready
      success.failDescription shouldBe None
      success.isSent shouldBe false
    }

    "fetch with CreatedSince filter correctly" in {
      val timePoint = DateTimeUtils.now().getMillis
      val receipts = receiptGen()
        .next(10)
        .map(_.copy(accountId = account.id))
        .toList
      receipts.foreach { r =>
        dao.insert(r).futureValue
      }
      val result = dao.get(ReceiptDao.CreatedSince(timePoint)).futureValue
      result.map(_.id) should contain theSameElementsAs receipts.map(_.id)
    }

    "fetch with FirstCreatedSince filter correctly" in {
      val timePoint = DateTimeUtils.now().getMillis
      val receipts = receiptGen()
        .next(10)
        .map(_.copy(accountId = account.id))
        .toList
      receipts.foreach { r =>
        dao.insert(r).futureValue
      }
      val result = dao.get(ReceiptDao.FirstCreatedSince(timePoint)).futureValue
      result.map(_.id).head shouldBe receipts.map(_.id).head
    }

    "upsert correctly for payment system receipts" in {
      checkUpsert(ReceiptFromPaymentRequest)
    }

    "upsert correctly for transaction receipts" in {
      checkUpsert(ReceiptFromTransaction)
    }

    "get check url correctly" in {
      val receiptContent = Source
        .fromInputStream(
          this.getClass.getResourceAsStream("/receipt/test.json")
        )
        .mkString
        .getBytes(Charsets.UTF_8)

      val receipt = receiptGen().next.copy(content = receiptContent).copy(accountId = account.id)
      val paymentId = receipt.paymentId

      dao.insert(receipt).futureValue

      dao
        .getCheckUrl(paymentId)
        .futureValue
        .get shouldBe "https://greed-ts.paysys.yandex.net:8019/?n=238872&fn=9999068902000069&fpd=1800838529"
    }

  }

  private def checkReceipts(a: Receipt, b: Receipt) = {
    a.id shouldBe b.id
    a.psId shouldBe b.psId
    a.paymentId shouldBe b.paymentId
    a.accountId shouldBe b.accountId
    a.content should equal(b.content)
    a.email shouldBe b.email
    a.status shouldBe b.status
    a.sent shouldBe b.sent
  }

}
