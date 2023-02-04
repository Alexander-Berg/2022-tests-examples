package ru.yandex.vertis.billing.banker.dao

import java.sql.SQLException
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.banker.model.CommonModel.OpaquePayload
import ru.yandex.vertis.billing.banker.dao.PaymentSystemDao.StateRecord.Patch
import ru.yandex.vertis.billing.banker.dao.PaymentSystemDao.{
  PaymentRequestRecord,
  RefundPaymentRequestRecord,
  RequestFilter,
  StateFilter,
  StateRecord
}
import ru.yandex.vertis.billing.banker.model.State.{StateStatuses, Statuses}
import ru.yandex.vertis.billing.banker.model.PaymentRequest.{
  Context,
  EmptyForm,
  Options,
  ReceiptData,
  ReceiptGood,
  Source,
  Targets
}
import ru.yandex.vertis.billing.banker.model.gens.{
  paymentRequestGen,
  refundRequestGen,
  requestReceiptGen,
  PaymentPayloadGen,
  PaymentRequestParams,
  PaymentRequestSourceParams,
  Producer,
  RefundPayloadGen
}
import ru.yandex.vertis.billing.banker.model.{
  Account,
  Payload,
  PaymentRequest,
  PaymentRequestId,
  PaymentSystemMethodId,
  Raw,
  RefundPaymentRequest,
  State
}
import ru.yandex.vertis.billing.banker.util.CollectionUtils.RichTraversableLike
import ru.yandex.vertis.billing.banker.util.DateTimeUtils
import spray.json.JsObject

import scala.concurrent.Future

/**
  * Specs for [[PaymentSystemDao]]
  *
  * @author alesavin
  */
trait PaymentSystemDaoSpec extends AnyWordSpec with Matchers with AsyncSpecBase {

  def accounts: AccountDao
  def payments: PaymentSystemDao

