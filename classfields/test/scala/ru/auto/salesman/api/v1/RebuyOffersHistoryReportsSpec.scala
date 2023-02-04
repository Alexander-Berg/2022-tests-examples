package ru.auto.salesman.api.v1

import java.io.ByteArrayOutputStream
import java.time.Instant
import java.util.UUID

import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.unmarshalling.{FromResponseUnmarshaller, Unmarshal}
import com.google.protobuf.{MessageLite, Timestamp, Value}
import org.joda.time.DateTime
import ru.auto.salesman.Task
import ru.auto.salesman.dao.impl.jdbc.user.JdbcSubscriptionDao._
import ru.auto.salesman.dao.slick.invariant.StaticQuery.interpolation
import ru.auto.salesman.model.user.ApiModel.{CreateTransactionResult, TransactionRequest}
import ru.auto.salesman.model.user.Subscription
import ru.auto.salesman.model.{
  DeprecatedDomain,
  DeprecatedDomains,
  ProductStatuses,
  TransactionId
}
import ru.yandex.vertis.banker.model.EventsModel.PaymentNotification
import zio.blocking.effectBlocking

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.reflect.ClassTag

class RebuyOffersHistoryReportsSpec extends JdbcProductServices {

  "TransactionService (when user already has offers-history-reports-10 and buys offers-history-reports-50 on top of it)" should {

    "leave offers-history-reports-10 in same state (active) as it was before buying offers-history-reports-50" in {

      cleanTransactions()

      val reports10TransactionId =
        makePayment("/transaction/create/offers-history-reports-10.json")
      val reports10PackageStateBeforeBuying50 =
        getVinHistoryPackage(reports10TransactionId)
      makePayment("/transaction/create/offers-history-reports-50.json")
      val reports10Package = getVinHistoryPackage(reports10TransactionId)

      reports10Package shouldBe reports10PackageStateBeforeBuying50
    }

    "set deadline ~= now + 365 days for offers-history-reports-10" in {
      cleanTransactions()

      val testStart = DateTime.now()
      val reports10TransactionId =
        makePayment("/transaction/create/offers-history-reports-10.json")
      val reports10Package = getVinHistoryPackage(reports10TransactionId)

      withClue(reports10Package) {
        reports10Package.deadline.getMillis shouldBe testStart
          .plusDays(365)
          .getMillis +- 1.day.toMillis
      }
    }

    "set deadline ~= now + 32 days for offers-history-reports-50" in {
      cleanTransactions()

      val testStart = DateTime.now()
      makePayment("/transaction/create/offers-history-reports-10.json")
      val reports50TransactionId =
        makePayment("/transaction/create/offers-history-reports-50.json")
      val reports50Package = getVinHistoryPackage(reports50TransactionId)
      withClue(reports50Package) {
        reports50Package.deadline.getMillis shouldBe testStart
          .plusDays(32)
          .getMillis +- 1.day.toMillis
      }
    }

    "set status = active for offers-history-reports-50" in {
      cleanTransactions()

      makePayment("/transaction/create/offers-history-reports-10.json")
      val reports50TransactionId =
        makePayment("/transaction/create/offers-history-reports-50.json")
      val reports50Package = getVinHistoryPackage(reports50TransactionId)
      withClue(reports50Package) {
        reports50Package.status shouldBe ProductStatuses.Active
      }
    }

    "set counter = 50 for offers-history-reports-50" in {
      cleanTransactions()

      makePayment("/transaction/create/offers-history-reports-10.json")
      val reports50TransactionId =
        makePayment("/transaction/create/offers-history-reports-50.json")
      val reports50Package = getVinHistoryPackage(reports50TransactionId)
      withClue(reports50Package) {
        reports50Package.counter shouldBe 50
      }
    }

    "not touch package on duplicate /payment/receive request" in {
      cleanTransactions()

      makePayment("/transaction/create/offers-history-reports-10.json")
      val reports50TransactionId =
        makePayment("/transaction/create/offers-history-reports-50.json")
      val reports50Package = getVinHistoryPackage(reports50TransactionId)

      postPaymentNotification(buildPaymentNotification(reports50TransactionId))
      val reports50PackageAfterDuplicatedRequest =
        getVinHistoryPackage(reports50TransactionId)
      reports50PackageAfterDuplicatedRequest shouldBe reports50Package
    }
  }

