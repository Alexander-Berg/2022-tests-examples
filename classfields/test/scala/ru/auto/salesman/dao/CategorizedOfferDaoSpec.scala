package ru.auto.salesman.dao

import org.joda.time.DateTime
import ru.auto.api.ApiOfferModel.{Category, Section}
import ru.auto.salesman.dao.OfferDao.Condition.OfferIdCategory
import ru.auto.salesman.dao.OfferDao._
import ru.auto.salesman.model._
import ru.auto.salesman.test.BaseSpec

trait CategorizedOfferDaoSpec extends BaseSpec {

  def offerDao: OfferDao

  "CategorizedOfferDao" should {
    "get nothing for filter with empty result" in {
      offerDao
        .get(ForIdAndCategory(1L, OfferCategories.Commercial))
        .success
        .value shouldBe empty
    }

    "get one record for filter by id and category" in {
      val offerId = 1002876493L
      val category = OfferCategories.Trucks
      val clientId = 12L

      val List(offer) =
        offerDao.get(ForIdAndCategory(offerId, category)).success.value
      offer.id shouldBe offerId
      offer.offerHash shouldBe "dh46d"
      offer.categoryId shouldBe category
      offer.sectionId shouldBe Section.USED
      offer.price shouldBe 123
      offer.status shouldBe OfferStatuses.Show
      offer.clientId shouldBe clientId
    }

    "get one record for filter by client id, category and status" in {
      val offerId = 1002876493L
      val category = OfferCategories.Trucks
      val clientId = 12L

      val List(offer) = offerDao
        .get(ForClientIdCategoryStatus(clientId, category, OfferStatuses.Show))
        .success
        .value
      offer.id shouldBe offerId
    }

    "get zero records for filter by client id, category and status" in {
      val category = OfferCategories.Trucks
      val clientId = 12L

      offerDao
        .get(
          ForClientIdCategoryStatus(clientId, category, OfferStatuses.Hidden)
        )
        .success
        .value shouldBe Nil
    }

    "get two records for filter ForClientPaymentGroupStatus" in {
      val status = Set(
        OfferStatuses.Expired,
        OfferStatuses.WaitingActivation,
        OfferStatuses.Show
      )
      val paymentGroup =
        PaymentGroup(Category.TRUCKS, Set(Section.NEW, Section.USED))
      val clientId = 12L
      val filter = ForClientPaymentGroupStatus(clientId, paymentGroup, status)

      val offers = offerDao.get(filter).success.value
      offers should have size 2
      val filtered = offers.map { o =>
        (o.id, o.clientId, o.status, o.categoryId)
      }
      val expected = Iterable(
        (1002876493L, 12L, OfferStatuses.Show, OfferCategories.Trucks),
        (1002876478L, 12L, OfferStatuses.Expired, OfferCategories.Swapbody)
      )
      filtered should contain theSameElementsAs expected
    }

    "failed for get records filtered by filters than id and category " in {
      offerDao
        .get(
          ForClientIdCategoryStatus(
            1L,
            OfferCategories.Cars,
            OfferStatuses.Show
          )
        )
        .failure
      offerDao.get(ForIdAndCategory(1L, OfferCategories.Scooters)).success
    }

    "failed get records for not exist category" in {
      offerDao.get(ForIdAndCategory(1L, OfferCategories.Cars)).failure
    }

    "update offer" in {
      val expireDate = DateTime.now.plusHours(2).withMillisOfSecond(0)
      val status = OfferStatuses.Expired
      val setDate = DateTime.now.plusHours(1).withMillisOfSecond(0)
      val freshDate = setDate.plusHours(1)
      val patch = OfferPatch(
        Some(expireDate),
        Some(status),
        Some(setDate),
        Some(freshDate)
      )
      val condition = OfferIdCategory(1002876471L, OfferCategories.Swapbody)

      offerDao.update(condition, patch).success

      val offers = offerDao
        .get(ForIdAndCategory(condition.offerId, condition.offerCategory))
        .success
        .value
      offers should have size 1
      val offer = offers.head
      offer.id shouldBe condition.offerId
      offer.expireDate shouldBe expireDate
      offer.status shouldBe OfferStatuses.Expired
      offer.setDate shouldBe setDate
      offer.freshDate.value shouldBe freshDate
    }

    "do nothing on empty patch" in {
      val patch = OfferPatch()
      val condition = OfferIdCategory(1002876471L, OfferCategories.Swapbody)

      def get() =
        offerDao
          .get(ForIdAndCategory(condition.offerId, condition.offerCategory))
          .success
          .value

      val before = get()
      offerDao.update(condition, patch).success.value shouldBe (())
      val after = get()
      after shouldBe before
    }

    "failed update offer with cars category" in {
      val expireDate = DateTime.now.plusHours(2).withMillisOfSecond(0)
      val status = OfferStatuses.Expired
      val setDate = DateTime.now.plusHours(1).withMillisOfSecond(0)
      val patch = OfferPatch(Some(expireDate), Some(status), Some(setDate))
      val condition = OfferIdCategory(1002876493L, OfferCategories.Cars)

      offerDao.update(condition, patch).failure
    }

    "failed update offer status with cars category" in {
      val status = OfferStatuses.Expired
      val patch = OfferPatch(None, Some(status), None)
      val condition = OfferIdCategory(1002876493L, OfferCategories.Cars)

      offerDao.update(condition, patch).failure
    }

    "failed update offer expire date with cars category" in {
      val expireDate = DateTime.now.plusHours(2).withMillisOfSecond(0)
      val patch = OfferPatch(Some(expireDate), None, None)
      val condition = OfferIdCategory(1002876493L, OfferCategories.Cars)

      offerDao.update(condition, patch).failure
    }

    "failed update offer set date with cars category" in {
      val setDate = DateTime.now.plusHours(2).withMillisOfSecond(0)
      val patch = OfferPatch(None, None, Some(setDate))
      val condition = OfferIdCategory(1002876493L, OfferCategories.Cars)

      offerDao.update(condition, patch).failure
    }
  }

}
