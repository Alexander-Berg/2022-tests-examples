package ru.yandex.vertis.billing.model_core

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
  * Specs on [[Phone]]
  *
  * @author sunlight
  * @author alesavin
  */
class PhoneSpec extends AnyWordSpec with Matchers {

  "Phone" should {
    "be parsed from correct string representation" in {
      Phone.unapply("7(812)9238193") should
        be(Some(Phone("7", "812", "9238193")))
      Phone.unapply("7(812)00000") should
        be(Some(Phone("7", "812", "00000")))
      Phone.unapply("0(2)10000") should
        be(Some(Phone("0", "2", "10000")))

      Phone.unapply("+7(812)9238193") should be(None)
      Phone.unapply("++7(812)9238193") should be(None)
      Phone.unapply("7812)9238193") should be(None)
      Phone.unapply("7(8129238193") should be(None)
      Phone.unapply("7(812)9a238193") should be(None)
      Phone.unapply("7(8a12)9238193") should be(None)
      Phone.unapply("a(8a12)9238193") should be(None)
      Phone.unapply("7a(8a12)9238193") should be(None)
      Phone.unapply("sadfsdf") should be(None)
    }
  }
}
