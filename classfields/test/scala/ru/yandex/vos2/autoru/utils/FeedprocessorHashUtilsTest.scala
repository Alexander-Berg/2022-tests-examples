package ru.yandex.vos2.autoru.utils

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import ru.auto.api.ApiOfferModel
import ru.auto.api.ApiOfferModel.{Category, Offer, Section}
import ru.yandex.vos2.autoru.model.TestUtils
import ru.yandex.vos2.model.UserRefAutoruClient

/**
  * @author pnaydenov
  */
@RunWith(classOf[JUnitRunner])
class FeedprocessorHashUtilsTest extends AnyFunSuite {
  test("unified VINs should give equal hash") {
    val vin1 = "Xw0ZzZ4L1EG001019"
    val vin2 = "xW0zZz4l1eg001019"

    val offerBuilder = TestUtils.createOffer()
    offerBuilder.getOfferAutoruBuilder.getDocumentsBuilder.setVin(vin1)
    val offer1 = offerBuilder.build()
    offerBuilder.getOfferAutoruBuilder.getDocumentsBuilder.setVin(vin2)
    val offer2 = offerBuilder.build()
    assert(
      FeedprocessorHashUtils.getFeedprocessorHash(offer1)
        == FeedprocessorHashUtils.getFeedprocessorHash(offer2)
    )

    val userRef = UserRefAutoruClient(1)
    val category = Category.CARS
    val formBuilder = Offer.newBuilder().setCategory(ApiOfferModel.Category.CARS)
    formBuilder.getDocumentsBuilder.setVin(vin1)
    val form1 = formBuilder.build()
    formBuilder.getDocumentsBuilder.setVin(vin2)
    val form2 = formBuilder.build()
    assert(
      FeedprocessorHashUtils.getFeedprocessorHash(userRef, category, form1) ==
        FeedprocessorHashUtils.getFeedprocessorHash(userRef, category, form2)
    )
  }

  test("VINs different by OQI -> 001 should have different hash") {
    val vin1a = "IIAAAAAAAAAAAAAAA"
    val vin1b = "11AAAAAAAAAAAAAAA"
    val vin2a = "OOAAAAAAAAAAAAAAA"
    val vin2b = "QQAAAAAAAAAAAAAAA"
    val vin2c = "00AAAAAAAAAAAAAAA"

    val userRef = UserRefAutoruClient(1)
    val category = Category.CARS
    val formBuilder = Offer.newBuilder().setCategory(ApiOfferModel.Category.CARS)

    def hash(vin: String): String = {
      formBuilder.getDocumentsBuilder.setVin(vin)
      val form = formBuilder.build()
      FeedprocessorHashUtils.getFeedprocessorHash(userRef, category, form)
    }

    assert(hash(vin1a) != hash(vin1b))
    assert(hash(vin2a) != hash(vin2b))
    assert(hash(vin2b) != hash(vin2c))
    assert(hash(vin2a) != hash(vin2c))
  }

  test("offers with different section should have different hash") {
    val userRef = UserRefAutoruClient(1)
    val category = Category.CARS
    val formBuilder = Offer.newBuilder().setCategory(ApiOfferModel.Category.CARS)

    formBuilder.setSection(Section.USED)
    val form1 = formBuilder.build()
    formBuilder.setSection(Section.NEW)
    val form2 = formBuilder.build()
    assert(
      FeedprocessorHashUtils.getFeedprocessorHash(userRef, category, form1)
        != FeedprocessorHashUtils.getFeedprocessorHash(userRef, category, form2)
    )
  }

  test("cars not depends on color") {
    val userRef = UserRefAutoruClient(1)
    val category = Category.CARS
    val formBuilder = Offer.newBuilder().setCategory(ApiOfferModel.Category.CARS)

    formBuilder.setColorHex("C49648")
    val form1 = formBuilder.build()
    formBuilder.setColorHex("007F00")
    val form2 = formBuilder.build()

    assert(
      FeedprocessorHashUtils.getFeedprocessorHash(userRef, category, form1)
        == FeedprocessorHashUtils.getFeedprocessorHash(userRef, category, form2)
    )
  }

  test("trucks not depends on color") {
    val userRef = UserRefAutoruClient(1)
    val category = Category.TRUCKS
    val formBuilder = Offer.newBuilder().setCategory(ApiOfferModel.Category.TRUCKS)

    formBuilder.setColorHex("C49648")
    val form1 = formBuilder.build()
    formBuilder.setColorHex("007F00")
    val form2 = formBuilder.build()

    assert(
      FeedprocessorHashUtils.getFeedprocessorHash(userRef, category, form1)
        == FeedprocessorHashUtils.getFeedprocessorHash(userRef, category, form2)
    )
  }

  test("not depends on tech_param_id") {
    val userRef = UserRefAutoruClient(1)
    val category = Category.CARS
    val formBuilder = Offer.newBuilder().setCategory(ApiOfferModel.Category.CARS)

    formBuilder.getCarInfoBuilder.setTechParamId(111)
    val form1 = formBuilder.build()
    formBuilder.getCarInfoBuilder.setTechParamId(222)
    val form2 = formBuilder.build()

    assert(
      FeedprocessorHashUtils.getFeedprocessorHash(userRef, category, form1)
        == FeedprocessorHashUtils.getFeedprocessorHash(userRef, category, form2)
    )
  }

  test("not depends on mark") {
    val userRef = UserRefAutoruClient(1)
    val category = Category.CARS
    val formBuilder = Offer.newBuilder().setCategory(ApiOfferModel.Category.CARS)

    formBuilder.getCarInfoBuilder.setMark("FOO")
    val form1 = formBuilder.build()
    formBuilder.getCarInfoBuilder.setMark("BAR")
    val form2 = formBuilder.build()

    assert(
      FeedprocessorHashUtils.getFeedprocessorHash(userRef, category, form1)
        == FeedprocessorHashUtils.getFeedprocessorHash(userRef, category, form2)
    )
  }

  test("not depends on model") {
    val userRef = UserRefAutoruClient(1)
    val category = Category.CARS
    val formBuilder = Offer.newBuilder().setCategory(ApiOfferModel.Category.CARS)

    formBuilder.getCarInfoBuilder.setModel("FOO")
    val form1 = formBuilder.build()
    formBuilder.getCarInfoBuilder.setModel("BAR")
    val form2 = formBuilder.build()

    assert(
      FeedprocessorHashUtils.getFeedprocessorHash(userRef, category, form1)
        == FeedprocessorHashUtils.getFeedprocessorHash(userRef, category, form2)
    )
  }

  test("not depends on year") {
    val userRef = UserRefAutoruClient(1)
    val category = Category.CARS
    val formBuilder = Offer.newBuilder().setCategory(ApiOfferModel.Category.CARS)

    formBuilder.getDocumentsBuilder.setYear(2001)
    val form1 = formBuilder.build()
    formBuilder.getDocumentsBuilder.setYear(2002)
    val form2 = formBuilder.build()

    assert(
      FeedprocessorHashUtils.getFeedprocessorHash(userRef, category, form1)
        == FeedprocessorHashUtils.getFeedprocessorHash(userRef, category, form2)
    )
  }
}
