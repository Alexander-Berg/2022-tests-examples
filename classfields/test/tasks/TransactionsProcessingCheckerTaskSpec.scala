package ru.yandex.vertis.billing.banker.tasks

import com.codahale.metrics.health.{HealthCheck, HealthCheckRegistry}
import org.scalacheck.{Gen, ShrinkLowPriority}
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.common.monitoring.CompoundHealthCheckRegistry
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.billing.banker.Domains
import ru.yandex.vertis.billing.banker.dao.AccountTransactionDao.TransactionsFilter
import ru.yandex.vertis.billing.banker.model.AccountTransaction.Statuses
import ru.yandex.vertis.billing.banker.model.AccountTransaction
import ru.yandex.vertis.billing.banker.service.AccountTransactionService
import ru.yandex.vertis.billing.banker.util.RequestContext
import ru.yandex.vertis.util.time.DateTimeUtil
import ru.yandex.vertis.billing.banker.model.gens.Producer
import ru.yandex.vertis.billing.banker.tasks.utils.TransactionGens.{
  HashProcessedTransactionGen,
  HashUnprocessedTransactionGen,
  PaymentSystemHashUnprocessedTransactionGen,
  PaymentSystemProcessedTransactionGen
}

import scala.concurrent.Future

class TransactionsProcessingCheckerTaskSpec
  extends MockFactory
  with Matchers
  with AnyWordSpecLike
  with ScalaCheckPropertyChecks
  with ShrinkLowPriority
  with AsyncSpecBase {

  private val accountTransactions = mock[AccountTransactionService]

  def mockTransactionsHas(status: Boolean): Unit =
    (accountTransactions
      .has(_: TransactionsFilter)(_: RequestContext))
      .expects(*, *)
      .returns(Future.successful(status)): Unit

  var checkerOpt: Option[HealthCheck] = None

  private val healthChecks = {
    val registryMock = mock[HealthCheckRegistry]
    (registryMock.register(_: String, _: HealthCheck)).expects(*, *).onCall { (_, check) =>
      checkerOpt = Some(check)
    }
    new CompoundHealthCheckRegistry(registryMock)
  }

  val initTime: Long = DateTimeUtil.now().getMillis

  def prepareTransactions(transactions: Seq[AccountTransaction]): Seq[AccountTransaction] = {
    val counter = Gen.choose(initTime - 2 * TransactionsProcessingCheckerTask.TimeLimit, initTime)
    transactions.map { transaction =>
      transaction.copy(epoch = Some(counter.next))
    }
  }

  val processingTask =
    new TransactionsProcessingCheckerTask(accountTransactions)(Domains.AutoRu, ec, healthChecks)

  implicit override val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(minSuccessful = 50)

  private def checkTask(genTransactions: Gen[AccountTransaction]): Unit = {
    forAll(Gen.listOfN(100, genTransactions)) { rawTransactions =>
      val transactions = prepareTransactions(rawTransactions)
      val status = transactions
        .sortBy(_.epoch)
        .exists { tr =>
          tr.epoch.get + TransactionsProcessingCheckerTask.TimeLimit <= initTime && tr.status == Statuses.Created
        }
      mockTransactionsHas(status)
      processingTask.execute().futureValue
      checkerOpt.get.execute().isHealthy shouldBe !status
    }
    ()
  }

  "TransactionsProcessingCheckerTask" should {
    "be healthy" in {
      checkTask(
        Gen.oneOf(
          PaymentSystemProcessedTransactionGen,
          HashProcessedTransactionGen
        )
      )
    }
    "be unhealthy" in {
      checkTask(
        Gen.oneOf(
          HashUnprocessedTransactionGen,
          PaymentSystemHashUnprocessedTransactionGen
        )
      )
    }
    "check correctly" in {
      checkTask(
        Gen.frequency(
          35 -> PaymentSystemProcessedTransactionGen,
          35 -> HashProcessedTransactionGen,
          15 -> PaymentSystemHashUnprocessedTransactionGen,
          15 -> HashUnprocessedTransactionGen
        )
      )
    }
  }
}
