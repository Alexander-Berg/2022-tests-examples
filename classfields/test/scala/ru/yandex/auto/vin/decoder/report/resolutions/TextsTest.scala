package ru.yandex.auto.vin.decoder.report.resolutions

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.auto.vin.decoder.model.{LicensePlate, VinCode}
import ru.yandex.auto.vin.decoder.report.processors.resolution.{Texts, UntrustedChecks}

class TextsTest extends AnyWordSpecLike with Matchers {

  private val vinCode = VinCode("JTMHT05J605098077")

  "Texts.getUntrustedText" should {
    "return correct messages for LP" in {
      val lp = Left(LicensePlate("H423PM777"))
      Texts.getUntrustedText(lp, rightWheel = false) shouldBe Texts.UntrustedLicensePlateText
    }

    "return correct messages for LP if right wheel" in {
      val lp = Left(LicensePlate("H423PM777"))
      Texts.getUntrustedText(lp, rightWheel = true) shouldBe Texts.UntrustedLicensePlateText
    }

    "return correct messages for VIN" in {
      val vin = Right(vinCode)
      Texts.getUntrustedText(vin, rightWheel = false) shouldBe Texts.UntrustedVinText
    }

    "return correct messages for right wheel" in {
      val vin = Right(vinCode)
      Texts.getUntrustedText(vin, rightWheel = true) shouldBe Texts.UntrustedRightWheelVinText
    }

    "return correct messages for old car" in {
      val vin = Right(vinCode)
      Texts.getUntrustedText(
        vin,
        rightWheel = false,
        Some(UntrustedChecks(isOld = true, isFromDealer = false, isNotRegisteredInRussia = false))
      ) shouldBe Texts.OldCarUntrusted
    }

    "return correct messages if dealer and not registered" in {
      val vin = Right(vinCode)
      Texts.getUntrustedText(
        vin,
        rightWheel = false,
        Some(UntrustedChecks(isOld = false, isFromDealer = true, isNotRegisteredInRussia = false))
      ) shouldBe Texts.UntrustedNotRegistered
    }

    "return correct messages if dealer but registered in Russia" in {
      val vin = Right(vinCode)
      Texts.getUntrustedText(
        vin,
        rightWheel = false,
        Some(UntrustedChecks(isOld = false, isFromDealer = true, isNotRegisteredInRussia = false))
      ) shouldBe Texts.UntrustedNotRegistered
    }

    "return correct messages if private seller and not registered in Russia" in {
      val vin = Right(vinCode)
      Texts.getUntrustedText(
        vin,
        rightWheel = false,
        Some(UntrustedChecks(isOld = false, isFromDealer = false, isNotRegisteredInRussia = true))
      ) shouldBe Texts.UntrustedNotRegistered
    }
  }

}
