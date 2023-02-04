package ru.yandex.vertis.moderation.kafka.externalupdates.converters

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase

/**
  * @author semkagtn
  */
@RunWith(classOf[JUnitRunner])
class AutoruEventsConverterSpec extends SpecBase {

  "AutoruEventsConverterSpec.UserRegex" should {
    "correctly extract" in {
      "user:123" match {
        case AutoruEventsConverter.UserRegex(id) => id shouldBe "123"
      }
    }
  }

  "AutoruEventsConverterSpec.DealerRegex" should {
    "correctly extract" in {
      "dealer:123" match {
        case AutoruEventsConverter.DealerRegex(id) => id shouldBe "123"
      }
    }
  }
}
