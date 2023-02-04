package ru.yandex.vertis.billing.banker.service.caching

import org.scalacheck.{Gen, ShrinkLowPriority}
import org.scalamock.scalatest.MockFactory
import org.scalatest.OneInstancePerTest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.billing.banker.dao.AccountTransactionDao
import ru.yandex.vertis.billing.banker.dao.AccountTransactionDao.{ForAccountId, TransactionsFilter, WithActivity}
import ru.yandex.vertis.billing.banker.model.Account.Info
import ru.yandex.vertis.billing.banker.model.AccountTransaction.Activities
import ru.yandex.vertis.billing.banker.model.{
  gens,
  AccountId,
  AccountTransaction,
  AccountTransactionRequest,
  AccountTransactionResponse,
  AccountTransactions
}
import ru.yandex.vertis.billing.banker.model.gens.{
  accountTransactionGen,
  hashAccountTransactionIdGen,
  incomingRqGen,
  AccountGen,
  Producer,
  RequestParams
}
import ru.yandex.vertis.billing.banker.service.impl.GlobalAccountTransactionService
import ru.yandex.vertis.billing.banker.service.impl.caching.{AsyncCacheFactory, InMemoryCacheBucket, Layouts}
import ru.yandex.vertis.billing.banker.util.{AutomatedContext, RequestContext}
import ru.yandex.vertis.caching.base.AsyncCache
import ru.yandex.vertis.caching.support.CacheControl
import ru.yandex.vertis.util.concurrent.Threads

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

