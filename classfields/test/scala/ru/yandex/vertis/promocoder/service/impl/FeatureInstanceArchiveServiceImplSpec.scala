package ru.yandex.vertis.promocoder.service.impl

import org.junit.runner.RunWith
import ru.yandex.vertis.promocoder.dao.impl.jvm.{JvmFeatureInstanceArchiveDao, JvmFeatureInstanceDao}
import ru.yandex.vertis.promocoder.model.FeatureInstance
import ru.yandex.vertis.promocoder.service.{
  FeatureInstanceArchiveService,
  FeatureInstanceArchiveServiceSpec,
  FeatureInstanceService
}
import ru.yandex.vertis.promocoder.util.TimeService

/** @author alex-kovalenko
  */
// scalastyle:off
class FeatureInstanceArchiveServiceImplSpec extends FeatureInstanceArchiveServiceSpec {

  import scala.concurrent.ExecutionContext.Implicits.global

  override def services(
      features: Iterable[FeatureInstance],
      archived: Iterable[FeatureInstance]): (FeatureInstanceService, FeatureInstanceArchiveService) = {
    val dao = new JvmFeatureInstanceDao(new TimeService())
    dao.upsert(features).futureValue
    val archiveDao = new JvmFeatureInstanceArchiveDao()
    archiveDao.upsert(archived).futureValue
    (new FeatureInstanceServiceImpl(dao, archiveDao), new FeatureInstanceArchiveServiceImpl(dao, archiveDao))
  }
}
