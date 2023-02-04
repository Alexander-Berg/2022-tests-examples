package ru.yandex.realty.giraffic.backend.preset

import java.util.Collections

import eu.timepit.refined.numeric.Positive
import eu.timepit.refined.refineMV
import org.junit.runner.RunWith
import org.mockito.invocation.InvocationOnMock
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.traffic.TestData
import ru.yandex.realty.model.billing.Campaign
import ru.yandex.realty.model.location.Location
import ru.yandex.realty.model.region.NodeRgid
import ru.yandex.realty.model.sites.Site
import ru.yandex.realty.proto.search.PresetLinkIdentity
import ru.yandex.realty.proto.village.Village
import ru.yandex.realty.sites.campaign.CampaignStorage
import ru.yandex.realty.sites.{ExtendedSiteStatisticsStorage, SitesGroupingService}
import ru.yandex.realty.util.maps._
import ru.yandex.realty.villages.VillagesStorage
import ru.yandex.vertis.mockito.MockitoSupport

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class PresetStatisticStorageBuilderSpec extends WordSpec with Matchers with MockitoSupport {

  import PresetStatisticStorageBuilderSpec._

  "PresetStatisticStorageBuilder" should {
    "build empty stat when no actual sites" in {
      buildStorage(
        makeSitesGroupingService(moscowSite(1), moscowSite(2)),
        makeVillagesStorageProvider(),
        withActualSites(),
        stubExtractor,
        stubExtractor
      ).index shouldBe empty
    }

    "build non empty stat when single actual site" in {
      buildStorage(
        makeSitesGroupingService(moscowSite(1), moscowSite(2)),
        makeVillagesStorageProvider(),
        withActualSites(moscowSite(1)),
        stubExtractor,
        stubExtractor
      ).shouldEqualTo(
        Map(
          587795L -> Map(PresetLinkIdentity.APARTMENT_RENT_ALL -> PresetStatisticItem(refineMV[Positive](1), 100f)),
          741964L -> Map(PresetLinkIdentity.APARTMENT_RENT_ALL -> PresetStatisticItem(refineMV[Positive](1), 100f)),
          143L -> Map(PresetLinkIdentity.APARTMENT_RENT_ALL -> PresetStatisticItem(refineMV[Positive](1), 100f)),
          0L -> Map(PresetLinkIdentity.APARTMENT_RENT_ALL -> PresetStatisticItem(refineMV[Positive](1), 100f))
        )
      )
    }

    "build non empty stat when all actual sites" in {
      buildStorage(
        makeSitesGroupingService(moscowSite(1), moscowSite(2)),
        makeVillagesStorageProvider(),
        withActualSites(moscowSite(1), moscowSite(2)),
        stubExtractor,
        stubExtractor
      ).shouldEqualTo(
        Map(
          587795L -> Map(PresetLinkIdentity.APARTMENT_RENT_ALL -> PresetStatisticItem(refineMV[Positive](2), 100f)),
          741964L -> Map(PresetLinkIdentity.APARTMENT_RENT_ALL -> PresetStatisticItem(refineMV[Positive](2), 100f)),
          143L -> Map(PresetLinkIdentity.APARTMENT_RENT_ALL -> PresetStatisticItem(refineMV[Positive](2), 100f)),
          0L -> Map(PresetLinkIdentity.APARTMENT_RENT_ALL -> PresetStatisticItem(refineMV[Positive](2), 100f))
        )
      )
    }

    "correctly merge stat" in {
      val s1 = moscowSite(1)
      val s2 = makeSite(2, NodeRgid.SPB)

      val extractor = mock[PresetStatisticItemsMapExtractor[Site]]

      when(extractor.extractItemsMap(?)).thenAnswer { (invoke: InvocationOnMock) =>
        val site = invoke.getArgument[Site](0)

        if (site.getId == 1)
          Map(
            PresetLinkIdentity.APARTMENT_SELL_ALL -> PresetStatisticItem(refineMV[Positive](1), 100),
            PresetLinkIdentity.APARTMENT_RENT_ALL -> PresetStatisticItem(refineMV[Positive](1), 100)
          )
        else if (site.getId == 2)
          Map(
            PresetLinkIdentity.APARTMENT_SELL_ALL -> PresetStatisticItem(refineMV[Positive](1), 95),
            PresetLinkIdentity.APARTMENT_RENT_AGENTS_NO -> PresetStatisticItem(refineMV[Positive](1), 95)
          )
        else Map.empty
      }

      buildStorage(
        makeSitesGroupingService(s1, s2),
        makeVillagesStorageProvider(),
        withActualSites(s1, s2),
        extractor,
        stubExtractor
      ).shouldEqualTo(
        Map(
          587795L -> Map(
            PresetLinkIdentity.APARTMENT_SELL_ALL -> PresetStatisticItem(refineMV[Positive](1), 100),
            PresetLinkIdentity.APARTMENT_RENT_ALL -> PresetStatisticItem(refineMV[Positive](1), 100)
          ),
          741964L -> Map(
            PresetLinkIdentity.APARTMENT_SELL_ALL -> PresetStatisticItem(refineMV[Positive](1), 100),
            PresetLinkIdentity.APARTMENT_RENT_ALL -> PresetStatisticItem(refineMV[Positive](1), 100)
          ),
          NodeRgid.SPB -> Map(
            PresetLinkIdentity.APARTMENT_SELL_ALL -> PresetStatisticItem(refineMV[Positive](1), 95),
            PresetLinkIdentity.APARTMENT_RENT_AGENTS_NO -> PresetStatisticItem(refineMV[Positive](1), 95)
          ),
          NodeRgid.SPB_AND_LEN_OBLAST -> Map(
            PresetLinkIdentity.APARTMENT_SELL_ALL -> PresetStatisticItem(refineMV[Positive](1), 95),
            PresetLinkIdentity.APARTMENT_RENT_AGENTS_NO -> PresetStatisticItem(refineMV[Positive](1), 95)
          ),
          143L -> Map(
            PresetLinkIdentity.APARTMENT_SELL_ALL -> PresetStatisticItem(refineMV[Positive](2), 95),
            PresetLinkIdentity.APARTMENT_RENT_ALL -> PresetStatisticItem(refineMV[Positive](1), 100),
            PresetLinkIdentity.APARTMENT_RENT_AGENTS_NO -> PresetStatisticItem(refineMV[Positive](1), 95)
          ),
          0L -> Map(
            PresetLinkIdentity.APARTMENT_SELL_ALL -> PresetStatisticItem(refineMV[Positive](2), 95),
            PresetLinkIdentity.APARTMENT_RENT_ALL -> PresetStatisticItem(refineMV[Positive](1), 100),
            PresetLinkIdentity.APARTMENT_RENT_AGENTS_NO -> PresetStatisticItem(refineMV[Positive](1), 95)
          )
        )
      )
    }

  }
}

