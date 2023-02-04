package ru.yandex.vertis.billing.banker.service.caching

import org.scalacheck.{Gen, ShrinkLowPriority}
import org.scalamock.scalatest.MockFactory
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.OneInstancePerTest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.billing.banker.dao.AccountDao
import ru.yandex.vertis.billing.banker.dao.impl.jdbc.JdbcAccountDao
import ru.yandex.vertis.billing.banker.model.Account.{Patch, Properties}
import ru.yandex.vertis.billing.banker.model.gens.{AccountGen, BooleanGen, Producer}
import ru.yandex.vertis.billing.banker.model.{Account, AccountId, User}
import ru.yandex.vertis.billing.banker.service.impl.AccountServiceImpl
import ru.yandex.vertis.billing.banker.service.impl.caching.{AsyncCacheFactory, InMemoryCacheBucket, Layouts}
import ru.yandex.vertis.billing.banker.util.{AutomatedContext, RequestContext}
import ru.yandex.vertis.caching.base.AsyncCache
import ru.yandex.vertis.caching.support.CacheControl
import ru.yandex.vertis.util.concurrent.Threads

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

class CachedAccountServiceSpec
  extends MockFactory
  with Matchers
  with AnyWordSpecLike
  with AsyncSpecBase
  with ScalaCheckPropertyChecks
  with OneInstancePerTest
  with ShrinkLowPriority {

  implicit override def ec: ExecutionContext = Threads.SameThreadEc

  implicit val defaultContext: RequestContext = AutomatedContext("CachedAccountServiceSpec", CacheControl.Cache)

  implicit override val generatorDrivenConfig: PropertyCheckConfiguration = {
    PropertyCheckConfiguration(minSuccessful = 1000)
  }

  private val accountDao = mock[JdbcAccountDao]

  private val accountServiceCache = AsyncCacheFactory.getCache(InMemoryCacheBucket, Layouts.accountServiceLayout)

  private val cachedAccountService = new AccountServiceImpl(accountDao) with CachedAccountService {

    override def cache: AsyncCache[User, Iterable[Account]] = accountServiceCache

  }

  def mockUpsert(account: Account): Unit = {
    (accountDao
      .upsert(_: Account))
      .expects(*)
      .returns(Future.successful(account)): Unit
  }

  def mockGet(accounts: Iterable[Account]): Unit = {
    (accountDao
      .get(_: AccountDao.Filter))
      .expects(*)
      .returns(Future.successful(accounts)): Unit
  }

  def mockGet(account: Account): Unit = {
    mockGet(Iterable(account))
  }

  def mockUpdate(account: Account): Unit = {
    (accountDao
      .update(_: AccountId, _: Patch))
      .expects(*, *)
      .returns(Future.successful(account)): Unit
  }

  val defaultPatch = Patch(Some("new@ru.ru"), Some("888"))

  "CachedAccountService" should {
    "empty cache after service create" in {
      val account: Account = AccountGen.next

      accountServiceCache.get(account.user).futureValue shouldBe None

      mockUpsert(account)
      cachedAccountService.create(account).futureValue
      accountServiceCache.get(account.user).futureValue shouldBe None
    }

    "cache update after service update" in {
      val account: Account = AccountGen.next

      mockUpsert(account)
      cachedAccountService.create(account).futureValue
      accountServiceCache.get(account.user).futureValue shouldBe None

      mockGet(account)
      cachedAccountService.get(account.user).futureValue shouldBe Iterable(account)
      accountServiceCache.get(account.user).futureValue shouldBe Some(Iterable(account))

      val updated: Account = account.copy(properties = Properties(defaultPatch.email, defaultPatch.phone))
      mockUpdate(updated)
      cachedAccountService.update(account.id, defaultPatch).futureValue shouldBe updated
      accountServiceCache.get(account.user).futureValue shouldBe None

      mockGet(updated)
      cachedAccountService.get(account.user).futureValue shouldBe Iterable(updated)
      accountServiceCache.get(account.user).futureValue shouldBe Some(Iterable(updated))
    }

    "cache update after service get" in {
      val account: Account = AccountGen.next

      accountServiceCache.get(account.user).futureValue shouldBe None

      mockUpsert(account)
      cachedAccountService.create(account).futureValue shouldBe account
      mockGet(account)
      cachedAccountService.get(account.user).futureValue shouldBe Iterable(account)
      accountServiceCache.get(account.user).futureValue shouldBe Some(Iterable(account))
    }

    "refresh cache when get with CacheControl.NoCache" in {
      val account: Account = AccountGen.next

      accountServiceCache.get(account.user).futureValue shouldBe None

      val noCache = AutomatedContext("CachedAccountServiceSpec")
      mockUpsert(account)
      cachedAccountService.create(account).futureValue shouldBe account
      mockGet(account)
      cachedAccountService.get(account.user)(noCache).futureValue shouldBe Iterable(account)
      accountServiceCache.get(account.user).futureValue shouldBe Some(Iterable(account))
    }

    "cache correctness" in {
      val accounts = AccountGen
        .next(1000)
        .groupBy(_.user)
        .map { case (_, v) => v.head }
        .toSeq
      val accountsMap = mutable.Map(accounts.map(a => (a.id, a)): _*)
      val accountIds = accounts.map(_.id).distinct
      val flushed = mutable.Set(accountIds: _*)
      val addedToDB = mutable.Set.empty[AccountId]

      forAll(Gen.oneOf(accountIds), BooleanGen, BooleanGen) { (accountId, add, flush) =>
        val currentAccount = accountsMap(accountId)
        val isAddedToDb = addedToDB(accountId)
        val isFlushed = flushed(accountId)

        val expectedState = if (isFlushed && isAddedToDb || !isAddedToDb) None else Some(Iterable(currentAccount))
        accountServiceCache.get(currentAccount.user).futureValue shouldBe expectedState

        if (add && !isAddedToDb) {
          accountServiceCache.get(currentAccount.user).futureValue shouldBe None
          mockUpsert(currentAccount)
          cachedAccountService.create(currentAccount).futureValue
          accountServiceCache.get(currentAccount.user).futureValue shouldBe None
          addedToDB += currentAccount.id
        }

        if (isAddedToDb) {
          if (isFlushed) {
            mockGet(currentAccount)
            flushed -= accountId
          }
          cachedAccountService.get(currentAccount.user).futureValue
          accountServiceCache.get(currentAccount.user).futureValue shouldBe Some(Iterable(currentAccount))
        }

        if (flush && isAddedToDb) {
          val updated: Account = currentAccount.copy(properties = Properties(defaultPatch.email, defaultPatch.phone))
          mockUpdate(updated)
          cachedAccountService.update(accountId, defaultPatch).futureValue shouldBe updated
          accountServiceCache.get(currentAccount.user).futureValue shouldBe None
          flushed += accountId
        } else if (!flush && isAddedToDb) {
          flushed -= accountId
        }
      }

    }
  }

}
