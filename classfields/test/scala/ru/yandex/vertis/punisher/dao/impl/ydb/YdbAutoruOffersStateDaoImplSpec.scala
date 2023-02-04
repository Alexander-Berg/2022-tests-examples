package ru.yandex.vertis.punisher.dao.impl.ydb

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vertis.punisher.dao.AutoruOffersStateDaoSpecBase
import ru.yandex.vertis.punisher.dao.impl.ydb.clearable.Clearable
import ru.yandex.vertis.punisher.dao.impl.ydb.clearable.ClearableInstances._
import ru.yandex.vertis.quality.ydb_utils.serialization.YdbSerialization

/**
  * @author mpoplavkov
  */
@RunWith(classOf[JUnitRunner])
class YdbAutoruOffersStateDaoImplSpec extends AutoruOffersStateDaoSpecBase with YdbBaseSpec {
  override type Dao = YdbAutoruOffersStateDaoImpl[F, Tx]

  override def clearable: Clearable[Dao] = Clearable[Dao]

  override def dao: Dao = new YdbAutoruOffersStateDaoImpl[F, Tx](ydb)(YdbSerialization)
}
