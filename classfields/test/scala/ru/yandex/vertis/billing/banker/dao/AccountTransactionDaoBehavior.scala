package ru.yandex.vertis.billing.banker.dao

import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.billing.banker.dao.AccountTransactionDao.Patch.{ProcessStatus, UpdatePushStatus}
import ru.yandex.vertis.billing.banker.dao.AccountTransactionDao.ScanFilter.PaidSince
import ru.yandex.vertis.billing.banker.dao.AccountTransactionDao.{
  ForAccountId,
  ForId,
  ForIds,
  ForPayloadSubString,
  ForType,
  ForUser,
  TransactionsFilter,
  WithProcessedStatus,
  WithPushStatus,
  WithoutRefunds
}
import ru.yandex.vertis.billing.banker.dao.impl.jdbc.JdbcAccountTransactionDao.{
  ByIntervalGroupContext,
  TypeActivityGroupContext
}
import ru.yandex.vertis.billing.banker.dao.util.CleanableDao
import ru.yandex.vertis.billing.banker.exceptions.Exceptions.NotEnoughFunds
import ru.yandex.vertis.billing.banker.model.Account.Info
import ru.yandex.vertis.billing.banker.model.AccountTransaction.Activities.Active
import ru.yandex.vertis.billing.banker.model.AccountTransaction.PushStatuses
import ru.yandex.vertis.billing.banker.model.AccountTransaction.Statuses.{Created, Processed}
import ru.yandex.vertis.billing.banker.model.AccountTransactionRequest.{IncomingRequest, RefundRequest, WithdrawRequest}
import ru.yandex.vertis.billing.banker.model.PaymentRequest.Targets
import ru.yandex.vertis.billing.banker.model.gens.{
  getOr,
  hashAccountTransactionIdGen,
  incomingRqGen,
  refundRqGen,
  withdrawRqGen,
  BooleanGen,
  HashAccountTransactionIdGen,
  Producer,
  RequestParams
}
import ru.yandex.vertis.billing.banker.model.{
  Account,
  AccountId,
  AccountTransaction,
  AccountTransactionId,
  AccountTransactionRequest,
  AccountTransactionResponse,
  AccountTransactions,
  EpochWithId,
  Funds,
  Payload
}
import ru.yandex.vertis.billing.banker.service.AccountTransactionService.{ByDayGroup, GroupedStatistics, TypeActivity}
import ru.yandex.vertis.billing.banker.util.CollectionUtils.RichTraversableLike
import ru.yandex.vertis.billing.banker.util.{DateTimeUtils, Page, Range, SlicedResult}
import ru.yandex.vertis.util.collection.RichTraversableOnce
import spray.json.{JsObject, JsString}

import scala.concurrent.Future
import scala.reflect.ClassTag
import scala.util.Failure

/**
  * Common basic behavior for any [[AccountTransactionDao]] implementation
  *
  * @author alex-kovalenko
  */
