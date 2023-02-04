package ru.auto.salesman.dao

import ru.auto.salesman.model.{GoodStatuses, OfferCategories, OfferCategory, OfferId}
import ru.auto.salesman.test.BaseSpec

trait BadgeDaoSpec extends BaseSpec {

  def badgeDao: BadgeDao

  val TestCarsOfferId: OfferId = 1003332211L
  val TestCarsCategory: OfferCategory = OfferCategories.Cars

  val TestMotoOfferId: OfferId = 1163788L
  val TestMotoCategory: OfferCategory = OfferCategories.Motorcycle

  "BadgeDao" should {
    "get nothing for filter with empty result" in {
      badgeDao
        .getBadge(1L, TestCarsOfferId, TestCarsCategory)
        .success
        .value shouldBe empty
    }

    "get one record for filter with active row" in {
      badgeDao
        .getBadge(1112223L, TestCarsOfferId, TestCarsCategory)
        .success
        .value
        .value shouldBe "Cars-Active-1"

      badgeDao
        .getBadge(1112224L, TestCarsOfferId, TestCarsCategory)
        .success
        .value
        .value shouldBe "Cars-Active-2"

      badgeDao
        .getBadge(1112227L, TestMotoOfferId, TestMotoCategory)
        .success
        .value
        .value shouldBe "Moto-Active-2"
    }

    "get nothing for filter with inactive row" in {
      badgeDao
        .getBadge(1112225L, TestCarsOfferId, TestCarsCategory)
        .success
        .value shouldBe empty
    }

    "update status" in {
      badgeDao
        .updateStatus(1112225L, TestCarsOfferId, TestCarsCategory, GoodStatuses.Active)
        .success

      badgeDao
        .getBadge(1112225L, TestCarsOfferId, TestCarsCategory)
        .success
        .value
        .value shouldBe "Cars-Inactive-1"
    }

    "delete badge" in {
      badgeDao
        .getBadge(1112223L, TestCarsOfferId, TestCarsCategory)
        .success
        .value
        .value shouldBe "Cars-Active-1"
      badgeDao
        .delete(1112223L, TestCarsOfferId, TestCarsCategory)
        .success
        .value shouldBe (())
      badgeDao
        .getBadge(1112223L, TestCarsOfferId, TestCarsCategory)
        .success
        .value shouldBe None
    }
  }

}
