package ru.yandex.vertis.general.gost.storage.test

import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import ru.yandex.vertis.general.gost.storage.testkit.QueueDaoSpec
import ru.yandex.vertis.general.gost.storage.ydb.YdbQueueDao
import zio.clock.Clock
import zio.test.DefaultRunnableSpec
import zio.test.TestAspect._

object YdbQueueDaoSpec extends DefaultRunnableSpec {

  override def spec =
    (QueueDaoSpec
      .spec("YdbQueueDao") @@ sequential @@ shrinks(1))
      .provideCustomLayer {
        TestYdb.ydb >>> (YdbQueueDao.live ++ Ydb.txRunner) ++ Clock.live
      }
}
