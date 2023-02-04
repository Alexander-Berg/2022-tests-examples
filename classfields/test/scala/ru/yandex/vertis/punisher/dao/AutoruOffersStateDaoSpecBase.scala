package ru.yandex.vertis.punisher.dao

import ru.yandex.vertis.moderation.proto.Autoru.AutoruEssentials
import ru.yandex.vertis.punisher.Generators._
import ru.yandex.vertis.punisher.dao.AutoruOffersStateDao.{Sort, UpsertOptions}
import ru.yandex.vertis.punisher.dao.impl.ydb.clearable.Clearable
import ru.yandex.vertis.punisher.model.{OfferId, OfferState, UserId}
import ru.yandex.vertis.punisher.{BaseSpec, Generators}
import ru.yandex.vertis.quality.cats_utils.Awaitable._
import ru.yandex.vertis.quality.lang_utils.Use
import ru.yandex.vertis.quality.lang_utils.interval.Interval

import java.time.Instant
import java.time.temporal.ChronoUnit

/**
  * @author mpoplavkov
  */
trait AutoruOffersStateDaoSpecBase extends BaseSpec {

  type Dao <: AutoruOffersStateDao[F]
  def dao: Dao
  def clearable: Clearable[Dao]

  before {
    clearable.clear()
  }

  private val now = Instant.now()
  private val offerState1 = Generators.generate[OfferState.Autoru].copy(firstActivated = None)
  private val offerState2 =
    Generators
      .generateSuchThat[OfferState.Autoru](s => s.offerId != offerState1.offerId && s.userId != offerState1.userId)
      .copy(firstActivated = Some(now))
  private val offerState3 =
    Generators
      .generateSuchThat[OfferState.Autoru](s =>
        s.offerId != offerState1.offerId && s.userId != offerState1.userId &&
          s.offerId != offerState2.offerId && s.userId != offerState2.userId
      )
      .copy(firstActivated = Some(now))

