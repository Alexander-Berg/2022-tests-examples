package ru.yandex.vertis.telepony.mts.impl

import ru.yandex.vertis.telepony.mts.{PhoneMappingService, PhoneMappingServiceSpec}
import ru.yandex.vertis.telepony.util.phone.PhoneMappingRulesProvider

/**
  * @author evans
  */
class PhoneMappingServiceImplSpec extends PhoneMappingServiceSpec {

  override val phoneMappingService: PhoneMappingService =
    new PhoneMappingServiceImpl(PhoneMappingRulesProvider.DefaultRules)
}
