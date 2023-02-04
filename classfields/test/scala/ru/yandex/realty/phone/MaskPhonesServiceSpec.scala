package ru.yandex.realty.phone

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase

@RunWith(classOf[JUnitRunner])
class MaskPhonesServiceSpec extends SpecBase {

  "MaskPhonesService in mask" should {
    "mask phone number" in {
      MaskPhonesService.mask("+79998502334") shouldBe "+7999850****"
    }
  }
}
