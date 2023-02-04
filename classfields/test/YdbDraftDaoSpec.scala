package ru.yandex.vertis.general.gost.storage.test

import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import ru.yandex.vertis.general.gost.storage.testkit.DraftDaoSpec
import ru.yandex.vertis.general.gost.storage.ydb.YdbDraftDao
import zio.clock.Clock
import zio.test.DefaultRunnableSpec
import zio.test.TestAspect._

object YdbDraftDaoSpec extends DefaultRunnableSpec {

  override def spec =
    (DraftDaoSpec.spec("YdbDraftDao") @@ sequential @@ shrinks(1))
      .provideCustomLayerShared {
        TestYdb.ydb >>> (YdbDraftDao.live ++ Ydb.txRunner) ++ Clock.live
      }
}
