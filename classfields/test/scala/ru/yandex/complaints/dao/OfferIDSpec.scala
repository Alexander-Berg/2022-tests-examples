package ru.yandex.complaints.dao

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}

/**
  * Specs for [[OfferID]]
  *
  * @author alesavin
  */
@RunWith(classOf[JUnitRunner])
class OfferIDSpec
  extends WordSpec
  with Matchers {
  
  "OfferID" should {
    "unapply plain id" in {
      OfferID.unapply("a") should be (Some(Plain("a")))
      OfferID.unapply("1") should be (Some(Plain("1")))
      OfferID.unapply("a1") should be (Some(Plain("a1")))
      OfferID.unapply("a1-*") should be (Some(Plain("a1-*")))
      OfferID.unapply("a,b,c") should be (Some(Plain("a,b,c")))
      OfferID.unapply("   ") should be (Some(Plain("   ")))
      OfferID.unapply("") should be (None)
    }
    "unapply autoru sto" in {
      OfferID.unapply("autoru-sto:a") should be (Some(AutoruSto("a")))
      OfferID.unapply("autoru-sto:1") should be (Some(AutoruSto("1")))
      OfferID.unapply("autoru-sto:a-*") should be (Some(AutoruSto("a-*")))
      OfferID.unapply("autoru-sto:a,b,c") should be (Some(AutoruSto("a,b,c")))
      OfferID.unapply("autoru-sto:a:b:c") should be (Some(AutoruSto("a:b:c")))
      OfferID.unapply("autoru-sto:    ") should be (Some(AutoruSto("    ")))

      OfferID.unapply("autoru-sto:") should be (Some(Plain("autoru-sto:")))
      OfferID.unapply(" autoru-sto:a") should be (Some(Plain(" autoru-sto:a")))
      OfferID.unapply("autoru_sto:a") should be (Some(Plain("autoru_sto:a")))
    }
  }
}
