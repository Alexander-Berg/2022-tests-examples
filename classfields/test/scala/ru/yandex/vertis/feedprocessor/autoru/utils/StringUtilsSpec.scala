package ru.yandex.vertis.feedprocessor.autoru.utils

import StringUtils.Implicits._
import ru.yandex.vertis.feedprocessor.WordSpecBase
import ru.yandex.vertis.feedprocessor.autoru.model.OfferError

class StringUtilsSpec extends WordSpecBase {
  "StringUtils" should {
    "throw Offer error max length exceeded" in {
      val slashes = List.fill(300)("""\""").mkString("")
      val str = s"""LXD364$slashes/D367"""

      intercept[OfferError] {
        str.maxLengthValidation("test")
      }
    }

    "validate success" in {
      val str = """LXD364\/D367"""
      str.maxLengthValidation("test") shouldBe str
    }
  }
}
