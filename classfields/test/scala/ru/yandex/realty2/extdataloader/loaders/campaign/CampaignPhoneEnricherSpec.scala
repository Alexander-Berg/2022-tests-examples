package ru.yandex.realty2.extdataloader.loaders.campaign

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalamock.scalatest.MockFactory
import org.scalatest.OneInstancePerTest
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.common.util.IOUtils
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.context.ProviderAdapter
import ru.yandex.realty.context.v2.AuctionResultStorage
import ru.yandex.realty.features.FeaturesStubComponent
import ru.yandex.realty.graph.RegionGraph
import ru.yandex.realty.graph.serialize.RegionGraphProtoConverter
import ru.yandex.realty.model.billing.Campaign
import ru.yandex.realty.model.location.Location
import ru.yandex.realty.model.phone.RealtyPhoneTags._
import ru.yandex.realty.model.phone.{PhoneRedirect, PhoneType}
import ru.yandex.realty.model.region.Regions
import ru.yandex.realty.model.sites.{Company, Site}
import ru.yandex.realty.phone.RedirectPhoneService.Tag
import ru.yandex.realty.phone.{PersonalRedirectService, PhoneGenerators, RedirectPhoneServiceTestComponents}
import ru.yandex.realty.proto.phone.PhoneRedirectStrategyAlgorithmType
import ru.yandex.realty.sites.campaign.CampaignStorage
import ru.yandex.realty.sites.{CompaniesStorage, SitesGroupingService}
import ru.yandex.realty2.extdataloader.loaders.sites.maps.SiteMapsPromotionStorage
import ru.yandex.realty2.extdataloader.loaders.sites.promo.SitePromotionStorage
import ru.yandex.vertis.ops.test.TestOperationalSupport

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.util.Success

