package ru.yandex.vertis.general.bonsai.storage.test

import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import ru.yandex.vertis.general.bonsai.storage.testkit.{runTx, ConstraintsDaoSpec}
import ru.yandex.vertis.general.bonsai.storage.ydb.YdbConstraintsDao
import zio.clock.Clock
import zio.test.DefaultRunnableSpec
import zio.test.TestAspect._

object YdbConstraintsDaoSpec extends DefaultRunnableSpec {

  override def spec =
    (ConstraintsDaoSpec
      .spec("YdbConstraintsDao") @@ before(runTx(YdbConstraintsDao.clean)) @@ sequential)
      .provideCustomLayerShared {
        TestYdb.ydb >>> (YdbConstraintsDao.live ++ Ydb.txRunner ++ Clock.live)
      }
}
