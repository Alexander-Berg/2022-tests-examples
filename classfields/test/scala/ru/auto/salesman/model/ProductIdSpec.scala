package ru.auto.salesman.model

import ru.auto.salesman.model.ProductId._
import ru.auto.salesman.test.BaseSpec

class ProductIdSpec extends BaseSpec {

  "packagesWith" should {

    "return turbo for premium" in {
      packagesWith(Premium) shouldBe Set(Turbo)
    }

    "return turbo for special" in {
      packagesWith(Special) shouldBe Set(Turbo)
    }

    "return empty set for fresh" in {
      packagesWith(Fresh) shouldBe empty
    }

    "return empty set for reset" in {
      packagesWith(Reset) shouldBe empty
    }
  }

  "toString" should {

    "for Placement return all_sale_activate" in {
      Placement.toString shouldBe "all_sale_activate"
    }
    "for Premium return all_sale_premium" in {
      Premium.toString shouldBe "all_sale_premium"
    }
    "for PremiumOffer return all_sale_premium_offer" in {
      PremiumOffer.toString shouldBe "all_sale_premium_offer"
    }
    "for Top return all_sale_toplist" in {
      Top.toString shouldBe "all_sale_toplist"
    }
    "for Special return all_sale_special" in {
      Special.toString shouldBe "all_sale_special"
    }
    "for Fresh return all_sale_fresh" in {
      Fresh.toString shouldBe "all_sale_fresh"
    }
    "for Color return all_sale_color" in {
      Color.toString shouldBe "all_sale_color"
    }
    "for Turbo return package_turbo" in {
      Turbo.toString shouldBe "package_turbo"
    }
    "for Certification return certification" in {
      Certification.toString shouldBe "certification"
    }
    "for CertificationMobile return certification_mobile" in {
      CertificationMobile.toString shouldBe "certification_mobile"
    }
    "for Badge return all_sale_badge" in {
      Badge.toString shouldBe "all_sale_badge"
    }
    "for Express return package_express" in {
      Express.toString shouldBe "package_express"
    }
    "for Add return all_sale_add" in {
      Add.toString shouldBe "all_sale_add"
    }
    "for FreshOffers return fresh_subscribes" in {
      FreshOffers.toString shouldBe "fresh_subscribes"
    }
    "for FreshOffersPhones return fresh_subscribes_phones" in {
      FreshOffersPhones.toString shouldBe "fresh_subscribes_phones"
    }
    "for BeFirst15 return be_first_15" in {
      BeFirst15.toString shouldBe "be_first_15"
    }
    "for BeFirst30 return be_first_30" in {
      BeFirst30.toString shouldBe "be_first_30"
    }
    "for BeFirst45 return be_first_45" in {
      BeFirst45.toString shouldBe "be_first_45"
    }
    "for OffersHistoryReports return offers_history_reports" in {
      OffersHistoryReports.toString shouldBe "offers_history_reports"
    }
    "for Call return call" in {
      Call.toString shouldBe "call"
    }
    "for Vip return package_vip" in {
      Vip.toString shouldBe "package_vip"
    }
    "for VinHistory return vin-history" in {
      VinHistory.toString shouldBe "vin-history"
    }
    "for TradeInRequestCarsNew return trade-in-request:cars:new" in {
      TradeInRequestCarsNew.toString shouldBe "trade-in-request:cars:new"
    }
    "for TradeInRequestCarsUsed return trade-in-request:cars:used" in {
      TradeInRequestCarsUsed.toString shouldBe "trade-in-request:cars:used"
    }
    "for QuotaPlacementCarsUsed return quota:placement:cars:used" in {
      QuotaPlacementCarsUsed.toString shouldBe "quota:placement:cars:used"
    }
    "for QuotaPlacementCarsUsedPremium return quota:placement:cars:used:premium" in {
      QuotaPlacementCarsUsedPremium.toString shouldBe "quota:placement:cars:used:premium"
    }
    "for QuotaPlacementCarsNew return quota:placement:cars:new" in {
      QuotaPlacementCarsNew.toString shouldBe "quota:placement:cars:new"
    }
    "for QuotaPlacementCarsNewPremium return quota:placement:cars:new:premium" in {
      QuotaPlacementCarsNewPremium.toString shouldBe "quota:placement:cars:new:premium"
    }
    "for QuotaPlacementMoto return quota:placement:moto" in {
      QuotaPlacementMoto.toString shouldBe "quota:placement:moto"
    }
    "for QuotaPlacementCommercial return quota:placement:commercial" in {
      QuotaPlacementCommercial.toString shouldBe "quota:placement:commercial"
    }
    "for QuotaPriority return quota:priority" in {
      QuotaPriority.toString shouldBe "quota:priority"
    }
    "for QuotaPlacementPartsUsed return quota:placement:parts:used" in {
      QuotaPlacementPartsUsed.toString shouldBe "quota:placement:parts:used"
    }
  }

