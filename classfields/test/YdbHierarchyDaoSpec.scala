package ru.yandex.vertis.general.bonsai.storage.test

import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import ru.yandex.vertis.general.bonsai.storage.testkit.{runTx, HierarchyDaoSpec}
import ru.yandex.vertis.general.bonsai.storage.ydb.YdbHierarchyDao
import zio.clock.Clock
import zio.test.DefaultRunnableSpec
import zio.test.TestAspect._

object YdbHierarchyDaoSpec extends DefaultRunnableSpec {

  override def spec =
    (HierarchyDaoSpec
      .spec("YdbHierarchyDao") @@ before(runTx(YdbHierarchyDao.clean)) @@ sequential)
      .provideCustomLayerShared {
        TestYdb.ydb >>> (YdbHierarchyDao.live ++ Ydb.txRunner ++ Clock.live)
      }
}