  "AutoruOffersStateDaoImpl.getStates" should {

    "select state by id" in {
      assume(getById(offerState1.userId, offerState1.offerId).await.isEmpty)
      dao.upsert(offerState1, UpsertOptions.RewriteAllValues).await
      getById(offerState1.userId, offerState1.offerId).await shouldBe Some(offerState1)
    }

    "select rows only with matching pair of user and offer" in {
      dao.upsert(offerState1, UpsertOptions.RewriteAllValues).await
      dao.upsert(offerState2, UpsertOptions.RewriteAllValues).await

      getById(offerState1.userId, offerState2.offerId).await shouldBe None
      getById(offerState2.userId, offerState1.offerId).await shouldBe None
    }

    "select all rows for an empty filter" in {
      dao.upsert(offerState1, UpsertOptions.RewriteAllValues).await
      dao.upsert(offerState2, UpsertOptions.RewriteAllValues).await

      dao
        .getStates(
          filter = AutoruOffersStateDao.Filter.Empty,
          sort = AutoruOffersStateDao.Sort.ByTriggerEventDatetime(),
          limit = 10
        )
        .await should contain theSameElementsAs Seq(offerState1, offerState2)
    }

    "select only matched rows" in {
      dao.upsert(offerState1, UpsertOptions.RewriteAllValues).await
      dao.upsert(offerState2, UpsertOptions.RewriteAllValues).await

      dao
        .getStates(
          filter =
            AutoruOffersStateDao.Filter(userIds = Use(Set(offerState1.userId)), offerId = Use(offerState1.offerId)),
          sort = AutoruOffersStateDao.Sort.ByTriggerEventDatetime(),
          limit = 10
        )
        .await shouldBe Seq(offerState1)
    }

    "select a few rows matching to the given criteria" in {
      val sameUserOffer = offerState2.copy(userId = offerState1.userId)
      dao.upsert(offerState1, UpsertOptions.RewriteAllValues).await
      dao.upsert(sameUserOffer, UpsertOptions.RewriteAllValues).await

      dao
        .getStates(
          filter = AutoruOffersStateDao.Filter(userIds = Use(Set(offerState1.userId))),
          sort = AutoruOffersStateDao.Sort.ByTriggerEventDatetime(),
          limit = 10
        )
        .await should contain theSameElementsAs Seq(offerState1, sameUserOffer)
    }

    "select rows in order" in {
      val offerStateWithTriggerEventDT1 =
        Generators
          .generateSuchThat[OfferState.Autoru](_.triggerEventDatetime.isDefined)
      val offerStateWithTriggerEventDT2 =
        Generators
          .generateSuchThat[OfferState.Autoru](_.offerId != offerStateWithTriggerEventDT1.offerId)
          .copy(triggerEventDatetime = offerStateWithTriggerEventDT1.triggerEventDatetime.map(_.minusSeconds(1)))
      dao.upsert(offerStateWithTriggerEventDT1, UpsertOptions.RewriteAllValues).await
      dao.upsert(offerStateWithTriggerEventDT2, UpsertOptions.RewriteAllValues).await

      dao
        .getStates(
          filter = AutoruOffersStateDao.Filter.Empty,
          sort = AutoruOffersStateDao.Sort.ByTriggerEventDatetime(),
          limit = 10
        )
        .await shouldBe Seq(offerStateWithTriggerEventDT2, offerStateWithTriggerEventDT1)
    }

    "select not more than specified limit" in {
      dao.upsert(offerState1, UpsertOptions.RewriteAllValues).await
      dao.upsert(offerState2, UpsertOptions.RewriteAllValues).await

      def getWithLimit(limit: Int): Seq[OfferState.Autoru] =
        dao
          .getStates(
            filter = AutoruOffersStateDao.Filter.Empty,
            sort = AutoruOffersStateDao.Sort.ByTriggerEventDatetime(),
            limit = limit
          )
          .await

      assume(getWithLimit(10).size > 1)
      getWithLimit(1).size shouldBe 1
    }

    "select several users' offers" in {
      dao.upsert(offerState1, UpsertOptions.RewriteAllValues).await
      dao.upsert(offerState2, UpsertOptions.RewriteAllValues).await

      dao
        .getStates(
          filter = AutoruOffersStateDao.Filter(userIds = Use(Set(offerState1.userId, offerState2.userId))),
          sort = AutoruOffersStateDao.Sort.ByTriggerEventDatetime(),
          limit = 10
        )
        .await should contain theSameElementsAs Seq(offerState1, offerState2)
    }

    "select rows with matched user ids only" in {
      dao.upsert(offerState1, UpsertOptions.RewriteAllValues).await
      dao.upsert(offerState2, UpsertOptions.RewriteAllValues).await
      dao.upsert(offerState3, UpsertOptions.RewriteAllValues).await

      dao
        .getStates(
          filter = AutoruOffersStateDao.Filter(userIds = Use(Set(offerState1.userId, offerState2.userId))),
          sort = AutoruOffersStateDao.Sort.ByTriggerEventDatetime(),
          limit = 10
        )
        .await should contain theSameElementsAs Seq(offerState1, offerState2)
    }

    "select offers with matched VINs only" in {
      val offerStateWithVin1 = Generators.generateSuchThat[OfferState.Autoru](_.vin.isDefined)
      val offerStateWithVin2 =
        Generators.generateSuchThat[OfferState.Autoru](s => s.vin.isDefined && s.vin != offerStateWithVin1.vin)
      dao.upsert(offerStateWithVin1, UpsertOptions.RewriteAllValues).await
      dao.upsert(offerStateWithVin2, UpsertOptions.RewriteAllValues).await

      dao
        .getStates(
          filter = AutoruOffersStateDao.Filter(vins = Use(Set(offerStateWithVin1.vin.get))),
          sort = AutoruOffersStateDao.Sort.ByTriggerEventDatetime(),
          limit = 10
        )
        .await should contain theSameElementsAs Seq(offerStateWithVin1)
    }

    "select nothing for query with an empty VIN list" in {
      dao.upsert(offerState1, UpsertOptions.RewriteAllValues).await
      dao.upsert(offerState2, UpsertOptions.RewriteAllValues).await

      dao
        .getStates(
          filter = AutoruOffersStateDao.Filter(vins = Use(Set.empty)),
          sort = AutoruOffersStateDao.Sort.ByTriggerEventDatetime(),
          limit = 10
        )
        .await shouldBe Symbol("empty")
    }

    "select active offers that were deactivated after specified instant" in {
      val since = generate[Instant]
      val activeOffer1 = offerState1.copy(deactivated = Some(since.plusMillis(1)), isActive = false)
      val activeOffer2 = offerState2.copy(deactivated = Some(since.plusSeconds(999999)), isActive = false)
      val nonActiveOffer = offerState3.copy(deactivated = Some(since.minusMillis(1)), isActive = false)
      dao.upsert(activeOffer1, UpsertOptions.RewriteAllValues).await
      dao.upsert(activeOffer2, UpsertOptions.RewriteAllValues).await
      dao.upsert(nonActiveOffer, UpsertOptions.RewriteAllValues).await

      dao
        .getStates(
          filter = AutoruOffersStateDao.Filter(wasActiveSince = Use(since)),
          sort = AutoruOffersStateDao.Sort.ByTriggerEventDatetime(),
          limit = 10
        )
        .await should contain theSameElementsAs Seq(activeOffer1, activeOffer2)
    }

    "select active offers with isActive flag set to true" in {
      val since = generate[Instant]
      val activeOffer1 = offerState1.copy(deactivated = None, isActive = true)
      val activeOffer2 = offerState2.copy(deactivated = Some(since.minusMillis(1)), isActive = true)
      val nonActiveOffer = offerState3.copy(deactivated = None, isActive = false)
      dao.upsert(activeOffer1, UpsertOptions.RewriteAllValues).await
      dao.upsert(activeOffer2, UpsertOptions.RewriteAllValues).await
      dao.upsert(nonActiveOffer, UpsertOptions.RewriteAllValues).await

      dao
        .getStates(
          filter = AutoruOffersStateDao.Filter(wasActiveSince = Use(since)),
          sort = AutoruOffersStateDao.Sort.ByTriggerEventDatetime(),
          limit = 10
        )
        .await should contain theSameElementsAs Seq(activeOffer1, activeOffer2)
    }

    "select active offers along with the offers deactivated after the specified instant" in {
      val since = generate[Instant]
      val activeOffer1 = offerState1.copy(deactivated = None, isActive = true)
      val activeOffer2 = offerState2.copy(deactivated = Some(since.plusMillis(1)), isActive = false)
      val nonActiveOffer = offerState3.copy(deactivated = None, isActive = false)
      dao.upsert(activeOffer1, UpsertOptions.RewriteAllValues).await
      dao.upsert(activeOffer2, UpsertOptions.RewriteAllValues).await
      dao.upsert(nonActiveOffer, UpsertOptions.RewriteAllValues).await

      dao
        .getStates(
          filter = AutoruOffersStateDao.Filter(wasActiveSince = Use(since)),
          sort = AutoruOffersStateDao.Sort.ByTriggerEventDatetime(),
          limit = 10
        )
        .await should contain theSameElementsAs Seq(activeOffer1, activeOffer2)
    }
  }

