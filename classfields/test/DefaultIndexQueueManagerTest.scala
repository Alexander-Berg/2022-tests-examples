package ru.yandex.vertis.general.search.logic.test

import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import common.zio.ydb.testkit.TestYdb.runTx
import org.apache.commons.codec.binary.StringUtils
import org.apache.commons.codec.digest.MurmurHash2
import ru.yandex.vertis.general.search.logic.IndexQueueManager
import ru.yandex.vertis.general.search.model.index.{IndexQueueConfig, IndexQueueShardCount}
import ru.yandex.vertis.general.search.storage.ydb.YdbIndexQueueDao
import zio.UIO
import zio.clock.Clock
import zio.test.Assertion.equalTo
import zio.test.TestAspect.{before, sequential}
import zio.test.{DefaultRunnableSpec, ZSpec}
import zio.test.assert

object DefaultIndexQueueManagerTest extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("YdbReindexQueueDaoSpec ")(
      testM("проверить IndexQueueManager") {
        val offerIds = List("offerId1", "offerId2", "offerId3")
        for {
          _ <- IndexQueueManager.add(offerIds)
          initialSavedOffersNumber <- IndexQueueManager.size()
          firstOffer <- IndexQueueManager.getByShardId(shardId("offerId1"), 100)
          _ <- IndexQueueManager.delete(offerIds.take(2))
          finalOffersNumber <- IndexQueueManager.size()
        } yield assert(initialSavedOffersNumber)(equalTo(3)) &&
          assert(finalOffersNumber)(equalTo(1)) &&
          assert(firstOffer.head)(equalTo("offerId1"))
      }
    ) @@ before(runTx(YdbIndexQueueDao.clean)) @@ sequential
  }.provideCustomLayerShared {
    val shardCount = UIO(IndexQueueShardCount.testCount).toLayer
    val ydb = TestYdb.ydb
    val txRunner = ydb >+> Ydb.txRunner
    val dao = (shardCount ++ txRunner) >+> YdbIndexQueueDao.live
    dao ++ Clock.live ++ txRunner >+> IndexQueueManager.live
  }

  private def shardId(offerId: String): Int = {
    val bytes = StringUtils.getBytesUtf8(offerId)
    Math.floorMod(MurmurHash2.hash32(bytes, bytes.length, 0), IndexQueueShardCount.testCount.count)
  }
}
