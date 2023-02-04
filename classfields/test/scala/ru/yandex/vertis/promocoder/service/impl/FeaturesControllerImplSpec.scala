package ru.yandex.vertis.promocoder.service.impl

import org.junit.runner.RunWith
import ru.yandex.vertis.promocoder.dao.impl.jvm.{
  JvmFeatureInstanceArchiveDao,
  JvmFeatureInstanceDao,
  JvmPromocodeAliasDao,
  JvmPromocodeDao,
  JvmPromocodeInstanceDao
}
import ru.yandex.vertis.promocoder.service.FeaturesControllerSpec
import ru.yandex.vertis.promocoder.service.FeaturesControllerSpec.Setup
import ru.yandex.vertis.promocoder.service.FeaturesShippingPromocodeInstanceServiceSpec.NoOpPromocodeService
import ru.yandex.vertis.promocoder.util.{CharsGenerator, DefaultPromocodeGenerator, TimeService}

/** Runnable specs on [[FeaturesControllerImpl]]
  *
  * @author alex-kovalenko
  */
class FeaturesControllerImplSpec extends FeaturesControllerSpec {

  import scala.concurrent.ExecutionContext.Implicits.global

  override def initialize: Setup = {
    val promocodeDao = new JvmPromocodeDao

    val promocodes =
      new PromocodeServiceImpl(
        promocodeDao,
        new JvmPromocodeAliasDao,
        new DefaultPromocodeGenerator(CharsGenerator.Default)
      )
    val promocodeInstances =
      new PromocodeInstanceServiceImpl(new JvmPromocodeInstanceDao(promocodeDao), new NoOpPromocodeService)
    val featureInstances =
      new FeatureInstanceServiceImpl(new JvmFeatureInstanceDao(new TimeService()), new JvmFeatureInstanceArchiveDao)

    Setup(
      promocodes,
      promocodeInstances,
      featureInstances,
      new FeaturesControllerImpl(promocodes, promocodeInstances, featureInstances)
    )
  }
}
