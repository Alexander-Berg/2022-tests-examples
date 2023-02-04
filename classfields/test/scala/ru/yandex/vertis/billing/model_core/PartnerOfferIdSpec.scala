package ru.yandex.vertis.billing.model_core

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
  * Specs on [[Business]]
  *
  * @author alesavin
  */
class PartnerOfferIdSpec extends AnyWordSpec with Matchers {

  "PartnerOfferId" should {
    "be provided from db representation" in {
      import OfferId._
      unapply("1008241476#444444") should
        be(Some(PartnerOfferId("1008241476", "444444")))
      unapply("1#2") should
        be(Some(PartnerOfferId("1", "2")))
    }
  }
}