  "id" should {

    "for Placement return 1" in {
      Placement.id shouldBe 1
    }
    //val Placement = Value(1, "all_sale_activate")
    "for Premium return 2" in {
      Premium.id shouldBe 2
    }
    "for PremiumOffer return 3" in {
      PremiumOffer.id shouldBe 3
    }
    "for Top return 4" in {
      Top.id shouldBe 4
    }
    "for Special return 5" in {
      Special.id shouldBe 5
    }
    "for Fresh return 6" in {
      Fresh.id shouldBe 6
    }
    "for Color return 7" in {
      Color.id shouldBe 7
    }
    "for Turbo return 8" in {
      Turbo.id shouldBe 8
    }
    "for Certification return 9" in {
      Certification.id shouldBe 9
    }
    "for CertificationMobile return 10" in {
      CertificationMobile.id shouldBe 10
    }
    "for Badge return 11" in {
      Badge.id shouldBe 11
    }
    "for Express return 12" in {
      Express.id shouldBe 12
    }
    "for Add return 13" in {
      Add.id shouldBe 13
    }
    "for FreshOffers return 15" in {
      FreshOffers.id shouldBe 15
    }
    "for FreshOffersPhones return 16" in {
      FreshOffersPhones.id shouldBe 16
    }
    "for BeFirst15 return 17" in {
      BeFirst15.id shouldBe 17
    }
    "for BeFirst30 return 18" in {
      BeFirst30.id shouldBe 18
    }
    "for BeFirst45 return 19" in {
      BeFirst45.id shouldBe 19
    }
    "for OffersHistoryReports return 22" in {
      OffersHistoryReports.id shouldBe 22
    }
    "for Call return 20" in {
      Call.id shouldBe 20
    }
    "for Vip return 21" in {
      Vip.id shouldBe 21
    }
    "for VinHistory return 23" in {
      VinHistory.id shouldBe 23
    }
    "for TradeInRequestCarsNew return 24" in {
      TradeInRequestCarsNew.id shouldBe 24
    }
    "for TradeInRequestCarsUsed return 25" in {
      TradeInRequestCarsUsed.id shouldBe 25
    }
    "for QuotaPlacementCarsUsed return 100" in {
      QuotaPlacementCarsUsed.id shouldBe 100
    }
    "for QuotaPlacementCarsUsedPremium return 101" in {
      QuotaPlacementCarsUsedPremium.id shouldBe 101
    }
    "for QuotaPlacementCarsNew return 102" in {
      QuotaPlacementCarsNew.id shouldBe 102
    }
    "for QuotaPlacementCarsNewPremium return 103" in {
      QuotaPlacementCarsNewPremium.id shouldBe 103
    }
    "for QuotaPlacementMoto return 104" in {
      QuotaPlacementMoto.id shouldBe 104
    }
    "for QuotaPlacementCommercial return 105" in {
      QuotaPlacementCommercial.id shouldBe 105
    }
    "for QuotaPriority return 300" in {
      QuotaPriority.id shouldBe 300
    }
    "for QuotaPlacementPartsUsed return 301" in {
      QuotaPlacementPartsUsed.id shouldBe 301
    }
  }

