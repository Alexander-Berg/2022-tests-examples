package ru.yandex.vertis.telepony.service.impl

import ru.yandex.vertis.telepony.service.{PhoneAnalyzerService, PhoneAnalyzerServiceSpec}

/**
  * @author evans
  */
class SimplePhoneAnalyzerServiceSpec extends PhoneAnalyzerServiceSpec {
  override def phoneAnalyzer: PhoneAnalyzerService = new SimplePhoneAnalyzerServiceImpl
}
