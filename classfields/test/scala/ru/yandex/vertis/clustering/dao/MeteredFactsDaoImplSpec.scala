package ru.yandex.vertis.clustering.dao

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.clustering.BaseSpec
import ru.yandex.vertis.clustering.dao.impl.metered.MeteredFactsDaoImpl
import ru.yandex.vertis.clustering.dao.impl.metered.MeteredFactsDaoImpl.ReadStatistics
import ru.yandex.vertis.clustering.model._

/**
  * SPecs for [[MeteredFactsDaoImpl]]
  *
  * @author devreggs
  */
@RunWith(classOf[JUnitRunner])
class MeteredFactsDaoImplSpec extends BaseSpec {

  "MeteredKarmaFactsDaoImpl" should {

    "fill ReadStatistics" in {
      val statistics = new ReadStatistics()
      statistics.incFailed()
      statistics.factType(FeatureTypes.SuidType)
      statistics.factType(FeatureTypes.IpType)
      statistics.factType(FeatureTypes.IpType)
      statistics.size should be > 0
    }

  }

}
