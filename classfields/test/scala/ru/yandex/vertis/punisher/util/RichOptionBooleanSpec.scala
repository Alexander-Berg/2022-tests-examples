package ru.yandex.vertis.punisher.util

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vertis.punisher.BaseSpec

@RunWith(classOf[JUnitRunner])
class RichOptionBooleanSpec extends BaseSpec {

  "RichOptionBoolean" should {
    "OR" in {
      false || Some(false) should be(false)
      Some(false) || false should be(false)
      Some(false) || Some(false) should be(false)

      true || Some(false) should be(true)
      Some(true) || false should be(true)
      Some(true) || Some(false) should be(true)
      true || None should be(true)
      Some(true) || false should be(true)
      Some(true) || Some(false) should be(true)

      false || Some(true) should be(true)
      Some(false) || true should be(true)
      Some(false) || Some(true) should be(true)

      true || Some(true) should be(true)
      Some(true) || true should be(true)
      Some(true) || true should be(true)

      false || None should be(false)
      None || false should be(false)
      true || None should be(true)
      None || true should be(true)
      None || None should be(false)
    }

    "AND" in {
      false && Some(false) should be(false)
      Some(false) && false should be(false)
      Some(false) && Some(false) should be(false)

      true && Some(false) should be(false)
      Some(true) && false should be(false)
      Some(true) && false should be(false)

      false && Some(true) should be(false)
      Some(false) && true should be(false)
      Some(false) && true should be(false)

      true && Some(true) should be(true)
      Some(true) && true should be(true)
      Some(true) && true should be(true)

      false && None should be(false)
      None && false should be(false)
      true && None should be(false)
      None && true should be(false)
      None && None should be(false)
    }
  }
}