  "TransactionService (when user has spent offers-history-reports-10 and initiates refund for offers-history-reports-50 bought on top of it)" should {

    "leave offers-history-reports-10 in same state (inactive, counter = 0) as it was before buying and refunding offers-history-reports-50" in {
      cleanTransactions()

      val reports10TransactionId =
        makePayment("/transaction/create/offers-history-reports-10.json")
      spendVinHistoryPackage(reports10TransactionId)
      val reports10PackageStateBeforeBuying50 =
        getVinHistoryPackage(reports10TransactionId)
      val reports50TransactionId =
        makePayment("/transaction/create/offers-history-reports-50.json")
      postPaymentNotification(buildRefundNotification(reports50TransactionId))
      val reports10Package = getVinHistoryPackage(reports10TransactionId)

      reports10Package shouldBe reports10PackageStateBeforeBuying50
    }

    "cancel offers-history-reports-50" in {
      cleanTransactions()

      val reports10TransactionId =
        makePayment("/transaction/create/offers-history-reports-10.json")
      spendVinHistoryPackage(reports10TransactionId)
      val reports50TransactionId =
        makePayment("/transaction/create/offers-history-reports-50.json")
      postPaymentNotification(buildRefundNotification(reports50TransactionId))
      val reports50Package = getVinHistoryPackage(reports50TransactionId)

      withClue(reports50Package) {
        reports50Package.status shouldBe ProductStatuses.Canceled
      }
    }
  }

  private def makePayment(requestPath: String): TransactionId = {
    val createTransactionRequest =
      Resources.toProto[TransactionRequest](requestPath)
    val transactionId =
      postTransaction(createTransactionRequest)
        .responseAs[CreateTransactionResult]
        .getTransactionId
    postPaymentNotification(buildPaymentNotification(transactionId))
    transactionId
  }

  private def postTransaction(body: MessageLite) =
    post("/api/1.x/service/autoru/transaction", body.toByteArray)

  private def postPaymentNotification(body: MessageLite) = {
    val stream = new ByteArrayOutputStream()
    body.writeDelimitedTo(stream)
    post("/api/1.x/payment/receive", stream.toByteArray).status shouldBe OK
  }

  implicit class RichResponse(private val response: HttpResponse) {

    def responseAs[A: FromResponseUnmarshaller: ClassTag]: A =
      Unmarshal(response).to[A].futureValue
  }

  private def buildPaymentNotification(
      transactionId: TransactionId
  ): PaymentNotification =
    buildPaymentNotification(transactionId, PaymentNotification.Action.ACTIVATE)

  private def buildRefundNotification(
      transactionId: TransactionId
  ): PaymentNotification =
    buildPaymentNotification(
      transactionId,
      PaymentNotification.Action.DEACTIVATE
    )

  private def buildPaymentNotification(
      transactionId: TransactionId,
      action: PaymentNotification.Action
  ): PaymentNotification = {
    val fields = Map("transaction" -> transactionId, "domain" -> "autoru").map {
      case (k, v) => k -> Value.newBuilder().setStringValue(v).build()
    }.asJava
    val b = PaymentNotification.newBuilder()
    // nameUUIDFromBytes вместо randomUUID, чтобы banker transaction id
    // всегда генерировался одинаковым для одного salesman transaction id
    b.setId(UUID.nameUUIDFromBytes(transactionId.getBytes).toString)
      .setTimestamp(
        Timestamp.newBuilder().setSeconds(Instant.now().getEpochSecond)
      )
      .setAction(action)
      .getPayloadBuilder
      .getStructBuilder
      .putAllFields(fields)
    b.build()
  }

  private def getVinHistoryPackage(transactionId: TransactionId) =
    effectBlocking {
      database.withTransaction { implicit session =>
        sql"#$selectQueryBase WHERE transaction_id = $transactionId"
          .as[Subscription]
          .list
      }
    }.flatMap {
      case List(expected) => Task.succeed(expected)
      case unexpected =>
        Task.fail(
          new Exception(
            s"Expected 1 vin-history package found by $transactionId, got $unexpected"
          )
        )
    }
      // Ручка процессинга транзакции раскладывает данные по таблицам асинхронно,
      // поэтому приходится ретраить, пока не разложит.
      .retryN(20)
      .success
      .value

  private def spendVinHistoryPackage(transactionId: TransactionId): Unit =
    database.withTransaction { implicit session =>
      sqlu"""
        UPDATE subscription
        SET counter = 0, status = 'inactive'
        WHERE transaction_id = $transactionId
      """.execute
    }

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu
}