  "AutoruOffersStateDaoImpl.getStatesLimitless" should {

    "select all states with an empty filter" in {
      dao.upsert(offerState1, UpsertOptions.RewriteAllValues).await
      dao.upsert(offerState2, UpsertOptions.RewriteAllValues).await
      dao.upsert(offerState3, UpsertOptions.RewriteAllValues).await

      val result = dao.getStatesLimitless(AutoruOffersStateDao.Filter.Empty).await
      result should contain theSameElementsAs Seq(offerState1, offerState2, offerState3)
    }

    "select all offers by vin which condition does not need repair" in {
      val offerWithSameVin1 = offerState1.copy(vin = Some("VIN-1"), condition = Some(AutoruEssentials.Condition.GOOD))
      val offerWithSameVin2 =
        offerState2.copy(vin = Some("VIN-2"), condition = Some(AutoruEssentials.Condition.EXCELLENT))
      val offerWithSameVin3 =
        offerState3.copy(vin = Some("VIN-1"), condition = Some(AutoruEssentials.Condition.NEED_REPAIR))
      dao.upsert(offerWithSameVin1, UpsertOptions.RewriteAllValues).await
      dao.upsert(offerWithSameVin2, UpsertOptions.RewriteAllValues).await
      dao.upsert(offerWithSameVin3, UpsertOptions.RewriteAllValues).await
      val result =
        dao
          .getStatesLimitless(
            AutoruOffersStateDao
              .Filter(vins = Use(Set("VIN-1")), excludeCondition = Use(AutoruEssentials.Condition.NEED_REPAIR))
          )
          .await
      result should contain theSameElementsAs Seq(offerWithSameVin1)
    }

    "select all offers by vin, which were not placed by call center" in {
      val offerWithSameVin1 = offerState1.copy(vin = Some("VIN-1"), isCallCenter = Some(true))
      val offerWithSameVin2 = offerState2.copy(vin = Some("VIN-2"))
      val offerWithSameVin3 = offerState3.copy(vin = Some("VIN-1"), isCallCenter = Some(false))
      dao.upsert(offerWithSameVin1, UpsertOptions.RewriteAllValues).await
      dao.upsert(offerWithSameVin2, UpsertOptions.RewriteAllValues).await
      dao.upsert(offerWithSameVin3, UpsertOptions.RewriteAllValues).await
      val result =
        dao.getStatesLimitless(AutoruOffersStateDao.Filter(vins = Use(Set("VIN-1")), isCallCenter = Use(false))).await
      result should contain theSameElementsAs Seq(offerWithSameVin3)
    }

    "select all offers by vin, which creation date is before" in {
      val offerWithSameVin1 =
        offerState1.copy(vin = Some("VIN-1"), creationDate = Some(Instant.now().minus(42, ChronoUnit.DAYS)))
      val offerWithSameVin2 = offerState2.copy(vin = Some("VIN-2"))
      val offerWithSameVin3 =
        offerState3.copy(vin = Some("VIN-1"), creationDate = Some(Instant.now().minus(40, ChronoUnit.DAYS)))
      dao.upsert(offerWithSameVin1, UpsertOptions.RewriteAllValues).await
      dao.upsert(offerWithSameVin2, UpsertOptions.RewriteAllValues).await
      dao.upsert(offerWithSameVin3, UpsertOptions.RewriteAllValues).await
      val result =
        dao
          .getStatesLimitless(
            AutoruOffersStateDao
              .Filter(vins = Use(Set("VIN-1")), createdBefore = Use(Instant.now().minus(41, ChronoUnit.DAYS)))
          )
          .await
      result should contain theSameElementsAs Seq(offerWithSameVin3)
    }

    "select all states by vin and deactivation date in range" in {
      val offerWithSameVin1 =
        offerState1.copy(vin = Some("VIN-1"), deactivated = Some(Instant.now().minus(80, ChronoUnit.DAYS)))
      val offerWithSameVin2 = offerState2.copy(vin = Some("VIN-2"))
      val offerWithSameVin3 =
        offerState3.copy(vin = Some("VIN-1"), deactivated = Some(Instant.now().minus(40, ChronoUnit.DAYS)))
      dao.upsert(offerWithSameVin1, UpsertOptions.RewriteAllValues).await
      dao.upsert(offerWithSameVin2, UpsertOptions.RewriteAllValues).await
      dao.upsert(offerWithSameVin3, UpsertOptions.RewriteAllValues).await
      val timeRange =
        Interval(
          now.minus(60, ChronoUnit.DAYS).toEpochMilli,
          now.plus(60, ChronoUnit.DAYS).toEpochMilli
        )
      val result =
        dao
          .getStatesLimitless(
            AutoruOffersStateDao.Filter(vins = Use(Set("VIN-1")), lastDeactivationInterval = Use(timeRange))
          )
          .await
      result should contain theSameElementsAs Seq(offerWithSameVin3)
    }

    "select all states matching to the  given filter for  vin-replacement task" in {
      val correctOffer =
        Generators
          .generate[OfferState.Autoru]
          .copy(
            vin = Some("VIN-666"),
            deactivated = Some(Instant.now().minus(24, ChronoUnit.DAYS)),
            condition = Some(AutoruEssentials.Condition.EXCELLENT),
            isCallCenter = Some(false),
            creationDate = Some(Instant.now().minus(63, ChronoUnit.DAYS))
          )
      val offerWithWrongDeactivationDate =
        Generators
          .generate[OfferState.Autoru]
          .copy(
            vin = Some("VIN-2"),
            deactivated = Some(Instant.now().minus(80, ChronoUnit.DAYS)),
            condition = Some(AutoruEssentials.Condition.EXCELLENT),
            isCallCenter = Some(false),
            creationDate = Some(Instant.now().minus(64, ChronoUnit.DAYS))
          )
      val offerWithWrongCondition =
        Generators
          .generate[OfferState.Autoru]
          .copy(
            vin = Some("VIN-3"),
            deactivated = Some(Instant.now().minus(24, ChronoUnit.DAYS)),
            condition = Some(AutoruEssentials.Condition.NEED_REPAIR),
            isCallCenter = Some(false),
            creationDate = Some(Instant.now().minus(64, ChronoUnit.DAYS))
          )
      val offerWithWrongCallCenterFlag =
        Generators
          .generate[OfferState.Autoru]
          .copy(
            vin = Some("VIN-3"),
            deactivated = Some(Instant.now().minus(24, ChronoUnit.DAYS)),
            condition = Some(AutoruEssentials.Condition.NEED_REPAIR),
            isCallCenter = Some(true),
            creationDate = Some(Instant.now().minus(64, ChronoUnit.DAYS))
          )
      val offerWithOldCreationDate =
        Generators
          .generate[OfferState.Autoru]
          .copy(
            vin = Some("VIN-4"),
            deactivated = Some(Instant.now().minus(24, ChronoUnit.DAYS)),
            condition = Some(AutoruEssentials.Condition.NEED_REPAIR),
            isCallCenter = Some(true),
            creationDate = Some(Instant.now().minus(66, ChronoUnit.DAYS))
          )

      dao.upsert(correctOffer, UpsertOptions.RewriteAllValues).await
      dao.upsert(offerWithWrongDeactivationDate, UpsertOptions.RewriteAllValues).await
      dao.upsert(offerWithWrongCondition, UpsertOptions.RewriteAllValues).await
      dao.upsert(offerWithWrongCallCenterFlag, UpsertOptions.RewriteAllValues).await
      dao.upsert(offerWithOldCreationDate, UpsertOptions.RewriteAllValues).await

      val timeRange =
        Interval(
          now.minus(25, ChronoUnit.DAYS).toEpochMilli,
          now.plus(25, ChronoUnit.DAYS).toEpochMilli
        )

      val result =
        dao
          .getStatesLimitless(
            AutoruOffersStateDao.Filter(
              isCallCenter = Use(false),
              createdBefore = Use(Instant.now().minus(65, ChronoUnit.DAYS)),
              excludeCondition = Use(AutoruEssentials.Condition.NEED_REPAIR),
              lastDeactivationInterval = Use(timeRange)
            )
          )
          .await
      result should contain theSameElementsAs Seq(correctOffer)
    }

    "select all states matching to the given filter" in {
      val state1 = offerState1.copy(vin = Some("A"))
      val state2 = offerState2.copy(vin = Some("B"))
      val state3 = offerState3.copy(vin = Some("C"))
      dao.upsert(state1, UpsertOptions.RewriteAllValues).await
      dao.upsert(state2, UpsertOptions.RewriteAllValues).await
      dao.upsert(state3, UpsertOptions.RewriteAllValues).await

      val result = dao.getStatesLimitless(AutoruOffersStateDao.Filter(vins = Use(Set("A", "B")))).await
      result should contain theSameElementsAs Seq(state1, state2)
    }

    "select all states using small pages" in {
      dao.upsert(offerState1, UpsertOptions.RewriteAllValues).await
      dao.upsert(offerState2, UpsertOptions.RewriteAllValues).await
      dao.upsert(offerState3, UpsertOptions.RewriteAllValues).await

      val result = dao.getStatesLimitless(AutoruOffersStateDao.Filter.Empty, pageSize = 1).await
      result should contain theSameElementsAs Seq(offerState1, offerState2, offerState3)
    }

  }

