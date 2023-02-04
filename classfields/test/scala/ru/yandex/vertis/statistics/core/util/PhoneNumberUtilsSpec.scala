package ru.yandex.vertis.statistics.core.util

import org.scalatest.{Matchers, WordSpec}

/**
  *
  * @author zvez
  */
class PhoneNumberUtilsSpec extends WordSpec with Matchers {

  "PhoneNumberUtils.normalize" should {
    import PhoneNumberUtils.normalize

    "not fail on empty strings" in {
      normalize("") shouldBe ""
    }

    "leave valid numbers unchanged" in {
      val validNumber = "+79052572910"
      normalize(validNumber) shouldBe validNumber
    }

    "fix numbers" in {
      normalize("9853929592") shouldBe "+79853929592"
      normalize("77471858604") shouldBe "+77471858604"
    }

    "fix '(' and ')'" in {
      normalize("7(911)9326474") shouldBe "+79119326474"
    }

  }

}
