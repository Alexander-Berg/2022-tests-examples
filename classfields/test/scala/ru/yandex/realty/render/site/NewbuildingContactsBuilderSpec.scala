package ru.yandex.realty.render.site

import org.joda.time.{DateTime, DateTimeUtils, Instant}
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.junit.JUnitRunner
import realty.response.Response
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.application.RedirectPhoneComponents
import ru.yandex.realty.billing.BillingDumpService
import ru.yandex.realty.context.v2.AuctionResultStorage
import ru.yandex.realty.features.FeaturesStubComponent
import ru.yandex.realty.geo.RegionGraphTestComponents
import ru.yandex.realty.model.auction.AuctionResult
import ru.yandex.realty.model.billing.Campaign
import ru.yandex.realty.model.location.Location
import ru.yandex.realty.model.message.ExtDataSchema.SalesDepartmentMessage
import ru.yandex.realty.model.phone.PhoneRedirect
import ru.yandex.realty.model.phone.RealtyPhoneTags.{
  CallCenterTagName,
  DefaultTagName,
  EmptyTagName,
  PersonalDefaultTagName,
  RtbHouseDesktopCampaignTagName
}
import ru.yandex.realty.model.region.{NodeRgid, Regions}
import ru.yandex.realty.model.serialization.phone.PhoneRedirectProtoConverter
import ru.yandex.realty.model.sites.{Company, Site}
import ru.yandex.realty.phone.{PersonalRedirectService, RedirectPhoneRefresher, RedirectPhoneService}
import ru.yandex.realty.proto.phone.PhoneRedirectMessage
import ru.yandex.realty.proto.site.api.NewbuildingContacts
import ru.yandex.realty.search.site.callcenter.{
  CallCenterPhonesService,
  CallCenterRegionsService,
  DefaultCallCenterRegionsService
}
import ru.yandex.realty.search.site.departments.SalesDepartmentsBuilder
import ru.yandex.realty.services.SalesDepartmentService
import ru.yandex.realty.sites.campaign.CampaignStorage
import ru.yandex.realty.sites.{CompaniesStorage, SitesGroupingService}
import ru.yandex.realty.telepony.TeleponyClientMockComponents
import ru.yandex.realty.tracing.Traced
import ru.yandex.vertis.ops.test.TestOperationalSupport

import java.time.Duration
import java.util
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext

