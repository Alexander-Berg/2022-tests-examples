package ru.yandex.realty.util

import org.junit.runner.RunWith
import org.scalatest.{FlatSpec, Matchers}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.util.PhoneUtils.{format, normalizeIfValid}

@RunWith(classOf[JUnitRunner])
class PhoneUtilsSpec extends FlatSpec with Matchers {

  behavior of "normalizeIfValid()"

  it should "accept correct +7 phone" in {
    normalizeIfValid("+7 (123) 456-78-90") shouldBe Some("71234567890")
  }

  it should "accept correct 7 phone" in {
    normalizeIfValid("7 (123) 456-78-90") shouldBe Some("71234567890")
  }

  it should "accept correct 8 phone" in {
    normalizeIfValid("8 (123) 456-78-90") shouldBe Some("71234567890")
  }

  it should "accept correct 10-digit phone" in {
    normalizeIfValid("(123) 456-78-90") shouldBe Some("71234567890")
  }

  it should "reject less-than-10-digit phone" in {
    normalizeIfValid("123456789") shouldBe None
  }

  it should "reject phone with non-Russian code" in {
    normalizeIfValid("1 (123) 4567890") shouldBe None
  }

  behavior of "format()"

  it should "format phone" in {
    format("70004327767") shouldBe "+7 (000) 432-77-67"
    format("+79060255580") shouldBe "+7 (906) 025-55-80"
    format("89060255580") shouldBe "+7 (906) 025-55-80"
    format("89060255580") shouldBe "+7 (906) 025-55-80"
    format("7649123") shouldBe "7649123"
  }
}
