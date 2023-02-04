package ru.yandex.vertis.billing.shop.storage.ydb.test

import billing.common_model.Project
import billing.log_model.TargetType
import ru.yandex.vertis.billing.shop.model._
import ru.yandex.vertis.billing.shop.model.processing.ProcessingTask._
import ru.yandex.vertis.billing.shop.storage.ydb.YdbProcessingQueueDao
import ru.yandex.vertis.billing.shop.storage.ProcessingQueueDao
import common.zio.ydb.testkit.TestYdb
import common.zio.ydb.testkit.TestYdb._
import common.zio.ydb._
import ru.yandex.vertis.billing.shop.domain.processing.ProductDeactivateProcessor._
import ru.yandex.vertis.billing.shop.model.Constants.RaiseFreeVasCode
import zio.clock.Clock
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test._

import java.time.Instant

object YdbProcessingQueueDaoSpec extends DefaultRunnableSpec {

  private val testUser = UserId("test_user")
  private val testTarget = Target(TargetType.Offer, id = "test_id")
  private val testCode = ProductCode(RaiseFreeVasCode)

  val testTask: DeactivateTask = DeactivateTask(
    project = Project.GENERAL,
    userId = testUser,
    target = testTarget,
    productCode = testCode
  )

  val now: Instant = Instant.ofEpochSecond(100)

  override def spec =
    (suite("YdbProcessingQueueDao")(
      testM("insert") {
        for {
          _ <- YdbProcessingQueueDao.clean
          _ <- runTx(ProcessingQueueDao.offer(testTask, now))
          metrics <- runTx(ProcessingQueueDao.getQueueMetrics)
        } yield assert(metrics)(hasKey(testTask._type, hasField("size", _.size, equalTo(1))))
      },
      testM("delete") {
        for {
          _ <- YdbProcessingQueueDao.clean
          _ <- runTx(ProcessingQueueDao.offer(testTask, now))
          _ <- runTx(ProcessingQueueDao.delete(testTask))
          metrics <- runTx(ProcessingQueueDao.getQueueMetrics)
        } yield assert(metrics)(not(hasKey(testTask._type)))
      },
      testM("select by shardId") {
        for {
          _ <- YdbProcessingQueueDao.clean
          _ <- runTx(ProcessingQueueDao.offer(testTask, now))
          select <- runTx(
            ProcessingQueueDao.peek(YdbProcessingQueueDao.shardId(testTask), 10, now.plusMillis(1))
          )
        } yield assert(select)(hasSize(equalTo(1)))
      }
    ) @@ sequential @@ shrinks(1))
      .provideCustomLayerShared {
        TestYdb.ydb >+> YdbProcessingQueueDao.live ++ Ydb.txRunner ++ Clock.live
      }
}
