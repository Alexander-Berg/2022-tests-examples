package ru.yandex.vertis.billing.shop.storage.ydb.test

import billing.common_model.Project
import billing.log_model.TargetType
import cats.data.NonEmptyList
import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import common.zio.ydb.testkit.TestYdb._
import ru.yandex.vertis.billing.shop.model.Constants.RaiseFreeVasCode
import ru.yandex.vertis.billing.shop.model._
import ru.yandex.vertis.billing.shop.storage.ActiveProductsDao
import ru.yandex.vertis.billing.shop.storage.ydb.YdbActiveProductsDao
import zio.ZIO
import zio.clock.Clock
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test._

import java.time.Instant

object YdbActiveProductsDaoSpec extends DefaultRunnableSpec {

  private val testUser = UserId("test_user")
  private val testTarget = Target(`type` = TargetType.Offer, id = "test_id")
  private val testCode = ProductCode(RaiseFreeVasCode)

  val testProduct: ActiveProductRecord = ActiveProductRecord(
    project = Project.GENERAL,
    user = testUser,
    target = testTarget,
    productCode = testCode,
    purchaseId = PurchaseId("test_purchase"),
    startTime = Instant.ofEpochMilli(100),
    finishTime = Instant.ofEpochMilli(200)
  )

  override def spec =
    (suite("YdbActiveProductsDao")(
      testM("insert") {
        for {
          _ <- YdbActiveProductsDao.clean
          _ <- runTx(ActiveProductsDao.insert(testProduct))
          select <- runTx(
            ActiveProductsDao.select(ActiveProductsFilter.pk(Project.GENERAL, testUser, testTarget, testCode))
          )
        } yield assert(select)(hasSize(equalTo(1))) && assert(select.headOption)(isSome(equalTo(testProduct)))
      },
      testM("delete") {
        for {
          _ <- YdbActiveProductsDao.clean
          _ <- runTx(ActiveProductsDao.insert(testProduct))
          _ <- runTx(
            ActiveProductsDao
              .delete(testProduct.project, testProduct.user, testProduct.target, testProduct.productCode)
          )
          select <- runTx(
            ActiveProductsDao.select(ActiveProductsFilter.pk(Project.GENERAL, testUser, testTarget, testCode))
          )
        } yield assert(select)(isEmpty)
      },
      testM("filter by targetId") {
        for {
          _ <- YdbActiveProductsDao.clean
          _ <- runTx(ActiveProductsDao.insert(NonEmptyList.one(testProduct)))
          select <- runTx(
            ActiveProductsDao.select(ActiveProductsFilter.singleTarget(Project.GENERAL, testUser, testTarget))
          )
        } yield assert(select)(hasSize(equalTo(1)))
      },
      testM("filter by targetType") {
        for {
          _ <- YdbActiveProductsDao.clean
          pairs <- ZIO.foreach((1 to 10).toList)(i =>
            ZIO.succeed(ProductCode(RaiseFreeVasCode) -> testTarget.copy(id = s"id_$i"))
          )
          _ <- runTx(
            ActiveProductsDao.insert(
              NonEmptyList.fromListUnsafe(pairs.map(pair => testProduct.copy(productCode = pair._1, target = pair._2)))
            )
          )
          select <- runTx(
            ActiveProductsDao.select(ActiveProductsFilter.singleType(Project.GENERAL, testUser, testTarget.`type`.name))
          )
        } yield assert(select)(hasSize(equalTo(10)))
      }
    ) @@ sequential @@ shrinks(1))
      .provideCustomLayerShared {
        TestYdb.ydb >+> YdbActiveProductsDao.live ++ Ydb.txRunner ++ Clock.live
      }
}