@RunWith(classOf[JUnitRunner])
class NewbuildingContactsBuilderSpec
  extends AsyncSpecBase
  with FeaturesStubComponent
  with RegionGraphTestComponents
  with TeleponyClientMockComponents
  with BeforeAndAfterEach {

  override def afterEach(): Unit = {
    DateTimeUtils.setCurrentMillisSystem()
  }

  "NewbuildingContactsBuilder build" should {
    "return personal contacts during day" in new NewbuildingContactsBuilderFixture {
      DateTimeUtils.setCurrentMillisFixed(DateTime.parse("2022-07-01T14:00:01").getMillis)
      expectTeleponyCall(personalRedirect)

      val site = new Site(siteId)
      val location = new Location()
      location.setGeocoderId(Regions.MSK_AND_MOS_OBLAST)
      location.setSubjectFederation(Regions.MSK_AND_MOS_OBLAST, NodeRgid.MOSCOW_AND_MOS_OBLAST)
      site.setLocation(location)

      val contacts: NewbuildingContacts = newbuildingContactsBuilder.build(site, Some(tag))(Traced.empty).futureValue
      contacts.getSalesDepartments(0).getPhones(0) shouldBe personalRedirectMessage.getSource
    }

    "return personal contacts at night" in new NewbuildingContactsBuilderFixture {
      DateTimeUtils.setCurrentMillisFixed(DateTime.parse("2022-07-01T03:00:01").getMillis)
      expectTeleponyCall(defaultRedirect)

      val site = new Site(siteId)
      val location = new Location()
      location.setGeocoderId(Regions.MSK_AND_MOS_OBLAST)
      location.setSubjectFederation(Regions.MSK_AND_MOS_OBLAST, NodeRgid.MOSCOW_AND_MOS_OBLAST)
      site.setLocation(location)

      val contacts: NewbuildingContacts = newbuildingContactsBuilder.build(site, Some(tag))(Traced.empty).futureValue
      contacts.getSalesDepartments(0).getPhones(0) shouldBe defaultRedirectMessage.getSource
    }

    "return personal contacts in case campaign is inactive less than 2 days during day" in new NewbuildingContactsBuilderFixture {
      DateTimeUtils.setCurrentMillisFixed(DateTime.parse("2022-07-01T14:00:01").getMillis)
      expectTeleponyCall(recentPersonalRedirect)

      val site = new Site(recentlyInactiveSiteId)
      val location = new Location()
      location.setGeocoderId(Regions.MSK_AND_MOS_OBLAST)
      location.setSubjectFederation(Regions.MSK_AND_MOS_OBLAST, NodeRgid.MOSCOW_AND_MOS_OBLAST)
      site.setLocation(location)

      val contacts: NewbuildingContacts =
        newbuildingContactsBuilder.build(site, Some(recentTag))(Traced.empty).futureValue
      contacts.getSalesDepartments(0).getPhones(0) shouldBe recentPersonalRedirectMessage.getSource
    }

    "return personal contacts in case campaign is inactive less than 2 days at night" in new NewbuildingContactsBuilderFixture {
      DateTimeUtils.setCurrentMillisFixed(DateTime.parse("2022-07-01T03:00:01").getMillis)
      expectTeleponyCall(recentDefaultRedirect)

      val site = new Site(recentlyInactiveSiteId)
      val location = new Location()
      location.setGeocoderId(Regions.MSK_AND_MOS_OBLAST)
      location.setSubjectFederation(Regions.MSK_AND_MOS_OBLAST, NodeRgid.MOSCOW_AND_MOS_OBLAST)
      site.setLocation(location)

      val contacts: NewbuildingContacts =
        newbuildingContactsBuilder.build(site, Some(recentTag))(Traced.empty).futureValue
      contacts.getSalesDepartments(0).getPhones(0) shouldBe recentDefaultRedirectMessage.getSource
    }

    "return personal default" in new NewbuildingContactsBuilderFixture {
      expectTeleponyCallFailed(personalRedirect)
      expectTeleponyCall(defaultRedirect)

      val site = new Site(siteId)
      val location = new Location()
      location.setGeocoderId(Regions.MSK_AND_MOS_OBLAST)
      location.setSubjectFederation(Regions.MSK_AND_MOS_OBLAST, NodeRgid.MOSCOW_AND_MOS_OBLAST)
      site.setLocation(location)

      val contacts: NewbuildingContacts = newbuildingContactsBuilder.build(site, Some(tag))(Traced.empty).futureValue
      contacts.getSalesDepartments(0).getPhones(0) shouldBe defaultRedirectMessage.getSource
    }

    "return no personal phone cause not msk" in new NewbuildingContactsBuilderFixture {
      expectTeleponyCall(emptyRedirect)

      val site = new Site(siteId)
      val location = new Location()
      location.setGeocoderId(Regions.YAROSLAVSKAYA_OBLAST)
      location.setSubjectFederation(Regions.YAROSLAVSKAYA_OBLAST, NodeRgid.YAROSLAVSKAYA_OBLAST)
      site.setLocation(location)

      val contacts: NewbuildingContacts = newbuildingContactsBuilder.build(site, None)(Traced.empty).futureValue
      contacts.getSalesDepartments(0).getPhones(0) shouldBe emptyRedirectMessage.getSource
    }

    "return call center redirect" in new NewbuildingContactsBuilderFixture {
      val site = new Site(novosibSiteId)
      site.setCallCenterPhonesCanBeUsed(true)
      val location = new Location()
      location.setGeocoderId(Regions.NOVOSIBIRSK)
      location.setSubjectFederation(Regions.NOVOSIBIRSK, NodeRgid.NOVOSIBIRSKAYA_OBLAST)
      site.setLocation(location)
      site.setSalesDepartmentCallCenterPhones(
        Seq(callCenterRedirectMessage).map(_.getSource).asJava
      )
      site.setSalesDepartmentCallCenterPhones(Seq("89261111111").asJava)
      site.setSalesDepartmentCallCenterRedirects(Seq(callCenterRedirectMessage).asJava)

      val contacts: NewbuildingContacts = newbuildingContactsBuilder.build(site, None)(Traced.empty).futureValue
      contacts.getSalesDepartments(0).getPhones(0) shouldBe callCenterRedirectMessage.getSource
      val phoneWithTagResponse: Response.PhoneWithTagResponse = contacts.getSalesDepartments(0).getPhonesWithTag(0)
      phoneWithTagResponse.getRedirectId shouldBe callCenterRedirectMessage.getId
      phoneWithTagResponse.getTag shouldBe callCenterRedirectMessage.getTag
      phoneWithTagResponse.getPhone shouldBe callCenterRedirectMessage.getSource
      val phone: String = contacts.getSalesDepartments(0).getPhones(0)
      phone shouldBe callCenterRedirectMessage.getSource
    }

    "return default if no call center redirect found" in new NewbuildingContactsBuilderFixture {
      expectTeleponyCall(emptyRedirect)

      val site = new Site(novosibSiteId)
      site.setCallCenterPhonesCanBeUsed(false)
      val location = new Location()
      location.setGeocoderId(Regions.NOVOSIBIRSK)
      location.setSubjectFederation(Regions.NOVOSIBIRSK, NodeRgid.NOVOSIBIRSKAYA_OBLAST)
      site.setLocation(location)
      site.setSalesDepartmentCallCenterPhones(
        Seq(emptyRedirectMessage).map(_.getSource).asJava
      )
      site.setSalesDepartmentCallCenterRedirects(Seq(emptyRedirectMessage).asJava)

      val contacts: NewbuildingContacts = newbuildingContactsBuilder.build(site, None)(Traced.empty).futureValue
      contacts.getSalesDepartments(0).getPhones(0) shouldBe emptyRedirectMessage.getSource
      val phoneWithTagResponse: Response.PhoneWithTagResponse = contacts.getSalesDepartments(0).getPhonesWithTag(0)
      phoneWithTagResponse.getRedirectId shouldBe emptyRedirectMessage.getId
      phoneWithTagResponse.getTag shouldBe DefaultTagName
      phoneWithTagResponse.getPhone shouldBe emptyRedirectMessage.getSource
    }
  }

  trait NewbuildingContactsBuilderFixture {
    val novosibSiteId = 2234L
    val siteId = 1123L
    val recentlyInactiveSiteId = 3345L
    val tag = s"$RtbHouseDesktopCampaignTagName#siteId=$siteId"
    val recentTag = s"$RtbHouseDesktopCampaignTagName#siteId=$recentlyInactiveSiteId"

    val personalRedirectMessage: PhoneRedirectMessage = PhoneRedirectMessage
      .newBuilder()
      .setTag(tag)
      .setSource("896844444")
      .setTarget("892122222")
      .build()

    val recentPersonalRedirectMessage: PhoneRedirectMessage = PhoneRedirectMessage
      .newBuilder()
      .setTag(recentTag)
      .setSource("896844444")
      .setTarget("892122223")
      .build()

    val defaultRedirectMessage: PhoneRedirectMessage = PhoneRedirectMessage
      .newBuilder()
      .setTag(PersonalDefaultTagName)
      .setSource("896811111")
      .setTarget("892122222")
      .build()

    val recentDefaultRedirectMessage: PhoneRedirectMessage = PhoneRedirectMessage
      .newBuilder()
      .setTag(PersonalDefaultTagName)
      .setSource("896844445")
      .setTarget("892122223")
      .build()

    val emptyRedirectMessage: PhoneRedirectMessage = PhoneRedirectMessage
      .newBuilder()
      .setTag(EmptyTagName)
      .setSource("896833333")
      .setTarget("892122222")
      .build()

    val callCenterRedirectMessage: PhoneRedirectMessage = PhoneRedirectMessage
      .newBuilder()
      .setTag(CallCenterTagName)
      .setSource("89430000000")
      .setTarget("89261111111")
      .setGeoId(Regions.NOVOSIBIRSK)
      .setPhoneType(ru.yandex.vertis.telepony.model.proto.PhoneType.MOBILE)
      .build()

    val defaultRedirect: PhoneRedirect = PhoneRedirectProtoConverter.fromMessage(defaultRedirectMessage)
    val emptyRedirect: PhoneRedirect = PhoneRedirectProtoConverter.fromMessage(emptyRedirectMessage)
    val personalRedirect: PhoneRedirect = PhoneRedirectProtoConverter.fromMessage(personalRedirectMessage)
    val recentPersonalRedirect: PhoneRedirect = PhoneRedirectProtoConverter.fromMessage(recentPersonalRedirectMessage)
    val recentDefaultRedirect: PhoneRedirect = PhoneRedirectProtoConverter.fromMessage(recentDefaultRedirectMessage)
    val callCenterRedirect: PhoneRedirect = PhoneRedirectProtoConverter.fromMessage(callCenterRedirectMessage)

    val departmentMessage: SalesDepartmentMessage = SalesDepartmentMessage
      .newBuilder()
      .setCampaignId("randomCampaignId")
      .setIsRedirectPhones(true)
      .addPhoneRedirect(defaultRedirectMessage)
      .addPhoneRedirect(emptyRedirectMessage)
      .addPhones("892122222")
      .build()

    val novosibDepartmentMessage: SalesDepartmentMessage = SalesDepartmentMessage
      .newBuilder()
      .setCampaignId("novosibCampaignId")
      .setIsRedirectPhones(true)
      .addPhoneRedirect(callCenterRedirectMessage)
      .addPhoneRedirect(emptyRedirectMessage)
      .addPhones("89261111111")
      .addPhones("892122222")
      .build()

    val recentlyInactiveDepartmentMessage: SalesDepartmentMessage = SalesDepartmentMessage
      .newBuilder()
      .setCampaignId("recentCampaignId")
      .setIsRedirectPhones(true)
      .addPhoneRedirect(defaultRedirectMessage)
      .addPhoneRedirect(emptyRedirectMessage)
      .addPhones("892122223")
      .build()

    val auctionResultStorage = new AuctionResultStorage(
      Seq(
        AuctionResult(siteId, IndexedSeq(departmentMessage), None, null, None, None, 1L),
        AuctionResult(novosibSiteId, IndexedSeq(novosibDepartmentMessage), None, null, None, None, 1L),
        AuctionResult(recentlyInactiveSiteId, IndexedSeq(recentlyInactiveDepartmentMessage), None, null, None, None, 1L)
      )
    )

    val campaignStorage: CampaignStorage = new CampaignStorage(
      util.List.of(
        new Campaign(
          "id",
          novosibSiteId,
          12L,
          callCenterRedirectMessage.getTarget,
          new util.HashMap[String, String](),
          util.Collections.emptyList(),
          util.Collections.emptyList(),
          1L,
          1L,
          false,
          false,
          123L,
          321L,
          Instant.now(),
          new util.HashMap[String, String](),
          null,
          null
        ),
        new Campaign(
          "activeId",
          siteId,
          13L,
          "somePhone",
          new util.HashMap[String, String](),
          util.Collections.emptyList(),
          util.Collections.emptyList(),
          1L,
          1L,
          true,
          false,
          123L,
          321L,
          Instant.now(),
          new util.HashMap[String, String](),
          null,
          null
        ),
        new Campaign(
          "recentlyInactiveId",
          recentlyInactiveSiteId,
          14L,
          "somePhone1",
          new util.HashMap[String, String](),
          util.Collections.emptyList(),
          util.Collections.emptyList(),
          1L,
          1L,
          false,
          false,
          123L,
          321L,
          Instant.now(),
          new util.HashMap[String, String](),
          null,
          Instant.now().minus(org.joda.time.Duration.standardHours(47))
        )
      )
    )

    val callCenterRegionsService: CallCenterRegionsService = new DefaultCallCenterRegionsService(
      Set(Regions.NOVOSIBIRSK),
      Set(Regions.NOVOSIBIRSK),
      regionGraphProvider
    )

    val callCenterPhonesService: CallCenterPhonesService = new CallCenterPhonesService(
      Map(Regions.NOVOSIBIRSK -> Seq("89261111111")),
      callCenterRegionsService
    )

    val auctionResultProvider: Provider[AuctionResultStorage] = () => new AuctionResultStorage(Seq())

    val companiesProvider: Provider[CompaniesStorage] = () => new CompaniesStorage(Seq[Company]().asJava)

    val sitesService: SitesGroupingService = mock[SitesGroupingService]

    val redirectPhoneService: RedirectPhoneService = RedirectPhoneComponents.createRedirectPhoneService(teleponyClient)
    val redirectPhoneRefresher: RedirectPhoneRefresher = new RedirectPhoneRefresher(redirectPhoneService)
    val billingDumpService: BillingDumpService = mock[BillingDumpService]

    val salesDepartmentService: SalesDepartmentService = new SalesDepartmentService(
      redirectPhoneRefresher,
      billingDumpService
    )
    val salesDepartmentsBuilder: SalesDepartmentsBuilder = mock[SalesDepartmentsBuilder]
    features.PersonalPhoneRedirectsEnabled.setNewState(true)

    val personalRedirectService: PersonalRedirectService = new PersonalRedirectService(
      redirectPhoneService,
      regionGraphProvider,
      companiesProvider,
      auctionResultProvider,
      features
    )(TestOperationalSupport)

    val newbuildingContactsBuilder = new NewbuildingContactsBuilder(
      () => auctionResultStorage,
      () => campaignStorage,
      Duration.parse("PT172800S"),
      salesDepartmentService,
      salesDepartmentsBuilder,
      redirectPhoneService,
      personalRedirectService
    )(ExecutionContext.global)
  }
}
