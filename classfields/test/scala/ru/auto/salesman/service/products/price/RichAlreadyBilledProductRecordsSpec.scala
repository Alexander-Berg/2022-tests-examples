package ru.auto.salesman.service.products.price

import org.joda.time.DateTime
import org.scalacheck.Gen
import ru.auto.salesman.dao.GoodsDao.Record
import ru.auto.salesman.model.FirstActivateDate
import ru.auto.salesman.model.ProductId._
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.dao.gens.goodRecordGen

class RichAlreadyBilledProductRecordsSpec extends BaseSpec {

  "RichAlreadyBilledProductRecords.until" should {

    "return None on empty bill history" in {
      val billed = Iterable.empty[Record]
      billed.until shouldBe None
    }

    "return deadline of one placement" in {
      forAll(goodRecordGen(productGen = Placement)) { placement =>
        val billed = Iterable(placement)
        billed.until.value shouldBe placement.offerBillingDeadline.value
      }
    }

    "return None on fresh" in {
      forAll(goodRecordGen(productGen = Fresh)) { fresh =>
        val billed = Iterable(fresh)
        billed.until shouldBe None
      }
    }

    "return None on reset" in {
      forAll(goodRecordGen(productGen = Reset)) { reset =>
        val billed = Iterable(reset)
        billed.until shouldBe None
      }
    }

    "return None on badge" in {
      forAll(goodRecordGen(productGen = Badge)) { badge =>
        val billed = Iterable(badge)
        billed.until shouldBe None
      }
    }

    "return None on turbo" in {
      forAll(goodRecordGen(productGen = Turbo)) { turbo =>
        val billed = Iterable(turbo)
        billed.until shouldBe None
      }
    }

    def placementGen(deadline: DateTime): Gen[Record] =
      goodRecordGen(productGen = Placement, offerBillingDeadlineGen = deadline)

    "return max deadline of history ordered by deadline asc" in {
      val earlierDeadline = DateTime.parse("2019-11-05T12:00:07+03:00")
      val laterDeadline = DateTime.parse("2019-11-07T12:00:07+03:00")
      forAll(
        placementGen(earlierDeadline),
        placementGen(laterDeadline)
      ) { (earlierPlacement, laterPlacement) =>
        val billed = Iterable(earlierPlacement, laterPlacement)
        billed.until.value shouldBe laterDeadline
      }
    }

    "return max deadline of history ordered by deadline desc" in {
      val earlierDeadline = DateTime.parse("2019-11-05T12:00:07+03:00")
      val laterDeadline = DateTime.parse("2019-11-07T12:00:07+03:00")
      forAll(
        placementGen(earlierDeadline),
        placementGen(laterDeadline)
      ) { (earlierPlacement, laterPlacement) =>
        val billed = Iterable(laterPlacement, earlierPlacement)
        billed.until.value shouldBe laterDeadline
      }
    }
  }

  "RichAlreadyBilledProductRecords.offerPlacementDay" should {

    def placementGen(
        firstActivateDate: FirstActivateDate,
        deadline: DateTime
    ): Gen[Record] =
      goodRecordGen(
        productGen = Placement,
        firstActivateDateGen = firstActivateDate,
        offerBillingDeadlineGen = deadline
      )

    "return 1 on empty bill history of placement" in {
      val billed = Iterable.empty[Record]
      billed.offerPlacementDay(Placement).value shouldBe 1
    }

    "return None on empty bill history of non-placement" in {
      val billed = Iterable.empty[Record]
      billed.offerPlacementDay(Special) shouldBe None
    }

    "return None on non-placement product with bill history" in {
      forAll(goodRecordGen(productGen = Premium)) { nonPlacement =>
        val billed = Iterable(nonPlacement)
        billed.offerPlacementDay(Premium) shouldBe None
      }
    }

    "return duration of one placement + 1" in {
      val firstActivateDate =
        FirstActivateDate(DateTime.parse("2019-11-02T12:27:00+03:00"))
      val deadline = DateTime.parse("2019-11-06T12:27:00+03:00")
      forAll(placementGen(firstActivateDate, deadline)) { placement =>
        val billed = Iterable(placement)
        // 11-02 minus 11-06 == 4 days, hence now is 5th day
        billed.offerPlacementDay(Placement).value shouldBe 5
      }
    }

    "return sum of placement durations + 1" in {
      val earlierFirstPlacementGen =
        FirstActivateDate(DateTime.parse("2019-11-01T12:28:00+03:00"))
      val earlierDeadline = DateTime.parse("2019-11-02T12:28:00+03:00")
      val laterFirstPlacementGen =
        FirstActivateDate(DateTime.parse("2019-11-02T16:42:00+03:00"))
      val laterDeadline = DateTime.parse("2019-11-07T16:42:00+03:00")
      forAll(
        placementGen(earlierFirstPlacementGen, earlierDeadline),
        placementGen(laterFirstPlacementGen, laterDeadline)
      ) { (earlierPlacement, laterPlacement) =>
        val billed = Iterable(earlierPlacement, laterPlacement)
        // 1 (earlier) + 5 (later) + 1
        billed.offerPlacementDay(Placement).value shouldBe 7
      }
    }

    "return duration of positive placement + 1 in case of negative placement existence" in {
      val firstActivateDate =
        FirstActivateDate(DateTime.parse("2019-11-01T12:27:00+03:00"))
      val deadline = DateTime.parse("2019-11-30T12:27:00+03:00")
      val negativeFirstActivateDate =
        FirstActivateDate(DateTime.parse("2019-12-05T15:00:00+03:00"))
      forAll(
        placementGen(firstActivateDate, deadline),
        placementGen(negativeFirstActivateDate, deadline)
      ) { (placement, negativePlacement) =>
        val billed = Iterable(placement, negativePlacement)
        // 11-01 minus 11-30 == 29 days, hence now is 30th day
        billed.offerPlacementDay(Placement).value shouldBe 30
      }
    }
  }
}
