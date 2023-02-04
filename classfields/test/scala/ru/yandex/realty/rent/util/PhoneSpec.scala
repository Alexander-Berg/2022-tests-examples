package ru.yandex.realty.rent.util

import org.junit.runner.RunWith
import org.scalatest.WordSpecLike
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class PhoneSpec extends WordSpecLike {

  import Phone._

  "Phone query transform function" should {
    "correctly detect and transform number to default format" in {
      assert(transformPhoneQuery("+7(904)2166222") == "79042166222")
      assert(transformPhoneQuery("+7(904)216-62-22") == "79042166222")
      assert(transformPhoneQuery("8(904)216-62-22") == "79042166222")
      assert(transformPhoneQuery("7-904-216-62-22") == "79042166222")
      assert(transformPhoneQuery("7 904 216 62 22") == "79042166222")
      assert(transformPhoneQuery("no-phone") == "no-phone")
    }
  }

  "Phone pan mask function" should {
    "correctly detect right and wrong pan masks" in {
      assert(isPhonePanMask("+7 (951) ***-**-72"))
      assert(isPhonePanMask("+8 (123) ***-72-**"))
      assert(isPhonePanMask("+9 (4567) 123-**-**"))
      assert(!isPhonePanMask("+asd (4567) 123-**-**"))
    }
  }
}
