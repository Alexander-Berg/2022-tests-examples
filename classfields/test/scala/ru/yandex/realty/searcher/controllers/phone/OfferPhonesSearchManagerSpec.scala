package ru.yandex.realty.searcher.controllers.phone

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.application.RedirectPhoneComponents
import ru.yandex.realty.auth.{Application, AuthInfo}
import ru.yandex.realty.billing.BillingDumpService
import ru.yandex.realty.billing.BillingDumpService.BillingDump
import ru.yandex.realty.context.v2.AuctionResultStorage
import ru.yandex.realty.features.FeaturesStubComponent
import ru.yandex.realty.graph.RegionGraph
import ru.yandex.realty.graph.core.Node
import ru.yandex.realty.model.auction.AuctionResult
import ru.yandex.realty.model.location.Location
import ru.yandex.realty.model.message.ExtDataSchema.{PhoneMessage, SalesDepartmentMessage}
import ru.yandex.realty.model.offer.{BuildingInfo, Offer, SaleAgent}
import ru.yandex.realty.model.phone.RealtyPhoneTags.{
  NewbuildingOfferTagName,
  PlatformDesktopOfferTagName,
  PlatformIosCampaignTagName,
  PlatformIosOfferTagName
}
import ru.yandex.realty.model.phone.{PhoneRedirect, PhoneType}
import ru.yandex.realty.model.region.{NodeRgid, Regions}
import ru.yandex.realty.model.sites.{Company, ExtendedSiteStatisticsAtom, Site}
import ru.yandex.realty.offers.OfferAdditionalDataService
import ru.yandex.realty.phone.{CampaignOfferTagResolver, PersonalRedirectService, PhoneTagRequestParams}
import ru.yandex.realty.platform.PlatformInfo
import ru.yandex.realty.proto.phone.PhoneRedirectMessage
import ru.yandex.realty.request.{Request, RequestImpl}
import ru.yandex.realty.search.site.callcenter.CallCenterPhonesAndRedirects
import ru.yandex.realty.searcher.api.SearcherApi.OfferPhonesResponse
import ru.yandex.realty.searcher.context.{SearchContext, SearchContextProvider}
import ru.yandex.realty.searcher.controllers.phone.PhoneSearchersHelper.redirectPhonesResponse
import ru.yandex.realty.searcher.controllers.phone.redirect.offer.{
  SecondaryOfferPersonalRedirectManager,
  SecondaryOfferPersonalRedirectService
}
import ru.yandex.realty.searcher.controllers.phone.redirect.site.NewBuildingPhoneSearcher
import ru.yandex.realty.searcher.controllers.phone.redirect.village.VillagePhoneSearcher
import ru.yandex.realty.searcher.response.builders.CallCenterPhonesForInactiveOfferService
import ru.yandex.realty.searcher.search.LuceneByIdSearcher
import ru.yandex.realty.sites.{CompaniesStorage, SitesGroupingService}
import ru.yandex.realty.telepony.TeleponyClient.Domain
import ru.yandex.realty.telepony.TeleponyClientMockComponents
import ru.yandex.realty.villages.VillageDynamicInfoStorage
import ru.yandex.vertis.ops.test.TestOperationalSupport

