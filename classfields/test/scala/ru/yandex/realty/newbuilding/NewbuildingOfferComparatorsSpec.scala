package ru.yandex.realty.newbuilding

import java.lang.{Float => JFloat}
import java.util.Comparator

import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.SpecBase
import ru.yandex.realty.model.offer.Rooms
import ru.yandex.realty.newbuilding.gen.NewbuildingOfferGenerators
import ru.yandex.realty.util.Mappings.MapAny

@RunWith(classOf[JUnitRunner])
class NewbuildingOfferComparatorsSpec extends SpecBase with PropertyChecks with NewbuildingOfferGenerators {

  val newbuildingOfferGen: Gen[NewbuildingOffer] = for {
    id <- Gen.choose[Long](0L, Long.MaxValue)
    siteId <- Gen.option(Gen.choose[Long](0L, Long.MaxValue).map(Long.box))
    phaseId <- Gen.option(Gen.choose[Long](0L, Long.MaxValue).map(Long.box))
    houseId <- Gen.option(Gen.choose[Long](0L, Long.MaxValue).map(Long.box))
    partnerId <- Gen.option(Gen.choose[Long](0L, Long.MaxValue).map(Long.box))
    commissioningDate <- commissioningDateGen
    rooms <- roomsOptionGen
    areaValue: JFloat <- areaValueGen
    area <- Gen.option(areaValue)
    kitchenSpaceValue <- kitchenSpaceGen(areaValue)
    kitchenSpace <- Gen.option(kitchenSpaceValue)
    livingSpace <- livingSpaceOptionGen(area, kitchenSpace)
    price <- priceGen
    priceSqM = priceSqMGen(area, price)
    isFinished = isFinishedGen(commissioningDate)
    bathroomUnit <- bathroomUnitOptionGen
    balcony <- balconyOptionGen
    floorsTotal <- floorsTotalOptionGen
    floor <- floorOptionGen(floorsTotal)
    dealStatus <- dealStatusOptionGen
    internal <- internalGen
    createTime <- Gen.choose(0L, Long.MaxValue)
    updateTimeMillis <- Gen.option(Gen.choose(0L, Long.MaxValue).map(Long.box))
    fromVos <- Gen.oneOf(true, false)
    relevance <- Gen.option(Gen.choose(0f, Float.MaxValue).map(Float.box))
    apartments <- apartmentsGen
    roomSpace <- roomSpaceGen
    ceilingHeight <- ceilingHeightGen
    regionGraphId <- Gen.choose[Long](0L, Long.MaxValue)
    subjectFederationId <- Gen.choose(0, Int.MaxValue)
    renovation <- renovationGen
    decoration <- decorationGen
    flatType <- flatTypeGen
  } yield new NewbuildingOffer.Builder(id)
    .applySideEffect(_.primarySaleV2 = false /* extinguish some of internal checks irrelevant for the test */ )
    .applySideEffect(_.siteId = siteId.orNull)
    .applySideEffect(_.phaseId = phaseId.orNull)
    .applySideEffect(_.houseId = houseId.orNull)
    .applySideEffect(_.partnerId = partnerId.orNull)
    .applySideEffect(_.commissioningDate = commissioningDate.orNull)
    .applySideEffect(_.complete = isFinished.orNull)
    .applySideEffect(_.studio = rooms.map(_ == Rooms.STUDIO.getMinRooms).map(Boolean.box).orNull)
    .applySideEffect(_.openPlan = rooms.map(_ == Rooms.OPEN_PLAN.getMinRooms).map(Boolean.box).orNull)
    .applySideEffect(_.rooms = rooms.orNull)
    .applySideEffect(_.area = area.orNull)
    .applySideEffect(_.kitchenSpace = kitchenSpace.orNull)
    .applySideEffect(_.livingSpace = livingSpace.orNull)
    .applySideEffect(_.price = price.orNull)
    .applySideEffect(_.priceSqM = priceSqM.orNull)
    .applySideEffect(_.bathroomUnit = bathroomUnit.orNull)
    .applySideEffect(_.balcony = balcony.orNull)
    .applySideEffect(_.floor = floor.orNull)
    .applySideEffect(_.floorsTotal = floorsTotal.orNull)
    .applySideEffect(_.dealStatus = dealStatus.orNull)
    .applySideEffect(_.internal = internal.orNull)
    .applySideEffect(_.createTime = createTime)
    .applySideEffect(_.updateTimeMillis = updateTimeMillis.orNull)
    .applySideEffect(_.fromVos = fromVos)
    .applySideEffect(_.relevance = relevance.orNull)
    .applySideEffect(_.apartments = apartments.orNull)
    .applySideEffect(_.roomSpace = roomSpace.orNull)
    .applySideEffect(_.renovation = renovation.orNull)
    .applySideEffect(_.decoration = decoration.orNull)
    .applySideEffect(_.flatType = flatType.orNull)
    .applySideEffect(_.ceilingHeight = ceilingHeight.orNull)
    .applySideEffect(_.regionGraphId = regionGraphId)
    .applySideEffect(_.subjectFederationId = subjectFederationId)
    .build()

  val listOfNewbuildingOffersGen: Gen[Seq[NewbuildingOffer]] = Gen.listOf(newbuildingOfferGen)

