package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import org.scalatest.BeforeAndAfterAll
import ru.auto.api.ApiOfferModel.Category
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.AutoruModel.AutoruOffer.TruckInfo.{BodyType, BusType, SpecialType, TruckCategory}
import ru.yandex.vos2.OfferModel.Offer
import ru.yandex.vos2.autoru.model.TestUtils
import org.junit.runner.RunWith
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class CommercialReplaceTypeWorkerYdbTest extends AnyWordSpec with MockitoSupport with Matchers with BeforeAndAfterAll {
  implicit val traced: Traced = Traced.empty

  def createTruckOffer(f: (Offer.Builder) => Unit = _ => ()): Offer = {

    val builder = TestUtils.createOffer(category = Category.TRUCKS)
    f(builder)
    builder.build()
  }

  abstract private class Fixture {

    val worker = new CommercialReplaceTypeWorkerYdb(
      ) with YdbWorkerTestImpl
  }

  ("shouldProcess") in new Fixture {

    val offerCars = TestUtils.createOffer(category = Category.CARS).build()

    val resultCars = worker.shouldProcess(offerCars, None).shouldProcess

    assert(!resultCars)

    val offerBus = TestUtils.createOffer(category = Category.TRUCKS)
    offerBus.getOfferAutoruBuilder.getTruckInfoBuilder
      .setAutoCategory(TruckCategory.TRUCK_CAT_BUS)

    val resultBus = worker.shouldProcess(offerBus.build(), None).shouldProcess

    assert(!resultBus)

    val offerTruck = TestUtils.createOffer(category = Category.TRUCKS)
    offerTruck.getOfferAutoruBuilder.getTruckInfoBuilder
      .setAutoCategory(TruckCategory.TRUCK_CAT_TRUCK)

    val resultTruck = worker.shouldProcess(offerTruck.build(), None).shouldProcess

    assert(resultTruck)

    val offerConstruction = TestUtils.createOffer(category = Category.TRUCKS)
    offerConstruction.getOfferAutoruBuilder.getTruckInfoBuilder
      .setAutoCategory(TruckCategory.TRUCK_CAT_CONSTRUCTION)

    val resultConstruction = worker.shouldProcess(offerConstruction.build(), None).shouldProcess

    assert(resultConstruction)

    val offerMunicipal = TestUtils.createOffer(category = Category.TRUCKS)
    offerMunicipal.getOfferAutoruBuilder.getTruckInfoBuilder
      .setAutoCategory(TruckCategory.TRUCK_CAT_MUNICIPAL)

    val resultMunicipal = worker.shouldProcess(offerMunicipal.build(), None).shouldProcess

    assert(resultMunicipal)
  }

  ("process THERMOBODY") in new Fixture {

    val offer = createTruckOffer { builder =>
      builder.getOfferAutoruBuilder.getTruckInfoBuilder.setAutoCategory(TruckCategory.TRUCK_CAT_TRUCK)
      builder.getOfferAutoruBuilder.getTruckInfoBuilder.setBodyType(BodyType.THERMOBODY)
    }

    val result = worker.process(offer, None).updateOfferFunc.get(offer)

    assert(result.getOfferAutoru.getTruckInfo.getBodyType == BodyType.ISOTHERMAL_BODY)
  }

  ("process SPECIAL_TYPE_MIXER") in new Fixture {

    val offer = createTruckOffer { builder =>
      builder.getOfferAutoruBuilder.getTruckInfoBuilder.setAutoCategory(TruckCategory.TRUCK_CAT_CONSTRUCTION)
      builder.getOfferAutoruBuilder.getTruckInfoBuilder.setSpecialTypeKey(SpecialType.SPECIAL_TYPE_MIXER)
    }

    val result = worker.process(offer, None).updateOfferFunc.get(offer)

    assert(result.getOfferAutoru.getTruckInfo.getSpecialTypeKey == SpecialType.SPECIAL_TYPE_MIXER_TRUCK)
  }

  ("process SPECIAL_TYPE_OOZE_CLEANING_MACHINE") in new Fixture {

    val offer = createTruckOffer { builder =>
      builder.getOfferAutoruBuilder.getTruckInfoBuilder.setAutoCategory(TruckCategory.TRUCK_CAT_MUNICIPAL)
      builder.getOfferAutoruBuilder.getTruckInfoBuilder
        .setSpecialTypeKey(SpecialType.SPECIAL_TYPE_OOZE_CLEANING_MACHINE)
    }

    val result = worker.process(offer, None).updateOfferFunc.get(offer)

    assert(result.getOfferAutoru.getTruckInfo.getSpecialTypeKey == SpecialType.SPECIAL_TYPE_VACUUM_MACHINE)
  }

  ("process SPECIAL_TYPE_PRESSER_GARBAGE") in new Fixture {

    val offer = createTruckOffer { builder =>
      builder.getOfferAutoruBuilder.getTruckInfoBuilder.setAutoCategory(TruckCategory.TRUCK_CAT_MUNICIPAL)
      builder.getOfferAutoruBuilder.getTruckInfoBuilder.setSpecialTypeKey(SpecialType.SPECIAL_TYPE_PRESSER_GARBAGE)
    }

    val result = worker.process(offer, None).updateOfferFunc.get(offer)

    assert(result.getOfferAutoru.getTruckInfo.getSpecialTypeKey == SpecialType.SPECIAL_TYPE_GARBAGE_TRUCK)
  }

  ("process other") in new Fixture {

    val offer = createTruckOffer { builder =>
      builder.getOfferAutoruBuilder.getTruckInfoBuilder.setAutoCategory(TruckCategory.TRUCK_CAT_BUS)
      builder.getOfferAutoruBuilder.getTruckInfoBuilder.setBusType(BusType.TRUCK_BUS_SCHOOL)
    }

    val result = worker.process(offer, None).updateOfferFunc.get(offer)

    assert(result.getOfferAutoru.getTruckInfo.getBusType == BusType.TRUCK_BUS_SCHOOL)
  }
}