import java.time.Instant
import java.util
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
class OfferPhonesSearchManagerSpec
  extends AsyncSpecBase
  with PropertyChecks
  with FeaturesStubComponent
  with TeleponyClientMockComponents {

  val offerId = 1L
  val offerIdForCallCenter = 1231L
  val companyId = 2L
  val siteId = 2L
  val luceneByIdSearcher: LuceneByIdSearcher = mock[LuceneByIdSearcher]
  val searchContextProvider: SearchContextProvider[SearchContext] = mock[SearchContextProvider[SearchContext]]
  val villageDynamicInfoProvider: Provider[VillageDynamicInfoStorage] = () => mock[VillageDynamicInfoStorage]
  val auctionResultStorage: AuctionResultStorage = prepareAuctionResult
  val billingDumpService: BillingDumpService = mock[BillingDumpService]
  val offerAdditionalDataService: OfferAdditionalDataService = mock[OfferAdditionalDataService]
  val inactiveOfferService: CallCenterPhonesForInactiveOfferService = mock[CallCenterPhonesForInactiveOfferService]
  val regionGraph: RegionGraph = mock[RegionGraph]
  val sitesService: SitesGroupingService = mock[SitesGroupingService]
  val companiesProvider: Provider[CompaniesStorage] = () => new CompaniesStorage(Seq[Company]().asJava)
  val auctionResultProvider: Provider[AuctionResultStorage] = () => auctionResultStorage

  val secondaryOfferPersonalRedirectManager: SecondaryOfferPersonalRedirectManager =
    mock[SecondaryOfferPersonalRedirectManager]

  val secondaryRedirectService: SecondaryOfferPersonalRedirectService =
    mock[SecondaryOfferPersonalRedirectService]
  features.AddPlatformTagsForPhoneRedirects.setNewState(true)
  features.AdSourceTagsForPhoneRedirects.setNewState(true)

  trait OfferPhonesSearchManagerCommonFixture {

    private val redirectPhoneService =
      RedirectPhoneComponents.createRedirectPhoneService(teleponyClient)(ExecutionContext.global)

    features.PersonalPhoneRedirectsEnabled.setNewState(false)

    private val personalRedirectService = new PersonalRedirectService(
      redirectPhoneService,
      () => regionGraph,
      companiesProvider,
      auctionResultProvider,
      features
    )(TestOperationalSupport)

    val villagePhoneSearcher = new VillagePhoneSearcher(
      billingDumpService,
      villageDynamicInfoProvider
    )

    val newBuildingPhoneSearcher = new NewBuildingPhoneSearcher(
      billingDumpService,
      new CampaignOfferTagResolver(() => regionGraph, features, personalRedirectService),
      personalRedirectService,
      () => auctionResultStorage,
      sitesService
    )

    val phoneSearcherResolver = new PhoneSearchersResolver(
      inactiveOfferService,
      offerAdditionalDataService,
      villagePhoneSearcher,
      newBuildingPhoneSearcher,
      secondaryOfferPersonalRedirectManager,
      secondaryRedirectService,
      TestOperationalSupport
    )

    val manager = new OfferPhonesSearchManager(
      luceneByIdSearcher,
      searchContextProvider,
      phoneSearcherResolver
    )(ExecutionContext.global)

  }

  trait OfferPhonesSearchManagerFixture extends OfferPhonesSearchManagerCommonFixture {
    val offer: Offer = prepareOffer

    (searchContextProvider
      .doWithContext(_: SearchContext => Option[Offer]))
      .expects(*)
      .returning(Option(offer))

    (inactiveOfferService
      .shouldUseCallCenterPhones(_: Offer))
      .expects(offer)
      .returning(true)

    (inactiveOfferService
      .getCallCenterPhonesForInactiveOffer(_: Offer))
      .expects(offer)
      .returning(None)

    (offerAdditionalDataService
      .isNewbuilding(_: Offer))
      .expects(offer)
      .returns(true)
      .anyNumberOfTimes()

    (billingDumpService
      .updateRedirectPhone(_: BillingDump, _: PhoneMessage))
      .expects(*, *)
      .returning(new util.HashMap())

    (sitesService
      .getSiteById(_: Long))
      .expects(siteId)
      .returns(new Site(siteId))
      .noMoreThanOnce()
  }

  trait SecondaryOfferPhonesSearchManagerFixture extends OfferPhonesSearchManagerCommonFixture {
    val offer: Offer = prepareOffer

    (searchContextProvider
      .doWithContext(_: SearchContext => Option[Offer]))
      .expects(*)
      .returning(Option(offer))

    (inactiveOfferService
      .shouldUseCallCenterPhones(_: Offer))
      .expects(offer)
      .returning(true)

    (inactiveOfferService
      .getCallCenterPhonesForInactiveOffer(_: Offer))
      .expects(offer)
      .returning(None)

    (offerAdditionalDataService
      .isNewbuilding(_: Offer))
      .expects(offer)
      .returns(false)
      .anyNumberOfTimes()

    (offerAdditionalDataService
      .isVillage(_: Offer))
      .expects(offer)
      .returns(false)
      .anyNumberOfTimes()
  }

  "OfferPhonesSearchManager" should {
    "search with no tag given from ios device " in new OfferPhonesSearchManagerFixture {
      initRegionGraph

      val request = new RequestImpl
      request.setApplication(Application.UnitTests)
      request.setAuthInfo(AuthInfo())
      request.setPlatformInfo(Option(PlatformInfo("ios", "")))

      val offerPhoneResponse: OfferPhonesResponse =
        manager.search(offerId, PhoneTagRequestParams(None, None, Set.empty))(request).futureValue

      val phone: OfferPhonesResponse.RedirectPhone = offerPhoneResponse.getRedirectPhones(0)
      phone.getTag shouldBe PlatformIosOfferTagName
      phone.getSource shouldBe "source"
      phone.getTarget shouldBe "target"
    }

    "search with explicit tag given from ios device " in new OfferPhonesSearchManagerFixture {
      val request = new RequestImpl
      request.setApplication(Application.UnitTests)
      request.setAuthInfo(AuthInfo())
      request.setPlatformInfo(Option(PlatformInfo("ios", "")))
      val offerPhoneResponse: OfferPhonesResponse =
        manager.search(offerId, PhoneTagRequestParams(Some("differentTag"), None, Set.empty))(request).futureValue

      val phone: OfferPhonesResponse.RedirectPhone = offerPhoneResponse.getRedirectPhones(0)
      phone.getTag shouldBe "differentTag"
      phone.getSource shouldBe "differentSource"
      phone.getTarget shouldBe "differentTarget"
    }

    "search with no tag given from desktop " in new OfferPhonesSearchManagerFixture {
      initRegionGraph

      val request = new RequestImpl
      request.setApplication(Application.UnitTests)
      request.setAuthInfo(AuthInfo())
      request.setPlatformInfo(Option(PlatformInfo("desktop", "")))
      val offerPhoneResponse: OfferPhonesResponse =
        manager.search(offerId, PhoneTagRequestParams(None, None, Set.empty))(request).futureValue

      val phone: OfferPhonesResponse.RedirectPhone = offerPhoneResponse.getRedirectPhones(0)
      phone.getTag shouldBe PlatformDesktopOfferTagName
      phone.getSource shouldBe "desktopSource"
      phone.getTarget shouldBe "desktopTarget"
    }

    "search with no tag given from ios from another region " in new OfferPhonesSearchManagerFixture {
      val node = new Node()
      node.setGeoId(22)
      (regionGraph.getNodeByGeoId(_: Int)).expects(1).anyNumberOfTimes().returns(node)
      (regionGraph.getRandomParent(_: Node)).expects(*).anyNumberOfTimes().returns(null)

      val request = new RequestImpl
      request.setApplication(Application.UnitTests)
      request.setAuthInfo(AuthInfo())
      request.setPlatformInfo(Option(PlatformInfo("ios", "")))
      val offerPhoneResponse: OfferPhonesResponse =
        manager.search(offerId, PhoneTagRequestParams(None, None, Set.empty))(request).futureValue

      val phone: OfferPhonesResponse.RedirectPhone = offerPhoneResponse.getRedirectPhones(0)
      phone.getTag shouldBe NewbuildingOfferTagName
      phone.getSource shouldBe "notMoscowSource"
      phone.getTarget shouldBe "notMoscowTarget"
    }

    "search with no tag from android if no android redirect specified " in new OfferPhonesSearchManagerFixture {
      initRegionGraph

      val request = new RequestImpl
      request.setApplication(Application.UnitTests)
      request.setAuthInfo(AuthInfo())
      request.setPlatformInfo(Option(PlatformInfo("andoid", "")))
      val offerPhoneResponse: OfferPhonesResponse =
        manager.search(offerId, PhoneTagRequestParams(None, None, Set.empty))(request).futureValue

      val phone: OfferPhonesResponse.RedirectPhone = offerPhoneResponse.getRedirectPhones(0)
      phone.getTag shouldBe ""
      phone.getSource shouldBe "noneSource"
      phone.getTarget shouldBe "noneTarget"
    }

    "search with no tag given and no platform info given " in new OfferPhonesSearchManagerFixture {
      initRegionGraph

      val request = new RequestImpl
      request.setApplication(Application.UnitTests)
      request.setAuthInfo(AuthInfo())
      val offerPhoneResponse: OfferPhonesResponse =
        manager.search(offerId, PhoneTagRequestParams(None, None, Set.empty))(request).futureValue

      val phone: OfferPhonesResponse.RedirectPhone = offerPhoneResponse.getRedirectPhones(0)
      phone.getTag shouldBe ""
      phone.getSource shouldBe "noneSource"
      phone.getTarget shouldBe "noneTarget"
    }

    "search with empty tag given from ios device " in new OfferPhonesSearchManagerFixture {
      initRegionGraph

      val request = new RequestImpl
      request.setApplication(Application.UnitTests)
      request.setAuthInfo(AuthInfo())
      request.setPlatformInfo(Option(PlatformInfo("ios", "")))

      val offerPhoneResponse: OfferPhonesResponse =
        manager.search(offerId, PhoneTagRequestParams(Some(""), None, Set.empty))(request).futureValue

      val phone: OfferPhonesResponse.RedirectPhone = offerPhoneResponse.getRedirectPhones(0)
      phone.getTag shouldBe PlatformIosOfferTagName
      phone.getSource shouldBe "source"
      phone.getTarget shouldBe "target"
    }

    "search inactive offer phone redirect" in new OfferPhonesSearchManagerCommonFixture {
      initRegionGraph

      val callCenterOffer: Offer = prepareCallCenterOffer

      (searchContextProvider
        .doWithContext(_: SearchContext => Option[Offer]))
        .expects(*)
        .returning(Option(callCenterOffer))

      (inactiveOfferService
        .shouldUseCallCenterPhones(_: Offer))
        .expects(callCenterOffer)
        .returning(true)

      (inactiveOfferService
        .getCallCenterPhonesForInactiveOffer(_: Offer))
        .expects(callCenterOffer)
        .returning(Some(CallCenterPhonesAndRedirects(Seq("sourceCallCenter"))))

      val request = new RequestImpl
      request.setApplication(Application.UnitTests)
      request.setAuthInfo(AuthInfo())
      request.setPlatformInfo(Option(PlatformInfo("ios", "")))

      val offerPhoneResponse: OfferPhonesResponse =
        manager.search(offerIdForCallCenter, PhoneTagRequestParams(Some(""), None, Set.empty))(request).futureValue

      val phone: OfferPhonesResponse.DirectPhone = offerPhoneResponse.getDirectPhones(0)
      phone.getPhone shouldBe "sourceCallCenter"
    }

    "search secondary offer personal redirect" in new SecondaryOfferPhonesSearchManagerFixture {
      (secondaryOfferPersonalRedirectManager
        .shouldUsePersonalRedirect(_: Offer))
        .expects(offer)
        .returns(true)

      (secondaryOfferPersonalRedirectManager
        .getPersonalRedirects(_: Offer, _: PhoneTagRequestParams)(_: Request))
        .expects(offer, *, *)
        .returns(
          Some(
            redirectPhonesResponse(
              Seq(
                PhoneRedirect(
                  Domain.`realty-offers`,
                  "",
                  "partner_987",
                  Some("personalTagForSecondaryOffer"),
                  new DateTime(),
                  None,
                  "personalSource",
                  "personalTarget",
                  Some(PhoneType.Mobile),
                  None,
                  Some(2.days)
                )
              )
            )
          )
        )
        .once()

      initRegionGraph

      val request = new RequestImpl
      request.setApplication(Application.UnitTests)
      request.setAuthInfo(AuthInfo())
      request.setPlatformInfo(Option(PlatformInfo("ios", "")))

      val offerPhoneResponse: OfferPhonesResponse =
        manager.search(offerIdForCallCenter, PhoneTagRequestParams(Some(""), None, Set.empty))(request).futureValue

      val phone: OfferPhonesResponse.RedirectPhone = offerPhoneResponse.getRedirectPhones(0)
      phone.getTag shouldBe "personalTagForSecondaryOffer"
      phone.getSource shouldBe "personalSource"
      phone.getTarget shouldBe "personalTarget"
    }

    "search secondary offer personal redirect fail - got saleAgent redirects " in
      new SecondaryOfferPhonesSearchManagerFixture {
        (secondaryOfferPersonalRedirectManager
          .shouldUsePersonalRedirect(_: Offer))
          .expects(offer)
          .returns(true)

        (secondaryOfferPersonalRedirectManager
          .getPersonalRedirects(_: Offer, _: PhoneTagRequestParams)(_: Request))
          .expects(offer, *, *)
          .returns(None)
          .once()

        initRegionGraph

        val request = new RequestImpl
        request.setApplication(Application.UnitTests)
        request.setAuthInfo(AuthInfo())
        request.setPlatformInfo(Option(PlatformInfo("ios", "")))

        val offerPhoneResponse: OfferPhonesResponse =
          manager.search(offerIdForCallCenter, PhoneTagRequestParams(Some(""), None, Set.empty))(request).futureValue

        val phone: OfferPhonesResponse.RedirectPhone = offerPhoneResponse.getRedirectPhones(0)
        phone.getTag shouldBe ""
        phone.getSource shouldBe "source1"
        phone.getTarget shouldBe "target1"
      }
  }

  private def initRegionGraph = {
    val node = new Node()
    node.setGeoId(1)
    (regionGraph.getNodeByGeoId(_: Int)).expects(1).anyNumberOfTimes().returns(node)
  }

  // scalastyle:off
  private def prepareAuctionResult = {
    val salesDepartmentMessage = SalesDepartmentMessage.newBuilder
      .setId(companyId)
      .setName("sdName")
      .setLogo("sdLogo")
      .addPhoneRedirect(
        PhoneRedirectMessage.newBuilder
          .setDomain("billing_realty")
          .setObjectId("objectId")
          .setSource("source")
          .setTarget("target")
          .setTag(PlatformIosOfferTagName)
          .setId("redirectId")
          .setCreateTime(Instant.now().toEpochMilli)
          .setDeadline(Instant.now().plusSeconds(60).toEpochMilli)
      )
      .addPhoneRedirect(
        PhoneRedirectMessage.newBuilder
          .setDomain("billing_realty")
          .setObjectId("campaignObjectId")
          .setSource("campaignSource")
          .setTarget("campaignTarget")
          .setTag(PlatformIosCampaignTagName)
          .setId("campaignRedirectId")
          .setCreateTime(Instant.now().toEpochMilli)
          .setDeadline(Instant.now().plusSeconds(60).toEpochMilli)
      )
      .addPhoneRedirect(
        PhoneRedirectMessage.newBuilder
          .setDomain("billing_realty")
          .setObjectId("desktopObjectId")
          .setSource("desktopSource")
          .setTarget("desktopTarget")
          .setTag(PlatformDesktopOfferTagName)
          .setId("desktopRedirectId")
          .setCreateTime(Instant.now().toEpochMilli)
          .setDeadline(Instant.now().plusSeconds(60).toEpochMilli)
      )
      .addPhoneRedirect(
        PhoneRedirectMessage.newBuilder
          .setDomain("billing_realty")
          .setObjectId("notMoscowObjectId")
          .setSource("notMoscowSource")
          .setTarget("notMoscowTarget")
          .setTag(NewbuildingOfferTagName)
          .setId("notMoscowRedirectId")
          .setCreateTime(Instant.now().toEpochMilli)
          .setDeadline(Instant.now().plusSeconds(60).toEpochMilli)
      )
      .addPhoneRedirect(
        PhoneRedirectMessage.newBuilder
          .setDomain("billing_realty")
          .setObjectId("differentObjectId")
          .setSource("differentSource")
          .setTarget("differentTarget")
          .setTag("differentTag")
          .setId("differentRedirectId")
          .setCreateTime(Instant.now().toEpochMilli)
          .setDeadline(Instant.now().plusSeconds(60).toEpochMilli)
      )
      .addPhoneRedirect(
        PhoneRedirectMessage.newBuilder
          .setDomain("billing_realty")
          .setObjectId("noneObjectId")
          .setSource("noneSource")
          .setTarget("noneTarget")
          .setId("noneRedirectId")
          .setCreateTime(Instant.now().toEpochMilli)
          .setDeadline(Instant.now().plusSeconds(60).toEpochMilli)
      )
      .build()

    val salesDepartmentMessageSeq: IndexedSeq[SalesDepartmentMessage] =
      IndexedSeq.newBuilder.+=(salesDepartmentMessage).result()

    val auctionResult: AuctionResult = AuctionResult(
      siteId,
      salesDepartmentMessageSeq,
      None,
      ExtendedSiteStatisticsAtom.EMPTY,
      None,
      None,
      0L
    )

    new AuctionResultStorage(List(auctionResult))
  }

  private def prepareOffer = {
    val offer: Offer = new Offer()
    offer.setFromVos(false)
    offer.setCompanyIds(List(Long.box(companyId)).asJava)
    val buildingInfo = new BuildingInfo()
    buildingInfo.setSiteId(siteId)
    offer.setBuildingInfo(buildingInfo)
    val agent: SaleAgent = offer.createAndGetSaleAgent()
    agent.setRedirects(
      List(
        PhoneRedirect(
          "billing_realty",
          id = System.nanoTime().toString,
          tag = None,
          objectId = "objectId",
          createTime = new DateTime(),
          deadline = Some(new DateTime()),
          source = "source1",
          target = "target1",
          phoneType = None,
          geoId = None,
          ttl = None
        )
      ).asJava
    )
    val location = new Location()
    location.setGeocoderId(1)
    offer.setLocation(location)
    offer
  }

  private def prepareCallCenterOffer = {
    val offer: Offer = new Offer()
    offer.setId(offerIdForCallCenter)
    offer.setFromVos(false)
    offer.setCompanyIds(List(Long.box(companyId)).asJava)
    val buildingInfo = new BuildingInfo()
    buildingInfo.setSiteId(siteId)
    offer.setBuildingInfo(buildingInfo)
    val location = new Location()
    location.setGeocoderId(Regions.NOVOSIBIRSKAYA_OBLAST)
    location.setSubjectFederation(Regions.NOVOSIBIRSKAYA_OBLAST, NodeRgid.NOVOSIBIRSKAYA_OBLAST)
    offer.setLocation(location)
    offer
  }
}
