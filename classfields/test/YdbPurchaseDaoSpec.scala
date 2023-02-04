package ru.yandex.vertis.billing.shop.storage.ydb.test

import billing.common_model.Project
import billing.log_model.Platform
import cats.data.NonEmptyList
import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import common.zio.ydb.testkit.TestYdb.runTx
import ru.yandex.vertis.billing.shop.billing_gates.trust.model.PurchaseToken
import ru.yandex.vertis.billing.common.paging.PagingRequest
import ru.yandex.vertis.billing.shop.model.Constants.{OfferTargetType, RaiseFreeVasCode}
import ru.yandex.vertis.billing.shop.model.Purchase.{RefundDetails, TrustDetails}
import ru.yandex.vertis.billing.shop.model.{Money, ProductCode, Purchase, PurchaseFilter, PurchaseId, Target, UserId}
import ru.yandex.vertis.billing.shop.model.Purchase.Status._
import ru.yandex.vertis.billing.shop.model.PurchaseFilter.UserLocator
import ru.yandex.vertis.billing.shop.storage.PurchaseDao
import ru.yandex.vertis.billing.shop.storage.ydb.YdbPurchaseDao
import zio.clock.Clock
import zio.test.Assertion.{equalTo, hasSameElements}
import zio.test.TestAspect.{sequential, shrinks}
import zio.test._
import zio.test.environment.TestEnvironment

import java.time.Instant

object YdbPurchaseDaoSpec extends DefaultRunnableSpec {

  val start: Instant = Instant.ofEpochSecond(122)
  val end: Instant = Instant.ofEpochMilli(86522)

  val purchased = Purchase(
    project = Project.GENERAL,
    user = UserId("111"),
    id = PurchaseId("123232132"),
    status = Purchase.Status.Purchased,
    refundStatus = Purchase.RefundStatus.NotRequired,
    refundDetails = None,
    subjects = NonEmptyList.one(Purchase.Subject(ProductCode(RaiseFreeVasCode), Target(OfferTargetType, "222"))),
    price = Money(12L),
    paymentDetails = NonEmptyList.one(TrustDetails(Money(12L), Some(PurchaseToken("eeee")), None, None)),
    startTime = start,
    updateTime = end,
    platform = Platform.DESKTOP
  )

  val purchased1 = Purchase(
    project = Project.GENERAL,
    user = UserId("111"),
    id = PurchaseId("12555555"),
    status = Purchase.Status.Purchased,
    refundStatus = Purchase.RefundStatus.NotRequired,
    refundDetails = None,
    subjects = NonEmptyList.one(Purchase.Subject(ProductCode(RaiseFreeVasCode), Target(OfferTargetType, "333"))),
    price = Money(12L),
    paymentDetails = NonEmptyList.one(TrustDetails(Money(12L), Some(PurchaseToken("eeee")), None, None)),
    startTime = start,
    updateTime = end,
    platform = Platform.DESKTOP
  )

  val purchased2 = Purchase(
    project = Project.GENERAL,
    user = UserId("111"),
    id = PurchaseId("12555566655"),
    status = Purchase.Status.Purchased,
    refundStatus = Purchase.RefundStatus.NotRequired,
    refundDetails = None,
    subjects = NonEmptyList.one(Purchase.Subject(ProductCode(RaiseFreeVasCode), Target(OfferTargetType, "33553"))),
    price = Money(12L),
    paymentDetails = NonEmptyList.one(TrustDetails(Money(12L), Some(PurchaseToken("eeee")), None, None)),
    startTime = start,
    updateTime = end,
    platform = Platform.DESKTOP
  )

  val canceled = Purchase(
    project = Project.GENERAL,
    user = UserId("111"),
    id = PurchaseId("1255556665444445"),
    status = Purchase.Status.Canceled,
    refundStatus = Purchase.RefundStatus.NotRequired,
    refundDetails = None,
    subjects = NonEmptyList.one(Purchase.Subject(ProductCode(RaiseFreeVasCode), Target(OfferTargetType, "33553"))),
    price = Money(12L),
    paymentDetails = NonEmptyList.one(TrustDetails(Money(12L), Some(PurchaseToken("eeee")), None, None)),
    startTime = start,
    updateTime = end,
    platform = Platform.DESKTOP
  )

