package ru.auto.salesman.model

import org.scalacheck.Gen
import ru.auto.api.ApiOfferModel.Category._
import ru.auto.api.ApiOfferModel.Section._
import ru.auto.salesman.model.OfferCategories._
import ru.auto.salesman.model.ProductId._
import ru.auto.salesman.test.BaseSpec
import ru.yandex.vertis.generators.BasicGenerators.bool

class QuotaTypesSpec extends BaseSpec {

  "QuotaType.toCampaign()" should {

    "create cars:used campaign" in {
      forAll(Gen.posNum[Int], bool) { (quotaSize, enabled) =>
        val campaign = QuotaPlacementCarsUsed.toCampaign(quotaSize, enabled)
        campaign.tag shouldBe "quota:placement:cars:used"
        campaign.category shouldBe CARS
        campaign.subcategories shouldBe Set()
        campaign.section shouldBe Set(USED)
        campaign.size shouldBe quotaSize
        campaign.enabled shouldBe enabled
      }
    }

    "create cars:used:premium campaign" in {
      forAll(Gen.posNum[Int], bool) { (quotaSize, enabled) =>
        val campaign =
          QuotaPlacementCarsUsedPremium.toCampaign(quotaSize, enabled)
        campaign.tag shouldBe "quota:placement:cars:used:premium"
        campaign.category shouldBe CARS
        campaign.subcategories shouldBe Set()
        campaign.section shouldBe Set(USED)
        campaign.size shouldBe quotaSize
        campaign.enabled shouldBe enabled
      }
    }

    "create cars:new campaign" in {
      forAll(Gen.posNum[Int], bool) { (quotaSize, enabled) =>
        val campaign = QuotaPlacementCarsNew.toCampaign(quotaSize, enabled)
        campaign.tag shouldBe "quota:placement:cars:new"
        campaign.category shouldBe CARS
        campaign.subcategories shouldBe Set()
        campaign.section shouldBe Set(NEW)
        campaign.size shouldBe quotaSize
        campaign.enabled shouldBe enabled
      }
    }

    "create cars:new:premium campaign" in {
      forAll(Gen.posNum[Int], bool) { (quotaSize, enabled) =>
        val campaign =
          QuotaPlacementCarsNewPremium.toCampaign(quotaSize, enabled)
        campaign.tag shouldBe "quota:placement:cars:new:premium"
        campaign.category shouldBe CARS
        campaign.subcategories shouldBe Set()
        campaign.section shouldBe Set(NEW)
        campaign.size shouldBe quotaSize
        campaign.enabled shouldBe enabled
      }
    }

    "create moto campaign" in {
      forAll(Gen.posNum[Int], bool) { (quotaSize, enabled) =>
        val campaign = QuotaPlacementMoto.toCampaign(quotaSize, enabled)
        campaign.tag shouldBe "quota:placement:moto"
        campaign.category shouldBe MOTO
        campaign.subcategories shouldBe Set(
          Motorcycle,
          Atv,
          Snowmobile,
          Carting,
          Amphibious,
          Baggi,
          Scooters
        )
        campaign.section shouldBe Set(NEW, USED)
        campaign.size shouldBe quotaSize
        campaign.enabled shouldBe enabled
      }
    }

    "create commercial campaign" in {
      forAll(Gen.posNum[Int], bool) { (quotaSize, enabled) =>
        val campaign = QuotaPlacementCommercial.toCampaign(quotaSize, enabled)
        campaign.tag shouldBe "quota:placement:commercial"
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
        campaign.size shouldBe quotaSize
        campaign.enabled shouldBe enabled
      }
    }
  }
}
