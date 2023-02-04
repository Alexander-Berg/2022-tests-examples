package ru.yandex.vertis.general.bonsai.logic.test.public

import common.cache.api.Cache
import common.cache.memory.MemoryCache
import common.zio.ydb.testkit.TestYdb
import general.bonsai.category_model.Category
import general.common.fail_policy.FailPolicy
import ru.yandex.vertis.general.bonsai.logic.public.LivePublicEntityManager
import ru.yandex.vertis.general.bonsai.model.{
  AnyBonsaiEntity,
  EntityRef,
  EntityRefWithVersion,
  IdWithExactVersion,
  Latest
}
import ru.yandex.vertis.general.bonsai.storage.EntityDao
import ru.yandex.vertis.general.bonsai.storage.ydb.YdbEntityDao
import ru.yandex.vertis.general.bonsai.storage.ydb.sign.EntityUpdateChecker
import ru.yandex.vertis.ydb.zio.TxRunner
import zio.clock.Clock
import zio.test._
import zio.{Has, Ref, ZIO, ZLayer}

object PublicManagerTransactionsTest extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {

    suite("PublicManagerTransactions") {
      testM("Lists latest categories without ydb transaction") {
        for {
          txRunner <- ZIO.service[TxRunner]
          entityDao <- ZIO.service[EntityDao.Service]
          entityCache <- ZIO.service[Cache.Service[IdWithExactVersion, AnyBonsaiEntity]]
          clock <- ZIO.environment[Clock]
          snapshot <- Ref.make(PublicLogicTestUtils.createSnapshotFromEntities(Seq(Category(id = "id")), Seq.empty))
          manager = new LivePublicEntityManager(txRunner, clock, entityDao, entityCache, snapshot)
          _ <-
            manager.listCategories(Set(EntityRefWithVersion(EntityRef("category", "id"), Latest)), FailPolicy.FAIL_FAST)
        } yield assertCompletes
      }
    }.provideCustomLayerShared {
      val clock = Clock.live
      val ydb = TestYdb.ydb
      val updateChecker = EntityUpdateChecker.live
      val txRunner = TestYdb.dyingTxRunner
      val entityDao = (ydb ++ updateChecker) >>> YdbEntityDao.live
      val entityCache =
        ZLayer.succeed(MemoryCache.Config(0, None)) >>> MemoryCache.live[IdWithExactVersion, AnyBonsaiEntity]
      clock ++ entityDao ++ txRunner ++ entityCache
    }
  }
}