  "alias" should {

    "for Placement return placement" in {
      alias(Placement) shouldBe "placement"
    }
    "for Premium return premium" in {
      alias(Premium) shouldBe "premium"
    }
    "for PremiumOffer return premium-offer" in {
      alias(PremiumOffer) shouldBe "premium-offer"
    }
    "for Top return top" in {
      alias(Top) shouldBe "top"
    }
    "for Special return special-offer" in {
      alias(Special) shouldBe "special-offer"
    }
    "for Fresh return boost" in {
      alias(Fresh) shouldBe "boost"
    }
    "for Color return highlighting" in {
      alias(Color) shouldBe "highlighting"
    }
    "for Certification return certification" in {
      alias(Certification) shouldBe "certification"
    }
    "for CertificationMobile return certification-mobile" in {
      alias(CertificationMobile) shouldBe "certification-mobile"
    }
    "for Badge return badge" in {
      alias(Badge) shouldBe "badge"
    }
    "for Express return express-package" in {
      alias(Express) shouldBe "express-package"
    }
    "for Vip return vip-package" in {
      alias(Vip) shouldBe "vip-package"
    }
    "for Turbo return turbo-package" in {
      alias(Turbo) shouldBe "turbo-package"
    }
    "for FreshOffers return fresh-offers" in {
      alias(FreshOffers) shouldBe "fresh-offers"
    }
    "for FreshOffersPhones return fresh-offers-phones" in {
      alias(FreshOffersPhones) shouldBe "fresh-offers-phones"
    }
    "for BeFirst15 return be-first-15" in {
      alias(BeFirst15) shouldBe "be-first-15"
    }
    "for BeFirst30 return be-first-30" in {
      alias(BeFirst30) shouldBe "be-first-30"
    }
    "for BeFirst45 return be-first-45" in {
      alias(BeFirst45) shouldBe "be-first-45"
    }
    "for OffersHistoryReports return offers-history-reports" in {
      alias(OffersHistoryReports) shouldBe "offers-history-reports"
    }
    "for Call return call" in {
      alias(Call) shouldBe "call"
    }
    "for VinHistory return vin-history" in {
      alias(VinHistory) shouldBe "vin-history"
    }
    "for TradeInRequestCarsNew return TradeInRequestCarsNew.toString" in {
      alias(TradeInRequestCarsNew) shouldBe TradeInRequestCarsNew.toString
    }
    "for QuotaPlacementCarsUsed return QuotaPlacementCarsUsed.toString" in {
      alias(QuotaPlacementCarsUsed) shouldBe QuotaPlacementCarsUsed.toString
    }
    "for QuotaPlacementCarsUsedPremium return QuotaPlacementCarsUsedPremium.toString" in {
      alias(
        QuotaPlacementCarsUsedPremium
      ) shouldBe QuotaPlacementCarsUsedPremium.toString
    }
    "for QuotaPlacementCarsNew return QuotaPlacementCarsNew.toString" in {
      alias(QuotaPlacementCarsNew) shouldBe QuotaPlacementCarsNew.toString
    }
    "for QuotaPlacementCarsNewPremium return QuotaPlacementCarsNewPremium.toString" in {
      alias(
        QuotaPlacementCarsNewPremium
      ) shouldBe QuotaPlacementCarsNewPremium.toString
    }
    "for QuotaPlacementMoto return QuotaPlacementMoto.toString" in {
      alias(QuotaPlacementMoto) shouldBe QuotaPlacementMoto.toString
    }
    "for QuotaPlacementCommercial return QuotaPlacementCommercial.toString" in {
      alias(QuotaPlacementCommercial) shouldBe QuotaPlacementCommercial.toString
    }
    "for QuotaPriority return QuotaPriority.toString" in {
      alias(QuotaPriority) shouldBe QuotaPriority.toString
    }
    "for QuotaPlacementPartsUsed return QuotaPlacementPartsUsed.toString()" in {
      alias(QuotaPlacementPartsUsed) shouldBe QuotaPlacementPartsUsed.toString
    }
  }
}
