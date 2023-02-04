package ru.yandex.vertis.telepony.service

import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.exception.CantParsePhone
import ru.yandex.vertis.telepony.model.{Operators, Phone, PhoneInfo, PhoneTypes}

/**
  * @author evans
  */
trait PhoneServiceSpec extends SpecBase {

  def phoneService: PhoneService

  private val defaultOperatorName = "ПАО \"МегаФон\""
  private val defaultOperator = Some(Operators.Megafon)

  "Phone service" should {
    "get info" when {
      "parse phone 1" in {
        val input = "89312320032"
        val expected =
          Some(
            PhoneInfo(Phone("+79312320032"), 2, PhoneTypes.Mobile, 10174, defaultOperatorName, defaultOperator)
          )
        phoneService.provideInfo(input).futureValue shouldEqual expected
      }
      "parse phone 2" in {
        val input = "+79312320032"
        val expected =
          Some(
            PhoneInfo(Phone("+79312320032"), 2, PhoneTypes.Mobile, 10174, defaultOperatorName, defaultOperator)
          )
        phoneService.provideInfo(input).futureValue shouldEqual expected
      }
      "parse phone 3" in {
        val input = "79312320032"
        val expected =
          Some(
            PhoneInfo(Phone("+79312320032"), 2, PhoneTypes.Mobile, 10174, defaultOperatorName, defaultOperator)
          )
        phoneService.provideInfo(input).futureValue shouldEqual expected
      }
      "parse phone 4" in {
        val input = "(931)232-00-32"
        val expected =
          Some(
            PhoneInfo(Phone("+79312320032"), 2, PhoneTypes.Mobile, 10174, defaultOperatorName, defaultOperator)
          )
        phoneService.provideInfo(input).futureValue shouldEqual expected
      }
    }
    "get nothing" when {
      "turkish phone number passed" in {
        val input = "+905340775966"
        phoneService.provideInfo(input).futureValue shouldEqual None
      }
      "united states number passed" in {
        val input = "+18325643248"
        phoneService.provideInfo(input).futureValue shouldEqual None
      }
    }
    "fail" when {
      "wrong format number passed" in {
        val input = "12345"
        phoneService.provideInfo(input).failed.futureValue shouldBe an[CantParsePhone]
      }
      "wrong country number passed" in {
        val input = "+39111234567"
        phoneService.provideInfo(input).failed.futureValue shouldBe an[CantParsePhone]
      }
      "non exist number passed" in {
        val input = "+71231234567"
        phoneService.provideInfo(input).failed.futureValue shouldBe an[CantParsePhone]
      }
    }
  }
}
