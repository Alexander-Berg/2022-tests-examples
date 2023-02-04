package ru.yandex.auto.vin.decoder.report.converters.raw.utils

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class CommonUtilsSpec extends AnyWordSpecLike with Matchers {

  "CommonUtils" should {

    "correct build quality" in {

      val res =
        CommonUtils.buildQuality(10, Set.empty, forOffer = false, hasFines = false, hasLeasings = false, 0, None)
      res.getQuality shouldBe 10
    }

    "correct build quality for offer" in {

      val res = CommonUtils.buildQuality(10, Set.empty, forOffer = true, hasFines = false, hasLeasings = false, 0, None)
      res.getQuality shouldBe 9
    }

    "correct increase quality for offer if there is advantage tag" in {

      val res = CommonUtils.buildQuality(
        10,
        Set("excellent_price"),
        forOffer = true,
        hasFines = false,
        hasLeasings = false,
        0,
        None
      )
      res.getQuality shouldBe 10
    }

    "correct increase quality for offer if there is more than one advantage tag" in {

      val res = CommonUtils.buildQuality(
        10,
        Set("excellent_price", "no_accidents", "one_owner", "good_price", "almost_new", "proven_owner"),
        forOffer = true,
        hasFines = false,
        hasLeasings = false,
        0,
        None
      )
      res.getQuality shouldBe 15
    }

    "do not increase quality if there is advantage tag and not offer" in {

      val res = CommonUtils.buildQuality(
        10,
        Set("excellent_price", "no_accidents", "one_owner", "good_price", "almost_new", "proven_owner"),
        forOffer = false,
        hasFines = false,
        hasLeasings = false,
        0,
        None
      )
      res.getQuality shouldBe 10
    }

    "increase quality if there are some fines" in {

      val res = CommonUtils.buildQuality(
        entitiesCount = 10,
        tags = Set.empty,
        forOffer = false,
        hasFines = true,
        hasLeasings = false,
        0,
        None
      )
      res.getQuality shouldBe 11
    }

    "increase quality if there are some programs" in {

      val res = CommonUtils.buildQuality(
        entitiesCount = 10,
        tags = Set.empty,
        forOffer = false,
        hasFines = false,
        hasLeasings = false,
        2,
        None
      )
      res.getQuality shouldBe 12
    }

    "increase quality if there are some leasings" in {

      val res = CommonUtils.buildQuality(
        entitiesCount = 10,
        tags = Set.empty,
        forOffer = false,
        hasFines = false,
        hasLeasings = true,
        0,
        None
      )
      res.getQuality shouldBe 11
    }
  }
}
