package ru.yandex.realty2.extdataloader.loaders.campaign

import org.junit.runner.RunWith
import org.scalatest.OneInstancePerTest
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.extdata.core.ServerController
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.application.RedirectPhoneComponents
import ru.yandex.realty.clients.billing.BillingInternalApiClient
import ru.yandex.realty.context.ProviderAdapter
import ru.yandex.realty.context.v2.AuctionResultStorage
import ru.yandex.realty.features.FeaturesStubComponent
import ru.yandex.realty.graph.RegionGraph
import ru.yandex.realty.model.billing.Campaign
import ru.yandex.realty.model.sites.Company
import ru.yandex.realty.phone.{PersonalRedirectService, RedirectPhoneService}
import ru.yandex.realty.sites.campaign.{CampaignParser, CampaignStorage}
import ru.yandex.realty.sites.{CompaniesStorage, SitesGroupingService}
import ru.yandex.realty.telepony.TeleponyClientMockComponents
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty2.extdataloader.loaders.sites.maps.SiteMapsPromotionStorage
import ru.yandex.realty2.extdataloader.loaders.sites.promo.SitePromotionStorage
import ru.yandex.vertis.billing.Model
import ru.yandex.vertis.ops.test.TestOperationalSupport

import scala.collection.JavaConverters._
import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class CampaignFetcherSpec
  extends AsyncSpecBase
  with OneInstancePerTest
  with CampaignGenerators
  with FeaturesStubComponent
  with TeleponyClientMockComponents {

  private val controller = mock[ServerController]
  (controller.register _).expects(*).anyNumberOfTimes()

  private val billingCampaignClient = mock[BillingInternalApiClient]
  private val regionGraphMock = mock[RegionGraph]
  private val regionGraphProvider = ProviderAdapter.create(regionGraphMock)
  private val sitesServiceMock = mock[SitesGroupingService]

  private val dummyOfferBillingSeq = Seq(Model.OfferBilling.newBuilder().setVersion(0).build())

  val siteMapsPromotionStorage: SiteMapsPromotionStorage = SiteMapsPromotionStorage(Set(42L))
  val siteMapsPromotionProvider: Provider[SiteMapsPromotionStorage] = () => siteMapsPromotionStorage

  val sitePromotionStorage: SitePromotionStorage = SitePromotionStorage(Set(101L))
  val sitePromotionProvider: Provider[SitePromotionStorage] = () => sitePromotionStorage

  val campaignsProvider: Provider[CampaignStorage] = mock[Provider[CampaignStorage]]
  val companiesProvider: Provider[CompaniesStorage] = () => new CompaniesStorage(Seq[Company]().asJava)
  val auctionResultProvider: Provider[AuctionResultStorage] = () => new AuctionResultStorage(Seq())

  val redirectPhoneService: RedirectPhoneService = RedirectPhoneComponents.createRedirectPhoneService(teleponyClient)

  val personalRedirectService = new PersonalRedirectService(
    redirectPhoneService,
    regionGraphProvider,
    companiesProvider,
    auctionResultProvider,
    features
  )(TestOperationalSupport)

  val testCampaignPhoneEnricher: CampaignPhoneEnricher =
    new CampaignPhoneEnricher(
      siteMapsPromotionProvider,
      sitePromotionProvider,
      redirectPhoneService,
      campaignsProvider,
      sitesServiceMock,
      regionGraphProvider,
      personalRedirectService
    ) {
      override def enrichPhone(
        campaign: Campaign
      ): Option[Campaign] = Some(campaign)
    }

  private val campaignParser = mock[CampaignParser]

  (billingCampaignClient.getOfferBillings(_: Traced)).expects(*).returning(Future.successful(dummyOfferBillingSeq))

  private val campaignFetcher: CampaignFetcher =
    new CampaignFetcher(controller, billingCampaignClient, testCampaignPhoneEnricher, campaignParser)

  "CampaignFetcher" should {
    "put additional phones for active campaigns" in {
      (campaignParser.parseBatch(_: java.util.List[Model.OfferBilling])).expects(*).returning(Seq(dummyCampaign).asJava)

      campaignFetcher.fetch(None).toOption should not be empty
    }

    "put additional phones for inactive campaigns with super call" in {
      (campaignParser
        .parseBatch(_: java.util.List[Model.OfferBilling]))
        .expects(*)
        .returning(Seq(inactiveDummyCampaignWithSuperCall).asJava)

      campaignFetcher.fetch(None).toOption should not be empty
    }
  }

}
