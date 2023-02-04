package ru.yandex.vertis.billing.banker.tasks

import billing.common.testkit.zio.ZIOSpecBase
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.banker.dao.impl.jdbc.JdbcSpecTemplate
import ru.yandex.vertis.billing.banker.dao.impl.jdbc.{
  GlobalJdbcAccountTransactionDao,
  JdbcAccountDao,
  JdbcKeyValueDao,
  JdbcSagaLogDao
}
import ru.yandex.vertis.billing.banker.dao.util.{
  CleanableJdbcAccountDao,
  CleanableJdbcAccountTransactionDao,
  CleanableJdbcSagaLogDao
}
import ru.yandex.vertis.billing.banker.model.Account
import ru.yandex.vertis.billing.banker.service.impl.SagaCoordinator._
import ru.yandex.vertis.billing.banker.service.impl.{EpochServiceImpl, SagaCoordinator}
import slick.dbio.DBIO
import zio.ZIO

class SagaCoordinatorTaskSpec
  extends Matchers
  with AnyWordSpecLike
  with JdbcSpecTemplate
  with AsyncSpecBase
  with BeforeAndAfterEach
  with ZIOSpecBase {

  val accountDao: CleanableJdbcAccountDao = new JdbcAccountDao(database) with CleanableJdbcAccountDao

  val accountTransactionDao: CleanableJdbcAccountTransactionDao =
    new GlobalJdbcAccountTransactionDao(database) with CleanableJdbcAccountTransactionDao

  val sagaLogDao = new JdbcSagaLogDao(database) with CleanableJdbcSagaLogDao

  val sagaCoordinator = new SagaCoordinator(sagaLogDao)

  val AccountId = "someId"

  override def beforeEach(): Unit = {
    accountDao.upsert(Account(AccountId, AccountId)).futureValue
    ()
  }

  override def afterEach(): Unit = {
    accountTransactionDao.cleanLocks().futureValue
    sagaLogDao.clean().futureValue
    accountDao.clean().futureValue
  }

  val epochService = new EpochServiceImpl(new JdbcKeyValueDao(database))

  def createAccount(account: String) = accountDao.upsert(Account(account, account)).futureValue

  "SagaCoordinatorTaskSpec" should {
    "process all saga transactions" in {
      def action(requestId: String) = ZIO(DBIO.successful(()))

      val saga = Saga(
        "sagaName",
        StartStep(action, "Started"),
        Step(action, "Step1"),
        Step(action, "Step2"),
        Step(action, "Finished")
      )

      val onlyStartedSaga = Saga(saga.name, saga.startStep)

      val transactions = for {
        accountId <- (1 to 10).map(i => s"account_$i")
        _ = createAccount(accountId)
        transactionId <- (1 to 10).map(i => s"${accountId}_transaction_$i")
        _ = sagaCoordinator.start(onlyStartedSaga, accountId, transactionId, "SomeRequest").unsafeRun()
      } yield transactionId

      val task = new SagaCoordinatorTask(saga.started, sagaCoordinator, epochService, chunkSize = 15)

      task.execute().futureValue

      transactions.foreach { transactionId =>
        sagaLogDao.get("sagaName", transactionId).unsafeRun().get.status shouldBe "Finished"
      }

    }
  }
}
