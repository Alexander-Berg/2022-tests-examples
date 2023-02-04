package ru.auto.api.util

import org.scalatest.OptionValues
import ru.auto.api.BaseSpec
import ru.auto.api.util.StringUtils._

class StringUtilsSpec extends BaseSpec with OptionValues {
  "String utils" should {
    "encode url" in {
      val p1 = "рус"
      val p2 = ":;"
      val p3 = "a b"
      url" $p1 $p2 $p3 ".toString shouldBe " %D1%80%D1%83%D1%81 %3A%3B a+b "
    }

    "encode val on string start" in {
      val p1 = "?"
      url"$p1 on start".toString shouldBe "%3F on start"
    }

    "encode val on string end" in {
      val p1 = "&"
      url"on end $p1".toString shouldBe "on end %26"
    }

    "encode url safe obj" in {
      val p = url":%#"
      url"$p".toString shouldBe ":%#"
    }

    "split4" in {
      val s = "user:12345|79291112233|cars|100500-hash"
      val slit4 = StringUtils.split4('|')
      slit4.unapply(s).value shouldBe (("user:12345", "79291112233", "cars", "100500-hash"))
    }

    "escape '..'" in {
      val vin = "../../vin123"
      val url = url"/api/v1/comments/$vin/"
      assert(url.value == "/api/v1/comments/vin123/")
    }
  }
}