object PresetStatisticStorageBuilderSpec extends Matchers with MockitoSupport {
  implicit class StorageOps(val st: PresetStatisticStorage) extends AnyVal {

    def shouldEqualTo(map: Map[Long, Map[PresetLinkIdentity, PresetStatisticItem]]): Unit = {

      st.index
        .fullOuterJoin(map)
        .foreach {
          case (rgid, (actual, expected)) =>
            withClue(s"on rgid $rgid") {
              actual should not be empty
              expected should not be empty

              actual.get
                .fullOuterJoin(expected.get)
                .foreach {
                  case (link, (actualStat, expectedStat)) =>
                    withClue(s"on link $link") {
                      expectedStat should not be empty
                      actualStat should not be empty

                      actualStat.get.count.value shouldBe expectedStat.get.count.value
                      actualStat.get.minPrice shouldBe expectedStat.get.minPrice
                    }
                }
            }
        }
    }
  }

  private def makeSite(id: Long, rgid: Long): Site = {
    val res = new Site(id)
    val loc = new Location
    loc.setRegionGraphId(rgid)
    res.setLocation(loc)
    res
  }

  private def moscowSite(id: Long): Site =
    makeSite(id, NodeRgid.MOSCOW)

  private def makeSitesGroupingService(sites: Site*): SitesGroupingService = {
    val res = mock[SitesGroupingService]
    when(res.getAllSites).thenReturn(sites.asJavaCollection)

    res
  }

  private def makeVillagesStorageProvider(villages: Village*): Provider[VillagesStorage] = {
    val res = mock[VillagesStorage]
    when(res.getAll).thenReturn(villages.toSeq)

    () => res
  }

  private def withActualSites(sites: Site*): (Provider[CampaignStorage], Provider[ExtendedSiteStatisticsStorage]) = {
    val ext = mock[ExtendedSiteStatisticsStorage]
    when(ext.get(?)).thenReturn(null) // sale closed

    val campaign = mock[CampaignStorage]
    when(campaign.getById(?)).thenAnswer { (invocation: InvocationOnMock) =>
      {
        val id = invocation.getArgument[Long](0)

        if (sites.exists(_.getId == id)) Collections.singletonList[Campaign](mock[Campaign])
        else Collections.emptyList[Campaign]()
      }

    } // promoted

    (() => campaign, () => ext)
  }

  private def buildStorage(
    sitesGroupingService: SitesGroupingService,
    villagesStorage: Provider[VillagesStorage],
    campaignStWithExtSt: (Provider[CampaignStorage], Provider[ExtendedSiteStatisticsStorage]),
    siteMapExtractor: PresetStatisticItemsMapExtractor[Site],
    villageMapExtractor: PresetStatisticItemsMapExtractor[Village]
  ) =
    new PresetStatisticStorageBuilder(
      sitesGroupingService,
      campaignStWithExtSt._1,
      () => TestData.regionGraph,
      campaignStWithExtSt._2,
      villagesStorage,
      siteMapExtractor,
      villageMapExtractor
    ).build().get

  private def stubExtractor[A]: PresetStatisticItemsMapExtractor[A] = {
    val res = mock[PresetStatisticItemsMapExtractor[A]]

    when(res.extractItemsMap(?)).thenReturn(
      Map(PresetLinkIdentity.APARTMENT_RENT_ALL -> PresetStatisticItem(refineMV[Positive](1), 100f))
    )
    res
  }

}