  "AutoruOffersStateDaoImpl.upsert" should {

    "insert new record" in {
      assume(getById(offerState1.userId, offerState1.offerId).await.isEmpty)
      dao.upsert(offerState1, UpsertOptions.RewriteAllValues).await
      getById(offerState1.userId, offerState1.offerId).await shouldBe Some(offerState1)
    }

    "replace existing record" in {
      val userId = offerState1.userId
      val offerId = offerState1.offerId
      val offerStateNew =
        Generators
          .generate[OfferState.Autoru]
          .copy(
            userId = userId,
            offerId = offerId
          )
      dao.upsert(offerState1, UpsertOptions.RewriteAllValues).await
      getById(userId, offerId).await shouldBe Some(offerState1)
      dao.upsert(offerStateNew, UpsertOptions.RewriteAllValues).await
      getById(userId, offerId).await shouldBe Some(offerStateNew)
    }

    "insert several records" in {
      dao.upsert(offerState1, UpsertOptions.RewriteAllValues).await
      dao.upsert(offerState2, UpsertOptions.RewriteAllValues).await
      getById(offerState1.userId, offerState1.offerId).await shouldBe Some(offerState1)
      getById(offerState2.userId, offerState2.offerId).await shouldBe Some(offerState2)
    }

    "replace existing value with None" in {
      val offerState = Generators.generateSuchThat[OfferState.Autoru](s => s.mark.isDefined || s.deactivated.isDefined)
      val userId = offerState.userId
      val offerId = offerState.offerId
      val offerStateWithNone = offerState.copy(mark = None, deactivated = None)
      dao.upsert(offerState, UpsertOptions.RewriteAllValues).await
      getById(userId, offerId).await shouldBe Some(offerState)
      dao.upsert(offerStateWithNone, UpsertOptions.RewriteAllValues).await
      getById(userId, offerId).await shouldBe Some(offerStateWithNone)
    }

    "not replace `deactivated` field with None using corresponding options" in {
      val deactivated = generate[Instant]
      val offerState1WithDeactivated = offerState1.copy(deactivated = Some(deactivated))
      val userId = offerState1.userId
      val offerId = offerState1.offerId
      val offerState2WithoutDeactivated =
        offerState2.copy(
          userId = userId,
          offerId = offerId,
          deactivated = None
        )
      val offerState2WithDeactivated = offerState2WithoutDeactivated.copy(deactivated = Some(deactivated))
      dao.upsert(offerState1WithDeactivated, UpsertOptions.RewriteAllValues).await
      getById(userId, offerId).await shouldBe Some(offerState1WithDeactivated)
      dao.upsert(offerState2WithoutDeactivated, UpsertOptions.LeaveStoredDeactivatedFieldIfNotPresent).await
      getById(userId, offerId).await shouldBe Some(offerState2WithDeactivated)
    }

    "create a state without 'deactivated' field with LeaveStoredDeactivatedFieldIfNotPresent options" in {
      val offer = offerState1.copy(deactivated = None)
      assume(getById(offer.userId, offer.offerId).await.isEmpty)
      dao.upsert(offer, UpsertOptions.LeaveStoredDeactivatedFieldIfNotPresent).await
      getById(offer.userId, offer.offerId).await shouldBe Some(offer)
    }

    "rewrite 'firstActivated' in db, if present in dto, do not rewrite in db, if not present in dto" in {
      def getFromDb: Option[OfferState.Autoru] = getById(offerState1.userId, offerState1.offerId).await

      val offerWithoutFirstActivated = offerState1.copy(firstActivated = None)
      val offerWithFirstActivated = offerState1.copy(firstActivated = Some(Instant.now()))
      dao.upsert(offerWithoutFirstActivated, UpsertOptions.RewriteAllValues).await

      dao.upsert(offerWithFirstActivated, UpsertOptions.RewriteAllValues).await
      getFromDb shouldBe Some(offerWithFirstActivated)

      dao.upsert(offerWithoutFirstActivated, UpsertOptions.RewriteAllValues).await
      getFromDb shouldBe Some(offerWithFirstActivated)
    }
  }

  private def getById(userId: UserId, offerId: OfferId): F[Option[OfferState.Autoru]] =
    dao
      .getStates(
        filter = AutoruOffersStateDao.Filter(userIds = Use(Set(userId)), offerId = Use(offerId)),
        sort = Sort.ByTriggerEventDatetime(order = SortOrder.Desc),
        limit = 1
      )
      .map(_.headOption)
}
