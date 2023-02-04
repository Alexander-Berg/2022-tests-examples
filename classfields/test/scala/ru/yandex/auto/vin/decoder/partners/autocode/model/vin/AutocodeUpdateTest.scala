package ru.yandex.auto.vin.decoder.partners.autocode.model.vin

import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.model.vin._

class AutocodeUpdateTest extends AnyFlatSpec with MockitoSugar with Matchers with BeforeAndAfter {

  private val vin = VinCode("VF7XS9HHCEZ005623")

  "AutocodeUpdate" should "apply in case classes" in {
    AutocodeUpdate.apply(vin, "aa", -2, 4343) shouldBe an[AutocodeInvalid]
    AutocodeUpdate.apply(vin, "aa", -3, 4343) shouldBe an[AutocodeMarkedForReUpdate]
    AutocodeUpdate.apply(vin, "", -3, 0) shouldBe an[AutocodeMarkedForUpdate]
    AutocodeUpdate.apply(vin, "asd", -1, 4343) shouldBe an[AutocodeMarkedForUpdate]
    AutocodeUpdate.apply(vin, "add", 312, 4343) shouldBe an[AutocodeUpdated]
    AutocodeUpdate.apply(vin, "assd", 312, 0) shouldBe an[AutocodeNotCompletedUpdate]
    AutocodeUpdate.apply(vin, "assd", 312, -3) shouldBe an[AutocodeRevisit]
    AutocodeUpdate.apply(vin, "", 0, 0) shouldBe an[AutocodeNeverUpdated]
  }

}
