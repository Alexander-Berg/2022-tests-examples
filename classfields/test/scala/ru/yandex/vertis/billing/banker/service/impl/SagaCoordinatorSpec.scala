package ru.yandex.vertis.billing.banker.service.impl

import billing.common.testkit.zio.ZIOSpecBase
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.banker.dao.impl.jdbc.JdbcSpecTemplate
import ru.yandex.vertis.billing.banker.dao.PaymentSystemDao.{
  PaymentRequestRecord,
  RequestFilter,
  RequestRecord,
  StateFilter,
  StateRecord
}
import ru.yandex.vertis.billing.banker.dao.impl.jdbc.{
  JdbcAccountDao,
  JdbcDowntimeMethodDao,
  JdbcPaymentSystemDao,
  JdbcSagaLogDao
}
import ru.yandex.vertis.billing.banker.dao.util.CleanableJdbcSagaLogDao
import ru.yandex.vertis.billing.banker.model.PaymentRequest.{Context, Options, Source, Targets}
import ru.yandex.vertis.billing.banker.model.gens.{PaymentPayloadGen, Producer}
import ru.yandex.vertis.billing.banker.model.{Account, PaymentRequest, PaymentSystemIds, State}
import ru.yandex.vertis.billing.banker.payment.impl.TrustPaymentSupport.{CardPaymentMethod, YandexAccountPaymentMethod}
import ru.yandex.vertis.billing.banker.service.impl.SagaCoordinator._
import ru.yandex.vertis.billing.banker.util.DateTimeUtils
import ru.yandex.vertis.mockito.MockitoSupport
import slick.dbio.{DBIO, DBIOAction, Effect, NoStream}
import zio.{ZIO, _}

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}
import scala.jdk.CollectionConverters._

