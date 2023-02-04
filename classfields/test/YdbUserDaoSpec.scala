package ru.yandex.vertis.general.users.storage.test

import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import ru.yandex.vertis.general.users.storage.testkit.UserDaoSpec
import ru.yandex.vertis.general.users.storage.ydb.YdbUserDao
import zio.clock.Clock
import zio.random.Random
import zio.test.DefaultRunnableSpec
import zio.test.TestAspect._

object YdbUserDaoSpec extends DefaultRunnableSpec {

  override def spec =
    (UserDaoSpec.spec("YdbUserDao") @@ sequential @@ shrinks(1))
      .provideCustomLayerShared {
        TestYdb.ydb >>> (YdbUserDao.live ++ Ydb.txRunner) ++ Clock.live ++ Random.live
      }
}
