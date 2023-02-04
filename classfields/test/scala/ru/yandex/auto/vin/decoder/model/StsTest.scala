package ru.yandex.auto.vin.decoder.model

import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.auto.vin.decoder.model.Sts.splitToParts

class StsTest extends AnyWordSpec {
  "Sts" should {
    "parse mixed series and number" in {
      val sts = Sts("62ТУ788650")
      val parts = splitToParts(sts)
      assert(parts.nonEmpty)
      assert(parts.get == ("62ТУ", "788650"))
    }

    "parse numeric series and number" in {
      val sts = Sts("6266788650")
      val parts = splitToParts(sts)
      assert(parts.nonEmpty)
      assert(parts.get == ("6266", "788650"))
    }
  }
}
