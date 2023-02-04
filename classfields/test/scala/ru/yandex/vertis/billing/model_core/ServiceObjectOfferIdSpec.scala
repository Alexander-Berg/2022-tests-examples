package ru.yandex.vertis.billing.model_core

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
  * Specs on [[ServiceObject]]
  *
  * @author alesavin
  */
class ServiceObjectOfferIdSpec extends AnyWordSpec with Matchers {

  "ServiceObjectOfferId" should {
    "be provided from db representation (NewBuilding)" in {
      import ServiceObject._

      unapply("obj_0@1/1") shouldBe Some(ServiceObject(Kinds.NewBuilding, "1", "1"))
      unapply("obj_0@aaaa/123") shouldBe Some(ServiceObject(Kinds.NewBuilding, "aaaa", "123"))
      unapply("obj_0@1234567890/fff") shouldBe Some(ServiceObject(Kinds.NewBuilding, "1234567890", "fff"))
      unapply("obj_0@/fff") shouldBe Some(ServiceObject(Kinds.NewBuilding, "", "fff"))

      unapply("obj_0#234567890") shouldBe None
      unapply("obj_0#234567890/") shouldBe None
      unapply("obj_333@1234567890") shouldBe None
      unapply("1obj_0@1234567890") shouldBe None
      unapply("obj_0_1234567890") shouldBe None
      unapply("obj_a@1234567890") shouldBe None
      unapply("_") shouldBe None
      unapply("biz_11111") shouldBe None
      unapply("0@1234567890") shouldBe None
      unapply("1obj_0@1/1") shouldBe None
      unapply("obj_0_1/1") shouldBe None
    }

    "be provided from db representation (Suburban)" in {
      import ServiceObject._

      unapply("obj_1@1/1") shouldBe Some(ServiceObject(Kinds.Suburban, "1", "1"))
      unapply("obj_1@aaaa/123") shouldBe Some(ServiceObject(Kinds.Suburban, "aaaa", "123"))
      unapply("obj_1@1234567890/fff") shouldBe Some(ServiceObject(Kinds.Suburban, "1234567890", "fff"))
      unapply("obj_1@/fff") shouldBe Some(ServiceObject(Kinds.Suburban, "", "fff"))

      unapply("obj_1#234567890") shouldBe None
      unapply("obj_1#234567890/") shouldBe None
      unapply("obj_333@1234567890") shouldBe None
      unapply("1obj_1@1234567890") shouldBe None
      unapply("obj_1_1234567890") shouldBe None
      unapply("obj_a@1234567890") shouldBe None
      unapply("_") shouldBe None
      unapply("biz_11111") shouldBe None
      unapply("1@1234567890") shouldBe None
    }
  }
}
