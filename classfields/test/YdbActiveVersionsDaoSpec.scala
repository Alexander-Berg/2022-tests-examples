package ru.yandex.vertis.general.aglomerat.storage.test

import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import zio.test.{DefaultRunnableSpec, ZSpec}
import zio.test._
import common.zio.ydb.testkit.TestYdb.runTx
import ru.yandex.vertis.general.aglomerat.model.ClusterType
import ru.yandex.vertis.general.aglomerat.storage.ActiveVersionsDao
import ru.yandex.vertis.general.aglomerat.storage.ydb.YdbActiveVersionsDao

object YdbActiveVersionsDaoSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("YdbActiveVersionsDao")(
      testM("save version and get version") {
        val version = 3
        for {
          _ <- runTx(ActiveVersionsDao.setActiveVersion(ClusterType.FullDuplicates, version))
          gotVersion <- runTx(ActiveVersionsDao.getActiveVersion(ClusterType.FullDuplicates))
        } yield assertTrue(
          gotVersion.contains(version)
        )
      }
    )
  }.provideCustomLayerShared {
    TestYdb.ydb >>> (YdbActiveVersionsDao.live ++ Ydb.txRunner)
  }
}
