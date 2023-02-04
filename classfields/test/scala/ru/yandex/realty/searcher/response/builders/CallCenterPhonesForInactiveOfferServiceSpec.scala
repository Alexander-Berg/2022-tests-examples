package ru.yandex.realty.searcher.response.builders

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.SpecBase
import ru.yandex.realty.billing.BillingCampaignStorage
import ru.yandex.realty.context.v2.AuctionResultStorage
import ru.yandex.realty.geo.RegionGraphTestComponents
import ru.yandex.realty.model.offer.{BuildingInfo, Offer}
import ru.yandex.realty.model.phone.RealtyPhoneTags.CallCenterTagName
import ru.yandex.realty.model.phone.{PhoneRedirect, PhoneType}
import ru.yandex.realty.model.region.Regions
import ru.yandex.realty.model.serialization.phone.PhoneRedirectProtoConverter
import ru.yandex.realty.model.sites.Site
import ru.yandex.realty.search.site.callcenter.{
  CallCenterPhonesService,
  CallCenterRegionsService,
  DefaultCallCenterRegionsService
}
import ru.yandex.realty.sites.{CompaniesStorage, SitesGroupingService}
import ru.yandex.realty.telepony.TeleponyClient.Domain
import ru.yandex.realty.telepony.TeleponyClientMockComponents

import java.util.Collections
import scala.collection.JavaConverters._
import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
class CallCenterPhonesForInactiveOfferServiceSpec
  extends SpecBase
  with PropertyChecks
  with RegionGraphTestComponents
  with TeleponyClientMockComponents {

  "CallCenterPhonesForInactiveOfferService" should {

    "getInactiveOfferPhones " in new CallCenterPhonesForInactiveOfferServiceFixture {
      offer.setPrimarySaleV2(true)

      site.setCallCenterPhonesCanBeUsed(true)
      site.setSalesDepartmentCallCenterPhones(Seq(redirect.target).asJava)
      site.setSalesDepartmentCallCenterRedirects(Seq(redirectMessage).asJava)

      service.shouldUseCallCenterPhones(offer) shouldBe true

      val phones = service.getCallCenterPhonesForInactiveOffer(offer).get
      phones.direct should have size 1
      phones.direct.iterator.next() shouldBe redirect.source
      phones.redirect should have size 1
      phones.redirect.iterator.next() shouldBe redirect
    }

    "getInactiveOfferPhones for non primary sale " in new CallCenterPhonesForInactiveOfferServiceFixture {
      site.setCallCenterPhonesCanBeUsed(true)
      site.setSalesDepartmentCallCenterPhones(Seq(redirect.target).asJava)
      site.setSalesDepartmentCallCenterRedirects(Seq(redirectMessage).asJava)

      service.shouldUseCallCenterPhones(offer) shouldBe false
    }

    "getInactiveOfferPhones for site without call center phones " in new CallCenterPhonesForInactiveOfferServiceFixture {
      offer.setPrimarySaleV2(true)

      service.shouldUseCallCenterPhones(offer) shouldBe false
    }

    "getInactiveOfferPhones for offer without site" in new CallCenterPhonesForInactiveOfferServiceFixture {
      offer.setPrimarySaleV2(true)

      site.setCallCenterPhonesCanBeUsed(true)
      site.setSalesDepartmentCallCenterPhones(Seq(redirect.target).asJava)
      site.setSalesDepartmentCallCenterRedirects(Seq(redirectMessage).asJava)

      offer.setBuildingInfo(new BuildingInfo())

      service.shouldUseCallCenterPhones(offer) shouldBe false
    }

    "getInactiveOfferPhones for offer with site not found" in new CallCenterPhonesForInactiveOfferServiceFixture {
      offer.setPrimarySaleV2(true)

      site.setCallCenterPhonesCanBeUsed(true)
      site.setSalesDepartmentCallCenterPhones(Seq(redirect.target).asJava)
      site.setSalesDepartmentCallCenterRedirects(Seq(redirectMessage).asJava)

      val info: BuildingInfo = new BuildingInfo()
      info.setSiteId(33L)
      offer.setBuildingInfo(info)

      (sitesGroupingService
        .getSiteById(_: Long))
        .expects(33L)
        .returning(null)

      service.shouldUseCallCenterPhones(offer) shouldBe false
    }

    "getInactiveOfferPhones for site with no call center tag in redirects" in new CallCenterPhonesForInactiveOfferServiceFixture {
      offer.setPrimarySaleV2(true)

      site.setCallCenterPhonesCanBeUsed(true)
      site.setSalesDepartmentCallCenterPhones(Seq(redirect.target).asJava)
      site.setSalesDepartmentCallCenterRedirects(Seq(redirectMessage.toBuilder.setTag("incorrectTag").build()).asJava)

      service.shouldUseCallCenterPhones(offer) shouldBe true
      val phones = service.getCallCenterPhonesForInactiveOffer(offer).get
      phones.direct should have size 1
      phones.direct.iterator.next() shouldBe redirect.target
      phones.redirect should have size 0
    }
  }

  trait CallCenterPhonesForInactiveOfferServiceFixture {
    val sitesGroupingService = mock[SitesGroupingService]

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

    val billingCampaign = mock[BillingCampaignStorage]
    val companies = new CompaniesStorage(Collections.emptyList())

    val service = new CallCenterPhonesForInactiveOfferService(
      sitesGroupingService
    )

    val offer = new Offer()
    val buildingInfo = new BuildingInfo()
    val siteId = 123123L
    buildingInfo.setSiteId(siteId)
    offer.setBuildingInfo(buildingInfo)

    val redirect = PhoneRedirect(
      Domain.`realty-offers`,
      "",
      "123",
      Some(CallCenterTagName),
      new DateTime(),
      None,
      "callCenterSource",
      "callCenterTarget",
      Some(PhoneType.Mobile),
      None,
      Some(2.days)
    )
    val redirectMessage = PhoneRedirectProtoConverter.toMessage(redirect)

    val site = new Site(siteId)

    (sitesGroupingService
      .getSiteById(_: Long))
      .expects(siteId)
      .anyNumberOfTimes()
      .returning(site)
  }
}
