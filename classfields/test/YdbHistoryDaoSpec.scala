package ru.yandex.vertis.general.bonsai.storage.test

import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import ru.yandex.vertis.general.bonsai.storage.testkit.HistoryDaoSpec
import ru.yandex.vertis.general.bonsai.storage.ydb.YdbHistoryDao
import zio.test.TestAspect._
import zio.test._

object YdbHistoryDaoSpec extends DefaultRunnableSpec {

  override def spec =
    (HistoryDaoSpec
      .spec("YdbHistoryDao") @@ sequential)
      .provideCustomLayerShared {
        TestYdb.ydb >>> (YdbHistoryDao.live ++ Ydb.txRunner)
      }
}
