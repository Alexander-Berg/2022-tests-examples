package ru.yandex.vertis.moderation.model.signal

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase

/**
  * @author semkagtn
  */
@RunWith(classOf[JUnitRunner])
class SignalKeyHashSpec extends SpecBase {

  import Signal.keyHash

  "Signal.keyHash" should {
    "correctly works" in {
      keyHash("automatic_COMPLAINTS_warn_USER_RESELLER") should be("2ba9f5c029")
      keyHash("manual_ban_NO_ANSWER") should be("19712ab30c")
      keyHash("") should be("d41d8cd98f")
      keyHash(" ") should be("7215ee9c7d")
    }
  }
}
