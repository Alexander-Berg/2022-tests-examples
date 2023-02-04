package ru.yandex.vertis.moderation.util

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase

@RunWith(classOf[JUnitRunner])
class PhoneUtilsSpec extends SpecBase {

  "PhoneUtilsSpec.isE164format" should {

    "return true if phone properly formatted" in {
      PhoneUtils.isE164format("+79998887766") shouldBe true // RU
      PhoneUtils.isE164format("+15058887766") shouldBe true // USA
      PhoneUtils.isE164format("+861067645489") shouldBe true // China
      PhoneUtils.isE164format("+380482111111") shouldBe true // UA
    }

    "return false if phone doesn't have + sign" in {
      PhoneUtils.isE164format("79998887766") shouldBe false
    }

    "return false if phone starts from 8" in {
      PhoneUtils.isE164format("89998887766") shouldBe false
    }

    "return false if phone has formatting symbols" in {
      PhoneUtils.isE164format("+7(999)888-77-66") shouldBe false
    }

    "return false if phone has too few digits" in {
      PhoneUtils.isE164format("+7999888776") shouldBe false
    }

    "return false if phone hash too many digits" in {
      PhoneUtils.isE164format("+799988877665") shouldBe false
    }
  }
}
