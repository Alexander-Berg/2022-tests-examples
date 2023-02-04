package ru.yandex.vertis.billing.model_core

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers

/**
  * Specs on [[Business]]
  *
  * @author alesavin
  */
class BusinessOfferIdSpec extends AnyWordSpec with Matchers {

  "BusinessOfferId" should {
    "be provided from db representation" in {
      import Business._
      unapply("biz_1") should be(Some(Business("1")))
      unapply("biz_1000") should be(Some(Business("1000")))
      unapply("biz_1196603805") should be(Some(Business("1196603805")))
      unapply("biz_") should be(None)
      unapply("biz#a") should be(None)
      unapply("_1000") should be(None)
      unapply("_") should be(None)
      unapply("_biz#1000") should be(None)
    }
  }
}