  "PaymentSystemDao" should {

    val Customer = "1"
    val Acc = "1001"

    var allowedMethod: Option[PaymentSystemMethodId] = None

    val prId: PaymentRequestId = "test"

    "has no payments on start" in {
      payments.getStates(StateFilter.All).futureValue.isEmpty shouldBe true
    }

    "has active methods" in {
      val mrs = payments.getMethods.futureValue
      mrs.find(_.isEnabled) should not be None
      allowedMethod = mrs.find(_.isEnabled).map(_.id)
    }

    "fail if upsert payment request for non-exist account" in {
      intercept[SQLException] {
        payments
          .insertRequest(
            PaymentRequestRecord(
              prId,
              "card",
              Source(Acc, 1L, PaymentPayloadGen.next, Options(), None, Context(Targets.Wallet), None, None),
              PaymentRequest.UrlForm("test", "http://")
            )
          )
          .await
      }
    }

    "fail to create payment request for exist account and non-exist method" in {
      val a = Account(Acc, Customer)
      (accounts.upsert(a).toTry should be).a(Symbol("Success"))

      intercept[SQLException] {
        payments
          .insertRequest(
            PaymentRequestRecord(
              prId,
              "non-exist",
              Source(Acc, 1L, PaymentPayloadGen.next, Options(), None, Context(Targets.Wallet), None, None),
              PaymentRequest.UrlForm("test", "http://")
            )
          )
          .await
      }
    }

    "pass to create payment request for exist account and exist method" in {
      (payments
        .insertRequest(
          PaymentRequestRecord(
            prId,
            allowedMethod.get,
            Source(
              Acc,
              1L,
              PaymentPayloadGen.next,
              Options(),
              Some(ReceiptData(Seq(ReceiptGood("name", 1, 1L)), Some("email"))),
              Context(Targets.Wallet),
              None,
              None
            ),
            PaymentRequest.UrlForm("test", "http://")
          )
        )
        .toTry should be).a(Symbol("Success"))
    }

    "have payment request with provided id" in {
      val request = payments.getPaymentRequests(RequestFilter.ForIds(prId)).futureValue.exactlyOne
      request.id should be(prId)
      request.method should be(allowedMethod.get)
      request.source.account should be(Acc)
    }

    "not have payment request with provided id from getRefundRequests" in {
      val request = payments.getRefundRequests(Seq(RequestFilter.ForIds(prId)))
      request.futureValue.isEmpty shouldBe true
    }

    "fail to upsert TotalIncoming with tid - have no request id for than PS" in {
      val record = StateRecord("tid1", State.Types.TotalIncoming, Acc, 1000L, DateTimeUtils.now())

      intercept[SQLException] {
        payments.upsert(record).await
      }
    }

    "pass to upsert Incoming with request id" in {
      val record = StateRecord(
        prId,
        State.Types.Incoming,
        Acc,
        1000L,
        DateTimeUtils.now()
      )
      (payments.upsert(record).toTry should be).a(Symbol("Success"))
    }

    "update payment" in {
      val id = "update_id"
      val request = PaymentRequestRecord(
        id,
        allowedMethod.get,
        Source(Acc, 100L, Payload.Empty, Options(), None, Context(Targets.Wallet), None, None),
        EmptyForm(id)
      )
      payments.insertRequest(request).futureValue

      val payment = payments
        .upsert(StateRecord(id, State.Types.Incoming, Acc, 100L, DateTimeUtils.now(), raw = Raw.Empty))
        .toTry
        .get
      payment.epoch should not be empty

      payments
        .updateState(
          payment.id,
          None,
          Patch(status = Some(Statuses.Processed), stateStatus = Some(StateStatuses.Cancelled))
        )
        .futureValue

      val update1 = payments.getStates(StateFilter.ForId(id)).futureValue.head
      update1.stateStatus shouldBe StateStatuses.Cancelled
      update1.status shouldBe Statuses.Processed
      update1.epoch.get should be > payment.epoch.get

      payments.updateState(payment.id, payment.epoch, Patch(stateStatus = Some(StateStatuses.Refunded))).futureValue
      val update2 = payments.getStates(StateFilter.ForId(id)).futureValue.head
      update2 shouldBe update1

      payments
        .updateState(
          id,
          update1.epoch,
          Patch(
            stateStatus = Some(StateStatuses.Refunded)
          )
        )
        .futureValue
      val update3 = payments.getStates(StateFilter.ForId(id)).futureValue.head
      update3.stateStatus shouldBe StateStatuses.Refunded
      update3.epoch.get should be > update2.epoch.get
    }

    "get payment requests and payments by ids" in {
      val ids = Iterable("p1", "p2")

      Future
        .traverse(
          ids.map(id =>
            PaymentRequestRecord(
              id,
              allowedMethod.get,
              Source(Acc, 1L, PaymentPayloadGen.next, Options(), None, Context(Targets.Wallet), None, None),
              EmptyForm(id)
            )
          )
        )(payments.insertRequest)
        .futureValue

      val rs = payments.getPaymentRequests(RequestFilter.ForIds(ids)).futureValue
      rs should have size 2
      rs.map(_.id) should contain theSameElementsAs ids

      Future
        .traverse(
          ids.map(StateRecord(_, State.Types.Incoming, Acc, 100, DateTimeUtils.now()))
        )(payments.upsert)
        .futureValue

      val ps = payments.getStates(StateFilter.ForIds(ids)).futureValue
      ps should have size 2
      ps.map(_.id) should contain theSameElementsAs ids
    }

    "store/get payment request with targets" in {
      val params = PaymentRequestParams(
        source = PaymentRequestSourceParams(withPayGateContext = Some(false), withYandexUid = Some(false))
      )
      val requests = paymentRequestGen(params).next(10).map { r =>
        PaymentRequestRecord(
          r.id,
          allowedMethod.get,
          r.source.copy(account = Acc),
          r.form
        )
      }

      requests.foreach(r => payments.insertRequest(r).futureValue)

      requests.foreach { r =>
        val rr = payments.getPaymentRequests(RequestFilter.ForIds(r.id)).futureValue.exactlyOne
        rr.copy(epoch = None) shouldBe r
      }
    }

    val rrId: PaymentRequestId = "refund_test"
    val refundContext = Context(Targets.Wallet)

    "fail if upsert refund request for non-exist account" in {
      val nonExistingAccount = "12345678"
      intercept[SQLException] {
        payments
          .insertRequest(
            RefundPaymentRequestRecord(
              rrId,
              "card",
              nonExistingAccount,
              RefundPaymentRequest.Source(
                prId,
                10L,
                refundContext,
                RefundPayloadGen.next,
                Some(requestReceiptGen(10L).next)
              )
            )
          )
          .await
      }
    }

    "fail to create refund request for exist account and non-exist method" in {
      val a = Account(Acc, Customer)
      (accounts.upsert(a).toTry should be).a(Symbol("Success"))

      intercept[SQLException] {
        payments
          .insertRequest(
            RefundPaymentRequestRecord(
              rrId,
              "non-exist",
              Acc,
              RefundPaymentRequest.Source(
                prId,
                10L,
                refundContext,
                RefundPayloadGen.next,
                Some(requestReceiptGen(10L).next)
              )
            )
          )
          .await
      }
    }

    "pass to create refund request for exist account and exist method" in {
      (payments
        .insertRequest(
          RefundPaymentRequestRecord(
            rrId,
            allowedMethod.get,
            Acc,
            RefundPaymentRequest.Source(
              prId,
              10L,
              refundContext,
              RefundPayloadGen.next,
              Some(requestReceiptGen(10L).next)
            )
          )
        )
        .toTry should be).a(Symbol("Success"))
    }

    "have refund request with provided id" in {
      val request = payments.getRefundRequests(Seq(RequestFilter.ForIds(rrId))).futureValue.exactlyOne
      request.id should be(rrId)
      request.method should be(allowedMethod.get)
      request.account should be(Acc)
    }

    "not have refund request with provided id from getPaymentRequests" in {
      val request = payments.getPaymentRequests(RequestFilter.ForIds(rrId)).futureValue
      request.isEmpty shouldBe true
    }

    "pass to upsert Refund with request id" in {
      val record = StateRecord(
        rrId,
        State.Types.Refund,
        Acc,
        10L,
        DateTimeUtils.now()
      )
      (payments.upsert(record).toTry should be).a(Symbol("Success"))
    }

    "update refund" in {
      val id = "refund_update_id"
      val payload = Payload.RefundPayload("user", "comment", Some(JsObject()), OpaquePayload.RefundPayload.Reason.OTHER)
      val receiptData = ReceiptData(Seq(ReceiptGood("good", 1, 100L)))
      val source = RefundPaymentRequest.Source("update_id", 100L, refundContext, payload, Some(receiptData))
      val request = RefundPaymentRequestRecord(
        id,
        allowedMethod.get,
        Acc,
        source
      )
      payments.insertRequest(request).futureValue

      val payment = payments
        .upsert(
          StateRecord(
            id,
            State.Types.Refund,
            Acc,
            100L,
            DateTimeUtils.now(),
            raw = Raw.Empty
          )
        )
        .toTry
        .get
      payment.stateStatus shouldBe StateStatuses.Valid
      payment.status shouldBe Statuses.Created
      payment.epoch should not be empty

      payments
        .updateState(
          payment.id,
          None,
          Patch(status = Some(Statuses.Processed))
        )
        .futureValue

      val update1 = payments.getStates(StateFilter.ForId(id)).futureValue.head
      update1.stateStatus shouldBe StateStatuses.Valid
      update1.status shouldBe Statuses.Processed
      update1.epoch.get should be > payment.epoch.get

      payments
        .updateState(
          payment.id,
          payment.epoch,
          Patch(stateStatus = Some(StateStatuses.Valid))
        )
        .futureValue

      val update2 = payments.getStates(StateFilter.ForId(id)).futureValue.head
      update2 shouldBe update1

      payments
        .updateState(
          id,
          update1.epoch,
          Patch(stateStatus = Some(StateStatuses.Cancelled))
        )
        .futureValue
      val update3 = payments.getStates(StateFilter.ForId(id)).futureValue.head
      update3.stateStatus shouldBe StateStatuses.Cancelled
      update3.epoch.get should be > update2.epoch.get
    }

    "get refund requests and payments by ids" in {
      val ids = Iterable("refund_p1", "refund_p2")

      Future
        .traverse(
          ids.map { id =>
            val payload =
              Payload.RefundPayload("user", "comment", Some(JsObject()), OpaquePayload.RefundPayload.Reason.OTHER)
            val receiptData = ReceiptData(Seq(ReceiptGood("good", 1, 1L)))
            val source = RefundPaymentRequest.Source("update_id", 1L, refundContext, payload, Some(receiptData))
            RefundPaymentRequestRecord(
              id,
              allowedMethod.get,
              Acc,
              source
            )
          }
        )(payments.insertRequest)
        .futureValue

      val rs = payments.getRefundRequests(Seq(RequestFilter.ForIds(ids))).futureValue
      rs should have size 2
      rs.map(_.id) should contain theSameElementsAs ids

      Future
        .traverse(
          ids.map(StateRecord(_, State.Types.Refund, Acc, 1, DateTimeUtils.now()))
        )(payments.upsert)
        .futureValue

      val ps = payments.getStates(StateFilter.ForIds(ids)).futureValue
      ps should have size 2
      ps.map(_.id) should contain theSameElementsAs ids
    }

    "store/get refund request with targets" in {
      val refundedRequestId = "refunded_request_id"
      val requests = refundRequestGen().next(10).map { r =>
        RefundPaymentRequestRecord(
          r.id,
          allowedMethod.get,
          Acc,
          r.source.copy(refundFor = refundedRequestId)
        )
      }

      requests.foreach(r => payments.insertRequest(r).futureValue)

      requests.foreach { r =>
        val rr = payments.getRefundRequests(Seq(RequestFilter.ForIds(r.id))).futureValue.exactlyOne
        rr.copy(epoch = None) shouldBe r

        payments.getPaymentRequests(RequestFilter.ForIds(r.id)).futureValue.isEmpty shouldBe true
      }

      val refunds = payments.getRefundRequests(Seq(RequestFilter.RefundsFor(refundedRequestId))).futureValue
      refunds.map(_.copy(epoch = None)) should contain theSameElementsAs requests

      payments.getPaymentRequests(RequestFilter.RefundsFor(refundedRequestId)).futureValue.isEmpty shouldBe true
    }

    "store/get refund request with UpdateBefore filter" in {
      val refundedRequestId = "refunded_request_id_updated_before_filter"
      val requests = refundRequestGen().next(10).map { r =>
        RefundPaymentRequestRecord(
          r.id,
          allowedMethod.get,
          Acc,
          r.source.copy(refundFor = refundedRequestId)
        )
      }

      requests.foreach(r => payments.insertRequest(r).futureValue)

      val allRefunds = payments.getRefundRequests(Seq(RequestFilter.RefundsFor(refundedRequestId))).futureValue
      val epoches = allRefunds.flatMap(_.epoch).toSeq.sorted
      val firstEpoch = epoches.head - 5
      val lastEpoch = epoches.head - 5
      (firstEpoch to lastEpoch).foreach { epoch =>
        val expected = allRefunds.filter(_.epoch.get <= epoch)

        val actual = payments
          .getRefundRequests(
            Seq(
              RequestFilter.RefundsFor(refundedRequestId),
              RequestFilter.InRange(firstEpoch, epoch)
            )
          )
          .futureValue

        actual should contain theSameElementsAs expected
      }
    }

  }
}
