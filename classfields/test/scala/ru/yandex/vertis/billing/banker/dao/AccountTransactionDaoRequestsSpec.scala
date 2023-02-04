package ru.yandex.vertis.billing.banker.dao

import org.scalatest.featurespec.AnyFeatureSpecLike
import org.scalatest.{BeforeAndAfterEach, GivenWhenThen, TryValues}
import org.scalatest.matchers.should.Matchers
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.billing.banker.dao.util.CleanableDao
import ru.yandex.vertis.billing.banker.exceptions.Exceptions.NotEnoughFunds
import ru.yandex.vertis.billing.banker.model.Account.Info
import ru.yandex.vertis.billing.banker.model.AccountTransaction.{Activities, Activity}
import ru.yandex.vertis.billing.banker.model.AccountTransactionRequest.WithdrawRequest
import ru.yandex.vertis.billing.banker.model.PaymentRequest.Targets
import ru.yandex.vertis.billing.banker.model.PaymentRequest.Targets.{Purchase, Wallet}
import ru.yandex.vertis.billing.banker.model.gens.{
  hashAccountTransactionIdGen,
  incomingRqGen,
  refundRqGen,
  withdrawRqGen,
  Producer,
  RequestParams
}
import ru.yandex.vertis.billing.banker.model.{AccountId, AccountTransactions, ConsumeAccountTransactionRequest, Funds}
import ru.yandex.vertis.billing.banker.util.DateTimeUtils

/**
  * Specs on [[AccountTransactionDao]] transaction requests execution
  *
  * @author alex-kovalenko
  */
