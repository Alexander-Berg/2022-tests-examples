package ru.yandex.vertis.statistics.transformer.realty.tasks.site

import org.joda.time.Instant
import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.features.FeaturesStubComponent
import ru.yandex.realty.model.billing.Campaign
import ru.yandex.realty.model.location.Location
import ru.yandex.realty.model.offer.Rooms
import ru.yandex.realty.model.sites.range.RangeImpl
import ru.yandex.realty.model.sites.{
  BuildingClass,
  ExtendedSiteStatistics,
  ExtendedSiteStatisticsAtom,
  SimpleSiteStatisticsResult,
  Site
}
import ru.yandex.realty.sites.campaign.CampaignStorage
import ru.yandex.realty.sites.{BidService, ExtendedSiteStatisticsStorage, SiteCompanyBid, SitesGroupingService}
import ru.yandex.realty.tracing.Traced
import ru.yandex.vertis.statistics.core.dao.site.{SiteCustomerStats, SiteCustomerStatsDao, SiteCustomerStatsKey}

import java.time.LocalDate
import java.util.Collections
import java.{lang, util}
import scala.collection.JavaConverters._
import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class SiteCustomerStatsSchedulerTaskSpec extends AsyncSpecBase with FeaturesStubComponent {

  trait SiteCustomerStatsSchedulerTaskFixture {
    features.SiteStatsSendingEnabled.setNewState(true)

    val sitesGroupingService: SitesGroupingService = mock[SitesGroupingService]
    val statisticsMap = new util.HashMap[lang.Long, ExtendedSiteStatistics]()
    val siteStatistics = new ExtendedSiteStatisticsStorage(statisticsMap)

    val siteId: Long = Gen.choose(1, Long.MaxValue).sample.get

    val campaignId1: String = Gen.choose(1, Long.MaxValue).sample.get.toString
    val companyId1: Long = Gen.choose(1, Long.MaxValue).sample.get
    val agencyId1: Long = Gen.choose(1, Long.MaxValue).sample.get
    val clientId1: Long = Gen.choose(1, Long.MaxValue).sample.get

    val campaignId2: String = Gen.choose(1, Long.MaxValue).sample.get.toString
    val companyId2: Long = Gen.choose(1, Long.MaxValue).sample.get
    val agencyId2: Long = Gen.choose(1, Long.MaxValue).sample.get
    val clientId2: Long = Gen.choose(1, Long.MaxValue).sample.get

    val campaignStorage = new CampaignStorage(
      Seq(
        buildCampaign(siteId, campaignId1, companyId1, agencyId1, clientId1),
        buildCampaign(siteId, campaignId2, companyId2, agencyId2, clientId2)
      ).asJava
    )
    val siteBidService: BidService = mock[BidService]
    val siteStatsDao: SiteCustomerStatsDao = mock[SiteCustomerStatsDao]

    val task = new SiteCustomerStatsSchedulerTask(
      sitesGroupingService,
      () => siteStatistics,
      () => campaignStorage,
      siteBidService,
      siteStatsDao
    )

    implicit val traced = Traced.empty
  }

  "SiteCustomerStatsSchedulerTask" should {
    "send stats for no sites" in new SiteCustomerStatsSchedulerTaskFixture {
      (sitesGroupingService.getAllSites _: () => java.util.Collection[Site])
        .expects()
        .returns(Collections.emptyList())

      (siteBidService
        .getSiteCompanyBidsAsync(_: Map[Long, Seq[Long]])(_: Traced))
        .expects(Map.empty[Long, Seq[Long]], *)
        .returns(Future.successful(Seq.empty))
        .never()

      task.run().futureValue
    }

    "send stats for all sites" in new SiteCustomerStatsSchedulerTaskFixture {
      val subjectFederationId = 419
      val buildingClass = BuildingClass.COMFORT
      val sites: util.ArrayList[Site] = initSites(siteId, subjectFederationId, buildingClass)
      (sitesGroupingService.getAllSites _: () => util.Collection[Site])
        .expects()
        .returns(sites)

      val statistics: ExtendedSiteStatistics = initStatistics(companyId2, companyId1)
      statisticsMap.put(siteId, statistics)

      val params: Map[Long, Seq[Long]] = Map(siteId -> Seq(companyId1, companyId2))
      (siteBidService
        .getSiteCompanyBidsAsync(_: Map[Long, Seq[Long]])(_: Traced))
        .expects(params, *)
        .returns(
          Future.successful(
            Seq(
              SiteCompanyBid(siteId, companyId2, 1L, None, 1L, None, Some(2L), None),
              SiteCompanyBid(siteId, companyId1, 1L, None, 1L, None, Some(3L), None)
            )
          )
        )

      val testDate = LocalDate.now()
      (siteStatsDao
        .get(_: Seq[SiteCustomerStatsKey])(_: Traced))
        .expects(where { (keys: Seq[SiteCustomerStatsKey], _) =>
          keys.size == 2 &&
          keys.contains(SiteCustomerStatsKey(siteId, clientId1, Some(agencyId1), testDate)) &&
          keys.contains(SiteCustomerStatsKey(siteId, clientId2, Some(agencyId2), testDate))
        })
        .returning(Future.successful(Seq.empty))
      (siteStatsDao
        .upsert(_: Seq[SiteCustomerStats])(_: Traced))
        .expects(where { (stats: Seq[SiteCustomerStats], _) =>
          val fixedStats = stats.map(_.copy(id = None, date = testDate))
          stats.size == 2 &&
          fixedStats
            .contains(SiteCustomerStats(None, testDate, siteId, clientId1, Some(agencyId1), Some(4), Some(3L))) &&
          fixedStats.contains(SiteCustomerStats(None, testDate, siteId, clientId2, Some(agencyId2), Some(2), Some(2L)))
        })
        .returning(Future.unit)

      task.run().futureValue
    }

    "update stats for some sites" in new SiteCustomerStatsSchedulerTaskFixture {
      val subjectFederationId = 419
      val buildingClass = BuildingClass.COMFORT
      val sites: util.ArrayList[Site] = initSites(siteId, subjectFederationId, buildingClass)
      (sitesGroupingService.getAllSites _: () => util.Collection[Site])
        .expects()
        .returns(sites)

      val statistics: ExtendedSiteStatistics = initStatistics(companyId2, companyId1)
      statisticsMap.put(siteId, statistics)

      val params: Map[Long, Seq[Long]] = Map(siteId -> Seq(companyId1, companyId2))
      (siteBidService
        .getSiteCompanyBidsAsync(_: Map[Long, Seq[Long]])(_: Traced))
        .expects(params, *)
        .returns(
          Future.successful(
            Seq(
              SiteCompanyBid(siteId, companyId2, 1L, None, 1L, None, Some(2L), None),
              SiteCompanyBid(siteId, companyId1, 1L, None, 1L, None, Some(3L), None)
            )
          )
        )

      val testDate = LocalDate.now()
      (siteStatsDao
        .get(_: Seq[SiteCustomerStatsKey])(_: Traced))
        .expects(where { (keys: Seq[SiteCustomerStatsKey], _) =>
          keys.size == 2 &&
          keys.contains(SiteCustomerStatsKey(siteId, clientId1, Some(agencyId1), testDate)) &&
          keys.contains(SiteCustomerStatsKey(siteId, clientId2, Some(agencyId2), testDate))
        })
        .returning(
          Future
            .successful(
              Seq(
                SiteCustomerStats(None, testDate, siteId, clientId1, Some(agencyId1), Some(4), Some(3L)),
                SiteCustomerStats(None, testDate, siteId, clientId2, Some(agencyId2), Some(1), Some(2L))
              )
            )
        )
      (siteStatsDao
        .upsert(_: Seq[SiteCustomerStats])(_: Traced))
        .expects(where { (stats: Seq[SiteCustomerStats], _) =>
          val fixedStats = stats.map(_.copy(id = None, date = testDate))
          stats.size == 1 &&
          fixedStats.contains(SiteCustomerStats(None, testDate, siteId, clientId2, Some(agencyId2), Some(2), Some(2L)))
        })
        .returning(Future.unit)

      task.run().futureValue
    }

  }

  private def buildCampaign(siteId: Long, campaignId: String, companyId: Long, agencyId: Long, clientId: Long) = {
    new Campaign(
      campaignId,
      siteId,
      companyId,
      "phone",
      Collections.emptyMap(),
      Collections.emptyList(),
      Collections.emptyList(),
      1L,
      1L,
      true,
      false,
      clientId,
      agencyId,
      Instant.now(),
      Collections.emptyMap(),
      null,
      null
    )
  }

  private def initSites(
    siteId: Long,
    subjectFederationId: Int,
    buildingClass: BuildingClass
  ): java.util.ArrayList[Site] = {
    val location = new Location()
    location.setSubjectFederation(subjectFederationId, 1L)

    val site = new Site(siteId)
    site.setBuildingClass(buildingClass)
    site.setLocation(location)

    val sites = new util.ArrayList[Site]()
    sites.add(site)
    sites
  }

  private def initStatistics(companyId2: Long, companyId3: Long) = {
    val companyStatsMap = new util.HashMap[lang.Long, ExtendedSiteStatisticsAtom]()
    val simpleSiteStatisticsResult2 = new SimpleSiteStatisticsResult(
      RangeImpl.create(null, null),
      RangeImpl.create(null, null),
      RangeImpl.create(null, null),
      1,
      1,
      1,
      1.0f
    )
    val extendedSiteStatisticsAtom2 = new ExtendedSiteStatisticsAtom(
      simpleSiteStatisticsResult2,
      new util.HashMap[Rooms, SimpleSiteStatisticsResult]()
    )
    val simpleSiteStatisticsResult3 = new SimpleSiteStatisticsResult(
      RangeImpl.create(null, null),
      RangeImpl.create(null, null),
      RangeImpl.create(null, null),
      2,
      2,
      2,
      2.0f
    )
    val extendedSiteStatisticsAtom3 = new ExtendedSiteStatisticsAtom(
      simpleSiteStatisticsResult3,
      new util.HashMap[Rooms, SimpleSiteStatisticsResult]()
    )
    companyStatsMap.put(companyId2, extendedSiteStatisticsAtom2)
    companyStatsMap.put(companyId3, extendedSiteStatisticsAtom3)
    val statistics = new ExtendedSiteStatistics(
      ExtendedSiteStatisticsAtom.EMPTY,
      ExtendedSiteStatisticsAtom.EMPTY,
      companyStatsMap,
      1,
      ExtendedSiteStatisticsAtom.EMPTY,
      ExtendedSiteStatisticsAtom.EMPTY,
      Collections.emptyList(),
      Collections.emptySet(),
      false,
      0,
      ExtendedSiteStatisticsAtom.EMPTY,
      ExtendedSiteStatisticsAtom.EMPTY
    )
    statistics
  }

}
