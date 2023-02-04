package ru.auto.cabinet.model.moisha

import org.scalatest.flatspec.{AnyFlatSpec => FlatSpec}
import org.scalatest.matchers.should.Matchers
import ru.auto.api.ApiOfferModel.{Offer, Category => OfferCategory}
import ru.auto.api.MotoModel.MotoCategory
import ru.auto.api.TrucksModel.TruckCategory
import ru.auto.cabinet.model.offer.buildOffer

class OfferTransportExtractorSpec extends FlatSpec with Matchers {

  // just alias
  val extractor: TransportExtractor[Offer] = offerTransportExtractor

  "Offer transport extractor" should "extract car" in {
    val offer =
      Offer.newBuilder().setCategory(OfferCategory.CARS).build()
    extractor(offer) shouldBe Some(Transports.Cars)
  }

  it should "extract moto" in {
    val offer = buildOffer {
      _.setCategory(OfferCategory.MOTO).getMotoInfoBuilder
        .setMotoCategory(MotoCategory.MOTO_CATEGORY_UNKNOWN)
    }
    extractor(offer) shouldBe Some(Transports.Moto)
  }

  it should "extract motorcycle" in {
    val offer = buildOffer {
      _.setCategory(OfferCategory.MOTO).getMotoInfoBuilder
        .setMotoCategory(MotoCategory.MOTORCYCLE)
    }
    extractor(offer) shouldBe Some(Transports.Motorcycle)
  }

  it should "extract truck" in {
    val offer = buildOffer {
      _.setCategory(OfferCategory.TRUCKS).getTruckInfoBuilder
        .setTruckCategory(TruckCategory.TRUCK_CATEGORY_UNKNOWN)
    }
    extractor(offer) shouldBe Some(Transports.Truck)
  }

  it should "extract bus" in {
    val offer = buildOffer {
      _.setCategory(OfferCategory.TRUCKS).getTruckInfoBuilder
        .setTruckCategory(TruckCategory.BUS)
    }
    extractor(offer) shouldBe Some(Transports.Bus)
  }

  it should "not extract category" in {
    val offer = buildOffer {
      _.setCategory(OfferCategory.CATEGORY_UNKNOWN)
    }
    extractor(offer) shouldBe None
  }
}