@RunWith(classOf[JUnitRunner])
class CampaignPhoneEnricherSpec
  extends AsyncSpecBase
  with MockFactory
  with OneInstancePerTest
  with PhoneGenerators
  with RedirectPhoneServiceTestComponents
  with FeaturesStubComponent
  with CampaignGenerators {

  val siteMapsPromotionStorage: SiteMapsPromotionStorage = SiteMapsPromotionStorage(Set(42L))
  val siteMapsPromotionProvider: Provider[SiteMapsPromotionStorage] = () => siteMapsPromotionStorage

  val sitePromotionStorage: SitePromotionStorage = SitePromotionStorage(Set(101L))
  val sitePromotionProvider: Provider[SitePromotionStorage] = () => sitePromotionStorage

  val campaignStorage: mutable.ArrayBuffer[Campaign] = new mutable.ArrayBuffer[Campaign]
  val campaignsProvider: Provider[CampaignStorage] = () => new CampaignStorage(campaignStorage.asJava)

  val companiesProvider: Provider[CompaniesStorage] = () => new CompaniesStorage(Seq[Company]().asJava)
  val auctionResultProvider: Provider[AuctionResultStorage] = () => new AuctionResultStorage(Seq())

  val regionGraph: RegionGraph = RegionGraphProtoConverter.deserialize(
    IOUtils.gunzip(
      getClass.getClassLoader.getResourceAsStream("region_graph-8-2.data")
    )
  )
  private val regionGraphProvider = ProviderAdapter.create(regionGraph)
  private val sitesServiceMock = mock[SitesGroupingService]

  val personalRedirectService = new PersonalRedirectService(
    redirectPhoneService,
    regionGraphProvider,
    companiesProvider,
    auctionResultProvider,
    features
  )(TestOperationalSupport)

  val stableCampaignPhoneEnricher: CampaignPhoneEnricher =
    new CampaignPhoneEnricher(
      siteMapsPromotionProvider,
      sitePromotionProvider,
      redirectPhoneService,
      campaignsProvider,
      sitesServiceMock,
      regionGraphProvider,
      personalRedirectService
    )

  private def getSite(id: Long) = {
    val s = new Site(id)
    s.setName(id.toString)
    val l = new Location
    l.setGeocoderId(Regions.MOSCOW)
    s.setLocation(l)
    s
  }

  val dummyDate = DateTime.parse("2000-01-01T00:00:00")
  val dummyDate2 = dummyDate.plusDays(2)

  val emptyTag: Option[String] = None

  val emptyTagPhoneRedirect: PhoneRedirect = PhoneRedirect(
    domain = "test",
    id = "dummyId0",
    objectId = "dummyObjectId0",
    tag = None,
    createTime = dummyDate,
    deadline = Some(dummyDate2),
    source = "3222323232",
    target = "00000000000",
    phoneType = None,
    geoId = None,
    ttl = None
  )

  val mapsTagPhoneRedirect: PhoneRedirect = PhoneRedirect(
    domain = "test",
    id = "dummyId345",
    objectId = "dummyObjectId124",
    tag = Some(MapsTagName),
    createTime = dummyDate,
    deadline = Some(dummyDate2),
    source = "0785678915",
    target = "00000000000",
    phoneType = None,
    geoId = None,
    ttl = None
  )

  val appNaviPhoneRedirect: PhoneRedirect = PhoneRedirect(
    domain = "test",
    id = "dummyId0896",
    objectId = "dummyObjectId2457",
    tag = Some(AppNaviTagName),
    createTime = dummyDate,
    deadline = Some(dummyDate2),
    source = "67893456789",
    target = "00000000000",
    phoneType = None,
    geoId = None,
    ttl = None
  )

  val mapsMobilePhoneRedirect: PhoneRedirect = PhoneRedirect(
    domain = "test",
    id = "dummyId0896",
    objectId = "dummyObjectId5943",
    tag = Some(MapsMobileTagName),
    createTime = dummyDate,
    deadline = Some(dummyDate2),
    source = "32393456770",
    target = "00000000000",
    phoneType = Some(PhoneType.Mobile),
    geoId = None,
    ttl = None
  )

  val serpPhoneRedirect: PhoneRedirect = PhoneRedirect(
    domain = "test",
    id = "dummyId0896",
    objectId = "dummyObjectId5943",
    tag = Some(SerpTagName),
    createTime = dummyDate,
    deadline = Some(dummyDate2),
    source = "899829924",
    target = "00000000000",
    phoneType = Some(PhoneType.Mobile),
    geoId = None,
    ttl = None
  )

  val promotionPhoneRedirect: PhoneRedirect = PhoneRedirect(
    domain = "test",
    id = "dummyId0897",
    objectId = "dummyObjectId5944",
    tag = Some(PromoTagName),
    createTime = dummyDate,
    deadline = Some(dummyDate2),
    source = "32393456771",
    target = "00000000001",
    phoneType = Some(PhoneType.Local),
    geoId = None,
    ttl = None
  )

  val platformDesktopCampaignPhoneRedirect: PhoneRedirect = PhoneRedirect(
    domain = "test",
    id = "mobileRedirectId002",
    objectId = "mobileRedirectId002",
    tag = Some(PlatformDesktopCampaignTagName),
    createTime = dummyDate,
    deadline = Some(dummyDate2),
    source = "01723991438",
    target = "00000000000",
    phoneType = Option(PhoneType.Local),
    geoId = None,
    ttl = None
  )

  "CampaignPhoneEnricher" when {
    "working with ordinary site" should {
      "enrich phone for common campaign" in {
        val oldEmptyTagRedirect: PhoneRedirect = phoneRedirectGen(
          tag = Tag.empty,
          phoneType = Some(PhoneType.Mobile),
          strategy = PhoneRedirectStrategyAlgorithmType.PRS_ONE_STEP
        ).next
        val oldMapsMobileTagRedirect: PhoneRedirect = phoneRedirectGen(
          tag = Tag(MapsMobileTagName),
          phoneType = Some(PhoneType.Mobile),
          strategy = PhoneRedirectStrategyAlgorithmType.PRS_ONE_STEP
        ).next

        val campaign: Campaign = campaignGen(redirects = Seq(oldEmptyTagRedirect, oldMapsMobileTagRedirect)).next
        campaignStorage += campaign

        val expectedTeleponyEmptyTagRedirect = phoneRedirectGen(
          targetPhone = campaign.getTargetPhone,
          tag = Tag.empty,
          phoneType = Some(PhoneType.Local),
          geoId = None
        ).next
        val expectedTeleponySerpRedirect = phoneRedirectGen(
          tag = Tag(SerpTagName),
          phoneType = Some(PhoneType.Local),
          strategy = PhoneRedirectStrategyAlgorithmType.PRS_ONE_STEP
        ).next
        val expectedTeleponyMapsMobileTagRedirect = oldMapsMobileTagRedirect

        expectTeleponyCall(expectedTeleponyEmptyTagRedirect)
        expectTeleponyCall(expectedTeleponySerpRedirect)
        expectTeleponyCall(expectedTeleponyMapsMobileTagRedirect)
        expectTeleponyDeleteCall(oldEmptyTagRedirect)

        (sitesServiceMock.getSiteById _).expects(1).anyNumberOfTimes().returning(getSite(1))

        val res = stableCampaignPhoneEnricher.enrichPhone(dummyCampaign).get

        res.getPhone(EmptyTagName).nonEmpty shouldBe true
        res.getPhone(SerpTagName).nonEmpty shouldBe true
        res.getPhone(MapsMobileTagName).nonEmpty shouldBe true
      }

      "not failed when some redirects were not created because of telepony error" in {
        expectTeleponyCall(tag = emptyTag, phoneType = Some(PhoneType.Local), result = Success(emptyTagPhoneRedirect))
        (sitesServiceMock.getSiteById _).expects(1).anyNumberOfTimes().returning(getSite(1))

        val res = stableCampaignPhoneEnricher.enrichPhone(dummyCampaign).get
        res.getPhone(EmptyTagName) shouldBe "3222323232"
      }

      "not produce campaign when redirect for empty tag was not created" in {
        (sitesServiceMock.getSiteById _).expects(1).anyNumberOfTimes().returning(getSite(1))

        val res = stableCampaignPhoneEnricher.enrichPhone(dummyCampaign)
        res.isEmpty shouldBe (true)
      }
    }

    "working with maps-extended site" should {
      "enrich phone with maps- and navi-specific tags " in {
        expectTeleponyCall(tag = emptyTag, phoneType = Some(PhoneType.Local), result = Success(emptyTagPhoneRedirect))
        expectTeleponyCall(tag = Some(MapsTagName), phoneType = None, result = Success(mapsTagPhoneRedirect))
        expectTeleponyCall(tag = Some(AppNaviTagName), phoneType = None, result = Success(appNaviPhoneRedirect))
        expectTeleponyCall(
          tag = Some(MapsMobileTagName),
          phoneType = Some(PhoneType.Mobile),
          result = Success(mapsMobilePhoneRedirect)
        )
        expectTeleponyCall(
          tag = Some(PlatformDesktopCampaignTagName),
          phoneType = Some(PhoneType.Local),
          result = Success(platformDesktopCampaignPhoneRedirect)
        )
        (sitesServiceMock.getSiteById _).expects(42).anyNumberOfTimes().returning(getSite(42))

        val res = stableCampaignPhoneEnricher.enrichPhone(mapsExtendedCampaign).get

        res.getPhone(EmptyTagName) shouldBe "3222323232"
        res.getPhone(MapsMobileTagName) shouldBe "32393456770"
        res.getPhone(MapsTagName) shouldBe "0785678915"
        res.getPhone(AppNaviTagName) shouldBe "67893456789"
        res.getPhone(PlatformDesktopCampaignTagName) shouldBe platformDesktopCampaignPhoneRedirect.source
      }
    }

    "working with promotion-extended site" should {
      "enrich phone with promo tag " in {
        expectTeleponyCall(tag = emptyTag, phoneType = Some(PhoneType.Local), result = Success(emptyTagPhoneRedirect))
        expectTeleponyCall(
          tag = Some(MapsMobileTagName),
          phoneType = Some(PhoneType.Mobile),
          result = Success(mapsMobilePhoneRedirect)
        )
        expectTeleponyCall(
          tag = Some(PromoTagName),
          phoneType = Some(PhoneType.Local),
          result = Success(promotionPhoneRedirect)
        )
        (sitesServiceMock.getSiteById _).expects(101).anyNumberOfTimes().returning(getSite(101))

        val res =
          stableCampaignPhoneEnricher.enrichPhone(promotionExtendedCampaign).get

        res.getPhone(EmptyTagName) shouldBe "3222323232"
        res.getPhone(MapsMobileTagName) shouldBe "32393456770"
        res.getPhone(PromoTagName) shouldBe "32393456771"
      }
    }
  }

}
