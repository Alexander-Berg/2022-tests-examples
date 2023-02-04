package ru.yandex.vertis.billing.banker.controller

import org.joda.time.DateTime
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.banker.dao.impl.jdbc.JdbcSpecTemplate
import ru.yandex.vertis.billing.banker.dao.AccountDao
import ru.yandex.vertis.billing.banker.dao.AccountTransactionDao.ForId
import ru.yandex.vertis.billing.banker.dao.impl.jdbc.{JdbcAccountDao, PaymentSystemJdbcAccountTransactionDao}
import ru.yandex.vertis.billing.banker.model.Account.Info
import ru.yandex.vertis.billing.banker.model.AccountTransaction.Activities
import ru.yandex.vertis.billing.banker.model.AccountTransactionRequest.WithdrawRequest
import ru.yandex.vertis.billing.banker.model.PaymentRequest.Targets
import ru.yandex.vertis.billing.banker.model.gens.{incomingRqGen, withdrawRqGen, Producer, RequestParams}
import ru.yandex.vertis.billing.banker.model.{
  Account,
  AccountTransaction,
  AccountTransactionId,
  ConsumePolicy,
  ConsumeTransactionDistributor,
  Funds,
  Payload,
  PaymentSystemId,
  PaymentSystemIds
}
import ru.yandex.vertis.billing.banker.service.impl.PaymentSystemTransactionService
import ru.yandex.vertis.billing.banker.service.{AccountTransactionService, TransparentValidator}
import ru.yandex.vertis.billing.banker.util.AutomatedContext
import ru.yandex.vertis.billing.banker.util.DateTimeUtils.now
import ru.yandex.vertis.mockito.MockitoSupport

import scala.util.Success

/**
  * Spec on [[ConsumeTransactionDistributor]]
  *
  * @author ruslansd
  */
class ConsumeTransactionDistributorSpec
  extends AnyWordSpec
  with Matchers
  with JdbcSpecTemplate
  with MockitoSupport
  with AsyncSpecBase {

  val accounts: AccountDao = new JdbcAccountDao(database)
  val account = accounts.upsert(Account("JdbcAccountTransactionDaoSpec", "u1")).toTry.get.id

  private val Robokassa = new PaymentSystemTransactionService(
    new PaymentSystemJdbcAccountTransactionDao(database, PaymentSystemIds.Robokassa),
    TransparentValidator
  )

  private val Yandexkassa = new PaymentSystemTransactionService(
    new PaymentSystemJdbcAccountTransactionDao(database, PaymentSystemIds.YandexKassa),
    TransparentValidator
  )

  implicit val oc = AutomatedContext("test")

  private val pss = Map(
    PaymentSystemIds.YandexKassa -> Yandexkassa,
    PaymentSystemIds.Robokassa -> Robokassa
  )

  private val ConsumeOrder = new ConsumePolicy {

    override def getConsumeOrder(timestamp: DateTime): Seq[PaymentSystemId] =
      Seq(PaymentSystemIds.YandexKassa, PaymentSystemIds.Robokassa)
  }
  private val policy = new ConsumeTransactionDistributor(ConsumeOrder, pss)

  private val params = RequestParams()
    .withAccount(account)
    .withTimestamp(now())
    .withWithdrawOpts(
      WithdrawRequest.Options(allowOverdraft = true, allowNegativeTotalSpent = true, allowNegativeOverdraft = true)
    )
    .withActivity(Activities.Active)
    .withPaymentPayload(Payload.Empty)

  "ConsumeTransactionController" should {

    "correctly consume withdraws" in {
      val incoming = incomingRqGen(params.withAmount(5000).withTarget(Targets.Wallet)).next
      Robokassa.execute(incoming).futureValue
      Yandexkassa.execute(incoming).futureValue

      val withdraw = withdrawRqGen(params.withAmount(10000)).next

      policy.consume(withdraw).toTry should matchPattern { case Success(()) =>
      }
      expected(Yandexkassa, withdraw.id, withdraw = 5000, overdraft = 5000)
      expectedInfo(Yandexkassa, income = 5000, withdraw = 5000, overdraft = 5000)

      expected(Robokassa, withdraw.id, withdraw = 5000)
      expectedInfo(Robokassa, income = 5000, withdraw = 5000)

    }

    "correctly consume withdraw update" in {
      val incoming =
        incomingRqGen(params.withAmount(5000).withTarget(Targets.Wallet)).next
      Robokassa.execute(incoming).futureValue
      Yandexkassa.execute(incoming).futureValue

      val withdraw = withdrawRqGen(params.withAmount(5000).withoutReceipt()).next

      policy.consume(withdraw).toTry should matchPattern { case Success(()) =>
      }

      expected(Yandexkassa, withdraw.id, withdraw = 5000)
      expectedInfo(Yandexkassa, income = 10000, withdraw = 10000, overdraft = 5000)

      expected(Robokassa, withdraw.id)
      expectedInfo(Robokassa, income = 10000, withdraw = 5000)

      val updated = withdraw.copy(amount = 7000, timestamp = now())

      policy.consume(updated).toTry should matchPattern { case Success(()) =>
      }

      expected(Yandexkassa, updated.id, withdraw = 5000, overdraft = 2000)
      expectedInfo(Yandexkassa, income = 10000, withdraw = 10000, overdraft = 7000)

      expected(Robokassa, updated.id, withdraw = 2000)
      expectedInfo(Robokassa, income = 10000, withdraw = 7000)
    }

  }

  private def getTransaction(dao: AccountTransactionService, id: AccountTransactionId): Option[AccountTransaction] = {
    dao.get(Seq(ForId(id))).futureValue.headOption
  }

  private def expected(
      transactions: AccountTransactionService,
      id: AccountTransactionId,
      income: Funds = 0,
      withdraw: Funds = 0,
      overdraft: Funds = 0) =
    getTransaction(transactions, id) should matchPattern {
      case Some(AccountTransaction(_, _, _, _, `income`, `withdraw`, `overdraft`, _, _, _, _, _, _, _, _, _)) =>
    }

  private def expectedInfo(
      transactions: AccountTransactionService,
      income: Funds = 0,
      withdraw: Funds = 0,
      refund: Funds = 0,
      overdraft: Funds = 0) =
    transactions.info(account).toTry.get shouldBe Info(income, withdraw, refund, overdraft)
}