trait AccountTransactionDaoRequestsSpec
  extends Matchers
  with AnyFeatureSpecLike
  with AsyncSpecBase
  with TryValues
  with GivenWhenThen
  with BeforeAndAfterEach {

  def transactions: AccountTransactionDao with CleanableDao
  def account: AccountId

  def accInfo: Info =
    transactions.info(account, AccountTransactionDao.WithActivity(Activities.Active)).futureValue

  def params: RequestParams =
    RequestParams()
      .withAccount(account)
      .withActivity(Activities.Active)
      .withTimestamp(DateTimeUtils.now())

  override def beforeEach(): Unit = {
    transactions.clean().futureValue
    super.beforeEach()
  }

  private val wStrictOpts = WithdrawRequest.Options()

  private val wTranspOpts =
    WithdrawRequest.Options(allowOverdraft = true, allowNegativeTotalSpent = true, allowNegativeOverdraft = true)

  Feature("Execute WithdrawRequest") {
    Scenario("simple") {
      Given("empty account")
      When("execute withdraw without overdraft")
      Then("fail")

      intercept[NotEnoughFunds] {
        transactions
          .execute(
            withdrawRqGen(params.withAmount(1000).withWithdrawOpts(wStrictOpts)).next
          )
          .await
      }

      When("execute withdraw with overdraft")
      Then("insert transaction")

      val response1 = transactions
        .execute(withdrawRqGen(params.withAmount(1000).withWithdrawOpts(wTranspOpts)).next)
        .toTry
        .success
        .value
      response1.unprocessed should contain(1000)
      response1.transaction.id shouldBe response1.request.id
      response1.transaction.withdraw shouldBe 0
      response1.transaction.overdraft shouldBe response1.request.amount
      accInfo shouldBe Info(0, 0, 0, 1000)

      Given("income")

      transactions.execute(incomingRqGen(params.withTarget(Wallet).withAmount(10000)).next).futureValue
      accInfo shouldBe Info(10000, 0, 0, 1000)

      When("execute withdraw request without overdraft")
      Then("success")
      val response2 = transactions
        .execute(withdrawRqGen(params.withAmount(2000).withWithdrawOpts(wStrictOpts)).next)
        .toTry
        .success
        .value
      response2.unprocessed shouldBe empty
      response2.transaction.id shouldBe response2.request.id
      response2.transaction.withdraw shouldBe response2.request.amount
      response2.transaction.overdraft shouldBe 0
      accInfo shouldBe Info(10000, 2000, 0, 1000)

      When("execute withdraw request with overdraft")
      Then("success")
      val response3 = transactions
        .execute(withdrawRqGen(params.withAmount(10000).withWithdrawOpts(wTranspOpts)).next)
        .toTry
        .success
        .value
      response3.unprocessed should contain(2000)
      response3.transaction.id shouldBe response3.request.id
      response3.transaction.withdraw shouldBe 8000
      response3.transaction.overdraft shouldBe 2000
      accInfo shouldBe Info(10000, 10000, 0, 3000)
    }

    Scenario("updates") {
      Given("account with 10000 income and 2000 withdraw")
      transactions.execute(incomingRqGen(params.withAmount(10000).withTarget(Wallet)).next).futureValue
      val w1 = withdrawRqGen(params.withAmount(2000).withWithdrawOpts(wTranspOpts).withoutReceipt()).next
      transactions.execute(w1).futureValue
      accInfo shouldBe Info(10000, 2000, 0, 0)

      When("update existent withdraw to 1000")
      Then("change transaction and update info")
      val response1 = transactions.execute(w1.copy(amount = 1000)).toTry.success.value
      response1.unprocessed shouldBe empty
      response1.transaction.withdraw shouldBe 1000
      accInfo shouldBe Info(10000, 1000, 0, 0)

      When("update existent withdraw to 12000")
      Then("change transaction and update account info")
      val response2 = transactions.execute(w1.copy(amount = 12000)).toTry.success.value
      response2.unprocessed should contain(2000)
      response2.transaction.withdraw shouldBe 10000
      response2.transaction.overdraft shouldBe 2000
      accInfo shouldBe Info(10000, 10000, 0, 2000)

      When("update existent withdraw to 8000")
      Then("change transaction and update account info")
      val response3 = transactions.execute(w1.copy(amount = 8000)).toTry.success.value
      response3.unprocessed shouldBe empty
      response3.transaction.withdraw shouldBe 8000
      response3.transaction.overdraft shouldBe 0
      accInfo shouldBe Info(10000, 8000, 0, 0)
    }

    Scenario("multiple") {
      Given("account with 10000 income and 7000 withdraw")
      transactions.execute(incomingRqGen(params.withAmount(10000).withTarget(Wallet)).next).futureValue
      val w1 = withdrawRqGen(params.withAmount(7000).withWithdrawOpts(wStrictOpts).withoutReceipt()).next
      transactions.execute(w1).futureValue
      accInfo shouldBe Info(10000, 7000, 0, 0)

      When("execute other request for 6000")
      Then("fail")
      intercept[NotEnoughFunds] {
        transactions.execute(withdrawRqGen(params.withAmount(6000).withWithdrawOpts(wStrictOpts)).next).await
      }

      When("execute second request for 6000 with overdraft")
      Then("insert second transaction and change account info")
      val w2 = withdrawRqGen(params.withAmount(6000).withWithdrawOpts(wTranspOpts).withoutReceipt()).next
      val response1 = transactions.execute(w2).toTry.success.value
      response1.unprocessed should contain(3000)
      response1.transaction.withdraw shouldBe 3000
      response1.transaction.overdraft shouldBe 3000
      accInfo shouldBe Info(10000, 10000, 0, 3000)

      When("change first withdraw to 4000")
      Then("update first transaction and account info's withdraw")
      val response2 = transactions.execute(w1.copy(amount = 4000)).toTry.success.value
      response2.unprocessed shouldBe empty
      response2.transaction.id shouldBe w1.id
      response2.transaction.withdraw shouldBe 4000
      response2.transaction.overdraft shouldBe 0
      accInfo shouldBe Info(10000, 7000, 0, 3000)

      When("change second withdraw to 2000")
      Then("update second transaction and account info")
      val response3 = transactions.execute(w2.copy(amount = 2000)).toTry.success.value
      response3.unprocessed shouldBe empty
      response3.transaction.id shouldBe w2.id
      response3.transaction.withdraw shouldBe 2000
      response3.transaction.overdraft shouldBe 0
      accInfo shouldBe Info(10000, 6000, 0, 0)
    }

    Scenario("with changing income when got withdraw") {
      Given("account with 10000 income and 7000 withdraw")
      val i1 = incomingRqGen(params.withAmount(10000).withTarget(Wallet)).next
      transactions.execute(i1).futureValue
      val w1 = withdrawRqGen(params.withAmount(7000).withWithdrawOpts(wStrictOpts).withoutReceipt()).next
      transactions.execute(w1).futureValue
      accInfo shouldBe Info(10000, 7000, 0, 0)

      When("change income to 7000")
      Then("update income and account info")
      val response1 = transactions.execute(i1.copy(amount = 7000)).toTry.success.value
      response1.unprocessed shouldBe empty
      response1.transaction.income shouldBe 7000
      accInfo shouldBe Info(7000, 7000, 0, 0)

      When("change income to 4000")
      Then("fail update income and account info")
      intercept[NotEnoughFunds] {
        transactions.execute(i1.copy(amount = 4000)).await
      }
    }

    Scenario("with changing incoming income when got refund") {
      checkUpdateOfIncomeRq(Wallet)
    }

    Scenario("with changing payment income when got refund") {
      checkUpdateOfIncomeRq(Purchase)
    }

    Scenario("deactivate") {
      Given("account with 5000 income and 5000 withdraw")
      val i1 = incomingRqGen(params.withAmount(5000).withTarget(Wallet)).next
      transactions.execute(i1).futureValue
      val w1 = withdrawRqGen(params.withAmount(3000).withWithdrawOpts(wStrictOpts)).next
      transactions.execute(w1).futureValue
      val w2 = withdrawRqGen(params.withAmount(2000).withWithdrawOpts(wStrictOpts)).next
      transactions.execute(w2).futureValue
      accInfo shouldBe Info(5000, 5000, 0, 0)

      When("deactivate withdraw for 3000")
      Then("succeed")
      val response1 = transactions.execute(w1.copy(activity = Activities.Inactive)).futureValue
      response1.transaction.id shouldBe w1.id
      response1.transaction.income shouldBe 0
      response1.transaction.withdraw shouldBe w1.amount
      response1.transaction.overdraft shouldBe 0
      response1.unprocessed shouldBe empty
      accInfo shouldBe Info(5000, 2000, 0, 0)

      When("deactivate withdraw for 2000")
      Then("succeed")
      val response2 = transactions
        .execute(w2.copy(activity = Activities.Inactive))
        .futureValue
      response2.transaction.id shouldBe w2.id
      response2.transaction.income shouldBe 0
      response2.transaction.withdraw shouldBe w2.amount
      response2.transaction.overdraft shouldBe 0
      response2.unprocessed shouldBe empty
      accInfo shouldBe Info(5000, 0, 0, 0)
    }
  }

  Feature("Execute RefundRequest") {
    checkRefundRqExecution("incoming", Wallet)
    checkRefundRqExecution("payment", Purchase)
  }

  def neededInfo(target: Targets.Value, income: Funds, refundAmount: Funds): Info = target match {
    case Wallet =>
      Info(income, 0, refundAmount, 0)
    case _ =>
      Info(income, income, refundAmount, 0)
  }

  def checkUpdateOfIncomeRq(target: Targets.Value): Unit = {

    val refundAmount = 3000

    Given("account with 10000 income and 3000 refund")
    val i1 = incomingRqGen(params.withAmount(10000).withTarget(target)).next
    transactions.execute(i1).futureValue
    val r1 = refundRqGen(params.withAmount(refundAmount).withRefundFor(i1.id)).next
    transactions.execute(r1).futureValue
    accInfo shouldBe neededInfo(target, 7000, refundAmount)

    When("change income to 7000")
    Then("update income and account info")
    val response1 = transactions.execute(i1.copy(amount = 7000)).futureValue
    response1.unprocessed shouldBe empty
    response1.transaction.income shouldBe 7000
    accInfo shouldBe neededInfo(target, 4000, refundAmount)

    When("change income to 4000")
    Then("update income and account info")
    val response2 = transactions.execute(i1.copy(amount = 4000)).futureValue
    response2.unprocessed shouldBe empty
    response2.transaction.income shouldBe 4000
    accInfo shouldBe neededInfo(target, 1000, refundAmount)

    When("change income to 4000")
    Then("fail update income and account info")
    intercept[IllegalArgumentException] {
      transactions.execute(i1.copy(amount = 2000)).await
    }
    ()
  }

  def refundRqFailWithoutIncoming(name: String, target: Targets.Value): Unit = {
    Scenario(s"fail without $name") {
      Given("nothing")
      val nonExist = hashAccountTransactionIdGen(AccountTransactions.Incoming).next

      When("process refund for non existing incoming")
      Then("fail")
      val r1 = refundRqGen(params.withAmount(3000).withRefundFor(nonExist)).next
      intercept[IllegalArgumentException] {
        transactions.execute(r1).await
      }
    }
  }

  def refundRqPassWithIncoming(name: String, target: Targets.Value): Unit = {
    Scenario(s"pass with $name") {
      Given("account with 10000 income")
      val i1 = incomingRqGen(params.withAmount(10000).withTarget(target)).next
      transactions.execute(i1).futureValue

      When("process refund")
      Then("succeed")
      val r1 = refundRqGen(params.withAmount(3000).withRefundFor(i1.id)).next
      transactions.execute(r1).futureValue
      accInfo shouldBe neededInfo(target, 7000, 3000)
    }
  }

  def refundRqFailAmountUpdate(name: String, target: Targets.Value): Unit = {
    Scenario(s"fail $name updates") {
      Given("account with 10000 income")
      val i1 = incomingRqGen(params.withAmount(10000).withTarget(target)).next
      transactions.execute(i1).futureValue

      When("process refund")
      Then("succeed")
      val r1 = refundRqGen(params.withAmount(3000).withRefundFor(i1.id)).next
      transactions.execute(r1).futureValue
      accInfo shouldBe neededInfo(target, 7000, 3000)

      When("increase refund amount")
      Then("fail")
      intercept[IllegalArgumentException] {
        transactions.execute(r1.copy(amount = r1.amount + 1000)).await
      }

      When("decrease refund amount")
      Then("fail")
      intercept[IllegalArgumentException] {
        transactions.execute(r1.copy(amount = r1.amount - 1000)).await
      }
    }
  }

  def refundRqProcessMultiple(name: String, target: Targets.Value): Unit = {
    Scenario(s"process multiple on $name") {
      Given("account with 10000 income")
      val i1 = incomingRqGen(params.withAmount(10000).withTarget(target)).next
      transactions.execute(i1).futureValue

      When("process first refund")
      Then("succeed")
      val r1 = refundRqGen(params.withAmount(3000).withRefundFor(i1.id)).next
      transactions.execute(r1).futureValue
      accInfo shouldBe neededInfo(target, 7000, 3000)

      When("process second refund")
      Then("succeed")
      val r2 = refundRqGen(params.withAmount(3000).withRefundFor(i1.id)).next
      transactions.execute(r2).futureValue
      accInfo shouldBe neededInfo(target, 4000, 6000)

      When("refund with too much amount")
      Then("fail")
      val r3 = refundRqGen(params.withAmount(5000).withRefundFor(i1.id)).next
      intercept[NotEnoughFunds] {
        transactions.execute(r3).await
      }

      When("process third refund")
      Then("succeed")
      val r4 = refundRqGen(params.withAmount(4000).withRefundFor(i1.id)).next
      transactions.execute(r4).futureValue
      accInfo shouldBe neededInfo(target, 0, 10000)
    }
  }

  def checkRefundRqExecution(name: String, target: Targets.Value): Unit = {
    refundRqFailWithoutIncoming(name, target)
    refundRqPassWithIncoming(name, target)
    refundRqFailAmountUpdate(name, target)
    refundRqProcessMultiple(name, target)
  }
}
