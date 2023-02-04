package ru.yandex.vertis.general.personal.storage.test

import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import ru.yandex.vertis.general.personal.storage.testkit.SearchHistoryDaoSpec
import ru.yandex.vertis.general.personal.storage.ydb.YdbSearchHistoryDao
import zio.clock.Clock
import zio.random.Random
import zio.test.{DefaultRunnableSpec, ZSpec}
import zio.test.TestAspect._

object YdbSearchHistoryDaoSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    (SearchHistoryDaoSpec.spec("YdbSearchHistoryDao") @@ sequential @@ shrinks(1))
      .provideCustomLayerShared {
        Clock.live ++ TestYdb.ydb >>> (YdbSearchHistoryDao.live ++ Ydb.txRunner) ++ Random.live
      }
}
