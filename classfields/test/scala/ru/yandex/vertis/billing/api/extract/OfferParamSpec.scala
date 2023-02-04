package ru.yandex.vertis.billing.api.extract

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.api.extract.OfferParam._
import ru.yandex.vertis.billing.model_core._

import scala.util.matching.Regex

/**
  * Specs on [[OfferParam]]
  *
  * @author alesavin
  */
class OfferParamSpec extends AnyWordSpec with Matchers {

  private def checkPattern(regex: Regex, sequence: String) =
    regex.pattern.matcher(sequence).matches() shouldBe true

  "OfferParam" should {
    "test match" in {
      checkPattern(BusinessOfferPattern, "Business(1196603805)")
      checkPattern(PartnerOfferPattern, "PartnerOffer(1,2)")
      checkPattern(PartnerOfferPattern, "PartnerOffer(1,2)")
      checkPattern(NewBuildingOfferPattern, "NewBuilding(3,4)")
      checkPattern(PuidOfferPattern, "Puid(5,6)")
      checkPattern(CookieOfferPattern, "Cookie(7,8)")
      checkPattern(SuburbanOfferPattern, "Suburban(3,4)")
    }
    "provide BusinessOfferId" in {
      unapply("Business(1196603805)") shouldBe Some(Business("1196603805"))
      unapply("Business(-1)") shouldBe Some(Business("-1"))
      unapply("Business(1196603805 )") shouldBe Some(Business("1196603805 "))
      unapply("Business(1196603805a)") shouldBe Some(Business("1196603805a"))
      unapply("Business(1196603805") shouldBe None
      unapply("Business(aaa)") shouldBe Some(Business("aaa"))
      unapply("1196603805") shouldBe None
    }

    "provide PartnerOfferId" in {
      unapply("PartnerOffer(1,2)") shouldBe Some(PartnerOfferId("1", "2"))
      unapply("PartnerOffer(14444,27777)") shouldBe Some(PartnerOfferId("14444", "27777"))
      unapply("PartnerOffer(part1,off2)") shouldBe Some(PartnerOfferId("part1", "off2"))
      unapply("Offer(14444,27777)") shouldBe None
      unapply("PartnerOffer(part1)") shouldBe None
      unapply("PartnerOffer(part1,333,)") shouldBe None
    }
    "provide ServiceObject with kind NewBuilding" in {
      import ServiceObject.Kinds._

      unapply("NewBuilding(1,1)") shouldBe Some(ServiceObject(NewBuilding, "1", "1"))
      unapply("NewBuilding(aaaaa,aaa)") shouldBe Some(ServiceObject(NewBuilding, "aaaaa", "aaa"))
      unapply("_NewBuilding(1,1)") shouldBe None
      unapply("_NewBuilding(1)") shouldBe None
      unapply("NewBuilding1)") shouldBe None
    }
    "provide ServiceObject with kind Suburban" in {
      import ServiceObject.Kinds._

      unapply("Suburban(1,1)") shouldBe Some(ServiceObject(Suburban, "1", "1"))
      unapply("Suburban(aaaaa,aaa)") shouldBe Some(ServiceObject(Suburban, "aaaaa", "aaa"))
      unapply("_Suburban(1,1)") shouldBe None
      unapply("_Suburban(1)") shouldBe None
      unapply("Suburban1)") shouldBe None
    }

    "provide UserOfferId with uid" in {

      unapply("Puid(1,1)") shouldBe Some(UserOfferId(Uid(1L), "1"))
      unapply("Puid(1000,1ddd)") shouldBe Some(UserOfferId(Uid(1000L), "1ddd"))
      unapply("Puid_(1000,1ddd)") shouldBe None
      unapply("Puid(a,1ddd)") shouldBe None
      unapply("Puid(1,1ddd") shouldBe None
    }
    "provide UserOfferId with cookie" in {
      unapply("Cookie(1,1)") shouldBe Some(UserOfferId(YandexUid("1"), "1"))
      unapply("Cookie(drr,1ddd)") shouldBe Some(UserOfferId(YandexUid("drr"), "1ddd"))
      unapply("Cookie_(1000,1ddd)") shouldBe None
      unapply("Cookie(a1ddd)") shouldBe None
      unapply("Cookie(1,1ddd") shouldBe None
    }
  }
}
