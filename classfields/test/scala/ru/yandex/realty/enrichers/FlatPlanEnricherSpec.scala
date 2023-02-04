package ru.yandex.realty.enrichers

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.flatplan.FlatPlanSearcherTestUtils.Plan
import ru.yandex.realty.flatplan.{
  FlatPlanGuessingBlacklist,
  FlatPlanGuessingBlacklistStorage,
  FlatPlanSearcher,
  FlatPlanSearcherTestUtils
}
import ru.yandex.realty.model.offer.{AreaInfo, AreaUnit, BuildingInfo, CategoryType, HouseInfo, Offer}
import ru.yandex.realty.model.raw.RawOfferImpl
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.unification.OfferWrapper

import scala.collection.JavaConverters.asScalaBufferConverter

@RunWith(classOf[JUnitRunner])
class FlatPlanEnricherSpec extends AsyncSpecBase with Matchers with BeforeAndAfter {

  var enricher: FlatPlanEnricher = _

  implicit val trace: Traced = Traced.empty

  private def index(plans: Plan*): Unit = index(plans, enabled = true)

  private def index(plans: Seq[Plan], enabled: Boolean): Unit = {
    enricher = new FlatPlanEnricher(
      FlatPlanSearcherTestUtils.createSearcherOverPlans(plans: _*),
      new Provider[FlatPlanGuessingBlacklist] {
        override val get: FlatPlanGuessingBlacklist = new FlatPlanGuessingBlacklistStorage(Set.empty, Set.empty)
      }
    )
  }

  private def indexWithBlacklist(offers: Set[String], feeds: Set[Long], plans: Plan*): Unit = {
    enricher = new FlatPlanEnricher(
      FlatPlanSearcherTestUtils.createSearcherOverPlans(plans: _*),
      new Provider[FlatPlanGuessingBlacklist] {
        override val get: FlatPlanGuessingBlacklist = new FlatPlanGuessingBlacklistStorage(offers, feeds)
      }
    )
  }

  private def offer(
    series: Long,
    totalArea: Float,
    livingArea: Option[Float],
    roomAreas: Seq[Float],
    kitchenArea: Option[Float]
  ): OfferWrapper = {
    val o = new Offer()
    o.setCategoryType(CategoryType.APARTMENT)
    o.setBuildingInfo(new BuildingInfo)
    o.getBuildingInfo.setUnifiedBuildingSeries(series, "")
    o.setArea(AreaInfo.create(AreaUnit.SQUARE_METER, totalArea))
    if (livingArea.isDefined) {
      o.setHouseInfo(new HouseInfo)
      livingArea.foreach((Float.box _).andThen(o.getHouseInfo.setLivingSpace))
      roomAreas.foreach((Float.box _).andThen(o.getHouseInfo.getRoomSpace.add))
      kitchenArea.foreach((Float.box _).andThen(o.getHouseInfo.setKitchenSpace))
    }
    val rawOffer = new RawOfferImpl
    rawOffer.setDisableFlatPlanGuess(false)
    new OfferWrapper(rawOffer, o, null)
  }

  val area = 200
  private val totalAreaLargeTolerance = FlatPlanSearcher.getTotalAreaTolerance(area, exactMatch = false)
  private val roomAreaLargeTolerance = FlatPlanSearcher.getRoomAreaTolerance(area, exactMatch = false)

