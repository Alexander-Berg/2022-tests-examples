package ru.yandex.vertis.vsquality.hobo.model

import ru.yandex.vertis.vsquality.hobo.model.generators.CoreGenerators
import ru.yandex.vertis.vsquality.hobo.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.vsquality.hobo.util.{DateTimeUtil, SpecBase}

/**
  * @author semkagtn
  */
class PhoneCallKeySpec extends SpecBase {

  "extractProvider" should {

    "correctly extract provider" in {
      val expectedProvider = CoreGenerators.ProviderGen.next
      val key = PhoneCall.generateKey(DateTimeUtil.now(), expectedProvider)
      val actualProvider = PhoneCallKey.extractProvider(key)

      actualProvider should smartEqual(Some(expectedProvider))
    }

    "return None if no provider" in {
      val key: PhoneCallKey = "no provider"

      val actualProvider = PhoneCallKey.extractProvider(key)
      actualProvider should smartEqual(None)
    }

    "return None if nonexistent provider" in {
      val key: PhoneCallKey = "prefix-999"

      val actualProvider = PhoneCallKey.extractProvider(key)
      actualProvider should smartEqual(None)
    }
  }
}
