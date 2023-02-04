package ru.yandex.vertis.telepony.service.impl

import ru.yandex.vertis.telepony.service.{PhoneAnalyzerService, PhoneAnalyzerServiceSpec}

/**
  * @author evans
  */
class PhoneAnalyzerServiceImplSpec extends PhoneAnalyzerServiceSpec {

  override val phoneAnalyzer: PhoneAnalyzerService =
    RegionServiceFactory.buildFromResource("regions.csv")
}
