package ru.auto.cabinet.util

import org.scalatest.flatspec.{AnyFlatSpec => FlatSpec}
import org.scalatest.matchers.should.Matchers

class PhoneUtilSpec extends FlatSpec with Matchers {
  it should "return parsed phone by mask without extension" in {
    val fullPhone = Phone("12345678901")
    val phoneMask = PhoneMask("1:3:7")

    val expectedCountryCode = CountryCode("1")
    val expectedCityCode = CityCode("234")
    val expectedPhoneNumber = PhoneNumber("5678901")

    val parsedPhoneNumberOpt =
      PhoneUtil.parsePhoneByMask(fullPhone, phoneMask)

    parsedPhoneNumberOpt.map { phoneInfo =>
      phoneInfo.countryCode shouldBe expectedCountryCode
      phoneInfo.cityCode shouldBe expectedCityCode
      phoneInfo.phoneNumber shouldBe expectedPhoneNumber
      phoneInfo.extension shouldBe None
    }.get
  }

  it should "return parsed phone by mask with extension" in {
    val fullPhone = Phone("123456789012")
    val phoneMask = PhoneMask("1:3:7:1")

    val expectedCountryCode = CountryCode("1")
    val expectedCityCode = CityCode("234")
    val expectedPhoneNumber = PhoneNumber("5678901")
    val expectedExtension = Extension("2")

    val parsedPhoneNumberOpt =
      PhoneUtil.parsePhoneByMask(fullPhone, phoneMask)

    parsedPhoneNumberOpt.map { phoneInfo =>
      phoneInfo.countryCode shouldBe expectedCountryCode
      phoneInfo.cityCode shouldBe expectedCityCode
      phoneInfo.phoneNumber shouldBe expectedPhoneNumber
      phoneInfo.extension shouldBe Some(expectedExtension)
    }.get
  }
  it should "return None on wrong phone mask" in {
    val fullPhone = Phone("12345678901")
    val phoneMask = PhoneMask("1:3:7:4")
    val parsedPhoneNumberOpt =
      PhoneUtil.parsePhoneByMask(fullPhone, phoneMask)

    parsedPhoneNumberOpt shouldBe None
  }
}