class CachedAccountTransactionServiceSpec
  extends MockFactory
  with Matchers
  with AnyWordSpecLike
  with AsyncSpecBase
  with ScalaCheckPropertyChecks
  with ShrinkLowPriority
  with OneInstancePerTest {

  implicit override def ec: ExecutionContext = Threads.SameThreadEc

  implicit val rc: RequestContext = AutomatedContext("CachedAccountTransactionServiceSpec", CacheControl.Cache)

  implicit override val generatorDrivenConfig: PropertyCheckConfiguration = {
    PropertyCheckConfiguration(minSuccessful = 1000)
  }

  private val transactionsDao = mock[AccountTransactionDao]

  private val transactionsCache = {
    AsyncCacheFactory.getCache(InMemoryCacheBucket, Layouts.accountInfoLayout)
  }

  private val cachedTransactions =
    new GlobalAccountTransactionService(transactionsDao, None) with CachedAccountTransactionService {

      implicit override val ec: ExecutionContext = CachedAccountTransactionServiceSpec.this.ec

      override def cache: AsyncCache[AccountId, Info] = transactionsCache

    }

  val defaultAccountId = "123"
  val defaultInfo = Info(100, 0, 0, 0)

  private val template = incomingRqGen(
    RequestParams(
      account = Some(defaultAccountId),
      `type` = Some(AccountTransactions.Incoming),
      amount = Some(100)
    )
  ).next
  private val defaultRequest = template.copy(account = defaultAccountId)

  val activeFilter = WithActivity(Activities.Active)
  val inactiveFilter = Seq(WithActivity(Activities.Inactive))
  val emptyFilters = Seq.empty[TransactionsFilter]
  val activityWithOther = Seq(WithActivity(Activities.Active), ForAccountId(defaultAccountId))

  private val BadFiltersGen = Gen.oneOf(inactiveFilter, emptyFilters, activityWithOther)

  def withdrawReq(accountId: AccountId): AccountTransaction = {
    accountTransactionGen(AccountTransactions.Withdraw).next
      .copy(account = accountId, id = hashAccountTransactionIdGen(AccountTransactions.Withdraw).next)
  }

  private val defaultWithdrawReq = withdrawReq(defaultAccountId)

  def mockExecute(accountId: AccountId): Unit = {
    (transactionsDao
      .execute(_: AccountTransactionRequest))
      .expects(*)
      .onCall { req: AccountTransactionRequest =>
        Future.successful(
          AccountTransactionResponse(
            req,
            accountTransactionGen(AccountTransactions.Incoming).next.copy(account = accountId),
            None
          )
        )
      }: Unit
  }

  def mockInfo(info: Info): Unit = {
    (transactionsDao
      .info(_: AccountId, _: AccountTransactionDao.TransactionsFilter))
      .expects(*, *)
      .returns(Future.successful(info)): Unit
  }

  def mockDeactivate(accountId: AccountId): Unit = {
    (transactionsDao
      .get(_: TransactionsFilter))
      .expects(*)
      .returns(Future.successful(Iterable(defaultWithdrawReq)))
    mockExecute(accountId)
  }

  "CachedAccountTransactionService" should {
    "update cache after execute" in {
      transactionsCache.get(defaultRequest.account).futureValue shouldBe None

      mockExecute(defaultAccountId)
      cachedTransactions.execute(defaultRequest).futureValue
      transactionsCache.get(defaultAccountId).futureValue shouldBe None

      mockInfo(defaultInfo)
      cachedTransactions.info(defaultAccountId, activeFilter).futureValue shouldBe defaultInfo
      transactionsCache.get(defaultAccountId).futureValue shouldBe Some(defaultInfo)

      mockExecute(defaultAccountId)
      cachedTransactions.execute(defaultRequest).futureValue
      transactionsCache.get(defaultAccountId).futureValue shouldBe None
    }

    "update cache after deactivate" in {
      transactionsCache.get(defaultRequest.account).futureValue shouldBe None

      mockInfo(defaultInfo)
      cachedTransactions.info(defaultAccountId, activeFilter).futureValue shouldBe defaultInfo
      transactionsCache.get(defaultAccountId).futureValue shouldBe Some(defaultInfo)

      mockDeactivate(defaultAccountId)
      cachedTransactions.deactivate(defaultWithdrawReq.account, defaultWithdrawReq.id).futureValue
      transactionsCache.get(defaultAccountId).futureValue shouldBe None
    }

    "update cache after info" in {
      transactionsCache.get(defaultRequest.account).futureValue shouldBe None

      mockExecute(defaultAccountId)
      cachedTransactions.execute(defaultRequest).futureValue
      transactionsCache.get(defaultRequest.account).futureValue shouldBe None

      mockInfo(defaultInfo)
      cachedTransactions.info(defaultAccountId, activeFilter).futureValue
      transactionsCache.get(defaultAccountId).futureValue shouldBe Some(defaultInfo)
    }

    "refresh cache when info with CacheControl.NoCache" in {
      transactionsCache.get(defaultRequest.account).futureValue shouldBe None

      mockExecute(defaultAccountId)
      cachedTransactions.execute(defaultRequest).futureValue
      transactionsCache.get(defaultRequest.account).futureValue shouldBe None

      val noCache = AutomatedContext("CachedAccountTransactionServiceSpec")
      mockInfo(defaultInfo)
      cachedTransactions.info(defaultAccountId, activeFilter)(noCache).futureValue
      transactionsCache.get(defaultAccountId).futureValue shouldBe Some(defaultInfo)
    }

    "check bad filters effect" in {
      transactionsCache.get(defaultRequest.account).futureValue shouldBe None

      forAll(BadFiltersGen) { badFilter =>
        mockInfo(defaultInfo)
        cachedTransactions.info(defaultAccountId, badFilter: _*).futureValue
        transactionsCache.get(defaultAccountId).futureValue shouldBe None
      }
    }

    "check cache correctness" in {
      val accounts = AccountGen.next(1000).toSeq
      val flushed = mutable.Set(accounts.map(_.id): _*)

      forAll(Gen.oneOf(accounts), gens.BooleanGen, gens.BooleanGen, gens.BooleanGen) {
        case (account, flush, useBadFilter, byUpdate) =>
          val isFlushed = flushed(account.id)
          val expected = if (isFlushed) None else Some(defaultInfo)
          transactionsCache.get(account.id).futureValue shouldBe expected

          if (!useBadFilter) {
            if (isFlushed) mockInfo(defaultInfo)

            cachedTransactions.info(account.id, activeFilter).futureValue shouldBe defaultInfo
            transactionsCache.get(account.id).futureValue shouldBe Some(defaultInfo)
            flushed -= account.id
          } else {
            val badFilter = BadFiltersGen.next
            val expected = if (isFlushed) None else Some(defaultInfo)

            mockInfo(defaultInfo)
            cachedTransactions.info(account.id, badFilter: _*).futureValue shouldBe defaultInfo
            transactionsCache.get(account.id).futureValue shouldBe expected
          }

          if (flush) {
            if (byUpdate) {
              val request = template.copy(account = account.id)
              mockExecute(account.id)
              cachedTransactions.execute(request).futureValue
              transactionsCache.get(account.id).futureValue shouldBe None
              flushed += account.id
            } else {
              val request = withdrawReq(account.id)
              mockDeactivate(account.id)
              cachedTransactions.deactivate(account.id, request.id).futureValue
              transactionsCache.get(account.id).futureValue shouldBe None
              flushed += account.id
            }
          }
      }
    }
  }

}
