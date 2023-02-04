package ru.yandex.vertis.promocoder.service.impl

import ru.yandex.vertis.promocoder.dao.impl.jvm.{JvmFeatureInstanceArchiveDao, JvmFeatureInstanceDao}
import ru.yandex.vertis.promocoder.model.FeatureInstance
import ru.yandex.vertis.promocoder.service.{FeatureInstanceService, FeatureInstanceServiceSpec}
import ru.yandex.vertis.promocoder.util.TimeService

/** Runnable specs on [[FeatureInstanceServiceImpl]]
  *
  * @author alex-kovalenko
  */
class FeatureInstanceServiceImplSpec extends FeatureInstanceServiceSpec {

  import scala.concurrent.ExecutionContext.Implicits.global

  def getService(initial: Iterable[FeatureInstance], archived: Iterable[FeatureInstance]): FeatureInstanceService = {
    val featureDao = new JvmFeatureInstanceDao(new TimeService())
    featureDao.upsert(initial).futureValue
    val archiveDao = new JvmFeatureInstanceArchiveDao()
    archiveDao.upsert(archived).futureValue
    new FeatureInstanceServiceImpl(featureDao, archiveDao)
  }

}
