package ru.yandex.vertis.billing.banker.dao

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.banker.dao.impl.jdbc.JdbcSpecTemplate
import ru.yandex.vertis.billing.banker.dao.RecurrentPaymentDao.{
  RecurrentPayment,
  RecurrentPaymentRequest,
  RecurrentPaymentStatus
}
import ru.yandex.vertis.billing.banker.dao.util.CleanableJdbcRecurrentPaymentDao
import ru.yandex.vertis.billing.banker.model.AccountTransaction.PushStatuses
import ru.yandex.vertis.billing.banker.model.gens.{recurrentPaymentSourceGen, Producer}
import ru.yandex.vertis.billing.banker.model.{
  AccountTransactions,
  ExternalTransactionId,
  HashAccountTransactionId,
  PaymentSystemIds
}

import java.util.UUID

trait RecurrentPaymentDaoSpec
  extends AnyWordSpec
  with Matchers
  with AsyncSpecBase
  with JdbcSpecTemplate
  with BeforeAndAfterEach {

  protected def recurrentPaymentDao: CleanableJdbcRecurrentPaymentDao

  override def afterEach(): Unit = {
    recurrentPaymentDao.clean().futureValue
  }

  "RecurrentPaymentDao request methods" should {

    "insert and get a request record" in {
      val request = createRecurrentPaymentRequest("transaction-id")

      recurrentPaymentDao.insertRequest(request).futureValue

      val inserted = recurrentPaymentDao.getRequest("transaction-id").futureValue.get
      inserted shouldBe request.copy(epoch = inserted.epoch)
    }

    "update status" in {
      val request = createRecurrentPaymentRequest("transaction-id")
      recurrentPaymentDao.insertRequest(request).futureValue

      recurrentPaymentDao.updateStatus(request.externalId, RecurrentPaymentStatus.Failure).futureValue

      val updated = recurrentPaymentDao.getRequest(request.externalId).futureValue
      updated.map(_.status) shouldBe Some(RecurrentPaymentStatus.Failure)
    }

    "update push status" in {
      val request = createRecurrentPaymentRequest("transaction-id")
      recurrentPaymentDao.insertRequest(request).futureValue

      recurrentPaymentDao.updatePushStatus(request.externalId, PushStatuses.Failed).futureValue

      val updated = recurrentPaymentDao.getRequest(request.externalId).futureValue
      updated.map(_.pushStatus) shouldBe Some(PushStatuses.Failed)
    }

    "find by statuses" in {
      val request1 = createRecurrentPaymentRequest("request-1").copy(status = RecurrentPaymentStatus.InProgress)
      val request2 = createRecurrentPaymentRequest("request-2").copy(status = RecurrentPaymentStatus.Success)
      val request3 = createRecurrentPaymentRequest("request-3").copy(status = RecurrentPaymentStatus.InProgress)
      recurrentPaymentDao.insertRequest(request1).futureValue
      recurrentPaymentDao.insertRequest(request2).futureValue
      recurrentPaymentDao.insertRequest(request3).futureValue

      val result = recurrentPaymentDao.findByStatus(RecurrentPaymentStatus.InProgress).futureValue

      result.map(_.externalId) should contain theSameElementsAs List(request1.externalId, request3.externalId)
    }

    "find by push statuses" in {
      val request1 =
        createRecurrentPaymentRequest("request-1")
          .copy(status = RecurrentPaymentStatus.InProgress, pushStatus = PushStatuses.Ready)
      val request2 =
        createRecurrentPaymentRequest("request-2")
          .copy(status = RecurrentPaymentStatus.Failure, pushStatus = PushStatuses.Ready)
      val request3 =
        createRecurrentPaymentRequest("request-3")
          .copy(status = RecurrentPaymentStatus.Failure, pushStatus = PushStatuses.Ok)
      recurrentPaymentDao.insertRequest(request1).futureValue
      recurrentPaymentDao.insertRequest(request2).futureValue
      recurrentPaymentDao.insertRequest(request3).futureValue

      val result = recurrentPaymentDao.findByPushStatus(RecurrentPaymentStatus.Failure, PushStatuses.Ready).futureValue

      result.map(_.externalId) should contain theSameElementsAs List(request2.externalId)
    }
  }

  "RecurrentPaymentDao payment methods" should {

    "insert and get payment records" in {
      val externalId = UUID.randomUUID().toString
      val request = createRecurrentPaymentRequest(externalId)
      recurrentPaymentDao.insertRequest(request).futureValue
      val payment1 = createRecurrentPayment(externalId, 1, "wallet")
      val payment2 = createRecurrentPayment(externalId, 2, "card-x1")
      val payment3 = createRecurrentPayment(externalId, 3, "card-x2")

      recurrentPaymentDao.insertPayment(payment1).futureValue
      recurrentPaymentDao.insertPayment(payment2).futureValue
      recurrentPaymentDao.insertPayment(payment3).futureValue

      val inserted = recurrentPaymentDao.findPaymentsByExternalId(externalId).futureValue
      inserted.map(_.copy(epoch = None)) should contain theSameElementsAs List(payment1, payment2, payment3)
    }

    "upsert payment record" in {
      val externalId = UUID.randomUUID().toString
      val request = createRecurrentPaymentRequest(externalId)
      recurrentPaymentDao.insertRequest(request).futureValue
      val payment = createRecurrentPayment(externalId, 1, "card-x123")

      val record = recurrentPaymentDao.upsertPayment(payment).futureValue
      val expected = record.copy(
        status = RecurrentPaymentStatus.Failure,
        paymentRequestId = Some("prId"),
        accountTransactionId = Some(HashAccountTransactionId("xyz", AccountTransactions.Withdraw)),
        errorCode = Some("error_code"),
        errorDescription = Some("error_description")
      )
      val updatedRecord = recurrentPaymentDao.upsertPayment(expected).futureValue

      val actualRecords = recurrentPaymentDao.findPaymentsByExternalId(externalId).futureValue
      actualRecords should contain theSameElementsAs List(updatedRecord)
      expected.copy(epoch = None) shouldBe updatedRecord.copy(epoch = None)
    }
  }

  private def createRecurrentPaymentRequest(externalId: ExternalTransactionId) = RecurrentPaymentRequest(
    externalId = externalId,
    user = "user:123",
    yandexUid = 123L,
    psId = PaymentSystemIds.Trust,
    source = recurrentPaymentSourceGen().next,
    status = RecurrentPaymentStatus.InProgress,
    pushStatus = PushStatuses.Ready
  )

  private def createRecurrentPayment(externalId: ExternalTransactionId, attempt: Int, paymentMethodId: String) =
    RecurrentPayment(
      externalId = externalId,
      attempt = attempt,
      paymentMethodId = paymentMethodId,
      status = RecurrentPaymentStatus.InProgress,
      accountTransactionId = None,
      paymentRequestId = None,
      errorCode = None,
      errorDescription = None
    )
}
