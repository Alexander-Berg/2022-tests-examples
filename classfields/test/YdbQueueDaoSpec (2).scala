package ru.yandex.vertis.general.users.storage.test

import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import ru.yandex.vertis.general.users.storage.QueueDaoSpec
import ru.yandex.vertis.general.users.storage.ydb.YdbQueueDao
import zio.clock.Clock
import zio.random.Random
import zio.test.DefaultRunnableSpec
import zio.test.TestAspect._

object YdbQueueDaoSpec extends DefaultRunnableSpec {

  override def spec =
    (QueueDaoSpec.spec("YdbQueueDao") @@ sequential @@ shrinks(1))
      .provideCustomLayerShared {
        TestYdb.ydb >>> (YdbQueueDao.live ++ Ydb.txRunner) ++ Clock.live ++ Random.live
      }
}
