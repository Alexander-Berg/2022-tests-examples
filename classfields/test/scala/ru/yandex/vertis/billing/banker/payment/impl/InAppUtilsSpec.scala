package ru.yandex.vertis.billing.banker.payment.impl

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.util.Success

/**
  * Spec on [[InAppUtils]]
  *
  * @author ruslansd
  */
class InAppUtilsSpec extends AnyWordSpec with Matchers {

  "InAppUtils" should {

    "parse cost from valid product ids" in {
      val id1 = "iap_buy_coins_10"
      val id2 = "iab_buy_coins_10"
      InAppUtils.extractAmount(id1) shouldBe Success(1000)
      InAppUtils.extractAmount(id2) shouldBe Success(1000)
    }

    "fail on invalid format" in {
      val ids = Iterable("iad_buy_coins_10", "iap_buy_coins_ad", "iap_buy_coins_ad")
      ids.foreach(id => (InAppUtils.extractAmount(id) should be).a(Symbol("Failure")))
    }
  }

}
