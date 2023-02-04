package ru.yandex.vertis.general.search.storage.test

import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import common.zio.ydb.testkit.TestYdb.runTx
import ru.yandex.vertis.general.search.model.index.IndexQueueShardCount
import ru.yandex.vertis.general.search.storage.IndexQueueDao
import ru.yandex.vertis.general.search.storage.ydb.YdbIndexQueueDao
import zio.UIO
import zio.clock.Clock
import zio.test.Assertion.equalTo
import zio.test.TestAspect.before
import zio.test.{assert, DefaultRunnableSpec, ZSpec}
import zio.test.TestAspect.{sequential, _}

object YdbIndexQueueDaoSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("YdbReindexQueueDaoSpec ")(
      testM("push, pull, size, delete") {
        val offerIds = List("offerId1", "offerId2", "offerId3")
        val shardId = 0
        for {
          _ <- runTx(IndexQueueDao.push(offerIds))
          savedOffers <- runTx(IndexQueueDao.peek(shardId, limit = 10))
          _ = println(savedOffers)
          _ <- runTx(IndexQueueDao.delete(List("offerId1", "offerId2")))
          sizeAfterDelete <- IndexQueueDao.getQueueMetrics
        } yield assert(savedOffers.size)(equalTo(3)) &&
          assert(sizeAfterDelete.size)(equalTo(1))
      }
    ) @@ before(runTx(YdbIndexQueueDao.clean)) @@ sequential
  }.provideCustomLayerShared {
    val shardCount = UIO(IndexQueueShardCount.testCount).toLayer
    val ydb = TestYdb.ydb
    val txRunner = ydb >+> Ydb.txRunner
    val dao = (shardCount ++ txRunner) >+> YdbIndexQueueDao.live
    dao ++ Clock.live
  }

}
