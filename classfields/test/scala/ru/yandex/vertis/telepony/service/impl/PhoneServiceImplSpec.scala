package ru.yandex.vertis.telepony.service.impl

import ru.yandex.vertis.application.runtime.VertisRuntime
import ru.yandex.vertis.ops.prometheus.PrometheusRegistry
import ru.yandex.vertis.telepony.geo.impl.{RegionGeneralizerServiceImpl, RegionTreeFactory}
import ru.yandex.vertis.telepony.operational.Operational
import ru.yandex.vertis.telepony.service.logging.LoggingPhoneAnalyzerService
import ru.yandex.vertis.telepony.service.metered.MeteredPhoneAnalyzerService
import ru.yandex.vertis.telepony.service.{PhoneService, PhoneServiceSpec}

import scala.io.Codec

/**
  * @author evans
  */
class PhoneServiceImplSpec extends PhoneServiceSpec {

  override def phoneService: PhoneService = {
    val phoneAnalyzer =
      new DelegatePhoneAnalyzerService(RegionServiceFactory.buildFromResource("regions.csv"))
        with MeteredPhoneAnalyzerService
        with LoggingPhoneAnalyzerService {
        override def prometheusRegistry: PrometheusRegistry =
          Operational.default(VertisRuntime).prometheusRegistry
      }

    val regionGeneralizer = {
      val regionTree = RegionTreeFactory.buildFromResource("geobase.xml")(Codec.UTF8)
      new RegionGeneralizerServiceImpl(regionTree)
    }

    new PhoneServiceImpl(phoneAnalyzer, regionGeneralizer)
  }
}
