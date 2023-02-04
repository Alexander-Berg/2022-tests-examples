package ru.yandex.realty.unification.unifier.processor.services

import org.junit.runner.RunWith
import org.scalatest.OneInstancePerTest
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.SpecBase
import ru.yandex.realty.model.gen.OfferModelGenerators
import ru.yandex.realty.model.offer.{FlatType, PaymentType, VasUnavailableReason}
import ru.yandex.realty.model.sites.AuctionWinnersStorage
import ru.yandex.realty.sites.CampaignService
import ru.yandex.realty.storage.TargetCallRegionsStorage
import ru.yandex.realty.tracing.Traced

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class VasAvailabilityCheckerSpec extends SpecBase with OneInstancePerTest with OfferModelGenerators {

  private val campaignService = mock[CampaignService]
  private val targetCallRegionsProvider = mock[Provider[TargetCallRegionsStorage]]
  private val auctionWinnersProvider = mock[Provider[AuctionWinnersStorage]]

  private val vasAvailabilityChecker =
    new VasAvailabilityChecker(campaignService, targetCallRegionsProvider, auctionWinnersProvider)

  implicit private val traced = Traced.empty

  "IsVasAvailableEnricher" should {
    "do not allow VAS when offer's region has campaigns but offer's site does not" in {
      val offer = offerGen(
        paymentType = Some(PaymentType.JURIDICAL_PERSON),
        imageCount = 1,
        withBuildingInfo = true,
        isPrimarySale = Some(true)
      ).next

      val targetCallRegionsStorage = createTargetCallRegionsStorage(Set(offer.getLocation.getSubjectFederationId))
      (targetCallRegionsProvider.get _).expects().returning(targetCallRegionsStorage)
      (campaignService.hasNewbuldingCampaign _).expects(*).returning(false)

      vasAvailabilityChecker.checkVasAvailability(offer)

      offer.getVasUnavailableReasons.contains(VasUnavailableReason.PRIMARY_SALE_NO_CAMPAIGN) shouldEqual (true)
    }

    "do not allow VAS when site has campaigns but offer is not from winner" in {
      val offer = offerGen(
        paymentType = Some(PaymentType.JURIDICAL_PERSON),
        imageCount = 1,
        withBuildingInfo = true,
        isPrimarySale = Some(false),
        companyIds = Seq(10001),
        siteId = Some(35),
        flatType = Some(FlatType.NEW_FLAT)
      ).next

      val auctionWinnersStorage =
        createAuctionWinnersStorage(siteIdToCompanyId = Map(offer.getBuildingInfo.getSiteId.longValue() -> 10002L))
      (auctionWinnersProvider.get _).expects().returning(auctionWinnersStorage)

      (campaignService.hasNewbuldingCampaign _).expects(*).returning(true)

      vasAvailabilityChecker.checkVasAvailability(offer)

      offer.getVasUnavailableReasons.contains(VasUnavailableReason.CAMPAIGN_NOT_WINNER) shouldEqual (true)
    }

    "do allow VAS when site does not have campaign in region without campaigns" in {
      val offer = offerGen(
        paymentType = Some(PaymentType.JURIDICAL_PERSON),
        imageCount = 1,
        withBuildingInfo = true,
        isPrimarySale = Some(true)
      ).next

      val targetCallRegionsStorage = createTargetCallRegionsStorage(Set.empty)
      (targetCallRegionsProvider.get _).expects().returning(targetCallRegionsStorage)
      (campaignService.hasNewbuldingCampaign _).expects(*).returning(false)

      vasAvailabilityChecker.checkVasAvailability(offer)

      offer.getVasUnavailableReasons.isEmpty shouldEqual (true)
    }

    "do allow VAS when site has campaigns and offer is from winner" in {
      val offer = offerGen(
        paymentType = Some(PaymentType.JURIDICAL_PERSON),
        imageCount = 1,
        withBuildingInfo = true,
        isPrimarySale = Some(false),
        companyIds = Seq(10001),
        siteId = Some(35),
        flatType = Some(FlatType.NEW_FLAT)
      ).next

      val auctionWinnersStorage =
        createAuctionWinnersStorage(
          siteIdToCompanyId = Map(offer.getBuildingInfo.getSiteId.longValue() -> offer.getCompanyIds.asScala.head)
        )
      (auctionWinnersProvider.get _).expects().returning(auctionWinnersStorage)

      (campaignService.hasNewbuldingCampaign _).expects(*).returning(true)

      vasAvailabilityChecker.checkVasAvailability(offer)

      offer.getVasUnavailableReasons.isEmpty shouldBe (true)
    }
  }

  private def createTargetCallRegionsStorage(sitesRegionIds: Set[Int] = Set.empty) = {
    new TargetCallRegionsStorage(Set.empty, sitesRegionIds)
  }

  private def createAuctionWinnersStorage(
    siteIdToCompanyId: Map[Long, Long] = Map.empty,
    siteIdToRevenue: Map[Long, Long] = Map.empty,
    siteIdToTopBid: Map[Long, Long] = Map.empty
  ) = {
    new AuctionWinnersStorage(
      asJavaLongMap(siteIdToCompanyId),
      asJavaLongMap(siteIdToRevenue),
      asJavaLongMap(siteIdToTopBid)
    )
  }

  private def asJavaLongMap(map: Map[Long, Long]): java.util.Map[java.lang.Long, java.lang.Long] =
    map.map {
      case (key, value) => (Long.box(key), Long.box(value))
    }.asJava

}
