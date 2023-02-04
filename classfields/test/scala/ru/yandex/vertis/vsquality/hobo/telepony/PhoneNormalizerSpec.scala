package ru.yandex.vertis.vsquality.hobo.telepony

import ru.yandex.vertis.vsquality.hobo.model.Phone
import ru.yandex.vertis.vsquality.hobo.telephony.DefaultPhoneNormalizer
import ru.yandex.vertis.vsquality.hobo.util.SpecBase

/**
  * @author semkagtn
  */

class PhoneNormalizerSpec extends SpecBase {

  "DefaultPhoneNormalizer" should {
    val params: Seq[(Phone, Phone)] =
      Seq(
        ("+79313540464", "9313540464"),
        ("89313540464", "9313540464"),
        ("79313540464", "9313540464"),
        ("  +79313540464", "9313540464"),
        ("+79313540464  ", "9313540464"),
        ("+7 931 354 04 64", "9313540464"),
        ("+7(931)354-04-64", "9313540464")
      )

    params.foreach { case (input, expectedResult) =>
      s"convert '$input' to '$expectedResult'" in {
        val actualResult = DefaultPhoneNormalizer.normalize(input)
        actualResult shouldBe expectedResult
      }
    }
  }
}
