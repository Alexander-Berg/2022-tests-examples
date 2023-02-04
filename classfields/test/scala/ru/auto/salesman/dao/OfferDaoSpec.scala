package ru.auto.salesman.dao

import org.joda.time.DateTime
import org.scalatest.Inspectors
import ru.auto.api.ApiOfferModel.{Category, Section}
import ru.auto.salesman.dao.OfferDao.Condition.OfferIdCategory
import ru.auto.salesman.dao.OfferDao._
import ru.auto.salesman.environment.now
import ru.auto.salesman.model._
import ru.auto.salesman.tasks.Partition
import ru.auto.salesman.test.BaseSpec
import zio.UIO

trait OfferDaoSpec extends BaseSpec {

  def offerDao: OfferDao

  "OfferDao" should {
    "get nothing for filter with empty result" in {
      offerDao
        .get(ForIdAndCategory(1L, OfferCategories.Cars))
        .success
        .value shouldBe empty
    }

    "get one record for filter by id and category" in {
      val offerId = 1002876471L
      val List(offer) = offerDao
        .get(ForIdAndCategory(offerId, OfferCategories.Cars))
        .success
        .value
      offer.id shouldBe offerId
      offer.offerHash shouldBe "2fe245"
      offer.categoryId shouldBe OfferCategories.Cars
      offer.sectionId shouldBe Section.NEW
      offer.price shouldBe 1250000
      offer.currency shouldBe OfferCurrencies.RUR
      offer.status shouldBe OfferStatuses.CreatedByClient
      offer.clientId shouldBe 16283
    }

    "failed for filter by id and category not equal cars" in {
      val offerId = 1002876471L
      offerDao
        .get(ForIdAndCategory(offerId, OfferCategories.Trailer))
        .failure
        .exception shouldBe an[UnsupportedOperationException]
    }

    "get records for filter ForClientPaymentGroupStatus" in {
      val status = Set(
        OfferStatuses.Expired,
        OfferStatuses.WaitingActivation,
        OfferStatuses.Show
      )
      val paymentGroup = PaymentGroup(Category.CARS, Set(Section.USED))
      val clientId = 16301L
      val filter = ForClientPaymentGroupStatus(clientId, paymentGroup, status)

      val offers = offerDao.get(filter).success.value
      offers.size should be(4)
      val filtered = offers.map { o =>
        (o.id, o.clientId, o.status, o.sectionId)
      }
      val expected = Iterable(
        (1003275380, clientId, OfferStatuses.WaitingActivation, Section.USED),
        (1003276986, clientId, OfferStatuses.Show, Section.USED),
        (1111, clientId, OfferStatuses.Show, Section.USED),
        (2222, clientId, OfferStatuses.Show, Section.USED)
      )
      filtered should contain theSameElementsAs expected
    }

    "get waiting activation since" in {

      offerDao
        .get(
          OfStatusSinceSetDate(1425168000000L, OfferStatuses.WaitingActivation)
        )
        .success
        .value shouldBe empty

      val offers = offerDao
        .get(
          OfStatusSinceSetDate(1423665240000L, OfferStatuses.WaitingActivation)
        )
        .success
        .value
      offers.size should be(1)
      offers.head.id should be(1003275380L)
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
      val condition = OfferIdCategory(1002876471L, OfferCategories.Cars)

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

    "update offer status" in {
      val status = OfferStatuses.Expired
      val patch = OfferPatch(None, Some(status), None)
      val condition = OfferIdCategory(1002876493L, OfferCategories.Cars)

      offerDao.update(condition, patch).success
      val offers = offerDao
        .get(ForIdAndCategory(condition.offerId, condition.offerCategory))
        .success
        .value
      offers should have size 1
      val offer = offers.head
      offer.id shouldBe condition.offerId
      offer.status shouldBe OfferStatuses.Expired
    }

    "do nothing on empty patch" in {
      val patch = OfferPatch()
      val condition = OfferIdCategory(1002876493L, OfferCategories.Cars)

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

    "update failed for not cars category" in {
      val patch = OfferPatch(None, Some(OfferStatuses.Expired), None)
      val condition = OfferIdCategory(1002876493L, OfferCategories.Carting)

      offerDao
        .update(condition, patch)
        .failure
        .exception shouldBe an[UnsupportedOperationException]
      offerDao
        .update(condition, OfferPatch(Some(now()), None, None))
        .failure
        .exception shouldBe an[UnsupportedOperationException]
      offerDao
        .update(condition, OfferPatch(None, None, Some(now())))
        .failure
        .exception shouldBe an[UnsupportedOperationException]
    }

    "find offers by client id and offer status" in {
      val clientId = 16301L
      val offerStatus = OfferStatuses.Freeze
      val offers = offerDao
        .get(
          ForClientIdCategoryStatus(clientId, OfferCategories.Cars, offerStatus)
        )
        .success
        .value
      offers should have size 2
      (offers.map(_.id) should contain).allOf(1003331550L, 1003331551L)
      offers.map(_.status) should contain only offerStatus
    }
  }

  // Properties of test data for this method:
  // 1. All user ids are located in range (2, 78).
  // 2. They all are even.
  // 3. Offer owned by user 10 is inactive.
  // 4. User 22 is a dealer.
  "getSortedUsersWithActiveOffers()" should {

    val partitions = Partition.all(4)

    val userOnlyWithInactiveOffer = 10
    val dealerUser = 22
    val allUserIds =
      (2 to 78 by 2).filter(id => id != userOnlyWithInactiveOffer && id != dealerUser)

    def getAllSortedUsersWithActiveOffers(
        withUserIdMoreThan: Option[Long] = None
    ): List[Long] =
      partitions.flatMap { partition =>
        getSortedUsersWithActiveOffers(partition, withUserIdMoreThan)
      }

    def getSortedUsersWithActiveOffers(
        partition: Partition,
        withUserIdMoreThan: Option[Long]
    ): List[Long] =
      offerDao
        .getSortedUsersWithActiveOffers(partition, withUserIdMoreThan)
        .use(users => UIO(users.toList))
        .success
        .value

    "not find any user ids more than 100" in {
      Inspectors.forEvery(partitions) { partition =>
        getSortedUsersWithActiveOffers(
          partition,
          withUserIdMoreThan = Some(100)
        ) shouldBe empty
      }
    }

    "not find user only with inactive offer" in {
      getAllSortedUsersWithActiveOffers() should not contain userOnlyWithInactiveOffer
    }

    "not find dealer user" in {
      getAllSortedUsersWithActiveOffers() should not contain dealerUser
    }

    "find all user ids" in {
      getAllSortedUsersWithActiveOffers() should contain theSameElementsAs allUserIds
    }

    "find exactly last user id" in {
      getAllSortedUsersWithActiveOffers(withUserIdMoreThan =
        Some(76)
      ) should contain only 78
    }

    "distribute user ids ~ evenly across partitions" in {
      val partitionSizes = partitions.map(
        getSortedUsersWithActiveOffers(_, withUserIdMoreThan = None).size
      )
      // Total user count = 39.
      // Partitions count = 4.
      // So, each partition should have from 5 to 15 elements to consider
      // distribution ~ even.
      Inspectors.forEvery(partitionSizes)(_ should be >= 5)
      Inspectors.forEvery(partitionSizes)(_ should be <= 15)
    }

    "have all partitions sorted" in {
      val userPartitions = partitions.map(
        getSortedUsersWithActiveOffers(_, withUserIdMoreThan = None)
      )
      Inspectors.forEvery(userPartitions)(_ shouldBe sorted)
    }
  }

}
