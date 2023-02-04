package ru.auto.salesman.model

import ru.auto.api.ApiOfferModel.{Category, Section}
import ru.auto.salesman.test.BaseSpec

class AdsRequestTypesSpec extends BaseSpec {

  "Ads request types" should {

    "be converted to string properly" in {
      AdsRequestTypes.CarsUsed.toString shouldBe "cars:used"
      AdsRequestTypes.Commercial.toString shouldBe "commercial"
    }

    "be created by apply()" in {
      AdsRequestTypes(Category.CARS, Section.NEW) shouldBe None
      AdsRequestTypes(
        Category.CARS,
        Section.USED
      ).value shouldBe AdsRequestTypes.CarsUsed
      AdsRequestTypes(
        Category.TRUCKS,
        Section.NEW
      ).value shouldBe AdsRequestTypes.Commercial
      AdsRequestTypes(
        Category.TRUCKS,
        Section.USED
      ).value shouldBe AdsRequestTypes.Commercial
      AdsRequestTypes(Category.MOTO, Section.NEW) shouldBe empty
      AdsRequestTypes(Category.MOTO, Section.USED) shouldBe empty
    }
  }
}
