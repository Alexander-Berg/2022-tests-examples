package ru.auto.api.model.salesman

import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiOfferModel.Category._
import ru.auto.api.ApiOfferModel.Section._
import ru.auto.api.BaseSpec
import ru.auto.api.MotoModel.MotoCategory._
import ru.auto.api.TrucksModel.TruckCategory._
import ru.auto.api.model.gen.SalesmanModelGenerators._

class CampaignSpec extends BaseSpec with ScalaCheckPropertyChecks {

  "convertCampaigns" should {

    "convert" in {
      forAll(Gen.listOf(CampaignGen).map(_.toSet)) { campaigns =>
        val apiCampaigns = convertCampaigns(campaigns)
        apiCampaigns.getCampaignsList should contain theSameElementsAs campaigns.map(convertCampaign)
      }
    }
  }

  "convertCampaign" should {

    "get cars used campaign" in {
      forAll(paymentModelGen, readableString) { (paymentModel, tag) =>
        val campaign = Campaign(paymentModel, tag, "cars", Nil, List("used"), 5, enabled = true)
        val apiCampaign = convertCampaign(campaign)
        apiCampaign.getCategory shouldBe CARS
        apiCampaign.getTruckSubcategories.getCategoriesList shouldBe empty
        apiCampaign.getMotoSubcategories.getCategoriesList shouldBe empty
        apiCampaign.getSectionList.get(0) shouldBe USED
        apiCampaign.getSize shouldBe 5
      }
    }

    "get truck trucks new campaign" in {
      forAll(paymentModelGen, readableString) { (paymentModel, tag) =>
        val campaign = Campaign(paymentModel, tag, "commercial", List("trucks"), List("new"), 5, enabled = true)
        val apiCampaign = convertCampaign(campaign)
        apiCampaign.getCategory shouldBe TRUCKS
        apiCampaign.getTruckSubcategories.getCategoriesList should contain only TRUCK
        apiCampaign.getMotoSubcategories.getCategoriesList shouldBe empty
        apiCampaign.getSectionList.get(0) shouldBe NEW
        apiCampaign.getSize shouldBe 5
      }
    }

    "get commercial swapbody new campaign" in {
      forAll(paymentModelGen, readableString) { (paymentModel, tag) =>
        val campaign = Campaign(paymentModel, tag, "commercial", List("swapbody"), List("new"), 5, enabled = true)
        val apiCampaign = convertCampaign(campaign)
        apiCampaign.getCategory shouldBe TRUCKS
        apiCampaign.getTruckSubcategories.getCategoriesList should contain only SWAP_BODY
        apiCampaign.getMotoSubcategories.getCategoriesList shouldBe empty
        apiCampaign.getSectionList.get(0) shouldBe NEW
        apiCampaign.getSize shouldBe 5
      }
    }

    "get commercial trailer new campaign" in {
      forAll(paymentModelGen, readableString) { (paymentModel, tag) =>
        val campaign = Campaign(paymentModel, tag, "commercial", List("trailer"), List("new"), 5, enabled = true)
        val apiCampaign = convertCampaign(campaign)
        apiCampaign.getCategory shouldBe TRUCKS
        apiCampaign.getTruckSubcategories.getCategoriesList should contain only TRAILER
        apiCampaign.getMotoSubcategories.getCategoriesList shouldBe empty
        apiCampaign.getSectionList.get(0) shouldBe NEW
        apiCampaign.getSize shouldBe 5
      }
    }

    "get moto snowmobile new campaign" in {
      forAll(paymentModelGen, readableString) { (paymentModel, tag) =>
        val campaign = Campaign(paymentModel, tag, "moto", List("snowmobile"), List("new"), 5, enabled = true)
        val apiCampaign = convertCampaign(campaign)
        apiCampaign.getCategory shouldBe MOTO
        apiCampaign.getTruckSubcategories.getCategoriesList shouldBe empty
        apiCampaign.getMotoSubcategories.getCategoriesList should contain only SNOWMOBILE
        apiCampaign.getSectionList.get(0) shouldBe NEW
        apiCampaign.getSize shouldBe 5
      }
    }

    "get moto amphibious new campaign" in {
      forAll(paymentModelGen, readableString) { (paymentModel, tag) =>
        val campaign = Campaign(paymentModel, tag, "moto", List("amphibious"), List("new"), 5, enabled = true)
        val apiCampaign = convertCampaign(campaign)
        apiCampaign.getCategory shouldBe MOTO
        apiCampaign.getTruckSubcategories.getCategoriesList shouldBe empty
        apiCampaign.getMotoSubcategories.getCategoriesList should contain only ATV
        apiCampaign.getSectionList.get(0) shouldBe NEW
        apiCampaign.getSize shouldBe 5
      }
    }

    "get moto baggi new campaign" in {
      forAll(paymentModelGen, readableString) { (paymentModel, tag) =>
        val campaign = Campaign(paymentModel, tag, "moto", List("baggi"), List("new"), 5, enabled = true)
        val apiCampaign = convertCampaign(campaign)
        apiCampaign.getCategory shouldBe MOTO
        apiCampaign.getTruckSubcategories.getCategoriesList shouldBe empty
        apiCampaign.getMotoSubcategories.getCategoriesList should contain only ATV
        apiCampaign.getSectionList.get(0) shouldBe NEW
        apiCampaign.getSize shouldBe 5
      }
    }

    "get moto carting new campaign" in {
      forAll(paymentModelGen, readableString) { (paymentModel, tag) =>
        val campaign = Campaign(paymentModel, tag, "moto", List("carting"), List("new"), 5, enabled = true)
        val apiCampaign = convertCampaign(campaign)
        apiCampaign.getCategory shouldBe MOTO
        apiCampaign.getTruckSubcategories.getCategoriesList shouldBe empty
        apiCampaign.getMotoSubcategories.getCategoriesList shouldBe empty
        apiCampaign.getSectionList.get(0) shouldBe NEW
        apiCampaign.getSize shouldBe 5
      }
    }
  }
}
