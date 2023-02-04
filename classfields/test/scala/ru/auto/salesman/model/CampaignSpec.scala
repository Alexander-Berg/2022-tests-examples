package ru.auto.salesman.model

import ru.auto.api.ApiOfferModel.Category._
import ru.auto.api.ApiOfferModel.Section._
import ru.auto.salesman.model.OfferCategories._
import ru.auto.salesman.model.AdsRequestTypes.{CarsUsed, Commercial}
import ru.auto.salesman.model.payment_model.PlacementPaymentModel.Single
import ru.auto.salesman.test.BaseSpec

class CampaignSpec extends BaseSpec {

  "Campaign.single()" should {

    "convert cars:used to campaign" in {
      val campaign = Campaign.single(CarsUsed, enabled = true)
      campaign.paymentModel shouldBe Single(CarsUsed)
      campaign.tag shouldBe "cars:used"
      campaign.category shouldBe CARS
      campaign.subcategories shouldBe empty
      campaign.section shouldBe Set(USED)
      campaign.size shouldBe Int.MaxValue
      campaign.enabled shouldBe true
    }

    "convert commercial to campaign" in {
      val campaign = Campaign.single(Commercial, enabled = true)
      campaign.paymentModel shouldBe Single(Commercial)
      campaign.tag shouldBe "commercial"
      campaign.category shouldBe TRUCKS
      campaign.subcategories shouldBe Set(
        Trailer,
        Lcv,
        Trucks,
        Artic,
        Bus,
        Swapbody,
        Agricultural,
        Construction,
        Autoloader,
        Crane,
        Dredge,
        Bulldozer,
        CraneHydraulics,
        Municipal
      )
      campaign.section shouldBe Set(NEW, USED)
      campaign.size shouldBe Int.MaxValue
      campaign.enabled shouldBe true
    }

    "convert cars:used to disabled campaign" in {
      val campaign = Campaign.single(CarsUsed, enabled = false)
      campaign.paymentModel shouldBe Single(CarsUsed)
      campaign.tag shouldBe "cars:used"
      campaign.category shouldBe CARS
      campaign.subcategories shouldBe empty
      campaign.section shouldBe Set(USED)
      campaign.size shouldBe Int.MaxValue
      campaign.enabled shouldBe false
    }
  }
}
