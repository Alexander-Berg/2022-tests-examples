package ru.yandex.vertis.telepony.model

import ru.yandex.vertis.telepony.SpecBase

class PhoneSpec extends SpecBase {

  private def checkRussianPhone(phone: Phone)(expectedValue: String): Unit = {
    phone match {
      case r: Phone =>
        r.value shouldBe expectedValue
      case result =>
        fail(s"Unexpected result $result")
    }
  }

  private def checkUnusualRussianPhone(phone: Phone)(expectedValue: String): Unit = {
    phone match {
      case r: UnusualRussianPhone =>
        r.value shouldBe expectedValue
      case result =>
        fail(s"Unexpected result $result")
    }
  }

  private def checkInternationalPhone(phone: Phone)(expectedValue: String): Unit = {
    phone match {
      case i: InternationalPhone =>
        i.value shouldBe expectedValue
      case result =>
        fail(s"Unexpected result $result")
    }
  }

  private val RussianNumber = "+78005553535"

  "Phone" should {
    "allow valid russian phones" when {
      "russian +7.* phone number passed" in {
        checkRussianPhone(Phone(RussianNumber))(RussianNumber)
      }
      "russian 8.* phone number passed" in {
        val phoneNumber = "88005553535"
        checkRussianPhone(Phone(phoneNumber))(RussianNumber)
      }
      "russian phone without country code passed" in {
        val phoneNumber = "8005553535"
        checkRussianPhone(Phone(phoneNumber))(RussianNumber)
      }
    }
    "allow valid unusual russian phones" when {
      "russian +78028012350 phone number passed" in {
        val phoneNumber = "+78028012350"
        checkUnusualRussianPhone(Phone(phoneNumber))(phoneNumber)
      }
      "mts +76760895321 phone number passed" in {
        val phoneNumber = "+76760895321"
        checkUnusualRussianPhone(Phone(phoneNumber))(phoneNumber)
      }
    }
    "allow valid international phones" when {
      "ukrainian phone number passed" in {
        val phoneNumber = "+380664324213"
        checkInternationalPhone(Phone(phoneNumber))(phoneNumber)
      }
      "belarusian phone number passed" in {
        val phoneNumber = "+375291542220"
        checkInternationalPhone(Phone(phoneNumber))(phoneNumber)
      }
      "turkish phone number passed" in {
        val phoneNumber = "+905340775966"
        checkInternationalPhone(Phone(phoneNumber))(phoneNumber)
      }
    }
    "not allow invalid phones" when {
      "invalid string passed" in {
        val phoneNumber = "I am not a number"
        intercept[IllegalArgumentException] {
          Phone(phoneNumber)
        }
      }
    }
  }

}