trait AccountTransactionDaoBehavior
  extends AnyWordSpec
  with Matchers
  with AsyncSpecBase
  with BeforeAndAfterEach
  with ScalaCheckPropertyChecks {

  def accountId: AccountId

  def account: Account

  def accounts: AccountDao

  def transactions: AccountTransactionDao with CleanableDao

  override def beforeEach(): Unit = {
    transactions.clean().futureValue
    super.beforeEach()
  }

  lazy val defaultParams = RequestParams()
    .withActivity(Active)
    .withTimestamp(DateTimeUtils.now())
    .withAccount(accountId)
    .withWithdrawOpts(WithdrawRequest.Options())

  def emptyDao(): Unit = {

    "has no transactions on start" in {
      assert(!transactions.has(ForAccountId(accountId)).futureValue)
      assert(transactions.get(ForAccountId(accountId)).futureValue.isEmpty)
      accInfo shouldBe Info.Empty
    }

    "fail if execute transaction for non-existent account" in {
      val params = defaultParams.withAccount("-").withAmount(100)

      transactions.execute(incomingRqGen(params).next).toTry should matchPattern {
        case Failure(_: NoSuchElementException) =>
      }
      transactions.execute(withdrawRqGen(params).next).toTry should matchPattern {
        case Failure(_: NoSuchElementException) =>
      }
    }

    "does not upsert withdraw without incomes" in {
      val withdraw = withdrawRqGen(defaultParams).next
      intercept[NotEnoughFunds] {
        transactions.execute(withdraw).await
      }
    }
  }

  def readableDao(): Unit = {
    val incomeGen = for {
      income <- Gen.chooseNum(10000L, 100000L)
      incomingParams = defaultParams.copy(
        amount = Some(income),
        timestamp = None
      )
      incomingReq <- incomingRqGen(incomingParams)
    } yield incomingReq

    def refundFor(tr: AccountTransaction): RefundRequest = {
      val gen = for {
        full <- BooleanGen
        refund <-
          if (full) {
            Gen.const(tr.income)
          } else {
            Gen.chooseNum(101, tr.income - 101)
          }
        refundParams = defaultParams.copy(
          amount = Some(refund),
          timestamp = None,
          refundFor = Some(tr.id)
        )
        refundReq <- refundRqGen(refundParams)
      } yield refundReq
      gen.next
    }

    def checkLast(tpe: AccountTransactions.Value): Unit = {
      val expectedLast = {
        val trs = transactions.get(ForAccountId(accountId), ForType(tpe)).futureValue
        trs.optMaxBy(_.timestamp.getMillis)
      }
      val actualLast = transactions
        .last(
          ForUser(account.user),
          ForType(tpe)
        )
        .futureValue

      actualLast shouldBe expectedLast: Unit
    }

    "get and list transactions" in {
      val filter = ForAccountId(accountId)

      transactions.has(filter).futureValue shouldBe false
      transactions.get(filter).futureValue should have size 0

      val incomes = incomeGen.next(20)

      val incomeTransactionResponses = Future.traverse(incomes)(transactions.execute).futureValue
      val incomeTransactions = incomeTransactionResponses.map(_.transaction)

      val refunds = incomeTransactions.map(refundFor)

      refunds.foreach { w =>
        transactions.execute(w).futureValue
      }

      val balance = incomeTransactions
        .zip(refunds)
        .map { case (i, r) =>
          if (i.income == i.withdraw) {
            0
          } else {
            i.income - r.amount
          }
        }
        .sum

      val info = transactions.info(accountId).futureValue
      val expectedBalance = info.balance
      balance shouldBe expectedBalance

      val withdrawAmounts = {
        val part = 10000L
        val count = balance / part
        val mainPats = (1L to count).map(_ => part)
        val last = balance - count * part
        if (last != 0) {
          mainPats :+ last
        } else {
          mainPats
        }
      }
      val withdraws = withdrawAmounts.map { amount =>
        val params = defaultParams.copy(
          amount = Some(amount),
          timestamp = None
        )
        withdrawRqGen(params).next
      }

      withdraws.map(_.amount).sum <= balance shouldBe true

      withdraws.foreach { w =>
        transactions.execute(w).futureValue
      }

      val requests = incomes ++ refunds ++ withdraws

      transactions.has(filter).futureValue shouldBe true
      val allByAccountId = transactions.get(filter).futureValue
      allByAccountId should have size requests.size
      val ids = requests.map(_.id)
      val allByIds = transactions.get(ForIds(ids)).futureValue
      allByIds should contain theSameElementsAs allByAccountId

      val count = requests.size
      val maxWindowSize = count / 2
      (1 to maxWindowSize).foreach { windowSize =>
        var from = 0
        val iCount = count / windowSize + 2
        (0 to iCount).foreach { i =>
          val page = Page(i, windowSize)
          val expected = Math.max(Math.min(count - from, windowSize), 0)
          transactions.list(page, filter).futureValue should matchPattern {
            case SlicedResult(values, `count`, `page`) if values.size == expected =>
          }
          val range = Range(from, from + windowSize)
          transactions.list(range, filter).futureValue should matchPattern {
            case SlicedResult(values, `count`, `range`) if values.size == expected =>
          }
          from = from + windowSize
        }
      }

      val sorted = allByAccountId.toList.sortBy(_.timestamp.getMillis).reverse
      transactions
        .list(
          Page(0, requests.size),
          filter
        )
        .futureValue
        .values
        .toList shouldBe sorted

      val sortedWithouRefunds = sorted.filterNot(_.id.`type` == AccountTransactions.Refund)
      transactions
        .list(
          Page(0, requests.size),
          filter,
          WithoutRefunds
        )
        .futureValue
        .values
        .toList shouldBe sortedWithouRefunds

      checkLast(AccountTransactions.Incoming)
      checkLast(AccountTransactions.Refund)
      checkLast(AccountTransactions.Withdraw)
    }
  }

  def updatableDao(): Unit = {
    "fail update nonexist transaction" in {
      transactions
        .update(
          accountId,
          HashAccountTransactionIdGen.next,
          ProcessStatus(Created, DateTimeUtils.now().getMillis)
        )
        .toTry should
        matchPattern { case Failure(_: NoSuchElementException) =>
        }
    }

    "perform transaction fields updates for non refund transactions" in {
      val income = {
        val minSuccessful = generatorDrivenConfig.minSuccessful
        val maxDiscardedFactor = generatorDrivenConfig.maxDiscardedFactor

        (minSuccessful * (1 + maxDiscardedFactor)).toLong * 100
      }
      val incomeParams = defaultParams.withAmount(income).withAccount(accountId).withTarget(Targets.Wallet)
      transactions.execute(incomingRqGen(incomeParams).next).futureValue

      val params = defaultParams
        .withAmount(1)
        .withAccount(accountId)

      forAll(transactionRqWithoutRefundGen(params)) { trReq =>
        val AccountTransactionResponse(_, tr, _) = transactions.execute(trReq).futureValue
        val epoch1 = tr.epoch.get

        val baseFilters: Seq[TransactionsFilter] = Seq(
          ForAccountId(accountId),
          ForId(tr.id)
        )

        val filtersWithCreated: Seq[TransactionsFilter] =
          baseFilters :+ WithProcessedStatus(Created)

        transactions.has(filtersWithCreated: _*).futureValue shouldBe true
        transactions.get(filtersWithCreated: _*).futureValue.head shouldBe tr

        val processed = transactions.update(accountId, tr.id, ProcessStatus(Processed, epoch1)).futureValue
        processed.id shouldBe tr.id
        processed.status shouldBe Processed
        assert(processed.epoch.get >= epoch1)

        transactions.update(accountId, tr.id, ProcessStatus(Processed, epoch1)).futureValue shouldBe processed

        val filtersWithProcessed: Seq[TransactionsFilter] =
          baseFilters :+ WithProcessedStatus(Processed)

        transactions.has(filtersWithProcessed: _*).futureValue shouldBe true
        transactions.get(filtersWithProcessed: _*).futureValue.exactlyOne.id shouldBe tr.id

        val processedEpoch = processed.epoch.get
        val pushed =
          transactions.update(accountId, tr.id, UpdatePushStatus(PushStatuses.Ok, processedEpoch)).futureValue
        pushed.id shouldBe tr.id
        pushed.status shouldBe Processed
        assert(pushed.epoch.get == processedEpoch)

        val filtersWithProcessedAndPushStatusOk: Seq[TransactionsFilter] =
          filtersWithProcessed :+ WithPushStatus(PushStatuses.Ok)

        transactions.has(filtersWithProcessedAndPushStatusOk: _*).futureValue shouldBe true
        transactions.get(filtersWithProcessedAndPushStatusOk: _*).futureValue.exactlyOne.id shouldBe tr.id

        val created = transactions.update(accountId, tr.id, ProcessStatus(Created, processedEpoch + 1000)).futureValue
        created.id shouldBe tr.id
        created.status shouldBe Created
        assert(created.epoch.get >= processed.epoch.get)

        val filtersWithCreatedAndPushStatusOk: Seq[TransactionsFilter] =
          baseFilters :+ WithProcessedStatus(Created) :+ WithPushStatus(PushStatuses.Ok)

        transactions.has(filtersWithCreatedAndPushStatusOk: _*).futureValue shouldBe true
        transactions.get(filtersWithCreatedAndPushStatusOk: _*).futureValue.exactlyOne.id shouldBe tr.id
      }
    }
  }

  def readyForPurchases(): Unit = {
    val params = defaultParams.withPaymentPayload(Payload.Text("payload")).withTarget(Targets.Purchase)

    val amount = 1000L
    val firstPurchase = incomingRqGen(params.withAmount(amount)).next

    val name = "Incoming for purchase"
    testInsert(
      firstPurchase,
      Expects(amount, amount, Info(amount, amount, 0, 0)),
      name
    )(())(())

    testInsertOther[IncomingRequest](
      incomingRqGen(params.withAmount(1)).next,
      firstPurchase.id,
      Expects(1, 1, Info(amount + 1, amount + 1, 0, 0)),
      _.copy(amount = 0)
    )(name) {
      transactions.execute(firstPurchase).futureValue
      ()
    }(())

    val increase = 2000L
    testUpdate(
      firstPurchase,
      firstPurchase.copy(amount = increase),
      Expects(increase, increase, Info(increase, increase, 0, 0)),
      s"increase Purchase if got request with existent id"
    ) {
      transactions.execute(firstPurchase).futureValue
      ()
    }

    val decrease = 500L
    testUpdate(
      firstPurchase,
      firstPurchase.copy(amount = decrease),
      Expects(decrease, decrease, Info(decrease, decrease, 0, 0)),
      s"decrease Purchase if got request with existent id"
    ) {
      transactions.execute(firstPurchase).futureValue
      ()
    }
  }

  def readyForIncomes(): Unit = {
    val params = defaultParams.withTarget(Targets.Wallet)

    val amount = 1000L
    val firstIncome = incomingRqGen(params.withAmount(amount)).next
    testInsert(
      firstIncome,
      Expects(amount, 0, Info(amount, 0, 0, 0)),
      "insert Incoming"
    )(())(())

    testInsertOther[IncomingRequest](
      incomingRqGen(params.withAmount(1)).next,
      firstIncome.id,
      Expects(1, 0, Info(amount + 1, 0, 0, 0)),
      _.copy(amount = 0)
    )() {
      transactions.execute(firstIncome).futureValue
      ()
    }(())

    val increase = 15000L
    testUpdate(
      firstIncome,
      firstIncome.copy(amount = increase),
      Expects(increase, 0, Info(increase, 0, 0, 0)),
      s"increase ${firstIncome.id.`type`} if got request with existent id"
    ) {
      transactions.execute(firstIncome).futureValue
      ()
    }

    val decrease = 500L
    testUpdate(
      firstIncome,
      firstIncome.copy(amount = decrease),
      Expects(decrease, 0, Info(decrease, 0, 0, 0)),
      s"decrease ${firstIncome.id.`type`} if got request with existent id"
    ) {
      transactions.execute(firstIncome).futureValue
      ()
    }
  }

  def readyForWithdraws(): Unit = {
    val income = 5000L
    val incomeRq = incomingRqGen(defaultParams.withTarget(Targets.Wallet).withAmount(income)).next

    val amount = 1000L
    val firstWithdraw = withdrawRqGen(defaultParams.withAmount(amount).withoutReceipt()).next
    testInsert(
      firstWithdraw,
      Expects(0, amount, Info(income, amount, 0, 0)),
      "insert withdraw"
    ) {
      transactions.execute(incomeRq).futureValue
      ()
    }(())

    testInsertOther[WithdrawRequest](
      withdrawRqGen(defaultParams.withAmount(1).withoutReceipt()).next,
      firstWithdraw.id,
      Expects(0, 1, Info(income, amount + 1, 0, 0)),
      _.copy(amount = 0)
    )() {
      transactions.execute(incomeRq).futureValue
      transactions.execute(firstWithdraw).futureValue
      ()
    }(())

    val w2 = 3000L
    testUpdate(
      firstWithdraw,
      firstWithdraw.copy(amount = w2),
      Expects(0, w2, Info(income, w2, 0, 0)),
      s"update ${firstWithdraw.id.`type`} if got request with existent id"
    ) {
      transactions.execute(incomeRq).futureValue
      transactions.execute(firstWithdraw).futureValue
      ()
    }
  }

  def readyForRefunds(): Unit = {
    val income = 5000L
    val withdraw = 3000L
    val incomeRq = incomingRqGen(defaultParams.withTarget(Targets.Wallet).withAmount(income)).next
    val withdrawRq = withdrawRqGen(defaultParams.withAmount(withdraw)).next

    val refundAmount = 1000L
    val nonExist = hashAccountTransactionIdGen(AccountTransactions.Incoming).next
    val refundForNonExist = refundRqGen(defaultParams.withAmount(refundAmount).withRefundFor(nonExist)).next
    testInsertFail[IllegalArgumentException](
      refundForNonExist,
      "fail insert refund for non exist incoming"
    )(())

    val params = defaultParams.withRefundFor(incomeRq.id)
    val toMuchMoneyToRefund = refundRqGen(params.withAmount(income)).next
    testInsertFail[NotEnoughFunds](
      toMuchMoneyToRefund,
      "fail insert refund when not enough funds"
    ) {
      transactions.execute(incomeRq).futureValue
      transactions.execute(withdrawRq).futureValue
      ()
    }

    val firstRefund = refundRqGen(params.withAmount(refundAmount)).next
    testInsert(
      firstRefund,
      Expects(-refundAmount, 0, Info(income - refundAmount, withdraw, refundAmount, 0)),
      "insert refund"
    ) {
      transactions.execute(incomeRq).futureValue
      transactions.execute(withdrawRq).futureValue
      ()
    } {
      checkRefundForSet(firstRefund.id, incomeRq.id)
    }

    val secondRefund = refundRqGen(params.withAmount(refundAmount)).next
    testInsertOther[RefundRequest](
      secondRefund,
      firstRefund.id,
      Expects(-refundAmount, 0, Info(income - (2 * refundAmount), withdraw, 2 * refundAmount, 0)),
      tr => tr
    )() {
      transactions.execute(incomeRq).futureValue
      transactions.execute(withdrawRq).futureValue
      transactions.execute(firstRefund).futureValue
      ()
    } {
      checkRefundForSet(secondRefund.id, incomeRq.id)
    }

    val r2 = 500L
    testUpdateFail[IllegalArgumentException](
      firstRefund,
      firstRefund.copy(amount = r2),
      "fail update refund amount"
    ) {
      transactions.execute(incomeRq).futureValue
      transactions.execute(withdrawRq).futureValue
      transactions.execute(firstRefund).futureValue
      ()
    }
  }

  def scanByPaidSince(): Unit = {
    "scan by paid since" in {
      val income = 5000L
      val withdraw = 3000L
      val count = 5
      val incomeRqs = incomingRqGen(defaultParams.withTarget(Targets.Wallet).withAmount(income)).next(count).toList
      val withdrawRqs = withdrawRqGen(defaultParams.withAmount(withdraw)).next(count).toList

      val trs = (incomeRqs ++ withdrawRqs).map(transactions.execute(_).futureValue).map(_.transaction)
      val filtered = trs.filter(t => t.id.`type` == AccountTransactions.Incoming && t.income > 0)
      transactions.scan(PaidSince(EpochWithId(0, None))).futureValue should contain theSameElementsAs filtered
      trs.foreach { tr =>
        val epochWithId = EpochWithId(tr.epoch.get, Some(tr.id.value))
        val result = transactions.scan(PaidSince(epochWithId)).futureValue
        val expected = filtered
          .groupBy(_.epoch.get)
          .flatMap { case (epoch, trs) =>
            if (epoch > epochWithId.epoch) {
              trs
            } else if (epoch == epochWithId.epoch) {
              trs.filter(_.id.value > epochWithId.id.get)
            } else {
              Seq.empty
            }
          }
        result should contain theSameElementsAs expected
      }

    }
  }

  def searchByPayload(): Unit = {
    "search by payload substring" in {
      val accParams = defaultParams.withAccount(accountId)
      val textOfferId = "12345"
      val jsonOfferId = "1234567890"
      val payloadText = Payload.Text(textOfferId)
      val payloadJson = Payload.Json(JsObject("offer_id" -> JsString(jsonOfferId)))
      val withText = withdrawRqGen(accParams.withPaymentPayload(payloadText)).next
      val withJson = withdrawRqGen(accParams.withPaymentPayload(payloadJson)).next
      val income = incomingRqGen(
        accParams
          .withAmount(withText.amount + withJson.amount)
          .withTarget(Targets.Wallet)
      ).next
      transactions.execute(income).futureValue
      transactions.execute(withText).futureValue
      transactions.execute(withJson).futureValue

      transactions.has(ForPayloadSubString(textOfferId)).futureValue shouldBe true
      transactions.get(ForPayloadSubString(textOfferId)).futureValue match {
        case trs =>
          trs.exists(_.id == withJson.id) shouldBe true
          trs.exists(_.id == withText.id) shouldBe true
      }

      transactions.has(ForPayloadSubString(jsonOfferId)).futureValue shouldBe true
      transactions.get(ForPayloadSubString(jsonOfferId)).futureValue match {
        case trs =>
          trs.exists(_.id == withJson.id) shouldBe true
      }

      transactions.has(ForPayloadSubString("5454")).futureValue shouldBe false
      transactions.get(ForPayloadSubString("5454")).futureValue match {
        case trs =>
          trs.isEmpty shouldBe true
      }
    }

  }

  def getStatistics(): Unit = {
    val firstAccount = "account-1"
    val secondAccount = "account-2"
    val trCount = 5
    val incomingAmount = 10000

    def freeAmount(i: IncomingRequest): Funds = {
      (i.target, i.payload) match {
        case (Some(Targets.Purchase), _) => 0L
        case (Some(Targets.SecurityDeposit), _) => 0L
        case (Some(_), _) => i.amount
        case (None, Payload.Empty) => i.amount
        case _ => 0L
      }
    }

    def actionOnNonZeroAmount[T](maxAmount: Long)(action: Long => T): (Funds, Option[T]) = {
      if (maxAmount == 0) {
        (maxAmount, None)
      } else {
        Gen.option(Gen.choose(1L, maxAmount)).map {
          case Some(amount) =>
            (maxAmount - amount, Some(action(amount)))
          case None =>
            (maxAmount, None)
        }
      }.next
    }

    def requestsFor(accountId: AccountId): Iterable[AccountTransactionRequest] = {
      val params = defaultParams.withoutTimestamp()
      val accParams = params.withAccount(accountId)
      val incomings = incomingRqGen(accParams.copy(amount = Some(incomingAmount))).next(trCount)
      incomings.flatMap { incoming =>
        val free = freeAmount(incoming)
        val (afterWithdraw, withdraw) = actionOnNonZeroAmount(free) { amount =>
          withdrawRqGen(accParams.withAmount(amount)).next
        }
        val (_, refund) = actionOnNonZeroAmount(afterWithdraw) { amount =>
          refundRqGen(
            params.copy(
              account = Some(incoming.account),
              amount = Some(amount),
              refundFor = Some(incoming.id)
            )
          ).next
        }
        Seq(Some(incoming), withdraw, refund).flatten
      }
    }

    def initializeTransactions() = {
      accounts.upsert(Account(firstAccount, "u1")).futureValue
      accounts.upsert(Account(secondAccount, "u1")).futureValue

      val trRequests = requestsFor(firstAccount) ++ requestsFor(secondAccount)

      trRequests.foreach(transactions.execute(_).futureValue)
    }

    def incomeAndWithdraw(tr: AccountTransaction): (Funds, Funds) =
      if (tr.id.`type` == AccountTransactions.Refund) {
        (0, 0)
      } else {
        (tr.income - tr.refund, if (tr.withdraw == 0) 0 else tr.withdraw - tr.refund)
      }

    "get type activity statistics" in {
      initializeTransactions()

      transactions.has().futureValue shouldBe true
      val trs = transactions.get(WithoutRefunds).futureValue
      val expectedTypeActivity = trs
        .map { r =>
          val (income, withdraw) = incomeAndWithdraw(r)

          GroupedStatistics(
            TypeActivity(r.id.`type`, r.activity),
            1,
            income,
            withdraw,
            r.refund,
            r.overdraft
          )
        }
        .groupBy(_.group)
        .view
        .mapValues(_.reduce { (w1, w2) =>
          GroupedStatistics(
            w1.group,
            w1.count + w2.count,
            w1.income + w2.income,
            w1.withdraw + w2.withdraw,
            w1.refund + w2.refund,
            w1.overdraft + w2.overdraft
          )
        })
        .values

      val stat = transactions.groupedStatistics[TypeActivity]()(TypeActivityGroupContext).futureValue

      expectedTypeActivity should contain theSameElementsAs stat
    }

    "get interval statistics" in {
      initializeTransactions()

      transactions.has().futureValue shouldBe true
      val trs = transactions.get(WithoutRefunds).futureValue
      val expectedIntervalGrouped = trs
        .map { r =>
          val (income, withdraw) = incomeAndWithdraw(r)

          GroupedStatistics(
            ByDayGroup(r.timestamp.toLocalDate),
            1,
            income,
            withdraw,
            r.refund,
            r.overdraft
          )
        }
        .groupBy(_.group)
        .view
        .mapValues(_.reduce { (w1, w2) =>
          GroupedStatistics(
            w1.group,
            w1.count + w2.count,
            w1.income + w2.income,
            w1.withdraw + w2.withdraw,
            w1.refund + w2.refund,
            w1.overdraft + w2.overdraft
          )
        })
        .values

      val stat = transactions.groupedStatistics[ByDayGroup]()(ByIntervalGroupContext).futureValue

      expectedIntervalGrouped should contain theSameElementsAs stat
    }

  }

  private def accInfo: Info =
    transactions.info(accountId).futureValue

  case class Expects(trIncome: Funds, trWithdraw: Funds, info: Info)

  private def testInsert(
      rq: AccountTransactionRequest,
      expected: Expects,
      name: String
    )(before: => Unit
    )(after: => Unit): Unit = {
    s"$name" in {
      before
      val AccountTransactionResponse(r, tr, _) = transactions.execute(rq).futureValue
      r shouldBe rq
      tr.id shouldBe rq.id
      tr.income shouldBe expected.trIncome
      tr.withdraw shouldBe expected.trWithdraw
      tr.overdraft shouldBe 0
      tr.account shouldBe accountId
      tr.payload shouldBe rq.payload

      transactions.has(ForAccountId(accountId)).futureValue shouldBe true
      val filtered = transactions
        .get(ForAccountId(accountId))
        .futureValue
        .filter(_.id.`type` == rq.id.`type`)
      filtered should have size 1
      filtered.head shouldBe tr

      accInfo shouldBe expected.info

      after
    }
  }

  private def testInsertFail[T <: AnyRef: ClassTag](
      rq: AccountTransactionRequest,
      name: String
    )(before: => Unit): Unit = {
    s"$name" in {
      before

      intercept[T] {
        transactions.execute(rq).await
      }
    }
  }

  private def testUpdate(
      first: AccountTransactionRequest,
      last: AccountTransactionRequest,
      expected: Expects,
      name: String
    )(before: => Unit): Unit = {
    s"$name" in {
      before
      first.id shouldBe last.id

      val AccountTransactionResponse(_, tr, _) = transactions.execute(last).futureValue
      tr.id shouldBe first.id
      tr.income shouldBe expected.trIncome
      tr.withdraw shouldBe expected.trWithdraw

      transactions.has(ForAccountId(accountId)).futureValue shouldBe true
      val head :: Nil = transactions
        .get(ForAccountId(accountId))
        .futureValue
        .filter(_.id.`type` == last.id.`type`)
        .toList
      head.income shouldBe expected.trIncome
      head.withdraw shouldBe expected.trWithdraw
      head.overdraft shouldBe 0

      accInfo shouldBe expected.info
    }
  }

  private def testUpdateFail[T <: AnyRef: ClassTag](
      first: AccountTransactionRequest,
      last: AccountTransactionRequest,
      name: String
    )(before: => Unit): Unit = {
    s"$name" in {
      before
      first.id shouldBe last.id

      intercept[T] {
        transactions.execute(last).await
      }
    }
  }

  private def testInsertOther[R <: AccountTransactionRequest](
      rq: R,
      prevId: AccountTransactionId,
      expected: Expects,
      reset: R => R
    )(name: String = prevId.`type`.toString
    )(before: => Unit
    )(after: => Unit): Unit = {
    val `type` = rq.id.`type`
    s"insert another $name" in {
      before
      rq.id.`type` shouldBe `type`

      val AccountTransactionResponse(_, tr, _) = transactions.execute(rq).futureValue
      tr.id should not be prevId
      tr.income shouldBe expected.trIncome
      tr.withdraw shouldBe expected.trWithdraw

      transactions.has(ForAccountId(accountId)).futureValue shouldBe true
      val ids = transactions
        .get(ForAccountId(accountId))
        .futureValue
        .filter(_.id.`type` == `type`)
        .map(_.id)
      ids should ((have size 2 and contain).allOf(prevId, rq.id))

      accInfo shouldBe expected.info
      transactions.execute(reset(rq)).futureValue

      after
    }
  }

  private def transactionRqWithoutRefundGen(params: RequestParams = RequestParams()): Gen[AccountTransactionRequest] =
    Gen.oneOf(
      incomingRqGen(params),
      withdrawRqGen(params)
    )

  private def checkRefundForSet(refundId: AccountTransactionId, incomingId: AccountTransactionId): Unit = {
    val refund = transactions.get(ForId(refundId)).futureValue
    refund.size shouldBe 1
    refund.head.id shouldBe refundId
    refund.head.refundFor shouldBe Some(incomingId): Unit
  }

}
