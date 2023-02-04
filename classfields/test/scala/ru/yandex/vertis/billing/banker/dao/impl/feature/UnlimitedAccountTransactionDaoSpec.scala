package ru.yandex.vertis.billing.banker.dao.impl.feature

import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.banker.dao.impl.jdbc.JdbcSpecTemplate
import ru.yandex.vertis.billing.banker.dao.impl.jdbc.{JdbcAccountDao, PaymentSystemJdbcAccountTransactionDao}
import ru.yandex.vertis.billing.banker.dao.util.CleanableJdbcAccountTransactionDao
import ru.yandex.vertis.billing.banker.model.Account.Info
import ru.yandex.vertis.billing.banker.model.AccountTransactionRequest.WithdrawRequest
import ru.yandex.vertis.billing.banker.model.gens.{withdrawRqGen, Producer, RequestParams}
import ru.yandex.vertis.billing.banker.model.{
  Account,
  AccountTransactionResponse,
  ConsumeAccountTransactionRequest,
  PaymentSystemIds
}
import ru.yandex.vertis.billing.banker.util.DateTimeUtils

import scala.util.Failure

/**
  * Specs for [[UnlimitedAccountTransactionDao]]
  *
  * @author alesavin
  */
class UnlimitedAccountTransactionDaoSpec
  extends AnyWordSpec
  with Matchers
  with JdbcSpecTemplate
  with ScalaCheckPropertyChecks
  with AsyncSpecBase
  with BeforeAndAfterEach {

  val accounts: JdbcAccountDao = new JdbcAccountDao(database)

  val transactions =
    new PaymentSystemJdbcAccountTransactionDao(database, PaymentSystemIds.Overdraft)
      with UnlimitedAccountTransactionDao
      with CleanableJdbcAccountTransactionDao

  val account = accounts.upsert(Account("JdbcAccountTransactionDaoSpec", "u1")).toTry.get.id

  override def beforeEach(): Unit = {
    transactions.clean().futureValue
    super.beforeEach()
  }

  def accInfo: Info = transactions.info(account).futureValue

  val params =
    RequestParams()
      .withAccount(account)
      .withTimestamp(DateTimeUtils.now())
      .withWithdrawOpts(WithdrawRequest.Options())

  "UnlimitedIncomeAccountTransactionDao" should {
    "be empty on start" in {
      accInfo shouldBe Info(0L, 0L, 0L, 0L)
    }
    "fail if execute transaction for non-exist account" in {
      transactions.execute(withdrawRqGen(RequestParams().withAccount("-")).next).toTry should
        matchPattern { case Failure(_: NoSuchElementException) =>
        }
    }
    "execute withdraw with huge amount" in {
      val wa = Long.MaxValue / 2
      val rq = withdrawRqGen(params.withAmount(wa)).next
      val AccountTransactionResponse(_, tr, unprocessed) = transactions.execute(rq).futureValue
      tr.account shouldBe account
      tr.income shouldBe 0L
      tr.withdraw shouldBe wa
      tr.overdraft shouldBe 0L
      unprocessed shouldBe empty

      accInfo shouldBe Info(0L, wa, 0L, 0L)
    }
    "update existent withdraw request" in {
      val amount = 3000L
      val rq = withdrawRqGen(params.withAmount(amount).withoutReceipt()).next
      transactions.execute(rq).futureValue

      val lessAmount = 2000L
      val lessRq = rq.withAmount(lessAmount)
      val AccountTransactionResponse(_, lessTr, _) = transactions.execute(lessRq).futureValue
      lessTr.withdraw shouldBe lessAmount
      accInfo shouldBe Info(0, lessAmount, 0, 0)

      val moreAmount = 4000L
      val moreRq = rq.withAmount(moreAmount)
      val AccountTransactionResponse(_, moreTr, _) = transactions.execute(moreRq).futureValue
      moreTr.withdraw shouldBe moreAmount
      accInfo shouldBe Info(0, moreAmount, 0, 0)
    }
    "pass to execute withdraw requests" in {
      val g = for {
        w <- withdrawRqGen(params)
      } yield w

      forAll(g) { rq =>
        transactions.execute(rq).futureValue
      }
      val ai = accInfo
      ai.totalIncome shouldBe 0
      ai.overdraft shouldBe 0
    }
    "pass to execute any consume requests" in {
      val g: Gen[ConsumeAccountTransactionRequest] = for {
        cr <- withdrawRqGen(params)
      } yield cr
      forAll(g) { tr =>
        transactions.execute(tr).futureValue
      }
      val ai = accInfo
      ai.totalIncome shouldBe 0
      ai.overdraft shouldBe 0
    }
  }
}