  val applied = Purchase(
    project = Project.GENERAL,
    user = UserId("111"),
    id = PurchaseId("123232132"),
    status = Purchase.Status.Applied,
    refundStatus = Purchase.RefundStatus.NotRequired,
    refundDetails = None,
    subjects = NonEmptyList.one(Purchase.Subject(ProductCode(RaiseFreeVasCode), Target(OfferTargetType, "222"))),
    price = Money(12L),
    paymentDetails = NonEmptyList.one(TrustDetails(Money(12L), Some(PurchaseToken("eeee")), None, None)),
    startTime = start,
    updateTime = end,
    platform = Platform.DESKTOP
  )

  val hold = Purchase(
    project = Project.GENERAL,
    user = UserId("222"),
    id = PurchaseId("6523234"),
    status = Purchase.Status.Hold,
    refundStatus = Purchase.RefundStatus.NotRequired,
    refundDetails = None,
    subjects = NonEmptyList.one(Purchase.Subject(ProductCode(RaiseFreeVasCode), Target(OfferTargetType, "333"))),
    price = Money(12L),
    paymentDetails = NonEmptyList.one(TrustDetails(Money(12L), Some(PurchaseToken("eeee")), None, None)),
    startTime = start,
    updateTime = end,
    platform = Platform.DESKTOP
  )

  val toBeRefunded = hold.copy(
    id = PurchaseId("76452673456"),
    status = Purchase.Status.Applied,
    refundStatus = Purchase.RefundStatus.Required,
    refundDetails = Some(RefundDetails("some-operator-login", Instant.ofEpochMilli(186522))),
    subjects = NonEmptyList.one(Purchase.Subject(ProductCode(RaiseFreeVasCode), Target(OfferTargetType, "2034958")))
  )

  override def spec: ZSpec[TestEnvironment, Any] = {
    (suite("YdbPurchaseDao")(
      testM("insert") {
        for {
          _ <- YdbPurchaseDao.clean
          _ <- runTx(PurchaseDao.insert(purchased))
          purchaseDb <- runTx(PurchaseDao.get(purchased.id))
        } yield assert(purchaseDb.get.id)(equalTo(purchased.id))
      },
      testM("update") {
        for {
          _ <- YdbPurchaseDao.clean
          _ <- runTx(PurchaseDao.insert(purchased))
          _ <- runTx(PurchaseDao.update(applied))
          updated <- runTx(PurchaseDao.get(purchased.id))
        } yield assert(updated.get.status)(equalTo(Purchase.Status.Applied))
      },
      testM("select") {
        for {
          _ <- YdbPurchaseDao.clean
          _ <- runTx(PurchaseDao.insert(purchased))
          _ <- runTx(PurchaseDao.insert(purchased1))
          _ <- runTx(PurchaseDao.insert(purchased2))
          _ <- runTx(PurchaseDao.insert(canceled))
          purchasedList <- runTx(
            PurchaseDao.select(
              PurchaseFilter(
                paging = PagingRequest(None, 100),
                project = purchased.project,
                locator = UserLocator(purchased.user),
                statuses = PurchaseFilter.StatusesList(NonEmptyList.of(Purchased)),
                refundStatuses = PurchaseFilter.Any
              )
            )
          )
          canceledList <- runTx(
            PurchaseDao.select(
              PurchaseFilter(
                paging = PagingRequest(None, 100),
                project = canceled.project,
                locator = UserLocator(canceled.user),
                statuses = PurchaseFilter.StatusesList(NonEmptyList.of(Canceled)),
                refundStatuses = PurchaseFilter.Any
              )
            )
          )
          both <- runTx(
            PurchaseDao.select(
              PurchaseFilter(
                paging = PagingRequest(None, 100),
                project = purchased.project,
                locator = UserLocator(purchased.user),
                statuses = PurchaseFilter.StatusesList(NonEmptyList.of(Purchased, Canceled)),
                refundStatuses = PurchaseFilter.Any
              )
            )
          )
        } yield assert(purchasedList.purchases.size)(equalTo(3)) &&
          assert(canceledList.purchases.size)(equalTo(1)) &&
          assert(both.purchases.size)(equalTo(4))
      },
      testM("pull stale purchases") {
        for {
          _ <- YdbPurchaseDao.clean
          _ <- runTx(PurchaseDao.insert(hold))
          _ <- runTx(PurchaseDao.insert(purchased))
          _ <- runTx(PurchaseDao.insert(toBeRefunded))
          stuck <- runTx(PurchaseDao.getStalePurchases(10, 0, end.plusSeconds(10)))
        } yield assert(stuck)(hasSameElements(List(hold, toBeRefunded)))
      }
    ) @@ sequential @@ shrinks(1))
      .provideCustomLayerShared {
        (TestYdb.ydb ++ Clock.live) >+> (YdbPurchaseDao.live ++ Ydb.txRunner)
      }
  }
}
