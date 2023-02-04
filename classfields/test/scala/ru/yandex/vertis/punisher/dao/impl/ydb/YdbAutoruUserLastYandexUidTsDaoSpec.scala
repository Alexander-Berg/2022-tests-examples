package ru.yandex.vertis.punisher.dao.impl.ydb

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vertis.punisher.dao.AutoruUserLastYandexUidTsDaoSpec
import ru.yandex.vertis.punisher.dao.impl.ydb.clearable.Clearable
import ru.yandex.vertis.punisher.dao.impl.ydb.clearable.ClearableInstances.ydbAutoruUserLastYandexUidTsDao
import ru.yandex.vertis.quality.cats_utils.Awaitable._
import ru.yandex.vertis.quality.ydb_utils.serialization.YdbSerialization

@RunWith(classOf[JUnitRunner])
class YdbAutoruUserLastYandexUidTsDaoSpec extends AutoruUserLastYandexUidTsDaoSpec {
  type Dao = YdbAutoruUserLastYandexUidTsDao[F, Tx]
  def clearable: Clearable[Dao] = Clearable[Dao]
  override lazy val dao: Dao = new YdbAutoruUserLastYandexUidTsDao[F, Tx](ydb)(YdbSerialization)
}