  "FlatPlanEnricher" should {
    "find an exactly matching plan" in {
      index(
        Plan(1, None, "1", 10, 10, Seq(10), 10, "1-1"),
        Plan(2, None, "1", 20, 20, Seq(20), 20, "2-1")
      )
      val ow = offer(1, totalArea = 10, livingArea = Some(10), roomAreas = Seq(10), kitchenArea = Some(10))
      enricher.process(ow).futureValue
      ow.getOffer.getApartmentInfo shouldNot be(null)
      ow.getOffer.getApartmentInfo.getGuessedFlatPlan shouldNot be(null)
      ow.getOffer.getApartmentInfo.getGuessedFlatPlan.getVariantCount shouldBe 1
      ow.getOffer.getApartmentInfo.getGuessedFlatPlan.getVariant(0).getSvg.getOrigin shouldBe "//1-1/orig"
    }

    "not guess when variants are too similar" in {
      index(
        Plan(1, None, "0", 9, 9, Seq(9), 9, "1-1"),
        Plan(1, None, "2", 11, 11, Seq(11), 11, "1-2")
      )
      val ow = offer(1, totalArea = 10, livingArea = Some(10), roomAreas = Seq(10), kitchenArea = Some(10))
      enricher.process(ow).futureValue
      ow.getOffer.getApartmentInfo should be(null)
    }

    "return all symmetrical variants in a series" in {
      index(
        Plan(1, None, "1", 10, 10, Seq(2, 8), 10, "1-1"),
        Plan(1, None, "2", 10, 10, Seq(8, 2), 10, "1-2")
      )
      val ow = offer(1, totalArea = 10, livingArea = Some(10), roomAreas = Seq(2, 8), kitchenArea = Some(10))
      enricher.process(ow).futureValue
      ow.getOffer.getApartmentInfo shouldNot be(null)
      ow.getOffer.getApartmentInfo.getGuessedFlatPlan shouldNot be(null)
      ow.getOffer.getApartmentInfo.getGuessedFlatPlan.getVariantCount shouldBe 2
      ow.getOffer.getApartmentInfo.getGuessedFlatPlan.getVariantList.asScala.map(_.getSvg.getOrigin).toSet shouldBe
        Set("//1-1/orig", "//1-2/orig")
    }

    "differentiate by room count" in {
      index(
        Plan(1, None, "1", 10, 10, Seq(10), 10, "1-1"),
        Plan(1, None, "2", 10, 10, Seq(10, 10), 10, "1-2")
      )
      val ow = offer(1, totalArea = 10, livingArea = Some(10), roomAreas = Seq(10), kitchenArea = Some(10))
      enricher.process(ow).futureValue
      ow.getOffer.getApartmentInfo shouldNot be(null)
      ow.getOffer.getApartmentInfo.getGuessedFlatPlan shouldNot be(null)
      ow.getOffer.getApartmentInfo.getGuessedFlatPlan.getVariantCount shouldBe 1
      ow.getOffer.getApartmentInfo.getGuessedFlatPlan.getVariant(0).getSvg.getOrigin shouldBe "//1-1/orig"
    }

    "search child series if no variants are found in the specified series" in {
      index(
        Plan(2, Some(1), "1", 10, 10, Seq(10), 10, "2-1")
      )
      val ow = offer(1, totalArea = 10, livingArea = Some(10), roomAreas = Seq(10), kitchenArea = Some(10))
      enricher.process(ow).futureValue
      ow.getOffer.getApartmentInfo shouldNot be(null)
      ow.getOffer.getApartmentInfo.getGuessedFlatPlan shouldNot be(null)
      ow.getOffer.getApartmentInfo.getGuessedFlatPlan.getVariantCount shouldBe 1
      ow.getOffer.getApartmentInfo.getGuessedFlatPlan.getVariant(0).getSvg.getOrigin shouldBe "//2-1/orig"
    }

    "guess exact match from two variants" in {
      index(
        Plan(1, None, "1", 10, 10, Seq(10), 10, "2-1"),
        Plan(1, None, "2", 11, 11, Seq(11), 11, "2-2")
      )
      val ow = offer(1, totalArea = 10, livingArea = Some(10), roomAreas = Seq(10), kitchenArea = Some(10))
      enricher.process(ow).futureValue
      ow.getOffer.getApartmentInfo.getGuessedFlatPlan.getVariant(0).getSvg.getOrigin shouldBe "//2-1/orig"
    }
    "not enrich blacklisted offers" in {
      indexWithBlacklist(
        Set("10"),
        Set(20),
        Plan(1, None, "1", 10, 10, Seq(10), 10, "1-1")
      )
      val ow = offer(1, totalArea = 10, livingArea = Some(10), roomAreas = Seq(10), kitchenArea = Some(10))
      ow.getOffer.setId(10)
      enricher.process(ow).futureValue

      ow.getOffer.getApartmentInfo should be(null)
    }

    "not enrich blacklisted feeds" in {
      indexWithBlacklist(
        Set("10"),
        Set(20),
        Plan(1, None, "1", 10, 10, Seq(10), 10, "1-1")
      )
      val ow = offer(1, totalArea = 10, livingArea = Some(10), roomAreas = Seq(10), kitchenArea = Some(10))
      ow.getOffer.setPartnerId(20L)
      enricher.process(ow).futureValue

      ow.getOffer.getApartmentInfo should be(null)
    }

    "not guess plans on upper area tolerance bound" in {
      val totalAreaUpperBound = area + totalAreaLargeTolerance
      val roomAreaUpperBound = area + roomAreaLargeTolerance

      index(
        Plan(1, None, "1", totalAreaUpperBound, roomAreaUpperBound, Seq(roomAreaUpperBound), roomAreaUpperBound, "1-1")
      )

      val ow = offer(1, area, Some(area), Seq(area), Some(area))
      enricher.process(ow).futureValue

      ow.getOffer.getApartmentInfo should be(null)
    }

    "not guess plans on lower area tolerance bound" in {
      val totalAreaLowerBound = area - totalAreaLargeTolerance
      val roomAreaLowerBound = area - roomAreaLargeTolerance

      index(
        Plan(1, None, "2", totalAreaLowerBound, roomAreaLowerBound, Seq(roomAreaLowerBound), roomAreaLowerBound, "1-3")
      )

      val ow = offer(1, area, Some(area), Seq(area), Some(area))
      enricher.process(ow).futureValue

      ow.getOffer.getApartmentInfo should be(null)
    }

    "guess plans near lower area tolerance bound" in {
      val totalAreaLowerBound = area - totalAreaLargeTolerance + 1
      val roomAreaLowerBound = area - roomAreaLargeTolerance + 1

      index(
        Plan(1, None, "2", totalAreaLowerBound, roomAreaLowerBound, Seq(roomAreaLowerBound), roomAreaLowerBound, "1-1")
      )

      val ow = offer(1, area, Some(area), Seq(area), Some(area))
      enricher.process(ow).futureValue

      ow.getOffer.getApartmentInfo.getGuessedFlatPlan.getVariant(0).getSvg.getOrigin shouldBe "//1-1/orig"
    }

    "guess plans near upper area tolerance bound" in {
      val totalAreaUpperBound = area + totalAreaLargeTolerance - 1
      val roomAreaUpperBound = area + roomAreaLargeTolerance - 1

      index(
        Plan(1, None, "1", totalAreaUpperBound, roomAreaUpperBound, Seq(roomAreaUpperBound), roomAreaUpperBound, "1-2")
      )

      val ow = offer(1, area, Some(area), Seq(area), Some(area))
      enricher.process(ow).futureValue

      ow.getOffer.getApartmentInfo.getGuessedFlatPlan.getVariant(0).getSvg.getOrigin shouldBe "//1-2/orig"
    }
  }

}