class SagaCoordinatorSpec
  extends AnyWordSpec
  with Matchers
  with AsyncSpecBase
  with MockitoSupport
  with JdbcSpecTemplate
  with BeforeAndAfterEach
  with ZIOSpecBase {

  val AccountId = "someId"
  val accountDao = new JdbcAccountDao(database)
  private val downtimePaymentSystemDao = new JdbcDowntimeMethodDao(database, PaymentSystemIds.Trust)
  val paymentSystemDao = new JdbcPaymentSystemDao(database, PaymentSystemIds.Trust)

  val sagaLogDao = new JdbcSagaLogDao(database) with CleanableJdbcSagaLogDao
  val sagaCoordinator = new SagaCoordinator(sagaLogDao)

  override def beforeEach(): Unit = {
    downtimePaymentSystemDao.enable(CardPaymentMethod).futureValue
    downtimePaymentSystemDao.enable(YandexAccountPaymentMethod).futureValue
    accountDao.upsert(Account(AccountId, AccountId)).futureValue
    sagaLogDao.clean().futureValue
    ()
  }

  "SagaCoordinator" should {

    "run start action and get response" in {
      def action(id: Int) = ZIO(DBIO.successful(id))

      val saga = Saga("someSaga", StartStep(action, "StartStatus"))

      sagaCoordinator
        .start(saga, AccountId, "transaction1", 222)
        .unsafeRun() shouldBe 222
    }

    "save start state" in {
      def action(id: Int) = ZIO(DBIO.successful(id))

      val saga = Saga("someSaga", StartStep(action, "StartStatus"))

      sagaCoordinator
        .start(saga, AccountId, "transaction1", 222)
        .unsafeRun()

      val logRecord = sagaLogDao
        .get("someSaga", "transaction1")
        .unsafeRun()
        .get

      logRecord.status shouldBe "StartStatus"
      logRecord.accountId shouldBe AccountId
    }

    "don't save start state if it failed" in {
      def action(id: Int): Task[DBIO[Int]] = ZIO(DBIO.failed(throw new Exception("Error")))

      val saga = Saga("someSaga", StartStep(action, "StartStatus"))

      sagaCoordinator
        .start(saga, AccountId, "transaction1", 222)
        .unsafeRunToTry()
        .isFailure shouldBe true

      sagaLogDao
        .get("someSaga", "transaction1")
        .unsafeRun() shouldBe None
    }

    "sequentially run steps on saga start" in {
      val counter = new AtomicInteger()
      val queue = new ArrayBlockingQueue[(Int, SagaStatus)](8)
      val paymentRequestIncrement = ZIO(queue.add((counter.incrementAndGet(), "PaymentRequestCreated")))
      val paymentIncrement = ZIO(queue.add((counter.incrementAndGet(), "PaymentProcessed")))
      val pushIncrement = ZIO(queue.add((counter.incrementAndGet(), "PaymentPushed")))

      val saga = Saga[RequestRecord, Unit](
        "someSaga",
        StartStep(paymentRequestIncrement *> createPaymentRequest(_), "PaymentRequestCreated"),
        Step(paymentIncrement *> createPayment(_), "PaymentProcessed"),
        Step(pushIncrement *> pushPayment(_), "PaymentPushed")
      )

      val paymentRequest = makePaymentRequestRecord("transaction1")

      sagaCoordinator
        .start(saga, AccountId, paymentRequest.id, paymentRequest)
        .unsafeRun()

      queue.asScala.toSet shouldBe Set(
        (1, "PaymentRequestCreated"),
        (2, "PaymentProcessed"),
        (3, "PaymentPushed")
      )

      paymentSystemDao.getPaymentRequests(RequestFilter.ForIds("transaction1")).futureValue.size shouldBe 1
      paymentSystemDao.getPayments(StateFilter.ForId("transaction1")).futureValue.size shouldBe 1
      val logRecord = sagaLogDao.get("someSaga", "transaction1").unsafeRun().get
      logRecord.status shouldBe "PaymentPushed"
    }

    "don't update log status if step is failed" in {
      def successAction(id: String) = ZIO(DBIO.successful(()))
      def failedAction(id: String) = ZIO(DBIO.failed(new Exception("Error")))

      val saga = Saga(
        "someSaga",
        StartStep(successAction, "StartStatus"),
        Step(successAction, "Step1"),
        Step(failedAction, "Step2"),
        Step(successAction, "Step3")
      )

      sagaCoordinator
        .start(saga, AccountId, "transaction1", "SomeStartRequest")
        .unsafeRun()

      val logRecord = sagaLogDao
        .get("someSaga", "transaction1")
        .unsafeRun()
        .get

      logRecord.status shouldBe "Step1"
    }

    "don't run next step if current step is failed" in {
      def successAction(id: String) = ZIO(DBIO.successful(()))
      def failedAction(id: String) = ZIO(DBIO.failed(new Exception("Error")))
      val counter = new AtomicInteger()
      def nextAction(id: String) = ZIO(counter.incrementAndGet()).as(DBIO.successful(()))

      val saga = Saga(
        "someSaga",
        StartStep(successAction, "StartStatus"),
        Step(successAction, "Step1"),
        Step(failedAction, "Step2"),
        Step(nextAction, "Step3")
      )

      sagaCoordinator
        .start(
          saga,
          AccountId,
          "transaction1",
          "SomeStartRequest"
        )
        .unsafeRun()

      counter.get shouldBe 0
    }

    "update status for AwaitStep only if action return true" in {
      val flag = new AtomicBoolean(false)
      def checkFlagIsTrue(id: String) = ZIO(DBIO.successful(flag.get))
      def successAction(id: String) = ZIO(DBIO.successful(()))

      val saga = Saga(
        "someSaga",
        StartStep(successAction, "StartStatus"),
        Step(successAction, "Step1"),
        AwaitStep(checkFlagIsTrue, "Step2")
      )

      sagaCoordinator
        .start(
          saga,
          AccountId,
          "transaction1",
          "SomeStartRequest"
        )
        .unsafeRun()

      sagaLogDao.get("someSaga", "transaction1").unsafeRun().get.status shouldBe "Step1"

      flag.set(true)

      sagaCoordinator
        .start(
          saga,
          AccountId,
          "transaction1",
          "SomeStartRequest"
        )
        .unsafeRun()

      sagaLogDao.get("someSaga", "transaction1").unsafeRun().get.status shouldBe "Step2"
    }

  }

  private def createPaymentRequest(request: RequestRecord): Task[DBIO[Unit]] =
    ZIO(paymentSystemDao.insertRequestQuery(request).map(_ => ()))

  private def createPayment(id: String): Task[DBIO[Unit]] =
    ZIO(paymentSystemDao.upsertRecordQuery(makePaymentRecord(id)).map(_ => ()))

  private def pushPayment(id: String): Task[DBIO[Unit]] =
    ZIO(DBIO.successful(()))

  private def makePaymentRecord(transactionId: String) =
    StateRecord(transactionId, State.Types.Incoming, AccountId, 1000L, DateTimeUtils.now())

  private def makePaymentRequestRecord(transactionId: String) =
    PaymentRequestRecord(
      transactionId,
      "card",
      Source(AccountId, 1000L, PaymentPayloadGen.next, Options(), None, Context(Targets.Wallet), None, None),
      PaymentRequest.UrlForm("test", "http://")
    )

}
