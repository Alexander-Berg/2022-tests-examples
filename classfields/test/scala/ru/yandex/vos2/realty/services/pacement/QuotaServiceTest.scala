package ru.yandex.vos2.realty.services.pacement

import org.junit.runner.RunWith
import org.scalamock.scalatest.MockFactory
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.realty.tracing.Traced
import ru.yandex.vertis.vos2.model.realty.{OfferType, PlacementStatus, RealtyOfferShowStatusV2}
import ru.yandex.vos2.OfferID
import ru.yandex.vos2.dao.quota.{QuotaDao, QuotaRecord}
import ru.yandex.vos2.model.{OfferRef, UserRef, UserRefUID}
import ru.yandex.vos2.realty.dao.offers.RealtyOfferDao
import ru.yandex.vos2.realty.model.QuotaStatisticItem

@RunWith(classOf[JUnitRunner])
class QuotaServiceTest extends WordSpec with Matchers with MockFactory {

  private val quotaDao = mock[QuotaDao]
  private val regionUtils = mock[RegionUtils]
  private val offerDao = mock[RealtyOfferDao]
  private val quotaService = new QuotaService(quotaDao, offerDao, regionUtils)
  implicit val emptyTraced: Traced = Traced.empty

  val MskGeoId = 1
  val SpbGeoId = 10174
  val MskRgid = 741964L
  "QuotaServiceTest" should {

    "checkAvailableQuota returns available where there is no quota records" in {
      val testUserRef = UserRefUID(MskGeoId)

      (quotaDao.findQuotasInsideWindow(_: UserRef, _: Int)).expects(testUserRef, *).returning(Seq.empty).twice()

      quotaService.checkAvailableQuota(testUserRef, sfGeoId = MskGeoId, None) shouldBe AvailableQuotaInRegion(2)
      quotaService.checkAvailableQuota(testUserRef, sfGeoId = SpbGeoId, None) shouldBe AvailableQuotaInRegion(1)

    }

    "checkAvailableQuota returns HasAppliedQuota where there is active quota record in region" in {
      val testUserRef = UserRefUID(2)
      val testOfferId = "880035553535"
      val offerRef = OfferRef.ref(testUserRef, testOfferId)

      val quotaRecord1: QuotaRecord = QuotaRecord(offerRef, MskRgid, 0)
      val existingQuotas = Seq(quotaRecord1)

      (quotaDao
        .findQuotasInsideWindow(_: UserRef, _: Int))
        .expects(testUserRef, *)
        .returning(existingQuotas)
        .twice()

      (regionUtils.isRgidInGeo _).expects(MskRgid, MskGeoId).returning(true).anyNumberOfTimes()
      (regionUtils.isRgidInGeo _).expects(MskRgid, SpbGeoId).returning(false).anyNumberOfTimes()

      quotaService.checkAvailableQuota(testUserRef, sfGeoId = MskGeoId, Some(testOfferId)) shouldBe
        HasAppliedQuota(quotaRecord1)
      quotaService.checkAvailableQuota(testUserRef, sfGeoId = SpbGeoId, Some(testOfferId)) shouldBe
        AvailableQuotaInRegion(1)
    }

    "checkAvailableQuota returns LimitExceed where there is no available quota records" in {
      val testUserRef = UserRefUID(2)
      val testOfferId = "880035553535"
      val testOfferId2 = "222222222222"
      val testOfferId3 = "333333333333"
      val offerRef = OfferRef.ref(testUserRef, testOfferId)

      val quotaRecord1: QuotaRecord = QuotaRecord(offerRef, MskRgid, 0)
      val quotaRecord2: QuotaRecord = QuotaRecord(OfferRef.ref(testUserRef, testOfferId2), MskRgid, 0)

      val existingQuotas = Seq(quotaRecord1, quotaRecord2)

      (quotaDao
        .findQuotasInsideWindow(_: UserRef, _: Int))
        .expects(testUserRef, *)
        .returning(existingQuotas)
        .twice()

      (regionUtils.isRgidInGeo _).expects(MskRgid, MskGeoId).returning(true).anyNumberOfTimes()
      (regionUtils.isRgidInGeo _).expects(MskRgid, SpbGeoId).returning(false).anyNumberOfTimes()

      quotaService.checkAvailableQuota(testUserRef, sfGeoId = MskGeoId, Some(testOfferId)) shouldBe
        HasAppliedQuota(quotaRecord1)
      quotaService.checkAvailableQuota(testUserRef, sfGeoId = MskGeoId, Some(testOfferId3)) shouldBe
        QuotaLimitExceeded

    }

    "checkActiveOffers returns LimitExceed where there is no available quota records" in {
      val testUserRef = UserRefUID(2)
      val testOfferId = "880035553535"
      val testOfferId2 = "222222222222"
      val testOfferId3 = "333333333333"
      val offerRef = OfferRef.ref(testUserRef, testOfferId)

      val quotaRecord1: QuotaRecord = QuotaRecord(offerRef, MskRgid, 0)
      val quotaRecord2: QuotaRecord = QuotaRecord(OfferRef.ref(testUserRef, testOfferId2), MskRgid, 0)

      val statisticItems = Seq(
        QuotaStatisticItem(
          OfferType.SELL,
          RealtyOfferShowStatusV2.SS2_PUBLISHED,
          Some(MskRgid),
          1,
          None,
          Some(PlacementStatus.FREE)
        ),
        QuotaStatisticItem(
          OfferType.SELL,
          RealtyOfferShowStatusV2.SS2_PUBLISHED,
          Some(MskRgid),
          1,
          None,
          Some(PlacementStatus.HAS_QUOTA)
        )
      )

      (offerDao
        .getQuotaStatistics(_: UserRef, _: Option[OfferID])(_: Traced))
        .expects(*, *, *)
        .returning(
          statisticItems
        )
        .twice()

      (regionUtils.isRgidInGeo _).expects(MskRgid, MskGeoId).returning(true).anyNumberOfTimes()
      (regionUtils.isRgidInGeo _).expects(MskRgid, SpbGeoId).returning(false).anyNumberOfTimes()

      quotaService.checkActiveOffers(testUserRef, sfGeoId = MskGeoId, Some(testOfferId)) shouldBe
        ActiveFreeLimitExceeded
      quotaService.checkActiveOffers(testUserRef, sfGeoId = SpbGeoId, Some(testOfferId3)) shouldBe
        ActiveFreeInLimit

    }

  }
}
