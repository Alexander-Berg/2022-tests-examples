package ru.yandex.vertis.billing.banker.payment.impl

import org.scalamock.scalatest.MockFactory
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.vertis.banker.dao.impl.jdbc.JdbcSpecTemplate
import ru.yandex.vertis.billing.banker.dao.impl.jdbc.{
  GlobalJdbcAccountTransactionDao,
  JdbcAccountDao,
  JdbcPaymentSystemDao,
  PaymentSystemJdbcAccountTransactionDao
}
import ru.yandex.vertis.billing.banker.dao.util.{CleanableJdbcAccountTransactionDao, CleanableJdbcPaymentSystemDao}
import ru.yandex.vertis.billing.banker.model.PaymentRequest.{Context, EmptyForm, Targets}
import ru.yandex.vertis.billing.banker.model.State.{BytesNotificationSource, Types}
import ru.yandex.vertis.billing.banker.model._
import ru.yandex.vertis.billing.banker.model.gens.{paymentRequestSourceGen, PaymentRequestSourceParams, Producer}
import ru.yandex.vertis.billing.banker.payment.payload.DefaultPayGatePayloadExtractor
import ru.yandex.vertis.billing.banker.service.AccountTransactionService
import ru.yandex.vertis.billing.banker.service.PaymentSystemSupport.MethodFilter
import ru.yandex.vertis.billing.banker.service.impl.{PaymentSystemServiceImpl, RefundHelperServiceImpl}
import ru.yandex.vertis.billing.banker.service.refund.processor.FreeOfChargeRefundProcessor
import ru.yandex.vertis.billing.banker.util.UserContext

import java.sql.SQLIntegrityConstraintViolationException
import scala.util.{Failure, Success}

/**
  * Runnable specs on [[FreeOfChargePaymentSupport]]
  *
  * @author alex-kovalenko
  */
class FreeOfChargePaymentSupportSpec
  extends Matchers
  with MockFactory
  with AnyWordSpecLike
  with JdbcSpecTemplate
  with BeforeAndAfterEach {

  private val accountDao =
    new JdbcAccountDao(database)

  private val paymentsDao =
    new JdbcPaymentSystemDao(database, PaymentSystemIds.FreeOfCharge) with CleanableJdbcPaymentSystemDao

  private val accountTransactionsDao =
    new GlobalJdbcAccountTransactionDao(database) with CleanableJdbcAccountTransactionDao

  private val psTransactionsDao =
    new PaymentSystemJdbcAccountTransactionDao(database, PaymentSystemIds.FreeOfCharge)
      with CleanableJdbcAccountTransactionDao

  private val refundHelperService = new RefundHelperServiceImpl(
    database,
    accountTransactionsDao,
    FreeOfChargeRefundProcessor,
    psTransactionsDao,
    paymentsDao
  )

  private val psService =
    new PaymentSystemServiceImpl(database, paymentsDao, refundHelperService, DefaultPayGatePayloadExtractor)

  private val transactionsService = mock[AccountTransactionService]

  val paymentSupport =
    new FreeOfChargePaymentSupport(psService, transactionsService)

  implicit private val context: UserContext =
    UserContext("FreeOfChargePaymentSupportSpec", "I am human. Trust me :)")

  val methodId = "free"
  val customer = "1"

  val account =
    accountDao.upsert(Account("FreeOfChargePaymentSupportSpec", customer)).futureValue.id
  val amount = 100L

  val sourceParams = PaymentRequestSourceParams(
    id = None,
    account = Some(account),
    amount = Some(amount),
    withReceipt = Some(true)
  )

  val method = PaymentMethod(PaymentSystemIds.FreeOfCharge, methodId)

  override protected def beforeEach(): Unit = {
    accountTransactionsDao.clean()
    psTransactionsDao.clean()
    paymentsDao.cleanPayments().futureValue
    paymentsDao.cleanRequests().futureValue
    super.beforeEach()
  }

  "FreeOfChargePaymentSupport" should {
    "return id" in {
      paymentSupport.psId should be(PaymentSystemIds.FreeOfCharge)
    }

    "return free method" in {
      val source = PaymentRequest.Source(
        account,
        1L,
        Payload.Empty,
        PaymentRequest.Options(),
        None,
        Context(Targets.Wallet),
        None,
        None
      )
      val methods = paymentSupport.getMethods(customer, MethodFilter.ForSource(source)).futureValue
      methods.map(_.id) should (have size 1 and contain(methodId))
    }

    "fail request if have no acc" in {
      paymentSupport
        .request(
          customer,
          methodId,
          paymentRequestSourceGen(sourceParams.copy(account = Some("-"))).next
        )
        .toTry match {
        case Failure(_: SQLIntegrityConstraintViolationException) => info("Done")
        case other => fail(s"Unexpected $other")
      }
    }

    "do request with existing acc" in {
      val requestId = "rq1"
      paymentSupport
        .request(
          customer,
          methodId,
          paymentRequestSourceGen(sourceParams.copy(id = Some(requestId))).next
        )
        .toTry should matchPattern { case Success(EmptyForm(`requestId`, None, _)) =>
      }
      val pr = paymentSupport.getPaymentRequest(customer, requestId).futureValue
      pr.id shouldBe requestId
      pr.method shouldBe PaymentMethod(PaymentSystemIds.FreeOfCharge, methodId)
      pr.source.account shouldBe account
      pr.source.amount shouldBe amount

      pr.state match {
        case Some(payment) =>
          payment.id shouldBe requestId
          payment.requestId shouldBe Some(requestId)
          payment.`type` shouldBe Types.Incoming
          payment.account shouldBe account
          payment.amount shouldBe amount
        case other => fail(s"Unexpected $other")
      }
    }

    "fail to parse" in {
      paymentSupport.parse(BytesNotificationSource("", "-", new Array[Byte](0))).toTry should matchPattern {
        case Failure(_: UnsupportedOperationException) =>
      }
    }

    "fail to refund unexistent payment" in {
      paymentSupport.fullRefund(customer, "not-exist", None, None).toTry should matchPattern {
        case Failure(_: NoSuchElementException) =>
      }
    }

    "do nothing when trying to refund request without payment" in {
      val source = paymentRequestSourceGen(sourceParams.copy(context = Some(Context(Targets.Purchase)))).next
      val request = psService.createPaymentRequest(method, source, emptyForm, emptyAction).futureValue
      paymentSupport.getPaymentRequest(customer, request.id).futureValue.state shouldBe empty
      intercept[IllegalArgumentException] {
        paymentSupport.fullRefund(customer, request.id, None, None).await
      }
    }

  }
}