  private def coverDualOrdering[T](
    comparatorAsc: Comparator[NewbuildingOffer],
    comparatorDesc: Comparator[NewbuildingOffer],
    accessor: NewbuildingOffer => T,
    listOfNewbuildingOffers: Seq[NewbuildingOffer]
  ): Unit = {
    val asc: Ordering[NewbuildingOffer] = Ordering.comparatorToOrdering(comparatorAsc)
    val desc: Ordering[NewbuildingOffer] = Ordering.comparatorToOrdering(comparatorDesc)

    val ascOrdered = listOfNewbuildingOffers.sorted(asc)
    val descOrdered = listOfNewbuildingOffers.sorted(desc)

    ascOrdered.size shouldBe listOfNewbuildingOffers.size
    ascOrdered.toSet shouldBe listOfNewbuildingOffers.toSet

    descOrdered.size shouldBe listOfNewbuildingOffers.size
    descOrdered.toSet shouldBe listOfNewbuildingOffers.toSet

    if (ascOrdered.size >= 2) {
      val comparisonResults =
        ascOrdered
          .zip(ascOrdered.tail)
          .map { case (a, b) => comparatorAsc.compare(a, b) }
          .toSet

      comparisonResults should (contain(0) or contain(-1))
      comparisonResults shouldNot contain(1)
    }

    if (descOrdered.size >= 2) {
      val comparisonResults =
        descOrdered
          .zip(descOrdered.tail)
          .map { case (a, b) => comparatorDesc.compare(a, b) }
          .toSet

      comparisonResults should (contain(0) or contain(-1))
      comparisonResults shouldNot contain(1)
    }
  }

  def dummyOffer(floor: Integer, id: Long): NewbuildingOffer = {
    val builder = new NewbuildingOffer.Builder(id)
    builder.floor = floor
    builder.primarySaleV2 = false; // extinguish some of its internal checks (not relevant for the test)
    builder.build()
  }

  "NewbuildingOfferComparators" should {
    "provide appropriate FLOOR comparator" in {
      val testData =
        Table(
          ("offer1", "offer2", "expected"),
          (dummyOffer(null, 0L), dummyOffer(null, 1L), 0),
          (dummyOffer(1, 0L), dummyOffer(null, 1L), 1),
          (dummyOffer(null, 0L), dummyOffer(1, 1L), -1),
          (dummyOffer(1, 0L), dummyOffer(1, 1L), 0),
          (dummyOffer(2, 0L), dummyOffer(1, 1L), 1),
          (dummyOffer(1, 0L), dummyOffer(2, 1L), -1)
        )

      forAll(testData) { (offer1: NewbuildingOffer, offer2: NewbuildingOffer, expected: Int) =>
        val res = NewbuildingOfferComparators.FLOOR.compare(offer1, offer2)
        res shouldBe expected
      }

    }

    "handle gracefully PRICE/PRICE_DESC orderings" in {
      forAll(listOfNewbuildingOffersGen) { listOfNewbuildingOffers =>
        coverDualOrdering(
          NewbuildingOfferComparators.PRICE,
          NewbuildingOfferComparators.PRICE_DESC,
          _.getPrice,
          listOfNewbuildingOffers
        )
      }
    }

    "handle gracefully PRICE_PER_SQR/PRICE_PER_SQR_DESC orderings" in {
      forAll(listOfNewbuildingOffersGen) { listOfNewbuildingOffers =>
        coverDualOrdering(
          NewbuildingOfferComparators.PRICE_PER_SQR,
          NewbuildingOfferComparators.PRICE_PER_SQR_DESC,
          _.getPriceSqM,
          listOfNewbuildingOffers
        )
      }
    }

    "handle gracefully AREA/AREA_DESC orderings" in {
      forAll(listOfNewbuildingOffersGen) { listOfNewbuildingOffers =>
        coverDualOrdering(
          NewbuildingOfferComparators.AREA,
          NewbuildingOfferComparators.AREA_DESC,
          _.getArea,
          listOfNewbuildingOffers
        )
      }
    }

    "handle gracefully KITCHEN_SPACE/KITCHEN_SPACE_DESC orderings" in {
      forAll(listOfNewbuildingOffersGen) { listOfNewbuildingOffers =>
        coverDualOrdering(
          NewbuildingOfferComparators.KITCHEN_SPACE,
          NewbuildingOfferComparators.KITCHEN_SPACE_DESC,
          _.getKitchenSpace,
          listOfNewbuildingOffers
        )
      }
    }

    "handle gracefully COMMISSIONING_DATE/COMMISSIONING_DATE_DESC orderings" in {
      forAll(listOfNewbuildingOffersGen) { listOfNewbuildingOffers =>
        coverDualOrdering(
          NewbuildingOfferComparators.COMMISSIONING_DATE,
          NewbuildingOfferComparators.COMMISSIONING_DATE_DESC,
          _.getCommissioningDate,
          listOfNewbuildingOffers
        )
      }
    }

    "handle gracefully FLOOR/FLOOR_DESC orderings" in {
      forAll(listOfNewbuildingOffersGen) { listOfNewbuildingOffers =>
        coverDualOrdering(
          NewbuildingOfferComparators.FLOOR,
          NewbuildingOfferComparators.FLOOR_DESC,
          _.getFloor,
          listOfNewbuildingOffers
        )
      }
    }

  }

}
